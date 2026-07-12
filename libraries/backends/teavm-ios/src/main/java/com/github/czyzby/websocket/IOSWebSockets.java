package com.github.czyzby.websocket;

import com.github.czyzby.websocket.impl.IOSWebSocket;

/** Initializes the TeaVM iOS websocket implementation. */
public class IOSWebSockets {

    private IOSWebSockets() {
    }

    public static void initiate() {
        WebSockets.FACTORY = new IOSWebSocketFactory();
    }

    protected static class IOSWebSocketFactory implements WebSockets.WebSocketFactory {
        @Override
        public WebSocket newWebSocket(String url) {
            return new IOSWebSocket(url);
        }
    }
}
