package com.oakstory.items;

/**
 * Tracks how many of each resource the player holds and what they have crafted.
 * The crafting recipes live here so the rules are in one place.
 */
public class Inventory {

    private final int[] counts = new int[ResourceType.values().length];
    private boolean hasKey;
    private int bridges;

    public void add(ResourceType type) {
        counts[type.ordinal()]++;
    }

    public int count(ResourceType type) {
        return counts[type.ordinal()];
    }

    public boolean hasKey() {
        return hasKey;
    }

    public int bridges() {
        return bridges;
    }

    // --- Recipes ---------------------------------------------------------

    /** Key = 2 Mushroom + 1 Herb. Opens the door to level 2. Only one is needed. */
    public boolean canCraftKey() {
        return !hasKey
                && count(ResourceType.MUSHROOM) >= 2
                && count(ResourceType.HERB) >= 1;
    }

    public boolean craftKey() {
        if (!canCraftKey()) return false;
        counts[ResourceType.MUSHROOM.ordinal()] -= 2;
        counts[ResourceType.HERB.ordinal()] -= 1;
        hasKey = true;
        return true;
    }

    /** Bridge = 3 Herb. Placed to cross a wide pit. */
    public boolean canCraftBridge() {
        return count(ResourceType.HERB) >= 3;
    }

    public boolean craftBridge() {
        if (!canCraftBridge()) return false;
        counts[ResourceType.HERB.ordinal()] -= 3;
        bridges++;
        return true;
    }

    public boolean useBridge() {
        if (bridges <= 0) return false;
        bridges--;
        return true;
    }
}
