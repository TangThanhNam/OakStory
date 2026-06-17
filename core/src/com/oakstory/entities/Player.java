package com.oakstory.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
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

    /** Combat tuning. */
    public static final int MAX_HP = 100;
    private static final float INVULN_TIME = 1.1f;   // i-frames after taking damage
    private static final float ATTACK_TIME = 0.30f;  // length of one swing (12 frames), kept short and snappy
    private static final float ATTACK_REACH = 22f;   // how far the swing reaches in front

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

    private int hp = MAX_HP;
    private float invulnTimer;
    private float attackTimer;     // > 0 while a swing is in progress
    private float attackStateTime; // drives the attack animation

    private final Texture idleTex, runTex, jumpTex, attackTex;
    private final Animation<TextureRegion> idleAnim, runAnim, attackAnim;
    private final TextureRegion jumpFrame;

    public Player(float spawnX, float spawnY, float worldWidth) {
        this.x = spawnX;
        this.y = spawnY;
        this.worldWidth = worldWidth;

        idleTex = new Texture(Gdx.files.internal("character/Idle.png"));
        runTex = new Texture(Gdx.files.internal("character/Run.png"));
        jumpTex = new Texture(Gdx.files.internal("character/Jump.png"));
        attackTex = new Texture(Gdx.files.internal("character/Attack.png"));

        // Note: the sheets use different frame sizes.
        idleAnim = new Animation<>(0.18f, firstRow(idleTex, 64, 80)); // 4 frames
        idleAnim.setPlayMode(Animation.PlayMode.LOOP);
        runAnim = new Animation<>(0.06f, firstRow(runTex, 80, 80));   // 8 frames
        runAnim.setPlayMode(Animation.PlayMode.LOOP);
        attackAnim = new Animation<>(ATTACK_TIME / 8f, firstRow(attackTex, 96, 80)); // 8 frames of 96x80

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
        if (invulnTimer > 0) invulnTimer -= dt;
        if (attackTimer > 0) { attackTimer -= dt; attackStateTime += dt; }

        // The character lunges within the attack frames, so root it horizontally
        // during a swing; otherwise it appears to slide sideways while attacking.
        if (attackTimer > 0) { left = false; right = false; }

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
        // Blink while invulnerable: skip drawing on alternating ~0.1s windows.
        if (invulnTimer > 0 && ((int) (invulnTimer * 10) & 1) == 0) return;

        // Per-state frame size, the character's content-centre x within the frame,
        // and how far the feet sit above the frame bottom. Aligning on the content
        // centre keeps the character from shifting horizontally between states.
        TextureRegion frame;
        float frameW, frameH, contentCx, feetFromBottom;
        if (attackTimer > 0) {
            // 96-wide attack frames: the body is centred at x=48 within the frame.
            frame = attackAnim.getKeyFrame(attackStateTime); frameW = 96; frameH = 80; contentCx = 48; feetFromBottom = 18;
        } else if (!grounded) {
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

    /** Current vertical velocity; negative while falling. Used to detect a stomp. */
    public float getVelocityY() {
        return vy;
    }

    /** Moves the player to a position and clears motion (used to respawn after a pit fall). */
    public void teleport(float nx, float ny) {
        x = nx;
        y = ny;
        vx = vy = 0;
    }

    /** Gives the player an upward hop, e.g. after stomping an enemy. */
    public void bounce() {
        vy = JUMP_SPEED * 0.65f;
        grounded = false;
    }

    // --- Combat ----------------------------------------------------------

    public int getHp() { return hp; }

    public boolean isAlive() { return hp > 0; }

    /** True briefly after taking damage; used for the blink and to ignore further hits. */
    public boolean isInvulnerable() { return invulnTimer > 0; }

    public boolean isAttacking() { return attackTimer > 0; }

    /** Starts a swing if one is not already in progress. */
    public void startAttack() {
        if (attackTimer <= 0) {
            attackTimer = ATTACK_TIME;
            attackStateTime = 0;
        }
    }

    /**
     * Applies damage, unless the player is still in i-frames from a previous hit.
     *
     * @return true if the damage actually landed
     */
    public boolean damage(int amount) {
        if (invulnTimer > 0 || hp <= 0) return false;
        hp = Math.max(0, hp - amount);
        invulnTimer = INVULN_TIME;
        return true;
    }

    /**
     * Writes the current attack hitbox into {@code out} when a swing is in its
     * active window (the middle of the animation).
     *
     * @return true if {@code out} now holds a live hitbox
     */
    public boolean getAttackBox(Rectangle out) {
        if (attackTimer <= 0) return false;
        float t = attackStateTime;
        if (t < ATTACK_TIME * 0.4f || t > ATTACK_TIME * 0.85f) return false; // active during the down-slash
        float bx = facingRight ? x + WIDTH : x - ATTACK_REACH;
        out.set(bx, y, ATTACK_REACH, HEIGHT);
        return true;
    }

    @Override
    public void dispose() {
        idleTex.dispose();
        runTex.dispose();
        jumpTex.dispose();
        attackTex.dispose();
    }
}
