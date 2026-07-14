# Shared websocket demo

This module contains the shared demo application code used by the example launchers in this repository.

It is not meant to be run directly. Instead, other example modules provide the platform-specific launchers:

- `:examples:desktop`
- `:examples:gwt`
- `:examples:android`
- `:examples:teavm-web`
- `:examples:teavm-android`
- `:examples:teavm-desktop-c`

## Contents

- `WebSocketDemoSelector`: the shared entry screen for choosing which demo scenario to run
- `WebSocketDemo`: the common websocket sample UI and interaction logic
- `PerMessageDeflateWebSocketDemo`: a shared variant that requests `permessage-deflate` and logs the negotiated result
- `InsecurePerMessageDeflateWebSocketDemo`: a local `wss://` variant for launchers that support self-signed local WSS testing
- shared rendering, logging, and reconnect flow used by every example target

## Notes

- All selector launchers offer `Normal WSS Echo` and `Local permessage-deflate`.
- Desktop, Android, RoboVM iOS, and TeaVM Android also expose `Local WSS permessage-deflate` for local self-signed certificate testing.
- Keyboard input is available on platforms that forward key events normally.
- Touch-friendly button controls are enabled for targets that need them, such as TeaVM Android.
