# libGDX Web Sockets

Maintained fork of [MrStahlfelge/gdx-websockets](https://github.com/MrStahlfelge/gdx-websockets), which itself forked [czyzby's websockets](https://github.com/czyzby/gdx-lml/tree/master/websocket).

This fork preserves SimonIT's TeaVM implementation, keeps the existing API shape for libGDX projects, and serves as the home for ongoing compatibility updates.

Default libGDX `Net` API provides only TCP sockets and HTTP requests. This library aims to add client-side web sockets support.
It works on all platforms targeted by libGDX.

## Fork goals
- Publish JitPack artifacts from `deedywu/gdx-websockets`
- Keep TeaVM support available and maintained
- Stay close to the original API so existing libGDX projects can migrate with minimal changes

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

### Version

Specify the `wsVersion` in the `gradle.properties` file in the root directory. Use a tag, branch snapshot, or commit published from this fork:

`wsVersion=2.0.2`

`wsVersion=master-SNAPSHOT`

TeaVM web artifact names changed in `2.0.2`:

- `2.0.1` uses `com.github.deedywu.gdx-websockets:teavm:$wsVersion`
- `2.0.2` and newer use `com.github.deedywu.gdx-websockets:teavm-web:$wsVersion`

### Extensions

- [gdx-websocket-serialization](serialization): a custom serialization mechanism, not based on reflection. Alternative to JSON-based communication. More verbose, but gives you full control over the serialization process. Useful for performance-critical applications.

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

### Connecting to a server

```
        WebSocket socket = WebSockets.newSocket(WebSockets.toWebSocketUrl(address, port));
        socket.setSendGracefully(true);
        socket.addListener(new WebSocketListener() { ... });
        socket.connect();
```

## Changes

1.5 -> 1.6

- Added `AbstractWebSocketListener`, which handles object deserialization and logs errors. This is a solid base for your `WebSocketListener` implementation if don't use pure string-based communication. 
- Added `WebSocketHandler`, which extends `AbstractWebSocketListener` even further. Instead of dealing with raw `Object` types and having to determine packet type on your own, you can register a `Handler` to a specific packet class and it will be invoked each time a packet of the selected type is received.
- Added default `Serializer` implementation: `JsonSerializer`. Uses **LibGDX** `Json` API to serialize objects as strings.
- Added `WebSockets#DEFAULT_SERIALIZER`. Modify this field to automatically assign serializer of your choice to all new web socket instances.
- Added `Base64Serializer`. Uses **LibGDX** `Base64Coder` API to encode and decode the data to and from *BASE64*. Wraps around an existing serializer.
- Added custom serialization in [gdx-websocket-serialization](serialization). `ManualSerializer` is an alternative to the default `JsonSerializer`.
- Added `WebSockets#closeGracefully(WebSocket)` null-safe utility method. Attempts to close the passed web socket and catches any thrown exceptions (their message is logged using `Gdx.app.debug` method). If passed web socket is null, it will be ignored. Useful for application disposing methods, when you don't exactly care if the web socket is not properly closed and *have to* continue disposing other native assets, even if `close()` call fails.
