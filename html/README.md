# LibGDX Web Sockets for GWT
GWT natives for [LibGDX Web Sockets](../..). Make sure to call `GwtWebSockets.initiate()` before creating web sockets:
```
        @Override
        public ApplicationListener createApplicationListener() {
            // Initiating web sockets module - safe to call before creating application listener:
            GwtWebSockets.initiate();
            return new MyApplicationListener();
        }
```

## Dependencies
Add JitPack to your repositories:
```
        maven { url "https://jitpack.io" }
```

`Gradle` dependency (for GWT LibGDX project):
```
        implementation "com.github.deedywu.gdx-websockets:core:$wsVersion:sources"
        implementation "com.github.deedywu.gdx-websockets:html:$wsVersion"
        implementation "com.github.deedywu.gdx-websockets:html:$wsVersion:sources"
```
Set `wsVersion` to a tag, branch snapshot, or commit published from `deedywu/gdx-websockets`.

GWT module:
```
        <inherits name='com.github.czyzby.websocket.GdxWebSocketGwt' />
```
