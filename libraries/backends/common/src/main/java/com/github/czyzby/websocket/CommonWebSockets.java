package com.github.czyzby.websocket;

import com.github.czyzby.websocket.WebSockets.WebSocketFactory;
import com.github.czyzby.websocket.data.WebSocketException;
import com.github.czyzby.websocket.impl.NvWebSocket;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/** Allows to initiate common web sockets module.
 *
 * @author MJ */
public class CommonWebSockets {
    private CommonWebSockets() {
    }

    /** Initiates {@link WebSocketFactory}. */
    public static void initiate() {
        WebSockets.FACTORY = new CommonWebSocketFactory();
    }

    /** Creates a web socket that trusts every TLS certificate and disables hostname verification.
     *
     * <p>This is intentionally named as an insecure helper. It exists for local development and test servers that use
     * {@code wss://} with a self-signed certificate, a certificate issued for a domain while connecting by local IP, or
     * another temporary certificate setup that the JVM would normally reject. Do not use this helper for production
     * traffic; production clients should rely on normal certificate validation or a deliberately configured
     * {@link SSLContext}.</p> */
    public static WebSocket newInsecureSocket(final String url) {
        final NvWebSocket webSocket = new NvWebSocket(url);
        webSocket.setVerifyHostname(false);
        webSocket.setSSLContext(newTrustAllSSLContext());
        return webSocket;
    }

    private static SSLContext newTrustAllSSLContext() {
        try {
            final TrustManager[] trustAllCertificates = new TrustManager[] { new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(final X509Certificate[] certificates, final String authType) {
                }

                @Override
                public void checkServerTrusted(final X509Certificate[] certificates, final String authType) {
                }
            } };
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCertificates, new SecureRandom());
            return sslContext;
        } catch (final GeneralSecurityException exception) {
            throw new WebSocketException("Unable to create insecure SSL context.", exception);
        }
    }

    /** Provides {@link NvWebSocket} instances.
     *
     * @author MJ */
    protected static class CommonWebSocketFactory implements WebSocketFactory {
        @Override
        public WebSocket newWebSocket(final String url) {
            return new NvWebSocket(url);
        }
    }
}
