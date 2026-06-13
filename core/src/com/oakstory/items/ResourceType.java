package com.oakstory.items;

/**
 * The three forage-able resources the player collects to craft with. Each maps
 * to a tile in the shared tileset (Tiles.png) used as its on-screen icon.
 */
public enum ResourceType {

    MUSHROOM("Mushroom", 391),
    HERB("Herb", 448),
    FLOWER("Flower", 518);

    /** Human-readable name shown in the HUD and crafting menu. */
    public final String label;
    /** Tile GID into Tiles.png used as the icon. */
    public final int gid;

    ResourceType(String label, int gid) {
        this.label = label;
        this.gid = gid;
    }
}
