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

## Local WSS testing
For local development only, `WebSockets.newInsecureSocket(url)` creates a `wss://` socket that trusts every TLS
certificate and disables hostname verification after `CommonWebSockets.initiate()` has registered this backend. This is
useful when testing against a local IP address, self-signed certificate, or temporary certificate whose hostname does
not match the address:
```
        WebSocket socket = WebSockets.newInsecureSocket("wss://192.168.2.1:8080/");
```
Do not use this helper for production traffic. Production clients should use normal TLS validation, or configure a
specific trusted certificate/keystore through a custom SSL context instead of trusting every certificate.

For production or staging environments that need a private CA, pinned certificate, or custom trust store, create a
normal socket and configure the trusted SSL context explicitly:
```
        WebSocket socket = WebSockets.newSocket("wss://example.internal:8080/");
        if (socket instanceof NvWebSocket) {
            ((NvWebSocket) socket).setSSLContext(sslContext);
        }
```
Call `setSSLContext` before `connect()`. This keeps TLS validation explicit without disabling certificate checks for
every server.
