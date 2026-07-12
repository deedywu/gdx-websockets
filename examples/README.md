# WebSocket examples

This directory groups runnable samples for `gdx-websockets`.

## Modules

- `core`: shared demo application code
- `desktop`: LWJGL3 launcher that initializes `CommonWebSockets`
- `teavm-desktop-c`: TeaVM C / GLFW example using the `teavm-desktop-c` backend
- `teavm-web`: TeaVM web example using the `teavm-web` backend
- `gwt`: GWT example using the `html` backend
- `android`: standard Android launcher using `common` when Android SDK is configured
- `ios`: standard RoboVM MetalANGLE iOS launcher using `common` when running on macOS
- `teavm-android`: TeaVM Android launcher using the `teavm-android` backend when Android SDK is configured

## Run

From the repository root:

```bash
./gradlew :examples:desktop:run
```

The sample uses the interactive `gdx-teavm` websocket demo flow and connects to `wss://ws.postman-echo.com/raw` by default.
Type text, press `Enter` to send, `F5` to reconnect, and `Esc` to clear the input buffer.

TeaVM desktop-c build:

```bash
./gradlew :examples:teavm-desktop-c:teavmDesktopCGenerate
./gradlew :examples:teavm-desktop-c:teavmDesktopCDebugBuild
./gradlew :examples:teavm-desktop-c:teavmDesktopCDebugRun
```

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

Android build:

```bash
./gradlew :examples:android:assembleDebug
./gradlew :examples:android:installDebug
```

iOS simulator build and run:

```bash
./gradlew :examples:ios:iosRunSimulator
./gradlew :examples:ios:iosRunDevice
./gradlew :examples:ios:iosPackageIpa
```

List available iOS simulators:

```bash
./gradlew :examples:ios:iosListSimulators
```

Run a specific iOS simulator:

```bash
./gradlew :examples:ios:iosRunSimulator \
    -PiosSimulatorName="iPhone 15 Pro" \
    -PiosSimulatorSdk=17.5
```

TeaVM Android build:

```bash
./gradlew :examples:teavm-android:assembleDebug
./gradlew :examples:teavm-android:installDebug
```
