# TeaVM Android websocket example

This example ports the `gdx-teavm` Android websocket sample into this repository.

It uses the TeaVM Android workflow, not the standard libGDX Android backend.

## Repository wiring

- It consumes published `gdx-teavm` artifacts from Maven.
- It consumes the local `:libraries:backends:teavm-android` project module so TeaVM Android backend changes in this repository are picked up immediately during local builds.

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
- The launcher opens a selector with `Normal WSS Echo`, `Local permessage-deflate`, and `Local WSS permessage-deflate` options.
- The `Local permessage-deflate` endpoint defaults to `ws://10.0.2.2:8787/` for Android Emulator access to a local `:examples:server-demo-pmdeflate` server running on the host machine.
- The `Local WSS permessage-deflate` endpoint defaults to `wss://10.0.2.2:8787/` and, for development testing only, trusts the local demo server's self-signed certificate by disabling TLS certificate and hostname checks.
- For a real device, change the endpoint in `Settings` to your host LAN IP or another reachable websocket endpoint, and run the local server on an address reachable from the device.
- Touch-friendly on-screen controls stay enabled because TeaVM Android does not receive the same keyboard events as the normal Android example.
- Tapping the Address field opens a native Android text input dialog because TeaVM Android does not currently route `Gdx.input.getTextInput` to an editable popup.
- The local permessage-deflate demo requests `permessage-deflate` and logs whether the server actually negotiated it.
- Cleartext traffic is enabled in the example manifest so the local `ws://` test server works on Android 9+.
