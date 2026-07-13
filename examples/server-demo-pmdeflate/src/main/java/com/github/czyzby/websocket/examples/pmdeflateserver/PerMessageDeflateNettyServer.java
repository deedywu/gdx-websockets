package com.github.czyzby.websocket.examples.pmdeflateserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.util.AttributeKey;

/** Small Netty websocket echo server that enables per-message deflate negotiation. */
public class PerMessageDeflateNettyServer {
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 8787;
    private static final String DEFAULT_PATH = "/";

    private static final AttributeKey<String> REQUESTED_EXTENSIONS =
            AttributeKey.valueOf("gdx.websocket.requestedExtensions");
    private static final AttributeKey<String> NEGOTIATED_EXTENSIONS =
            AttributeKey.valueOf("gdx.websocket.negotiatedExtensions");

    private PerMessageDeflateNettyServer() {
    }

    public static void main(final String[] args) throws Exception {
        final String host = args.length > 0 && args[0] != null && !args[0].trim().isEmpty()
                ? args[0].trim() : DEFAULT_HOST;
        final int port = args.length > 1 && args[1] != null && !args[1].trim().isEmpty()
                ? Integer.parseInt(args[1].trim()) : DEFAULT_PORT;
        final String websocketPath = normalizePath(args.length > 2 ? args[2] : DEFAULT_PATH);

        final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        final EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            final ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ServerInitializer(websocketPath));

            final ChannelFuture future = bootstrap.bind(host, port).sync();
            final Channel channel = future.channel();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                channel.close();
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }));

            System.out.println("Netty permessage-deflate websocket server listening on ws://" + host + ":" + port
                    + websocketPath);
            channel.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully().syncUninterruptibly();
            workerGroup.shutdownGracefully().syncUninterruptibly();
        }
    }

    private static String normalizePath(final String value) {
        if (value == null || value.trim().isEmpty()) {
            return DEFAULT_PATH;
        }
        final String trimmed = value.trim();
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }

    private static String normalizeExtensions(final String value) {
        return value == null || value.trim().isEmpty() ? "none" : value.trim();
    }

    private static final class ServerInitializer extends ChannelInitializer<SocketChannel> {
        private final String websocketPath;

        private ServerInitializer(final String websocketPath) {
            this.websocketPath = websocketPath;
        }

        @Override
        protected void initChannel(final SocketChannel channel) {
            channel.pipeline()
                    .addLast(new HttpServerCodec())
                    .addLast(new HttpObjectAggregator(65536))
                    .addLast(new RequestedExtensionsCaptureHandler())
                    .addLast(new NegotiatedExtensionsCaptureHandler())
                    .addLast(new WebSocketServerCompressionHandler())
                    .addLast(new WebSocketServerProtocolHandler(websocketPath, null, true))
                    .addLast(new EchoHandler());
        }
    }

    private static final class RequestedExtensionsCaptureHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(final ChannelHandlerContext context, final Object message) throws Exception {
            if (message instanceof FullHttpRequest) {
                final FullHttpRequest request = (FullHttpRequest) message;
                context.channel().attr(REQUESTED_EXTENSIONS)
                        .set(normalizeExtensions(request.headers().get(HttpHeaderNames.SEC_WEBSOCKET_EXTENSIONS)));
            }
            super.channelRead(context, message);
        }
    }

    private static final class NegotiatedExtensionsCaptureHandler extends ChannelOutboundHandlerAdapter {
        @Override
        public void write(final ChannelHandlerContext context, final Object message, final ChannelPromise promise)
                throws Exception {
            if (message instanceof HttpResponse) {
                final HttpResponse response = (HttpResponse) message;
                if (response.status().equals(HttpResponseStatus.SWITCHING_PROTOCOLS)) {
                    context.channel().attr(NEGOTIATED_EXTENSIONS)
                            .set(normalizeExtensions(response.headers().get(HttpHeaderNames.SEC_WEBSOCKET_EXTENSIONS)));
                }
            }
            super.write(context, message, promise);
        }
    }

    private static final class EchoHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
        @Override
        public void userEventTriggered(final ChannelHandlerContext context, final Object event) throws Exception {
            if (event instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
                final WebSocketServerProtocolHandler.HandshakeComplete handshake =
                        (WebSocketServerProtocolHandler.HandshakeComplete) event;
                final String requestedExtensions = normalizeExtensions(context.channel().attr(REQUESTED_EXTENSIONS).get());
                final String negotiatedExtensions = normalizeExtensions(context.channel().attr(NEGOTIATED_EXTENSIONS).get());

                System.out.println("[open] path=" + handshake.requestUri()
                        + " requested=" + requestedExtensions
                        + " negotiated=" + negotiatedExtensions);

                context.writeAndFlush(new TextWebSocketFrame("server-ready negotiated=" + negotiatedExtensions));
                return;
            }
            super.userEventTriggered(context, event);
        }

        @Override
        protected void channelRead0(final ChannelHandlerContext context, final TextWebSocketFrame frame) {
            final String payload = frame.text();
            System.out.println("[message] " + payload);
            context.writeAndFlush(new TextWebSocketFrame("echo:" + payload));
        }

        @Override
        public void exceptionCaught(final ChannelHandlerContext context, final Throwable cause) {
            System.out.println("[error] " + (cause == null ? "unknown" : cause.getMessage()));
            context.close();
        }

        @Override
        public void channelInactive(final ChannelHandlerContext context) throws Exception {
            System.out.println("[close] remote=" + context.channel().remoteAddress());
            super.channelInactive(context);
        }
    }
}
