# WebSocket examples

This directory groups runnable samples for `gdx-websockets`.

## Modules

- `core`: shared demo application code
- `desktop`: LWJGL3 launcher that initializes `CommonWebSockets`

## Run

From the repository root:

```bash
./gradlew :examples:websockets:desktop:run
```

The sample uses the interactive `gdx-teavm` websocket demo flow and connects to `wss://ws.postman-echo.com/raw` by default.
Type text, press `Enter` to send, `F5` to reconnect, and `Esc` to clear the input buffer.
