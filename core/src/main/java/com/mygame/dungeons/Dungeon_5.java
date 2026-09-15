/*
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mygame.dungeons;

import com.jme3.math.Vector3f;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author User
 */
public class Dungeon_5 implements DungeonChestData {

    private static final Vector3f[] CHEST_POSITIONS = {
            new Vector3f(-81.42645f, -0.12748912f, -61.14055f),
            new Vector3f(-116.83963f, -0.12748903f, -152.96713f),
            new Vector3f(-256.5478f, -0.12748903f, -7.592849f)

    };
    @Override
    public List<Vector3f> getChestPositions() {
        return Arrays.asList(CHEST_POSITIONS);
    }
}