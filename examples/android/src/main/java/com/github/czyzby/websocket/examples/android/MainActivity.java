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
    private static final String LOCAL_PMDEFLATE_ENDPOINT = "ws://host-machine-ip:8787/";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CommonWebSockets.initiate();

        final AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
        initialize(createDemoSelector(), configuration);
    }

    private static ApplicationListener createDemoSelector() {
        return WebSocketDemoSelector.createDefaultSelector(LOCAL_PMDEFLATE_ENDPOINT,
                new WebSocketDemoSelector.DemoFactory() {
                    @Override
                    public ApplicationListener create() {
                        return createPerMessageDeflateDemo();
                    }
                });
    }

    private static ApplicationListener createPerMessageDeflateDemo() {
        return new PerMessageDeflateWebSocketDemo(LOCAL_PMDEFLATE_ENDPOINT) {
            @Override
            protected String getNegotiatedExtensionsDescription(final WebSocket webSocket) {
                return webSocket instanceof NvWebSocket
                        ? ((NvWebSocket) webSocket).getAgreedExtensionsDescription()
                        : null;
            }

            @Override
            protected Boolean isPerMessageDeflateNegotiated(final WebSocket webSocket) {
                return webSocket instanceof NvWebSocket
                        ? ((NvWebSocket) webSocket).isPerMessageDeflateAgreed()
                        : null;
            }
        };
    }
}
