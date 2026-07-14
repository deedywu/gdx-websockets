package com.github.czyzby.websocket;

import com.github.czyzby.websocket.impl.GLFWWebSocket;

/** Initializes the TeaVM C GLFW websocket implementation.
 * Native backend support currently includes Windows, Linux, and macOS. */
public class GLFWWebSockets {

    private GLFWWebSockets() {
    }

    public static void initiate() {
        WebSockets.FACTORY = new GLFWWebSocketFactory();
    }

    /** Creates a web socket that trusts every TLS certificate and disables hostname verification.
     *
     * <p>This is intentionally named as an insecure helper. It exists for local development and test servers that use
     * {@code wss://} with a self-signed certificate, a certificate issued for a domain while connecting by local IP, or
     * another temporary certificate setup that the native runtime would normally reject. Do not use this helper for
     * production traffic.</p> */
    public static WebSocket newInsecureSocket(String url) {
        return new GLFWWebSocket(url, true);
    }

    protected static class GLFWWebSocketFactory implements WebSockets.WebSocketFactory {
        @Override
        public WebSocket newWebSocket(String url) {
            return new GLFWWebSocket(url);
        }

        @Override
        public WebSocket newInsecureWebSocket(String url) {
            return newInsecureSocket(url);
        }
    }
}
