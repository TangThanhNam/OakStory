package com.oakstory;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;

/**
 * Entry point of the OakStory game.
 *
 * <p>For now this just clears the screen to a forest-green colour so we can
 * confirm the libGDX project builds and runs on both desktop and Android.
 * Screens (title, gameplay, game-over) are added in later steps.</p>
 */
public class OakStoryGame extends ApplicationAdapter {

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.42f, 0.60f, 0.36f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }
}
