# LibGDX Web Sockets for TeaVM Android

TeaVM Android backend for [LibGDX Web Sockets](../../..). Use this module with `gdx-teavm` Android targets.

## Dependencies

Add JitPack to your repositories:

```gradle
maven { url "https://jitpack.io" }
```

For a `gdx-teavm` Android application module:

```gradle
dependencies {
    implementation "com.github.deedywu.gdx-websockets:teavm-android:$wsVersion"
    teavm "com.github.deedywu.gdx-websockets:teavm-android:$wsVersion"
}
```

This artifact publishes:

- the TeaVM-side `AndroidWebSockets` factory
- JNI/C bridge resources under `external_cpp/`
- the Android runtime helper class that delegates to `nv-websocket-client`

`gdx-teavm` copies the bundled `external_cpp` resources into the generated Android native project and compiles them into the final shared library.

## Initialization

Call `AndroidWebSockets.initiate()` before creating sockets in the TeaVM Android launcher:

```java
public static void main(String[] args) {
    AndroidWebSockets.initiate();
    new AndroidApplication(new MyApplicationListener(), config);
}
```

## Local WSS testing

For local development only, `WebSockets.newInsecureSocket(url)` creates a `wss://` socket that trusts every TLS
certificate and disables hostname verification after `AndroidWebSockets.initiate()` has registered this backend. This is
useful when testing against a local IP address, self-signed certificate, or temporary certificate whose hostname does
not match the address:

```java
WebSocket socket = WebSockets.newInsecureSocket("wss://192.168.2.1:8080/");
```

Do not use this helper for production traffic. Production clients should use normal TLS validation. Backends that
cannot override TLS validation, such as browser-backed websocket implementations, reject `WebSockets.newInsecureSocket`.

## Current limits

- text websocket frames are supported
- binary websocket frames are not implemented yet
