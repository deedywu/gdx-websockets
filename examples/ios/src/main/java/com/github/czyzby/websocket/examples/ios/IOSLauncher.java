package com.github.czyzby.websocket.examples.ios;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration;
import com.github.czyzby.websocket.CommonWebSockets;
import com.github.czyzby.websocket.WebSocket;
import com.github.czyzby.websocket.examples.PerMessageDeflateWebSocketDemo;
import com.github.czyzby.websocket.examples.WebSocketDemoSelector;
import com.github.czyzby.websocket.impl.NvWebSocket;
import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.uikit.UIApplication;

/** Standard RoboVM MetalANGLE iOS launcher for the websocket example. */
public class IOSLauncher extends IOSApplication.Delegate {
    @Override
    protected IOSApplication createApplication() {
        CommonWebSockets.initiate();

        final IOSApplicationConfiguration configuration = new IOSApplicationConfiguration();
        configuration.orientationLandscape = true;
        configuration.orientationPortrait = true;
        configuration.useAccelerometer = false;
        configuration.useCompass = false;

        return new IOSApplication(createDemoSelector(), configuration);
    }

    private static ApplicationListener createDemoSelector() {
        return WebSocketDemoSelector.createDefaultSelector(new WebSocketDemoSelector.DemoFactory() {
            @Override
            public ApplicationListener create() {
                return createPerMessageDeflateDemo();
            }
        });
    }

    private static ApplicationListener createPerMessageDeflateDemo() {
        return new PerMessageDeflateWebSocketDemo(PerMessageDeflateWebSocketDemo.DEFAULT_PMDEFLATE_ENDPOINT) {
            @Override
            protected String getNegotiatedExtensionsDescription(final WebSocket webSocket) {
                return webSocket instanceof NvWebSocket
                        ? ((NvWebSocket) webSocket).getAgreedExtensionsDescription()
                        : null;
            }

            @Override
            protected Boolean isPerMessageDeflateNegotiated(final WebSocket webSocket) {
                return webSocket instanceof NvWebSocket
                        ? Boolean.valueOf(((NvWebSocket) webSocket).isPerMessageDeflateAgreed())
                        : null;
            }
        };
    }

    public static void main(final String[] args) {
        final NSAutoreleasePool pool = new NSAutoreleasePool();
        UIApplication.main(args, null, IOSLauncher.class);
        pool.close();
    }
}
