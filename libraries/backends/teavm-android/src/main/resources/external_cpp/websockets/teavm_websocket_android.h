#ifndef TEAVM_WEBSOCKET_ANDROID_H
#define TEAVM_WEBSOCKET_ANDROID_H

#include <jni.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

int gdx_teavm_ws_android_supported(void);
int64_t gdx_teavm_ws_android_create(const char* url, int use_per_message_deflate, int insecure_tls);
int gdx_teavm_ws_android_state(int64_t handle);
int gdx_teavm_ws_android_send_text(int64_t handle, const char* text);
int gdx_teavm_ws_android_permessage_deflate_agreed(int64_t handle);
int gdx_teavm_ws_android_agreed_extensions(int64_t handle, void* target_buffer, int target_buffer_capacity);
int gdx_teavm_ws_android_text_input_show(int64_t request_id, const char* title, const char* text, const char* hint);
int gdx_teavm_ws_android_text_input_state(int64_t request_id);
int gdx_teavm_ws_android_text_input_result(int64_t request_id, void* target_buffer, int target_buffer_capacity);
int gdx_teavm_ws_android_close(int64_t handle, int code, const char* reason);
int gdx_teavm_ws_android_poll_event(int64_t handle, int32_t* event_data, void* message_buffer, int message_buffer_capacity);
void gdx_teavm_ws_android_destroy(int64_t handle);
int gdx_teavm_ws_android_last_error(void* target_buffer, int target_buffer_capacity);

JNIEXPORT void JNICALL Java_com_github_czyzby_websocket_android_WebSocketsAndroidBridge_nativeOnStateChanged(
        JNIEnv* env, jclass clazz, jlong handle, jint state);
JNIEXPORT void JNICALL Java_com_github_czyzby_websocket_android_WebSocketsAndroidBridge_nativeOnOpen(
        JNIEnv* env, jclass clazz, jlong handle);
JNIEXPORT void JNICALL Java_com_github_czyzby_websocket_android_WebSocketsAndroidBridge_nativeOnMessageText(
        JNIEnv* env, jclass clazz, jlong handle, jstring message);
JNIEXPORT void JNICALL Java_com_github_czyzby_websocket_android_WebSocketsAndroidBridge_nativeOnError(
        JNIEnv* env, jclass clazz, jlong handle, jstring message);
JNIEXPORT void JNICALL Java_com_github_czyzby_websocket_android_WebSocketsAndroidBridge_nativeOnClose(
        JNIEnv* env, jclass clazz, jlong handle, jint closeCode, jstring reason);

#ifdef __cplusplus
}
#endif

#endif
