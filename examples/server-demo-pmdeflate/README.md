# Per-message deflate Netty server

This example runs a small Netty websocket server that enables `permessage-deflate` and echoes incoming text frames.

## Run

```bash
./gradlew :examples:server-demo-pmdeflate:run
```

Default bind address:

- host: `127.0.0.1`
- port: `8787`
- websocket path: `/`

Custom host, port, or path:

```bash
./gradlew :examples:server-demo-pmdeflate:run --args="127.0.0.1 8787 /"
```

Then point the desktop demo at it:

```bash
./gradlew :examples:desktop:run --args="ws://127.0.0.1:8787/"
```
