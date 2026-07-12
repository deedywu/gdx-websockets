package com.github.czyzby.websocket.examples.teavmweb;

import com.github.czyzby.websocket.TeaWebSockets;
import com.github.czyzby.websocket.examples.WebSocketDemo;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;

/** TeaVM web entry point for the websocket example. */
public class TeaVMWebLauncher {
    private TeaVMWebLauncher() {
    }

    public static void main(final String[] args) {
        TeaWebSockets.initiate();

        final WebApplicationConfiguration config = new WebApplicationConfiguration("canvas");
        config.width = 0;
        config.height = 0;
        config.showDownloadLogs = true;

        new WebApplication(new WebSocketDemo(), config);
    }
}
