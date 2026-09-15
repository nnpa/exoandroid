package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.material.MatParam;
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
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.focus.FocusNavigationFunctions;
import com.mygame.Main;
import com.mygame.items.ItemGenerator;
import com.mygame.managers.GameManager.GameState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class UIManager {

    private WorldManager worldManager;

    public void setWorldManager(WorldManager wm) {
        this.worldManager = wm;
    }

    private Node backgroundNode;
    private Geometry backgroundGeom;
    private String backgroundTexturePath = "Interface/login_bg.png";
    private TalentManager talentManager;

    public TalentManager getTalentManager() {
        return talentManager;
    }

    public TalentWindow getTalentWindow() {
        if (talentWindow == null && talentManager != null) {
            createWindows(true);
        }
        return talentWindow;
    }

    private Button globalCloseButton;

    private void createGlobalCloseButton() {
        globalCloseButton = new Button(getLocalized("ui.close_button"));
        globalCloseButton.setFontSize(16f);
        globalCloseButton.setPreferredSize(new Vector3f(100f, 34f, 0f));
        globalCloseButton.setLocalTranslation(
                15f,
                app.getCamera().getHeight() - 15f,
                999f
        );
        globalCloseButton.addClickCommands(source -> {
            SoundManager.playSound(SoundManager.SOUND_CLICK);
            app.stop();
        });
        guiNode.attachChild(globalCloseButton);
    }

    public TraderWindow getTraderWindow() {
        if (traderWindow == null && playerManager != null && inventoryManager != null) {
            traderWindow = new TraderWindow(app, playerManager, inventoryManager, this);
        }
        return traderWindow;
    }

    public AuctionWindow getAuctionWindow() {
        if (auctionWindow == null && playerManager != null && inventoryManager != null) {
            auctionWindow = new AuctionWindow(app, this, inventoryManager, playerManager);
        }
        return auctionWindow;
    }

    private AuctionWindow auctionWindow;
    private TraderWindow traderWindow;

    public void openAuction() { toggleAuction(); }
    public void openTrader() { toggleTrader(); }

    public void toggleInventory() {
        if (inventoryManager == null) return;
        if (inventoryManager.isVisible()) {
            inventoryManager.hide();
            SoundManager.playSound(SoundManager.SOUND_WINDOW_CLOSE);
        } else {
            closeAllWindowsExcept("inventory");
            inventoryManager.show();
            SoundManager.playSound(SoundManager.SOUND_WINDOW_TALENTS);
        }
    }

    public void toggleTalents() {
        if (talentWindow == null) return;
        if (talentWindow.isVisible()) {
            talentWindow.hide();
            SoundManager.playSound(SoundManager.SOUND_WINDOW_CLOSE);
        } else {
            closeAllWindowsExcept("talents");
            talentWindow.show();
            SoundManager.playSound(SoundManager.SOUND_WINDOW_TALENTS);
        }
    }

    public void toggleAuction() {
        if (auctionWindow == null) {
            auctionWindow = new AuctionWindow(app, this, inventoryManager, playerManager);
        }
        if (auctionWindow.isVisible()) {
            auctionWindow.hide();
            return;
        }
        closeAllWindowsExcept("auction");
        SoundManager.playSound(SoundManager.SOUND_WINDOW_TRADER);
        auctionWindow.show();
    }

    public void toggleTrader() {
        if (traderWindow == null) {
            traderWindow = new TraderWindow(app, playerManager, inventoryManager, this);
        }
        if (traderWindow.isVisible()) {
            traderWindow.hide();
            return;
        }
        closeAllWindowsExcept("trader");
        SoundManager.playSound(SoundManager.SOUND_WINDOW_TRADER);
        traderWindow.show();
    }

    private void closeAllWindowsExcept(String keepOpen) {
        if (!"inventory".equals(keepOpen) && inventoryManager != null && inventoryManager.isVisible()) {
            inventoryManager.hide();
        }
        if (!"talents".equals(keepOpen) && talentWindow != null && talentWindow.isVisible()) {
            talentWindow.hide();
        }
        if (!"auction".equals(keepOpen) && auctionWindow != null && auctionWindow.isVisible()) {
            auctionWindow.hide();
        }
        if (!"trader".equals(keepOpen) && traderWindow != null && traderWindow.isVisible()) {
            traderWindow.hide();
        }
        if (!"settings".equals(keepOpen) && settingsWindow != null && settingsWindow.isVisible()) {
            settingsWindow.hide();
        }
        if (!"blacksmith".equals(keepOpen) && blacksmithWindow != null && blacksmithWindow.isVisible()) {
            blacksmithWindow.hide();
        }
        if (!"characterStats".equals(keepOpen) && characterStatsWindow != null && characterStatsWindow.isVisible()) {
            characterStatsWindow.hide();
        }
        if (!"controlsHelp".equals(keepOpen) && controlsHelpWindow != null && controlsHelpWindow.isVisible()) {
            controlsHelpWindow.hide();
        }
    }

    private SimpleApplication app;
    private Node guiNode;
    private NetworkManager networkManager;

    private Node hudNode;
    private boolean hudVisible = false;

    // HUD
    private Geometry hudBackground;
    private List<Button> hudButtons = new ArrayList<>();
    private Button talentButton;
    private Button inventoryButton;
    private Label hpCountLabel;
    private Label mpCountLabel;
    private Button healthPotionBtn;
    private Button manaPotionBtn;
    private Button skill1Btn, skill2Btn, skill3Btn, skill4Btn;
    private Map<Button, Geometry> iconGeoms = new HashMap<>();

    // Статистика игрока
    private Container playerStatsContainer;
    private Label playerNameLabel;
    private ProgressBar hpBar;
    private ProgressBar manaBar;
    private Label hpTextLabel;
    private Label manaTextLabel;

    private float lastHealth = -1;
    private float lastMaxHealth = -1;
    private float lastMana = -1;
    private float lastMaxMana = -1;
    private String lastName = "";

    private float hudHeight = 160;
    private float buttonSize = 140;
    private float buttonSpacing = 8;
    private float bottomOffset = 20;

    private PlayerManager playerManager;
    private InventoryManager inventoryManager;
    private TalentWindow talentWindow;

    private float scale = 1f;

    private static final String KEY_SKILL1 = "skill1";
    private static final String KEY_SKILL2 = "skill2";
    private static final String KEY_SKILL3 = "skill3";
    private static final String KEY_SKILL4 = "skill4";
    private static final String KEY_HEALTH_POTION = "healthPotion";
    private static final String KEY_MANA_POTION = "manaPotion";
    private static final String KEY_INVENTORY = "inventory";
    private static final String KEY_TALENTS = "talents";
    private static final String KEY_CHARACTER_STATS = "characterStats";

    private SettingsWindow settingsWindow;

    public UIManager(SimpleApplication app) {
        this.app = app;
        this.guiNode = app.getGuiNode();
        Main main = (Main) app;
        if (main != null) {
            this.networkManager = main.getNetworkManager();
        }
        updateScale();
    }

    private void updateScale() {
        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();
        float baseWidth = 800f;
        float baseHeight = 600f;
        float scaleX = screenWidth / baseWidth;
        float scaleY = screenHeight / baseHeight;
        scale = Math.min(scaleX, scaleY);
        scale = Math.max(0.5f, Math.min(scale, 1.5f));
    }

    public void attachNode(Node node) {
        if (node != null && !guiNode.hasChild(node)) {
            guiNode.attachChild(node);
            System.out.println("[UIManager] ATTACHED: " + node.getName());
        }
    }

    public void detachNode(Node node) {
        if (node != null && guiNode.hasChild(node)) {
            guiNode.detachChild(node);
            System.out.println("[UIManager] DETACHED: " + node.getName());
        }
    }

    public void onInventoryOpened(Node node) { attachNode(node); }
    public void onInventoryClosed(Node node) { detachNode(node); }
    public void onTalentOpened(Node node) { attachNode(node); }
    public void onTalentClosed(Node node) { detachNode(node); }
    public void onTraderOpened(Node node) { attachNode(node); }
    public void onTraderClosed(Node node) { detachNode(node); }

    public Geometry createBackgroundGeometry(float width, float height) {
        Texture leatherTexture = null;
        try {
            leatherTexture = app.getAssetManager().loadTexture("Interface/leather_border.png");
        } catch (Exception e) {
            System.err.println("[UIManager] Текстура не загружена, используем цвет.");
        }
        Quad quad = new Quad(width, height);
        Geometry bgGeom = new Geometry("WindowBg", quad);
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        if (leatherTexture != null) {
            mat.setTexture("ColorMap", leatherTexture);
        } else {
            mat.setColor("Color", new ColorRGBA(0.4f, 0.2f, 0.05f, 1f));
        }
        bgGeom.setMaterial(mat);
        bgGeom.setLocalTranslation(0, 0, 0f);
        return bgGeom;
    }

    private void applyStoredLanguage() {
        String lang = SettingsManager.getInstance().getLanguage();
        LocalizationManager.getInstance().init(app.getAssetManager());
        LocalizationManager.getInstance().loadLanguage(lang);
        try {
            BitmapFont customFont = app.getAssetManager().loadFont("Interface/Fonts/ru.fnt");
            GuiGlobals.getInstance().getStyles().setDefault(customFont);
        } catch (Exception e) {
            System.err.println("Failed to load ru.fnt, using default Lemur font.");
        }
    }

    private void setupSettingsKey() {
        app.getInputManager().addMapping("TOGGLE_SETTINGS", new KeyTrigger(KeyInput.KEY_GRAVE));
        ActionListener listener = (name, isPressed, tpf) -> {
            if (!isPressed) return;
            if (name.equals("TOGGLE_SETTINGS")) {
                toggleSettings();
            }
        };
        app.getInputManager().addListener(listener, "TOGGLE_SETTINGS");
    }

    public void toggleSettings() {
        if (settingsWindow == null) return;

        if (settingsWindow.isVisible()) {
            settingsWindow.hide();
            SoundManager.playSound(SoundManager.SOUND_WINDOW_CLOSE);
        } else {
            closeAllWindowsExcept("settings");
            settingsWindow.show();
            SoundManager.playSound(SoundManager.SOUND_WINDOW_TALENTS);
        }
    }

    // ============================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ============================================================

    public void initialize() {
        applyStoredLanguage();
        settingsWindow = new SettingsWindow(app, this);
        setupSettingsKey();

        GuiGlobals.getInstance().getFocusNavigationState().setEnabled(false);
        GuiGlobals.getInstance().getInputMapper().deactivateGroup(FocusNavigationFunctions.UI_NAV);

        createHUD();
        createPlayerStatsUI();

        hudNode = new Node("HUDNode");
        hudNode.setName("HUDNode");
        hudNode.attachChild(hudBackground);
        for (Button btn : hudButtons) {
            hudNode.attachChild(btn);
        }
        hudNode.attachChild(playerStatsContainer);

        /*
         * ФИКС: talentButton раньше вообще не прикреплялся к сцене
         * (только позиционировался/переключался CullHint, как будто
         * уже был в дереве) — кнопка физически не существовала.
         */
        if (talentButton != null) {
            hudNode.attachChild(talentButton);
        }
        if (inventoryButton != null) {
            hudNode.attachChild(inventoryButton);
        }
        if (teleportButton != null) {
            hudNode.attachChild(teleportButton);
        }
        if (statsButton != null) {
            hudNode.attachChild(statsButton);
        }   
        if (settingsButton != null) {
            hudNode.attachChild(settingsButton);
        }
        hideHUD();

        setupKeyboardShortcuts();

        createBackground();
        showBackground();

        if (backgroundNode != null && !guiNode.hasChild(backgroundNode)) {
            guiNode.attachChild(backgroundNode);
        }

        createGlobalCloseButton();
    }

    // ============================================================
    // ФОН
    // ============================================================

    private void createBackground() {
        if (backgroundNode != null) return;
        backgroundNode = new Node("LoginBackground");
        try {
            Texture tex = app.getAssetManager().loadTexture(backgroundTexturePath);
            float w = app.getCamera().getWidth();
            float h = app.getCamera().getHeight();
            Quad quad = new Quad(w, h);
            backgroundGeom = new Geometry("LoginBg", quad);
            Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setTexture("ColorMap", tex);
            backgroundGeom.setMaterial(mat);
            backgroundGeom.setLocalTranslation(0, 0, -10);
            backgroundNode.attachChild(backgroundGeom);
            backgroundNode.setCullHint(Node.CullHint.Always);
        } catch (Exception e) {
            System.err.println("[UIManager] Failed to load background, using fallback");
            createFallbackBackground();
        }
    }

    private void createFallbackBackground() {
        float w = app.getCamera().getWidth();
        float h = app.getCamera().getHeight();
        Quad quad = new Quad(w, h);
        backgroundGeom = new Geometry("LoginBg", quad);
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0.15f, 0.15f, 0.25f, 1f));
        backgroundGeom.setMaterial(mat);
        backgroundGeom.setLocalTranslation(0, 0, -10);
        backgroundNode.attachChild(backgroundGeom);
        backgroundNode.setCullHint(Node.CullHint.Always);
    }

    private void updateBackgroundScale() {
        if (backgroundGeom == null) return;
        float w = app.getCamera().getWidth();
        float h = app.getCamera().getHeight();
        Quad q = (Quad) backgroundGeom.getMesh();
        q.updateGeometry(w, h);
        backgroundGeom.setLocalTranslation(0, 0, -10);
    }

    private void showBackground() {
        if (backgroundNode != null) {
            backgroundNode.setCullHint(Node.CullHint.Never);
            updateBackgroundScale();
        }
    }

    private void hideBackground() {
        if (backgroundNode != null) {
            backgroundNode.setCullHint(Node.CullHint.Always);
        }
    }

    // ============================================================
    // setupKeyboardShortcuts
    // ============================================================

    private void setupKeyboardShortcuts() {
        app.getInputManager().addMapping(KEY_CHARACTER_STATS, new KeyTrigger(KeyInput.KEY_C));
        app.getInputManager().addMapping(KEY_SKILL1, new KeyTrigger(KeyInput.KEY_1));
        app.getInputManager().addMapping(KEY_SKILL2, new KeyTrigger(KeyInput.KEY_2));
        app.getInputManager().addMapping(KEY_SKILL3, new KeyTrigger(KeyInput.KEY_3));
        app.getInputManager().addMapping(KEY_SKILL4, new KeyTrigger(KeyInput.KEY_4));
        app.getInputManager().addMapping(KEY_HEALTH_POTION, new KeyTrigger(KeyInput.KEY_Q));
        app.getInputManager().addMapping(KEY_MANA_POTION, new KeyTrigger(KeyInput.KEY_W));
        app.getInputManager().addMapping(KEY_INVENTORY, new KeyTrigger(KeyInput.KEY_I));
        app.getInputManager().addMapping(KEY_TALENTS, new KeyTrigger(KeyInput.KEY_T));

        ActionListener listener = (name, isPressed, tpf) -> {
            if (!isPressed) return;
            if (!hudVisible) return;

            switch (name) {
                case KEY_CHARACTER_STATS: toggleCharacterStats(); break;
                case KEY_SKILL1:
                    if (playerManager != null) playerManager.castSkill("Heal");
                    flashButton(skill1Btn);
                    break;
                case KEY_SKILL2:
                    if (playerManager != null) playerManager.castSkill("ShieldBash");
                    flashButton(skill2Btn);
                    break;
                case KEY_SKILL3:
                    if (playerManager != null) playerManager.castSkill("Whirlwind");
                    flashButton(skill3Btn);
                    break;
                case KEY_SKILL4:
                    if (playerManager != null) playerManager.castSkill("Kick");
                    flashButton(skill4Btn);
                    break;
                case KEY_HEALTH_POTION:
                    if (playerManager != null) {
                        playerManager.useHealthPotion();
                        updatePotionCounts();
                        updatePlayerStats();
                        flashButton(healthPotionBtn);
                    }
                    break;
                case KEY_MANA_POTION:
                    if (playerManager != null) {
                        playerManager.useManaPotion();
                        updatePotionCounts();
                        updatePlayerStats();
                        flashButton(manaPotionBtn);
                    }
                    break;
                case KEY_INVENTORY: toggleInventory(); break;
                case KEY_TALENTS:
                    toggleTalents();
                    flashButton(talentButton);
                    break;
            }
        };

        app.getInputManager().addListener(listener,
                KEY_SKILL1, KEY_SKILL2, KEY_SKILL3, KEY_SKILL4,
                KEY_HEALTH_POTION, KEY_MANA_POTION, KEY_INVENTORY,
                KEY_TALENTS, KEY_CHARACTER_STATS);
    }

    private void flashButton(Button btn) {
        if (btn == null) return;
        Geometry iconGeom = iconGeoms.get(btn);
        if (iconGeom == null) return;
        Material mat = iconGeom.getMaterial();
        if (mat == null) return;
        MatParam colorParam = mat.getParam("Color");
        ColorRGBA originalColor = colorParam != null ? (ColorRGBA) colorParam.getValue() : ColorRGBA.White;
        final ColorRGBA finalOriginalColor = originalColor.clone();
        mat.setColor("Color", new ColorRGBA(1f, 1f, 0f, 1f));
        new Thread(() -> {
            try { Thread.sleep(150); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            app.enqueue(() -> mat.setColor("Color", finalOriginalColor));
        }).start();
    }

    // ============================================================
    // HUD
    // ============================================================

    private Button createIconOnlyButton(String iconPath, float size) {
        Button btn = new Button("");
        btn.setPreferredSize(new Vector3f(size, size, 0));

        /*
         * ВАЖНО: НЕ ставим background в null. Именно по background
         * Lemur считает область клика (hit-test). Раз фона нет,
         * а текст кнопки пустой — область клика схлопывается почти
         * до нуля (по пустому лейблу), в то время как иконка
         * (отдельная Geometry поверх) рисуется полноразмерной,
         * но в системе кликов не участвует вообще.
         *
         * Вместо null — полностью прозрачный фон того же размера,
         * что и иконка: невидим на экране, но даёт Lemur реальную
         * геометрию для расчёта области нажатия.
         */
        QuadBackgroundComponent transparentBg =
                new QuadBackgroundComponent(new ColorRGBA(0f, 0f, 0f, 0f));
        btn.setBackground(transparentBg);

        btn.setColor(ColorRGBA.White);
        try {
            Texture tex = app.getAssetManager().loadTexture(iconPath);
            if (tex != null) {
                Geometry iconGeom = new Geometry("Icon", new Quad(size, size));
                Material iconMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
                iconMat.setTexture("ColorMap", tex);
                iconMat.setColor("Color", ColorRGBA.White);
                iconGeom.setMaterial(iconMat);
                iconGeom.setLocalTranslation(0, 0, 0.1f);
                btn.attachChild(iconGeom);
                iconGeoms.put(btn, iconGeom);
            }
        } catch (Exception e) {
            System.err.println("[UIManager] Failed to load icon: " + iconPath);
            btn.setText("?");
        }
        btn.setCullHint(Node.CullHint.Always);
        return btn;
    }

    private void createHUD() {
        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();
        float hudHeightScaled = hudHeight * scale;
        float buttonSizeScaled = buttonSize * scale;

        Quad quad = new Quad(screenWidth, hudHeightScaled);
        hudBackground = new Geometry("HUDBackground", quad);

        /*
         * Полупрозрачная полоса HUD занимает всю ширину экрана внизу.
         * Без этой пометки Main.isOverGui() (рейкаст по guiNode)
         * считал ЛЮБОЙ тап в нижней части экрана "попаданием в
         * интерфейс" — включая клики по предметам на земле рядом
         * с персонажем, которые часто проецируются как раз в эту
         * полосу. Это чисто декоративный фон, не должен блокировать
         * клики по игровому миру.
         */
        hudBackground.setUserData("pickable", false);
        hudBackground.setName("HUDBackground");
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0f, 0f, 0f, 0.5f));
        hudBackground.setMaterial(mat);

        healthPotionBtn = createIconOnlyButton("Interface/Icons/hp.png", buttonSizeScaled);
        healthPotionBtn.addClickCommands((source) -> {
            if (playerManager != null) playerManager.useHealthPotion();
            updatePotionCounts();
            updatePlayerStats();
            flashButton(healthPotionBtn);
        });
        hudButtons.add(healthPotionBtn);

        hpCountLabel = new Label("0");
        hpCountLabel.setFontSize(14 * scale);
        hpCountLabel.setColor(ColorRGBA.White);
        hpCountLabel.setLocalTranslation(buttonSizeScaled - 25 * scale, 10 * scale, 0.1f);
        healthPotionBtn.attachChild(hpCountLabel);

        manaPotionBtn = createIconOnlyButton("Interface/Icons/mp.png", buttonSizeScaled);
        manaPotionBtn.addClickCommands((source) -> {
            if (playerManager != null) playerManager.useManaPotion();
            updatePotionCounts();
            updatePlayerStats();
            flashButton(manaPotionBtn);
        });
        hudButtons.add(manaPotionBtn);

        mpCountLabel = new Label("0");
        mpCountLabel.setFontSize(14 * scale);
        mpCountLabel.setColor(ColorRGBA.White);
        mpCountLabel.setLocalTranslation(buttonSizeScaled - 25 * scale, 10 * scale, 0.1f);
        manaPotionBtn.attachChild(mpCountLabel);

        skill1Btn = createIconOnlyButton("Interface/Icons/light.png", buttonSizeScaled);
        skill1Btn.addClickCommands((source) -> {
            if (playerManager != null) playerManager.castSkill("Heal");
        });
        hudButtons.add(skill1Btn);

        skill2Btn = createIconOnlyButton("Interface/Icons/shield.png", buttonSizeScaled);
        skill2Btn.addClickCommands((source) -> {
            if (playerManager != null) playerManager.castSkill("ShieldBash");
        });
        hudButtons.add(skill2Btn);

        skill3Btn = createIconOnlyButton("Interface/Icons/whirlwind.png", buttonSizeScaled);
        skill3Btn.addClickCommands((source) -> {
            if (playerManager != null) playerManager.castSkill("Whirlwind");
        });
        hudButtons.add(skill3Btn);

        skill4Btn = createIconOnlyButton("Interface/Icons/kick.png", buttonSizeScaled);
        skill4Btn.addClickCommands((source) -> {
            if (playerManager != null) playerManager.castSkill("Kick");
        });
        hudButtons.add(skill4Btn);

        // ============================================================
        // Кнопка талантов — теперь с иконкой, как остальные HUD-кнопки.
        // (Иконка: Interface/Icons/talent.png)
        // ============================================================

        talentButton = createIconOnlyButton(
                "Interface/Icons/talent.png",
                buttonSizeScaled
        );
        talentButton.addClickCommands((source) -> {
            SoundManager.playSound(SoundManager.SOUND_CLICK);
            toggleTalents();
        });

        // ============================================================
        // Кнопка «Телепорт из данжа» — иконка Interface/Icons/teleport.png
        // ============================================================

        teleportButton = createIconOnlyButton(
                "Interface/Icons/teleport.png",
                buttonSizeScaled
        );
        teleportButton.addClickCommands((source) -> {
            SoundManager.playSound(SoundManager.SOUND_CLICK);
            toggleTeleport();
        });

        // ============================================================
        // Кнопка «Характеристики» — иконка Interface/Icons/stats.png
        // ============================================================

        statsButton = createIconOnlyButton(
                "Interface/Icons/stats.png",
                buttonSizeScaled
        );
        statsButton.addClickCommands((source) -> {
            SoundManager.playSound(SoundManager.SOUND_CLICK);
            toggleCharacterStats();
        });
        
        settingsButton = createIconOnlyButton(
                "Interface/Icons/settings.png",
                buttonSizeScaled
        );
        settingsButton.addClickCommands((source) -> {
            SoundManager.playSound(SoundManager.SOUND_CLICK);
            toggleSettings();
        });
        /*
         * На Android нет клавиатуры (клавиша T недоступна) — кнопка
         * ОБЯЗАНА быть кликабельной сама по себе. Раньше она даже
         * не прикреплялась к сцене — см. фикс ниже в блоке создания
         * hudNode.
         */

        // Кнопка инвентаря — раньше открывалась ТОЛЬКО клавишей "I",
        // на тач-экране без физической клавиатуры это делало инвентарь
        // недоступным вообще. Переиспользуем уже исправленный
        // createIconOnlyButton — у него сразу корректная область клика.
        inventoryButton = createIconOnlyButton("Interface/Icons/inventory.png", buttonSizeScaled);
        inventoryButton.addClickCommands((source) -> {
            SoundManager.playSound(SoundManager.SOUND_CLICK);
            toggleInventory();
        });
    }

    // ============================================================
    // PLAYER STATS
    // ============================================================

    private void createPlayerStatsUI() {
        float screenHeight = app.getCamera().getHeight();

        playerStatsContainer = new Container();
        playerStatsContainer.setName("PlayerStatsContainer");
        playerStatsContainer.setLayout(new SpringGridLayout(Axis.Y, Axis.X));
        playerStatsContainer.setPreferredSize(new Vector3f(200 * scale, 80 * scale, 0));
        playerStatsContainer.setBackground(new QuadBackgroundComponent(new ColorRGBA(0f, 0f, 0f, 0.5f)));
        playerStatsContainer.setLocalTranslation(10 * scale, screenHeight - 110 * scale, 0);
        playerStatsContainer.setCullHint(Node.CullHint.Always);

        playerNameLabel = new Label("Player");
        playerNameLabel.setFontSize(18 * scale);
        playerNameLabel.setColor(ColorRGBA.White);
        playerStatsContainer.addChild(playerNameLabel);

        hpBar = new ProgressBar();
        hpBar.setPreferredSize(new Vector3f(180 * scale, 18 * scale, 0));
        hpBar.setProgressPercent(1.0f);
        hpBar.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.2f, 0f, 0f, 0.8f)));
        Panel hpIndicator = hpBar.getValueIndicator();
        if (hpIndicator != null) hpIndicator.setBackground(new QuadBackgroundComponent(ColorRGBA.Green));
        playerStatsContainer.addChild(hpBar);

        hpTextLabel = new Label("100/100");
        hpTextLabel.setFontSize(12 * scale);
        hpTextLabel.setColor(ColorRGBA.White);
        playerStatsContainer.addChild(hpTextLabel);

        manaBar = new ProgressBar();
        manaBar.setPreferredSize(new Vector3f(180 * scale, 18 * scale, 0));
        manaBar.setProgressPercent(1.0f);
        manaBar.setBackground(new QuadBackgroundComponent(new ColorRGBA(0f, 0f, 0.2f, 0.8f)));
        Panel manaIndicator = manaBar.getValueIndicator();
        if (manaIndicator != null) manaIndicator.setBackground(new QuadBackgroundComponent(ColorRGBA.Blue));
        playerStatsContainer.addChild(manaBar);

        manaTextLabel = new Label("50/50");
        manaTextLabel.setFontSize(12 * scale);
        manaTextLabel.setColor(ColorRGBA.White);
        playerStatsContainer.addChild(manaTextLabel);

        lastHealth = -1; lastMaxHealth = -1;
        lastMana = -1; lastMaxMana = -1;
        lastName = "";
    }

    public void updatePlayerStats() {
        if (playerManager == null || playerStatsContainer == null) return;
        float hp = playerManager.getHealth();
        float maxHp = playerManager.getMaxHealth();
        float mana = playerManager.getMana();
        float maxMana = playerManager.getMaxMana();
        String name = playerManager.getPlayerName();

        if (hp != lastHealth || maxHp != lastMaxHealth) {
            if (maxHp > 0) hpBar.setProgressPercent(hp / maxHp);
            hpTextLabel.setText((int) hp + " / " + (int) maxHp);
            lastHealth = hp; lastMaxHealth = maxHp;
        }
        if (mana != lastMana || maxMana != lastMaxMana) {
            if (maxMana > 0) manaBar.setProgressPercent(mana / maxMana);
            manaTextLabel.setText((int) mana + " / " + (int) maxMana);
            lastMana = mana; lastMaxMana = maxMana;
        }
        if (!name.equals(lastName)) {
            playerNameLabel.setText(name);
            lastName = name;
        }
    }

    public void updatePotionCounts() {
        if (playerManager == null) return;
        if (hpCountLabel != null) hpCountLabel.setText(String.valueOf(playerManager.getHealthPotions()));
        if (mpCountLabel != null) mpCountLabel.setText(String.valueOf(playerManager.getManaPotions()));
    }

    public void updateHUDPosition(boolean force) {
        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();

        if (hudBackground != null) {
            hudBackground.setLocalTranslation(0, bottomOffset * scale, 0);
            Quad quad = (Quad) ((Geometry) hudBackground).getMesh();
            quad.updateGeometry(screenWidth, hudHeight * scale);
        }

        float yPos = bottomOffset * scale + 10 * scale;
        float xPos = 20 * scale;
        float buttonSizeScaled = buttonSize * scale;
        float spacingScaled = buttonSpacing * scale;

        for (int i = 0; i < 2 && i < hudButtons.size(); i++) {
            hudButtons.get(i).setLocalTranslation(xPos, yPos, 0);
            xPos += buttonSizeScaled + spacingScaled;
        }
        xPos += buttonSizeScaled * 0.5f;
        for (int i = 2; i < 6 && i < hudButtons.size(); i++) {
            hudButtons.get(i).setLocalTranslation(xPos, yPos, 0);
            xPos += buttonSizeScaled + spacingScaled;
        }
        xPos += buttonSizeScaled * 0.5f;
        if (hudButtons.size() > 6) {
            hudButtons.get(6).setLocalTranslation(xPos, yPos, 0);
        }
        /*
         * MapWindow рисует миникарту в верхнем правом углу, занимая
         * область [screenWidth-420, screenWidth] x [screenHeight-420,
         * screenHeight] В СЫРЫХ пикселях (без учёта scale — MapWindow
         * его не использует). Раньше кнопки талантов/инвентаря
         * садились прямо в эту область — опускаем их чуть ниже карты.
         */
        float mapReservedHeight = 420f; // 400 (mapSize) + 20 (отступ) — держать в синхроне с MapWindow

                // ============================================================
        // Правая колонка кнопок:
        //   [inventory] [talent]
        //               [teleport]
        //               [stats]
        //
        // Столбец talent/teleport/stats — вертикально, как просили.
        // inventory остаётся слева от talent, как было.
        // ============================================================

        float rightX = screenWidth - buttonSizeScaled - 10 * scale;
        float topY   = screenHeight - mapReservedHeight - buttonSizeScaled - 10 * scale;
        float step   = buttonSizeScaled + 10 * scale;

        if (talentButton != null) {
            talentButton.setLocalTranslation(rightX, topY, 0);
        }
        if (inventoryButton != null) {
            inventoryButton.setLocalTranslation(rightX - step, topY, 0);
        }
        if (teleportButton != null) {
            teleportButton.setLocalTranslation(rightX, topY - step, 0);
        }
        if (statsButton != null) {
            statsButton.setLocalTranslation(rightX, topY - step * 2, 0);
        }
        if (settingsButton != null) {
            settingsButton.setLocalTranslation(rightX, topY - step * 3, 0);
        }
        if (playerStatsContainer != null) {
            playerStatsContainer.setLocalTranslation(10 * scale, screenHeight - 110 * scale, 0);
        }
    }

    public void showHUD() {
        if (hudNode == null) return;
        attachNode(hudNode);
        hudVisible = true;
        for (Button btn : hudButtons) btn.setCullHint(Node.CullHint.Dynamic);
        if (hudBackground != null) hudBackground.setCullHint(Node.CullHint.Dynamic);
        if (talentButton != null) talentButton.setCullHint(Node.CullHint.Dynamic);
        if (inventoryButton != null) inventoryButton.setCullHint(Node.CullHint.Dynamic);
        if (settingsButton != null) settingsButton.setCullHint(Node.CullHint.Dynamic);

        if (teleportButton != null) teleportButton.setCullHint(Node.CullHint.Dynamic);
        if (statsButton != null) statsButton.setCullHint(Node.CullHint.Dynamic);
        if (playerStatsContainer != null) playerStatsContainer.setCullHint(Node.CullHint.Dynamic);
        updateHUDPosition(true);
        updatePotionCounts();
        updatePlayerStats();
        updatePlayerName();
    }

    public void hideHUD() {
        detachNode(hudNode);
        hudVisible = false;
        for (Button btn : hudButtons) btn.setCullHint(Node.CullHint.Always);
        if (hudBackground != null) hudBackground.setCullHint(Node.CullHint.Always);
        if (talentButton != null) talentButton.setCullHint(Node.CullHint.Always);
        if (inventoryButton != null) inventoryButton.setCullHint(Node.CullHint.Always);
        if (teleportButton != null) teleportButton.setCullHint(Node.CullHint.Always);
        if (statsButton != null) statsButton.setCullHint(Node.CullHint.Always);
        if (settingsButton != null) settingsButton.setCullHint(Node.CullHint.Always);

        if (playerStatsContainer != null) playerStatsContainer.setCullHint(Node.CullHint.Always);
    }
