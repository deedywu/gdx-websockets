import com.badlogic.gdx.ApplicationListener;
import com.github.czyzby.websocket.AndroidWebSockets;
import com.github.czyzby.websocket.WebSocket;
import com.github.czyzby.websocket.examples.PerMessageDeflateWebSocketDemo;
import com.github.czyzby.websocket.examples.WebSocketDemo;
import com.github.czyzby.websocket.impl.AndroidWebSocket;
import com.github.xpenatan.gdx.teavm.backends.android.AndroidApplication;
import com.github.xpenatan.gdx.teavm.backends.android.AndroidApplicationConfiguration;

public class WebSocketsAndroidLauncher {

    public static void main(String[] args) {
        AndroidWebSockets.initiate();

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        new AndroidApplication(createPerMessageDeflateDemo(), config);
    }

    private static ApplicationListener createPerMessageDeflateDemo() {
        // Use the original shared demo for a normal wss endpoint test.
        // return new WebSocketDemo();

        // Use the permessage-deflate demo for local ws://host-machine-ip:8787/ testing.
        return new PerMessageDeflateWebSocketDemo("ws://host-machine-ip:8787/", true) {
            @Override
            public void render() {
                TeaVMAndroidTextInput.update();
                super.render();
            }

            @Override
            protected void promptForEndpointAddress() {
                TeaVMAndroidTextInput.show("WebSocket Address", getEndpointSettingsAddress(),
                        "host-machine-ip:8787/", new TeaVMAndroidTextInput.Listener() {
                            @Override
                            public void input(final String text) {
                                setEndpointSettingsAddress(text);
                            }

                            @Override
                            public void canceled() {
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
}
