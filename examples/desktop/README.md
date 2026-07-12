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
- The shared demo connects to `wss://ws.postman-echo.com/raw` by default.
