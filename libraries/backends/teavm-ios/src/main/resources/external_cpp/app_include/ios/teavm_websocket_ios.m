#include "teavm_websocket_ios.h"

#import <Foundation/Foundation.h>
#include <pthread.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#define WS_EVENT_NONE 0
#define WS_EVENT_OPEN 1
#define WS_EVENT_MESSAGE_TEXT 2
#define WS_EVENT_ERROR 3
#define WS_EVENT_CLOSE 4

#define WS_STATE_CONNECTING 0
#define WS_STATE_OPEN 1
#define WS_STATE_CLOSING 2
#define WS_STATE_CLOSED 3

static pthread_mutex_t g_teavm_ws_ios_error_lock = PTHREAD_MUTEX_INITIALIZER;
static char g_teavm_ws_ios_last_error[2048];

static void teavm_ws_ios_set_error(const char* message) {
    pthread_mutex_lock(&g_teavm_ws_ios_error_lock);
    if(message == NULL) {
        g_teavm_ws_ios_last_error[0] = '\0';
    }
    else {
        strncpy(g_teavm_ws_ios_last_error, message, sizeof(g_teavm_ws_ios_last_error) - 1);
        g_teavm_ws_ios_last_error[sizeof(g_teavm_ws_ios_last_error) - 1] = '\0';
    }
    pthread_mutex_unlock(&g_teavm_ws_ios_error_lock);
}

static int teavm_ws_ios_copy_string(char* target, int capacity, const char* value) {
    if(target == NULL || capacity <= 0) {
        return 0;
    }
    if(value == NULL) {
        target[0] = '\0';
        return 0;
    }
    int length = (int)strlen(value);
    int copy_length = length < capacity ? length : capacity - 1;
    memcpy(target, value, copy_length);
    target[copy_length] = '\0';
    return copy_length;
}

static NSString* teavm_ws_ios_string(const char* value) {
    if(value == NULL) {
        return @"";
    }
    NSString* string = [NSString stringWithUTF8String:value];
    return string == nil ? @"" : string;
}

