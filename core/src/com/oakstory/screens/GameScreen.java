package com.oakstory.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.oakstory.OakStoryGame;
import com.oakstory.entities.Player;

/**
 * The gameplay screen. Loads the Level 1 Tiled map, runs the player and follows
 * the player with the camera. The player collides with the "ground" tile layer.
 */
public class GameScreen extends ScreenAdapter {

    /** Visible area in pixels. 30 tiles wide with a 16:9 landscape ratio. */
    private static final float VIEW_WIDTH = 480f;
    private static final float VIEW_HEIGHT = 270f;
    private static final float TILE = 16f;

    private final OakStoryGame game;
    private final OrthographicCamera camera;
    private final Viewport viewport;

    private final TiledMap map;
    private final OrthogonalTiledMapRenderer mapRenderer;
    private final TiledMapTileLayer groundLayer;
    private final float mapWidthPx, mapHeightPx;

    private final Player player;
    private final float spawnX, spawnY;

    public GameScreen(OakStoryGame game) {
        this.game = game;

        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEW_WIDTH, VIEW_HEIGHT, camera);

        map = new TmxMapLoader().load("maps/forest.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map, game.batch);
        groundLayer = (TiledMapTileLayer) map.getLayers().get("ground");
        mapWidthPx = groundLayer.getWidth() * TILE;
        mapHeightPx = groundLayer.getHeight() * TILE;

        spawnX = 2 * TILE;
        spawnY = 6 * TILE; // a little above the ground; falls and lands
        player = new Player(spawnX, spawnY, mapWidthPx);
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                || Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            game.setScreen(new TitleScreen(game));
            dispose();
            return;
        }

        update(delta);

        ScreenUtils.clear(0.45f, 0.70f, 0.86f, 1f); // sky blue
        camera.update();

        mapRenderer.setView(camera);
        mapRenderer.render();

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        player.render(game.batch);
        game.batch.end();
    }

    private void update(float delta) {
        boolean left = Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A);
        boolean right = Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D);
        boolean jump = Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                || Gdx.input.isKeyJustPressed(Input.Keys.UP)
                || Gdx.input.isKeyJustPressed(Input.Keys.W);

        // Touch controls (Android): left third = left, right third = right, middle = jump.
        float screenW = Gdx.graphics.getWidth();
        for (int p = 0; p < 2; p++) {
            if (Gdx.input.isTouched(p)) {
                float tx = Gdx.input.getX(p);
                if (tx < screenW / 3f) left = true;
                else if (tx > screenW * 2f / 3f) right = true;
            }
        }
        if (Gdx.input.justTouched()) {
            float tx = Gdx.input.getX();
            if (tx >= screenW / 3f && tx <= screenW * 2f / 3f) jump = true;
        }

        player.update(delta, groundLayer, left, right, jump);

        // Fell into a pit -> respawn (full death/restart handled in a later step).
        if (player.getFeetY() < -Player.HEIGHT) {
            player.x = spawnX;
            player.y = spawnY;
        }

        // Camera follows the player, clamped to the map bounds.
        float camX = MathUtils.clamp(player.x, VIEW_WIDTH / 2f, mapWidthPx - VIEW_WIDTH / 2f);
        float camY = MathUtils.clamp(player.y, VIEW_HEIGHT / 2f, mapHeightPx - VIEW_HEIGHT / 2f);
        camera.position.set(camX, camY, 0);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
    }

    @Override
    public void dispose() {
        map.dispose();
        mapRenderer.dispose();
        player.dispose();
    }
}
