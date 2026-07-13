package com.github.czyzby.websocket.examples;

import java.lang.reflect.Method;

import com.github.czyzby.websocket.WebSocket;

/** Shared websocket demo that requests {@code permessage-deflate} and logs the negotiated result when available. */
public class PerMessageDeflateWebSocketDemo extends WebSocketDemo {
    public PerMessageDeflateWebSocketDemo() {
        super();
    }

    public PerMessageDeflateWebSocketDemo(final String endpoint) {
        super(endpoint);
    }

    public PerMessageDeflateWebSocketDemo(final String endpoint, final boolean touchControlsEnabled) {
        super(endpoint, touchControlsEnabled);
    }

    @Override
    protected void configureSocket(final WebSocket webSocket) {
        webSocket.setPerMessageDeflate(true);
    }

    @Override
    protected void onSocketOpen(final WebSocket webSocket) {
        log("Requested extension: permessage-deflate");

        final String extensionsDescription = invokeStringMethod(webSocket, "getAgreedExtensionsDescription");
        final Boolean agreed = invokeBooleanMethod(webSocket, "isPerMessageDeflateAgreed");

        if (extensionsDescription != null) {
            log("Negotiated extensions: " + extensionsDescription);
        } else {
            log("Negotiated extensions: unavailable for backend " + webSocket.getClass().getSimpleName());
        }

        if (agreed != null) {
            log("permessage-deflate " + (agreed.booleanValue() ? "enabled" : "not negotiated"));
        }
    }

    private static String invokeStringMethod(final WebSocket webSocket, final String methodName) {
        try {
            final Method method = webSocket.getClass().getMethod(methodName);
            final Object value = method.invoke(webSocket);
            return value == null ? null : value.toString();
        } catch (final Exception ignored) {
            return null;
        }
    }

    private static Boolean invokeBooleanMethod(final WebSocket webSocket, final String methodName) {
        try {
            final Method method = webSocket.getClass().getMethod(methodName);
            final Object value = method.invoke(webSocket);
            return value instanceof Boolean ? (Boolean) value : null;
        } catch (final Exception ignored) {
            return null;
        }
    }
}
