package com.oakstory.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

/**
 * Central sound manager. All clips are CC0 assets from Kenney.nl (see ASSETS.md).
 *
 * <p>Short one-shot effects are loaded as {@link Sound} and triggered with the
 * static {@code play*} helpers. Background tracks are {@link Music} so they can
 * stream and loop. Everything is created once in {@link #load()} (called from the
 * game's {@code create()}) and released in {@link #dispose()} on exit.</p>
 */
public final class Audio {

    private static Sound jump, pickup, craft, locked, door, hurt;
    private static Music forestTheme, caveTheme, win, gameOver;
    private static Music current; // the track currently playing, if any

    private static final float SFX_VOLUME = 0.6f;
    private static final float MUSIC_VOLUME = 0.35f;

    private Audio() {}

    /** Loads every clip. Call once after the GL context exists (game create). */
    public static void load() {
        jump = sound("sfx/jump.ogg");
        pickup = sound("sfx/pickup.ogg");
        craft = sound("sfx/craft.ogg");
        locked = sound("sfx/locked.ogg");
        door = sound("sfx/door.ogg");
        hurt = sound("sfx/hurt.ogg");

        forestTheme = music("music/theme.ogg");
        caveTheme = music("music/cave_theme.ogg");
        gameOver = music("music/gameover.ogg");
        win = music("music/win.ogg");
        forestTheme.setLooping(true);
        caveTheme.setLooping(true);
    }

    private static Sound sound(String path) {
        return Gdx.audio.newSound(Gdx.files.internal(path));
    }

    private static Music music(String path) {
        Music m = Gdx.audio.newMusic(Gdx.files.internal(path));
        m.setVolume(MUSIC_VOLUME);
        return m;
    }

    public static void playJump()   { if (jump != null) jump.play(SFX_VOLUME); }
    public static void playPickup() { if (pickup != null) pickup.play(SFX_VOLUME); }
    public static void playCraft()  { if (craft != null) craft.play(SFX_VOLUME); }
    public static void playLocked() { if (locked != null) locked.play(SFX_VOLUME); }
    public static void playDoor()   { if (door != null) door.play(SFX_VOLUME); }
    public static void playHurt()   { if (hurt != null) hurt.play(SFX_VOLUME); }

    /** Starts the looping theme for the given level (1 = forest, 2 = cave). No-op if already playing. */
    public static void playTheme(int level) { switchTo(level == 1 ? forestTheme : caveTheme); }

    /** Plays the victory jingle once (and stops the looping theme). */
    public static void playWin() { switchTo(win); }

    /** Plays the game-over track once (and stops the looping theme). */
    public static void playGameOver() { switchTo(gameOver); }

    /** Stops whatever track is playing (e.g. when leaving gameplay for a menu). */
    public static void stopMusic() {
        if (current != null) current.stop();
        current = null;
    }

    private static void switchTo(Music next) {
        if (next == null || current == next) return;
        if (current != null) current.stop();
        current = next;
        next.play();
    }

    /** Releases all audio resources. Call from the game's dispose(). */
    public static void dispose() {
        Sound[] sounds = {jump, pickup, craft, locked, door, hurt};
        for (Sound s : sounds) if (s != null) s.dispose();
        Music[] tracks = {forestTheme, caveTheme, win, gameOver};
        for (Music m : tracks) if (m != null) m.dispose();
        jump = pickup = craft = locked = door = hurt = null;
        forestTheme = caveTheme = win = gameOver = current = null;
    }
}
