# WebSocket examples

This directory groups runnable samples for `gdx-websockets`.

## Modules

- `core`: shared demo application code
- `desktop`: LWJGL3 launcher that initializes `CommonWebSockets`
- `server-demo-pmdeflate`: Netty websocket server for local `permessage-deflate` negotiation tests
- `teavm-desktop-c`: TeaVM C / GLFW example using the `teavm-desktop-c` backend
- `teavm-web`: TeaVM web example using the `teavm-web` backend
- `gwt`: GWT example using the `html` backend
- `android`: standard Android launcher using `common` with the permessage-deflate demo when Android SDK is configured
- `ios`: standard RoboVM MetalANGLE iOS launcher using `common` when running on macOS
- `teavm-android`: TeaVM Android launcher using the `teavm-android` backend when Android SDK is configured
- `teavm-ios`: TeaVM iOS launcher using the `teavm-ios` backend when running on macOS

## Run

From the repository root:

```bash
./gradlew :examples:desktop:run
```

Local permessage-deflate server:

```bash
./gradlew :examples:server-demo-pmdeflate:run
```

The server accepts both `ws://` and `wss://` on the same port. If its local certificate is missing, it generates a
self-signed certificate under `examples/server-demo-pmdeflate/build/cert`.

The desktop, GWT, Android, iOS, TeaVM web, TeaVM desktop-c, TeaVM Android, and TeaVM iOS launchers open a shared selector with `Normal WSS Echo` and `Local permessage-deflate` options.
The desktop, GWT, iOS simulator, TeaVM web, TeaVM desktop-c, and TeaVM iOS simulator `Local permessage-deflate` option uses `ws://127.0.0.1:8787/` for local testing.
The Android and TeaVM Android `Local permessage-deflate` option uses the placeholder `ws://host-machine-ip:8787/` for the same local server.
The desktop, Android, RoboVM iOS, TeaVM Android, TeaVM iOS, and TeaVM desktop-c launchers also expose `Local WSS permessage-deflate` for testing the same server through a self-signed local `wss://` connection. That option is test-only and disables TLS certificate and hostname validation on supported native backends.
Replace `host-machine-ip` with your host machine LAN IP, or use the demo UI to switch to another reachable endpoint.
For a real iOS or TeaVM iOS device, use the demo UI to replace `127.0.0.1` with your host machine LAN IP.
Type text, press `Enter` to send, `F2` to edit connection settings, `F5` to reconnect, and `Esc` to clear the input buffer.

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

List connected iOS devices and run on a real device:

```bash
./gradlew :examples:ios:iosListDevices
./gradlew :examples:ios:iosRunDevice -PiosSkipSigning=false
```

List available iOS simulators:

```bash
./gradlew :examples:ios:iosListSimulators
```

Use the simulator name and runtime version from that local output to run a specific simulator. For example, if your machine lists `iPhone 15 Pro` under `iOS 17.5`:

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

TeaVM iOS simulator build:

```bash
./gradlew :examples:teavm-ios:iosGenerate
./gradlew :examples:teavm-ios:iosInitXcode
./gradlew :examples:teavm-ios:iosRunSimulator
```

TeaVM iOS device and IPA tasks:

```bash
./gradlew :examples:teavm-ios:iosListDevices
./gradlew :examples:teavm-ios:iosRunDevice \
    -PiosDeviceUdid=<device-udid> \
    -PiosSkipSigning=false
./gradlew :examples:teavm-ios:iosPackageIpa
```
