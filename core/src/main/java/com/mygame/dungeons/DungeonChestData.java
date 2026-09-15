package com.mygame.dungeons;

import com.jme3.math.Vector3f;

import java.util.List;

/**
 * Реализуется классами вида Dungeon_1, Dungeon_2 и т.д.
 * Каждый такой класс описывает координаты сундуков
 * для конкретного данжа.
 */
public interface DungeonChestData {

    List<Vector3f> getChestPositions();
}