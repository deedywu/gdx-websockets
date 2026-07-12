# TeaVM C desktop websocket example

This example ports the `gdx-teavm` desktop-c websocket sample into this repository.

It consumes published artifacts only:

- `com.github.xpenatan.gdx-teavm:backend-glfw:-SNAPSHOT`
- `com.github.deedywu.gdx-websockets:teavm-desktop-c:2.0.3`

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
