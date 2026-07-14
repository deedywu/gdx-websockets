package com.github.czyzby.websocket.android;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketExtension;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketState;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class WebSocketsAndroidBridge {
    private static final int STATE_CONNECTING = 0;
    private static final int STATE_OPEN = 1;
    private static final int STATE_CLOSING = 2;
    private static final int STATE_CLOSED = 3;

    private static final int NORMAL_CLOSE = 1000;

    private static final WebSocketFactory FACTORY = new WebSocketFactory().setConnectionTimeout(10000);
    private static final WebSocketFactory INSECURE_FACTORY = new WebSocketFactory()
            .setConnectionTimeout(10000)
            .setVerifyHostname(false)
            .setSSLContext(newTrustAllSSLContext());
    private static final ConcurrentHashMap<Long, WebSocket> SOCKETS = new ConcurrentHashMap<>();

    private static volatile String lastError = "";

    private WebSocketsAndroidBridge() {
    }

    public static boolean createSocket(long handle, String url, boolean usePerMessageDeflate, boolean insecureTls) {
        lastError = "";
        if(url == null || url.trim().isEmpty()) {
            lastError = "A websocket URL is required.";
            return false;
        }
        try {
            WebSocket socket = (insecureTls ? INSECURE_FACTORY : FACTORY).createSocket(url.trim());
            if(usePerMessageDeflate) {
                socket.addExtension(WebSocketExtension.PERMESSAGE_DEFLATE);
            }
            socket.setDirectTextMessage(true);
            socket.addListener(new BridgeListener(handle));
            if(SOCKETS.putIfAbsent(handle, socket) != null) {
                lastError = "WebSocket handle already exists: " + handle;
                return false;
            }
            socket.connectAsynchronously();
            return true;
        } catch(IOException e) {
            lastError = describeThrowable(e);
            return false;
        } catch(RuntimeException e) {
            lastError = describeThrowable(e);
            return false;
        }
    }

    private static SSLContext newTrustAllSSLContext() {
        try {
            TrustManager[] trustAllCertificates = new TrustManager[] { new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certificates, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certificates, String authType) {
                }
            } };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCertificates, new SecureRandom());
            return sslContext;
        } catch(GeneralSecurityException e) {
            throw new IllegalStateException("Unable to create insecure SSL context.", e);
        }
    }

    public static boolean sendText(long handle, String text) {
        lastError = "";
        WebSocket socket = SOCKETS.get(handle);
        if(socket == null) {
            lastError = "WebSocket handle is not open.";
            return false;
        }
        try {
            socket.sendText(text == null ? "" : text);
            return true;
        } catch(RuntimeException e) {
            lastError = describeThrowable(e);
            return false;
        }
    }

    public static boolean closeSocket(long handle, int code, String reason) {
        lastError = "";
        WebSocket socket = SOCKETS.get(handle);
        if(socket == null) {
            return true;
        }
        try {
            socket.disconnect(code, reason == null ? "" : reason);
            return true;
        } catch(RuntimeException e) {
            lastError = describeThrowable(e);
            return false;
        }
    }

    public static void destroySocket(long handle) {
        WebSocket socket = SOCKETS.remove(handle);
        if(socket == null) {
            return;
        }
        try {
            socket.disconnect();
        } catch(RuntimeException ignored) {
        }
    }

    public static String consumeLastError() {
        String value = lastError;
        lastError = "";
        return value;
    }

    public static boolean isPerMessageDeflateAgreed(long handle) {
        WebSocket socket = SOCKETS.get(handle);
        if(socket == null) {
            return false;
        }
        List<WebSocketExtension> extensions = socket.getAgreedExtensions();
        if(extensions == null) {
            return false;
        }
        for(WebSocketExtension extension : extensions) {
            if(extension != null && WebSocketExtension.PERMESSAGE_DEFLATE.equalsIgnoreCase(extension.getName())) {
                return true;
            }
        }
        return false;
    }

    public static String getAgreedExtensionsDescription(long handle) {
        WebSocket socket = SOCKETS.get(handle);
        if(socket == null) {
            return "none";
        }
        List<WebSocketExtension> extensions = socket.getAgreedExtensions();
        return extensions == null || extensions.isEmpty() ? "none" : extensions.toString();
    }

    private static int mapState(WebSocketState state) {
        if(state == null) {
            return STATE_CLOSED;
        }
        switch(state) {
            case CREATED:
            case CONNECTING:
                return STATE_CONNECTING;
            case OPEN:
                return STATE_OPEN;
            case CLOSING:
                return STATE_CLOSING;
            case CLOSED:
            default:
                return STATE_CLOSED;
        }
    }

    private static int extractCloseCode(WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame) {
        WebSocketFrame closeFrame = serverCloseFrame != null ? serverCloseFrame : clientCloseFrame;
        if(closeFrame == null) {
            return NORMAL_CLOSE;
        }
        int closeCode = closeFrame.getCloseCode();
        return closeCode > 0 ? closeCode : NORMAL_CLOSE;
    }

    private static String extractCloseReason(WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame) {
        WebSocketFrame closeFrame = serverCloseFrame != null ? serverCloseFrame : clientCloseFrame;
        if(closeFrame == null) {
            return "";
        }
        String reason = closeFrame.getCloseReason();
        return reason == null ? "" : reason;
    }

    private static String describeThrowable(Throwable throwable) {
        if(throwable == null) {
            return "unknown websocket error";
        }
        String message = throwable.getMessage();
        if(message != null && !message.trim().isEmpty()) {
            return message.trim();
        }
        return throwable.toString();
    }

    private static native void nativeOnStateChanged(long handle, int state);
    private static native void nativeOnOpen(long handle);
    private static native void nativeOnMessageText(long handle, String message);
    private static native void nativeOnError(long handle, String message);
    private static native void nativeOnClose(long handle, int closeCode, String reason);

    private static class BridgeListener extends WebSocketAdapter {
        private final long handle;

        private BridgeListener(long handle) {
            this.handle = handle;
        }

        @Override
        public void onStateChanged(WebSocket websocket, WebSocketState newState) throws Exception {
            nativeOnStateChanged(handle, mapState(newState));
        }

        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
            nativeOnOpen(handle);
        }

        @Override
        public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
            nativeOnError(handle, describeThrowable(exception));
        }

        @Override
        public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame,
                                   boolean closedByServer) throws Exception {
            nativeOnClose(handle, extractCloseCode(serverCloseFrame, clientCloseFrame),
                    extractCloseReason(serverCloseFrame, clientCloseFrame));
            SOCKETS.remove(handle, websocket);
        }

        @Override
        public void onTextMessage(WebSocket websocket, String text) throws Exception {
            nativeOnMessageText(handle, text == null ? "" : text);
        }

        @Override
        public void onTextMessage(WebSocket websocket, byte[] data) throws Exception {
            nativeOnMessageText(handle, data == null ? "" : new String(data, StandardCharsets.UTF_8));
        }

        @Override
        public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
            nativeOnError(handle, describeThrowable(cause));
        }

        @Override
        public void onUnexpectedError(WebSocket websocket, WebSocketException cause) throws Exception {
            nativeOnError(handle, describeThrowable(cause));
        }
    }
}
