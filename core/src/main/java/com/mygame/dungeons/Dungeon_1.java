package com.mygame.dungeons;

import com.jme3.math.Vector3f;

import java.util.Arrays;
import java.util.List;

/**
 * Координаты сундуков для dungeon_1.
 *
 * Имя класса ДОЛЖНО совпадать с dungeonId
 * с заглавной первой буквой:
 *
 * dungeonId "dungeon_1" -> класс Dungeon_1
 */
public class Dungeon_1 implements DungeonChestData {

    private static final Vector3f[] CHEST_POSITIONS = {
            new Vector3f(-46.279446f, 1.8264819f, -83.8622f),
            new Vector3f(-190.06467f, 0.87249154f, -40.403873f),
            new Vector3f(-341.52838f, 1.7697734f, -77.0069f)

    };
//[Player] Position: x=-46.279446, y=1.8264819, z=-83.8622
    //[Player] Position: x=-190.06467, y=0.87249154, z=-40.403873
    //[Player] Position: x=-341.52838, y=1.7697734, z=-77.0069
    @Override
    public List<Vector3f> getChestPositions() {
        return Arrays.asList(CHEST_POSITIONS);
    }
}