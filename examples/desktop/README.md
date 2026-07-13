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
- The desktop launcher defaults to `ws://127.0.0.1:8787/` so it can talk to `:examples:server-demo-pmdeflate` out of the box.
- The desktop demo requests `permessage-deflate` and logs whether the server actually negotiated it.
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
