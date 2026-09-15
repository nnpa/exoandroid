package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;

import com.simsilica.lemur.Button;
import com.simsilica.lemur.Label;

import java.util.Arrays;
import java.util.List;

/**
 * Окно со списком горячих клавиш. Показывается один раз за сессию
 * сразу после входа в игру (если пользователь не отключил его
 * навсегда кнопкой "Больше не показывать").
 */
public class ControlsHelpWindow {

    private final SimpleApplication app;
    private final UIManager uiManager;

    private Node windowNode;
    private boolean isVisible = false;

    private float scale = 1f;
    private float winW;
    private float winH;

    private BitmapFont currentFont;

    private static class KeyRow {
        String key;
        String descriptionKey;

        KeyRow(String key, String descriptionKey) {
            this.key = key;
            this.descriptionKey = descriptionKey;
        }
    }

    private final List<KeyRow> keyRows = Arrays.asList(
            new KeyRow("T", "controls.talents"),
            new KeyRow("C", "controls.character_stats"),
            new KeyRow("I", "controls.inventory"),
            new KeyRow("N", "controls.teleport"),
            new KeyRow("Q", "controls.health_potion"),
            new KeyRow("W", "controls.mana_potion"),
            new KeyRow("1", "controls.skill1"),
            new KeyRow("2", "controls.skill2"),
            new KeyRow("3", "controls.skill3"),
            new KeyRow("4", "controls.skill4"),
            new KeyRow("~", "controls.settings")
    );

    public ControlsHelpWindow(
            SimpleApplication app,
            UIManager uiManager
    ) {

        this.app = app;
        this.uiManager = uiManager;

        loadCurrentFont();
        createWindow();
    }

    private void loadCurrentFont() {

        String language =
                SettingsManager.getInstance().getLanguage();

        String path = "Interface/Fonts/ru.fnt";


        try {
            currentFont = app.getAssetManager().loadFont(path);
        } catch (Exception e) {
            System.err.println(
                    "[ControlsHelpWindow] Failed to load font: " + e.getMessage()
            );
            currentFont = null;
        }
    }

    private void updateScale() {

        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();

        float scaleX = screenWidth / 800f;
        float scaleY = screenHeight / 600f;

        scale = Math.min(scaleX, scaleY);
        scale = Math.max(0.5f, Math.min(scale, 1.5f));
    }

    private void createWindow() {

        updateScale();

        windowNode = new Node("ControlsHelpWindowNode");

        winW = 380f * scale;
        winH = (110f + keyRows.size() * 28f + 50f) * scale;

        Geometry bg = uiManager.createBackgroundGeometry(winW, winH);
        bg.setLocalTranslation(0f, 0f, -0.1f);
        windowNode.attachChild(bg);

        Label titleLabel = createLabel(getLocalized("controls.title"), 22f);
        titleLabel.setLocalTranslation(winW / 2f - 90f * scale, winH - 30f * scale, 0.1f);
        windowNode.attachChild(titleLabel);

        float startY = winH - 70f * scale;
        float rowHeight = 28f * scale;

        for (int i = 0; i < keyRows.size(); i++) {

            KeyRow row = keyRows.get(i);
            float y = startY - i * rowHeight;

            Label keyLabel = createLabel("[" + row.key + "]", 15f);
            keyLabel.setColor(new ColorRGBA(1f, 0.85f, 0.3f, 1f));
            keyLabel.setLocalTranslation(20f * scale, y, 0.1f);
            windowNode.attachChild(keyLabel);

            Label descLabel = createLabel(getLocalized(row.descriptionKey), 15f);
            descLabel.setLocalTranslation(75f * scale, y, 0.1f);
            windowNode.attachChild(descLabel);
        }

        // =========================================================
        // КНОПКИ
        // =========================================================

        float buttonY = 20f * scale;

        Button dontShowButton = createButton(getLocalized("controls.dont_show_again"), 14f);
        dontShowButton.setLocalTranslation(20f * scale, buttonY, 0.1f);
        dontShowButton.addClickCommands(source -> {
            SettingsManager.getInstance().setHideControlsHelp(true);
            hide();
        });
        windowNode.attachChild(dontShowButton);

        Button closeButton = createButton(getLocalized("controls.close"), 14f);
        closeButton.setLocalTranslation(winW - 130f * scale, buttonY, 0.1f);
        closeButton.addClickCommands(source -> hide());
        windowNode.attachChild(closeButton);

        windowNode.setCullHint(Node.CullHint.Always);
        positionWindow();
    }

    private void positionWindow() {

        float w = app.getCamera().getWidth();
        float h = app.getCamera().getHeight();

        windowNode.setLocalTranslation(
                (w - winW) / 2f,
                (h - winH) / 2f,
                0f
        );
    }

    private Label createLabel(String text, float fontSize) {

        Label label = new Label(text);

        if (currentFont != null) {
            label.setFont(currentFont);
        }

        label.setFontSize(fontSize * scale);
        label.setColor(ColorRGBA.White);

        return label;
    }

    private Button createButton(String text, float fontSize) {

        Button button = new Button(text);

        if (currentFont != null) {
            button.setFont(currentFont);
        }

        button.setFontSize(fontSize * scale);

        return button;
    }

    private String getLocalized(String key) {
        return LocalizationManager.getInstance().get(key);
    }

    public void show() {

        if (isVisible) {
            return;
        }

        isVisible = true;

        positionWindow();

        windowNode.setCullHint(Node.CullHint.Dynamic);
        uiManager.attachNode(windowNode);
    }

    public void hide() {

        if (!isVisible) {
            return;
        }

        isVisible = false;

        windowNode.setCullHint(Node.CullHint.Always);
        uiManager.detachNode(windowNode);
    }

    public boolean isVisible() {
        return isVisible;
    }
}