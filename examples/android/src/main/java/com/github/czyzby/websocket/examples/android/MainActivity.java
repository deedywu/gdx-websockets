package com.github.czyzby.websocket.examples.android;

import android.os.Bundle;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.github.czyzby.websocket.CommonWebSockets;
import com.github.czyzby.websocket.WebSocket;
import com.github.czyzby.websocket.examples.InsecurePerMessageDeflateWebSocketDemo;
import com.github.czyzby.websocket.examples.PerMessageDeflateWebSocketDemo;
import com.github.czyzby.websocket.examples.WebSocketDemoSelector;
import com.github.czyzby.websocket.impl.NvWebSocket;

/** Standard Android launcher for the websocket example. */
public class MainActivity extends AndroidApplication {
    private static final String LOCAL_PMDEFLATE_ENDPOINT = "ws://host-machine-ip:8787/";
    private static final String LOCAL_SECURE_PMDEFLATE_ENDPOINT = "wss://host-machine-ip:8787/";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CommonWebSockets.initiate();

        final AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
        initialize(createDemoSelector(), configuration);
    }

    private static ApplicationListener createDemoSelector() {
        return WebSocketDemoSelector.createDefaultSelectorWithLocalWss(
                WebSocketDemoSelector.normalWssEchoFactory(true),
                LOCAL_PMDEFLATE_ENDPOINT,
                new WebSocketDemoSelector.DemoFactory() {
                    @Override
                    public ApplicationListener create() {
                        return createPerMessageDeflateDemo(LOCAL_PMDEFLATE_ENDPOINT, false);
                    }
                },
                LOCAL_SECURE_PMDEFLATE_ENDPOINT,
                new WebSocketDemoSelector.DemoFactory() {
                    @Override
                    public ApplicationListener create() {
                        return createPerMessageDeflateDemo(LOCAL_SECURE_PMDEFLATE_ENDPOINT, true);
                    }
                });
    }

    private static ApplicationListener createPerMessageDeflateDemo(final String endpoint, final boolean insecure) {
        if (insecure) {
            return new InsecurePerMessageDeflateWebSocketDemo(endpoint, true) {
                @Override
                protected String getNegotiatedExtensionsDescription(final WebSocket webSocket) {
                    return MainActivity.getNegotiatedExtensionsDescription(webSocket);
                }

                @Override
                protected Boolean isPerMessageDeflateNegotiated(final WebSocket webSocket) {
                    return MainActivity.isPerMessageDeflateNegotiated(webSocket);
                }
            };
        }
        return new PerMessageDeflateWebSocketDemo(endpoint, true) {
            @Override
            protected String getNegotiatedExtensionsDescription(final WebSocket webSocket) {
                return MainActivity.getNegotiatedExtensionsDescription(webSocket);
            }

            @Override
            protected Boolean isPerMessageDeflateNegotiated(final WebSocket webSocket) {
                return MainActivity.isPerMessageDeflateNegotiated(webSocket);
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
