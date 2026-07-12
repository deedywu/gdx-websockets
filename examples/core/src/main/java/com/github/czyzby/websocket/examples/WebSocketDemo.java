package com.github.czyzby.websocket.examples;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
import com.github.czyzby.websocket.WebSocket;
import com.github.czyzby.websocket.WebSocketAdapter;
import com.github.czyzby.websocket.WebSockets;

/** Interactive websocket example */
public class WebSocketDemo extends ApplicationAdapter implements InputProcessor {
    public static final String DEFAULT_URL = "wss://ws.postman-echo.com/raw";

    private static final int MAX_LOG_LINES = 10;
    private static final int MAX_INPUT_LENGTH = 120;
    private static final int BUTTON_NONE = 0;
    private static final int BUTTON_SEND = 1;
    private static final int BUTTON_RECONNECT = 2;
    private static final int BUTTON_CLEAR = 3;

    private final Array<String> logLines = new Array<String>();
    private final String endpoint;
    private final boolean touchControlsEnabled;
    private final StringBuilder inputBuffer = new StringBuilder("Hello server");
    private final Rectangle sendBounds = new Rectangle();
    private final Rectangle reconnectBounds = new Rectangle();
    private final Rectangle clearBounds = new Rectangle();
    private final GlyphLayout glyphLayout = new GlyphLayout();

    private SpriteBatch batch;
    private BitmapFont font;
    private ShapeRenderer shapeRenderer;
    private WebSocket socket;
    private String status = "Idle";
    private String lastMessage = "No messages yet";
    private String helperText;
    private int width = 800;
    private int height = 480;
    private float uiScale = 1f;
    private float bottomUiReservedHeight;
    private int hoveredButton = BUTTON_NONE;
    private int pressedButton = BUTTON_NONE;

    public WebSocketDemo() {
        this(DEFAULT_URL, true);
    }

    public WebSocketDemo(final String endpoint) {
        this(endpoint, true);
    }

    public WebSocketDemo(final String endpoint, final boolean touchControlsEnabled) {
        this.endpoint = endpoint == null || endpoint.isEmpty() ? DEFAULT_URL : endpoint;
        this.touchControlsEnabled = touchControlsEnabled;
        helperText = touchControlsEnabled
                ? "Tap buttons below or type with a hardware keyboard"
                : "Type text, press Enter to send, F5 reconnect, Esc clear";
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        if (touchControlsEnabled) {
            shapeRenderer = new ShapeRenderer();
        }
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.input.setInputProcessor(this);
        connect();
    }

    private void connect() {
        closeSocket();
        status = "Connecting";
        lastMessage = "Waiting for server response";
        log("Connecting to " + endpoint);

        socket = WebSockets.newSocket(endpoint);
        socket.addListener(new WebSocketAdapter() {
            @Override
            public boolean onOpen(final WebSocket webSocket) {
                status = "Open";
                log("Connected");
                sendMessage("Hello from gdx-websockets @ " + TimeUtils.millis());
                return NOT_HANDLED;
            }

            @Override
            public boolean onClose(final WebSocket webSocket, final int closeCode, final String reason) {
                status = "Closed (" + closeCode + ")";
                log("Closed: " + normalizeReason(reason));
                return NOT_HANDLED;
            }

            @Override
            public boolean onMessage(final WebSocket webSocket, final String packet) {
                lastMessage = packet;
                log("Received: " + packet);
                return NOT_HANDLED;
            }

            @Override
            public boolean onError(final WebSocket webSocket, final Throwable error) {
                status = "Error";
                log("Error: " + (error == null ? "unknown" : error.getMessage()));
                return NOT_HANDLED;
            }
        });
        socket.connect();
    }

    private boolean sendMessage(final String message) {
        final String trimmed = message == null ? "" : message.trim();
        if (trimmed.isEmpty()) {
            log("Nothing to send");
            return false;
        }
        if (socket == null) {
            log("Socket not created yet");
            return false;
        }
        if (!socket.isOpen()) {
            log("Socket is not open yet");
            return false;
        }
        socket.send(trimmed);
        log("Sent: " + trimmed);
        return true;
    }

    private void sendTypedMessage() {
        if (sendMessage(inputBuffer.toString())) {
            inputBuffer.setLength(0);
        }
    }

    private void appendTypedChar(final char character) {
        if (character < 32 || character == 127) {
            return;
        }
        if (inputBuffer.length() >= MAX_INPUT_LENGTH) {
            log("Input limit reached");
            return;
        }
        inputBuffer.append(character);
    }

    private void deleteTypedChar() {
        if (inputBuffer.length() > 0) {
            inputBuffer.deleteCharAt(inputBuffer.length() - 1);
        }
    }

    private String normalizeReason(final String reason) {
        return reason == null || reason.isEmpty() ? "no reason" : reason;
    }

    private void log(final String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        if (logLines.size >= MAX_LOG_LINES) {
            logLines.removeIndex(0);
        }
        logLines.add(message);
        Gdx.app.log("WebSocketDemo", message);
    }