public void toggleTeleport() {

        Main main = (Main) app;
        if (main == null) return;

        WorldManager wm = main.getWorldManager();
        if (wm == null) return;

        String dungeon = (playerManager != null)
                ? playerManager.getCurrentDungeon()
                : null;

        boolean inDungeon = (dungeon != null && !dungeon.isEmpty());

        if (inDungeon) {
            wm.returnToCity();          // ✅ правильный метод
        } else {
            showTeleporterDialog();
        }
    }

    // ============================================================
    // TELEPORTER
    // ============================================================

    private Container teleporterDialog;
    private boolean teleporterDialogVisible = false;

    public void showTeleporterDialog() {
        if (teleporterDialog == null) createTeleporterDialog();
        if (teleporterDialogVisible) return;
        teleporterDialog.setCullHint(Node.CullHint.Never);
        teleporterDialogVisible = true;
        attachNode(teleporterDialog);
    }

    public void hideTeleporterDialog() {
        if (teleporterDialog != null && teleporterDialogVisible) {
            teleporterDialog.setCullHint(Node.CullHint.Always);
            detachNode(teleporterDialog);
            teleporterDialogVisible = false;
        }
    }

    private void createTeleporterDialog() {

        updateScale();

        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();

        // Окно расширено под увеличенные шрифты.
        float winW = 520 * scale;
        float winH = 260 * scale;

        teleporterDialog = new Container();
        teleporterDialog.setPreferredSize(new Vector3f(winW, winH, 0));
        teleporterDialog.setLayout(null);
        teleporterDialog.setName("TeleporterDialog");

        float x = (screenWidth - winW) / 2;
        float y = (screenHeight - winH) / 2;
        if (y < 0) y = 0;
        teleporterDialog.setLocalTranslation(x, y, 0);

        Geometry bgGeom = createBackgroundGeometry(winW, winH);
        teleporterDialog.attachChild(bgGeom);

        // ============================================================
        // Вопрос — шрифт ×2 (было 22 * scale, стало 44 * scale).
        // Позиция по X пересчитана: строка стала длиннее.
        // ============================================================

        Label question = new Label(getLocalized("teleporter.question"));
        question.setFontSize(44 * scale);
        question.setColor(ColorRGBA.White);
        question.setLocalTranslation(
                winW / 2 - 200 * scale,
                winH - 60 * scale,
                0.1f);
        teleporterDialog.attachChild(question);

        // ============================================================
        // Кнопка "Да" — шрифт ×2, размер под крупный текст,
        // DOWN-обработчик вместо addClickCommands.
        // ============================================================

        Button yesButton = new Button(getLocalized("teleporter.yes"));
        yesButton.setPreferredSize(new Vector3f(160 * scale, 60 * scale, 0));
        yesButton.setFontSize(36 * scale);
        yesButton.setColor(ColorRGBA.White);
        yesButton.setLocalTranslation(
                50 * scale,
                40 * scale,
                0.1f);
        bindTouchAction(yesButton, () -> {
            SoundManager.playSound(SoundManager.SOUND_CLICK);
            hideTeleporterDialog();
            Main main = (Main) app;
            if (main != null) {
                WorldManager wm = main.getWorldManager();
                if (wm != null) wm.teleportToDungeon();
            }
        });
        teleporterDialog.attachChild(yesButton);

        // ============================================================
        // Кнопка "Нет" — шрифт ×2, размер под крупный текст,
        // DOWN-обработчик.
        // ============================================================

        Button noButton = new Button(getLocalized("teleporter.no"));
        noButton.setPreferredSize(new Vector3f(160 * scale, 60 * scale, 0));
        noButton.setFontSize(36 * scale);
        noButton.setColor(ColorRGBA.White);
        noButton.setLocalTranslation(
                winW - 210 * scale,
                40 * scale,
                0.1f);
        bindTouchAction(noButton, () -> hideTeleporterDialog());
        teleporterDialog.attachChild(noButton);

        teleporterDialog.setCullHint(Node.CullHint.Never);
        guiNode.attachChild(teleporterDialog);
    }
    // ============================================================
    // MANAGERS
    // ============================================================

    public void setPlayerManager(PlayerManager pm) {
        this.playerManager = pm;
        updatePlayerStats();
        updatePlayerName();
    }

    public void setInventoryManager(InventoryManager im) {
        this.inventoryManager = im;
        if (im != null) im.setUIManager(this);
    }

    public void setTalentManager(TalentManager tm) {
        this.talentManager = tm;
        if (tm != null) createWindows(true);
    }

    private void createWindows(boolean force) {
        if (talentManager == null) {
            System.err.println("[UIManager] Cannot create windows: talentManager is null");
            return;
        }
        if (force || talentWindow == null) {
            talentWindow = new TalentWindow(app, talentManager, this);
        }
        if (force || traderWindow == null) {
            traderWindow = new TraderWindow(app, playerManager, inventoryManager, this);
        }
        if (force || auctionWindow == null) {
            auctionWindow = new AuctionWindow(app, this, inventoryManager, playerManager);
        }
    }

    // ============================================================
    // GAME STATE
    // ============================================================

    public void onStateChanged(GameState newState) {
        if (newState == GameState.CITY || newState == GameState.DUNGEON) {
            showHUD();
            updatePlayerStats();
            updatePotionCounts();
            hideBackground();
            //showControlsHelpIfNeeded();
        } else {
            hideHUD();
        }
    }
        private Button settingsButton;   // <-- НОВОЕ

    /**
     * Привязывает действие к кнопке через DOWN-событие.
     *
     * addClickCommands срабатывает по UP, которое на Android при
     * тапе нередко теряется — кнопка мигает, но действие не
     * выполняется. Тот же приём используется в InventoryManager,
     * TraderWindow, TalentWindow, AuctionWindow, BlacksmithWindow,
     * VirtualKeyboard.
     */
    private void bindTouchAction(Button button, final Runnable action) {

        com.simsilica.lemur.event.MouseEventControl.addListenersToSpatial(
                button,
                new com.simsilica.lemur.event.MouseListener() {

                    @Override
                    public void mouseButtonEvent(
                            com.jme3.input.event.MouseButtonEvent evt,
                            Spatial s,
                            Spatial t) {
                        if (evt.getButtonIndex() != 0) return;
                        if (!evt.isPressed()) return;
                        action.run();
                    }

                    @Override public void mouseEntered(
                            com.jme3.input.event.MouseMotionEvent evt,
                            Spatial s, Spatial t) {}
                    @Override public void mouseExited(
                            com.jme3.input.event.MouseMotionEvent evt,
                            Spatial s, Spatial t) {}
                    @Override public void mouseMoved(
                            com.jme3.input.event.MouseMotionEvent evt,
                            Spatial s, Spatial t) {}
                }
        );
    }

    public PlayerManager getPlayerManager() { return playerManager; }

    public boolean isAnyWindowOpen() {
        return teleporterDialogVisible
                || (inventoryManager != null && inventoryManager.isVisible())
                || (talentWindow != null && talentWindow.isVisible())
                || (traderWindow != null && traderWindow.isVisible())
                || (auctionWindow != null && auctionWindow.isVisible())
                || (blacksmithWindow != null && blacksmithWindow.isVisible());
    }

    public void update(float tpf) {
        if (hudVisible) {
            updateHUDPosition(false);
            updatePlayerStats();
        }
        if (talentWindow != null) talentWindow.update(tpf);
        if (settingsWindow != null && settingsWindow.isVisible()) settingsWindow.update();
    }

    public void onResize(int width, int height) {
        updateScale();
        if (hudVisible) {
            updateHUDPosition(true);
            if (playerStatsContainer != null) {
                playerStatsContainer.setLocalTranslation(10 * scale, app.getCamera().getHeight() - 110 * scale, 0);
            }
        }
        if (inventoryManager != null) inventoryManager.updateLayout(width, height);
        if (talentWindow != null) talentWindow.updateLayout(width, height);
        if (traderWindow != null) traderWindow.updateLayout(width, height);
        if (auctionWindow != null) auctionWindow.updateLayout(width, height);
        if (backgroundNode != null && backgroundNode.getCullHint() == Node.CullHint.Never) {
            updateBackgroundScale();
        }
    }

    public void cleanup() {
        detachNode(hudNode);
        if (inventoryManager != null) inventoryManager.cleanup();
        if (talentWindow != null) talentWindow.hide();
        if (traderWindow != null) traderWindow.hide();
        if (auctionWindow != null) auctionWindow.hide();
        if (mapRenderer != null) mapRenderer.cleanup();
        if (mapWindow != null) mapWindow.cleanup();
        if (blacksmithWindow != null) blacksmithWindow.cleanup();
    }

    public Node getGuiNode() { return guiNode; }

    // ============================================================
    // VK PLAY / NETWORK
    // ============================================================

    /**
     * Тестовый режим (без VK Play) — сразу грузим тестового персонажа.
     */
    public void forceShowLogin() {
        loadTestCharacter();
        Main main = (Main) app;
        if (main != null) {
            main.loadGameWorld();
        }
    }

    /**
     * Полностью автоматический вход — без VK Play, без логина/пароля.
     * Использует уникальный ID устройства (генерируется и сохраняется
     * локально при первом запуске). Если пользователя с таким ID
     * ещё нет на сервере — он будет создан автоматически.
     */
    public void autoLoginDevice() {
        hideHUD();
        showBackground();

        if (networkManager == null) {
            System.err.println("[UI] NetworkManager не инициализирован, автологин невозможен.");
            forceShowLogin();
            return;
        }

        String deviceId = networkManager.getOrCreateDeviceId();

        if (deviceId == null) {
            System.err.println("[UI] Не удалось получить/создать deviceId.");
            forceShowLogin();
            return;
        }

        System.out.println("[UI] Автологин по устройству, deviceId=" + deviceId);

        networkManager.deviceLogin(deviceId)
                .thenAccept(success -> {
                    app.enqueue(() -> {
                        if (success) {
                            System.out.println("[UI] Автологин выполнен успешно.");
                            loadCharacterFromServer();
                        } else {
                            System.err.println("[UI] Автологин не удался, локальный тестовый персонаж.");
                            forceShowLogin();
                        }
                    });
                })
                .exceptionally(ex -> {
                    app.enqueue(() -> {
                        System.err.println("[UI] Network error: " + ex.getMessage());
                        forceShowLogin();
                    });
                    return null;
                });
    }
