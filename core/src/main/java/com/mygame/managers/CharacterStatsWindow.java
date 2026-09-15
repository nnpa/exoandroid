package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import com.simsilica.lemur.Button;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.event.MouseEventControl;
import com.simsilica.lemur.event.MouseListener;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Окно "Характеристики персонажа".
 */
public class CharacterStatsWindow {

    private final SimpleApplication app;
    private final UIManager uiManager;
    private final PlayerManager playerManager;

    private Node windowNode;
    private boolean isVisible = false;

    private float scale = 1f;
    private float winW;
    private float winH;

    private BitmapFont currentFont;

    private List<Label> valueLabels = new java.util.ArrayList<>();
    private List<StatRow> statRows;

    private static class StatRow {
        String labelKey;
        Supplier<String> valueSupplier;

        StatRow(String labelKey, Supplier<String> valueSupplier) {
            this.labelKey = labelKey;
            this.valueSupplier = valueSupplier;
        }
    }

    public CharacterStatsWindow(
            SimpleApplication app,
            UIManager uiManager,
            PlayerManager playerManager
    ) {
        this.app = app;
        this.uiManager = uiManager;
        this.playerManager = playerManager;

        loadCurrentFont();
        buildStatRows();
        createWindow();
    }

    // =============================================================
    // ANDROID-SAFE BUTTON BINDING
    // =============================================================

    /**
     * Привязывает действие к кнопке через DOWN-событие.
     *
     * addClickCommands срабатывает по UP, которое на Android при тапе
     * нередко теряется — кнопка мигает, но действие не выполняется.
     * Здесь — как в InventoryManager/TraderWindow/AuctionWindow/
     * TalentWindow/BlacksmithWindow/VirtualKeyboard.
     */
    private void bindTouchAction(Button button, final Runnable action) {

        MouseEventControl.addListenersToSpatial(button, new MouseListener() {

            @Override
            public void mouseButtonEvent(MouseButtonEvent evt,
                                         Spatial spatial,
                                         Spatial target) {
                if (evt.getButtonIndex() != 0) return;
                if (!evt.isPressed()) return;
                action.run();
            }

            @Override public void mouseEntered(MouseMotionEvent evt, Spatial s, Spatial t) {}
            @Override public void mouseExited(MouseMotionEvent evt, Spatial s, Spatial t) {}
            @Override public void mouseMoved(MouseMotionEvent evt, Spatial s, Spatial t) {}
        });
    }

    // =============================================================
    // ШРИФТ
    // =============================================================

    private void loadCurrentFont() {
        String language = SettingsManager.getInstance().getLanguage();
        String path = "ru".equalsIgnoreCase(language)
                ? "Interface/Fonts/ru.fnt"
                : "Interface/Fonts/en.fnt";

        try {
            currentFont = app.getAssetManager().loadFont(path);
        } catch (Exception e) {
            System.err.println("[CharacterStatsWindow] Failed to load font: " + e.getMessage());
            currentFont = null;
        }
    }

    // =============================================================
    // СПИСОК СТАТОВ
    // =============================================================

    private void buildStatRows() {
        statRows = Arrays.asList(
                new StatRow("stats.health", () ->
                        playerManager.getHealth() + " / " + playerManager.getMaxHealth()),
                new StatRow("stats.mana", () ->
                        playerManager.getMana() + " / " + playerManager.getMaxMana()),
                new StatRow("stats.base_damage", () ->
                        formatNumber(playerManager.getBaseDamage())),
                new StatRow("stats.physical_defense", () ->
                        formatPercent(playerManager.getPhysicalDefense())),
                new StatRow("stats.armor", () ->
                        formatPercent(playerManager.getArmor())),
                new StatRow("stats.magical_defense", () ->
                        formatPercent(playerManager.getMagicalDefense())),
                new StatRow("stats.block_chance", () ->
                        formatPercent(playerManager.getBlockChance())),
                new StatRow("stats.crit_chance", () ->
                        formatPercent(playerManager.getCritChance())),
                new StatRow("stats.crit_damage", () ->
                        formatPercent(playerManager.getCritDamage())),
                new StatRow("stats.crit_damage_reduction", () ->
                        formatPercent(playerManager.getCritDamageReduction())),
                new StatRow("stats.attack_speed", () ->
                        formatPercent((playerManager.getAttackSpeed() - 1f) * 100f)),
                new StatRow("stats.hit_chance", () ->
                        formatPercent(playerManager.getHitChance())),
                new StatRow("stats.heal_power", () ->
                        formatPercent(playerManager.getHealPower())),
                new StatRow("stats.incoming_heal", () ->
                        formatPercent(playerManager.getIncomingHeal())),
                new StatRow("stats.holy_damage", () ->
                        formatPercent(playerManager.getHolyDamagePercent())),
                new StatRow("stats.light_damage", () ->
                        formatPercent(playerManager.getLightDamagePercent())),
                new StatRow("stats.shield_from_heal", () ->
                        formatPercent(playerManager.getShieldFromHeal())),
                new StatRow("stats.mana_on_heal", () ->
                        formatPercent(playerManager.getManaOnHealPercent())),
                new StatRow("stats.damage_ignored", () ->
                        formatPercent(playerManager.getDamageIgnored())),
                new StatRow("stats.whirlwind_radius", () ->
                        formatNumber(playerManager.getWhirlwindRadius())),
                new StatRow("stats.kick_stun_duration", () ->
                        formatSeconds(playerManager.getKickStunDuration())),
                new StatRow("stats.rage_attack_speed", () ->
                        formatPercent(playerManager.getRageAttackSpeed()))
        );
    }

