package com.oakstory.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.ScreenUtils;
import com.oakstory.OakStoryGame;

/**
 * The gameplay screen. For now this is a placeholder that confirms screen
 * switching works; the tile map, player and crafting are added in later steps.
 */
public class GameScreen extends MenuScreen {

    public GameScreen(OakStoryGame game) {
        super(game);
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                || Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            game.setScreen(new TitleScreen(game));
            dispose();
        }

        ScreenUtils.clear(0.42f, 0.60f, 0.36f, 1f);
        viewport.apply();
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        drawCentered("Level 1 - Forest", OakStoryGame.WORLD_HEIGHT * 0.55f, 1.4f);
        drawCentered("(gameplay coming next)", OakStoryGame.WORLD_HEIGHT * 0.48f, 0.9f);
        drawCentered("ESC / Back - title", OakStoryGame.WORLD_HEIGHT * 0.10f, 0.8f);
        game.batch.end();
    }
}
