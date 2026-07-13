package com.github.czyzby.websocket.examples.teavmdesktopc;

import com.github.czyzby.websocket.GLFWWebSockets;
import com.github.czyzby.websocket.examples.WebSocketDemoSelector;
import com.github.xpenatan.gdx.teavm.backends.glfw.GLFWApplication;
import com.github.xpenatan.gdx.teavm.backends.glfw.GLFWApplicationConfiguration;

/** TeaVM C / GLFW launcher for the websocket example. */
public class TeaVMDesktopCLauncher {
    private TeaVMDesktopCLauncher() {
    }

    public static void main(final String[] args) {
        GLFWWebSockets.initiate();

        final GLFWApplicationConfiguration config = new GLFWApplicationConfiguration();
        config.setTitle("gdx-websockets teavm desktop-c");
        config.setWindowedMode(800, 480);
        config.useVsync(true);
        config.setForegroundFPS(60);

        new GLFWApplication(WebSocketDemoSelector.createDefaultSelector(), config);
    }
}
