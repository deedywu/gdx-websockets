package com.github.czyzby.websocket.examples.android;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.github.czyzby.websocket.CommonWebSockets;
import com.github.czyzby.websocket.examples.WebSocketDemo;

/** Standard Android launcher for the websocket example. */
public class MainActivity extends AndroidApplication {
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CommonWebSockets.initiate();

        final AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
        initialize(new WebSocketDemo(), configuration);
    }
}