    @Override
    public void resize(final int width, final int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        if (font != null) {
            uiScale = Math.max(1f, Math.min(this.width / 800f, this.height / 480f));
            font.getData().setScale(uiScale);
        }
        if (touchControlsEnabled) {
            final float margin = 24f * uiScale;
            final float gap = 14f * uiScale;
            final float buttonHeight = 52f * uiScale;
            final float buttonWidth = (this.width - margin * 2f - gap * 2f) / 3f;
            final float y = margin;
            bottomUiReservedHeight = margin * 2f + buttonHeight;

            sendBounds.set(margin, y, buttonWidth, buttonHeight);
            reconnectBounds.set(margin + buttonWidth + gap, y, buttonWidth, buttonHeight);
            clearBounds.set(margin + (buttonWidth + gap) * 2f, y, buttonWidth, buttonHeight);
        } else {
            bottomUiReservedHeight = 0f;
        }
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.08f, 0.1f, 0.14f, 1f);

        if (touchControlsEnabled && shapeRenderer != null) {
            shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            drawButtonBackground(sendBounds, BUTTON_SEND);
            drawButtonBackground(reconnectBounds, BUTTON_RECONNECT);
            drawButtonBackground(clearBounds, BUTTON_CLEAR);
            shapeRenderer.end();
        }

        batch.begin();
        final float x = 24f * uiScale;
        float y = height - 24f * uiScale;
        final float lineHeight = font.getLineHeight() + 10f * uiScale;

        font.setColor(Color.WHITE);
        font.draw(batch, "gdx-websockets demo", x, y);
        y -= lineHeight;
        font.draw(batch, "Status: " + status, x, y);
        y -= lineHeight;
        font.draw(batch, "Endpoint: " + endpoint, x, y);
        y -= lineHeight;
        font.draw(batch, helperText, x, y);
        y -= lineHeight;
        font.draw(batch, "Last message: " + lastMessage, x, y);
        y -= lineHeight;
        font.setColor(Color.valueOf("9CE5FF"));
        font.draw(batch, "Input > " + inputBuffer, x, y);
        y -= lineHeight * 1.5f;
        font.setColor(Color.WHITE);
        font.draw(batch, "Recent events:", x, y);
        y -= lineHeight;
        final float minLogY = touchControlsEnabled ? bottomUiReservedHeight + lineHeight * 0.5f : 0f;

        for (int i = logLines.size - 1; i >= 0; i--) {
            if (y < minLogY) {
                break;
            }
            font.draw(batch, logLines.get(i), x, y);
            y -= lineHeight;
        }

