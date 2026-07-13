package com.github.czyzby.websocket.examples;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;

/** Calculates demo UI scale across backends that report either logical points or back-buffer pixels. */
final class DemoUiScale {
    static final int BASE_WIDTH = 800;
    static final int BASE_HEIGHT = 480;

    private DemoUiScale() {
    }

    static float calculate(final int width, final int height) {
        final float scale = getBackBufferScale();
        if (isPixelBackedIOS(width, height, scale)) {
            return scale * calculateLogicalScale(width / scale, height / scale);
        }
        return calculateLogicalScale(width, height);
    }

    private static float calculateLogicalScale(final float width, final float height) {
        return Math.max(1f, Math.min(width / BASE_WIDTH, height / BASE_HEIGHT));
    }

    private static boolean isPixelBackedIOS(final int width, final int height, final float scale) {
        return Gdx.app != null
                && Gdx.app.getType() == Application.ApplicationType.iOS
                && Gdx.graphics != null
                && scale > 1f
                && Gdx.graphics.getBackBufferWidth() == width
                && Gdx.graphics.getBackBufferHeight() == height;
    }

    private static float getBackBufferScale() {
        return Gdx.graphics == null ? 1f : Math.max(1f, Gdx.graphics.getBackBufferScale());
    }
}
