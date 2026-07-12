# iOS websocket example

This example shows how to use the standard RoboVM MetalANGLE iOS libGDX backend together with `gdx-websockets:common`.

It does not use the TeaVM iOS backend.

## Enable the module

The iOS example is only included on macOS because RoboVM iOS builds require the Apple toolchain.

## Tasks

```bash
./gradlew :examples:ios:iosRunSimulator
./gradlew :examples:ios:iosRunIPadSimulator
./gradlew :examples:ios:iosRunDevice
./gradlew :examples:ios:iosBuildApp
./gradlew :examples:ios:iosPackageIpa
```

List available simulators and runtimes:

```bash
./gradlew :examples:ios:iosListSimulators
```

Use the simulator name and iOS runtime version from that output to run a specific simulator:

```bash
./gradlew :examples:ios:iosRunSimulator \
    -PiosSimulatorName="iPhone 15 Pro" \
    -PiosSimulatorSdk=17.5
```

The same values can be passed directly to RoboVM:

```bash
./gradlew :examples:ios:iosRunSimulator \
    -Probovm.device.name="iPhone 15 Pro" \
    -Probovm.sdk.version=17.5
```

## Notes

- This example reuses `:examples:core`.
- It initializes `CommonWebSockets` and uses the normal RoboVM MetalANGLE `IOSApplication` launcher.
- It keeps the default websocket endpoint from the shared demo: `wss://ws.postman-echo.com/raw`.
