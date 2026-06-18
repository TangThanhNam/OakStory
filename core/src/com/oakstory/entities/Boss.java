package com.oakstory.entities;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;

/**
 * The end boss: a giant snail. It slowly chases the player, but when the player
 * gets close it <em>stops</em> and performs a telegraphed attack: it rears up
 * (the hide animation) and only deals damage during the brief strike in the middle
 * of that animation. This gives the player safe windows to dodge in and hit it.
 *
 * <p>The walk, hide (attack) and death sheets are each 8 frames of 48x32, drawn
 * enlarged. Health is whittled down by the player's attacks and stomps.</p>
 */
public class Boss {

    private static final float TILE = 16f;
    private static final float GRAVITY = -640f;
    private static final float SPEED = 40f;   // slow, lumbering chase
    private static final float MAX_FALL = 520f;
    private static final float SCALE = 3.2f;  // how much the 48x32 sprite is enlarged
    private static final float FRAME_W = 48f, FRAME_H = 32f;
    private static final float HIT_COOLDOWN = 0.3f;
    private static final float VANISH_TIME = 0.7f;

    // Attack pacing: how close triggers an attack, how long the (stationary) attack
    // lasts, the fraction of it that actually hurts, and the rest before the next one.
    private static final float ATTACK_RANGE = 96f;
    private static final float ATTACK_TIME = 0.9f;
    private static final float STRIKE_FROM = 0.40f, STRIKE_TO = 0.66f;
    private static final float ATTACK_COOLDOWN = 0.9f;

    public static final int MAX_HP = 14;
    /** Large collision box (still smaller than the drawn sprite). */
    public static final float WIDTH = 100f;
    public static final float HEIGHT = 56f;

    public float x, y;
    private float vy;
    private boolean facingRight;
    private float stateTime;

    private int hp = MAX_HP;
    private float hitCooldown;
    private boolean dying;
    private float dyingTime;

    private float attackTimer;     // > 0 while an attack (stationary) is in progress
    private float attackStateTime; // drives the attack animation / strike window
    private float attackCooldown;  // delay before the next attack

    private final Animation<TextureRegion> walk, hide, dead;

    public Boss(float x, float y, Animation<TextureRegion> walk,
                Animation<TextureRegion> hide, Animation<TextureRegion> dead) {
        this.x = x;
        this.y = y;
        this.walk = walk;
        this.hide = hide;
        this.dead = dead;
    }

    public int getHp() { return hp; }

    /** Dangerous and damageable: still alive and not yet dying. */
    public boolean isAlive() { return !dying && hp > 0; }

    /** True once the death animation has finished: the player has won. */
    public boolean isDefeated() { return dying && dyingTime >= VANISH_TIME; }

    public boolean isAttacking() { return attackTimer > 0; }

    /** True only during the brief strike in the middle of an attack: the danger window. */
    public boolean isStriking() {
        return attackTimer > 0 && attackStateTime >= ATTACK_TIME * STRIKE_FROM
                && attackStateTime <= ATTACK_TIME * STRIKE_TO;
    }

    /** Deals damage (from an attack or stomp), respecting a short cooldown. */
    public boolean hit(int amount) {
        if (dying || hitCooldown > 0) return false;
        hp -= amount;
        hitCooldown = HIT_COOLDOWN;
        if (hp <= 0) { dying = true; dyingTime = 0; }
        return true;
    }

    /** Advances the boss: gravity, then either a stationary attack or a slow chase. */
    public void update(float dt, TiledMapTileLayer solid, float playerCentreX) {
        stateTime += dt;
        if (hitCooldown > 0) hitCooldown -= dt;
        if (attackCooldown > 0) attackCooldown -= dt;

        vy += GRAVITY * dt;
        if (vy < -MAX_FALL) vy = -MAX_FALL;
        y += vy * dt;
        if (collides(solid)) {
            if (vy <= 0) y = (float) (Math.floor(y / TILE) + 1) * TILE + 0.01f;
            vy = 0;
        }

        if (dying) { dyingTime += dt; return; }

        // Mid-attack: stand still and play out the animation, then start the cooldown.
        if (attackTimer > 0) {
            attackTimer -= dt;
            attackStateTime += dt;
            if (attackTimer <= 0) attackCooldown = ATTACK_COOLDOWN;
            return; // no movement while attacking
        }

        // Face the player.
        float centre = x + WIDTH / 2f;
        if (playerCentreX > centre + 4f) facingRight = true;
        else if (playerCentreX < centre - 4f) facingRight = false;

        // Close enough and recovered: stop and begin a telegraphed attack.
        if (Math.abs(playerCentreX - centre) < ATTACK_RANGE && attackCooldown <= 0) {
            attackTimer = ATTACK_TIME;
            attackStateTime = 0;
            return;
        }

        // Otherwise shuffle toward the player.
        float dir = facingRight ? 1f : -1f;
        x += dir * SPEED * dt;
        if (collides(solid)) x -= dir * SPEED * dt; // blocked by a wall
    }

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
        TextureRegion frame = dying ? dead.getKeyFrame(dyingTime)
                : attackTimer > 0 ? hide.getKeyFrame(attackStateTime)
                : walk.getKeyFrame(stateTime);
        float drawW = FRAME_W * SCALE, drawH = FRAME_H * SCALE;
        float drawX = x + WIDTH / 2f - drawW / 2f;
        float drawY = y - 6f; // feet sit near the sprite's bottom
        // The snail sheet faces left, so mirror it when moving right.
        if (facingRight) {
            batch.draw(frame, drawX + drawW, drawY, -drawW, drawH);
        } else {
            batch.draw(frame, drawX, drawY, drawW, drawH);
        }
    }
}
