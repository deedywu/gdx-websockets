package com.github.czyzby.websocket;

import com.github.czyzby.websocket.data.WebSocketException;
import com.github.czyzby.websocket.impl.TeaWebSocket;

/** Allows to initiate the TeaVM web sockets module.
 *
 * @author SimonIT */
public class TeaWebSockets {

	private TeaWebSockets() {
	}

	/** Initiates {@link WebSockets.WebSocketFactory}. */
	public static void initiate() {
		WebSockets.FACTORY = new TeaWebSocketFactory();
	}

	/** Provides {@link TeaWebSocket} instances.
	 *
	 * @author SimonIT */
	protected static class TeaWebSocketFactory implements WebSockets.WebSocketFactory {
		@Override
		public WebSocket newWebSocket(final String url) {
			return new TeaWebSocket(url);
		}

		@Override
		public WebSocket newInsecureWebSocket(final String url) {
			throw new WebSocketException("Insecure web sockets are not supported by the TeaVM web backend. Browser "
					+ "WebSocket APIs do not allow application code to disable TLS certificate or hostname "
					+ "validation; use ws:// for local browser testing or install a certificate trusted by the "
					+ "browser.");
		}
	}
}
