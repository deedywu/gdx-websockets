package com.github.czyzby.websocket.examples.teavmweb;

import java.io.File;

import org.teavm.vm.TeaVMOptimizationLevel;

import com.github.xpenatan.gdx.teavm.backends.shared.config.builder.TeaBuilder;
import com.github.xpenatan.gdx.teavm.backends.web.config.backend.WebBackend;

/** Builds the TeaVM web example and optionally starts the embedded web server. */
public class BuildTeaVMWebExample {
    private BuildTeaVMWebExample() {
    }

    public static void main(final String[] args) {
        final boolean wasm = args.length > 0 && "wasm".equalsIgnoreCase(args[0]);
        final boolean serve = args.length > 1 && "serve".equalsIgnoreCase(args[1]);

        final WebBackend backend = new WebBackend()
                .setWebAssembly(wasm)
                .setStartJettyAfterBuild(serve)
                .setHtmlTitle("gdx-websockets teavm-web");

        new TeaBuilder(backend)
                .setOptimizationLevel(TeaVMOptimizationLevel.SIMPLE)
                .setMainClass(TeaVMWebLauncher.class.getName())
                .setObfuscated(false)
                .build(new File(wasm ? "build/dist/wasm" : "build/dist/web"));
    }
}
