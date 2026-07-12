package com.github.czyzby.websocket.examples.gwt;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/** Small static file server for the generated GWT example. */
public class GwtHttpServer {
    private static final int DEFAULT_PORT = 8080;

    private GwtHttpServer() {
    }

    public static void main(final String[] args) throws Exception {
        final Path root = args.length > 0 ? Paths.get(args[0]).toAbsolutePath().normalize()
                : Paths.get("build/dist/gwt").toAbsolutePath().normalize();
        final int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;

        if (!Files.isDirectory(root)) {
            throw new IllegalStateException("Missing GWT output directory: " + root);
        }

        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", new StaticHandler(root));
        server.setExecutor(null);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));

        System.out.println("Serving GWT example from " + root);
        System.out.println("Open http://127.0.0.1:" + port + "/index.html");

        new CountDownLatch(1).await();
    }

    private static final class StaticHandler implements HttpHandler {
        private final Path root;

        private StaticHandler(final Path root) {
            this.root = root;
        }

        @Override
        public void handle(final HttpExchange exchange) throws IOException {
            final String method = exchange.getRequestMethod();
            if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }

            String requestPath = exchange.getRequestURI().getPath();
            if (requestPath == null || requestPath.isEmpty() || "/".equals(requestPath)) {
                requestPath = "/index.html";
            }

            final Path resolved = root.resolve(requestPath.substring(1)).normalize();
            if (!resolved.startsWith(root) || !Files.exists(resolved) || Files.isDirectory(resolved)) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }

            final byte[] body = Files.readAllBytes(resolved);
            exchange.getResponseHeaders().set("Content-Type", guessContentType(resolved));
            exchange.sendResponseHeaders(200, "HEAD".equalsIgnoreCase(method) ? -1 : body.length);

            if (!"HEAD".equalsIgnoreCase(method)) {
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(body);
                }
            } else {
                exchange.close();
            }
        }

        private static String guessContentType(final Path path) throws IOException {
            final String detected = Files.probeContentType(path);
            if (detected != null) {
                return detected;
            }

            final String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
            if (fileName.endsWith(".js")) {
                return "application/javascript; charset=UTF-8";
            }
            if (fileName.endsWith(".html")) {
                return "text/html; charset=UTF-8";
            }
            if (fileName.endsWith(".css")) {
                return "text/css; charset=UTF-8";
            }
            if (fileName.endsWith(".json")) {
                return "application/json; charset=UTF-8";
            }
            if (fileName.endsWith(".wasm")) {
                return "application/wasm";
            }
            if (fileName.endsWith(".png")) {
                return "image/png";
            }
            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                return "image/jpeg";
            }
            if (fileName.endsWith(".glsl") || fileName.endsWith(".txt") || fileName.endsWith(".fnt")) {
                return "text/plain; charset=UTF-8";
            }
            return "application/octet-stream";
        }
    }
}
