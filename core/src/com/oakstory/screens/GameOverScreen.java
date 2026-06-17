package com.oakstory.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.oakstory.OakStoryGame;
import com.oakstory.audio.Audio;

/**
 * Shown when the player runs out of health. Offers two touch buttons: Retry
 * (restart the level that was being played) and Title (back to the start screen).
 */
public class GameOverScreen extends MenuScreen {

    private final int level;
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final Vector3 tmp = new Vector3();

    private final Rectangle retryBtn = new Rectangle(OakStoryGame.WORLD_WIDTH / 2f - 90, 150, 180, 46);
    private final Rectangle titleBtn = new Rectangle(OakStoryGame.WORLD_WIDTH / 2f - 90, 90, 180, 46);

    public GameOverScreen(OakStoryGame game, int level) {
        super(game);
        this.level = level;
        Audio.playGameOver();
    }

    @Override
    public void render(float delta) {
        if (handleInput()) return; // a button was pressed; this screen is now disposed

        ScreenUtils.clear(0.12f, 0.06f, 0.07f, 1f);
        viewport.apply();

        shapes.setProjectionMatrix(camera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.45f, 0.20f, 0.20f, 0.95f);
        shapes.rect(retryBtn.x, retryBtn.y, retryBtn.width, retryBtn.height);
        shapes.setColor(0.30f, 0.32f, 0.40f, 0.95f);
        shapes.rect(titleBtn.x, titleBtn.y, titleBtn.width, titleBtn.height);
        shapes.end();

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        drawCentered("Game Over", OakStoryGame.WORLD_HEIGHT * 0.70f, 2.5f);
        drawCentered("You ran out of health.", OakStoryGame.WORLD_HEIGHT * 0.56f, 0.9f);
        drawCenteredAt("Retry", retryBtn.x + retryBtn.width / 2f, retryBtn.y + retryBtn.height / 2f, 1.3f);
        drawCenteredAt("Title", titleBtn.x + titleBtn.width / 2f, titleBtn.y + titleBtn.height / 2f, 1.1f);
        game.batch.end();
    }

    /** @return true if a button was pressed and a new screen was set (stop rendering this one). */
    private boolean handleInput() {
        if (tapped(retryBtn) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            game.setScreen(new GameScreen(game, level));
            dispose();
            return true;
        }
        if (tapped(titleBtn) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Audio.stopMusic();
            game.setScreen(new TitleScreen(game));
            dispose();
            return true;
        }
        return false;
    }

    /** True if a tap this frame landed inside the given button. */
    private boolean tapped(Rectangle button) {
        if (!Gdx.input.justTouched()) return false;
        tmp.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(tmp);
        return button.contains(tmp.x, tmp.y);
    }

    @Override
    public void dispose() {
        shapes.dispose();
    }
}
