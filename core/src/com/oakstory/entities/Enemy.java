package com.oakstory.entities;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;

/**
 * A boar that patrols a stretch of ground. Its AI is deliberately simple but
 * "alive": it walks in one direction and turns around whenever it would hit a
 * wall or walk off a ledge, so it never falls into a pit.
 *
 * <p>The boar has a small pool of health. The player can hurt it by stomping it
 * from above or by attacking it; touching it from the side hurts the player. When
 * its health runs out it plays a short vanish animation and is then removed.</p>
 *
 * <p>The walk sheet is 6 frames of 48x32 and the vanish sheet 4 frames of 48x32.
 * Both animations are shared between boars and passed in rather than each enemy
 * loading its own texture.</p>
 */
public class Enemy {

    private static final float TILE = 16f;
    private static final float GRAVITY = -640f;
    private static final float SPEED = 34f;
    private static final float MAX_FALL = 520f;
    private static final int MAX_HP = 2;
    private static final float HIT_COOLDOWN = 0.3f; // ignore further hits briefly after one lands
    private static final float VANISH_TIME = 0.32f; // length of the death animation

    /** Collision box, smaller than the 48x32 sprite which has transparent padding. */
    public static final float WIDTH = 30f;
    public static final float HEIGHT = 20f;
    private static final float FRAME_W = 48f, FRAME_H = 32f;

    public float x, y;
    private float vy;
    private boolean facingRight;
    private float stateTime;

    private int hp = MAX_HP;
    private float hitCooldown;
    private boolean dying;
    private float dyingTime;

    private final Animation<TextureRegion> walk;
    private final Animation<TextureRegion> vanish;

    public Enemy(float x, float y, boolean facingRight,
                 Animation<TextureRegion> walk, Animation<TextureRegion> vanish) {
        this.x = x;
        this.y = y;
        this.facingRight = facingRight;
        this.walk = walk;
        this.vanish = vanish;
    }

    /** Dangerous and damageable: still patrolling, not yet dying. */
    public boolean isAlive() {
        return !dying && hp > 0;
    }

    /** True once the vanish animation has finished and the boar should be discarded. */
    public boolean isGone() {
        return dying && dyingTime >= VANISH_TIME;
    }

    /**
     * Deals damage to the boar (from a stomp or attack), respecting a short
     * cooldown so a single hit does not register every frame.
     *
     * @return true if the hit landed
     */
    public boolean hit(int amount) {
        if (dying || hitCooldown > 0) return false;
        hp -= amount;
        hitCooldown = HIT_COOLDOWN;
        if (hp <= 0) { dying = true; dyingTime = 0; }
        return true;
    }

    /** Advances the boar: gravity, horizontal patrol, and turning at walls/ledges. */
    public void update(float dt, TiledMapTileLayer solid) {
        stateTime += dt;
        if (hitCooldown > 0) hitCooldown -= dt;
        if (dying) { dyingTime += dt; return; } // no movement while vanishing

        // Gravity and vertical resolve (so the boar rests on ground and platforms).
        vy += GRAVITY * dt;
        if (vy < -MAX_FALL) vy = -MAX_FALL;
        y += vy * dt;
        boolean grounded = false;
        if (collides(solid)) {
            if (vy <= 0) {
                y = (float) (Math.floor(y / TILE) + 1) * TILE + 0.01f;
                grounded = true;
            }
            vy = 0;
        }

        // Horizontal patrol; reverse on a wall.
        float dir = facingRight ? 1f : -1f;
        x += dir * SPEED * dt;
        if (collides(solid)) {
            x -= dir * SPEED * dt;
            facingRight = !facingRight;
            return;
        }

        // Reverse at a ledge: if there is no solid tile just beyond the leading
        // foot, turn around instead of walking off into a pit.
        if (grounded) {
            float frontX = facingRight ? x + WIDTH + 1f : x - 1f;
            int cx = (int) Math.floor(frontX / TILE);
            int cy = (int) Math.floor((y - 2f) / TILE);
            if (solid.getCell(cx, cy) == null) {
                facingRight = !facingRight;
            }
        }
    }

    /** True if the boar's collision box overlaps any solid tile. */
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
        if (isGone()) return;
        TextureRegion frame = dying ? vanish.getKeyFrame(dyingTime) : walk.getKeyFrame(stateTime);
        float drawX = x + WIDTH / 2f - FRAME_W / 2f;
        float drawY = y - 1f; // feet sit at the sprite's bottom row
        // The sheet's boar faces left, so mirror it when walking right.
        if (facingRight) {
            batch.draw(frame, drawX + FRAME_W, drawY, -FRAME_W, FRAME_H);
        } else {
            batch.draw(frame, drawX, drawY, FRAME_W, FRAME_H);
        }
    }
}
