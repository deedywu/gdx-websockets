# TeaVM C desktop websocket example

This example ports the `gdx-teavm` desktop-c websocket sample into this repository.
It uses the local websocket backend project so backend changes can be tested directly.
It still consumes the published `gdx-teavm` GLFW backend:

- `com.github.xpenatan.gdx-teavm:backend-glfw:1.6.0`

## Build and run

Generate TeaVM C sources:

```bash
./gradlew :examples:teavm-desktop-c:teavmDesktopCGenerate
```

Build a Debug executable:

```bash
./gradlew :examples:teavm-desktop-c:teavmDesktopCDebugBuild
```

Build and run the Debug executable:

```bash
./gradlew :examples:teavm-desktop-c:teavmDesktopCDebugRun
```

Release variants are also available:

```bash
./gradlew :examples:teavm-desktop-c:teavmDesktopCReleaseBuild
./gradlew :examples:teavm-desktop-c:teavmDesktopCReleaseRun
./gradlew :examples:teavm-desktop-c:teavmDesktopCReleaseConsoleRun
```

## Notes

- The shared demo still comes from `:examples:core`.
- The launcher opens a selector with `Normal WSS Echo`, `Local permessage-deflate`, and `Local WSS permessage-deflate` options.
- The `Local permessage-deflate` option defaults to `ws://127.0.0.1:8787/`, which lets it talk to a local `:examples:server-demo-pmdeflate` server.
- The `Local WSS permessage-deflate` option defaults to `wss://127.0.0.1:8787/` and, for development testing only, trusts the local demo server's self-signed certificate by disabling TLS certificate and hostname checks.
- The local permessage-deflate demo exercises the same UI path, but the current TeaVM desktop-c backend does not implement the `permessage-deflate` websocket extension yet, so the local server reports `negotiated=none`.
- This example uses the local `:libraries:backends:teavm-desktop-c` websocket backend module.
- `backend-glfw` follows the released `gdx-teavm` version configured by the root build.
- The generated files are written under `examples/teavm-desktop-c/build/dist`.
- A clean first build needs network access. Gradle downloads its wrapper distribution and dependencies, and the `libcurl` helper downloads the curl source archive if the local system `curl` does not already support both `ws` and `wss`.
- On macOS, the debug build path has been smoke-tested from a clean worktree and empty Gradle cache. The build automatically produced a WebSocket-enabled `libcurl.4.dylib`, copied it next to `websockets_debug`, and the executable stayed running without startup errors.
- The Linux helper script follows the same flow for `libcurl.so.4`, but it should be validated on a Linux host or CI runner with the required development headers installed.
- Gradle may report `exec(...)` deprecation warnings, and the generated TeaVM C code may emit compiler warnings. These warnings are expected with the current toolchain and do not prevent the executable from building.
- The desktop-c executable loads `libcurl` at runtime with `dlopen`. Keeping the generated `libcurl.4.dylib` or `libcurl.so.4` next to the executable is the default portable path; `GDX_TEAVM_LIBCURL_PATH` can point to a specific runtime when needed.
