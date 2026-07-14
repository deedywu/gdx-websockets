package com.github.czyzby.websocket.examples.desktop;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.github.czyzby.websocket.CommonWebSockets;
import com.github.czyzby.websocket.WebSocket;
import com.github.czyzby.websocket.examples.InsecurePerMessageDeflateWebSocketDemo;
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
        return WebSocketDemoSelector.createDefaultSelectorWithLocalWss(
                new WebSocketDemoSelector.DemoFactory() {
                    @Override
                    public ApplicationListener create() {
                        return createPerMessageDeflateDemo(
                                PerMessageDeflateWebSocketDemo.DEFAULT_PMDEFLATE_ENDPOINT, false);
                    }
                },
                new WebSocketDemoSelector.DemoFactory() {
                    @Override
                    public ApplicationListener create() {
                        return createPerMessageDeflateDemo(
                                PerMessageDeflateWebSocketDemo.DEFAULT_SECURE_PMDEFLATE_ENDPOINT, true);
                    }
                });
    }

    private static ApplicationListener createPerMessageDeflateDemo(final String endpoint, final boolean insecure) {
        if (insecure) {
            return new InsecurePerMessageDeflateWebSocketDemo(endpoint) {
                @Override
                protected String getNegotiatedExtensionsDescription(final WebSocket webSocket) {
                    return DesktopLauncher.getNegotiatedExtensionsDescription(webSocket);
                }

                @Override
                protected Boolean isPerMessageDeflateNegotiated(final WebSocket webSocket) {
                    return DesktopLauncher.isPerMessageDeflateNegotiated(webSocket);
                }
            };
        }
        return new PerMessageDeflateWebSocketDemo(endpoint) {
            @Override
            protected String getNegotiatedExtensionsDescription(final WebSocket webSocket) {
                return DesktopLauncher.getNegotiatedExtensionsDescription(webSocket);
            }

            @Override
            protected Boolean isPerMessageDeflateNegotiated(final WebSocket webSocket) {
                return DesktopLauncher.isPerMessageDeflateNegotiated(webSocket);
            }
        };
    }

    private static String getNegotiatedExtensionsDescription(final WebSocket webSocket) {
        return webSocket instanceof NvWebSocket
                ? ((NvWebSocket) webSocket).getAgreedExtensionsDescription()
                : null;
    }

    private static Boolean isPerMessageDeflateNegotiated(final WebSocket webSocket) {
        return webSocket instanceof NvWebSocket
                ? Boolean.valueOf(((NvWebSocket) webSocket).isPerMessageDeflateAgreed())
                : null;
    }
}
