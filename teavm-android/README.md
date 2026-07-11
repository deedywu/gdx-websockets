# LibGDX Web Sockets for TeaVM Android

TeaVM Android backend for [LibGDX Web Sockets](../..). Use this module with `gdx-teavm` Android targets.

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

## Current limits

- text websocket frames are supported
- binary websocket frames are not implemented yet
