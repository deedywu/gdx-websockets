package com.github.czyzby.websocket.examples;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;

/** Shared entry screen that lets users choose which websocket demo to run. */
public class WebSocketDemoSelector extends ApplicationAdapter implements InputProcessor {
    private static final int OPTION_NONE = -1;

    private final Array<DemoOption> options = new Array<DemoOption>();
    private final Array<Rectangle> optionBounds = new Array<Rectangle>();
    private final GlyphLayout glyphLayout = new GlyphLayout();

    private SpriteBatch batch;
    private BitmapFont font;
    private ShapeRenderer shapeRenderer;
    private ApplicationListener activeDemo;
    private int width = DemoUiScale.BASE_WIDTH;
    private int height = DemoUiScale.BASE_HEIGHT;
    private float uiScale = 1f;
    private int hoveredOption = OPTION_NONE;
    private int pressedOption = OPTION_NONE;
    private boolean selectorDisposed;

    public WebSocketDemoSelector(final DemoOption... demoOptions) {
        if (demoOptions != null) {
            for (final DemoOption option : demoOptions) {
                if (option != null) {
                    options.add(option);
                    optionBounds.add(new Rectangle());
                }
            }
        }
    }

    public static WebSocketDemoSelector createDefaultSelector() {
        return createDefaultSelector(PerMessageDeflateWebSocketDemo.DEFAULT_PMDEFLATE_ENDPOINT);
    }

    public static WebSocketDemoSelector createDefaultSelector(final String localPerMessageDeflateEndpoint) {
        return createDefaultSelector(localPerMessageDeflateEndpoint,
                perMessageDeflateFactory(localPerMessageDeflateEndpoint, true));
    }

    public static WebSocketDemoSelector createDefaultSelector(final DemoFactory localPerMessageDeflateFactory) {
        return createDefaultSelector(PerMessageDeflateWebSocketDemo.DEFAULT_PMDEFLATE_ENDPOINT,
                localPerMessageDeflateFactory);
    }

    public static WebSocketDemoSelector createDefaultSelector(final String localPerMessageDeflateEndpoint,
            final DemoFactory localPerMessageDeflateFactory) {
        return createDefaultSelector(normalWssEchoFactory(true), localPerMessageDeflateEndpoint,
                localPerMessageDeflateFactory);
    }

    public static WebSocketDemoSelector createDefaultSelector(final DemoFactory normalWssEchoFactory,
            final String localPerMessageDeflateEndpoint, final DemoFactory localPerMessageDeflateFactory) {
        return new WebSocketDemoSelector(
                normalWssEcho(normalWssEchoFactory),
                localPerMessageDeflate(localPerMessageDeflateEndpoint, localPerMessageDeflateFactory));
    }

    public static WebSocketDemoSelector createDefaultSelectorWithLocalWss(
            final DemoFactory localPerMessageDeflateFactory,
            final DemoFactory localSecurePerMessageDeflateFactory) {
        return createDefaultSelectorWithLocalWss(normalWssEchoFactory(true),
                PerMessageDeflateWebSocketDemo.DEFAULT_PMDEFLATE_ENDPOINT, localPerMessageDeflateFactory,
                PerMessageDeflateWebSocketDemo.DEFAULT_SECURE_PMDEFLATE_ENDPOINT,
                localSecurePerMessageDeflateFactory);
    }

    public static WebSocketDemoSelector createDefaultSelectorWithLocalWss(final DemoFactory normalWssEchoFactory,
            final String localPerMessageDeflateEndpoint, final DemoFactory localPerMessageDeflateFactory,
            final String localSecurePerMessageDeflateEndpoint,
            final DemoFactory localSecurePerMessageDeflateFactory) {
        return new WebSocketDemoSelector(
                normalWssEcho(normalWssEchoFactory),
                localPerMessageDeflate(localPerMessageDeflateEndpoint, localPerMessageDeflateFactory),
                localSecurePerMessageDeflate(localSecurePerMessageDeflateEndpoint,
                        localSecurePerMessageDeflateFactory));
    }

    public static DemoFactory normalWssEchoFactory(final boolean touchControlsEnabled) {
        return new DemoFactory() {
            @Override
            public ApplicationListener create() {
                return new WebSocketDemo(WebSocketDemo.DEFAULT_URL, touchControlsEnabled);
            }
        };
    }

