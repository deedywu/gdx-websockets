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
    private static final String LOCAL_PMDEFLATE_ENDPOINT = "ws://host-machine-ip:8787/";

    public static void main(String[] args) {
        AndroidWebSockets.initiate();

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        new AndroidApplication(createDemoSelector(), config);
    }

    private static ApplicationListener createDemoSelector() {
        return WebSocketDemoSelector.createDefaultSelector(new WebSocketDemoSelector.DemoFactory() {
            @Override
            public ApplicationListener create() {
                return createNormalDemo();
            }
        }, LOCAL_PMDEFLATE_ENDPOINT, new WebSocketDemoSelector.DemoFactory() {
            @Override
            public ApplicationListener create() {
                return createPerMessageDeflateDemo();
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

    private static ApplicationListener createPerMessageDeflateDemo() {
        return new PerMessageDeflateWebSocketDemo(LOCAL_PMDEFLATE_ENDPOINT, true) {
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
                return webSocket instanceof AndroidWebSocket
                        ? ((AndroidWebSocket) webSocket).getAgreedExtensionsDescription()
                        : null;
            }

            @Override
            protected Boolean isPerMessageDeflateNegotiated(final WebSocket webSocket) {
                return webSocket instanceof AndroidWebSocket
                        ? Boolean.valueOf(((AndroidWebSocket) webSocket).isPerMessageDeflateAgreed())
                        : null;
            }
        };
    }

    private static void promptForEndpointAddressWithNativeDialog(final String address,
            final EndpointAddressReceiver receiver) {
        TeaVMAndroidTextInput.show("WebSocket Address", address,
                "host-machine-ip:8787/", new TeaVMAndroidTextInput.Listener() {
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
