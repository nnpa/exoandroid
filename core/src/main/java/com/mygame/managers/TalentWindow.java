package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.event.MouseEventControl;
import com.simsilica.lemur.event.MouseListener;

import java.util.*;

public class TalentWindow {

    /**
     * Множитель размера шрифтов — как и в других окнах
     * (InventoryManager, TraderWindow, AuctionWindow,
     * VirtualKeyboard, BlacksmithWindow).
     */
    private static final float FONT_MULT = 2f;

    private SimpleApplication app;
    private TalentManager talentManager;
    private UIManager uiManager;
    private Node windowNode;
    private boolean isVisible = false;

    private Label pointsLabel;
    private Label tooltipLabel;
    private float tooltipTimer = 0f;
    private boolean tooltipVisible = false;
    private List<Button> talentButtons = new ArrayList<>();
    private Talent.Branch currentBranch = Talent.Branch.DEFENSE;
    private Map<String, Long> lastClickTime = new HashMap<>();

    private float currentWidth = 500;
    private float currentHeight = 550;
    private float buttonSize = 60;
    private float buttonSpacingH = 20;
    private float buttonSpacingV = 30;

    private static final Map<String, String> ICON_MAP = new HashMap<>();
    static {
        ICON_MAP.put("def_1", "talent_shield.png");
        ICON_MAP.put("def_2", "talent_iron_skin.png");
        ICON_MAP.put("def_3", "talent_magic_barrier.png");
        ICON_MAP.put("def_4", "talent_block.png");
        ICON_MAP.put("def_5", "talent_resilience.png");
        ICON_MAP.put("def_6", "talent_regen.png");
        ICON_MAP.put("def_7", "talent_fortify.png");
        ICON_MAP.put("def_8", "talent_impervious.png");
        ICON_MAP.put("def_9", "talent_undying.png");
        ICON_MAP.put("def_10", "talent_aura.png");
        ICON_MAP.put("light_1", "talent_heal.png");
        ICON_MAP.put("light_2", "talent_divine_shield.png");
        ICON_MAP.put("light_3", "talent_holy_strike.png");
        ICON_MAP.put("light_4", "talent_cleanse.png");
        ICON_MAP.put("light_5", "talent_holy_power.png");
        ICON_MAP.put("light_6", "talent_angelic.png");
        ICON_MAP.put("light_7", "talent_blessing.png");
        ICON_MAP.put("light_8", "talent_inspire.png");
        ICON_MAP.put("light_9", "talent_light_nova.png");
        ICON_MAP.put("light_10", "talent_resurrect.png");
        ICON_MAP.put("attack_1", "talent_bash.png");
        ICON_MAP.put("attack_2", "talent_sweep.png");
        ICON_MAP.put("attack_3", "talent_kick.png");
        ICON_MAP.put("attack_4", "talent_wrath.png");
        ICON_MAP.put("attack_5", "talent_critical.png");
        ICON_MAP.put("attack_6", "talent_mastery.png");
        ICON_MAP.put("attack_7", "talent_strength.png");
        ICON_MAP.put("attack_8", "talent_precision.png");
        ICON_MAP.put("attack_9", "talent_lethal.png");
        ICON_MAP.put("attack_10", "talent_rage.png");
    }

    public TalentWindow(SimpleApplication app, TalentManager tm, UIManager ui) {
        this.app = app;
        this.talentManager = tm;
        this.uiManager = ui;
        createTooltip();
        createWindow();
    }

    public boolean isVisible() { return isVisible; }
    public Node getNode() { return windowNode; }

    private String getLocalized(String key) {
        return LocalizationManager.getInstance().get(key);
    }

    // ============================================================
    // ANDROID-SAFE BUTTON BINDING
    // ============================================================

