package com.oakstory.items;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.IntMap;

/**
 * Provides 16x16 tile icons from the shared tileset for UI use (HUD, crafting
 * menu, pickups). Regions are cached by GID so repeated HUD draws don't allocate.
 */
public class Icons implements Disposable {

    private static final int COLUMNS = 25;
    private static final int TILE = 16;

    private final Texture sheet;
    private final IntMap<TextureRegion> cache = new IntMap<>();

    public Icons() {
        sheet = new Texture(Gdx.files.internal("tiles/Tiles.png"));
    }

    /** Returns the icon region for the given tile GID. */
    public TextureRegion get(int gid) {
        TextureRegion region = cache.get(gid);
        if (region == null) {
            int index = gid - 1;
            region = new TextureRegion(sheet,
                    (index % COLUMNS) * TILE, (index / COLUMNS) * TILE, TILE, TILE);
            cache.put(gid, region);
        }
        return region;
    }

    @Override
    public void dispose() {
        sheet.dispose();
    }
}
