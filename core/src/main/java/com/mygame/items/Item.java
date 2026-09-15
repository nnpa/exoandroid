package com.mygame.items;

import com.jme3.math.ColorRGBA;
import com.mygame.managers.LocalizationManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Item {
    public String getLocalizedType() {
    return LocalizationManager.getInstance().get("item.type." + type);
}
    private int difficulty;

    private String id;
    private String name;
    private String type;
    private int level;
    private ItemRarity rarity;
    private String description;
    private int damage;
    private int defense;
    private int healthBonus;
    private int manaBonus;
    private String iconPath;
    private int socketCount;
    private List<String> runes;
    private ColorRGBA fallbackColor;
public int getDifficulty() { return difficulty; }
public void setDifficulty(int difficulty) { this.difficulty = difficulty; }
    public Item() {
        runes = new ArrayList<>();
        fallbackColor = generateRandomColor();
        socketCount = 0;
        healthBonus = 0;
        manaBonus = 0;
        damage = 0;
        defense = 0;
    }

    public Item(String id, String name, String type, int level, ItemRarity rarity, String description,
                int damage, int defense, String iconPath) {
        this();
        this.id = id;
        this.name = name;
        this.type = type;
        this.level = level;
        this.rarity = rarity;
        this.description = description;
        this.damage = damage;
        this.defense = defense;
        this.iconPath = iconPath;
    }

    // Геттеры и сеттеры (без изменений)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public ItemRarity getRarity() { return rarity; }
    public void setRarity(ItemRarity rarity) { this.rarity = rarity; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getDamage() { return damage; }
    public void setDamage(int damage) { this.damage = damage; }
    public int getDefense() { return defense; }
    public void setDefense(int defense) { this.defense = defense; }
    public int getHealthBonus() { return healthBonus; }
    public void setHealthBonus(int healthBonus) { this.healthBonus = healthBonus; }
    public int getManaBonus() { return manaBonus; }
    public void setManaBonus(int manaBonus) { this.manaBonus = manaBonus; }
    public String getIconPath() { return iconPath; }
    public void setIconPath(String iconPath) { this.iconPath = iconPath; }
    public int getSocketCount() { return socketCount; }
    public void setSocketCount(int socketCount) { this.socketCount = socketCount; }
    public List<String> getRunes() { return runes; }
    public void setRunes(List<String> runes) { this.runes = runes; }
    public ColorRGBA getFallbackColor() {
        if (fallbackColor == null) fallbackColor = generateRandomColor();
        return fallbackColor;
    }
    public void setFallbackColor(ColorRGBA color) { this.fallbackColor = color; }
    public ColorRGBA getColor() { return rarity != null ? rarity.getColor() : ColorRGBA.White; }

    private ColorRGBA generateRandomColor() {
        float r = 0.3f + (float) Math.random() * 0.7f;
        float g = 0.3f + (float) Math.random() * 0.7f;
        float b = 0.3f + (float) Math.random() * 0.7f;
        return new ColorRGBA(r, g, b, 1f);
    }

    // ================================================================
    //   ПАРСЕР С ЯВНЫМИ ПРОВЕРКАМИ И ВЫВОДОМ ОШИБОК (без try-catch)
    // ================================================================
    public static Item fromMap(Map<String, Object> map) {
        if (map == null) {
            System.err.println("[Item.fromMap] Map is null!");
            return null;
        }

        Item item = new Item();

        // 1. Парсим строковые поля
        Object idObj = map.get("id");
        if (idObj == null) {
            System.err.println("[Item.fromMap] Missing key: id");
            return null;
        }
        item.setId(String.valueOf(idObj));

        Object nameObj = map.get("name");
        if (nameObj == null) {
            System.err.println("[Item.fromMap] Missing key: name");
            return null;
        }
        item.setName(String.valueOf(nameObj));

        Object typeObj = map.get("type");
        if (typeObj == null) {
            System.err.println("[Item.fromMap] Missing key: type");
            return null;
        }
        item.setType(String.valueOf(typeObj));

        Object descObj = map.get("description");
        if (descObj != null) {
            item.setDescription(String.valueOf(descObj));
        }

        Object iconObj = map.get("iconPath");
        if (iconObj != null) {
            item.setIconPath(String.valueOf(iconObj));
        }

        // 2. Парсим числовые поля (только если они есть, иначе оставляем 0)
        Object levelObj = map.get("level");
        if (levelObj instanceof Number) {
            item.setLevel(((Number) levelObj).intValue());
        } else if (levelObj != null) {
            System.err.println("[Item.fromMap] level is not a Number: " + levelObj);
            return null;
        }

        Object damageObj = map.get("damage");
        if (damageObj instanceof Number) {
            item.setDamage(((Number) damageObj).intValue());
        } else if (damageObj != null) {
            System.err.println("[Item.fromMap] damage is not a Number: " + damageObj);
            return null;
        }

        Object defenseObj = map.get("defense");
        if (defenseObj instanceof Number) {
            item.setDefense(((Number) defenseObj).intValue());
        } else if (defenseObj != null) {
            System.err.println("[Item.fromMap] defense is not a Number: " + defenseObj);
            return null;
        }

        Object hpObj = map.get("healthBonus");
        if (hpObj instanceof Number) {
            item.setHealthBonus(((Number) hpObj).intValue());
        } else if (hpObj != null) {
            System.err.println("[Item.fromMap] healthBonus is not a Number: " + hpObj);
            return null;
        }

        Object mpObj = map.get("manaBonus");
        if (mpObj instanceof Number) {
            item.setManaBonus(((Number) mpObj).intValue());
        } else if (mpObj != null) {
            System.err.println("[Item.fromMap] manaBonus is not a Number: " + mpObj);
            return null;
        }

        Object diffObj = map.get("difficulty");
if (diffObj instanceof Number) {
    item.setDifficulty(((Number) diffObj).intValue());
}

        Object socketObj = map.get("socketCount");
        if (socketObj instanceof Number) {
            item.setSocketCount(((Number) socketObj).intValue());
        } else if (socketObj != null) {
            System.err.println("[Item.fromMap] socketCount is not a Number: " + socketObj);
            return null;
        }

        // 3. Парсим редкость
        Object rarityObj = map.get("rarity");
        if (rarityObj != null) {
            String rarityStr = String.valueOf(rarityObj);
            try {
                item.setRarity(ItemRarity.valueOf(rarityStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                System.err.println("[Item.fromMap] Unknown rarity: " + rarityStr);
                item.setRarity(ItemRarity.COMMON); // fallback
            }
        } else {
            System.err.println("[Item.fromMap] Missing rarity, using COMMON");
            item.setRarity(ItemRarity.COMMON);
        }

        System.out.println("[Item.fromMap] Item created successfully: " + item.getName());
        return item;
    }
}