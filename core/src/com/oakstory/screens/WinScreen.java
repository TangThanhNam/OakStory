package com.oakstory.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.ScreenUtils;
import com.oakstory.OakStoryGame;
import com.oakstory.audio.Audio;

/** Shown when the player opens the chest in the cave. Tap to return to the title. */
public class WinScreen extends MenuScreen {

    public WinScreen(OakStoryGame game) {
        super(game);
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Audio.stopMusic();
            game.setScreen(new TitleScreen(game));
            dispose();
            return;
        }

        ScreenUtils.clear(0.08f, 0.12f, 0.10f, 1f);
        viewport.apply();
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        float h = OakStoryGame.WORLD_HEIGHT;
        drawCentered("You Win!", h * 0.62f, 2.5f);
        drawCentered("You found the treasure in the cave.", h * 0.45f, 0.9f);
        drawCentered("Tap anywhere to return to the title", h * 0.25f, 0.9f);
        game.batch.end();
    }
}