static NSArray<NSString*>* teavm_ws_ios_protocols(const char* value) {
    NSString* text = [teavm_ws_ios_string(value) stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
    if(text.length == 0) {
        return nil;
    }
    NSArray<NSString*>* parts = [text componentsSeparatedByString:@","];
    NSMutableArray<NSString*>* protocols = [NSMutableArray arrayWithCapacity:parts.count];
    NSCharacterSet* whitespace = [NSCharacterSet whitespaceAndNewlineCharacterSet];
    for(NSString* part in parts) {
        NSString* protocol = [part stringByTrimmingCharactersInSet:whitespace];
        if(protocol.length > 0) {
            [protocols addObject:protocol];
        }
    }
    return protocols.count == 0 ? nil : protocols;
}

static int teavm_ws_ios_utf8_length(NSString* value) {
    if(value == nil || value.length == 0) {
        return 0;
    }
    return (int)[value lengthOfBytesUsingEncoding:NSUTF8StringEncoding];
}

static NSString* teavm_ws_ios_describe_error(NSError* error, NSString* fallback) {
    if(error == nil) {
        return fallback == nil ? @"iOS websocket error" : fallback;
    }
    NSString* message = error.localizedDescription;
    if(message == nil || message.length == 0) {
        return error.description == nil ? fallback : error.description;
    }
    return message;
}

@interface GdxTeaVMIOSWebSocketEvent : NSObject
@property(nonatomic, assign) int type;
@property(nonatomic, assign) int data0;
@property(nonatomic, assign) int data1;
@property(nonatomic, copy) NSString* message;
@end

@implementation GdxTeaVMIOSWebSocketEvent
@end

@interface GdxTeaVMIOSWebSocket : NSObject <NSURLSessionWebSocketDelegate>
@property(nonatomic, strong) NSURLSession* session;
@property(nonatomic, strong) NSURLSessionWebSocketTask* task;
@property(nonatomic, strong) NSMutableArray<GdxTeaVMIOSWebSocketEvent*>* events;
@property(nonatomic, strong) NSLock* lock;
@property(nonatomic, assign) int state;
@property(nonatomic, assign) BOOL closeEventEmitted;
@property(nonatomic, assign) BOOL insecureTls;
@end

@implementation GdxTeaVMIOSWebSocket

- (instancetype)initWithURL:(NSURL*)url protocols:(NSArray<NSString*>*)protocols insecureTls:(BOOL)insecureTls {
    self = [super init];
    if(self != nil) {
        _events = [NSMutableArray array];
        _lock = [[NSLock alloc] init];
        _state = WS_STATE_CONNECTING;
        _insecureTls = insecureTls && [url.scheme.lowercaseString isEqualToString:@"wss"];
        NSURLSessionConfiguration* configuration = [NSURLSessionConfiguration defaultSessionConfiguration];
        _session = [NSURLSession sessionWithConfiguration:configuration delegate:self delegateQueue:nil];
        _task = protocols.count > 0
                ? [_session webSocketTaskWithURL:url protocols:protocols]
                : [_session webSocketTaskWithURL:url];
    }
    return self;
}

- (void)start {
    [self.task resume];
}

- (int)currentState {
    [self.lock lock];
    int value = self.state;
    [self.lock unlock];
    return value;
}

- (void)setStateLocked:(int)state {
    [self.lock lock];
    self.state = state;
    [self.lock unlock];
}

- (void)pushEvent:(int)type data0:(int)data0 data1:(int)data1 message:(NSString*)message {
    GdxTeaVMIOSWebSocketEvent* event = [[GdxTeaVMIOSWebSocketEvent alloc] init];
    event.type = type;
    event.data0 = data0;
    event.data1 = data1;
    event.message = message == nil ? @"" : message;

    [self.lock lock];
    [self.events addObject:event];
    [self.lock unlock];
}

- (void)pushOpenEvent {
    [self pushEvent:WS_EVENT_OPEN data0:0 data1:0 message:nil];
}

- (void)pushTextEvent:(NSString*)message {
    [self pushEvent:WS_EVENT_MESSAGE_TEXT data0:teavm_ws_ios_utf8_length(message) data1:0 message:message];
}

- (void)pushErrorEvent:(NSString*)message {
    [self pushEvent:WS_EVENT_ERROR data0:teavm_ws_ios_utf8_length(message) data1:0 message:message];
}

- (void)emitCloseOnce:(int)code reason:(NSString*)reason {
    [self.lock lock];
    if(self.closeEventEmitted) {
        self.state = WS_STATE_CLOSED;
        [self.lock unlock];
        return;
    }
    self.closeEventEmitted = YES;
    self.state = WS_STATE_CLOSED;
    [self.lock unlock];

    NSString* normalizedReason = reason == nil ? @"" : reason;
    [self pushEvent:WS_EVENT_CLOSE
              data0:code
              data1:teavm_ws_ios_utf8_length(normalizedReason)
            message:normalizedReason];
}

- (GdxTeaVMIOSWebSocketEvent*)pollEvent {
    [self.lock lock];
    GdxTeaVMIOSWebSocketEvent* event = nil;
    if(self.events.count > 0) {
        event = self.events[0];
        [self.events removeObjectAtIndex:0];
    }
    [self.lock unlock];
    return event;
}

- (BOOL)sendText:(NSString*)text {
    if(self.task == nil || [self currentState] != WS_STATE_OPEN) {
        teavm_ws_ios_set_error("WebSocket is not open.");
        return NO;
    }

    NSURLSessionWebSocketMessage* message = [[NSURLSessionWebSocketMessage alloc] initWithString:text == nil ? @"" : text];
    __weak GdxTeaVMIOSWebSocket* weakSelf = self;
    [self.task sendMessage:message completionHandler:^(NSError* error) {
        GdxTeaVMIOSWebSocket* strongSelf = weakSelf;
        if(strongSelf == nil || error == nil) {
            return;
        }
        [strongSelf pushErrorEvent:teavm_ws_ios_describe_error(error, @"iOS websocket send failed.")];
    }];
    return YES;
}

- (BOOL)closeWithCode:(int)code reason:(NSString*)reason {
    if(self.task == nil) {
        [self emitCloseOnce:code reason:reason];
        return YES;
    }
    [self setStateLocked:WS_STATE_CLOSING];
    NSData* reasonData = reason == nil ? nil : [reason dataUsingEncoding:NSUTF8StringEncoding];
    [self.task cancelWithCloseCode:(NSURLSessionWebSocketCloseCode)code reason:reasonData];
    return YES;
}

- (void)destroy {
    [self setStateLocked:WS_STATE_CLOSED];
    [self.task cancelWithCloseCode:NSURLSessionWebSocketCloseCodeNormalClosure reason:nil];
    [self.session invalidateAndCancel];
    [self.lock lock];
    [self.events removeAllObjects];
    [self.lock unlock];
}

- (void)receiveNext {
    if([self currentState] >= WS_STATE_CLOSING) {
        return;
    }

    __weak GdxTeaVMIOSWebSocket* weakSelf = self;
    [self.task receiveMessageWithCompletionHandler:^(NSURLSessionWebSocketMessage* message, NSError* error) {
        GdxTeaVMIOSWebSocket* strongSelf = weakSelf;
        if(strongSelf == nil) {
            return;
        }
        if(error != nil) {
            if([strongSelf currentState] < WS_STATE_CLOSING) {
                NSString* errorText = teavm_ws_ios_describe_error(error, @"iOS websocket receive failed.");
                [strongSelf pushErrorEvent:errorText];
                [strongSelf emitCloseOnce:1006 reason:errorText];
            }
            return;
        }

        if(message.type == NSURLSessionWebSocketMessageTypeString) {
            [strongSelf pushTextEvent:message.string];
        }
        else {
            [strongSelf pushErrorEvent:@"Binary websocket frames are not implemented for TeaVM iOS yet."];
        }
        [strongSelf receiveNext];
    }];
}

- (void)URLSession:(NSURLSession*)session
              task:(NSURLSessionTask*)task
didReceiveChallenge:(NSURLAuthenticationChallenge*)challenge
 completionHandler:(void (^)(NSURLSessionAuthChallengeDisposition disposition, NSURLCredential* credential))completionHandler {
    (void)session;
    (void)task;
    if(self.insecureTls
            && [challenge.protectionSpace.authenticationMethod isEqualToString:NSURLAuthenticationMethodServerTrust]
            && challenge.protectionSpace.serverTrust != nil) {
        completionHandler(NSURLSessionAuthChallengeUseCredential,
                [NSURLCredential credentialForTrust:challenge.protectionSpace.serverTrust]);
        return;
    }
    completionHandler(NSURLSessionAuthChallengePerformDefaultHandling, nil);
}

- (void)URLSession:(NSURLSession*)session webSocketTask:(NSURLSessionWebSocketTask*)webSocketTask didOpenWithProtocol:(NSString*)protocol {
    (void)session;
    (void)webSocketTask;
    (void)protocol;
    [self setStateLocked:WS_STATE_OPEN];
    [self pushOpenEvent];
    [self receiveNext];
}

- (void)URLSession:(NSURLSession*)session webSocketTask:(NSURLSessionWebSocketTask*)webSocketTask didCloseWithCode:(NSURLSessionWebSocketCloseCode)closeCode reason:(NSData*)reason {
    (void)session;
    (void)webSocketTask;
    NSString* reasonText = @"";
    if(reason != nil && reason.length > 0) {
        NSString* decoded = [[NSString alloc] initWithData:reason encoding:NSUTF8StringEncoding];
        reasonText = decoded == nil ? @"" : decoded;
    }
    [self emitCloseOnce:(int)closeCode reason:reasonText];
}

@end

static GdxTeaVMIOSWebSocket* teavm_ws_ios_bridge(int64_t handle) {
    if(handle == 0) {
        return nil;
    }
    return (__bridge GdxTeaVMIOSWebSocket*)(void*)(intptr_t)handle;
}

int gdx_teavm_ws_ios_supported(void) {
    if(@available(iOS 13.0, macOS 10.15, *)) {
        return 1;
    }
    return 0;
}

int64_t gdx_teavm_ws_ios_create(const char* url, const char* protocols, int insecure_tls) {
    teavm_ws_ios_set_error(NULL);
    if(!gdx_teavm_ws_ios_supported()) {
        teavm_ws_ios_set_error("NSURLSessionWebSocketTask requires iOS 13 or newer.");
        return 0;
    }
    if(url == NULL || url[0] == '\0') {
        teavm_ws_ios_set_error("A websocket URL is required.");
        return 0;
    }

    @autoreleasepool {
        NSString* urlText = [teavm_ws_ios_string(url) stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
        NSURL* parsedURL = [NSURL URLWithString:urlText];
        NSString* scheme = parsedURL.scheme.lowercaseString;
        if(parsedURL == nil || !([scheme isEqualToString:@"ws"] || [scheme isEqualToString:@"wss"])) {
            teavm_ws_ios_set_error("Only ws:// and wss:// websocket URLs are supported.");
            return 0;
        }

        GdxTeaVMIOSWebSocket* socket = [[GdxTeaVMIOSWebSocket alloc] initWithURL:parsedURL
                                                                      protocols:teavm_ws_ios_protocols(protocols)
                                                                     insecureTls:insecure_tls != 0];
        if(socket == nil) {
            teavm_ws_ios_set_error("Failed to create iOS websocket.");
            return 0;
        }
        [socket start];
        return (int64_t)(intptr_t)(__bridge_retained void*)socket;
    }
}

int gdx_teavm_ws_ios_state(int64_t handle) {
    GdxTeaVMIOSWebSocket* socket = teavm_ws_ios_bridge(handle);
    return socket == nil ? WS_STATE_CLOSED : [socket currentState];
}

int gdx_teavm_ws_ios_send_text(int64_t handle, const char* text) {
    teavm_ws_ios_set_error(NULL);
    GdxTeaVMIOSWebSocket* socket = teavm_ws_ios_bridge(handle);
    if(socket == nil) {
        teavm_ws_ios_set_error("WebSocket handle is not open.");
        return 0;
    }
    @autoreleasepool {
        return [socket sendText:teavm_ws_ios_string(text)] ? 1 : 0;
    }
}

int gdx_teavm_ws_ios_close(int64_t handle, int code, const char* reason) {
    teavm_ws_ios_set_error(NULL);
    GdxTeaVMIOSWebSocket* socket = teavm_ws_ios_bridge(handle);
    if(socket == nil) {
        return 1;
    }
    @autoreleasepool {
        return [socket closeWithCode:code reason:teavm_ws_ios_string(reason)] ? 1 : 0;
    }
}

int gdx_teavm_ws_ios_poll_event(int64_t handle, int32_t* event_data, void* message_buffer, int message_buffer_capacity) {
    if(event_data == NULL) {
        return 0;
    }
    event_data[0] = WS_EVENT_NONE;
    event_data[1] = 0;
    event_data[2] = 0;
    event_data[3] = 0;

    GdxTeaVMIOSWebSocket* socket = teavm_ws_ios_bridge(handle);
    if(socket == nil) {
        return 0;
    }

    @autoreleasepool {
        GdxTeaVMIOSWebSocketEvent* event = [socket pollEvent];
        if(event == nil) {
            return 0;
        }

        const char* message = event.message == nil ? "" : event.message.UTF8String;
        int message_length = message == NULL ? 0 : (int)strlen(message);
        event_data[0] = event.type;
        event_data[1] = event.type == WS_EVENT_CLOSE ? event.data0 : message_length;
        event_data[2] = event.type == WS_EVENT_CLOSE ? message_length : event.data1;
        event_data[3] = 0;
        if(message_buffer != NULL && message_buffer_capacity > 0 && message != NULL) {
            teavm_ws_ios_copy_string((char*)message_buffer, message_buffer_capacity, message);
        }
        return 1;
    }
}

void gdx_teavm_ws_ios_destroy(int64_t handle) {
    if(handle == 0) {
        return;
    }
    @autoreleasepool {
        GdxTeaVMIOSWebSocket* socket = (__bridge_transfer GdxTeaVMIOSWebSocket*)(void*)(intptr_t)handle;
        [socket destroy];
    }
}

int gdx_teavm_ws_ios_last_error(void* target_buffer, int target_buffer_capacity) {
    pthread_mutex_lock(&g_teavm_ws_ios_error_lock);
    int copied = teavm_ws_ios_copy_string((char*)target_buffer, target_buffer_capacity, g_teavm_ws_ios_last_error);
    pthread_mutex_unlock(&g_teavm_ws_ios_error_lock);
    return copied;
}