public InventoryManager getInventoryManager() {
    return inventoryManager;
}
    public void autoLoginVkPlay(String persId) {
        hideHUD();
        showBackground();

        if (networkManager == null) {
            System.err.println("[UI] NetworkManager не инициализирован, VK Play автологин невозможен.");
            forceShowLogin();
            return;
        }

        System.out.println("[UI] VK Play автологин, persId=" + persId);

        networkManager.vkPlayLogin(persId, VkPlayLaunchArgs.getToken())
                .thenAccept(success -> {
                    app.enqueue(() -> {
                        if (success) {
                            System.out.println("[UI] VK Play вход выполнен успешно.");
                            loadCharacterFromServer();
                        } else {
                            System.err.println("[UI] VK Play вход не удался.");
                            forceShowLogin();
                        }
                    });
                })
                .exceptionally(ex -> {
                    app.enqueue(() -> {
                        System.err.println("[UI] VK Play network error: " + ex.getMessage());
                        forceShowLogin();
                    });
                    return null;
                });
    }

private void loadCharacterFromServer() {
    if (networkManager == null) return;
    networkManager.loadCharacterData()
            .thenAccept(data -> {
                app.enqueue(() -> {
                    try {
                        if (data != null) {
                            System.out.println("[UI] Данные персонажа загружены с сервера. keys=" + data.keySet());
                            applyCharacterData(data);
                            Main main = (Main) app;
                            if (main != null) {
                                main.loadGameWorld();
                                main.getGameManager().setState(GameState.CITY);
                            }
                        } else {
                            System.err.println("[UI] loadCharacterFromServer: data == null!");
                            forceShowLogin();
                        }
                    } catch (Throwable t) {
                        System.err.println("[UI] applyCharacterData FAILED: " + t);
                        t.printStackTrace();
                        forceShowLogin();
                    }
                    return null;
                });
            })
            .exceptionally(ex -> {
                app.enqueue(() -> {
                    System.err.println("[UI] Network error: " + ex.getMessage());
                    ex.printStackTrace();
                    forceShowLogin();
                });
                return null;
            });
}
    @SuppressWarnings("unchecked")
    public void applyCharacterData(Map<String, Object> data) {
        if (playerManager == null) return;
        System.out.println("[UI] applyCharacterData: data keys = " + data.keySet());

        if (data.containsKey("name")) playerManager.setPlayerName((String) data.get("name"));
        if (data.containsKey("gold")) playerManager.setGold(((Number) data.get("gold")).intValue());
        if (data.containsKey("healthPotions")) playerManager.setHealthPotions(((Number) data.get("healthPotions")).intValue());
        if (data.containsKey("manaPotions")) playerManager.setManaPotions(((Number) data.get("manaPotions")).intValue());
        if (data.containsKey("health")) playerManager.setHealth(((Number) data.get("health")).intValue());
        if (data.containsKey("maxHealth")) playerManager.setMaxHealth(((Number) data.get("maxHealth")).intValue());
        if (data.containsKey("mana")) playerManager.setMana(((Number) data.get("mana")).intValue());
        if (data.containsKey("maxMana")) playerManager.setMaxMana(((Number) data.get("maxMana")).intValue());
        if (data.containsKey("level")) playerManager.setLevel(((Number) data.get("level")).intValue());
        if (data.containsKey("currentDungeon")) playerManager.setCurrentDungeon((String) data.get("currentDungeon"));
        if (data.containsKey("difficulty")) playerManager.setCurrentDifficulty(((Number) data.get("difficulty")).intValue());

        updatePlayerName();

        if (data.containsKey("experience")) playerManager.setExperience(((Number) data.get("experience")).intValue());
        if (data.containsKey("inventory")) {
            List<Map<String, Object>> invList = (List<Map<String, Object>>) data.get("inventory");
            if (inventoryManager != null) inventoryManager.loadFromServerData(invList);
        }
        updatePlayerStats();
        updatePotionCounts();
    }

    // ============================================================
    // TOAST
    // ============================================================

    public void showToast(String message) {
        Label toast = new Label(message);
        toast.setFontSize(18 * scale);
        toast.setColor(ColorRGBA.Green);
        toast.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.1f, 0.1f, 0.1f, 0.9f)));
        toast.setPreferredSize(new Vector3f(400 * scale, 40 * scale, 0));
        toast.setLocalTranslation(
                (app.getCamera().getWidth() - 400 * scale) / 2,
                app.getCamera().getHeight() - 100 * scale,
                0.1f);
        guiNode.attachChild(toast);
        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            app.enqueue(() -> guiNode.detachChild(toast));
        }).start();
    }

    // ============================================================
    // TEST CHARACTER
    // ============================================================

    private void loadTestCharacter() {
        Main main = (Main) app;
        if (main == null) return;
        PlayerManager pm = main.getPlayerManager();
        if (pm == null) return;

        pm.setPlayerName("Test Player");
        pm.setLevel(1);
        pm.setMaxHealth(100);
        pm.setHealth(100);
        pm.setMaxMana(50);
        pm.setMana(50);
        pm.setExperience(0);
        pm.setGold(100);
        pm.setHealthPotions(3);
        pm.setManaPotions(3);

        InventoryManager im = main.getInventoryManager();
        if (im != null) {
            im.addItem(ItemGenerator.generateItem(1, "Weapon", 1));
            im.addItem(ItemGenerator.generateItem(1, "Helmet", 1));
            im.addItem(ItemGenerator.generateItem(1, "Chest", 1));
            im.addItem(ItemGenerator.generateItem(1, "Legs", 1));
            im.addItem(ItemGenerator.generateItem(1, "Boots", 1));
        }
        updatePlayerStats();
        updatePotionCounts();
    }

