package com.github.czyzby.websocket.examples.teavmdesktopc;

import java.io.IOException;

import com.github.czyzby.websocket.GLFWWebSocketsBuild;

/** Builds the TeaVM desktop-c websocket example. */
public class BuildTeaVMDesktopCExample {
    private BuildTeaVMDesktopCExample() {
    }

    public static void main(final String[] args) throws IOException {
        GLFWWebSocketsBuild.build("websockets", TeaVMDesktopCLauncher.class, args);
    }
}
