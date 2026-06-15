package com.oakstory;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.oakstory.audio.Audio;
import com.oakstory.screens.TitleScreen;

/**
 * Entry point of OakStory.
 *
 * <p>Extends {@link Game} so the project can switch between screens (title,
 * gameplay, credits, game-over). A single {@link SpriteBatch} and
 * {@link BitmapFont} are created here and shared by every screen so we are not
 * allocating GPU resources per screen.</p>
 */
public class OakStoryGame extends Game {

    /** Virtual world size used by the UI screens (landscape 16:9); consistent layout across resolutions. */
    public static final float WORLD_WIDTH = 640f;
    public static final float WORLD_HEIGHT = 360f;

    public SpriteBatch batch;
    public BitmapFont font;

    @Override
    public void create() {
        batch = new SpriteBatch();
        // libGDX ships a default 15px bitmap font, so no asset file is needed yet.
        font = new BitmapFont();
        font.setUseIntegerPositions(false);
        Audio.load();
        setScreen(new TitleScreen(this));
    }

    @Override
    public void dispose() {
        if (getScreen() != null) {
            getScreen().dispose();
        }
        batch.dispose();
        font.dispose();
        Audio.dispose();
    }
}
