import com.badlogic.gdx.ApplicationListener;
import com.github.czyzby.websocket.IOSWebSockets;
import com.github.czyzby.websocket.examples.InsecurePerMessageDeflateWebSocketDemo;
import com.github.czyzby.websocket.examples.PerMessageDeflateWebSocketDemo;
import com.github.czyzby.websocket.examples.WebSocketDemoSelector;
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
        new IOSApplication(createDemoSelector(), config);
    }

    private static ApplicationListener createDemoSelector() {
        return WebSocketDemoSelector.createDefaultSelectorWithLocalWss(
                new WebSocketDemoSelector.DemoFactory() {
                    @Override
                    public ApplicationListener create() {
                        return new PerMessageDeflateWebSocketDemo(
                                PerMessageDeflateWebSocketDemo.DEFAULT_PMDEFLATE_ENDPOINT, true);
                    }
                },
                new WebSocketDemoSelector.DemoFactory() {
                    @Override
                    public ApplicationListener create() {
                        return new InsecurePerMessageDeflateWebSocketDemo(
                                PerMessageDeflateWebSocketDemo.DEFAULT_SECURE_PMDEFLATE_ENDPOINT, true);
                    }
                });
    }
}
