package com.mygame.monsters;

import com.mygame.items.Item;
import com.mygame.items.ItemRarity;
import com.mygame.items.LootTable;
import com.mygame.items.ItemGenerator;

public class Goblin extends MeleMonster {

    public Goblin() {
        setId("goblin");
        setName("goblin");
        setLevel(1);
        setMaxHealth(30);
        setHealth(30);
        setDamage(5);
        setAttackRange(1.5f);
        setMoveSpeed(2.0f);
        setAggroRange(8.0f);

        // Таблица дропа – только одеваемые предметы
LootTable loot = new LootTable();
loot.addEntry("Weapon", 0.25f);
loot.addEntry("Helmet", 0.2f);
loot.addEntry("Chest", 0.2f);
loot.addEntry("Shield", 0.15f);
loot.addEntry("Legs", 0.2f);
loot.addEntry("Boots", 0.15f);
loot.addEntry("Gloves", 0.15f);
setLootTable(loot);

        setLootTable(loot);
    }
}