/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mygame.managers;


import com.atr.jme.font.TrueTypeFont;
import com.atr.jme.font.asset.TrueTypeKeyBMP;
import com.atr.jme.font.asset.TrueTypeLoader;
import com.atr.jme.font.shape.TrueTypeContainer;
import com.atr.jme.font.shape.TrueTypeNode;
import com.atr.jme.font.util.Style;
import com.jme3.asset.AssetManager;
import com.jme3.math.ColorRGBA;

public class FontFactory {
    private static TrueTypeFont ttfFont;

    // Вызвать один раз при старте приложения
    public static void init(AssetManager assetManager) {
        assetManager.registerLoader(TrueTypeLoader.class, "ttf");
        TrueTypeKeyBMP key = new TrueTypeKeyBMP(
            "Interface/Fonts/main.ttf",
            Style.Plain,
            24
        );
        ttfFont = (TrueTypeFont) assetManager.loadAsset(key);
    }

    // Создать текст с заданным цветом
public static TrueTypeNode createText(String text, ColorRGBA color) {
    if (ttfFont == null) {
        throw new IllegalStateException("FontFactory not initialized!");
    }
    return ttfFont.getText(text, 0, color);
}

public static TrueTypeNode createText(String text) {
    return createText(text, ColorRGBA.White);
}
}