package com.oakstory.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.oakstory.OakStoryGame;
import com.oakstory.audio.Audio;
import com.oakstory.entities.Player;
import com.oakstory.items.Icons;
import com.oakstory.items.Inventory;
import com.oakstory.items.Pickup;
import com.oakstory.items.ResourceType;
import com.oakstory.ui.CraftingMenu;
import com.oakstory.ui.TouchPad;

/**
 * The gameplay screen, used for both levels. Loads the level's Tiled map, runs
 * the player, handles resource pickups, crafting and the level goal: a locked
 * chest the player reaches. On level 1 the chest needs the crafted Key and leads
 * to level 2 (the cave); on level 2 it ends the game with a win.
 */
public class GameScreen extends ScreenAdapter {

    /** Visible area in pixels. 40 tiles wide with a 16:9 landscape ratio. */
    private static final float VIEW_WIDTH = 640f;
    private static final float VIEW_HEIGHT = 360f;
    private static final float TILE = 16f;
    private static final int KEY_ICON_GID = 516;
    private static final int CHEST_GID = 444; // top-left tile of the 2x2 chest
    private static final float CHEST_SIZE = 32f;
    private static final float DOOR_W = 31f, DOOR_H = 47f;
    private static final int START_LIVES = 3;

    private final OakStoryGame game;
    private final int level;

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
    private final TextureRegion chest;

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final TouchPad touchPad = new TouchPad();
    private final CraftingMenu menu = new CraftingMenu();
    private final Rectangle craftOpenBtn = new Rectangle(544, 320, 88, 30);
    private final Vector3 tmp = new Vector3();
    private final GlyphLayout layout = new GlyphLayout();

    private final float bgR, bgG, bgB;
    private final float goalX, goalY;
    private final boolean finalLevel;
    private final Texture doorTex;
    private final float doorX, doorY;
    private boolean chestOpened;
    private boolean showKeyHint;
    private boolean lockedSoundPlayed; // so the "locked" sound fires once per visit, not every frame
    private int lives = START_LIVES;

    public GameScreen(OakStoryGame game, int level) {
        this.game = game;
        this.level = level;

        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEW_WIDTH, VIEW_HEIGHT, camera);
        hudCamera = new OrthographicCamera();
        hudViewport = new FitViewport(VIEW_WIDTH, VIEW_HEIGHT, hudCamera);

        map = new TmxMapLoader().load(level == 1 ? "maps/forest.tmx" : "maps/cave.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map, game.batch);
        groundLayer = (TiledMapTileLayer) map.getLayers().get("ground");
        mapWidthPx = groundLayer.getWidth() * TILE;
        mapHeightPx = groundLayer.getHeight() * TILE;

        spawnX = 2 * TILE;
        spawnY = 6 * TILE;
        player = new Player(spawnX, spawnY, mapWidthPx);

        icons = new Icons();
        chest = icons.block(CHEST_GID, 2, 2);

        finalLevel = (level == 2);
        if (level == 1) {
            bgR = 0.45f; bgG = 0.70f; bgB = 0.86f; // sky
            goalX = 56 * TILE; goalY = 4 * TILE;
            spawnForestPickups();
        } else {
            bgR = 0.12f; bgG = 0.10f; bgB = 0.16f; // dark cave
            goalX = 54 * TILE; goalY = 4 * TILE;
            spawnCavePickups();
        }

        doorTex = new Texture(Gdx.files.internal("props/door.png"));
        doorTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        doorX = goalX + 36;
        doorY = 4 * TILE;

        Audio.playTheme(level); // forest or cave loop; switches automatically on the level-2 transition
    }

    private void spawnForestPickups() {
        pickups.add(new Pickup(ResourceType.MUSHROOM, 96, 64));
        pickups.add(new Pickup(ResourceType.HERB, 144, 64));
        pickups.add(new Pickup(ResourceType.FLOWER, 192, 128));
        pickups.add(new Pickup(ResourceType.HERB, 240, 64));
        pickups.add(new Pickup(ResourceType.MUSHROOM, 416, 192));
        pickups.add(new Pickup(ResourceType.FLOWER, 496, 112));
        pickups.add(new Pickup(ResourceType.HERB, 704, 64));
        pickups.add(new Pickup(ResourceType.MUSHROOM, 800, 64));
        pickups.add(new Pickup(ResourceType.HERB, 832, 64));
    }

    private void spawnCavePickups() {
        pickups.add(new Pickup(ResourceType.HERB, 240, 112));   // platform row21
        pickups.add(new Pickup(ResourceType.MUSHROOM, 432, 160)); // platform row18
        pickups.add(new Pickup(ResourceType.FLOWER, 592, 128));   // platform row20
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                || Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            if (menu.isOpen()) {
                menu.close();
            } else {
                Audio.stopMusic();
                game.setScreen(new TitleScreen(game));
                dispose();
                return;
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) menu.toggle();

        if (update(delta)) return; // a level transition happened; stop using this screen

        ScreenUtils.clear(bgR, bgG, bgB, 1f);
        camera.update();
        mapRenderer.setView(camera);
        mapRenderer.render();

        // World-space sprites.
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        for (Pickup p : pickups) {
            if (!p.collected) game.batch.draw(icons.get(p.type.gid), p.x, p.y, Pickup.SIZE, Pickup.SIZE);
        }
        game.batch.draw(chest, goalX, goalY, CHEST_SIZE, CHEST_SIZE);
        if (chestOpened && !finalLevel) game.batch.draw(doorTex, doorX, doorY, DOOR_W, DOOR_H);
        player.render(game.batch);
        game.batch.end();

        // HUD + on-screen controls (or the crafting menu when it is open).
        hudCamera.update();
        shapes.setProjectionMatrix(hudCamera.combined);
        game.batch.setProjectionMatrix(hudCamera.combined);

        if (menu.isOpen()) {
            menu.render(shapes, game.batch, game.font, icons, inventory);
        } else {
            touchPad.drawButtons(shapes);
            drawCraftButtonBg();
            game.batch.begin();
            drawHud();
            touchPad.drawLabels(game.batch, game.font);
            drawCraftButtonLabel();
            if (showKeyHint) drawCenteredHud("Locked!  Craft a Key first.", VIEW_HEIGHT - 44);
            else if (chestOpened && !finalLevel) drawCenteredHud("The door is open!  Walk into it.", VIEW_HEIGHT - 44);
            game.batch.end();
        }
    }

