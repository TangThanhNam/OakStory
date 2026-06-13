package com.oakstory.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.ScreenUtils;
import com.oakstory.OakStoryGame;

/**
 * Lists asset attributions and licensing, as required when using third-party
 * assets. Full details are also kept in {@code docs/ASSETS.md}.
 */
public class CreditsScreen extends MenuScreen {

    public CreditsScreen(OakStoryGame game) {
        super(game);
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                || Gdx.input.isKeyJustPressed(Input.Keys.BACK)
                || Gdx.input.justTouched()) {
            game.setScreen(new TitleScreen(game));
            dispose();
        }

        ScreenUtils.clear(0.10f, 0.16f, 0.11f, 1f);
        viewport.apply();
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        float h = OakStoryGame.WORLD_HEIGHT;
        drawCentered("Credits", h * 0.80f, 2.0f);
        drawCentered("Game engine: libGDX (Apache 2.0)", h * 0.62f, 0.9f);
        drawCentered("Forest art: anokolisa (itch.io, free)", h * 0.56f, 0.9f);
        drawCentered("Sound & music: see docs/ASSETS.md", h * 0.50f, 0.9f);
        drawCentered("Made by the OakStory team", h * 0.40f, 0.9f);
        drawCentered("Tap or press ESC to go back", h * 0.20f, 0.9f);
        game.batch.end();
    }
}
