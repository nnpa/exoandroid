package com.mygame.items;

import com.jme3.math.ColorRGBA;

public enum ItemRarity {
    COMMON("Common", ColorRGBA.White),
    UNCOMMON("Uncommon", ColorRGBA.Green),
    RARE("Rare", ColorRGBA.Blue),
    EPIC("Epic", new ColorRGBA(0.6f, 0.2f, 0.8f, 1f)),
    LEGENDARY("Legendary", new ColorRGBA(1f, 0.5f, 0f, 1f));

    private final String displayName;
    private final ColorRGBA color;

    ItemRarity(String displayName, ColorRGBA color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() { return displayName; }
    public ColorRGBA getColor() { return color; }
}