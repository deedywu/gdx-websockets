package com.github.czyzby.websocket.examples;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.TextInputListener;
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

    private static final int DEFAULT_WIDTH = DemoUiScale.BASE_WIDTH;
    private static final int DEFAULT_HEIGHT = DemoUiScale.BASE_HEIGHT;
    private static final int MAX_LOG_LINES = 10;
    private static final int MAX_INPUT_LENGTH = 120;
    private static final int MAX_ENDPOINT_LENGTH = 200;
    private static final int BUTTON_NONE = 0;
    private static final int BUTTON_SEND = 1;
    private static final int BUTTON_RECONNECT = 2;
    private static final int BUTTON_CLEAR = 3;
    private static final int BUTTON_SETTINGS = 4;
    private static final int BUTTON_ENDPOINT_WS = 5;
    private static final int BUTTON_ENDPOINT_WSS = 6;
    private static final int BUTTON_ENDPOINT_APPLY = 7;
    private static final int BUTTON_ENDPOINT_CANCEL = 8;
    private static final int BUTTON_ENDPOINT_INPUT = 9;
    private static final int BUTTON_BACK = 10;

    private final Array<String> logLines = new Array<String>();
    private final boolean touchControlsEnabled;
    private final StringBuilder inputBuffer = new StringBuilder("Hello server");
    private final Rectangle sendBounds = new Rectangle();
    private final Rectangle reconnectBounds = new Rectangle();
    private final Rectangle clearBounds = new Rectangle();
    private final Rectangle settingsBounds = new Rectangle();
    private final Rectangle backBounds = new Rectangle();
    private final Rectangle settingsPanelBounds = new Rectangle();
    private final Rectangle endpointWsBounds = new Rectangle();
    private final Rectangle endpointWssBounds = new Rectangle();
    private final Rectangle endpointApplyBounds = new Rectangle();
    private final Rectangle endpointCancelBounds = new Rectangle();
    private final Rectangle endpointInputBounds = new Rectangle();
    private final GlyphLayout glyphLayout = new GlyphLayout();
    private final StringBuilder endpointSettingsAddressBuffer = new StringBuilder();

    private SpriteBatch batch;
    private BitmapFont font;
    private ShapeRenderer shapeRenderer;
    private WebSocket socket;
    private String endpoint;
    private String status = "Idle";
    private String lastMessage = "No messages yet";
    private String helperText;
    private BackHandler backHandler;
    private int width = DEFAULT_WIDTH;
    private int height = DEFAULT_HEIGHT;
    private float uiScale = 1f;
    private float bottomUiReservedHeight;
    private int hoveredButton = BUTTON_NONE;
    private int pressedButton = BUTTON_NONE;
    private boolean endpointSettingsOpen;
    private boolean endpointSettingsSecure;

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
                ? "Tap buttons below or type with a hardware keyboard. Settings edits the endpoint."
                : "Type text, press Enter to send, F2 settings, F5 reconnect, Esc clear";
    }

    public final void setBackHandler(final BackHandler backHandler) {
        this.backHandler = backHandler;
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

        try {
            socket = createSocket(endpoint);
            configureSocket(socket);
            socket.addListener(new WebSocketAdapter() {
                @Override
                public boolean onOpen(final WebSocket webSocket) {
                    if (!isCurrentSocket(webSocket)) {
                        return NOT_HANDLED;
                    }
                    status = "Open";
                    log("Connected");
                    onSocketOpen(webSocket);
                    sendMessage("Hello from gdx-websockets @ " + TimeUtils.millis());
                    return NOT_HANDLED;
                }

                @Override
                public boolean onClose(final WebSocket webSocket, final int closeCode, final String reason) {
                    if (!isCurrentSocket(webSocket)) {
                        return NOT_HANDLED;
                    }
                    status = "Closed (" + closeCode + ")";
                    log("Closed: " + normalizeReason(reason));
                    return NOT_HANDLED;
                }

                @Override
                public boolean onMessage(final WebSocket webSocket, final String packet) {
                    if (!isCurrentSocket(webSocket)) {
                        return NOT_HANDLED;
                    }
                    lastMessage = packet;
                    log("Received: " + packet);
                    return NOT_HANDLED;
                }

                @Override
                public boolean onError(final WebSocket webSocket, final Throwable error) {
                    if (!isCurrentSocket(webSocket)) {
                        return NOT_HANDLED;
                    }
                    final String message = describeError(error);
                    status = "Error";
                    lastMessage = "Error: " + message;
                    log("Error: " + message);
                    return NOT_HANDLED;
                }
            });
            socket.connect();
        } catch (final Exception exception) {
            handleConnectionFailure(exception);
        }
    }

    private boolean isCurrentSocket(final WebSocket webSocket) {
        return webSocket == socket;
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
        try {
            socket.send(trimmed);
            log("Sent: " + trimmed);
            return true;
        } catch (final Exception exception) {
            log("Send failed: " + describeError(exception));
            return false;
        }
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

    private void appendEndpointChar(final char character) {
        if (character < 32 || character == 127) {
            return;
        }
        if (endpointSettingsAddressBuffer.length() >= MAX_ENDPOINT_LENGTH) {
            log("Endpoint input limit reached");
            return;
        }
        endpointSettingsAddressBuffer.append(character);
    }

    private void deleteEndpointChar() {
        if (endpointSettingsAddressBuffer.length() > 0) {
            endpointSettingsAddressBuffer.deleteCharAt(endpointSettingsAddressBuffer.length() - 1);
        }
    }

    private String normalizeReason(final String reason) {
        return reason == null || reason.isEmpty() ? "no reason" : reason;
    }

    private void handleConnectionFailure(final Throwable error) {
        final String message = "Connection failed: " + describeError(error);
        status = "Error";
        lastMessage = message;
        log(message);
        closeSocket();
    }

    private static String describeError(final Throwable error) {
        if (error == null) {
            return "unknown error";
        }
        final String message = normalizeErrorMessage(error.getMessage());
        final Throwable cause = error.getCause();
        final String causeMessage = cause == null ? "" : normalizeErrorMessage(cause.getMessage());
        if (message.length() == 0) {
            return causeMessage.length() == 0 ? error.toString() : causeMessage;
        }
        if (causeMessage.length() > 0 && !causeMessage.equals(message)) {
            return message + ": " + causeMessage;
        }
        return message;
    }

    private static String normalizeErrorMessage(final String message) {
        return message == null ? "" : message.trim();
    }

    private void openEndpointSettings() {
        endpointSettingsOpen = true;
        pressedButton = BUTTON_NONE;
        hoveredButton = BUTTON_NONE;
        populateEndpointSettingsFromEndpoint();
    }

    private void cancelEndpointSettings() {
        endpointSettingsOpen = false;
        pressedButton = BUTTON_NONE;
        hoveredButton = BUTTON_NONE;
    }

    private boolean applyEndpointSettings() {
        final String address = endpointSettingsAddressBuffer.toString().trim();
        if (address.isEmpty()) {
            log("Endpoint address cannot be empty");
            return false;
        }

        endpoint = buildEndpoint(endpointSettingsSecure, address);
        endpointSettingsOpen = false;
        pressedButton = BUTTON_NONE;
        hoveredButton = BUTTON_NONE;
        log("Endpoint updated to " + endpoint);
        connect();
        return true;
    }

    private void populateEndpointSettingsFromEndpoint() {
        String normalizedEndpoint = endpoint == null || endpoint.isEmpty() ? DEFAULT_URL : endpoint.trim();
        final String lowercaseEndpoint = normalizedEndpoint.toLowerCase();
        if (lowercaseEndpoint.startsWith("wss://")) {
            endpointSettingsSecure = true;
            normalizedEndpoint = normalizedEndpoint.substring(6);
        } else if (lowercaseEndpoint.startsWith("ws://")) {
            endpointSettingsSecure = false;
            normalizedEndpoint = normalizedEndpoint.substring(5);
        } else {
            endpointSettingsSecure = true;
        }

        endpointSettingsAddressBuffer.setLength(0);
        endpointSettingsAddressBuffer.append(normalizedEndpoint);
    }

    private static String buildEndpoint(final boolean secure, final String address) {
        String normalizedAddress = address.trim();
        final String lowercaseAddress = normalizedAddress.toLowerCase();
        if (lowercaseAddress.startsWith("wss://")) {
            normalizedAddress = normalizedAddress.substring(6);
        } else if (lowercaseAddress.startsWith("ws://")) {
            normalizedAddress = normalizedAddress.substring(5);
        }
        return (secure ? "wss://" : "ws://") + normalizedAddress;
    }

    protected void promptForEndpointAddress() {
        Gdx.input.getTextInput(new TextInputListener() {
            @Override
            public void input(final String text) {
                if (text == null) {
                    return;
                }
                setEndpointSettingsAddress(text);
            }

            @Override
            public void canceled() {
            }
        }, "WebSocket Address", endpointSettingsAddressBuffer.toString(), "127.0.0.1:8787/");
    }

    protected final String getEndpointSettingsAddress() {
        return endpointSettingsAddressBuffer.toString();
    }

    protected final void setEndpointSettingsAddress(final String address) {
        endpointSettingsAddressBuffer.setLength(0);
        if (address != null) {
            endpointSettingsAddressBuffer.append(address.trim());
        }
    }

    protected WebSocket createSocket(final String socketEndpoint) {
        return WebSockets.newSocket(socketEndpoint);
    }

    protected void configureSocket(final WebSocket webSocket) {
    }

    protected void onSocketOpen(final WebSocket webSocket) {
    }

    protected final void log(final String message) {
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
            uiScale = DemoUiScale.calculate(this.width, this.height);
            font.getData().setScale(uiScale);
        }
        if (touchControlsEnabled) {
            final float margin = 24f * uiScale;
            final float gap = 14f * uiScale;
            final float buttonHeight = 52f * uiScale;
            final int buttonCount = backHandler == null ? 4 : 5;
            final float buttonWidth = (this.width - margin * 2f - gap * (buttonCount - 1)) / buttonCount;
            final float y = margin;
            bottomUiReservedHeight = margin * 2f + buttonHeight;

            sendBounds.set(margin, y, buttonWidth, buttonHeight);
            reconnectBounds.set(margin + buttonWidth + gap, y, buttonWidth, buttonHeight);
            clearBounds.set(margin + (buttonWidth + gap) * 2f, y, buttonWidth, buttonHeight);
            settingsBounds.set(margin + (buttonWidth + gap) * 3f, y, buttonWidth, buttonHeight);
            if (backHandler == null) {
                backBounds.set(0f, 0f, 0f, 0f);
            } else {
                backBounds.set(margin + (buttonWidth + gap) * 4f, y, buttonWidth, buttonHeight);
            }

            final float panelWidth = Math.min(this.width - margin * 2f, 620f * uiScale);
            final float panelHeight = Math.min(this.height - margin * 3f, 320f * uiScale);
            final float panelX = (this.width - panelWidth) * 0.5f;
            final float panelY = (this.height - panelHeight) * 0.5f;
            settingsPanelBounds.set(panelX, panelY, panelWidth, panelHeight);

            final float panelMargin = 22f * uiScale;
            final float toggleWidth = 92f * uiScale;
            final float toggleHeight = 42f * uiScale;
            final float actionWidth = 120f * uiScale;
            final float actionHeight = 46f * uiScale;
            final float actionY = panelY + panelMargin;
            final float inputY = actionY + actionHeight + 28f * uiScale;
            final float toggleY = inputY + 90f * uiScale;

            endpointWsBounds.set(panelX + panelMargin, toggleY, toggleWidth, toggleHeight);
            endpointWssBounds.set(panelX + panelMargin + toggleWidth + gap, toggleY, toggleWidth, toggleHeight);

            endpointInputBounds.set(panelX + panelMargin, inputY, panelWidth - panelMargin * 2f, 52f * uiScale);

            endpointCancelBounds.set(panelX + panelWidth - panelMargin - actionWidth * 2f - gap, actionY,
                    actionWidth, actionHeight);
            endpointApplyBounds.set(panelX + panelWidth - panelMargin - actionWidth, actionY,
                    actionWidth, actionHeight);
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
            drawButtonBackground(settingsBounds, BUTTON_SETTINGS);
            if (backHandler != null) {
                drawButtonBackground(backBounds, BUTTON_BACK);
            }
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
            drawButtonLabel("Settings", settingsBounds);
            if (backHandler != null) {
                drawButtonLabel("Back", backBounds);
            }
        }
        batch.end();

        if (endpointSettingsOpen) {
            if (touchControlsEnabled && shapeRenderer != null) {
                shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                drawSettingsOverlay();
                shapeRenderer.end();
            }

            batch.begin();
            drawSettingsOverlayText();
            batch.end();
        }
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

    private void drawSettingsOverlay() {
        shapeRenderer.setColor(0.10f, 0.13f, 0.18f, 1f);
        shapeRenderer.rect(settingsPanelBounds.x, settingsPanelBounds.y, settingsPanelBounds.width, settingsPanelBounds.height);

        drawSettingsToggle(endpointWsBounds, BUTTON_ENDPOINT_WS, !endpointSettingsSecure);
        drawSettingsToggle(endpointWssBounds, BUTTON_ENDPOINT_WSS, endpointSettingsSecure);
        drawSettingsInputBackground();
        drawButtonBackground(endpointCancelBounds, BUTTON_ENDPOINT_CANCEL);
        drawButtonBackground(endpointApplyBounds, BUTTON_ENDPOINT_APPLY);
    }

    private void drawSettingsToggle(final Rectangle bounds, final int buttonType, final boolean selected) {
        if (pressedButton == buttonType) {
            shapeRenderer.setColor(0.27f, 0.44f, 0.62f, 1f);
        } else if (selected) {
            shapeRenderer.setColor(0.17f, 0.36f, 0.55f, 1f);
        } else if (hoveredButton == buttonType) {
            shapeRenderer.setColor(0.20f, 0.28f, 0.38f, 1f);
        } else {
            shapeRenderer.setColor(0.14f, 0.18f, 0.24f, 1f);
        }
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    private void drawSettingsInputBackground() {
        if (hoveredButton == BUTTON_ENDPOINT_INPUT) {
            shapeRenderer.setColor(0.17f, 0.24f, 0.33f, 1f);
        } else {
            shapeRenderer.setColor(0.12f, 0.16f, 0.22f, 1f);
        }
        shapeRenderer.rect(endpointInputBounds.x, endpointInputBounds.y, endpointInputBounds.width, endpointInputBounds.height);
    }

    private void drawSettingsOverlayText() {
        final float panelMargin = 20f * uiScale;
        final float panelX = settingsPanelBounds.x + panelMargin;
        final float lineHeight = font.getLineHeight() + 8f * uiScale;
        final float titleBaseline = settingsPanelBounds.y + settingsPanelBounds.height - 28f * uiScale;
        final float descriptionBaseline = titleBaseline - 34f * uiScale;
        final float schemeLabelBaseline = endpointWsBounds.y + endpointWsBounds.height + 18f * uiScale;
        final float addressLabelBaseline = endpointInputBounds.y + endpointInputBounds.height + 18f * uiScale;
        final float hintBaseline = endpointInputBounds.y - 14f * uiScale;

        font.setColor(Color.WHITE);
        font.draw(batch, "Connection Settings", panelX, titleBaseline);
        font.draw(batch, "Choose ws or wss, then edit host:port/path.", panelX, descriptionBaseline);
        font.draw(batch, "Scheme", panelX, schemeLabelBaseline);
        drawButtonLabel("ws", endpointWsBounds);
        drawButtonLabel("wss", endpointWssBounds);

        font.setColor(Color.WHITE);
        font.draw(batch, "Address", panelX, addressLabelBaseline);

        if (endpointSettingsAddressBuffer.length() > 0) {
            font.setColor(Color.valueOf("9CE5FF"));
            font.draw(batch, endpointSettingsAddressBuffer.toString(), endpointInputBounds.x + 12f * uiScale,
                    endpointInputBounds.y + endpointInputBounds.height * 0.68f);
        }

        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "Enter applies, Esc cancels, Tab toggles scheme.", panelX, hintBaseline);

        font.setColor(Color.WHITE);
        drawButtonLabel("Cancel", endpointCancelBounds);
        drawButtonLabel("Apply", endpointApplyBounds);
    }

    private int getButtonAt(final float touchX, final float touchY) {
        if (endpointSettingsOpen) {
            if (endpointWsBounds.contains(touchX, touchY)) {
                return BUTTON_ENDPOINT_WS;
            }
            if (endpointWssBounds.contains(touchX, touchY)) {
                return BUTTON_ENDPOINT_WSS;
            }
            if (endpointInputBounds.contains(touchX, touchY)) {
                return BUTTON_ENDPOINT_INPUT;
            }
            if (endpointApplyBounds.contains(touchX, touchY)) {
                return BUTTON_ENDPOINT_APPLY;
            }
            if (endpointCancelBounds.contains(touchX, touchY)) {
                return BUTTON_ENDPOINT_CANCEL;
            }
            return BUTTON_NONE;
        }
        if (sendBounds.contains(touchX, touchY)) {
            return BUTTON_SEND;
        }
        if (reconnectBounds.contains(touchX, touchY)) {
            return BUTTON_RECONNECT;
        }
        if (clearBounds.contains(touchX, touchY)) {
            return BUTTON_CLEAR;
        }
        if (settingsBounds.contains(touchX, touchY)) {
            return BUTTON_SETTINGS;
        }
        if (backHandler != null && backBounds.contains(touchX, touchY)) {
            return BUTTON_BACK;
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
            case BUTTON_SETTINGS:
                openEndpointSettings();
                return true;
            case BUTTON_BACK:
                if (backHandler != null) {
                    backHandler.back();
                    return true;
                }
                return false;
            case BUTTON_ENDPOINT_WS:
                endpointSettingsSecure = false;
                return true;
            case BUTTON_ENDPOINT_WSS:
                endpointSettingsSecure = true;
                return true;
            case BUTTON_ENDPOINT_INPUT:
                promptForEndpointAddress();
                return true;
            case BUTTON_ENDPOINT_APPLY:
                return applyEndpointSettings();
            case BUTTON_ENDPOINT_CANCEL:
                cancelEndpointSettings();
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
        if (keycode == Input.Keys.F2) {
            if (endpointSettingsOpen) {
                cancelEndpointSettings();
            } else {
                openEndpointSettings();
            }
            return true;
        }

        if (endpointSettingsOpen) {
            if (keycode == Input.Keys.ESCAPE) {
                cancelEndpointSettings();
                return true;
            }
            if (keycode == Input.Keys.TAB) {
                endpointSettingsSecure = !endpointSettingsSecure;
                return true;
            }
            return false;
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

    @Override
    public boolean keyTyped(final char character) {
        if (endpointSettingsOpen) {
            if (character == '\r' || character == '\n') {
                return applyEndpointSettings();
            }
            if (character == '\b') {
                deleteEndpointChar();
                return true;
            }
            appendEndpointChar(character);
            return true;
        }
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

    public interface BackHandler {
        void back();
    }
}
