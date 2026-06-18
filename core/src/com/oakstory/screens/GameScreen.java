package com.oakstory.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
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
import com.oakstory.entities.Boss;
import com.oakstory.entities.Enemy;
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
    private static final int CONTACT_DMG = 25; // health lost from a boar touch or a pit fall
    private static final int ATTACK_DMG = 1;   // damage a stomp or swing deals to a boar

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
    private final Array<Enemy> enemies = new Array<>();
    private final Texture boarTex, boarHitTex;
    private final Animation<TextureRegion> boarWalk, boarVanish;
    private final TextureRegion chest;

    private final Texture skyTex;
    private final Texture[] treeFar, treeNear; // parallax background tree layers (forest)
    private final Texture[] caveRocks;         // stalagmites/stalactites (cave)
    private final Color caveTop = new Color(0.05f, 0.04f, 0.08f, 1f);
    private final Color caveBottom = new Color(0.17f, 0.12f, 0.20f, 1f);
    private final Color mtnFar = new Color(0.52f, 0.64f, 0.64f, 1f);  // hazy distant range
    private final Color mtnNear = new Color(0.34f, 0.47f, 0.45f, 1f); // closer, darker range

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final TouchPad touchPad = new TouchPad();
    private final CraftingMenu menu = new CraftingMenu();
    private final Rectangle craftOpenBtn = new Rectangle(544, 320, 88, 30);
    private final Vector3 tmp = new Vector3();
    private final GlyphLayout layout = new GlyphLayout();

    private final float bgR, bgG, bgB;
    private final float goalX, goalY;
    private final boolean caveStyle;  // dark cave background (levels 2 and 3)
    private final boolean hasChest;   // a key-gated chest goal (level 1 only)
    private final boolean bossLevel;  // the boss arena (level 3)
    private final int nextLevel;      // level the door leads to (-1 if none)
    private final Texture doorTex;
    private final float doorX, doorY;
    private boolean chestOpened;
    private boolean showKeyHint;
    private boolean lockedSoundPlayed; // so the "locked" sound fires once per visit, not every frame
    private final Rectangle attackBox = new Rectangle();

    private final Texture snailWalkTex, snailHideTex, snailDeadTex;
    private Boss boss; // only on the boss level

    public GameScreen(OakStoryGame game, int level) {
        this.game = game;
        this.level = level;

        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEW_WIDTH, VIEW_HEIGHT, camera);
        hudCamera = new OrthographicCamera();
        hudViewport = new FitViewport(VIEW_WIDTH, VIEW_HEIGHT, hudCamera);

        String mapFile = level == 1 ? "maps/forest.tmx" : level == 2 ? "maps/cave.tmx" : "maps/boss.tmx";
        map = new TmxMapLoader().load(mapFile);
        mapRenderer = new OrthogonalTiledMapRenderer(map, game.batch);
        groundLayer = (TiledMapTileLayer) map.getLayers().get("ground");
        mapWidthPx = groundLayer.getWidth() * TILE;
        mapHeightPx = groundLayer.getHeight() * TILE;

        spawnX = 2 * TILE;
        spawnY = 6 * TILE;
        player = new Player(spawnX, spawnY, mapWidthPx);

        icons = new Icons();
        chest = icons.block(CHEST_GID, 2, 2);

        // Boar enemy: a single shared 48x32, 6-frame walk animation reused by every boar.
        boarTex = new Texture(Gdx.files.internal("enemy/boar_walk.png"));
        boarTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        boarWalk = new Animation<>(0.12f, TextureRegion.split(boarTex, 48, 32)[0]);
        boarWalk.setPlayMode(Animation.PlayMode.LOOP);
        boarHitTex = new Texture(Gdx.files.internal("enemy/boar_hit.png"));
        boarHitTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        boarVanish = new Animation<>(0.08f, TextureRegion.split(boarHitTex, 48, 32)[0]); // 4-frame poof

        // Boss snail: 48x32, 8-frame walk and death sheets, drawn enlarged.
        snailWalkTex = new Texture(Gdx.files.internal("enemy/snail_walk.png"));
        snailWalkTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        snailHideTex = new Texture(Gdx.files.internal("enemy/snail_hide.png"));
        snailHideTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        snailDeadTex = new Texture(Gdx.files.internal("enemy/snail_dead.png"));
        snailDeadTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        // Parallax background art. Trees are large and drawn shrunk, so use linear
        // filtering to avoid shimmer. tree2 is the dark silhouette (far layer).
        skyTex = loadLinear("bg/sky.png");
        Texture t1 = loadLinear("props/tree1.png");
        Texture t2 = loadLinear("props/tree2.png");
        Texture t3 = loadLinear("props/tree3.png");
        treeFar = new Texture[]{t2};
        treeNear = new Texture[]{t1, t3};
        caveRocks = new Texture[]{loadLinear("props/rock1.png"), loadLinear("props/rock2.png"), loadLinear("props/rock3.png")};

        caveStyle = (level >= 2);
        hasChest = (level == 1);
        bossLevel = (level == 3);
        nextLevel = (level < 3) ? level + 1 : -1;
        if (level == 1) {
            bgR = 0.45f; bgG = 0.70f; bgB = 0.86f; // sky
            goalX = 56 * TILE; goalY = 4 * TILE;
            spawnForestPickups();
            // Four boars on clean flat ground (clear of the plank platforms, or they
            // would spawn clipping a plank and hang in the air). Each patrols a stretch
            // bounded by pits / the hill / the world edge and turns around on its own.
            enemies.add(new Enemy(128, 80, true, boarWalk, boarVanish));  // opening stretch
            enemies.add(new Enemy(560, 80, true, boarWalk, boarVanish));  // between the two pits
            enemies.add(new Enemy(690, 80, false, boarWalk, boarVanish)); // between pit and hill
            enemies.add(new Enemy(832, 80, false, boarWalk, boarVanish)); // past the hill, near the goal
        } else if (level == 2) {
            bgR = 0.12f; bgG = 0.10f; bgB = 0.16f; // dark cave
            goalX = 54 * TILE; goalY = 4 * TILE;
            spawnCavePickups();
            // Three boars on flat cave floor, away from the gaps and plank columns.
            enemies.add(new Enemy(144, 80, true, boarWalk, boarVanish));  // first hall
            enemies.add(new Enemy(496, 80, false, boarWalk, boarVanish)); // between the two gaps
            enemies.add(new Enemy(800, 80, true, boarWalk, boarVanish));  // near the goal
        } else {
            bgR = 0.10f; bgG = 0.08f; bgB = 0.13f; // boss cavern
            goalX = 0; goalY = 0; // no chest/door in the boss room
            Animation<TextureRegion> snailWalk = new Animation<>(0.10f, TextureRegion.split(snailWalkTex, 48, 32)[0]);
            snailWalk.setPlayMode(Animation.PlayMode.LOOP);
            Animation<TextureRegion> snailHide = new Animation<>(0.9f / 8f, TextureRegion.split(snailHideTex, 48, 32)[0]);
            Animation<TextureRegion> snailDead = new Animation<>(0.09f, TextureRegion.split(snailDeadTex, 48, 32)[0]);
            boss = new Boss(620, 120, snailWalk, snailHide, snailDead);
        }

        doorTex = new Texture(Gdx.files.internal("props/door.png"));
        doorTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        // Level 1's door appears next to the chest once unlocked; level 2's door is the
        // open exit at the goal. The boss level has no door.
        doorX = hasChest ? goalX + 36 : goalX;
        doorY = 4 * TILE;

        Audio.playTheme(level); // forest (1) or cave (2,3) loop; switches automatically on transitions
    }

    private static Texture loadLinear(String path) {
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    /**
     * Draws the parallax backdrop behind the tilemap: a sky and two scrolling tree
     * layers in the forest, or a dark vertical gradient in the cave. Layers scroll
     * slower than the camera to give a sense of depth.
     */
    private void drawBackground() {
        float camX = camera.position.x, camY = camera.position.y;
        if (caveStyle) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapes.setProjectionMatrix(camera.combined);
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.rect(camX - VIEW_WIDTH / 2f, camY - VIEW_HEIGHT / 2f, VIEW_WIDTH, VIEW_HEIGHT,
                    caveBottom, caveBottom, caveTop, caveTop);
            shapes.end();
            drawCaveDecor(camX, camY);
            return;
        }
        // Sky.
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        game.batch.draw(skyTex, camX - VIEW_WIDTH / 2f, camY - VIEW_HEIGHT / 2f, VIEW_WIDTH, VIEW_HEIGHT);
        game.batch.end();

        // Mountains, behind the trees.
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        mountainRidge(0.14f, 300f, 210f, 150f, 8 * TILE, mtnFar, camX);
        mountainRidge(0.24f, 250f, 180f, 116f, 7 * TILE, mtnNear, camX);
        shapes.end();

        // Tree layers.
        game.batch.begin();
        drawTreeRow(treeFar, 0.30f, 140f, 175f, 60f, 0.55f, camX);  // distant, darkened
        drawTreeRow(treeNear, 0.55f, 175f, 150f, 44f, 1.00f, camX); // nearer, full colour
        game.batch.setColor(Color.WHITE);
        game.batch.end();
    }

    /**
     * Draws cave background decor: a row of stalagmites rising from the floor and a
     * row of stalactites hanging from the top of the view, both muted and parallaxed.
     */
    private void drawCaveDecor(float camX, float camY) {
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        game.batch.setColor(0.50f, 0.52f, 0.60f, 1f); // muted, slightly blue cave stone
        caveStones(0.50f, 150f, 70f, false, camX, camY); // stalagmites on the floor
        caveStones(0.42f, 130f, 56f, true, camX, camY);  // stalactites from the ceiling
        game.batch.setColor(Color.WHITE);
        game.batch.end();
    }

    /** One repeating row of cave rocks; ceiling rows are drawn flipped to hang down. */
    private void caveStones(float p, float spacing, float baseH, boolean ceiling, float camX, float camY) {
        float half = (VIEW_WIDTH / 2f) / p + spacing;
        float startA = (float) Math.floor((camX - half) / spacing) * spacing;
        for (float a = startA; a <= camX + half; a += spacing) {
            int k = Math.round(a / spacing);
            Texture t = caveRocks[Math.floorMod(k, caveRocks.length)];
            float th = baseH * (0.7f + 0.3f * Math.abs((float) Math.sin(k * 1.7f)));
            float tw = t.getWidth() * (th / t.getHeight());
            float ax = a * p + camX * (1f - p);
            if (ceiling) {
                float topY = camY + VIEW_HEIGHT / 2f;
                game.batch.draw(t, ax - tw / 2f, topY, tw, -th); // negative height flips it downward
            } else {
                game.batch.draw(t, ax - tw / 2f, 44f, tw, th); // base sits below the floor surface
            }
        }
    }

    /** Draws one repeating ridge of overlapping mountain triangles with parallax. */
    private void mountainRidge(float p, float spacing, float halfW, float peakH, float baseY, Color col, float camX) {
        shapes.setColor(col);
        float half = (VIEW_WIDTH / 2f) / p + spacing;
        float startA = (float) Math.floor((camX - half) / spacing) * spacing;
        for (float a = startA; a <= camX + half; a += spacing) {
            int k = Math.round(a / spacing);
            float vary = 0.72f + 0.28f * Math.abs((float) Math.sin(k * 1.3f)); // uneven peaks
            float ax = a * p + camX * (1f - p);
            shapes.triangle(ax - halfW, baseY, ax + halfW, baseY, ax, baseY + peakH * vary);
        }
    }

    /**
     * Draws one repeating row of trees with horizontal parallax.
     *
     * @param set     tree textures to cycle through across the row
     * @param p       parallax factor (smaller = farther/slower)
     * @param drawH   draw height in world pixels (width keeps aspect)
     * @param spacing world distance between trees
     * @param baseY   world y of the tree base (kept low so trunks hide behind the ground)
     * @param shade   colour multiplier (less than 1 darkens distant trees)
     */
    private void drawTreeRow(Texture[] set, float p, float drawH, float spacing, float baseY, float shade, float camX) {
        game.batch.setColor(shade, shade, shade, 1f);
        float half = (VIEW_WIDTH / 2f) / p + spacing;
        float startA = (float) Math.floor((camX - half) / spacing) * spacing;
        for (float a = startA; a <= camX + half; a += spacing) {
            int k = Math.round(a / spacing);
            Texture t = set[Math.floorMod(k, set.length)];
            float tw = t.getWidth() * (drawH / t.getHeight());
            float ax = a * p + camX * (1f - p); // parallax: offset from centre scales by p
            game.batch.draw(t, ax - tw / 2f, baseY, tw, drawH);
        }
        game.batch.setColor(Color.WHITE);
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
        drawBackground();
        mapRenderer.setView(camera);
        mapRenderer.render();

        // World-space sprites.
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        for (Pickup p : pickups) {
            if (!p.collected) game.batch.draw(icons.get(p.type.gid), p.x, p.y, Pickup.SIZE, Pickup.SIZE);
        }
        if (hasChest) game.batch.draw(chest, goalX, goalY, CHEST_SIZE, CHEST_SIZE);
        if (doorVisible()) game.batch.draw(doorTex, doorX, doorY, DOOR_W, DOOR_H);
        for (Enemy e : enemies) e.render(game.batch);
        if (boss != null) boss.render(game.batch);
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
            drawHpBar();
            if (boss != null) drawBossBar();
            game.batch.begin();
            drawHud();
            touchPad.drawLabels(game.batch, game.font);
            drawCraftButtonLabel();
            if (showKeyHint) drawCenteredHud("Locked!  Craft a Key first.", VIEW_HEIGHT - 44);
            else if (doorVisible()) drawCenteredHud("The door is open!  Walk into it.", VIEW_HEIGHT - 44);
            if (boss != null) drawCenteredHud("BOSS", 40);
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

        boolean attackPressed = touchPad.attackJustPressed()
                || Gdx.input.isKeyJustPressed(Input.Keys.J)
                || Gdx.input.isKeyJustPressed(Input.Keys.X);
        if (attackPressed && !player.isAttacking()) {
            player.startAttack();
            Audio.playAttack();
        }

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

        // Goal. Level 1: a key-gated chest reveals a door to the cave. Level 2: an
        // open door to the boss room. Level 3 (boss): no door - win by defeating it.
        showKeyHint = false;
        if (!bossLevel) {
            boolean atChest = hasChest && overlaps(player.x, player.y, Player.WIDTH, Player.HEIGHT, goalX, goalY, CHEST_SIZE, CHEST_SIZE);
            if (hasChest && !chestOpened) {
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
                game.setScreen(new GameScreen(game, nextLevel));
                dispose();
                return true;
            }
        }

        // Boars: patrol, then resolve combat with the player. A live boar can be hit
        // by the attack swing or stomped from above; touching it otherwise hurts the
        // player (whose own i-frames stop repeated hits from draining health at once).
        boolean swinging = player.getAttackBox(attackBox);
        for (Enemy e : enemies) {
            e.update(delta, groundLayer);
            if (!e.isAlive()) continue;

            if (swinging && overlaps(attackBox.x, attackBox.y, attackBox.width, attackBox.height,
                    e.x, e.y, Enemy.WIDTH, Enemy.HEIGHT)) {
                e.hit(ATTACK_DMG);
                continue;
            }
            if (!overlaps(player.x, player.y, Player.WIDTH, Player.HEIGHT, e.x, e.y, Enemy.WIDTH, Enemy.HEIGHT)) continue;

            boolean stomp = player.getVelocityY() < 0 && player.getFeetY() >= e.y + Enemy.HEIGHT - 8f;
            if (stomp) {
                e.hit(ATTACK_DMG);
                player.bounce();
                Audio.playJump();
            } else if (!player.isAttacking()) {
                hurtPlayer(CONTACT_DMG);
            }
        }

        // Boss fight (level 3): the snail chases the player; attacks and stomps wear
        // down its health, contact hurts the player, and its defeat wins the game.
        if (boss != null) {
            boss.update(delta, groundLayer, player.x + Player.WIDTH / 2f);
            if (boss.isAlive()) {
                boolean overlap = overlaps(player.x, player.y, Player.WIDTH, Player.HEIGHT,
                        boss.x, boss.y, Boss.WIDTH, Boss.HEIGHT);
                // The player wears the boss down with attacks and stomps at any time.
                if (swinging && overlaps(attackBox.x, attackBox.y, attackBox.width, attackBox.height,
                        boss.x, boss.y, Boss.WIDTH, Boss.HEIGHT)) {
                    boss.hit(ATTACK_DMG);
                } else if (overlap && player.getVelocityY() < 0 && player.getFeetY() >= boss.y + Boss.HEIGHT - 10f) {
                    boss.hit(ATTACK_DMG);
                    player.bounce();
                    Audio.playJump();
                }
                // The boss only hurts the player during its stationary strike window.
                if (boss.isStriking() && overlap) {
                    hurtPlayer(CONTACT_DMG);
                }
            }
            if (boss.isDefeated()) {
                Audio.playWin();
                game.setScreen(new WinScreen(game));
                dispose();
                return true;
            }
        }

        // Falling into a pit hurts the player and resets them to the level start.
        if (player.getFeetY() < -Player.HEIGHT) {
            hurtPlayer(CONTACT_DMG);
            player.teleport(spawnX, spawnY);
        }

        // Out of health: end the run.
        if (!player.isAlive()) {
            Audio.playGameOver();
            game.setScreen(new GameOverScreen(game, level));
            dispose();
            return true;
        }

        float camX = MathUtils.clamp(player.x, VIEW_WIDTH / 2f, mapWidthPx - VIEW_WIDTH / 2f);
        float camY = MathUtils.clamp(player.y, VIEW_HEIGHT / 2f, mapHeightPx - VIEW_HEIGHT / 2f);
        camera.position.set(camX, camY, 0);
        return false;
    }

    /** Applies damage to the player and plays the hurt sound only if it actually landed. */
    private void hurtPlayer(int amount) {
        if (player.damage(amount)) {
            Audio.playHurt();
        }
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
    }

    /** Whether the exit door should be shown/active: level 2 always, level 1 once unlocked. */
    private boolean doorVisible() {
        return !bossLevel && (!hasChest || chestOpened);
    }

    /** Boss health bar, drawn wide across the bottom-centre of the screen. */
    private void drawBossBar() {
        float w = 360, h = 14, bx = VIEW_WIDTH / 2f - w / 2f, by = 22;
        float frac = Math.max(0f, boss.getHp() / (float) Boss.MAX_HP);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.6f);
        shapes.rect(bx - 2, by - 2, w + 4, h + 4);
        shapes.setColor(0.30f, 0.06f, 0.06f, 1f);
        shapes.rect(bx, by, w, h);
        shapes.setColor(0.85f, 0.20f, 0.16f, 1f);
        shapes.rect(bx, by, w * frac, h);
        shapes.end();
    }

    /** Background + green fill health bar, drawn top-centre. Expects no active batch/shapes. */
    private void drawHpBar() {
        float w = 140, h = 12, bx = VIEW_WIDTH / 2f - w / 2f, by = VIEW_HEIGHT - 18;
        float frac = player.getHp() / (float) Player.MAX_HP;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.55f);
        shapes.rect(bx - 2, by - 2, w + 4, h + 4);
        shapes.setColor(0.45f, 0.12f, 0.12f, 1f);
        shapes.rect(bx, by, w, h);
        shapes.setColor(0.30f, 0.78f, 0.30f, 1f);
        shapes.rect(bx, by, w * frac, h);
        shapes.end();
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
        boarTex.dispose();
        boarHitTex.dispose();
        snailWalkTex.dispose();
        snailHideTex.dispose();
        snailDeadTex.dispose();
        skyTex.dispose();
        for (Texture t : treeFar) t.dispose();
        for (Texture t : treeNear) t.dispose();
        for (Texture t : caveRocks) t.dispose();
        shapes.dispose();
    }
}
