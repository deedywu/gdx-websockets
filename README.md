# libGDX Web Sockets

[![JitPack](https://jitpack.io/v/deedywu/gdx-websockets.svg)](https://jitpack.io/#deedywu/gdx-websockets)

Maintained fork of [MrStahlfelge/gdx-websockets](https://github.com/MrStahlfelge/gdx-websockets), which itself forked [czyzby's websockets](https://github.com/czyzby/gdx-lml/tree/master/websocket).

This fork preserves SimonIT's TeaVM implementation, keeps the existing API shape for libGDX projects, and serves as the home for ongoing compatibility updates.

Default libGDX `Net` API provides only TCP sockets and HTTP requests. This library aims to add client-side web sockets support.
It works on all platforms targeted by libGDX.

## Fork goals
- Publish JitPack artifacts from `deedywu/gdx-websockets`
- Keep TeaVM support available and maintained
- Stay close to the original API so existing libGDX projects can migrate with minimal changes

## Project layout
- `libraries/core`: shared websocket API and default serializers
- `libraries/backends/*`: platform-specific websocket implementations
- `libraries/extensions/*`: optional add-ons such as manual serialization
- `examples/*`: runnable sample projects

## Dependencies
If you do not already have it, add JitPack to the repositories in your root `build.gradle` file:

        maven { url "https://jitpack.io" }

Use `com.github.deedywu.gdx-websockets` as the dependency group and the module name as the artifact ID.

`Gradle` dependency (for libGDX core project):
```
         implementation "com.github.deedywu.gdx-websockets:core:$wsVersion"
```

GWT module:
```
         <inherits name='com.github.czyzby.websocket.GdxWebSocket' />
```

Desktop/Android/iOS:
```
         implementation "com.github.deedywu.gdx-websockets:common:$wsVersion"
```

(based on [nv-websocket-client](https://github.com/TakahikoKawasaki/nv-websocket-client))

### GWT (Web)
`Gradle` dependency for libGDX html project
```
        implementation "com.github.deedywu.gdx-websockets:core:$wsVersion:sources"
        implementation "com.github.deedywu.gdx-websockets:html:$wsVersion"
        implementation "com.github.deedywu.gdx-websockets:html:$wsVersion:sources"
```

GWT module (GdxDefinition.gwt.xml):
```
        <inherits name='com.github.czyzby.websocket.GdxWebSocketGwt' />
```

### TeaVM Web

This module is based on the TeaVM work originally contributed by SimonIT.

`Gradle` dependency for libGDX TeaVM web project
```
        implementation "com.github.deedywu.gdx-websockets:teavm-web:$wsVersion"
```

### TeaVM Desktop C

Use this module with `gdx-teavm` desktop-c targets. It publishes Java classes plus `external_cpp` resources that `gdx-teavm` compiles into the final native executable. It does not bundle a prebuilt native binary.

`Gradle` dependency for a `gdx-teavm` desktop-c project
```
        implementation "com.github.deedywu.gdx-websockets:teavm-desktop-c:$wsVersion"
```

### TeaVM Android

Use this module with `gdx-teavm` Android targets. It publishes the TeaVM-side websocket factory, the JNI/C bridge resources used by the `gdx-teavm` Android backend, and the Android runtime helper class that talks to `nv-websocket-client`.

For a `gdx-teavm` Android application module:

```gradle
dependencies {
        implementation "com.github.deedywu.gdx-websockets:teavm-android:$wsVersion"
        teavm "com.github.deedywu.gdx-websockets:teavm-android:$wsVersion"
}
```

### TeaVM iOS

Use this module with experimental `gdx-teavm` iOS targets. It publishes the TeaVM-side websocket factory plus Objective-C/C bridge resources compiled into the generated Xcode project.

For a `gdx-teavm` iOS application module:

```gradle
dependencies {
        implementation "com.github.deedywu.gdx-websockets:teavm-ios:$wsVersion"
}
```

### Version

Specify the `wsVersion` in the `gradle.properties` file in the root directory. Use a tag, branch snapshot, or commit published from this fork:

`wsVersion=2.0.3`

`wsVersion=master-SNAPSHOT`

TeaVM web artifact names changed in `2.0.2`:

- `2.0.1` uses `com.github.deedywu.gdx-websockets:teavm:$wsVersion`
- `2.0.2` and newer use `com.github.deedywu.gdx-websockets:teavm-web:$wsVersion`

### Extensions

- [gdx-websocket-serialization](libraries/extensions/serialization): a custom serialization mechanism, not based on reflection. Alternative to JSON-based communication. More verbose, but gives you full control over the serialization process. Useful for performance-critical applications.

## Examples

Samples now live under `examples`.

- `:examples:desktop`: LWJGL3 launcher using `common`
- `:examples:teavm-desktop-c`: TeaVM C / GLFW launcher using `teavm-desktop-c`
- `:examples:teavm-web`: TeaVM web launcher using `teavm-web`
- `:examples:gwt`: GWT launcher using `html`
- `:examples:android`: standard Android launcher using `common` when Android SDK is configured
- `:examples:ios`: standard RoboVM MetalANGLE iOS launcher using `common` when running on macOS
- `:examples:teavm-android`: TeaVM Android launcher using `teavm-android` when Android SDK is configured

Run it with:

```bash
./gradlew :examples:desktop:run
```

For the iOS example, first list simulators installed on your local machine, then use a simulator name and runtime version from that output:

```bash
./gradlew :examples:ios:iosListSimulators
./gradlew :examples:ios:iosRunSimulator \
    -PiosSimulatorName="iPhone 15 Pro" \
    -PiosSimulatorSdk=17.5
```

For a connected iOS device, list devices and run with signing enabled:

```bash
./gradlew :examples:ios:iosListDevices
./gradlew :examples:ios:iosRunDevice -PiosSkipSigning=false
```

The demo uses the interactive `gdx-teavm` websocket UI flow and connects to `wss://ws.postman-echo.com/raw` by default.

## Basic usage

### Initialization

Make sure to call `CommonWebSockets.initiate()` in DesktopLauncher/AndroidLauncher/IOSLauncher launchers before creating web sockets:
```
        // Initiating web sockets module - safe to call before creating application:
        CommonWebSockets.initiate();
        new LwjglApplication(new MyApplicationListener());
```

In HTMLLauncher, make sure to call `GwtWebSockets.initiate()` before creating web sockets:
```
        @Override
        public ApplicationListener createApplicationListener() {
            // Initiating web sockets module - safe to call before creating application listener:
            GwtWebSockets.initiate();
            return new MyApplicationListener();
        }
```

In TeaVM web launchers, make sure to call `TeaWebSockets.initiate()` before creating web sockets:
```
        public static void main (String[] args) {
            // Initiating web sockets module - safe to call before creating application:
            TeaWebSockets.initiate();

            WebApplicationConfiguration config = new WebApplicationConfiguration("canvas");
            new WebApplication(new MyApplicationListener(), config);
        }
```

In TeaVM GLFW launchers, make sure to call `GLFWWebSockets.initiate()` before creating web sockets:
```
        public static void main (String[] args) {
            // Initiating web sockets module - safe to call before creating application:
            GLFWWebSockets.initiate();
            new GLFWApplication(new MyApplicationListener(), config);
        }
```

In TeaVM Android launchers, make sure to call `AndroidWebSockets.initiate()` before creating web sockets:
```
        public static void main (String[] args) {
            // Initiating web sockets module - safe to call before creating application:
            AndroidWebSockets.initiate();
            new AndroidApplication(new MyApplicationListener(), config);
        }
```

In TeaVM iOS launchers, make sure to call `IOSWebSockets.initiate()` before creating web sockets:
```
        public static void main (String[] args) {
            // Initiating web sockets module - safe to call before creating application:
            IOSWebSockets.initiate();
            new IOSApplication(new MyApplicationListener(), config);
        }
```

### Connecting to a server

```
        WebSocket socket = WebSockets.newSocket(WebSockets.toWebSocketUrl(address, port));
        socket.setSendGracefully(true);
        socket.addListener(new WebSocketListener() { ... });
        socket.connect();
```

## Changes

### 2.0.3

- Added TeaVM Android support for `gdx-teavm` Android targets, including the `AndroidWebSockets` factory, JNI/C bridge resources, and an Android runtime bridge backed by `nv-websocket-client`.
- Added TeaVM iOS support for experimental `gdx-teavm` iOS targets, including the `IOSWebSockets` factory and Objective-C/C bridge resources backed by `NSURLSessionWebSocketTask`.
- Updated the root README with Android and iOS TeaVM dependencies and launcher initialization examples.

### 2.0.2

- Added the TeaVM Desktop C / GLFW backend for `gdx-teavm` desktop-c targets.
- Renamed the TeaVM web artifact from `teavm` to `teavm-web`; use `com.github.deedywu.gdx-websockets:teavm-web:$wsVersion` from this release onward.
- Published `external_cpp` resources and helper scripts for linking websocket support into generated native desktop-c executables.

### 2.0.1

- Prepared the fork for JitPack releases that work for JDK 17 consumers.
- Centralized Java release configuration in the root Gradle build: Java 8 for regular modules and Java 11 for TeaVM modules.
- Updated the published version from `2.0.0` to `2.0.1`.
