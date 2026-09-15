package com.mygame.dungeons;

import java.util.ArrayList;
import java.util.List;

public class Dungeon {
    private String id;
    private List<MonsterSpawn> spawns = new ArrayList<>();

    public Dungeon(String id) {
        this.id = id;
    }

    public String getId() { return id; }
    public List<MonsterSpawn> getSpawns() { return spawns; }
    public void addSpawn(MonsterSpawn spawn) { spawns.add(spawn); }

    public static class MonsterSpawn {
        public String className;
        public float x, y, z;
        public int level;
        public float health, damage;
        public String nextDungeon;          // следующий данж после босса
        public boolean isBoss;              // флаг босса
        public boolean isFinalBoss;         // финальный босс (увеличивает сложность)
        public boolean increaseDifficulty;  // увеличивать ли сложность после смерти

        public MonsterSpawn(String className, float x, float y, float z, int level,
                            float health, float damage, String nextDungeon,
                            boolean isBoss, boolean isFinalBoss, boolean increaseDifficulty) {
            this.className = className;
            this.x = x; this.y = y; this.z = z;
            this.level = level;
            this.health = health;
            this.damage = damage;
            this.nextDungeon = nextDungeon;
            this.isBoss = isBoss;
            this.isFinalBoss = isFinalBoss;
            this.increaseDifficulty = increaseDifficulty;
        }
    }
}