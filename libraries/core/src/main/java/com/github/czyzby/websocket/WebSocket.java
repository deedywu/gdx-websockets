package com.github.czyzby.websocket;

import com.github.czyzby.websocket.data.WebSocketCloseCode;
import com.github.czyzby.websocket.data.WebSocketException;
import com.github.czyzby.websocket.data.WebSocketState;
import com.github.czyzby.websocket.serialization.Serializer;

import java.util.Arrays;
import java.util.List;

/** Common interface for all web socket implementations.
 *
 * @author MJ */
public interface WebSocket {
    /** Will try to connect with the given URL. Note that
     *
     * @throws WebSocketException if unable to connect. */
    void connect() throws WebSocketException;

    /** @return current web socket state. Should never return null - if web socket was not opened yet, should return
     *         CLOSED state. */
    WebSocketState getState();

    /** @return true if client is currently connected with the server. */
    boolean isOpen();

    /** @return true if web socket is in connecting state. */
    boolean isConnecting();

    /** @return true if web socket is in closing state. */
    boolean isClosing();

    /** @return true if web socket is in closed state. Might return true if the web socket was not opened yet. */
    boolean isClosed();

    /** @return true if client's connection is secure. */
    boolean isSecure();

    /** @return URL used by current web socket. Can be null if not properly connected or web socket is corrupted. */
    String getUrl();

    /** @return subprotocols requested during the websocket handshake, or null when none were configured.
     * @author deedywu */
    List<String> getProtocols();

    /** @param listener will be notified of web socket events. Listeners are notified in the order that they were
     *            added. */
    void addListener(WebSocketListener listener);

    /** Enables fluent listener registration.
     *
     * @author deedywu
     * @param listener will be notified of web socket events. Listeners are notified in the order that they were added.
     * @return this web socket. */
    default WebSocket withListener(WebSocketListener listener) {
        addListener(listener);
        return this;
    }

    /** @param listener will no longer be notified of web socket events. */
    void removeListener(WebSocketListener listener);

    /** Sends an empty packet to the server.
     *
     * @throws WebSocketException if unable to send the packet. */
    void sendKeepAlivePacket() throws WebSocketException;

    /** @param serializer will be used to serialize passed packets. */
    void setSerializer(Serializer serializer);

    /** Enables fluent serializer configuration.
     *
     * @author deedywu
     * @param serializer will be used to serialize passed packets.
     * @return this web socket. */
    default WebSocket withSerializer(Serializer serializer) {
        setSerializer(serializer);
        return this;
    }

    /** @return serializer used to serialize passed packets. */
    Serializer getSerializer();

    /** @param asString if true, packets will be serialized to strings instead of bytes. Defaults to false.
     * @see #send(Object) */
    void setSerializeAsString(boolean asString);

    /** Enables fluent serialized packet format configuration.
     *
     * @author deedywu
     * @param asString if true, packets will be serialized to strings instead of bytes.
     * @return this web socket.
     * @see #setSerializeAsString(boolean) */
    default WebSocket withSerializeAsString(boolean asString) {
        setSerializeAsString(asString);
        return this;
    }

    /** @param sendGracefully if true, exceptions thrown during packet sending will not be thrown immediately - instead,
     *            they will be passed to {@link WebSocketListener#onError(WebSocket, Throwable)} to all registered
     *            listeners. Defaults to false. */
    void setSendGracefully(boolean sendGracefully);

    /** Enables fluent graceful send error handling configuration.
     *
     * @author deedywu
     * @param sendGracefully if true, exceptions thrown during packet sending will be passed to listeners.
     * @return this web socket.
     * @see #setSendGracefully(boolean) */
    default WebSocket withSendGracefully(boolean sendGracefully) {
        setSendGracefully(sendGracefully);
        return this;
    }

    /** @param perMessageDeflate if true, implementation should try to negotiate the
     *            {@code permessage-deflate} extension during the websocket handshake when the platform can control
     *            extensions. Implementations that do not support or expose extension negotiation may ignore this
     *            setting. Defaults to false. Set before {@link #connect()} for consistent behavior. */
    void setPerMessageDeflate(boolean perMessageDeflate);

