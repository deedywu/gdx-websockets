package com.github.czyzby.websocket.examples.gwt;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.github.czyzby.websocket.GwtWebSockets;
import com.github.czyzby.websocket.examples.WebSocketDemoSelector;

/** GWT entry point for the websocket example. */
public class GwtLauncher extends GwtApplication {
    @Override
    public GwtApplicationConfiguration getConfig() {
        return new GwtApplicationConfiguration(800, 480);
    }

    @Override
    public ApplicationListener createApplicationListener() {
        GwtWebSockets.initiate();
        return WebSocketDemoSelector.createDefaultSelector();
    }
}
