# Android websocket example

This example shows how to use the standard Android libGDX backend together with `gdx-websockets:common`.

It does not use the TeaVM Android backend.

## Enable the module

The Android example is only included when an Android SDK is configured through one of:

- `ANDROID_HOME`
- `ANDROID_SDK_ROOT`
- `local.properties` with `sdk.dir=...`

## Build

```bash
./gradlew :examples:android:assembleDebug
```

## Install on a connected device or emulator

```bash
./gradlew :examples:android:installDebug
```

## Notes

- This example reuses `:examples:core`.
- It initializes `CommonWebSockets` and uses the normal `AndroidApplication` launcher.
- The launcher opens a selector with `Normal WSS Echo`, `Local permessage-deflate`, and `Local WSS permessage-deflate` options.
- The `Local permessage-deflate` endpoint defaults to `ws://10.0.2.2:8787/` for Android Emulator access to a local `:examples:server-demo-pmdeflate` server running on the host machine.
- The `Local WSS permessage-deflate` endpoint defaults to `wss://10.0.2.2:8787/` and, for development testing only, trusts the local demo server's self-signed certificate by disabling TLS certificate and hostname checks.
- The local permessage-deflate demo requests `permessage-deflate` and logs whether the server actually negotiated it.
- For a real device, change the endpoint in `Settings` to your host LAN IP or another reachable websocket endpoint, and run the local server on an address reachable from the device.
- Cleartext traffic is enabled in the example manifest so the local `ws://` test server works on Android 9+.
