# LibGDX Web Sockets for TeaVM Web
TeaVM web backend for [LibGDX Web Sockets](../../..). Make sure to call `TeaWebSockets.initiate()` before creating web sockets:
```
        public static void main (String[] args) {
            // Initiating web sockets module - safe to call before creating application:
            TeaWebSockets.initiate();

            WebApplicationConfiguration config = new WebApplicationConfiguration("canvas");
            new WebApplication(new MyApplicationListener(), config);
        }
```

This module keeps the TeaVM web implementation originally contributed by SimonIT.

## Dependencies
Add JitPack to your repositories:
```
        maven { url "https://jitpack.io" }
```

`Gradle` dependency (for TeaVM web LibGDX project):
```
        implementation "com.github.deedywu.gdx-websockets:teavm-web:$wsVersion"
```

Artifact name note:

- `2.0.1` uses `com.github.deedywu.gdx-websockets:teavm:$wsVersion`
- `2.0.2` and newer use `com.github.deedywu.gdx-websockets:teavm-web:$wsVersion`

Set `wsVersion` to a tag, branch snapshot, or commit published from `deedywu/gdx-websockets`.
