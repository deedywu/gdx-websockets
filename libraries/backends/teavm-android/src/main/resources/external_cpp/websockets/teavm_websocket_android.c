#include "teavm_websocket_android.h"

#include <pthread.h>
#include <stdint.h>
#include <stdio.h>
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

typedef struct TeavmWsEvent {
    int type;
    int data0;
    int data1;
    char* message;
    struct TeavmWsEvent* next;
} TeavmWsEvent;

typedef struct TeavmWsHandle {
    int64_t id;
    int state;
    int ref_count;
    int destroy_requested;
    pthread_mutex_t event_lock;
    TeavmWsEvent* event_head;
    TeavmWsEvent* event_tail;
    struct TeavmWsHandle* next;
} TeavmWsHandle;

static JavaVM* g_vm;
static jclass g_bridge_class;
static jmethodID g_create_socket_method;
static jmethodID g_send_text_method;
static jmethodID g_is_permessage_deflate_agreed_method;
static jmethodID g_get_agreed_extensions_method;
static jmethodID g_close_socket_method;
static jmethodID g_destroy_socket_method;
static jmethodID g_consume_last_error_method;
static jclass g_text_input_bridge_class;
static jmethodID g_show_text_input_method;
static jmethodID g_get_text_input_state_method;
static jmethodID g_consume_text_input_method;

static pthread_mutex_t g_handle_lock = PTHREAD_MUTEX_INITIALIZER;
static TeavmWsHandle* g_handles;
static int64_t g_next_handle_id = 1;
static char g_last_error[2048];

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    (void)reserved;
    g_vm = vm;
    return JNI_VERSION_1_6;
}

static void teavm_ws_set_error(const char* message) {
    if(message == NULL) {
        g_last_error[0] = '\0';
        return;
    }
    strncpy(g_last_error, message, sizeof(g_last_error) - 1);
    g_last_error[sizeof(g_last_error) - 1] = '\0';
}