    private String formatPercent(float value) {
        return String.format("%.1f%%", value);
    }

    private String formatNumber(float value) {
        return String.format("%.1f", value);
    }

    private String formatSeconds(float value) {
        return String.format("%.1fs", value);
    }

    // =============================================================
    // СОЗДАНИЕ ОКНА
    // =============================================================

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

        windowNode = new Node("CharacterStatsWindowNode");

        int rowCount = statRows.size();
        int columns = 2;
        int rowsPerColumn = (rowCount + columns - 1) / columns;

        winW = 520f * scale;

        /*
         * Высота: заголовок (80) + строки + место под большую
         * кнопку "ЗАКРЫТЬ" внизу (60).
         */
        winH = (80f + rowsPerColumn * 26f + 60f) * scale;

        Geometry bg = uiManager.createBackgroundGeometry(winW, winH);
        bg.setLocalTranslation(0f, 0f, -0.1f);
        windowNode.attachChild(bg);

        Label titleLabel = createLabel(getLocalized("stats.title"), 22f);
        titleLabel.setLocalTranslation(winW / 2f - 90f * scale, winH - 30f * scale, 0.1f);
        windowNode.attachChild(titleLabel);

        // ============================================================
        // БОЛЬШАЯ КНОПКА "ЗАКРЫТЬ" ВНИЗУ ПО ЦЕНТРУ
        //
        // Было: маленький "X" 25×25, addClickCommands (UP).
        // Стало: 180×50, шрифт 22*scale, DOWN-обработчик.
        // ============================================================

        Button closeButton = createButton("ЗАКРЫТЬ", 22f);
        closeButton.setPreferredSize(new Vector3f(180f * scale, 50f * scale, 0f));
        closeButton.setLocalTranslation(
                winW / 2f - 90f * scale,
                15f * scale,
                0.1f
        );
        bindTouchAction(closeButton, this::hide);
        windowNode.attachChild(closeButton);

        valueLabels.clear();

        float startY = winH - 70f * scale;
        float rowHeight = 26f * scale;
        float colWidth = winW / columns;

        for (int i = 0; i < rowCount; i++) {

            int col = i / rowsPerColumn;
            int row = i % rowsPerColumn;

            float x = 20f * scale + col * colWidth;
            float y = startY - row * rowHeight;

            StatRow statRow = statRows.get(i);

            Label nameLabel = createLabel(getLocalized(statRow.labelKey) + ":", 14f);
            nameLabel.setLocalTranslation(x, y, 0.1f);
            windowNode.attachChild(nameLabel);

            Label valueLabel = createLabel(statRow.valueSupplier.get(), 14f);
            valueLabel.setColor(new ColorRGBA(0.6f, 1f, 0.6f, 1f));
            valueLabel.setLocalTranslation(x + colWidth - 110f * scale, y, 0.1f);
            windowNode.attachChild(valueLabel);

            valueLabels.add(valueLabel);
        }

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

    // =============================================================
    // ОБНОВЛЕНИЕ ЗНАЧЕНИЙ
    // =============================================================

    public void refreshValues() {
        for (int i = 0; i < valueLabels.size() && i < statRows.size(); i++) {
            valueLabels.get(i).setText(statRows.get(i).valueSupplier.get());
        }
    }

    // =============================================================
    // LABEL / BUTTON
    // =============================================================

    private Label createLabel(String text, float fontSize) {
        Label label = new Label(text);
        if (currentFont != null) label.setFont(currentFont);
        label.setFontSize(fontSize * scale);
        label.setColor(ColorRGBA.White);
        return label;
    }

    private Button createButton(String text, float fontSize) {
        Button button = new Button(text);
        if (currentFont != null) button.setFont(currentFont);
        button.setFontSize(fontSize * scale);
        return button;
    }

    private String getLocalized(String key) {
        return LocalizationManager.getInstance().get(key);
    }

    // =============================================================
    // SHOW / HIDE
    // =============================================================

    public void show() {
        if (isVisible) return;
        isVisible = true;
        refreshValues();
        positionWindow();
        windowNode.setCullHint(Node.CullHint.Dynamic);
        uiManager.attachNode(windowNode);
    }

    public void hide() {
        if (!isVisible) return;
        isVisible = false;
        windowNode.setCullHint(Node.CullHint.Always);
        uiManager.detachNode(windowNode);
    }

    public boolean isVisible() {
        return isVisible;
    }
}