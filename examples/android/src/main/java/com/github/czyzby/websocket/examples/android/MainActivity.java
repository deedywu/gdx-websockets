package com.github.czyzby.websocket.examples.android;

import android.os.Bundle;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.github.czyzby.websocket.CommonWebSockets;
import com.github.czyzby.websocket.WebSocket;
import com.github.czyzby.websocket.examples.PerMessageDeflateWebSocketDemo;
import com.github.czyzby.websocket.examples.WebSocketDemoSelector;
import com.github.czyzby.websocket.impl.NvWebSocket;

/** Standard Android launcher for the websocket example. */
public class MainActivity extends AndroidApplication {
    private static final String LOCAL_PMDEFLATE_ENDPOINT = "ws://10.0.2.2:8787/";
    private static final String LOCAL_SECURE_PMDEFLATE_ENDPOINT = "wss://10.0.2.2:8787/";

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
        return new PerMessageDeflateWebSocketDemo(endpoint, true, insecure) {
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
