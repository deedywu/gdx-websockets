package com.github.czyzby.websocket.examples.gwt;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.github.czyzby.websocket.GwtWebSockets;
import com.github.czyzby.websocket.examples.PerMessageDeflateWebSocketDemo;
import com.github.czyzby.websocket.examples.WebSocketDemo;

/** GWT entry point for the websocket example. */
public class GwtLauncher extends GwtApplication {
    @Override
    public GwtApplicationConfiguration getConfig() {
        return new GwtApplicationConfiguration(800, 480);
    }

    @Override
    public ApplicationListener createApplicationListener() {
        GwtWebSockets.initiate();
        return createPerMessageDeflateDemo();
    }

    private static ApplicationListener createPerMessageDeflateDemo() {
        // Use the original shared demo for a normal wss endpoint test.
        // return new WebSocketDemo();

        // Use the permessage-deflate demo for local ws://127.0.0.1:8787/ testing.
        return new PerMessageDeflateWebSocketDemo(PerMessageDeflateWebSocketDemo.DEFAULT_PMDEFLATE_ENDPOINT);
    }
}
