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
- local certificate: `examples/server-demo-pmdeflate/build/cert/server-demo-pmdeflate.crt`
- local private key: `examples/server-demo-pmdeflate/build/cert/server-demo-pmdeflate.key`

If the certificate or private key is missing, the server generates a local self-signed certificate at startup and
reuses it on later runs.

That generated certificate is what the demo selector's `Local WSS permessage-deflate` option is intended to talk to.
On supported native backends, that client path is test-only and disables TLS certificate and hostname validation.

Custom host, port, path, certificate, or private key:

```bash
./gradlew :examples:server-demo-pmdeflate:run --args="127.0.0.1 8787 / build/cert/server-demo-pmdeflate.crt build/cert/server-demo-pmdeflate.key"
```

Then point the desktop demo at it:

```bash
./gradlew :examples:desktop:run --args="ws://127.0.0.1:8787/"
```

The desktop, Android, RoboVM iOS, TeaVM Android, TeaVM iOS, and TeaVM desktop-c examples also expose a local WSS option that connects to:

```text
wss://127.0.0.1:8787/
```

That local WSS selector entry is specifically for self-signed demo testing and should not be used as a production TLS pattern.

or, on Android Emulator:

```text
wss://10.0.2.2:8787/
```

For a real Android device, replace `10.0.2.2` with the host machine LAN IP and run the server on an address reachable from the device.
