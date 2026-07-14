# LibGDX Web Sockets for TeaVM iOS

TeaVM iOS backend for [LibGDX Web Sockets](../../..). Use this module with experimental `gdx-teavm` iOS targets.

## Dependencies

Add JitPack to your repositories:

```gradle
maven { url "https://jitpack.io" }
```

For a `gdx-teavm` iOS application module:

```gradle
dependencies {
    implementation "com.github.deedywu.gdx-websockets:teavm-ios:$wsVersion"
}
```

This artifact publishes:

- the TeaVM-side `IOSWebSockets` factory
- Objective-C/C bridge resources under `external_cpp/`
- an iOS native implementation backed by `NSURLSessionWebSocketTask`

`gdx-teavm` copies the bundled `external_cpp` resources into the generated iOS native project and includes them from the generated `app_include.c` file.

## Initialization

Call `IOSWebSockets.initiate()` before creating sockets in the TeaVM iOS launcher:

```java
public static void main(String[] args) {
    IOSWebSockets.initiate();
    new IOSApplication(new MyApplicationListener(), config);
}
```

## Local WSS testing

For local development only, `WebSockets.newInsecureSocket(url)` creates a `wss://` socket that trusts every TLS
certificate and disables hostname verification after `IOSWebSockets.initiate()` has registered this backend. This is
useful for simulator or device tests against a local server with a self-signed certificate:

```java
WebSocket socket = WebSockets.newInsecureSocket("wss://127.0.0.1:8787/");
```

Do not use this helper for production traffic. On TeaVM iOS, the helper accepts `NSURLAuthenticationMethodServerTrust`
challenges for that socket only; normal `WebSockets.newSocket(url)` connections continue to use the default iOS trust
policy.

## Current limits

- text websocket frames are supported
- binary websocket frames are not implemented yet