        if (touchControlsEnabled) {
            drawButtonLabel("Send", sendBounds);
            drawButtonLabel("Reconnect", reconnectBounds);
            drawButtonLabel("Clear", clearBounds);
        }
        batch.end();
    }

    private void drawButtonLabel(final String label, final Rectangle bounds) {
        glyphLayout.setText(font, label);
        final float textX = bounds.x + (bounds.width - glyphLayout.width) * 0.5f;
        final float textY = bounds.y + (bounds.height + glyphLayout.height) * 0.5f;
        font.draw(batch, label, textX, textY);
    }

    private void drawButtonBackground(final Rectangle bounds, final int buttonType) {
        if (pressedButton == buttonType) {
            shapeRenderer.setColor(0.27f, 0.44f, 0.62f, 1f);
        } else if (hoveredButton == buttonType) {
            shapeRenderer.setColor(0.20f, 0.28f, 0.38f, 1f);
        } else {
            shapeRenderer.setColor(0.14f, 0.18f, 0.24f, 1f);
        }
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    private int getButtonAt(final float touchX, final float touchY) {
        if (sendBounds.contains(touchX, touchY)) {
            return BUTTON_SEND;
        }
        if (reconnectBounds.contains(touchX, touchY)) {
            return BUTTON_RECONNECT;
        }
        if (clearBounds.contains(touchX, touchY)) {
            return BUTTON_CLEAR;
        }
        return BUTTON_NONE;
    }

    private boolean triggerButton(final int buttonType) {
        switch (buttonType) {
            case BUTTON_SEND:
                return sendMessage(inputBuffer.toString());
            case BUTTON_RECONNECT:
                connect();
                return true;
            case BUTTON_CLEAR:
                logLines.clear();
                lastMessage = "No messages yet";
                return true;
            default:
                return false;
        }
    }

    private void closeSocket() {
        if (socket == null) {
            return;
        }
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (final Exception ignored) {
            log("Ignoring close error: " + ignored.getMessage());
        }
        socket = null;
    }

    @Override
    public void dispose() {
        closeSocket();
        Gdx.input.setInputProcessor(null);
        font.dispose();
        batch.dispose();
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }

    @Override
    public boolean keyDown(final int keycode) {
        if (touchControlsEnabled) {
            if (keycode == Input.Keys.ENTER) {
                sendTypedMessage();
                return true;
            }
            if (keycode == Input.Keys.BACKSPACE || keycode == Input.Keys.DEL || keycode == Input.Keys.FORWARD_DEL) {
                deleteTypedChar();
                return true;
            }

            final char mappedCharacter = mapKeycodeToCharacter(keycode);
            if (mappedCharacter != 0) {
                appendTypedChar(mappedCharacter);
                return true;
            }
        }
        if (keycode == Input.Keys.ESCAPE) {
            inputBuffer.setLength(0);
            return true;
        }
        if (keycode == Input.Keys.F5) {
            connect();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyUp(final int keycode) {
        return false;
    }

    private char mapKeycodeToCharacter(final int keycode) {
        final boolean shifted = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);

        if (keycode >= Input.Keys.A && keycode <= Input.Keys.Z) {
            final char base = (char) ('a' + (keycode - Input.Keys.A));
            return shifted ? Character.toUpperCase(base) : base;
        }
        if (keycode >= Input.Keys.NUM_0 && keycode <= Input.Keys.NUM_9) {
            final char[] shiftedDigits = {')', '!', '@', '#', '$', '%', '^', '&', '*', '('};
            final int index = keycode - Input.Keys.NUM_0;
            return shifted ? shiftedDigits[index] : (char) ('0' + index);
        }
        if (keycode >= Input.Keys.NUMPAD_0 && keycode <= Input.Keys.NUMPAD_9) {
            return (char) ('0' + (keycode - Input.Keys.NUMPAD_0));
        }

        switch (keycode) {
            case Input.Keys.SPACE:
                return ' ';
            case Input.Keys.COMMA:
                return shifted ? '<' : ',';
            case Input.Keys.PERIOD:
                return shifted ? '>' : '.';
            case Input.Keys.SEMICOLON:
                return shifted ? ':' : ';';
            case Input.Keys.APOSTROPHE:
                return shifted ? '"' : '\'';
            case Input.Keys.SLASH:
                return shifted ? '?' : '/';
            case Input.Keys.BACKSLASH:
                return shifted ? '|' : '\\';
            case Input.Keys.LEFT_BRACKET:
                return shifted ? '{' : '[';
            case Input.Keys.RIGHT_BRACKET:
                return shifted ? '}' : ']';
            case Input.Keys.MINUS:
                return shifted ? '_' : '-';
            case Input.Keys.EQUALS:
                return shifted ? '+' : '=';
            case Input.Keys.GRAVE:
                return shifted ? '~' : '`';
            case Input.Keys.NUMPAD_DIVIDE:
                return '/';
            case Input.Keys.NUMPAD_MULTIPLY:
                return '*';
            case Input.Keys.NUMPAD_SUBTRACT:
                return '-';
            case Input.Keys.NUMPAD_ADD:
                return '+';
            case Input.Keys.NUMPAD_DOT:
                return '.';
            default:
                return 0;
        }
    }

    @Override
    public boolean keyTyped(final char character) {
        if (character == '\r' || character == '\n') {
            sendTypedMessage();
            return true;
        }
        if (character == '\b') {
            deleteTypedChar();
            return true;
        }
        appendTypedChar(character);
        return false;
    }

    @Override
    public boolean touchDown(final int screenX, final int screenY, final int pointer, final int button) {
        if (!touchControlsEnabled) {
            return false;
        }
        final float touchX = screenX;
        final float touchY = height - screenY;
        pressedButton = getButtonAt(touchX, touchY);
        hoveredButton = pressedButton;
        return pressedButton != BUTTON_NONE;
    }

    @Override
    public boolean touchUp(final int screenX, final int screenY, final int pointer, final int button) {
        if (!touchControlsEnabled) {
            return false;
        }
        final float touchX = screenX;
        final float touchY = height - screenY;
        final int releasedButton = getButtonAt(touchX, touchY);
        final boolean handled = pressedButton != BUTTON_NONE && pressedButton == releasedButton && triggerButton(releasedButton);
        pressedButton = BUTTON_NONE;
        hoveredButton = releasedButton;
        return handled;
    }

    @Override
    public boolean touchCancelled(final int screenX, final int screenY, final int pointer, final int button) {
        return false;
    }

    @Override
    public boolean touchDragged(final int screenX, final int screenY, final int pointer) {
        if (!touchControlsEnabled) {
            return false;
        }
        final float touchX = screenX;
        final float touchY = height - screenY;
        hoveredButton = getButtonAt(touchX, touchY);
        if (pressedButton != BUTTON_NONE && hoveredButton != pressedButton) {
            pressedButton = BUTTON_NONE;
        }
        return hoveredButton != BUTTON_NONE;
    }

    @Override
    public boolean mouseMoved(final int screenX, final int screenY) {
        if (!touchControlsEnabled) {
            return false;
        }
        final float touchX = screenX;
        final float touchY = height - screenY;
        hoveredButton = getButtonAt(touchX, touchY);
        return hoveredButton != BUTTON_NONE;
    }

    @Override
    public boolean scrolled(final float amountX, final float amountY) {
        return false;
    }
}
