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

## Real Device

Connect the device, unlock it, trust this Mac, and enable Developer Mode on the device if iOS asks for it. Then list devices:

```bash
./gradlew :examples:ios:iosListDevices
```

Run on the connected device:

```bash
./gradlew :examples:ios:iosRunDevice \
    -PiosSkipSigning=false
```

If more than one device is connected, use the UDID from the local device list:

```bash
./gradlew :examples:ios:iosRunDevice \
    -PiosSkipSigning=false \
    -PiosDeviceUdid="<device udid>"
```

If RoboVM cannot choose a signing identity or provisioning profile automatically, pass them explicitly:

```bash
./gradlew :examples:ios:iosRunDevice \
    -PiosSkipSigning=false \
    -PiosSignIdentity="Apple Development: Your Name (TEAMID)" \
    -PiosProvisioningProfile="<profile name or uuid>"
```

On newer Xcode/iOS versions where RoboVM's built-in device launcher cannot mount a developer disk image, build with RoboVM and launch with Xcode `devicectl`:

```bash
./gradlew :examples:ios:iosRunDeviceDevicectl \
    -PiosSkipSigning=false \
    -PiosDeviceUdid="<device udid>" \
    -PiosSignIdentity="Apple Development: Your Name (TEAMID)" \
    -PiosProvisioningProfile="<profile name or uuid>"
```

Check local code signing identities with:

```bash
security find-identity -v -p codesigning
```

List available simulators and runtimes:

```bash
./gradlew :examples:ios:iosListSimulators
```

This reads the simulators installed on your local machine. For example, one local machine might print:

```text
-- iOS 17.5 --
    iPhone 15 Pro (...) (Booted)
    ...
```

Use the simulator name and iOS runtime version from your own output to run a specific simulator:

```bash
./gradlew :examples:ios:iosRunSimulator \
    -PiosSimulatorName="<simulator name>" \
    -PiosSimulatorSdk=<runtime version>
```

For the example output above:

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
- The launcher opens a selector with `Normal WSS Echo`, `Local permessage-deflate`, and `Local WSS permessage-deflate` options.
- The `Local permessage-deflate` option defaults to `ws://127.0.0.1:8787/` for simulator testing against `:examples:server-demo-pmdeflate`.
- The `Local WSS permessage-deflate` option defaults to `wss://127.0.0.1:8787/` and, for development testing only, trusts the local demo server's self-signed certificate by disabling TLS certificate and hostname checks.
- For a real device, change the endpoint in `Settings` to your host machine LAN IP or another reachable websocket endpoint.
- The local permessage-deflate demo requests `permessage-deflate` and logs whether the server actually negotiated it.
