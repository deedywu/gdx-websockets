package com.github.czyzby.websocket.impl;

import com.github.czyzby.websocket.WebSockets;
import com.github.czyzby.websocket.data.WebSocketCloseCode;
import com.github.czyzby.websocket.data.WebSocketException;
import com.github.czyzby.websocket.data.WebSocketState;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketExtension;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.net.SocketException;
import java.util.List;

/** Default web socket implementation for desktop and mobile platforms.
 *
 * @author MJ */
public class NvWebSocket extends AbstractWebSocket {
    private final WebSocketFactory webSocketFactory = new WebSocketFactory();
    private WebSocket webSocket;

    public NvWebSocket(final String url) {
        super(url);
        webSocketFactory.setVerifyHostname(verifyHostname);
    }

    @Override
    public void connect() throws WebSocketException {
        try {
            dispose();
            final WebSocket currentWebSocket = webSocket = webSocketFactory.createSocket(getUrl());
            if (usePerMessageDeflate) {
                currentWebSocket.addExtension(WebSocketExtension.PERMESSAGE_DEFLATE);
            }
            currentWebSocket.addListener(new NvWebSocketListener(this));
            currentWebSocket.connectAsynchronously();
        } catch (final Throwable exception) {
            throw new WebSocketException("Unable to connect.", exception);
        }
    }

    @Override
    public void setUseTcpNoDelay(boolean useTcpNoDelay) {
        super.setUseTcpNoDelay(useTcpNoDelay);
        if (webSocket != null && webSocket.getSocket() != null) {
            try {
                webSocket.getSocket().setTcpNoDelay(useTcpNoDelay);
            } catch (SocketException ignored) {
            }
        }
    }

    /** Removes current web socket instance. */
    protected void dispose() {
        final WebSocket currentWebSocket = webSocket;
        if (currentWebSocket != null && currentWebSocket.isOpen()) {
            try {
                currentWebSocket.disconnect(WebSockets.ABNORMAL_AUTOMATIC_CLOSE_CODE);
            } catch (final Exception exception) {
                postErrorEvent(exception);
            }
        }
    }

    @Override
    public WebSocketState getState() {
        final WebSocket currentWebSocket = webSocket;
        return currentWebSocket == null ? WebSocketState.CLOSED : convertState(currentWebSocket.getState());
    }

    private static WebSocketState convertState(final com.neovisionaries.ws.client.WebSocketState state) {
        switch (state) {
            case CLOSED:
            case CREATED:
                return WebSocketState.CLOSED;
            case CLOSING:
                return WebSocketState.CLOSING;
            case CONNECTING:
                return WebSocketState.CONNECTING;
            case OPEN:
                return WebSocketState.OPEN;
        }
        return WebSocketState.CLOSED;
    }

    @Override
    public boolean isSecure() {
        final WebSocket currentWebSocket = webSocket;
        return currentWebSocket != null && "wss".equalsIgnoreCase(currentWebSocket.getURI().getScheme());
    }

    @Override
    public boolean isOpen() {
        final WebSocket currentWebSocket = webSocket;
        return currentWebSocket != null && currentWebSocket.isOpen();
    }

    @Override
    public void close(final int closeCode, final String reason) throws WebSocketException {
        WebSocketCloseCode.checkIfAllowedInClient(closeCode);
        final WebSocket currentWebSocket = webSocket;
        if (currentWebSocket != null) {
            try {
                currentWebSocket.disconnect(closeCode, reason);
            } catch (final Throwable exception) {
                throw new WebSocketException("Unable to close the web socket.", exception);
            }
        }
    }

    @Override
    protected void sendBinary(final byte[] packet) throws Exception {
        webSocket.sendBinary(packet);
    }

    @Override
    protected void sendString(final String packet) throws Exception {
        webSocket.sendText(packet);
    }

    /** @return true if the server agreed to use the {@code permessage-deflate} extension for the current connection. */
    public boolean isPerMessageDeflateAgreed() {
        final WebSocket currentWebSocket = webSocket;
        if (currentWebSocket == null) {
            return false;
        }
        final List<WebSocketExtension> extensions = currentWebSocket.getAgreedExtensions();
        if (extensions == null) {
            return false;
        }
        for (final WebSocketExtension extension : extensions) {
            if (extension != null && WebSocketExtension.PERMESSAGE_DEFLATE.equalsIgnoreCase(extension.getName())) {
                return true;
            }
        }
        return false;
    }

    /** @return description of all extensions agreed during the current handshake or {@code none}. */
    public String getAgreedExtensionsDescription() {
        final WebSocket currentWebSocket = webSocket;
        if (currentWebSocket == null) {
            return "none";
        }
        final List<WebSocketExtension> extensions = currentWebSocket.getAgreedExtensions();
        return extensions == null || extensions.isEmpty() ? "none" : extensions.toString();
    }
}
