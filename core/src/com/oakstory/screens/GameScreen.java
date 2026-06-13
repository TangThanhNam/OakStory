package com.oakstory.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.oakstory.OakStoryGame;

/**
 * The gameplay screen. Loads the Level 1 Tiled map and renders it through a
 * camera. The player, collision and crafting are added on top of this in the
 * following steps.
 */
public class GameScreen extends ScreenAdapter {

    /** Visible area in pixels. Roughly 16 tiles wide with a 9:16 portrait ratio. */
    private static final float VIEW_WIDTH = 256f;
    private static final float VIEW_HEIGHT = 455f;

    private final OakStoryGame game;
    private final OrthographicCamera camera;
    private final Viewport viewport;

    private final TiledMap map;
    private final OrthogonalTiledMapRenderer mapRenderer;

    public GameScreen(OakStoryGame game) {
        this.game = game;

        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEW_WIDTH, VIEW_HEIGHT, camera);

        map = new TmxMapLoader().load("maps/forest.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map, game.batch);

        // Start looking at the bottom-left of the map (where the player will spawn).
        camera.position.set(VIEW_WIDTH / 2f, VIEW_HEIGHT / 2f, 0);
        camera.update();
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                || Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            game.setScreen(new TitleScreen(game));
            dispose();
            return;
        }

        ScreenUtils.clear(0.45f, 0.70f, 0.86f, 1f); // sky blue
        camera.update();
        mapRenderer.setView(camera);
        mapRenderer.render();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
    }

    @Override
    public void dispose() {
        map.dispose();
        mapRenderer.dispose();
    }
}
