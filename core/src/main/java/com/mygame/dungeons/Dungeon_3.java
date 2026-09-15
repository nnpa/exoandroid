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
public class Dungeon_3 implements DungeonChestData {

    private static final Vector3f[] CHEST_POSITIONS = {
            new Vector3f(28.79609f, -0.502216f, -82.41226f),
            new Vector3f(94.04763f, -0.50221646f, -152.96713f),
            new Vector3f(-76.47538f, 0.50221014f, -212.92484f)

    };
//[Player] Position: x=28.79609, y=-0.502216, z=-82.41226z
    //[Player] Position: x=94.04763, y=-0.50221646, z=-152.96713
    //[Player] Position: x=-76.47538, y=-0.50221014, z=-212.92484
    @Override
    public List<Vector3f> getChestPositions() {
        return Arrays.asList(CHEST_POSITIONS);
    }
}