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

## Current limits

- text websocket frames are supported
- binary websocket frames are not implemented yet
