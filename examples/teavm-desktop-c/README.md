# TeaVM C desktop websocket example

This example ports the `gdx-teavm` desktop-c websocket sample into this repository.

It consumes published artifacts only:

- `com.github.xpenatan.gdx-teavm:backend-glfw:-SNAPSHOT`
- `com.github.deedywu.gdx-websockets:teavm-desktop-c:2.0.4-rc1`

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
- This example uses the online `teavm-desktop-c` websocket artifact instead of the local backend module.
- `backend-glfw` currently follows the `-SNAPSHOT` line in the same spirit as `teavm-android`.
- The generated files are written under `examples/teavm-desktop-c/build/dist`.
- A clean first build needs network access. Gradle downloads its wrapper distribution and dependencies, and the `libcurl` helper downloads the curl source archive if the local system `curl` does not already support both `ws` and `wss`.
- On macOS, the debug build path has been smoke-tested from a clean worktree and empty Gradle cache. The build automatically produced a WebSocket-enabled `libcurl.4.dylib`, copied it next to `websockets_debug`, and the executable stayed running without startup errors.
- The Linux helper script follows the same flow for `libcurl.so.4`, but it should be validated on a Linux host or CI runner with the required development headers installed.
- Gradle may report `exec(...)` deprecation warnings, and the generated TeaVM C code may emit compiler warnings. These warnings are expected with the current toolchain and do not prevent the executable from building.
- The desktop-c executable loads `libcurl` at runtime with `dlopen`. Keeping the generated `libcurl.4.dylib` or `libcurl.so.4` next to the executable is the default portable path; `GDX_TEAVM_LIBCURL_PATH` can point to a specific runtime when needed.
