package com.github.czyzby.websocket;

import com.github.czyzby.websocket.impl.AndroidWebSocket;

/** Initializes the TeaVM Android websocket implementation. */
public class AndroidWebSockets {

    private AndroidWebSockets() {
    }

    public static void initiate() {
        WebSockets.FACTORY = new AndroidWebSocketFactory();
    }

    /** Creates a web socket that trusts every TLS certificate and disables hostname verification.
     *
     * <p>This is intentionally named as an insecure helper. It exists for local development and test servers that use
     * {@code wss://} with a self-signed certificate, a certificate issued for a domain while connecting by local IP, or
     * another temporary certificate setup that the Android runtime would normally reject. Do not use this helper for
     * production traffic.</p> */
    public static WebSocket newInsecureSocket(String url) {
        return new AndroidWebSocket(url, true);
    }

    protected static class AndroidWebSocketFactory implements WebSockets.WebSocketFactory {
        @Override
        public WebSocket newWebSocket(String url) {
            return new AndroidWebSocket(url);
        }

        @Override
        public WebSocket newInsecureWebSocket(String url) {
            return newInsecureSocket(url);
        }
    }
}
