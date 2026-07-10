# LibGDX Web Sockets for TeaVM
TeaVM natives for [LibGDX Web Sockets](../..). Make sure to call `TeaWebSockets.initiate()` before creating web sockets:
```
        public static void main (String[] args) {
            // Initiating web sockets module - safe to call before creating application:
            TeaWebSockets.initiate();
            new TeaApplication(new MyApplicationListener(), config);
}
```

This module keeps the TeaVM implementation originally contributed by SimonIT.

## Dependencies
Add JitPack to your repositories:
```
        maven { url "https://jitpack.io" }
```

`Gradle` dependency (for TeaVM LibGDX project):
```
        implementation "com.github.deedywu.gdx-websockets:teavm:$wsVersion"
```
Set `wsVersion` to a tag, branch snapshot, or commit published from `deedywu/gdx-websockets`.
