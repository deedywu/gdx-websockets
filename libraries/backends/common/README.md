# LibGDX Web Sockets
Desktop and Android natives for [LibGDX Web Sockets](../../..). Based on [nv-websocket-client](https://github.com/TakahikoKawasaki/nv-websocket-client). Make sure to call `CommonWebSockets.initiate()` before creating web sockets:
```
        // Initiating web sockets module - safe to call before creating application:
        CommonWebSockets.initiate();
        new LwjglApplication(new MyApplicationListener());
```

## Dependencies
Add JitPack to your repositories:
```
        maven { url "https://jitpack.io" }
```

`Gradle` dependency (for desktop and Android LibGDX projects - safe to use in the core project if you do not target any other platforms):
```
        implementation "com.github.deedywu.gdx-websockets:common:$wsVersion"
```
Set `wsVersion` to a tag, branch snapshot, or commit published from `deedywu/gdx-websockets`.
