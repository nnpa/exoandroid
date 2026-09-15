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
public class Dungeon_2 implements DungeonChestData {

    private static final Vector3f[] CHEST_POSITIONS = {

    };
    @Override
    public List<Vector3f> getChestPositions() {
        return Arrays.asList(CHEST_POSITIONS);
    }
}