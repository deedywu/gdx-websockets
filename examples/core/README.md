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

- `WebSocketDemo`: the common websocket sample UI and interaction logic
- shared rendering, logging, and reconnect flow used by every example target

## Notes

- The demo defaults to `wss://ws.postman-echo.com/raw`.
- Keyboard input is available on platforms that forward key events normally.
- Touch-friendly button controls are enabled for targets that need them, such as TeaVM Android.