static int teavm_ws_copy_string(char* target, int capacity, const char* value) {
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

static JNIEnv* teavm_ws_attach_env(int* did_attach) {
    if(did_attach != NULL) {
        *did_attach = 0;
    }
    if(g_vm == NULL) {
        teavm_ws_set_error("Android JVM is not available.");
        return NULL;
    }
    JNIEnv* env = NULL;
    jint state = (*g_vm)->GetEnv(g_vm, (void**)&env, JNI_VERSION_1_6);
    if(state == JNI_OK) {
        return env;
    }
    if(state != JNI_EDETACHED) {
        teavm_ws_set_error("Failed to query Android JNI environment.");
        return NULL;
    }
    if((*g_vm)->AttachCurrentThread(g_vm, &env, NULL) != JNI_OK) {
        teavm_ws_set_error("Failed to attach thread to Android JVM.");
        return NULL;
    }
    if(did_attach != NULL) {
        *did_attach = 1;
    }
    return env;
}

static void teavm_ws_detach_env(int did_attach) {
    if(did_attach && g_vm != NULL) {
        (*g_vm)->DetachCurrentThread(g_vm);
    }
}

static void teavm_ws_free_event(TeavmWsEvent* event) {
    if(event == NULL) {
        return;
    }
    if(event->message != NULL) {
        free(event->message);
    }
    free(event);
}

static void teavm_ws_free_handle(TeavmWsHandle* handle) {
    if(handle == NULL) {
        return;
    }
    pthread_mutex_lock(&handle->event_lock);
    TeavmWsEvent* current = handle->event_head;
    while(current != NULL) {
        TeavmWsEvent* next = current->next;
        teavm_ws_free_event(current);
        current = next;
    }
    handle->event_head = NULL;
    handle->event_tail = NULL;
    pthread_mutex_unlock(&handle->event_lock);
    pthread_mutex_destroy(&handle->event_lock);
    free(handle);
}

static TeavmWsHandle* teavm_ws_find_handle_locked(int64_t handle_id) {
    TeavmWsHandle* current = g_handles;
    while(current != NULL) {
        if(current->id == handle_id) {
            return current;
        }
        current = current->next;
    }
    return NULL;
}

static TeavmWsHandle* teavm_ws_acquire_handle(int64_t handle_id) {
    pthread_mutex_lock(&g_handle_lock);
    TeavmWsHandle* handle = teavm_ws_find_handle_locked(handle_id);
    if(handle != NULL && !handle->destroy_requested) {
        handle->ref_count++;
    }
    else {
        handle = NULL;
    }
    pthread_mutex_unlock(&g_handle_lock);
    return handle;
}

static void teavm_ws_release_handle(TeavmWsHandle* handle) {
    if(handle == NULL) {
        return;
    }
    int should_free = 0;
    pthread_mutex_lock(&g_handle_lock);
    if(handle->ref_count > 0) {
        handle->ref_count--;
    }
    if(handle->ref_count == 0 && handle->destroy_requested) {
        should_free = 1;
    }
    pthread_mutex_unlock(&g_handle_lock);
    if(should_free) {
        teavm_ws_free_handle(handle);
    }
}

static void teavm_ws_push_event(TeavmWsHandle* handle, int type, int data0, int data1, const char* message) {
    if(handle == NULL) {
        return;
    }
    TeavmWsEvent* event = (TeavmWsEvent*)calloc(1, sizeof(TeavmWsEvent));
    if(event == NULL) {
        return;
    }
    event->type = type;
    event->data0 = data0;
    event->data1 = data1;
    if(message != NULL) {
        size_t length = strlen(message);
        event->message = (char*)calloc(length + 1, sizeof(char));
        if(event->message != NULL) {
            memcpy(event->message, message, length);
            event->message[length] = '\0';
        }
    }

    pthread_mutex_lock(&handle->event_lock);
    if(handle->event_tail == NULL) {
        handle->event_head = event;
        handle->event_tail = event;
    }
    else {
        handle->event_tail->next = event;
        handle->event_tail = event;
    }
    pthread_mutex_unlock(&handle->event_lock);
}

static void teavm_ws_set_state(TeavmWsHandle* handle, int state) {
    if(handle != NULL) {
        handle->state = state;
    }
}

static void teavm_ws_set_error_from_exception(JNIEnv* env, const char* fallback) {
    if(env == NULL || !(*env)->ExceptionCheck(env)) {
        teavm_ws_set_error(fallback);
        return;
    }

    jthrowable exception = (*env)->ExceptionOccurred(env);
    (*env)->ExceptionClear(env);
    if(exception == NULL) {
        teavm_ws_set_error(fallback);
        return;
    }

    jclass throwable_class = (*env)->FindClass(env, "java/lang/Throwable");
    jmethodID to_string = throwable_class == NULL ? NULL
            : (*env)->GetMethodID(env, throwable_class, "toString", "()Ljava/lang/String;");
    if(to_string == NULL) {
        teavm_ws_set_error(fallback);
        (*env)->DeleteLocalRef(env, exception);
        if(throwable_class != NULL) {
            (*env)->DeleteLocalRef(env, throwable_class);
        }
        return;
    }

    jstring text = (jstring)(*env)->CallObjectMethod(env, exception, to_string);
    if((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
        teavm_ws_set_error(fallback);
    }
    else if(text != NULL) {
        const char* chars = (*env)->GetStringUTFChars(env, text, NULL);
        teavm_ws_set_error(chars == NULL || chars[0] == '\0' ? fallback : chars);
        if(chars != NULL) {
            (*env)->ReleaseStringUTFChars(env, text, chars);
        }
        (*env)->DeleteLocalRef(env, text);
    }
    else {
        teavm_ws_set_error(fallback);
    }

    (*env)->DeleteLocalRef(env, exception);
    if(throwable_class != NULL) {
        (*env)->DeleteLocalRef(env, throwable_class);
    }
}

static int teavm_ws_copy_last_error_from_java(JNIEnv* env) {
    if(env == NULL || g_bridge_class == NULL || g_consume_last_error_method == NULL) {
        return 0;
    }
    jstring text = (jstring)(*env)->CallStaticObjectMethod(env, g_bridge_class, g_consume_last_error_method);
    if((*env)->ExceptionCheck(env)) {
        teavm_ws_set_error_from_exception(env, "Android websocket bridge error.");
        return 1;
    }
    if(text == NULL) {
        return 0;
    }
    const char* chars = (*env)->GetStringUTFChars(env, text, NULL);
    if(chars != NULL && chars[0] != '\0') {
        teavm_ws_set_error(chars);
        (*env)->ReleaseStringUTFChars(env, text, chars);
        (*env)->DeleteLocalRef(env, text);
        return 1;
    }
    if(chars != NULL) {
        (*env)->ReleaseStringUTFChars(env, text, chars);
    }
    (*env)->DeleteLocalRef(env, text);
    return 0;
}

static int teavm_ws_ensure_bridge(JNIEnv* env) {
    if(env == NULL) {
        return 0;
    }
    if(g_bridge_class != NULL) {
        return 1;
    }

    jclass local_class = (*env)->FindClass(env, "com/github/czyzby/websocket/android/WebSocketsAndroidBridge");
    if(local_class == NULL) {
        teavm_ws_set_error_from_exception(env, "Unable to find WebSocketsAndroidBridge.");
        return 0;
    }

    g_bridge_class = (*env)->NewGlobalRef(env, local_class);
    (*env)->DeleteLocalRef(env, local_class);
    if(g_bridge_class == NULL) {
        teavm_ws_set_error("Unable to retain WebSocketsAndroidBridge class.");
        return 0;
    }

    g_create_socket_method = (*env)->GetStaticMethodID(env, g_bridge_class, "createSocket",
            "(JLjava/lang/String;Ljava/lang/String;ZZ)Z");
    g_send_text_method = (*env)->GetStaticMethodID(env, g_bridge_class, "sendText", "(JLjava/lang/String;)Z");
    g_is_permessage_deflate_agreed_method = (*env)->GetStaticMethodID(env, g_bridge_class,
            "isPerMessageDeflateAgreed", "(J)Z");
    g_get_agreed_extensions_method = (*env)->GetStaticMethodID(env, g_bridge_class,
            "getAgreedExtensionsDescription", "(J)Ljava/lang/String;");
    g_close_socket_method = (*env)->GetStaticMethodID(env, g_bridge_class, "closeSocket", "(JILjava/lang/String;)Z");
    g_destroy_socket_method = (*env)->GetStaticMethodID(env, g_bridge_class, "destroySocket", "(J)V");
    g_consume_last_error_method = (*env)->GetStaticMethodID(env, g_bridge_class, "consumeLastError", "()Ljava/lang/String;");

    if(g_create_socket_method == NULL
            || g_send_text_method == NULL
            || g_is_permessage_deflate_agreed_method == NULL
            || g_get_agreed_extensions_method == NULL
            || g_close_socket_method == NULL
            || g_destroy_socket_method == NULL
            || g_consume_last_error_method == NULL) {
        teavm_ws_set_error_from_exception(env, "Unable to resolve Android websocket bridge methods.");
        return 0;
    }
    return 1;
}

static int teavm_ws_ensure_text_input_bridge(JNIEnv* env) {
    if(env == NULL) {
        return 0;
    }
    if(g_text_input_bridge_class != NULL) {
        return 1;
    }

    jclass local_class = (*env)->FindClass(env,
            "com/github/czyzby/websocket/examples/teavm/android/TeaVMAndroidTextInputBridge");
    if(local_class == NULL) {
        teavm_ws_set_error_from_exception(env, "Unable to find TeaVMAndroidTextInputBridge.");
        return 0;
    }

    g_text_input_bridge_class = (*env)->NewGlobalRef(env, local_class);
    (*env)->DeleteLocalRef(env, local_class);
    if(g_text_input_bridge_class == NULL) {
        teavm_ws_set_error("Unable to retain TeaVMAndroidTextInputBridge class.");
        return 0;
    }

    g_show_text_input_method = (*env)->GetStaticMethodID(env, g_text_input_bridge_class, "showTextInput",
            "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z");
    g_get_text_input_state_method = (*env)->GetStaticMethodID(env, g_text_input_bridge_class, "getTextInputState",
            "(J)I");
    g_consume_text_input_method = (*env)->GetStaticMethodID(env, g_text_input_bridge_class, "consumeTextInput",
            "(J)Ljava/lang/String;");

    if(g_show_text_input_method == NULL
            || g_get_text_input_state_method == NULL
            || g_consume_text_input_method == NULL) {
        teavm_ws_set_error_from_exception(env, "Unable to resolve Android text input bridge methods.");
        return 0;
    }
    return 1;
}

int gdx_teavm_ws_android_supported(void) {
    teavm_ws_set_error(NULL);
    int did_attach = 0;
    JNIEnv* env = teavm_ws_attach_env(&did_attach);
    int supported = teavm_ws_ensure_bridge(env);
    teavm_ws_detach_env(did_attach);
    return supported;
}

int64_t gdx_teavm_ws_android_create(const char* url, const char* protocols, int use_per_message_deflate, int insecure_tls) {
    teavm_ws_set_error(NULL);
    if(url == NULL || url[0] == '\0') {
        teavm_ws_set_error("A websocket URL is required.");
        return 0;
    }

    int did_attach = 0;
    JNIEnv* env = teavm_ws_attach_env(&did_attach);
    if(!teavm_ws_ensure_bridge(env)) {
        teavm_ws_detach_env(did_attach);
        return 0;
    }

    TeavmWsHandle* handle = (TeavmWsHandle*)calloc(1, sizeof(TeavmWsHandle));
    if(handle == NULL) {
        teavm_ws_set_error("Failed to allocate websocket handle.");
        teavm_ws_detach_env(did_attach);
        return 0;
    }
    pthread_mutex_init(&handle->event_lock, NULL);
    handle->state = WS_STATE_CONNECTING;

    pthread_mutex_lock(&g_handle_lock);
    handle->id = g_next_handle_id++;
    handle->next = g_handles;
    g_handles = handle;
    pthread_mutex_unlock(&g_handle_lock);

    jstring url_value = (*env)->NewStringUTF(env, url);
    jstring protocols_value = protocols == NULL || protocols[0] == '\0' ? NULL : (*env)->NewStringUTF(env, protocols);
    jboolean created = (*env)->CallStaticBooleanMethod(env, g_bridge_class, g_create_socket_method, (jlong)handle->id,
            url_value, protocols_value, (jboolean)(use_per_message_deflate != 0), (jboolean)(insecure_tls != 0));
    if(url_value != NULL) {
        (*env)->DeleteLocalRef(env, url_value);
    }
    if(protocols_value != NULL) {
        (*env)->DeleteLocalRef(env, protocols_value);
    }

    if((*env)->ExceptionCheck(env)) {
        teavm_ws_set_error_from_exception(env, "Android websocket creation failed.");
        created = JNI_FALSE;
    }
    else if(created == JNI_FALSE && !teavm_ws_copy_last_error_from_java(env)) {
        teavm_ws_set_error("Android websocket creation failed.");
    }

    teavm_ws_detach_env(did_attach);
    if(created == JNI_TRUE) {
        return handle->id;
    }

    pthread_mutex_lock(&g_handle_lock);
    if(g_handles == handle) {
        g_handles = handle->next;
    }
    else {
        TeavmWsHandle* current = g_handles;
        while(current != NULL && current->next != handle) {
            current = current->next;
        }
        if(current != NULL) {
            current->next = handle->next;
        }
    }
    pthread_mutex_unlock(&g_handle_lock);
    teavm_ws_free_handle(handle);
    return 0;
}

int gdx_teavm_ws_android_state(int64_t handle_id) {
    TeavmWsHandle* handle = teavm_ws_acquire_handle(handle_id);
    if(handle == NULL) {
        return WS_STATE_CLOSED;
    }
    int state = handle->state;
    teavm_ws_release_handle(handle);
    return state;
}

int gdx_teavm_ws_android_send_text(int64_t handle_id, const char* text) {
    teavm_ws_set_error(NULL);
    TeavmWsHandle* handle = teavm_ws_acquire_handle(handle_id);
    if(handle == NULL) {
        teavm_ws_set_error("WebSocket handle is not open.");
        return 0;
    }

    int did_attach = 0;
    JNIEnv* env = teavm_ws_attach_env(&did_attach);
    if(!teavm_ws_ensure_bridge(env)) {
        teavm_ws_detach_env(did_attach);
        teavm_ws_release_handle(handle);
        return 0;
    }

    if(text == NULL) {
        text = "";
    }
    jstring text_value = (*env)->NewStringUTF(env, text);
    jboolean sent = (*env)->CallStaticBooleanMethod(env, g_bridge_class, g_send_text_method, (jlong)handle_id, text_value);
    if(text_value != NULL) {
        (*env)->DeleteLocalRef(env, text_value);
    }

    if((*env)->ExceptionCheck(env)) {
        teavm_ws_set_error_from_exception(env, "Android websocket send failed.");
        sent = JNI_FALSE;
    }
    else if(sent == JNI_FALSE && !teavm_ws_copy_last_error_from_java(env)) {
        teavm_ws_set_error("Android websocket send failed.");
    }

    teavm_ws_detach_env(did_attach);
    teavm_ws_release_handle(handle);
    return sent == JNI_TRUE;
}

int gdx_teavm_ws_android_permessage_deflate_agreed(int64_t handle_id) {
    TeavmWsHandle* handle = teavm_ws_acquire_handle(handle_id);
    if(handle == NULL) {
        return 0;
    }

    int did_attach = 0;
    JNIEnv* env = teavm_ws_attach_env(&did_attach);
    if(!teavm_ws_ensure_bridge(env)) {
        teavm_ws_detach_env(did_attach);
        teavm_ws_release_handle(handle);
        return 0;
    }

    jboolean agreed = (*env)->CallStaticBooleanMethod(env, g_bridge_class, g_is_permessage_deflate_agreed_method,
            (jlong)handle_id);
    if((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
        agreed = JNI_FALSE;
    }

    teavm_ws_detach_env(did_attach);
    teavm_ws_release_handle(handle);
    return agreed == JNI_TRUE;
}

int gdx_teavm_ws_android_agreed_extensions(int64_t handle_id, void* target_buffer, int target_buffer_capacity) {
    TeavmWsHandle* handle = teavm_ws_acquire_handle(handle_id);
    if(handle == NULL) {
        return teavm_ws_copy_string((char*)target_buffer, target_buffer_capacity, "none");
    }

    int did_attach = 0;
    JNIEnv* env = teavm_ws_attach_env(&did_attach);
    if(!teavm_ws_ensure_bridge(env)) {
        teavm_ws_detach_env(did_attach);
        teavm_ws_release_handle(handle);
        return teavm_ws_copy_string((char*)target_buffer, target_buffer_capacity, "none");
    }

    jstring text = (jstring)(*env)->CallStaticObjectMethod(env, g_bridge_class, g_get_agreed_extensions_method,
            (jlong)handle_id);
    if((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
        text = NULL;
    }

    int length = 0;
    if(text != NULL) {
        const char* chars = (*env)->GetStringUTFChars(env, text, NULL);
        if(chars != NULL) {
            length = teavm_ws_copy_string((char*)target_buffer, target_buffer_capacity, chars);
            (*env)->ReleaseStringUTFChars(env, text, chars);
        }
        (*env)->DeleteLocalRef(env, text);
    }
    if(length <= 0) {
        length = teavm_ws_copy_string((char*)target_buffer, target_buffer_capacity, "none");
    }

    teavm_ws_detach_env(did_attach);
    teavm_ws_release_handle(handle);
    return length;
}

int gdx_teavm_ws_android_text_input_show(int64_t request_id, const char* title, const char* text, const char* hint) {
    teavm_ws_set_error(NULL);
    int did_attach = 0;
    JNIEnv* env = teavm_ws_attach_env(&did_attach);
    if(!teavm_ws_ensure_text_input_bridge(env)) {
        teavm_ws_detach_env(did_attach);
        return 0;
    }

    jstring title_value = (*env)->NewStringUTF(env, title == NULL ? "" : title);
    jstring text_value = (*env)->NewStringUTF(env, text == NULL ? "" : text);
    jstring hint_value = (*env)->NewStringUTF(env, hint == NULL ? "" : hint);
    jboolean shown = (*env)->CallStaticBooleanMethod(env, g_text_input_bridge_class, g_show_text_input_method,
            (jlong)request_id, title_value, text_value, hint_value);
    if(title_value != NULL) {
        (*env)->DeleteLocalRef(env, title_value);
    }
    if(text_value != NULL) {
        (*env)->DeleteLocalRef(env, text_value);
    }
    if(hint_value != NULL) {
        (*env)->DeleteLocalRef(env, hint_value);
    }

    if((*env)->ExceptionCheck(env)) {
        teavm_ws_set_error_from_exception(env, "Android text input dialog failed.");
        shown = JNI_FALSE;
    }

    teavm_ws_detach_env(did_attach);
    return shown == JNI_TRUE;
}

int gdx_teavm_ws_android_text_input_state(int64_t request_id) {
    int did_attach = 0;
    JNIEnv* env = teavm_ws_attach_env(&did_attach);
    if(!teavm_ws_ensure_text_input_bridge(env)) {
        teavm_ws_detach_env(did_attach);
        return 3;
    }

    jint state = (*env)->CallStaticIntMethod(env, g_text_input_bridge_class, g_get_text_input_state_method,
            (jlong)request_id);
    if((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
        state = 3;
    }

    teavm_ws_detach_env(did_attach);
    return (int)state;
}

int gdx_teavm_ws_android_text_input_result(int64_t request_id, void* target_buffer, int target_buffer_capacity) {
    int did_attach = 0;
    JNIEnv* env = teavm_ws_attach_env(&did_attach);
    if(!teavm_ws_ensure_text_input_bridge(env)) {
        teavm_ws_detach_env(did_attach);
        return teavm_ws_copy_string((char*)target_buffer, target_buffer_capacity, "");
    }

    jstring text = (jstring)(*env)->CallStaticObjectMethod(env, g_text_input_bridge_class,
            g_consume_text_input_method, (jlong)request_id);
    if((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
        text = NULL;
    }

    int length = 0;
    if(text != NULL) {
        const char* chars = (*env)->GetStringUTFChars(env, text, NULL);
        if(chars != NULL) {
            length = teavm_ws_copy_string((char*)target_buffer, target_buffer_capacity, chars);
            (*env)->ReleaseStringUTFChars(env, text, chars);
        }
        (*env)->DeleteLocalRef(env, text);
    }

    teavm_ws_detach_env(did_attach);
    return length;
}

int gdx_teavm_ws_android_close(int64_t handle_id, int code, const char* reason) {
    teavm_ws_set_error(NULL);
    TeavmWsHandle* handle = teavm_ws_acquire_handle(handle_id);
    if(handle == NULL) {
        return 1;
    }
    teavm_ws_set_state(handle, WS_STATE_CLOSING);

    int did_attach = 0;
    JNIEnv* env = teavm_ws_attach_env(&did_attach);
    if(!teavm_ws_ensure_bridge(env)) {
        teavm_ws_detach_env(did_attach);
        teavm_ws_release_handle(handle);
        return 0;
    }

    if(reason == NULL) {
        reason = "";
    }
    jstring reason_value = (*env)->NewStringUTF(env, reason);
    jboolean closed = (*env)->CallStaticBooleanMethod(env, g_bridge_class, g_close_socket_method, (jlong)handle_id, (jint)code, reason_value);
    if(reason_value != NULL) {
        (*env)->DeleteLocalRef(env, reason_value);
    }

    if((*env)->ExceptionCheck(env)) {
        teavm_ws_set_error_from_exception(env, "Android websocket close failed.");
        closed = JNI_FALSE;
    }
    else if(closed == JNI_FALSE && !teavm_ws_copy_last_error_from_java(env)) {
        teavm_ws_set_error("Android websocket close failed.");
    }

    teavm_ws_detach_env(did_attach);
    teavm_ws_release_handle(handle);
    return closed == JNI_TRUE;
}

int gdx_teavm_ws_android_poll_event(int64_t handle_id, int32_t* event_data, void* message_buffer, int message_buffer_capacity) {
    if(event_data == NULL) {
        return 0;
    }
    event_data[0] = WS_EVENT_NONE;
    event_data[1] = 0;
    event_data[2] = 0;
    event_data[3] = 0;

    TeavmWsHandle* handle = teavm_ws_acquire_handle(handle_id);
    if(handle == NULL) {
        return 0;
    }

    pthread_mutex_lock(&handle->event_lock);
    TeavmWsEvent* event = handle->event_head;
    if(event == NULL) {
        pthread_mutex_unlock(&handle->event_lock);
        teavm_ws_release_handle(handle);
        return 0;
    }
    handle->event_head = event->next;
    if(handle->event_head == NULL) {
        handle->event_tail = NULL;
    }
    pthread_mutex_unlock(&handle->event_lock);

    event_data[0] = event->type;
    event_data[1] = event->data0;
    event_data[2] = event->data1;
    if(message_buffer != NULL && message_buffer_capacity > 0 && event->message != NULL) {
        teavm_ws_copy_string((char*)message_buffer, message_buffer_capacity, event->message);
    }
    teavm_ws_free_event(event);
    teavm_ws_release_handle(handle);
    return 1;
}

void gdx_teavm_ws_android_destroy(int64_t handle_id) {
    int did_attach = 0;
    JNIEnv* env = teavm_ws_attach_env(&did_attach);
    if(teavm_ws_ensure_bridge(env)) {
        (*env)->CallStaticVoidMethod(env, g_bridge_class, g_destroy_socket_method, (jlong)handle_id);
        if((*env)->ExceptionCheck(env)) {
            teavm_ws_set_error_from_exception(env, "Android websocket destroy failed.");
        }
    }
    teavm_ws_detach_env(did_attach);

    TeavmWsHandle* handle = NULL;
    int should_free = 0;
    pthread_mutex_lock(&g_handle_lock);
    handle = g_handles;
    TeavmWsHandle* previous = NULL;
    while(handle != NULL && handle->id != handle_id) {
        previous = handle;
        handle = handle->next;
    }
    if(handle != NULL) {
        handle->destroy_requested = 1;
        if(previous == NULL) {
            g_handles = handle->next;
        }
        else {
            previous->next = handle->next;
        }
        handle->next = NULL;
        should_free = handle->ref_count == 0;
    }
    pthread_mutex_unlock(&g_handle_lock);

    if(should_free) {
        teavm_ws_free_handle(handle);
    }
}

int gdx_teavm_ws_android_last_error(void* target_buffer, int target_buffer_capacity) {
    return teavm_ws_copy_string((char*)target_buffer, target_buffer_capacity, g_last_error);
}

JNIEXPORT void JNICALL Java_com_github_czyzby_websocket_android_WebSocketsAndroidBridge_nativeOnStateChanged(
        JNIEnv* env, jclass clazz, jlong handle_id, jint state) {
    (void)env;
    (void)clazz;
    TeavmWsHandle* handle = teavm_ws_acquire_handle((int64_t)handle_id);
    teavm_ws_set_state(handle, (int)state);
    teavm_ws_release_handle(handle);
}

JNIEXPORT void JNICALL Java_com_github_czyzby_websocket_android_WebSocketsAndroidBridge_nativeOnOpen(
        JNIEnv* env, jclass clazz, jlong handle_id) {
    (void)env;
    (void)clazz;
    TeavmWsHandle* handle = teavm_ws_acquire_handle((int64_t)handle_id);
    if(handle == NULL) {
        return;
    }
    teavm_ws_set_state(handle, WS_STATE_OPEN);
    teavm_ws_push_event(handle, WS_EVENT_OPEN, 0, 0, NULL);
    teavm_ws_release_handle(handle);
}

JNIEXPORT void JNICALL Java_com_github_czyzby_websocket_android_WebSocketsAndroidBridge_nativeOnMessageText(
        JNIEnv* env, jclass clazz, jlong handle_id, jstring message) {
    (void)clazz;
    TeavmWsHandle* handle = teavm_ws_acquire_handle((int64_t)handle_id);
    if(handle == NULL) {
        return;
    }
    const char* chars = message == NULL ? NULL : (*env)->GetStringUTFChars(env, message, NULL);
    teavm_ws_push_event(handle, WS_EVENT_MESSAGE_TEXT, chars == NULL ? 0 : (int)strlen(chars), 0, chars);
    if(message != NULL && chars != NULL) {
        (*env)->ReleaseStringUTFChars(env, message, chars);
    }
    teavm_ws_release_handle(handle);
}

JNIEXPORT void JNICALL Java_com_github_czyzby_websocket_android_WebSocketsAndroidBridge_nativeOnError(
        JNIEnv* env, jclass clazz, jlong handle_id, jstring message) {
    (void)clazz;
    TeavmWsHandle* handle = teavm_ws_acquire_handle((int64_t)handle_id);
    if(handle == NULL) {
        return;
    }
    const char* chars = message == NULL ? NULL : (*env)->GetStringUTFChars(env, message, NULL);
    teavm_ws_push_event(handle, WS_EVENT_ERROR, chars == NULL ? 0 : (int)strlen(chars), 0, chars);
    if(message != NULL && chars != NULL) {
        (*env)->ReleaseStringUTFChars(env, message, chars);
    }
    teavm_ws_release_handle(handle);
}

JNIEXPORT void JNICALL Java_com_github_czyzby_websocket_android_WebSocketsAndroidBridge_nativeOnClose(
        JNIEnv* env, jclass clazz, jlong handle_id, jint close_code, jstring reason) {
    (void)clazz;
    TeavmWsHandle* handle = teavm_ws_acquire_handle((int64_t)handle_id);
    if(handle == NULL) {
        return;
    }
    const char* chars = reason == NULL ? NULL : (*env)->GetStringUTFChars(env, reason, NULL);
    teavm_ws_set_state(handle, WS_STATE_CLOSED);
    teavm_ws_push_event(handle, WS_EVENT_CLOSE, (int)close_code, chars == NULL ? 0 : (int)strlen(chars), chars);
    if(reason != NULL && chars != NULL) {
        (*env)->ReleaseStringUTFChars(env, reason, chars);
    }
    teavm_ws_release_handle(handle);
}
