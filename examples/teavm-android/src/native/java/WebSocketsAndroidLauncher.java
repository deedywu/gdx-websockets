import com.github.czyzby.websocket.AndroidWebSockets;
import com.github.czyzby.websocket.examples.WebSocketDemo;
import com.github.xpenatan.gdx.teavm.backends.android.AndroidApplication;
import com.github.xpenatan.gdx.teavm.backends.android.AndroidApplicationConfiguration;

public class WebSocketsAndroidLauncher {
    public static void main(String[] args) {
        AndroidWebSockets.initiate();

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        new AndroidApplication(new WebSocketDemo(WebSocketDemo.DEFAULT_URL, true), config);
    }
}
