import com.badlogic.gdx.ApplicationListener;
import com.github.czyzby.websocket.AndroidWebSockets;
import com.github.czyzby.websocket.WebSocket;
import com.github.czyzby.websocket.examples.PerMessageDeflateWebSocketDemo;
import com.github.czyzby.websocket.examples.WebSocketDemo;
import com.github.czyzby.websocket.examples.WebSocketDemoSelector;
import com.github.czyzby.websocket.impl.AndroidWebSocket;
import com.github.xpenatan.gdx.teavm.backends.android.AndroidApplication;
import com.github.xpenatan.gdx.teavm.backends.android.AndroidApplicationConfiguration;

public class WebSocketsAndroidLauncher {
    private static final String LOCAL_PMDEFLATE_ENDPOINT = "ws://10.0.2.2:8787/";
    private static final String LOCAL_SECURE_PMDEFLATE_ENDPOINT = "wss://10.0.2.2:8787/";

    public static void main(String[] args) {
        AndroidWebSockets.initiate();

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        new AndroidApplication(createDemoSelector(), config);
    }

    private static ApplicationListener createDemoSelector() {
        return WebSocketDemoSelector.createDefaultSelectorWithLocalWss(
                new WebSocketDemoSelector.DemoFactory() {
                    @Override
                    public ApplicationListener create() {
                        return createNormalDemo();
                    }
                },
                LOCAL_PMDEFLATE_ENDPOINT,
                new WebSocketDemoSelector.DemoFactory() {
                    @Override
                    public ApplicationListener create() {
                        return createPerMessageDeflateDemo(LOCAL_PMDEFLATE_ENDPOINT, false);
                    }
                },
                LOCAL_SECURE_PMDEFLATE_ENDPOINT,
                new WebSocketDemoSelector.DemoFactory() {
                    @Override
                    public ApplicationListener create() {
                        return createPerMessageDeflateDemo(LOCAL_SECURE_PMDEFLATE_ENDPOINT, true);
                    }
                });
    }

    private static ApplicationListener createNormalDemo() {
        return new WebSocketDemo(WebSocketDemo.DEFAULT_URL, true) {
            @Override
            public void render() {
                TeaVMAndroidTextInput.update();
                super.render();
            }

            @Override
            protected void promptForEndpointAddress() {
                promptForEndpointAddressWithNativeDialog(getEndpointSettingsAddress(), new EndpointAddressReceiver() {
                    @Override
                    public void input(final String text) {
                        setEndpointSettingsAddress(text);
                    }
                });
            }
        };
    }

    private static ApplicationListener createPerMessageDeflateDemo(final String endpoint, final boolean insecure) {
        return new PerMessageDeflateWebSocketDemo(endpoint, true, insecure) {
            @Override
            public void render() {
                TeaVMAndroidTextInput.update();
                super.render();
            }

            @Override
            protected void promptForEndpointAddress() {
                promptForEndpointAddressWithNativeDialog(getEndpointSettingsAddress(), new EndpointAddressReceiver() {
                    @Override
                    public void input(final String text) {
                        setEndpointSettingsAddress(text);
                    }
                });
            }

            @Override
            protected String getNegotiatedExtensionsDescription(final WebSocket webSocket) {
                return WebSocketsAndroidLauncher.getNegotiatedExtensionsDescription(webSocket);
            }

            @Override
            protected Boolean isPerMessageDeflateNegotiated(final WebSocket webSocket) {
                return WebSocketsAndroidLauncher.isPerMessageDeflateNegotiated(webSocket);
            }
        };
    }

    private static String getNegotiatedExtensionsDescription(final WebSocket webSocket) {
        return webSocket instanceof AndroidWebSocket
                ? ((AndroidWebSocket) webSocket).getAgreedExtensionsDescription()
                : null;
    }

    private static Boolean isPerMessageDeflateNegotiated(final WebSocket webSocket) {
        return webSocket instanceof AndroidWebSocket
                ? Boolean.valueOf(((AndroidWebSocket) webSocket).isPerMessageDeflateAgreed())
                : null;
    }

    private static void promptForEndpointAddressWithNativeDialog(final String address,
            final EndpointAddressReceiver receiver) {
        TeaVMAndroidTextInput.show("WebSocket Address", address,
                "10.0.2.2:8787/", new TeaVMAndroidTextInput.Listener() {
                    @Override
                    public void input(final String text) {
                        receiver.input(text);
                    }

                    @Override
                    public void canceled() {
                    }
                });
    }

    private interface EndpointAddressReceiver {
        void input(String text);
    }
}
