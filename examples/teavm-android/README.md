# TeaVM Android websocket example

This example ports the `gdx-teavm` Android websocket sample into this repository.

It uses the TeaVM Android workflow, not the standard libGDX Android backend.

## Repositories

This module is intended to consume published `gdx-teavm` artifacts from Maven:

- release line such as `1.5.6` when available
- or `-SNAPSHOT` from the Sonatype snapshots repository

It also consumes the published `gdx-websockets:teavm-android` artifact instead of the local project module.

## Enable the module

The module is only included when an Android SDK is configured through one of:

- `ANDROID_HOME`
- `ANDROID_SDK_ROOT`
- `local.properties` with `sdk.dir=...`

## Build

```bash
./gradlew :examples:teavm-android:assembleDebug
```

## Install on a connected device or emulator

```bash
./gradlew :examples:teavm-android:installDebug
```

## Notes

- Shared demo code still comes from `:examples:core`.
- The TeaVM Android main class is `WebSocketsAndroidLauncher`.
- The Android Activity is a minimal `TeaAndroidActivity` subclass.
- This variant still reuses the shared `WebSocketDemo`, but enables on-screen touch buttons because TeaVM Android does not receive the same keyboard events as the normal Android example.
