package com.oakstory.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;

/**
 * The player character: a small adventurer that runs, jumps and collides with
 * the solid tiles of the level.
 *
 * <p>Movement uses a simple, stable tile-based AABB scheme: the player is moved
 * one axis at a time and, if the new position overlaps a solid tile, it is
 * snapped back to the tile edge. Because the per-frame movement is always
 * smaller than a tile, single-step snapping is enough and produces no jitter.</p>
 */
public class Player implements Disposable {

    private static final float TILE = 16f;
    private static final float GRAVITY = -640f;
    private static final float MOVE_SPEED = 95f;
    private static final float JUMP_SPEED = 300f;
    private static final float MAX_FALL = 520f;

    /** Collision box size in world pixels (smaller than the sprite, which has padding). */
    public static final float WIDTH = 14f;
    public static final float HEIGHT = 30f;

    public float x, y;
    /** True for the single frame in which a jump was launched (for the jump sound). */
    public boolean justJumped;
    private float vx, vy;
    private boolean grounded;
    private boolean facingRight = true;
    private float stateTime;
    private final float worldWidth;

    private final Texture idleTex, runTex, jumpTex;
    private final Animation<TextureRegion> idleAnim, runAnim;
    private final TextureRegion jumpFrame;

    public Player(float spawnX, float spawnY, float worldWidth) {
        this.x = spawnX;
        this.y = spawnY;
        this.worldWidth = worldWidth;

        idleTex = new Texture(Gdx.files.internal("character/Idle.png"));
        runTex = new Texture(Gdx.files.internal("character/Run.png"));
        jumpTex = new Texture(Gdx.files.internal("character/Jump.png"));

        // Note: the sheets use different frame sizes.
        idleAnim = new Animation<>(0.18f, firstRow(idleTex, 64, 80)); // 4 frames
        idleAnim.setPlayMode(Animation.PlayMode.LOOP);
        runAnim = new Animation<>(0.06f, firstRow(runTex, 80, 80));   // 8 frames
        runAnim.setPlayMode(Animation.PlayMode.LOOP);

        TextureRegion[] jumpFrames = firstRow(jumpTex, 64, 64);       // 15 frames
        jumpFrame = jumpFrames[Math.min(4, jumpFrames.length - 1)];
    }

    /** Splits a horizontal sprite strip into its frames. */
    private static TextureRegion[] firstRow(Texture tex, int fw, int fh) {
        TextureRegion[][] grid = TextureRegion.split(tex, fw, fh);
        return grid[0];
    }

    /**
     * Advances the player by one frame.
     *
     * @param dt    delta time in seconds
     * @param solid the solid tile layer to collide against
     * @param left  whether "move left" is held
     * @param right whether "move right" is held
     * @param jump  whether a jump was requested this frame
     */
    public void update(float dt, TiledMapTileLayer solid, boolean left, boolean right, boolean jump) {
        stateTime += dt;

        vx = 0;
        justJumped = false;
        if (left) { vx = -MOVE_SPEED; facingRight = false; }
        if (right) { vx = MOVE_SPEED; facingRight = true; }
        if (jump && grounded) { vy = JUMP_SPEED; grounded = false; justJumped = true; }

        vy += GRAVITY * dt;
        if (vy < -MAX_FALL) vy = -MAX_FALL;

        // Horizontal move then resolve.
        x += vx * dt;
        x = MathUtils.clamp(x, 0, worldWidth - WIDTH);
        if (collides(solid)) {
            if (vx > 0) x = (float) Math.floor((x + WIDTH) / TILE) * TILE - WIDTH - 0.01f;
            else if (vx < 0) x = (float) (Math.floor(x / TILE) + 1) * TILE + 0.01f;
        }

        // Vertical move then resolve.
        y += vy * dt;
        grounded = false;
        if (collides(solid)) {
            if (vy > 0) {
                y = (float) Math.floor((y + HEIGHT) / TILE) * TILE - HEIGHT - 0.01f;
            } else {
                y = (float) (Math.floor(y / TILE) + 1) * TILE + 0.01f;
                grounded = true;
            }
            vy = 0;
        }
    }

    /** True if the collision box currently overlaps any non-empty cell in {@code layer}. */
    private boolean collides(TiledMapTileLayer layer) {
        int x0 = (int) Math.floor(x / TILE);
        int x1 = (int) Math.floor((x + WIDTH - 0.01f) / TILE);
        int y0 = (int) Math.floor(y / TILE);
        int y1 = (int) Math.floor((y + HEIGHT - 0.01f) / TILE);
        for (int cx = x0; cx <= x1; cx++) {
            for (int cy = y0; cy <= y1; cy++) {
                if (layer.getCell(cx, cy) != null) return true;
            }
        }
        return false;
    }

    public void render(Batch batch) {
        // Per-state frame size, the character's content-centre x within the frame,
        // and how far the feet sit above the frame bottom. Aligning on the content
        // centre keeps the character from shifting horizontally between states.
        TextureRegion frame;
        float frameW, frameH, contentCx, feetFromBottom;
        if (!grounded) {
            frame = jumpFrame; frameW = 64; frameH = 64; contentCx = 21; feetFromBottom = 5;
        } else if (vx != 0) {
            frame = runAnim.getKeyFrame(stateTime); frameW = 80; frameH = 80; contentCx = 44; feetFromBottom = 18;
        } else {
            frame = idleAnim.getKeyFrame(stateTime); frameW = 64; frameH = 80; contentCx = 38; feetFromBottom = 18;
        }

        float boxCentre = x + WIDTH / 2f;
        float drawY = y - feetFromBottom;
        if (facingRight) {
            float drawX = boxCentre - contentCx;
            batch.draw(frame, drawX, drawY, frameW, frameH);
        } else {
            // Mirror around the content centre by drawing with a negative width.
            float drawX = boxCentre - (frameW - contentCx);
            batch.draw(frame, drawX + frameW, drawY, -frameW, frameH);
        }
    }

    /** Used by the screen to detect a fall into a pit. */
    public float getFeetY() {
        return y;
    }

    @Override
    public void dispose() {
        idleTex.dispose();
        runTex.dispose();
        jumpTex.dispose();
    }
}
