package com.oakstory.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.oakstory.audio.Audio;
import com.oakstory.items.Icons;
import com.oakstory.items.Inventory;

/**
 * The crafting menu overlay. Shows the two recipes (Key and Bridge) with a
 * tappable Craft button each, and reflects whether the player has the resources.
 * Crafting rules live in {@link Inventory}; this class only drives the UI.
 */
public class CraftingMenu {

    private static final int KEY_ICON = 516;   // golden key tile
    private static final int PLANK_ICON = 157; // plank tile, used for the bridge

    private boolean open;

    private final Rectangle keyBtn = new Rectangle(410, 196, 96, 30);
    private final Rectangle bridgeBtn = new Rectangle(410, 134, 96, 30);
    private final Rectangle closeBtn = new Rectangle(486, 256, 24, 24);
    private final GlyphLayout layout = new GlyphLayout();

    public boolean isOpen() {
        return open;
    }

    public void open() {
        open = true;
    }

    public void close() {
        open = false;
    }

    public void toggle() {
        open = !open;
    }

    /** Number-key shortcuts: 1 = Key, 2 = Bridge. */
    public void handleKeys(Inventory inv) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) craftSound(inv.craftKey());
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) craftSound(inv.craftBridge());
    }

    /** Handles a tap at HUD coordinates (x, y). */
    public void handleTap(float x, float y, Inventory inv) {
        if (closeBtn.contains(x, y)) {
            Audio.playLocked();
            open = false;
        } else if (keyBtn.contains(x, y)) {
            craftSound(inv.craftKey());
        } else if (bridgeBtn.contains(x, y)) {
            craftSound(inv.craftBridge());
        }
    }

    /** Plays a confirming sound on a successful craft, a denied sound otherwise. */
    private void craftSound(boolean crafted) {
        if (crafted) Audio.playCraft();
        else Audio.playLocked();
    }

    public void render(ShapeRenderer shapes, Batch batch, BitmapFont font, Icons icons, Inventory inv) {
        Gdx.gl.glEnable(GL20.GL_BLEND);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.6f);
        shapes.rect(0, 0, 640, 360);
        shapes.setColor(0.16f, 0.11f, 0.07f, 0.98f);
        shapes.rect(120, 70, 400, 220);
        drawButton(shapes, keyBtn, inv.hasKey() ? 2 : (inv.canCraftKey() ? 1 : 0));
        drawButton(shapes, bridgeBtn, inv.canCraftBridge() ? 1 : 0);
        shapes.setColor(0.6f, 0.2f, 0.2f, 1f);
        shapes.rect(closeBtn.x, closeBtn.y, closeBtn.width, closeBtn.height);
        shapes.end();

        batch.begin();
        font.getData().setScale(1.4f);
        centered(batch, font, "CRAFTING", 320, 278);

        font.getData().setScale(0.9f);
        batch.draw(icons.get(KEY_ICON), 150, 192, 28, 28);
        font.draw(batch, "Key  -  2 Mushroom + 1 Herb", 188, 216);
        font.draw(batch, "Opens the door to the cave level", 188, 199);
        centered(batch, font, inv.hasKey() ? "Owned" : "Craft", keyBtn.x + keyBtn.width / 2, keyBtn.y + 16);

        batch.draw(icons.get(PLANK_ICON), 150, 132, 28, 28);
        font.draw(batch, "Bridge  -  3 Herb", 188, 154);
        font.draw(batch, "Cross a wide pit   (have " + inv.bridges() + ")", 188, 137);
        centered(batch, font, "Craft", bridgeBtn.x + bridgeBtn.width / 2, bridgeBtn.y + 16);

        font.getData().setScale(0.9f);
        centered(batch, font, "X", closeBtn.x + 12, closeBtn.y + 16);
        batch.end();
    }

    /** state: 0 = cannot craft (grey), 1 = craftable (green), 2 = owned (gold). */
    private void drawButton(ShapeRenderer shapes, Rectangle r, int state) {
        if (state == 1) shapes.setColor(0.20f, 0.55f, 0.22f, 1f);
        else if (state == 2) shapes.setColor(0.55f, 0.45f, 0.10f, 1f);
        else shapes.setColor(0.32f, 0.32f, 0.34f, 1f);
        shapes.rect(r.x, r.y, r.width, r.height);
    }

    private void centered(Batch batch, BitmapFont font, String text, float cx, float cy) {
        layout.setText(font, text);
        font.draw(batch, text, cx - layout.width / 2f, cy + layout.height / 2f);
    }
}
