# TeaVM iOS websocket example

This example ports the `gdx-teavm` iOS websocket sample into this repository.

It uses the experimental TeaVM iOS workflow, not the standard RoboVM iOS backend.

## Repositories

This module is intended to consume published `gdx-teavm` artifacts from Maven:

- release line such as `1.5.6` when available
- or `-SNAPSHOT` from the Sonatype snapshots repository

It consumes this repository's local `:libraries:backends:teavm-ios` project module so the example always uses the backend sources being edited here.

## Enable the module

The module is only included on macOS because the generated project requires Xcode.

## Build

Generate TeaVM C sources and assets:

```bash
./gradlew :examples:teavm-ios:iosGenerate
```

Initialize the generated Xcode project:

```bash
./gradlew :examples:teavm-ios:iosInitXcode
```

Open the generated Xcode project:

```bash
./gradlew :examples:teavm-ios:iosOpenXcode
```

Build and run on an iOS simulator:

```bash
./gradlew :examples:teavm-ios:iosBuildSimulator
./gradlew :examples:teavm-ios:iosRunSimulator
```

By default the simulator run task uses the currently booted simulator. You can also target a specific simulator:

```bash
./gradlew :examples:teavm-ios:iosRunSimulator \
    -PiosSimulatorUdid=<simulator-udid>
```

List connected devices and run on a real device with Xcode signing:

```bash
./gradlew :examples:teavm-ios:iosListDevices
./gradlew :examples:teavm-ios:iosRunDevice \
    -PiosDeviceUdid=<device-udid> \
    -PiosSkipSigning=false
```

Build and package an IPA:

```bash
./gradlew :examples:teavm-ios:iosPackageIpa
```

Generated output is written under `examples/teavm-ios/build/dist/ios`.
IPA output is written under `examples/teavm-ios/build/distributions`.

## Notes

- Shared demo code still comes from `:examples:core`.
- The TeaVM iOS main class is `WebSocketsIOSLauncher`.
- The launcher initializes `IOSWebSockets` before creating the `IOSApplication`.
- This variant uses on-screen touch buttons, matching the mobile TeaVM Android example.
- The default graphics API is `angle`, which downloads the pinned MetalANGLEKit framework bundle. Set Gradle property `gdx.teavm.ios.graphicsApi=gles` to use the older OpenGL ES / GLKit template.
- This module includes compatibility resources and a generated CMake patch for the current `backend-ios:-SNAPSHOT` line.
