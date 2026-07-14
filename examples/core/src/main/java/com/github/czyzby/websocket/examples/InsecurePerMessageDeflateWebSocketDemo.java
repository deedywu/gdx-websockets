package com.github.czyzby.websocket.examples;

/** Development-only local {@code wss://} permessage-deflate demo that accepts the example server's self-signed
 * certificate. This is useful for local testing, not as a production trust strategy. */
public class InsecurePerMessageDeflateWebSocketDemo extends PerMessageDeflateWebSocketDemo {
    public InsecurePerMessageDeflateWebSocketDemo() {
        super(DEFAULT_SECURE_PMDEFLATE_ENDPOINT, true, true);
    }

    public InsecurePerMessageDeflateWebSocketDemo(final String endpoint) {
        super(endpoint, true, true);
    }

    public InsecurePerMessageDeflateWebSocketDemo(final String endpoint, final boolean touchControlsEnabled) {
        super(endpoint, touchControlsEnabled, true);
    }
}