    /** Enables fluent configuration of the {@code permessage-deflate} extension request.
     *
     * @author deedywu
     * @param perMessageDeflate if true, request {@code permessage-deflate} during the websocket handshake.
     * @return this web socket. */
    default WebSocket withPerMessageDeflate(boolean perMessageDeflate) {
        setPerMessageDeflate(perMessageDeflate);
        return this;
    }

    /** @author deedywu
     * @param protocols websocket subprotocols to request during the handshake. Null, empty, and blank values are
     *            ignored. Set before {@link #connect()} for consistent behavior. */
    void setProtocols(List<String> protocols);

    /** Enables fluent configuration of websocket subprotocols.
     *
     * @author deedywu
     * @param protocols websocket subprotocols to request during the handshake.
     * @return this web socket. */
    default WebSocket withProtocols(List<String> protocols) {
        setProtocols(protocols);
        return this;
    }

    /** Enables fluent configuration of websocket subprotocols.
     *
     * @author deedywu
     * @param protocols websocket subprotocols to request during the handshake.
     * @return this web socket. */
    default WebSocket withProtocols(String... protocols) {
        setProtocols(protocols == null ? null : Arrays.asList(protocols));
        return this;
    }

    /**
     * @param tcpNoDelay set this flag to configure if websocket should use TCP_NODELAY. This does
     *                   not change behaviour on HTML5, where it is activated on most browsers.
     *                   Default is true
     */
    void setUseTcpNoDelay(boolean tcpNoDelay);

    /** Enables fluent TCP_NODELAY configuration.
     *
     * @author deedywu
     * @param tcpNoDelay set this flag to configure if websocket should use TCP_NODELAY.
     * @return this web socket.
     * @see #setUseTcpNoDelay(boolean) */
    default WebSocket withUseTcpNoDelay(boolean tcpNoDelay) {
        setUseTcpNoDelay(tcpNoDelay);
        return this;
    }

    /**
     * @param verifyHostname set this flag to configure if websocket should verify hostnames for SSL
     *                       connections. It is more secure, but will fail on older Android devices
     *                       with self-signed certificates. This does not change behaviour on HTML5.
     *                       Default is false
     */
    void setVerifyHostname(boolean verifyHostname);

    /** Enables fluent SSL hostname verification configuration.
     *
     * @author deedywu
     * @param verifyHostname set this flag to configure if websocket should verify hostnames for SSL connections.
     * @return this web socket.
     * @see #setVerifyHostname(boolean) */
    default WebSocket withVerifyHostname(boolean verifyHostname) {
        setVerifyHostname(verifyHostname);
        return this;
    }

    /** @param packet will be serialized and sent to the server if the client is connected. Nulls are ignored. Request
     *            is ignored if client is not connected. Fails if no serializer is set.
     * @throws WebSocketException if unable to send the packet due to an exception. Not thrown if the client is not
     *             connected.
     * @see #setSerializer(Serializer)
     * @see #setSerializeAsString(boolean) */
    void send(Object packet) throws WebSocketException;

    /** @param packet will be sent as-is to the server. Nulls are ignored. Request is ignored if client is not
     *            connected.
     * @throws WebSocketException if unable to send the packet due to an exception. Not thrown if the client is not
     *             connected. */
    void send(String packet) throws WebSocketException;

    /** @param packet will be sent as binary data to the server. Nulls are ignored. Might not be supported by every
     *            platform - older browsers in particular. Request is ignored if client is not connected.
     * @throws WebSocketException if unable to send the packet due to an exception. Not thrown if the client is not
     *             connected. */
    void send(byte[] packet) throws WebSocketException;

    /** @return true if web sockets are supported on this platform. */
    boolean isSupported();

    /** Closes the connection. Uses normal close code and no reason.
     *
     * @throws WebSocketException if unable to close the connection. */
    void close() throws WebSocketException;

    /** Closes the connection. Uses normal close code.
     *
     * @param reason closing reason. Optional.
     * @throws WebSocketException if unable to close the connection. */
    void close(String reason) throws WebSocketException;

    /** Closes the connection. Uses no disconnection reason.
     *
     * @param closeCode connection close code. Cannot be null.
     * @throws WebSocketException if unable to close the connection. */
    void close(int closeCode) throws WebSocketException;

    /** Closes the connection.
     *
     * @param closeCode connection close code.
     * @param reason closing reason. Optional.
     * @throws WebSocketException if unable to close the connection. */
    void close(int closeCode, String reason) throws WebSocketException;
}
