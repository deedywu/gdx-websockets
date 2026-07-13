import java.nio.charset.StandardCharsets;

import org.teavm.interop.Address;
import org.teavm.interop.Import;
import org.teavm.interop.c.Include;

@Include("teavm_websocket_android.h")
final class TeaVMAndroidTextInput {
    private static final int STATE_PENDING = 0;
    private static final int STATE_INPUT = 1;
    private static final int RESULT_BUFFER_SIZE = 2048;

    private static final byte[] RESULT_BUFFER = new byte[RESULT_BUFFER_SIZE];

    private static long nextRequestId = 1;
    private static long activeRequestId;
    private static Listener activeListener;

    private TeaVMAndroidTextInput() {
    }

    static void show(final String title, final String text, final String hint, final Listener listener) {
        if (listener == null) {
            return;
        }
        if (activeListener != null) {
            activeListener.canceled();
        }

        final long requestId = nextRequestId++;
        if (showTextInput(requestId, title, text, hint)) {
            activeRequestId = requestId;
            activeListener = listener;
        } else {
            listener.canceled();
        }
    }

    static void update() {
        if (activeListener == null) {
            return;
        }

        final int state = getTextInputState(activeRequestId);
        if (state == STATE_PENDING) {
            return;
        }

        final long requestId = activeRequestId;
        final Listener listener = activeListener;
        activeRequestId = 0;
        activeListener = null;

        final int length = readTextInputResult(requestId, Address.ofData(RESULT_BUFFER), RESULT_BUFFER_SIZE);
        if (state == STATE_INPUT) {
            final int size = Math.max(0, Math.min(length, RESULT_BUFFER_SIZE));
            listener.input(new String(RESULT_BUFFER, 0, size, StandardCharsets.UTF_8));
        } else {
            listener.canceled();
        }
    }

    interface Listener {
        void input(String text);

        void canceled();
    }

    @Import(name = "gdx_teavm_ws_android_text_input_show")
    private static native boolean showTextInput(long requestId, String title, String text, String hint);

    @Import(name = "gdx_teavm_ws_android_text_input_state")
    private static native int getTextInputState(long requestId);

    @Import(name = "gdx_teavm_ws_android_text_input_result")
    private static native int readTextInputResult(long requestId, Address targetBuffer, int targetBufferCapacity);
}
