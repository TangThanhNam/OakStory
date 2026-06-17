package com.oakstory.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * On-screen touch controls: a left/right D-pad and a jump button, drawn in HUD
 * space. Works with touch on Android and with the mouse on desktop, so the same
 * controls can be tested everywhere.
 *
 * <p>Call {@link #poll(Viewport)} once per frame to read the input, then
 * {@link #drawButtons(ShapeRenderer)} and {@link #drawLabels(Batch, BitmapFont)}
 * to render the overlay.</p>
 */
public class TouchPad {

    /** A round button defined by its centre and radius in HUD units. */
    private static final class Button {
        final float cx, cy, r;
        final String label;
        boolean held;
        Button(float cx, float cy, float r, String label) {
            this.cx = cx; this.cy = cy; this.r = r; this.label = label;
        }
        boolean contains(float x, float y) {
            float dx = x - cx, dy = y - cy;
            return dx * dx + dy * dy <= r * r;
        }
    }

    private final Button left = new Button(70, 70, 36, "<");
    private final Button right = new Button(165, 70, 36, ">");
    private final Button jump = new Button(585, 80, 42, "JUMP");
    private final Button attack = new Button(495, 60, 38, "ATK");

    private boolean jumpWasHeld;
    private boolean jumpJustPressed;
    private boolean attackWasHeld;
    private boolean attackJustPressed;

    private final Vector3 tmp = new Vector3();
    private final GlyphLayout layout = new GlyphLayout();

    /** Reads all active pointers and updates the button states. */
    public void poll(Viewport hudViewport) {
        left.held = right.held = jump.held = attack.held = false;
        for (int p = 0; p < 6; p++) {
            if (!Gdx.input.isTouched(p)) continue;
            tmp.set(Gdx.input.getX(p), Gdx.input.getY(p), 0);
            hudViewport.unproject(tmp);
            if (left.contains(tmp.x, tmp.y)) left.held = true;
            if (right.contains(tmp.x, tmp.y)) right.held = true;
            if (jump.contains(tmp.x, tmp.y)) jump.held = true;
            if (attack.contains(tmp.x, tmp.y)) attack.held = true;
        }
        jumpJustPressed = jump.held && !jumpWasHeld;
        jumpWasHeld = jump.held;
        attackJustPressed = attack.held && !attackWasHeld;
        attackWasHeld = attack.held;
    }

    public boolean left() { return left.held; }
    public boolean right() { return right.held; }
    public boolean jumpJustPressed() { return jumpJustPressed; }
    public boolean attackJustPressed() { return attackJustPressed; }

    /** Draws the translucent button circles (call between ShapeRenderer begin/end is handled here). */
    public void drawButtons(ShapeRenderer shapes) {
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        circle(shapes, left);
        circle(shapes, right);
        circle(shapes, jump);
        circle(shapes, attack);
        shapes.end();
    }

    private void circle(ShapeRenderer shapes, Button b) {
        float a = b.held ? 0.45f : 0.22f;
        shapes.setColor(1f, 1f, 1f, a);
        shapes.circle(b.cx, b.cy, b.r, 24);
    }

    /** Draws the button labels; expects an active Batch. */
    public void drawLabels(Batch batch, BitmapFont font) {
        label(batch, font, left, 1.4f);
        label(batch, font, right, 1.4f);
        label(batch, font, jump, 0.9f);
        label(batch, font, attack, 0.9f);
    }

    private void label(Batch batch, BitmapFont font, Button b, float scale) {
        font.getData().setScale(scale);
        layout.setText(font, b.label);
        font.draw(batch, b.label, b.cx - layout.width / 2f, b.cy + layout.height / 2f);
    }
}
