package com.github.czyzby.websocket.examples;

import com.github.czyzby.websocket.WebSocket;
import com.github.czyzby.websocket.WebSockets;

/** Development-only local {@code wss://} permessage-deflate demo that accepts the example server's self-signed
 * certificate. This is useful for local testing, not as a production trust strategy. */
public class InsecurePerMessageDeflateWebSocketDemo extends PerMessageDeflateWebSocketDemo {
    public InsecurePerMessageDeflateWebSocketDemo() {
        super(DEFAULT_SECURE_PMDEFLATE_ENDPOINT);
    }

    public InsecurePerMessageDeflateWebSocketDemo(final String endpoint) {
        super(endpoint);
    }

    public InsecurePerMessageDeflateWebSocketDemo(final String endpoint, final boolean touchControlsEnabled) {
        super(endpoint, touchControlsEnabled);
    }

    @Override
    protected WebSocket createSocket(final String socketEndpoint) {
        if (isSecureEndpoint(socketEndpoint)) {
            return WebSockets.newInsecureSocket(socketEndpoint);
        }
        return super.createSocket(socketEndpoint);
    }

    private static boolean isSecureEndpoint(final String socketEndpoint) {
        return socketEndpoint != null && socketEndpoint.regionMatches(true, 0, "wss://", 0, 6);
    }
}
