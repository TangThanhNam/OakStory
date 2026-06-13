package com.oakstory.items;

/** A collectible resource sitting in the level at a fixed world position. */
public class Pickup {

    public static final float SIZE = 16f;

    public final ResourceType type;
    public final float x, y;
    public boolean collected;

    public Pickup(ResourceType type, float x, float y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }
}