public void updatePlayerName() {
    if (playerManager == null || playerNameLabel == null) return;
    String name = playerManager.getPlayerName();
    int level = playerManager.getLevel();
    System.out.println("[UI] updatePlayerName: name=" + name + " level=" + level);
    playerNameLabel.setText(name + " [" + level + "]");
}

    // ============================================================
    // LOADING SCREEN
    // ============================================================

    private Node loadingScreenNode;
    private Geometry loadingImage;
    private List<String> loadingImages = Arrays.asList(
            "Interface/hud/1.png",
            "Interface/hud/2.png",
            "Interface/hud/3.png",
            "Interface/hud/4.png",
            "Interface/hud/5.png",
            "Interface/hud/6.png"
    );
    private Random random = new Random();

    public void showLoadingScreen() {
        if (loadingScreenNode == null) {
            loadingScreenNode = new Node("LoadingScreen");
            float w = app.getCamera().getWidth();
            float h = app.getCamera().getHeight();
            if (w <= 0) w = 1280;
            if (h <= 0) h = 720;
            Quad quad = new Quad(w, h);
            loadingImage = new Geometry("LoadingImage", quad);
            loadingImage.setLocalTranslation(0, 0, 0);
            loadingScreenNode.attachChild(loadingImage);
            loadingScreenNode.setLocalTranslation(0, 0, 100);
        } else {
            updateLoadingScreenSize();
        }

        String path = loadingImages.get(random.nextInt(loadingImages.size()));
        try {
            Texture tex = app.getAssetManager().loadTexture(path);
            Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setTexture("ColorMap", tex);
            loadingImage.setMaterial(mat);
        } catch (Exception e) {
            Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", new ColorRGBA(0.2f, 0.2f, 0.3f, 1f));
            loadingImage.setMaterial(mat);
        }

        if (!guiNode.hasChild(loadingScreenNode)) {
            guiNode.attachChild(loadingScreenNode);
        }
    }

    public void hideLoadingScreen() {
        if (loadingScreenNode != null && guiNode.hasChild(loadingScreenNode)) {
            guiNode.detachChild(loadingScreenNode);
        }
    }

    private void updateLoadingScreenSize() {
        if (loadingImage == null) return;
        float w = app.getCamera().getWidth();
        float h = app.getCamera().getHeight();
        if (w <= 0) w = 1280;
        if (h <= 0) h = 720;
        Quad q = (Quad) loadingImage.getMesh();
        q.updateGeometry(w, h);
        loadingImage.setLocalTranslation(0, 0, 0);
    }

    // ============================================================
    // MAP
    // ============================================================

    private MapRenderer mapRenderer;
    private MapWindow mapWindow;

    public void initMap(Node sceneNode, PlayerManager pm) {
        if (mapRenderer == null) {
            mapRenderer = new MapRenderer(app);
            mapRenderer.initialize();
        }
        if (mapWindow == null) {
            mapWindow = new MapWindow(app, mapRenderer, pm);
            mapWindow.show();
        }
    }

    public void toggleMap() {
        if (mapWindow == null) return;
        if (mapWindow.isVisible()) {
            mapWindow.hide();
            SoundManager.playSound(SoundManager.SOUND_WINDOW_CLOSE);
        } else {
            closeAllWindowsExcept("map");
            mapWindow.show();
            SoundManager.playSound(SoundManager.SOUND_WINDOW_TALENTS);
        }
    }

    public void updateMap(float tpf) {
        if (mapRenderer == null || playerManager == null || mapWindow == null) return;
        if (!mapWindow.isVisible()) return;
        Vector3f pos = playerManager.getPosition();
        if (pos != null) mapRenderer.update(playerManager);
        mapWindow.update();
    }

    private String getLocalized(String key) {
        return LocalizationManager.getInstance().get(key);
    }

    // ============================================================
    // BLACKSMITH
    // ============================================================

    private BlacksmithWindow blacksmithWindow;

    public BlacksmithWindow getBlacksmithWindow() {
        if (blacksmithWindow == null) blacksmithWindow = new BlacksmithWindow(app, this);
        return blacksmithWindow;
    }

    public void toggleBlacksmith() {
        BlacksmithWindow w = getBlacksmithWindow();
        if (w.isVisible()) {
            w.hide();
            SoundManager.playSound(SoundManager.SOUND_WINDOW_CLOSE);
        } else {
            closeAllWindowsExcept("blacksmith");
            w.show();
            SoundManager.playSound(SoundManager.SOUND_WINDOW_TRADER);
        }
    }

    // ============================================================
    // CHARACTER STATS
    // ============================================================

    private CharacterStatsWindow characterStatsWindow;

    public void toggleCharacterStats() {
        if (characterStatsWindow == null) {
            if (playerManager == null) return;
            characterStatsWindow = new CharacterStatsWindow(app, this, playerManager);
        }
        if (characterStatsWindow.isVisible()) {
            characterStatsWindow.hide();
            SoundManager.playSound(SoundManager.SOUND_WINDOW_CLOSE);
        } else {
            closeAllWindowsExcept("characterStats");
            characterStatsWindow.show();
            SoundManager.playSound(SoundManager.SOUND_WINDOW_TALENTS);
        }
    }

    public CharacterStatsWindow getCharacterStatsWindow() {
        return characterStatsWindow;
    }

    // ============================================================
    // CONTROLS HELP
    // ============================================================

    private ControlsHelpWindow controlsHelpWindow;
    private boolean controlsHelpAttemptedThisSession = false;

    public void showControlsHelpIfNeeded() {
        if (controlsHelpAttemptedThisSession) return;
        controlsHelpAttemptedThisSession = true;
        if (SettingsManager.getInstance().isHideControlsHelp()) return;
        if (controlsHelpWindow == null) controlsHelpWindow = new ControlsHelpWindow(app, this);
        closeAllWindowsExcept("controlsHelp");
        controlsHelpWindow.show();
    }
     private Button teleportButton;   // <-- ДОБАВЛЕНО
    private Button statsButton;
}