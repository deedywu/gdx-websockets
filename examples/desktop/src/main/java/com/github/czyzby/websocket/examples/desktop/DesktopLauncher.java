package com.github.czyzby.websocket.examples.desktop;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.github.czyzby.websocket.CommonWebSockets;
import com.github.czyzby.websocket.WebSocket;
import com.github.czyzby.websocket.examples.PerMessageDeflateWebSocketDemo;
import com.github.czyzby.websocket.examples.WebSocketDemoSelector;
import com.github.czyzby.websocket.impl.NvWebSocket;

/** Desktop entry point for the websocket sample. */
public class DesktopLauncher {
    private DesktopLauncher() {
    }

    public static void main(final String[] args) {
        CommonWebSockets.initiate();

        final Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("gdx-websockets websockets desktop");
        configuration.setWindowedMode(800, 480);
        configuration.useVsync(true);
        new Lwjgl3Application(createDemoSelector(), configuration);
    }

    private static ApplicationListener createDemoSelector() {
        return WebSocketDemoSelector.createDefaultSelector(new WebSocketDemoSelector.DemoFactory() {
            @Override
            public ApplicationListener create() {
                return createPerMessageDeflateDemo();
            }
        });
    }

    private static ApplicationListener createPerMessageDeflateDemo() {
        return new PerMessageDeflateWebSocketDemo(PerMessageDeflateWebSocketDemo.DEFAULT_PMDEFLATE_ENDPOINT) {
            @Override
            protected String getNegotiatedExtensionsDescription(final WebSocket webSocket) {
                return webSocket instanceof NvWebSocket
                        ? ((NvWebSocket) webSocket).getAgreedExtensionsDescription()
                        : null;
            }

            @Override
            protected Boolean isPerMessageDeflateNegotiated(final WebSocket webSocket) {
                return webSocket instanceof NvWebSocket
                        ? Boolean.valueOf(((NvWebSocket) webSocket).isPerMessageDeflateAgreed())
                        : null;
            }
        };
    }
}