    /**
     * Привязывает действие к кнопке через DOWN-событие.
     *
     * ВАЖНО: не используем button.addClickCommands(...) — этот путь
     * срабатывает по UP-событию, которое на Android при тапе
     * нередко теряется (см. InventoryManager/TraderWindow/AuctionWindow).
     * Кнопка мигает нажатой, но действие не выполняется.
     */
    private void bindTouchAction(Button button, final Runnable action) {

        MouseEventControl.addListenersToSpatial(button, new MouseListener() {

            @Override
            public void mouseButtonEvent(MouseButtonEvent evt,
                                         Spatial spatial,
                                         Spatial target) {

                if (!isVisible) return;
                if (evt.getButtonIndex() != 0) return;
                if (!evt.isPressed()) return;

                action.run();
            }

            @Override public void mouseEntered(MouseMotionEvent evt, Spatial s, Spatial t) {}
            @Override public void mouseExited(MouseMotionEvent evt, Spatial s, Spatial t) {}
            @Override public void mouseMoved(MouseMotionEvent evt, Spatial s, Spatial t) {}
        });
    }

private void createTooltip() {
    tooltipLabel = new Label("");

    // Шрифт ×2 (как в остальных окнах).
    tooltipLabel.setFontSize(14 * FONT_MULT);
    tooltipLabel.setColor(ColorRGBA.White);
    tooltipLabel.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.1f, 0.1f, 0.2f, 0.95f)));

    // Под увеличенный шрифт нужно больше места.
    tooltipLabel.setPreferredSize(new Vector3f(600, 160, 0));

    // ============================================================
    // ТУЛТИП ОПУЩЕН НИЖЕ ПАНЕЛИ СТАТОВ (ник/HP/MP).
    //
    // Панель статов живёт в UIManager в левом верхнем углу
    // и занимает примерно [screenHeight − 110*scale,
    //                       screenHeight − 30*scale].
    //
    // Раньше тултип стартовал на Y = screenHeight − 170
    // и на некоторых разрешениях заезжал на статы.
    //
    // Теперь он стартует на Y = screenHeight − 340 — с
    // гарантированным запасом ниже статов при любом scale
    // (0.5 … 1.5). На узких экранах Y дополнительно
    // ограничен сверху, чтобы тултип не уехал в самый низ
    // окна талантов.
    // ============================================================
    float screenHeight = app.getCamera().getHeight();
    float tooltipTopY = screenHeight - 340f;

    // Страховка: не ниже, чем «верх окна талантов − 20».
    // Окно талантов центрируется по вертикали, его верх
    // примерно на (screenHeight + 550*scale) / 2 — возьмём
    // 550 * 1.0 как верхнюю границу.
    float minAllowedY = (screenHeight + 550f) / 2f - 20f;
    if (tooltipTopY < minAllowedY) {
        tooltipTopY = minAllowedY;
    }

    tooltipLabel.setLocalTranslation(10, tooltipTopY, 0);
    tooltipLabel.setCullHint(Node.CullHint.Always);
    app.getGuiNode().attachChild(tooltipLabel);
}
    private void createWindow() {
        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();
        if (screenWidth <= 0 || screenHeight <= 0) {
            screenWidth = 1280;
            screenHeight = 720;
        }

        float scaleX = screenWidth / 1280f;
        float scaleY = screenHeight / 720f;
        float scale = Math.min(scaleX, scaleY);

        currentWidth = 500 * scale;
        currentHeight = 550 * scale;
        buttonSize = 60 * scale;
        buttonSpacingH = 20 * scale;
        buttonSpacingV = 30 * scale;

        float x = (screenWidth - currentWidth) / 2;
        float y = (screenHeight - currentHeight) / 2 + 48 * scale + 50 * scale;

        windowNode = new Node("TalentWindowNode");
        windowNode.setName("TalentWindowNode");
        windowNode.setLocalTranslation(x, y, 0);

        // Фон
        if (uiManager != null) {
            Geometry bgGeom = uiManager.createBackgroundGeometry(currentWidth, currentHeight);
            windowNode.attachChild(bgGeom);
        } else {
            Quad quad = new Quad(currentWidth, currentHeight);
            Geometry background = new Geometry("Bg", quad);
            Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", new ColorRGBA(0.08f, 0.08f, 0.15f, 0.97f));
            background.setMaterial(mat);
            background.setLocalTranslation(0, 0, -0.1f);
            windowNode.attachChild(background);
        }

        Label title = new Label(getLocalized("talents.title"));
        title.setFontSize(24 * scale);
        title.setColor(ColorRGBA.White);
        title.setLocalTranslation(currentWidth / 2 - 45 * scale, currentHeight - 25 * scale, 0.1f);
        windowNode.attachChild(title);

        float tabY = currentHeight - 60 * scale;

        // ============================================================
        // TABS — DOWN-обработчик (bindTouchAction)
        // ============================================================

        Button defTab = new Button(getLocalized("talents.tab.defense"));
        defTab.setPreferredSize(new Vector3f(90 * scale, 25 * scale, 0));
        defTab.setFontSize(12 * scale);
        defTab.setLocalTranslation(40 * scale, tabY, 0.1f);
        bindTouchAction(defTab, () -> switchBranch(Talent.Branch.DEFENSE));
        windowNode.attachChild(defTab);

        Button lightTab = new Button(getLocalized("talents.tab.light"));
        lightTab.setPreferredSize(new Vector3f(90 * scale, 25 * scale, 0));
        lightTab.setFontSize(12 * scale);
        lightTab.setLocalTranslation(150 * scale, tabY, 0.1f);
        bindTouchAction(lightTab, () -> switchBranch(Talent.Branch.LIGHT));
        windowNode.attachChild(lightTab);

        Button attackTab = new Button(getLocalized("talents.tab.attack"));
        attackTab.setPreferredSize(new Vector3f(90 * scale, 25 * scale, 0));
        attackTab.setFontSize(12 * scale);
        attackTab.setLocalTranslation(260 * scale, tabY, 0.1f);
        bindTouchAction(attackTab, () -> switchBranch(Talent.Branch.ATTACK));
        windowNode.attachChild(attackTab);

        pointsLabel = new Label(getLocalized("talents.points") + "0");
        pointsLabel.setFontSize(16 * scale);
        pointsLabel.setColor(ColorRGBA.White);
        pointsLabel.setLocalTranslation(15 * scale, 25 * scale, 0.1f);
        windowNode.attachChild(pointsLabel);

        // ============================================================
        // RESET  — увеличена: 70×25 → 180×50, шрифт 12 → 22
        //
        // Кнопка уже была переведена на bindTouchAction, здесь
        // меняем только размер/шрифт/позицию.
        // ============================================================

        Button resetButton = new Button(getLocalized("talents.reset"));
        resetButton.setPreferredSize(new Vector3f(180 * scale, 50 * scale, 0));
        resetButton.setFontSize(22 * scale);
        resetButton.setColor(ColorRGBA.White);
        resetButton.setLocalTranslation(
                currentWidth - 200 * scale,
                25 * scale,
                0.1f);
        bindTouchAction(resetButton, () -> {
            SoundManager.playSound(SoundManager.SOUND_CLICK);
            if (isVisible) {
                talentManager.resetTalentsAsync().thenAccept(success -> {
                    app.enqueue(() -> {
                        if (success) {
                            updateUI();
                            showTooltip("✓ " + getLocalized("talents.reset.success"));
                        } else {
                            showTooltip("✗ " + getLocalized("talents.reset.fail"));
                        }
                        return null;
                    });
                });
            }
        });
        windowNode.attachChild(resetButton);

        // ============================================================
        // CLOSE  — теперь жёстко «ЗАКРЫТЬ» капсом (как в
        // InventoryManager / TraderWindow / AuctionWindow /
        // BlacksmithWindow), 25×25 → 180×50, шрифт 14 → 22.
        // ============================================================

        Button closeButton = new Button(getLocalized("ui.close_button"));
        closeButton.setPreferredSize(new Vector3f(180 * scale, 50 * scale, 0));
        closeButton.setFontSize(22 * scale);
        closeButton.setColor(ColorRGBA.White);
        closeButton.setLocalTranslation(
                currentWidth - 170 * scale,
                currentHeight - 60 * scale,
                0.1f);
        bindTouchAction(closeButton, () -> hide());
        windowNode.attachChild(closeButton);

        switchBranch(Talent.Branch.DEFENSE);
    }

    private void switchBranch(Talent.Branch branch) {
        this.currentBranch = branch;
        updateUI();
    }

    private List<Spatial> talentContainers = new ArrayList<>();
    private boolean tooltipPersistent = false;

    public void updateUI() {

        // ============================================================
        // 1. Удаляем старые элементы талантов
        // ============================================================

        for (Spatial spatial : talentContainers) {

            if (spatial == null) {
                continue;
            }

            MouseEventControl.removeListenersFromSpatial(spatial);

            if (spatial.getParent() == windowNode) {
                windowNode.detachChild(spatial);
            }
        }

        talentContainers.clear();

        // ============================================================
        // 2. Получаем текущее дерево талантов
        // ============================================================

        TalentTree tree = talentManager.getTrees().get(currentBranch);

        if (tree == null) {

            if (pointsLabel != null) {
                pointsLabel.setText(
                        getLocalized("talents.points")
                                + talentManager.getAvailablePoints()
                );
            }

            return;
        }

        List<Talent> talents = new ArrayList<>(tree.getTalents());

        talents.sort((a, b) -> {
            if (a.getRow() != b.getRow()) {
                return Integer.compare(a.getRow(), b.getRow());
            }
            return Integer.compare(a.getColumn(), b.getColumn());
        });

        // ============================================================
        // 3. Размеры сетки
        // ============================================================

        float totalWidth = 2 * buttonSize + buttonSpacingH;
        float totalHeight = 5 * buttonSize + 4 * buttonSpacingV;

        float startX = (currentWidth - totalWidth) / 2f;

        float startY = currentHeight
                - 120 * (currentHeight / 550f)
                - 100;

        if (startY - totalHeight < 10) {
            startY = totalHeight + 10;
        }

        // ============================================================
        // 4. Фон окна
        // ============================================================

        Node bgNode = new Node("WindowBgNode");

        Geometry windowBg = new Geometry(
                "WindowBg",
                new Quad(currentWidth, currentHeight)
        );

        Material bgMat = new Material(
                app.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md"
        );

        bgMat.setColor("Color", new ColorRGBA(0.1f, 0.1f, 0.15f, 0.95f));

        windowBg.setMaterial(bgMat);
        windowBg.setLocalTranslation(0, 0, -0.2f);

        bgNode.attachChild(windowBg);
        windowNode.attachChild(bgNode);
        talentContainers.add(bgNode);

        // ============================================================
        // 5. Сетка 5 x 2
        // ============================================================

        for (int r = 0; r < 5; r++) {

            for (int c = 0; c < 2; c++) {

                Talent found = null;

                for (Talent talent : talents) {
                    if (talent.getRow() == r && talent.getColumn() == c) {
                        found = talent;
                        break;
                    }
                }

                float x = startX + c * (buttonSize + buttonSpacingH);
                float y = startY - r * (buttonSize + buttonSpacingV) - 20;

                Node cellNode = new Node("TalentCell_" + r + "_" + c);
                cellNode.setLocalTranslation(x, y, 0.1f);

                if (found != null) {

                    final Talent talent = found;

                    int level = talentManager
                            .getLearned()
                            .getOrDefault(talent.getId(), 0);

                    boolean hasPrereqs = tree.isAvailable(
                            talent,
                            talentManager.getLearned()
                    );

                    boolean hasPoints = talentManager.getAvailablePoints()
                            >= talent.getCost();

                    boolean isAvailable = hasPoints && hasPrereqs;

                    boolean isMaxLevel = level >= talent.getMaxLevel();

                    // =================================================
                    // Визуальная иконка
                    // =================================================

                    Geometry bg = new Geometry(
                            "TalentCellBg_" + r + "_" + c,
                            new Quad(buttonSize, buttonSize)
                    );
                    bg.setLocalTranslation(0, 0, 0);

                    Material mat = new Material(
                            app.getAssetManager(),
                            "Common/MatDefs/Misc/Unshaded.j3md"
                    );

                    String iconName = ICON_MAP.get(talent.getId());
                    Texture tex = null;

                    if (iconName != null) {
                        try {
                            tex = app.getAssetManager().loadTexture(
                                    "Interface/Talents/" + iconName
                            );
                        } catch (Exception e) {
                            System.out.println(
                                    "[TalentWindow] Не удалось загрузить "
                                            + "иконку таланта "
                                            + talent.getId()
                                            + ": "
                                            + e.getMessage()
                            );
                        }
                    }

                    if (tex != null) {
                        mat.setTexture("ColorMap", tex);
                    } else {
                        ColorRGBA color;
                        if (isMaxLevel) {
                            color = new ColorRGBA(0.8f, 0.7f, 0.1f, 0.9f);
                        } else if (isAvailable) {
                            color = new ColorRGBA(0.1f, 0.8f, 0.1f, 0.9f);
                        } else {
                            color = new ColorRGBA(0.3f, 0.3f, 0.3f, 0.9f);
                        }
                        mat.setColor("Color", color);
                    }

                    bg.setMaterial(mat);
                    cellNode.attachChild(bg);

                    // =================================================
                    // Текст таланта (с тенями)
                    // =================================================

                    String displayText = talent.getLocalizedName();
                    if (level > 0) {
                        displayText += " " + level + "/" + talent.getMaxLevel();
                    }

                    float fontSize = 15f;

                    Label temp = new Label(displayText);
                    temp.setFontSize(fontSize);
                    float tw = temp.getPreferredSize().x;
                    float th = temp.getPreferredSize().y;

                    float posX = (buttonSize - tw) / 2f;
                    float posY = (buttonSize - th) / 2f;

                    Label shadow1 = new Label(displayText);
                    shadow1.setFontSize(fontSize);
                    shadow1.setColor(new ColorRGBA(0f, 0f, 0f, 0.95f));
                    shadow1.setLocalTranslation(posX + 1, posY + 1, 0.1f);
                    cellNode.attachChild(shadow1);

                    Label shadow2 = new Label(displayText);
                    shadow2.setFontSize(fontSize);
                    shadow2.setColor(new ColorRGBA(0f, 0f, 0f, 0.8f));
                    shadow2.setLocalTranslation(posX + 2, posY + 2, 0.1f);
                    cellNode.attachChild(shadow2);

                    Label textLabel = new Label(displayText);
                    textLabel.setFontSize(fontSize);
                    textLabel.setColor(ColorRGBA.White);
                    textLabel.setLocalTranslation(posX, posY, 0.2f);
                    cellNode.attachChild(textLabel);

                    // =================================================
                    // Невидимая область клика (уже DOWN-friendly)
                    // =================================================

                    Geometry clickTarget = new Geometry(
                            "TalentClickTarget_" + r + "_" + c,
                            new Quad(buttonSize, buttonSize)
                    );
                    clickTarget.setLocalTranslation(0, 0, 0.5f);

                    Material clickMaterial = new Material(
                            app.getAssetManager(),
                            "Common/MatDefs/Misc/Unshaded.j3md"
                    );
                    clickMaterial.setColor("Color", ColorRGBA.White);
                    clickTarget.setMaterial(clickMaterial);
                    clickTarget.setCullHint(Node.CullHint.Always);

                    cellNode.attachChild(clickTarget);

                    // =================================================
                    // MouseListener таланта — уже реагирует на DOWN
                    // (isPressed), поэтому на Android работает.
                    // =================================================

                    MouseListener talentListener = new MouseListener() {

                        @Override
                        public void mouseButtonEvent(MouseButtonEvent evt,
                                                     Spatial spatial,
                                                     Spatial target) {

                            if (!evt.isPressed()) return;
                            if (evt.getButtonIndex() != 0) return;
                            if (!isVisible) return;

                            evt.setConsumed();

                            if (isMaxLevel) {
                                showTooltip(
                                        talent.getLocalizedName()
                                                + getLocalized("talents.maxlevel")
                                );
                                return;
                            }

                            if (!isAvailable) {

                                StringBuilder msg = new StringBuilder(
                                        getLocalized("talents.notavailable")
                                );

                                if (!hasPrereqs) {

                                    msg.append(getLocalized("talents.prereqmissing"));

                                    for (String prereqId : talent.getPrerequisites()) {

                                        Integer learnedLevel = talentManager
                                                .getLearned()
                                                .get(prereqId);

                                        if (learnedLevel == null || learnedLevel == 0) {

                                            Talent prereqTalent = tree.getTalentById(prereqId);

                                            if (prereqTalent != null) {
                                                msg.append(prereqTalent.getLocalizedName());
                                            } else {
                                                msg.append(prereqId);
                                            }

                                            msg.append(" ");
                                        }
                                    }

                                } else if (!hasPoints) {
                                    msg.append(getLocalized("talents.notenoughpoints"));
                                }

                                showTooltip(msg.toString());
                                return;
                            }

                            SoundManager.playSound(SoundManager.SOUND_CLICK);

                            talentManager
                                    .levelUpTalentAsync(talent.getId())
                                    .thenAccept(success -> {

                                        app.enqueue(() -> {

                                            if (success) {

                                                updateUI();

                                                showTooltip(
                                                        "✓ "
                                                                + talent.getLocalizedName()
                                                                + getLocalized("talents.upgrade.success")
                                                );

                                            } else {

                                                showTooltip(
                                                        "✗ "
                                                                + getLocalized("talents.upgrade.fail")
                                                                + " "
                                                                + talent.getLocalizedName()
                                                );
                                            }

                                            return null;
                                        });
                                    });
                        }

                        @Override
                        public void mouseEntered(MouseMotionEvent evt, Spatial spatial, Spatial target) {
                            String desc = talent.getLocalizedDescription();
                            if (desc == null || desc.isEmpty()) {
                                desc = talent.getLocalizedName();
                            }
                            showTooltipPersistent(desc);
                        }

                        @Override
                        public void mouseExited(MouseMotionEvent evt, Spatial spatial, Spatial target) {
                            hideTooltipPersistent();
                        }

                        @Override
                        public void mouseMoved(MouseMotionEvent evt, Spatial spatial, Spatial target) {}
                    };

                    MouseEventControl.removeListenersFromSpatial(clickTarget);
                    MouseEventControl.addListenersToSpatial(clickTarget, talentListener);

                } else {

                    // Пустая ячейка

                    Geometry bg = new Geometry(
                            "TalentCellBg_" + r + "_" + c,
                            new Quad(buttonSize, buttonSize)
                    );

                    Material mat = new Material(
                            app.getAssetManager(),
                            "Common/MatDefs/Misc/Unshaded.j3md"
                    );

                    mat.setColor("Color", new ColorRGBA(0.15f, 0.15f, 0.15f, 0.9f));
                    bg.setMaterial(mat);
                    cellNode.attachChild(bg);
                }

                windowNode.attachChild(cellNode);
                talentContainers.add(cellNode);
            }
        }

        // ============================================================
        // Обновляем количество очков
        // ============================================================

        if (pointsLabel != null) {
            pointsLabel.setText(
                    getLocalized("talents.points")
                            + talentManager.getAvailablePoints()
            );
        }

        // ============================================================
        // ВАЖНО про updateLogicalState / updateGeometricState
        //
        // Эти вызовы намеренно убраны — на Android они давали
        // исключение, потому что jME3 обновляет guiNode в
        // рендер-цикле каждый кадр, а повторный ручной вызов из
        // сетевого колбэка (levelUpTalentAsync) приводил к
        // двойному обновлению контролов Lemur.
        //
        // attach/detach детей сам выставляет флаги update —
        // bounds и world-transform подтянутся на следующем кадре.
        // ============================================================
    }

    private void showTooltip(String text) {
        if (tooltipLabel == null || !isVisible) return;
        tooltipLabel.setText(text);
        tooltipLabel.setCullHint(Node.CullHint.Dynamic);
        tooltipVisible = true;
        tooltipTimer = 3.0f;
    }

    private void hideTooltip() {
        if (tooltipLabel == null) return;
        tooltipLabel.setCullHint(Node.CullHint.Always);
        tooltipVisible = false;
        tooltipTimer = 0f;
    }

    public void update(float tpf) {
        if (tooltipVisible && isVisible && !tooltipPersistent) {
            tooltipTimer -= tpf;
            if (tooltipTimer <= 0) hideTooltip();
        }
    }

    private void showTooltipPersistent(String text) {
        if (tooltipLabel == null) return;
        tooltipLabel.setText(text);
        tooltipLabel.setCullHint(Node.CullHint.Dynamic);
        tooltipPersistent = true;
        tooltipVisible = true;
    }

    private void hideTooltipPersistent() {
        tooltipPersistent = false;
        hideTooltip();
    }

    public void updateLayout(int screenWidth, int screenHeight) {
        if (isVisible) {
            if (uiManager != null) uiManager.onTalentClosed(windowNode);
            windowNode.detachAllChildren();
            createWindow();
            if (uiManager != null) uiManager.onTalentOpened(windowNode);
        }
    }

    public void show() {
        isVisible = true;
        if (uiManager != null) uiManager.onTalentOpened(windowNode);
        else if (!app.getGuiNode().hasChild(windowNode)) app.getGuiNode().attachChild(windowNode);
        updateUI();
    }

    public void hide() {
        isVisible = false;
        if (uiManager != null) uiManager.onTalentClosed(windowNode);
        else if (app.getGuiNode().hasChild(windowNode)) app.getGuiNode().detachChild(windowNode);
        hideTooltip();
    }

    public void toggle() {
        if (isVisible) hide(); else show();
    }
}