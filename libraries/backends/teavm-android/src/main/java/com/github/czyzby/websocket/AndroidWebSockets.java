package com.github.czyzby.websocket;

import com.github.czyzby.websocket.impl.AndroidWebSocket;

/** Initializes the TeaVM Android websocket implementation. */
public class AndroidWebSockets {

    private AndroidWebSockets() {
    }

    public static void initiate() {
        WebSockets.FACTORY = new AndroidWebSocketFactory();
    }

    protected static class AndroidWebSocketFactory implements WebSockets.WebSocketFactory {
        @Override
        public WebSocket newWebSocket(String url) {
            return new AndroidWebSocket(url);
        }
    }
}
