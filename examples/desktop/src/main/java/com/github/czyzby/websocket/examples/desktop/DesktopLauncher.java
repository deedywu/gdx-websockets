package com.github.czyzby.websocket.examples.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.github.czyzby.websocket.CommonWebSockets;
import com.github.czyzby.websocket.examples.PerMessageDeflateWebSocketDemo;

/** Desktop entry point for the websocket sample. */
public class DesktopLauncher {
    private static final String DEFAULT_PMDEFLATE_ENDPOINT = "ws://127.0.0.1:8787/";

    private DesktopLauncher() {
    }

    public static void main(final String[] args) {
        CommonWebSockets.initiate();

        final Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("gdx-websockets websockets desktop");
        configuration.setWindowedMode(800, 480);
        configuration.useVsync(true);

        new Lwjgl3Application(new PerMessageDeflateWebSocketDemo(resolveEndpoint(args)), configuration);
    }

    private static String resolveEndpoint(final String[] args) {
        if (args != null && args.length > 0 && args[0] != null && !args[0].trim().isEmpty()) {
            return args[0].trim();
        }
        final String propertyEndpoint = System.getProperty("gdx.websocket.url");
        if (propertyEndpoint != null && !propertyEndpoint.trim().isEmpty()) {
            return propertyEndpoint.trim();
        }
        return DEFAULT_PMDEFLATE_ENDPOINT;
    }
}
