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
- The launcher opens a selector with `Normal WSS Echo` and `Local permessage-deflate` options.
- The `Local permessage-deflate` option defaults to `ws://127.0.0.1:8787/`, which lets it talk to a local `:examples:server-demo-pmdeflate` server.
- Browser websocket APIs do not expose extension negotiation details to TeaVM web, so the demo logs the request while negotiated extensions remain unavailable for this backend.