    public static DemoFactory perMessageDeflateFactory(final String endpoint, final boolean touchControlsEnabled) {
        return new DemoFactory() {
            @Override
            public ApplicationListener create() {
                return new PerMessageDeflateWebSocketDemo(endpoint, touchControlsEnabled);
            }
        };
    }

    public static DemoOption normalWssEcho(final boolean touchControlsEnabled) {
        return normalWssEcho(normalWssEchoFactory(touchControlsEnabled));
    }

    public static DemoOption normalWssEcho(final DemoFactory factory) {
        return new DemoOption("Normal WSS Echo", WebSocketDemo.DEFAULT_URL, factory);
    }

    public static DemoOption localPerMessageDeflate(final String endpoint, final DemoFactory factory) {
        return new DemoOption("Local permessage-deflate", endpoint, factory);
    }

    public static DemoOption localSecurePerMessageDeflate(final String endpoint, final DemoFactory factory) {
        return new DemoOption("Local WSS permessage-deflate", endpoint, factory);
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        shapeRenderer = new ShapeRenderer();
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void resize(final int width, final int height) {
        if (activeDemo != null) {
            activeDemo.resize(width, height);
            return;
        }

        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        if (font != null) {
            uiScale = DemoUiScale.calculate(this.width, this.height);
            font.getData().setScale(uiScale);
        }
        layoutOptions();
    }

    private void layoutOptions() {
        if (optionBounds.size == 0) {
            return;
        }

        final float margin = 32f * uiScale;
        final float gap = 16f * uiScale;
        final float optionHeight = 86f * uiScale;
        final float optionWidth = Math.min(width - margin * 2f, 620f * uiScale);
        final float totalHeight = optionHeight * optionBounds.size + gap * (optionBounds.size - 1);
        final float startX = (width - optionWidth) * 0.5f;
        final float startY = Math.max(margin, (height - totalHeight) * 0.5f - 18f * uiScale);

        for (int index = 0; index < optionBounds.size; index++) {
            final float y = startY + (optionBounds.size - 1 - index) * (optionHeight + gap);
            optionBounds.get(index).set(startX, y, optionWidth, optionHeight);
        }
    }

    @Override
    public void render() {
        if (activeDemo != null) {
            activeDemo.render();
            return;
        }

        ScreenUtils.clear(0.08f, 0.1f, 0.14f, 1f);

        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int index = 0; index < optionBounds.size; index++) {
            drawOptionBackground(optionBounds.get(index), index);
        }
        shapeRenderer.end();

        batch.begin();
        drawTitle();
        for (int index = 0; index < options.size; index++) {
            drawOptionText(options.get(index), optionBounds.get(index));
        }
        batch.end();
    }

    private void drawTitle() {
        final float x = width * 0.5f;
        final float y = height - 54f * uiScale;
        font.setColor(Color.WHITE);
        drawCentered("gdx-websockets demos", x, y);
        font.setColor(Color.LIGHT_GRAY);
        drawCentered("Choose a websocket scenario to run.", x, y - 34f * uiScale);
    }

