package com.mygame.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LootTable {

    private List<LootEntry> entries = new ArrayList<>();
    private Random random = new Random();

    /**
     * Теперь хранит ТИП предмета, а не готовый Item —
     * сам предмет генерируется в момент выпадения,
     * с актуальными уровнем игрока и сложностью.
     */
    public void addEntry(String itemType, float chance) {
        entries.add(new LootEntry(itemType, chance));
    }

public List<Item> rollForLoot(int playerLevel, int difficulty) {
    List<Item> dropped = new ArrayList<>();
    for (LootEntry entry : entries) {
        if (random.nextFloat() < entry.chance) {

            if ("Gem".equalsIgnoreCase(entry.itemType)) {
                dropped.add(ItemGenerator.generateRandomGem(difficulty));
            } else {
                dropped.add(ItemGenerator.generateItem(playerLevel, entry.itemType, difficulty));
            }
        }
    }
    return dropped;
}

    private static class LootEntry {
        String itemType;
        float chance;
        LootEntry(String itemType, float chance) {
            this.itemType = itemType;
            this.chance = chance;
        }
    }
}