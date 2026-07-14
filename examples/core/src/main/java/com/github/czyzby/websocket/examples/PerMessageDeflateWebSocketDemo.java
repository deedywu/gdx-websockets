package com.github.czyzby.websocket.examples;

import com.github.czyzby.websocket.WebSocket;
import com.github.czyzby.websocket.WebSockets;

/** Shared websocket demo that requests {@code permessage-deflate} and logs the negotiated result when available. */
public class PerMessageDeflateWebSocketDemo extends WebSocketDemo {
    public static final String DEFAULT_PMDEFLATE_ENDPOINT = "ws://127.0.0.1:8787/";
    public static final String DEFAULT_SECURE_PMDEFLATE_ENDPOINT = "wss://127.0.0.1:8787/";

    private final boolean insecureTls;

    public PerMessageDeflateWebSocketDemo() {
        this(DEFAULT_PMDEFLATE_ENDPOINT, true, false);
    }

    public PerMessageDeflateWebSocketDemo(final String endpoint) {
        this(endpoint, true, false);
    }

    public PerMessageDeflateWebSocketDemo(final String endpoint, final boolean touchControlsEnabled) {
        this(endpoint, touchControlsEnabled, false);
    }

    public PerMessageDeflateWebSocketDemo(final String endpoint, final boolean touchControlsEnabled,
            final boolean insecureTls) {
        super(endpoint, touchControlsEnabled);
        this.insecureTls = insecureTls;
    }

    @Override
    @SuppressWarnings("deprecation")
    protected WebSocket createSocket(final String socketEndpoint) {
        return insecureTls ? WebSockets.newInsecureSocket(socketEndpoint) : super.createSocket(socketEndpoint);
    }

    @Override
    protected void configureSocket(final WebSocket webSocket) {
        webSocket.setPerMessageDeflate(true);
    }

    @Override
    protected void onSocketOpen(final WebSocket webSocket) {
        log("Requested extension: permessage-deflate");

        final String extensionsDescription = getNegotiatedExtensionsDescription(webSocket);
        final Boolean agreed = isPerMessageDeflateNegotiated(webSocket);

        if (extensionsDescription != null) {
            log("Negotiated extensions: " + extensionsDescription);
        } else {
            log("Negotiated extensions: unavailable for this backend");
        }

        if (agreed != null) {
            log("permessage-deflate " + (agreed.booleanValue() ? "enabled" : "not negotiated"));
        }
    }

    protected String getNegotiatedExtensionsDescription(final WebSocket webSocket) {
        return null;
    }

    protected Boolean isPerMessageDeflateNegotiated(final WebSocket webSocket) {
        return null;
    }
}
