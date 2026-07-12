# TeaVM web websocket example

This example runs the shared websocket demo through the TeaVM web backend.

It uses:

- `:examples:core`
- `:libraries:backends:teavm-web`
- published `gdx-teavm` web artifacts from Maven

## Build

Generate the JavaScript web build:

```bash
./gradlew :examples:teavm-web:teavmWebJsBuild
```

Generate the Wasm web build:

```bash
./gradlew :examples:teavm-web:teavmWebWasmBuild
```

## Run with local server

JavaScript:

```bash
./gradlew :examples:teavm-web:teavmWebJsRun
```

Wasm:

```bash
./gradlew :examples:teavm-web:teavmWebWasmRun
```

## Notes

- The TeaVM builder writes output under `examples/teavm-web/build/dist`.
- The launcher initializes `TeaWebSockets` and then starts the TeaVM web application.
- The shared demo connects to `wss://ws.postman-echo.com/raw` by default.
