package com.oakstory.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.oakstory.OakStoryGame;

/**
 * Shared base for the simple text menu screens (title, credits, game-over).
 *
 * <p>Holds a {@link FitViewport} so the menus look the same on any screen
 * resolution, and offers a small helper for drawing horizontally-centred text.</p>
 */
abstract class MenuScreen extends ScreenAdapter {

    protected final OakStoryGame game;
    protected final OrthographicCamera camera;
    protected final Viewport viewport;
    private final GlyphLayout layout = new GlyphLayout();

    protected MenuScreen(OakStoryGame game) {
        this.game = game;
        camera = new OrthographicCamera();
        viewport = new FitViewport(OakStoryGame.WORLD_WIDTH, OakStoryGame.WORLD_HEIGHT, camera);
    }

    /**
     * Draws a line of text centred on the X axis.
     *
     * @param text  the text to draw
     * @param y     vertical position of the text baseline in world units
     * @param scale font scale multiplier
     */
    protected void drawCentered(String text, float y, float scale) {
        game.font.getData().setScale(scale);
        layout.setText(game.font, text);
        game.font.draw(game.batch, text, (OakStoryGame.WORLD_WIDTH - layout.width) / 2f, y);
    }

    /** Draws text centred on a point, e.g. inside a button. */
    protected void drawCenteredAt(String text, float cx, float cy, float scale) {
        game.font.getData().setScale(scale);
        layout.setText(game.font, text);
        game.font.draw(game.batch, text, cx - layout.width / 2f, cy + layout.height / 2f);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }
}
