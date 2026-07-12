import com.github.czyzby.websocket.IOSWebSockets;
import com.github.czyzby.websocket.examples.WebSocketDemo;
import com.github.xpenatan.gdx.teavm.backends.ios.IOSApplication;
import com.github.xpenatan.gdx.teavm.backends.ios.IOSApplicationConfiguration;
import com.github.xpenatan.gdx.teavm.backends.ios.IOSFiles;

/** TeaVM iOS launcher for the websocket example. */
public class WebSocketsIOSLauncher {
    private static final String TEAVM_IOS_PROGRAM_NAME = "gdx-teavm-ios";

    private WebSocketsIOSLauncher() {
    }

    public static void main(final String[] args) {
        if (args.length > 0) {
            final String assetsPath = args[args.length - 1];
            if (!TEAVM_IOS_PROGRAM_NAME.equals(assetsPath)) {
                IOSFiles.setLocalPath(assetsPath);
            }
        }

        IOSWebSockets.initiate();

        final IOSApplicationConfiguration config = new IOSApplicationConfiguration();
        new IOSApplication(new WebSocketDemo(WebSocketDemo.DEFAULT_URL, true), config);
    }
}