    private void drawOptionBackground(final Rectangle bounds, final int optionIndex) {
        if (pressedOption == optionIndex) {
            shapeRenderer.setColor(0.27f, 0.44f, 0.62f, 1f);
        } else if (hoveredOption == optionIndex) {
            shapeRenderer.setColor(0.20f, 0.28f, 0.38f, 1f);
        } else {
            shapeRenderer.setColor(0.14f, 0.18f, 0.24f, 1f);
        }
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    private void drawOptionText(final DemoOption option, final Rectangle bounds) {
        final float textX = bounds.x + 24f * uiScale;
        final float titleY = bounds.y + bounds.height - 26f * uiScale;
        final float descriptionY = titleY - 32f * uiScale;

        font.setColor(Color.WHITE);
        font.draw(batch, option.title, textX, titleY);
        font.setColor(Color.valueOf("9CE5FF"));
        font.draw(batch, option.description, textX, descriptionY);
    }

    private void drawCentered(final String text, final float centerX, final float y) {
        glyphLayout.setText(font, text);
        font.draw(batch, text, centerX - glyphLayout.width * 0.5f, y);
    }

    private int getOptionAt(final float touchX, final float touchY) {
        for (int index = 0; index < optionBounds.size; index++) {
            if (optionBounds.get(index).contains(touchX, touchY)) {
                return index;
            }
        }
        return OPTION_NONE;
    }

    private boolean selectOption(final int optionIndex) {
        if (optionIndex < 0 || optionIndex >= options.size || activeDemo != null) {
            return false;
        }

        final ApplicationListener demo = options.get(optionIndex).factory.create();
        if (demo == null) {
            return false;
        }
        if (demo instanceof WebSocketDemo) {
            ((WebSocketDemo) demo).setBackHandler(new WebSocketDemo.BackHandler() {
                @Override
                public void back() {
                    requestReturnToSelector();
                }
            });
        }

        activeDemo = demo;
        activeDemo.create();
        activeDemo.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        return true;
    }

    private void requestReturnToSelector() {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                returnToSelector();
            }
        });
    }

    private void returnToSelector() {
        if (activeDemo == null) {
            return;
        }

        final ApplicationListener demo = activeDemo;
        activeDemo = null;
        try {
            demo.dispose();
        } finally {
            pressedOption = OPTION_NONE;
            hoveredOption = OPTION_NONE;
            resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            Gdx.input.setInputProcessor(this);
        }
    }

    private void disposeSelectorResources() {
        if (selectorDisposed) {
            return;
        }
        selectorDisposed = true;
        if (Gdx.input.getInputProcessor() == this) {
            Gdx.input.setInputProcessor(null);
        }
        if (font != null) {
            font.dispose();
        }
        if (batch != null) {
            batch.dispose();
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }

    @Override
    public void pause() {
        if (activeDemo != null) {
            activeDemo.pause();
        }
    }

    @Override
    public void resume() {
        if (activeDemo != null) {
            activeDemo.resume();
        }
    }

    @Override
    public void dispose() {
        if (activeDemo != null) {
            activeDemo.dispose();
        }
        disposeSelectorResources();
    }

    @Override
    public boolean keyDown(final int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(final int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(final char character) {
        if (character >= '1' && character <= '9') {
            return selectOption(character - '1');
        }
        return false;
    }

    @Override
    public boolean touchDown(final int screenX, final int screenY, final int pointer, final int button) {
        final float touchX = screenX;
        final float touchY = height - screenY;
        pressedOption = getOptionAt(touchX, touchY);
        hoveredOption = pressedOption;
        return pressedOption != OPTION_NONE;
    }

    @Override
    public boolean touchUp(final int screenX, final int screenY, final int pointer, final int button) {
        final float touchX = screenX;
        final float touchY = height - screenY;
        final int releasedOption = getOptionAt(touchX, touchY);
        final boolean handled = pressedOption != OPTION_NONE && pressedOption == releasedOption
                && selectOption(releasedOption);
        pressedOption = OPTION_NONE;
        hoveredOption = releasedOption;
        return handled;
    }

    @Override
    public boolean touchCancelled(final int screenX, final int screenY, final int pointer, final int button) {
        pressedOption = OPTION_NONE;
        return false;
    }

    @Override
    public boolean touchDragged(final int screenX, final int screenY, final int pointer) {
        final float touchX = screenX;
        final float touchY = height - screenY;
        hoveredOption = getOptionAt(touchX, touchY);
        if (pressedOption != OPTION_NONE && hoveredOption != pressedOption) {
            pressedOption = OPTION_NONE;
        }
        return hoveredOption != OPTION_NONE;
    }

    @Override
    public boolean mouseMoved(final int screenX, final int screenY) {
        final float touchX = screenX;
        final float touchY = height - screenY;
        hoveredOption = getOptionAt(touchX, touchY);
        return hoveredOption != OPTION_NONE;
    }

    @Override
    public boolean scrolled(final float amountX, final float amountY) {
        return false;
    }

    public interface DemoFactory {
        ApplicationListener create();
    }

    public static class DemoOption {
        private final String title;
        private final String description;
        private final DemoFactory factory;

        public DemoOption(final String title, final String description, final DemoFactory factory) {
            this.title = title;
            this.description = description;
            this.factory = factory;
        }
    }
}
