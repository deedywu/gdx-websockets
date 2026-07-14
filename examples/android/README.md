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
- The `Local permessage-deflate` endpoint placeholder is `ws://host-machine-ip:8787/`.
- The `Local WSS permessage-deflate` endpoint placeholder is `wss://host-machine-ip:8787/` and trusts the local server's self-signed certificate for development testing.
- Replace `host-machine-ip` with your host machine LAN IP before running against a local `:examples:server-demo-pmdeflate` server.
- The local permessage-deflate demo requests `permessage-deflate` and logs whether the server actually negotiated it.
- For a real device, change the endpoint in `Settings` to your host LAN IP or another reachable websocket endpoint.
- Cleartext traffic is enabled in the example manifest so the local `ws://` test server works on Android 9+.
