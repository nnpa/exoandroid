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
public class Dungeon_4 implements DungeonChestData {

    private static final Vector3f[] CHEST_POSITIONS = {
            new Vector3f(10.699587f, -1.3988507f, -77.66368f),
            new Vector3f(-359.0642f, -1.3988576f, -147.8412f),
            new Vector3f(-881.53766f, -1.3988624f, -578.35846f),
            new Vector3f(-987.24976f, -1.3988544f, -578.35846f)


    };

    @Override
    public List<Vector3f> getChestPositions() {
        return Arrays.asList(CHEST_POSITIONS);
    }
}