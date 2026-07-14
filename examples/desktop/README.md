# Desktop websocket example

This example runs the shared websocket demo through the standard LWJGL3 desktop backend.

It uses:

- `:examples:core`
- `:libraries:backends:common`

## Run

```bash
./gradlew :examples:desktop:run
```

## Notes

- This is the simplest way to try the demo locally.
- It initializes `CommonWebSockets` before starting the application.
- The desktop launcher opens a selector with `Normal WSS Echo`, `Local permessage-deflate`, and `Local WSS permessage-deflate` options.
- The `Local permessage-deflate` option uses `ws://127.0.0.1:8787/` so it can talk to `:examples:server-demo-pmdeflate` out of the box.
- The `Local WSS permessage-deflate` option uses `wss://127.0.0.1:8787/` and trusts the local server's self-signed certificate for development testing.
- The local permessage-deflate demo requests `permessage-deflate` and logs whether the server actually negotiated it.
- Use the `Settings` button in the demo, or press `F2`, to switch `ws/wss` and edit `host:port/path` at runtime.
- You can still point it at `wss://ws.postman-echo.com/raw` or any other endpoint by editing the address in the demo UI.

## Local permessage-deflate test server

To verify a positive negotiation path locally:

```bash
./gradlew :examples:server-demo-pmdeflate:run
```

Then run the desktop example against the local server:

```bash
./gradlew :examples:desktop:run
```
