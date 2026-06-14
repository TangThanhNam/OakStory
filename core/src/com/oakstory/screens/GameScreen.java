package com.oakstory.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.oakstory.OakStoryGame;
import com.oakstory.entities.Player;
import com.oakstory.items.Icons;
import com.oakstory.items.Inventory;
import com.oakstory.items.Pickup;
import com.oakstory.items.ResourceType;
import com.oakstory.ui.TouchPad;

/**
 * The gameplay screen. Loads the Level 1 Tiled map, runs the player, lets the
 * player collect resource pickups and shows the inventory HUD. The crafting
 * menu and level transition are added on top of this.
 */
public class GameScreen extends ScreenAdapter {

    /** Visible area in pixels. 40 tiles wide with a 16:9 landscape ratio. */
    private static final float VIEW_WIDTH = 640f;
    private static final float VIEW_HEIGHT = 360f;
    private static final float TILE = 16f;
    private static final int KEY_ICON_GID = 516;

    private final OakStoryGame game;
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final OrthographicCamera hudCamera;
    private final Viewport hudViewport;

    private final TiledMap map;
    private final OrthogonalTiledMapRenderer mapRenderer;
    private final TiledMapTileLayer groundLayer;
    private final float mapWidthPx, mapHeightPx;

    private final Player player;
    private final float spawnX, spawnY;

    private final Icons icons;
    private final Inventory inventory = new Inventory();
    private final Array<Pickup> pickups = new Array<>();

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final TouchPad touchPad = new TouchPad();

    public GameScreen(OakStoryGame game) {
        this.game = game;

        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEW_WIDTH, VIEW_HEIGHT, camera);
        hudCamera = new OrthographicCamera();
        hudViewport = new FitViewport(VIEW_WIDTH, VIEW_HEIGHT, hudCamera);

        map = new TmxMapLoader().load("maps/forest.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map, game.batch);
        groundLayer = (TiledMapTileLayer) map.getLayers().get("ground");
        mapWidthPx = groundLayer.getWidth() * TILE;
        mapHeightPx = groundLayer.getHeight() * TILE;

        spawnX = 2 * TILE;
        spawnY = 6 * TILE;
        player = new Player(spawnX, spawnY, mapWidthPx);

        icons = new Icons();
        spawnPickups();
    }

    /** Scatters resources across the ground and platforms. */
    private void spawnPickups() {
        pickups.add(new Pickup(ResourceType.MUSHROOM, 96, 64));
        pickups.add(new Pickup(ResourceType.HERB, 144, 64));
        pickups.add(new Pickup(ResourceType.FLOWER, 192, 128));   // platform
        pickups.add(new Pickup(ResourceType.HERB, 240, 64));
        pickups.add(new Pickup(ResourceType.MUSHROOM, 416, 192)); // high platform
        pickups.add(new Pickup(ResourceType.FLOWER, 496, 112));   // platform
        pickups.add(new Pickup(ResourceType.HERB, 704, 64));
        pickups.add(new Pickup(ResourceType.MUSHROOM, 800, 64));
        pickups.add(new Pickup(ResourceType.HERB, 832, 64));
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

        ScreenUtils.clear(0.45f, 0.70f, 0.86f, 1f);
        camera.update();
        mapRenderer.setView(camera);
        mapRenderer.render();

        // World-space sprites.
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        for (Pickup p : pickups) {
            if (!p.collected) game.batch.draw(icons.get(p.type.gid), p.x, p.y, Pickup.SIZE, Pickup.SIZE);
        }
        player.render(game.batch);
        game.batch.end();

        // HUD + on-screen controls.
        hudCamera.update();
        shapes.setProjectionMatrix(hudCamera.combined);
        touchPad.drawButtons(shapes);

        game.batch.setProjectionMatrix(hudCamera.combined);
        game.batch.begin();
        drawHud();
        touchPad.drawLabels(game.batch, game.font);
        game.batch.end();
    }

    private void update(float delta) {
        boolean left = Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A);
        boolean right = Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D);
        boolean jump = Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                || Gdx.input.isKeyJustPressed(Input.Keys.UP)
                || Gdx.input.isKeyJustPressed(Input.Keys.W);

        // On-screen touch controls (also clickable with the mouse on desktop).
        touchPad.poll(hudViewport);
        left |= touchPad.left();
        right |= touchPad.right();
        jump |= touchPad.jumpJustPressed();

        player.update(delta, groundLayer, left, right, jump);

        // Collect any pickup the player overlaps.
        for (Pickup p : pickups) {
            if (p.collected) continue;
            if (overlaps(player.x, player.y, Player.WIDTH, Player.HEIGHT, p.x, p.y, Pickup.SIZE, Pickup.SIZE)) {
                p.collected = true;
                inventory.add(p.type);
            }
        }

        if (player.getFeetY() < -Player.HEIGHT) {
            player.x = spawnX;
            player.y = spawnY;
        }

        float camX = MathUtils.clamp(player.x, VIEW_WIDTH / 2f, mapWidthPx - VIEW_WIDTH / 2f);
        float camY = MathUtils.clamp(player.y, VIEW_HEIGHT / 2f, mapHeightPx - VIEW_HEIGHT / 2f);
        camera.position.set(camX, camY, 0);
    }

    private void drawHud() {
        float pad = 8, iconSize = 20, y = VIEW_HEIGHT - pad - iconSize, x = pad;
        for (ResourceType t : ResourceType.values()) {
            game.batch.draw(icons.get(t.gid), x, y, iconSize, iconSize);
            game.font.getData().setScale(0.9f);
            game.font.draw(game.batch, "x" + inventory.count(t), x + iconSize + 2, y + iconSize - 4);
            x += iconSize + 36;
        }
        if (inventory.hasKey()) {
            game.batch.draw(icons.get(KEY_ICON_GID), x, y, iconSize, iconSize);
        }
        game.font.getData().setScale(0.8f);
        game.font.draw(game.batch, "C: craft", VIEW_WIDTH - 58, 16);
    }

    private static boolean overlaps(float ax, float ay, float aw, float ah,
                                    float bx, float by, float bw, float bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
        hudViewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        map.dispose();
        mapRenderer.dispose();
        player.dispose();
        icons.dispose();
        shapes.dispose();
    }
}
