package com.oakstory.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.oakstory.OakStoryGame;

/** The start screen: a title and two touch buttons (Play, Credits). */
public class TitleScreen extends MenuScreen {

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final Vector3 tmp = new Vector3();

    private final Rectangle playBtn = new Rectangle(OakStoryGame.WORLD_WIDTH / 2f - 90, 150, 180, 46);
    private final Rectangle creditsBtn = new Rectangle(OakStoryGame.WORLD_WIDTH / 2f - 90, 90, 180, 46);

    public TitleScreen(OakStoryGame game) {
        super(game);
    }

    @Override
    public void render(float delta) {
        if (handleInput()) return; // a button was pressed; this screen is now disposed

        ScreenUtils.clear(0.10f, 0.16f, 0.11f, 1f);
        viewport.apply();

        shapes.setProjectionMatrix(camera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.20f, 0.50f, 0.24f, 0.95f);
        shapes.rect(playBtn.x, playBtn.y, playBtn.width, playBtn.height);
        shapes.setColor(0.30f, 0.32f, 0.40f, 0.95f);
        shapes.rect(creditsBtn.x, creditsBtn.y, creditsBtn.width, creditsBtn.height);
        shapes.end();

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        drawCentered("OakStory", OakStoryGame.WORLD_HEIGHT * 0.72f, 2.5f);
        drawCenteredAt("Play", playBtn.x + playBtn.width / 2f, playBtn.y + playBtn.height / 2f, 1.3f);
        drawCenteredAt("Credits", creditsBtn.x + creditsBtn.width / 2f, creditsBtn.y + creditsBtn.height / 2f, 1.1f);
        game.batch.end();
    }

    /** @return true if a button was pressed and a new screen was set (stop rendering this one). */
    private boolean handleInput() {
        if (tapped(playBtn) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            game.setScreen(new GameScreen(game, 1));
            dispose();
            return true;
        }
        if (tapped(creditsBtn) || Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            game.setScreen(new CreditsScreen(game));
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