    /** @return true if the screen transitioned (caller must stop rendering this screen). */
    private boolean update(float delta) {
        // When the crafting menu is open, gameplay freezes and we only read the menu.
        if (menu.isOpen()) {
            if (Gdx.input.justTouched()) {
                tmp.set(Gdx.input.getX(), Gdx.input.getY(), 0);
                hudViewport.unproject(tmp);
                menu.handleTap(tmp.x, tmp.y, inventory);
            }
            menu.handleKeys(inventory);
            return false;
        }

        boolean left = Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A);
        boolean right = Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D);
        boolean jump = Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                || Gdx.input.isKeyJustPressed(Input.Keys.UP)
                || Gdx.input.isKeyJustPressed(Input.Keys.W);

        touchPad.poll(hudViewport);
        left |= touchPad.left();
        right |= touchPad.right();
        jump |= touchPad.jumpJustPressed();

        if (Gdx.input.justTouched()) {
            tmp.set(Gdx.input.getX(), Gdx.input.getY(), 0);
            hudViewport.unproject(tmp);
            if (craftOpenBtn.contains(tmp.x, tmp.y)) menu.open();
        }

        player.update(delta, groundLayer, left, right, jump);
        if (player.justJumped) Audio.playJump();

        for (Pickup p : pickups) {
            if (p.collected) continue;
            if (overlaps(player.x, player.y, Player.WIDTH, Player.HEIGHT, p.x, p.y, Pickup.SIZE, Pickup.SIZE)) {
                p.collected = true;
                inventory.add(p.type);
                Audio.playPickup();
            }
        }

        // Goal: a chest. On the final level it ends the game; otherwise opening it
        // (with the Key) reveals a door the player then walks into to descend.
        showKeyHint = false;
        boolean atChest = overlaps(player.x, player.y, Player.WIDTH, Player.HEIGHT, goalX, goalY, CHEST_SIZE, CHEST_SIZE);
        if (finalLevel) {
            if (atChest) {
                Audio.playWin();
                game.setScreen(new WinScreen(game));
                dispose();
                return true;
            }
        } else if (!chestOpened) {
            if (atChest) {
                if (inventory.hasKey()) {
                    chestOpened = true;
                    Audio.playDoor();
                } else {
                    showKeyHint = true;
                    if (!lockedSoundPlayed) { Audio.playLocked(); lockedSoundPlayed = true; }
                }
            } else {
                lockedSoundPlayed = false; // re-arm once the player steps away from the chest
            }
        } else if (overlaps(player.x, player.y, Player.WIDTH, Player.HEIGHT, doorX, doorY, DOOR_W, DOOR_H)) {
            game.setScreen(new GameScreen(game, 2));
            dispose();
            return true;
        }

        // Falling into a pit costs a life. At zero lives the run ends.
        if (player.getFeetY() < -Player.HEIGHT) {
            Audio.playHurt();
            lives--;
            if (lives <= 0) {
                Audio.playGameOver();
                game.setScreen(new GameOverScreen(game, level));
                dispose();
                return true;
            }
            player.x = spawnX;
            player.y = spawnY;
        }

        float camX = MathUtils.clamp(player.x, VIEW_WIDTH / 2f, mapWidthPx - VIEW_WIDTH / 2f);
        float camY = MathUtils.clamp(player.y, VIEW_HEIGHT / 2f, mapHeightPx - VIEW_HEIGHT / 2f);
        camera.position.set(camX, camY, 0);
        return false;
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

        // Lives, shown top-centre so they stay clear of the resource and CRAFT HUD.
        game.font.getData().setScale(1.0f);
        layout.setText(game.font, "Lives x" + lives);
        game.font.draw(game.batch, "Lives x" + lives, VIEW_WIDTH / 2f - layout.width / 2f, VIEW_HEIGHT - 8);
    }

    private void drawCenteredHud(String text, float y) {
        game.font.getData().setScale(1.0f);
        layout.setText(game.font, text);
        game.font.draw(game.batch, text, VIEW_WIDTH / 2f - layout.width / 2f, y);
    }

    private void drawCraftButtonBg() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.20f, 0.45f, 0.55f, 0.85f);
        shapes.rect(craftOpenBtn.x, craftOpenBtn.y, craftOpenBtn.width, craftOpenBtn.height);
        shapes.end();
    }

    private void drawCraftButtonLabel() {
        game.font.getData().setScale(0.9f);
        layout.setText(game.font, "CRAFT");
        game.font.draw(game.batch, "CRAFT",
                craftOpenBtn.x + craftOpenBtn.width / 2f - layout.width / 2f,
                craftOpenBtn.y + craftOpenBtn.height / 2f + layout.height / 2f);
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
        doorTex.dispose();
        shapes.dispose();
    }
}
