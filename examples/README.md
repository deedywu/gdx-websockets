# WebSocket examples

This directory groups runnable samples for `gdx-websockets`.

## Modules

- `core`: shared demo application code
- `desktop`: LWJGL3 launcher that initializes `CommonWebSockets`
- `teavm-web`: TeaVM web example using the `teavm-web` backend
- `gwt`: GWT example using the `html` backend

## Run

From the repository root:

```bash
./gradlew :examples:desktop:run
```

The sample uses the interactive `gdx-teavm` websocket demo flow and connects to `wss://ws.postman-echo.com/raw` by default.
Type text, press `Enter` to send, `F5` to reconnect, and `Esc` to clear the input buffer.

TeaVM web build:

```bash
./gradlew :examples:teavm-web:teavmWebJsBuild
./gradlew :examples:teavm-web:teavmWebJsRun
```

GWT build:

```bash
./gradlew :examples:gwt:gwtCompile
./gradlew :examples:gwt:gwtRun
```

GWT Super Dev:

```bash
./gradlew :examples:gwt:superDev
```
