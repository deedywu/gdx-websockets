# GWT websocket example

This example shows how to use the `gdx-websockets:html` backend from a libGDX GWT app.

It reuses the shared demo from `:examples:core` and compiles it with the GWT backend.

## Tasks

### Compile only

Build the GWT output:

```bash
./gradlew :examples:gwt:gwtCompile
```

Generated files are written to:

```text
examples/gwt/build/dist/gwt
```

### Run locally

Compile the example and start a small local static server:

```bash
./gradlew :examples:gwt:gwtRun
```

Then open:

```text
http://127.0.0.1:8080/index.html
```

### Super Dev

Start the GWT code server:

```bash
./gradlew :examples:gwt:superDev
```

The code server runs on:

```text
http://127.0.0.1:9876
```

For an actual browser session, keep `superDev` running and also start the normal local server:

```bash
./gradlew :examples:gwt:gwtRun
```

Then open the example in Super Dev mode with:

```text
http://127.0.0.1:8080/index.html?gwt.codesvr=127.0.0.1:9876
```

You can also use the `Dev Mode On` bookmarklet from the code server page, but the query-string approach is usually simpler.

## Notes

- `gwtRun` serves the already generated output from `examples/gwt/build/dist/gwt`.
- `superDev` only starts the GWT code server. It does not replace the static file server.
- `examples/gwt/war/` is a generated intermediate directory used during GWT compilation.
- `examples/gwt/build/` is also generated output.
- The GWT launcher opens a selector with `Normal WSS Echo`, `Local permessage-deflate`, and `Local WSS permessage-deflate` options.
- The `Local permessage-deflate` option defaults to `ws://127.0.0.1:8787/` so it can talk to `:examples:server-demo-pmdeflate` during local testing.
- The `Local WSS permessage-deflate` option defaults to `wss://127.0.0.1:8787/`; browser TLS validation still applies, so trust the local certificate in the browser before testing.
- Use the `Settings` button in the demo to switch `ws/wss` and edit the target address for other machines or endpoints.
- Verified locally with JDK 21.
