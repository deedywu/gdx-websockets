# Per-message deflate Netty server

This example runs a small Netty websocket server that enables `permessage-deflate` and echoes incoming text frames. It
accepts both `ws://` and `wss://` traffic on the same port.

## Run

```bash
./gradlew :examples:server-demo-pmdeflate:run
```

Default bind address:

- host: `127.0.0.1`
- port: `8787`
- websocket path: `/`
- local certificate: `build/cert/server-demo-pmdeflate.crt`
- local private key: `build/cert/server-demo-pmdeflate.key`

If the certificate or private key is missing, the server generates a local self-signed certificate at startup and
reuses it on later runs.

Custom host, port, path, certificate, or private key:

```bash
./gradlew :examples:server-demo-pmdeflate:run --args="127.0.0.1 8787 / build/cert/server-demo-pmdeflate.crt build/cert/server-demo-pmdeflate.key"
```

Then point the desktop demo at it:

```bash
./gradlew :examples:desktop:run --args="ws://127.0.0.1:8787/"
```

The desktop, Android, RoboVM iOS, TeaVM Android, and TeaVM desktop-c examples also expose a local WSS option that connects to:

```text
wss://127.0.0.1:8787/
```

or, on Android devices, the host-machine placeholder:

```text
wss://host-machine-ip:8787/
```
