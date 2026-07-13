package com.github.czyzby.websocket.examples.android;

import android.os.Bundle;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.github.czyzby.websocket.CommonWebSockets;
import com.github.czyzby.websocket.WebSocket;
import com.github.czyzby.websocket.examples.PerMessageDeflateWebSocketDemo;
import com.github.czyzby.websocket.examples.WebSocketDemo;
import com.github.czyzby.websocket.impl.NvWebSocket;

/** Standard Android launcher for the websocket example. */
public class MainActivity extends AndroidApplication {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CommonWebSockets.initiate();

        final AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
        initialize(createPerMessageDeflateDemo(), configuration);
    }

    private static ApplicationListener createPerMessageDeflateDemo() {
        return new PerMessageDeflateWebSocketDemo("ws://host-machine-ip:8787/") {
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
