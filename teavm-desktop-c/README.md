# LibGDX Web Sockets for TeaVM Desktop C

TeaVM C/GLFW backend for [LibGDX Web Sockets](../..). Use this module with `gdx-teavm` desktop-c targets.

## Dependencies

Add JitPack to your repositories:

```gradle
maven { url "https://jitpack.io" }
```

Gradle dependency for a `gdx-teavm` desktop-c project:

```gradle
implementation "com.github.deedywu.gdx-websockets:teavm-desktop-c:$wsVersion"
```

The module publishes a normal Java jar with TeaVM C resources under `external_cpp/`. `gdx-teavm` discovers those resources through `META-INF/gdx-teavm.properties`, copies them into the generated C project, and compiles them into the final native executable.

This artifact does not bundle native binaries. The final application build owns the native executable and links the copied C sources through the `gdx-teavm` GLFW backend.

## Initialization

Call `GLFWWebSockets.initiate()` before creating sockets:

```java
public static void main(String[] args) {
    GLFWWebSockets.initiate();
    new GLFWApplication(new MyApplicationListener(), config);
}
```

Windows uses WinHTTP. Linux and macOS use `libcurl` loaded at runtime; make sure a `libcurl` build with `ws` and `wss` support is available on those platforms.

## Linux and macOS `libcurl`

Some Linux and macOS `libcurl` builds expose the WebSocket API symbols but do not enable the `ws` and `wss` protocols. In that case connecting to a `wss://` endpoint can fail with a protocol-not-supported error.

This module includes helper scripts that build a local `libcurl` runtime with WebSocket support:

```shell
teavm-desktop-c/scripts/build-linux-libcurl-wss.sh
teavm-desktop-c/scripts/build-mac-libcurl-wss.sh
```

The scripts install the runtime under their local `build/` directories:

```text
teavm-desktop-c/scripts/build/libcurl-wss/install/lib/libcurl.so.4
teavm-desktop-c/scripts/build/libcurl-wss-macos/install/lib/libcurl.4.dylib
```

Pass the generated runtime to your `gdx-teavm` desktop-c build:

```shell
./gradlew \
  -PgdxTeaVMLinuxCurlPath=/absolute/path/to/libcurl.so.4 \
  :your-desktop-c-project:your_run_task

./gradlew \
  -PgdxTeaVMMacCurlPath=/absolute/path/to/libcurl.4.dylib \
  :your-desktop-c-project:your_run_task
```

If you use the manual `TeaBuilder` API, the default build helper can generate/build/run a `gdx-teavm` GLFW executable with websocket support:

```java
public static void main(String[] args) throws IOException {
    GLFWWebSocketsBuild.build(MyGLFWLauncher.class, args);
}
```

Defaults:

- output root: `build/dist`
- release path: `build/dist/c/release`
- output name: derived from the launcher class name
- build type: `Debug`
- action: `generate`
- optimization: `FULL`
- min heap: `64 MB`
- max heap: `512 MB`
- direct buffers: `64 MB`

The helper accepts the same small argument shape used by the examples:

```text
[Debug|Release] [generate|build|run] [console]
```

You can override the defaults with the fluent builder:

```java
GLFWWebSocketsBuild.builder(MyGLFWLauncher.class)
        .setOutputName("my-game")
        .setOutputRoot(new File("build/dist"))
        .setMaxHeapSize(1024 * 1024 * 1024)
        .build(args);
```

The TeaVM GLFW backend also honors `GDX_TEAVM_LIBCURL_PATH` at runtime if you need to point an executable at a specific `libcurl` without copying it next to the executable.
