package com.mygame.dungeons;

import com.jme3.scene.Node;
import com.mygame.monsters.Monster;

import java.util.ArrayList;
import java.util.List;

public class DungeonManager {

    private Dungeon currentDungeon;
    private List<Monster> activeMonsters = new ArrayList<>();
    private boolean isCleared = false;

    public void setCurrentDungeon(Dungeon dungeon) {
        this.currentDungeon = dungeon;
        this.isCleared = false;
        // Не очищаем активных монстров, они добавляются отдельно
    }

    public void addMonster(Monster monster) {
        activeMonsters.add(monster);
    }

    public void removeMonster(Monster monster) {
        activeMonsters.remove(monster);
    }

    /**
     * Очищает список активных монстров (например, при перезагрузке данжа).
     */
    public void clearMonsters() {
        activeMonsters.clear();
        isCleared = false;
    }

    public List<Monster> getActiveMonsters() {
        return activeMonsters;
    }

    public boolean isCleared() {
        return isCleared;
    }

    public void update() {
        boolean allDead = true;
        for (Monster m : activeMonsters) {
            if (m.isAlive()) {
                allDead = false;
                break;
            }
        }
        if (allDead && !activeMonsters.isEmpty() && !isCleared) {
            isCleared = true;
            System.out.println("[DungeonManager] Dungeon cleared!");
        }
    }

    public void resetDungeon() {
        for (Monster m : activeMonsters) {
            m.setHealth(m.getMaxHealth());
            m.setAlive(true);
            if (m.getModelNode() != null) {
                m.getModelNode().setCullHint(Node.CullHint.Dynamic);
                m.setPosition(m.getSpawnPosition());
            }
        }
        isCleared = false;
    }
}