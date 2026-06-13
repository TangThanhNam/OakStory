package com.oakstory.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.ScreenUtils;
import com.oakstory.OakStoryGame;

/** The start screen. Shows the title and waits for the player to start or open credits. */
public class TitleScreen extends MenuScreen {

    public TitleScreen(OakStoryGame game) {
        super(game);
    }

    @Override
    public void render(float delta) {
        handleInput();

        ScreenUtils.clear(0.10f, 0.16f, 0.11f, 1f);
        viewport.apply();
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        drawCentered("OakStory", OakStoryGame.WORLD_HEIGHT * 0.66f, 2.5f);
        drawCentered("Tap or press SPACE to play", OakStoryGame.WORLD_HEIGHT * 0.42f, 1.1f);
        drawCentered("C  -  Credits", OakStoryGame.WORLD_HEIGHT * 0.34f, 1.0f);
        game.batch.end();
    }

    private void handleInput() {
        if (Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            game.setScreen(new GameScreen(game));
            dispose();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            game.setScreen(new CreditsScreen(game));
            dispose();
        }
    }
}
