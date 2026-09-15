package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;

import com.simsilica.lemur.Button;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.TextField;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.event.MouseEventControl;
import com.simsilica.lemur.event.MouseListener;
public class SettingsWindow implements RawInputListener {

    private final SimpleApplication app;
    private final UIManager uiManager;

    private Node windowNode;

    private boolean isVisible = false;

    private float scale = 1f;
    private float winW;
    private float winH;
  /**
     * Привязывает действие к кнопке через DOWN-событие.
     *
     * addClickCommands срабатывает по UP, которое на Android при
     * тапе нередко теряется — кнопка мигает, но действие не
     * выполняется. Тот же приём используется в InventoryManager,
     * TraderWindow, TalentWindow, AuctionWindow, BlacksmithWindow,
     * VirtualKeyboard, UIManager.
     */
    private void bindTouchAction(Button button, final Runnable action) {

        MouseEventControl.addListenersToSpatial(
                button,
                new MouseListener() {

                    @Override
                    public void mouseButtonEvent(
                            MouseButtonEvent evt,
                            Spatial s,
                            Spatial t) {
                        if (!isVisible) return;
                        if (evt.getButtonIndex() != 0) return;
                        if (!evt.isPressed()) return;
                        action.run();
                    }

                    @Override public void mouseEntered(MouseMotionEvent evt, Spatial s, Spatial t) {}
                    @Override public void mouseExited(MouseMotionEvent evt, Spatial s, Spatial t) {}
                    @Override public void mouseMoved(MouseMotionEvent evt, Spatial s, Spatial t) {}
                }
        );
    }
    // =============================================================
    // ШРИФТ
    // =============================================================

    private BitmapFont currentFont;

    // =============================================================
    // ГРОМКОСТЬ
    // =============================================================

    private float soundVolume = 0.5f;

    private Label soundValueLabel;

    /**
     * Фон полосы.
     */
    private Geometry volumeBar;

    /**
     * Заполненная часть полосы.
     */
    private Geometry volumeFill;

    /**
     * Ручка слайдера.
     */
    private Geometry volumeKnob;

    /**
     * Координаты слайдера внутри окна.
     */
    private float sliderX;
    private float sliderY;
    private float sliderWidth;
    private float sliderHeight;

    /**
     * Перетаскиваем ли ручку.
     */
    private boolean draggingVolume = false;

    /**
     * Зарегистрирован ли listener.
     */
    private boolean inputRegistered = false;

    // =============================================================
    // ТЕЛЕПОРТ (X / Y / Z)
    // =============================================================

    private TextField xField;
    private TextField yField;
    private TextField zField;

    // =============================================================
    // КОНСТРУКТОР
    // =============================================================

    public SettingsWindow(
            SimpleApplication app,
            UIManager uiManager
    ) {

        this.app = app;
        this.uiManager = uiManager;

        loadCurrentFont();

        createWindow();

        registerInput();
    }

    // =============================================================
    // ШРИФТ
    // =============================================================

    private void loadCurrentFont() {

        String language =
                SettingsManager.getInstance()
                        .getLanguage();

        if ("ru".equalsIgnoreCase(language)) {

            try {

                currentFont =
                        app.getAssetManager()
                                .loadFont(
                                        "Interface/Fonts/ru.fnt"
                                );

                System.out.println(
                        "[SettingsWindow] Russian font loaded"
                );

            } catch (Exception e) {

                System.err.println(
                        "[SettingsWindow] Failed to load Russian font: "
                                + e.getMessage()
                );

                currentFont = null;
            }

        } else {

            try {

                currentFont =
                        app.getAssetManager()
                                .loadFont(
                                        "Interface/Fonts/en.fnt"
                                );

                System.out.println(
                        "[SettingsWindow] English font loaded"
                );

            } catch (Exception e) {

                System.err.println(
                        "[SettingsWindow] Failed to load English font: "
                                + e.getMessage()
                );

                currentFont = null;
            }
        }
    }

    // =============================================================
    // INPUT
    // =============================================================

    private void registerInput() {

        if (inputRegistered) {
            return;
        }

        if (app.getInputManager() == null) {
            return;
        }

        app.getInputManager()
                .addRawInputListener(this);

        inputRegistered = true;
    }

    // =============================================================
    // SCALE
    // =============================================================

    private void updateScale() {

        float screenWidth =
                app.getCamera().getWidth();

        float screenHeight =
                app.getCamera().getHeight();

        float baseWidth = 800f;
        float baseHeight = 600f;

        float scaleX =
                screenWidth / baseWidth;

        float scaleY =
                screenHeight / baseHeight;

        scale =
                Math.min(
                        scaleX,
                        scaleY
                );

        scale =
                Math.max(
                        0.5f,
                        Math.min(
                                scale,
                                1.5f
                        )
                );
    }

    // =============================================================
    // СОЗДАНИЕ ОКНА
    // =============================================================

    private void createWindow() {

        updateScale();

        windowNode =
                new Node(
                        "SettingsWindowNode"
                );

        // =========================================================
        // РАЗМЕР
        // =========================================================

        winW =
                420f * scale;

        winH =
                540f * scale;

        // =========================================================
        // ФОН
        // =========================================================

        Geometry bg =
                uiManager.createBackgroundGeometry(
                        winW,
                        winH
                );

        bg.setLocalTranslation(
                0f,
                0f,
                -0.1f
        );

        windowNode.attachChild(
                bg
        );

        // =========================================================
        // ЗАГОЛОВОК
        // =========================================================

        Label titleLabel =
                createLabel(
                        getLocalized(
                                "settings.title"
                        ),
                        24f
                );

        titleLabel.setLocalTranslation(
                winW / 2f - 80f * scale,
                winH - 30f * scale,
                0.1f
        );

        windowNode.attachChild(
                titleLabel
        );

        // =========================================================
        // X
        // =========================================================

Button closeButton =
        createButton(
                getLocalized("ui.close_button"),
                14f
        );

closeButton.setColor(ColorRGBA.White);
closeButton.setBackground(
        new com.simsilica.lemur.component.QuadBackgroundComponent(
                new ColorRGBA(0f, 0f, 0f, 0f)   // прозрачный фон
        ));

closeButton.setPreferredSize(
        new Vector3f(
                100f * scale,
                30f * scale,
                0f
        )
);

closeButton.setLocalTranslation(
        winW - 110f * scale,
        winH - 35f * scale,
        0.1f
);

bindTouchAction(closeButton, () -> hide());

windowNode.attachChild(
        closeButton
);

        // =========================================================
        // НАЧАЛЬНАЯ ПОЗИЦИЯ
        // =========================================================

        float yPos =
                winH - 75f * scale;

        float stepY =
                70f * scale;

        // =========================================================
        // ЗВУК
        // =========================================================

        Label soundLabel =
                createLabel(
                        getLocalized(
                                "settings.sound"
                        ),
                        16f
                );

        soundLabel.setLocalTranslation(
                20f * scale,
                yPos,
                0.1f
        );

        windowNode.attachChild(
                soundLabel
        );

        // =========================================================
        // ЗАГРУЗКА ГРОМКОСТИ
        // =========================================================

        soundVolume =
                SettingsManager.getInstance()
                        .getSoundVolume();

        soundVolume =
                clamp(
                        soundVolume,
                        0f,
                        1f
                );

        // =========================================================
        // СОЗДАНИЕ СЛАЙДЕРА
        // =========================================================

        createVolumeSlider(
                yPos - 35f * scale
        );

        // =========================================================
        // ПРОЦЕНТ
        // =========================================================

        soundValueLabel =
                createLabel(
                        getVolumeText(),
                        14f
                );

        soundValueLabel.setLocalTranslation(
                315f * scale,
                yPos - 29f * scale,
                0.1f
        );

        windowNode.attachChild(
                soundValueLabel
        );

        // =========================================================
        // ЯЗЫК
        // =========================================================

        yPos -= stepY;

        Label langLabel =
                createLabel(
                        getLocalized(
                                "settings.language"
                        ),
                        16f
                );

        langLabel.setLocalTranslation(
                20f * scale,
                yPos,
                0.1f
        );

        windowNode.attachChild(
                langLabel
        );

        // =========================================================
        // КНОПКА ЯЗЫКА
        // =========================================================

        yPos -= 32f * scale;

        Button langButton =
                createButton(
                        getLocalized(
                                "settings.language.switch"
                        ),
                        18f
                );

        langButton.setPreferredSize(
                new Vector3f(
                        180f * scale,
                        30f * scale,
                        0f
                )
        );

        langButton.setColor(
                ColorRGBA.White
        );

        langButton.setLocalTranslation(
                20f * scale,
                yPos,
                0.1f
        );

        bindTouchAction(langButton, () -> changeLanguage());


        windowNode.attachChild(
                langButton
        );

        // =========================================================
        // ТЕЛЕПОРТ (X / Y / Z)
        // =========================================================
        /**
        yPos -= stepY;

        Label teleportTitle =
                createLabel(
                        getLocalized("settings.teleport_title"),
                        16f
                );

        teleportTitle.setLocalTranslation(
                20f * scale,
                yPos,
                0.1f
        );

        windowNode.attachChild(teleportTitle);

        yPos -= 32f * scale;
        xField = createCoordField("X:", yPos, "0");

        yPos -= 32f * scale;
        yField = createCoordField("Y:", yPos, "0");

        yPos -= 32f * scale;
        zField = createCoordField("Z:", yPos, "2");

        yPos -= 40f * scale;

        Button teleportButton =
                createButton(
                        getLocalized("settings.teleport_button"),
                        16f
                );

        teleportButton.setPreferredSize(
                new Vector3f(150f * scale, 30f * scale, 0f)
        );

        teleportButton.setLocalTranslation(
                20f * scale,
                yPos,
                0.1f
        );

        teleportButton.addClickCommands(
                source -> teleportPlayer()
        );

        windowNode.attachChild(teleportButton);
        **/
        yPos -= stepY;

        Button emergencyTeleportButton =
                createButton(
                        getLocalized("settings.emergency_teleport"),
                        16f
                );

        emergencyTeleportButton.setPreferredSize(
                new Vector3f(220f * scale, 30f * scale, 0f)
        );

        emergencyTeleportButton.setLocalTranslation(
                20f * scale,
                yPos,
                0.1f
        );

        bindTouchAction(emergencyTeleportButton, () -> emergencyTeleport());


        windowNode.attachChild(emergencyTeleportButton);
        // =========================================================
        // ПОЗИЦИЯ ОКНА
        // =========================================================

        positionWindow();
    }

    // =============================================================
    // СОЗДАНИЕ СЛАЙДЕРА
    // =============================================================

    private void createVolumeSlider(
            float localY
    ) {

        sliderX =
                20f * scale;

        sliderY =
                localY;

        sliderWidth =
                270f * scale;

        sliderHeight =
                12f * scale;

        // =========================================================
        // ДОРОЖКА
        // =========================================================

        volumeBar =
                createRectangle(
                        sliderWidth,
                        sliderHeight
                );

        volumeBar.setLocalTranslation(
                sliderX,
                sliderY,
                0.05f
        );

        windowNode.attachChild(
                volumeBar
        );

        // =========================================================
        // ЗАПОЛНЕНИЕ
        // =========================================================

        float fillWidth =
                Math.max(
                        2f * scale,
                        sliderWidth * soundVolume
                );

        volumeFill =
                createRectangle(
                        fillWidth,
                        sliderHeight
                );

        volumeFill.setLocalTranslation(
                sliderX,
                sliderY,
                0.06f
        );

        windowNode.attachChild(
                volumeFill
        );

        // =========================================================
        // РУЧКА
        // =========================================================

        float knobSize =
                20f * scale;

        volumeKnob =
                createRectangle(
                        knobSize,
                        knobSize
                );

        windowNode.attachChild(
                volumeKnob
        );

        updateVolumeKnobPosition();
    }

    // =============================================================
    // ПРЯМОУГОЛЬНИК
    // =============================================================

    private Geometry createRectangle(
            float width,
            float height
    ) {

        Quad quad =
                new Quad(
                        Math.max(
                                1f,
                                width
                        ),
                        Math.max(
                                1f,
                                height
                        )
                );

        Geometry geometry =
                new Geometry(
                        "SettingsVolumeElement",
                        quad
                );

        com.jme3.material.Material material =
                new com.jme3.material.Material(
                        app.getAssetManager(),
                        "Common/MatDefs/Gui/Gui.j3md"
                );

        material.setColor(
                "Color",
                ColorRGBA.White
        );

        geometry.setMaterial(
                material
        );

        return geometry;
    }

    // =============================================================
    // ПОЛЕ ВВОДА КООРДИНАТЫ
    // =============================================================

    private TextField createCoordField(
            String labelText,
            float y,
            String defaultValue
    ) {

        Label label =
                createLabel(labelText, 14f);

        label.setLocalTranslation(
                20f * scale,
                y,
                0.1f
        );

        windowNode.attachChild(label);

        TextField field =
                new TextField(defaultValue);

        if (currentFont != null) {
            field.setFont(currentFont);
        }

        field.setFontSize(14f * scale);

        field.setPreferredSize(
                new Vector3f(100f * scale, 22f * scale, 0f)
        );

        field.setLocalTranslation(
                60f * scale,
                y,
                0.1f
        );

        windowNode.attachChild(field);

        return field;
    }

    // =============================================================
    // ПОЗИЦИЯ РУЧКИ
    // =============================================================

    private void updateVolumeKnobPosition() {

        if (volumeKnob == null) {
            return;
        }

        float knobSize =
                20f * scale;

        float knobX =
                sliderX
                        + sliderWidth * soundVolume
                        - knobSize / 2f;

        float knobY =
                sliderY
                        - (knobSize - sliderHeight) / 2f;

        volumeKnob.setLocalTranslation(
                knobX,
                knobY,
                0.08f
        );
    }

    // =============================================================
    // ОБНОВЛЕНИЕ ЗАПОЛНЕНИЯ
    // =============================================================

    private void updateVolumeBar() {

        if (volumeFill == null) {
            return;
        }

        float fillWidth =
                Math.max(
                        2f * scale,
                        sliderWidth * soundVolume
                );

        Quad quad =
                new Quad(
                        fillWidth,
                        sliderHeight
                );

        volumeFill.setMesh(
                quad
        );

        volumeFill.setLocalTranslation(
                sliderX,
                sliderY,
                0.06f
        );
    }

    // =============================================================
    // УСТАНОВКА ГРОМКОСТИ ПО МЫШИ
    // =============================================================

    private void setVolumeFromMouse(
            float mouseX
    ) {

        if (!isVisible) {
            return;
        }

        float windowX =
                windowNode
                        .getLocalTranslation()
                        .x;

        float localX =
                mouseX - windowX;

        float value =
                (localX - sliderX)
                        / sliderWidth;

        value =
                clamp(
                        value,
                        0f,
                        1f
                );

        soundVolume =
                value;

        // =========================================================
        // СОХРАНЕНИЕ
        // =========================================================

        SettingsManager.getInstance()
                .setSoundVolume(
                        soundVolume
                );
SoundManager.setMasterVolume(
        soundVolume
);
        // =========================================================
        // UI
        // =========================================================

        updateVolumeBar();

        updateVolumeKnobPosition();

        if (soundValueLabel != null) {

            soundValueLabel.setText(
                    getVolumeText()
            );
        }
    }

    // =============================================================
    // ТЕКСТ
    // =============================================================

    private String getVolumeText() {

        return Math.round(
                soundVolume * 100f
        ) + "%";
    }

    // =============================================================
    // НАЖАТИЕ МЫШИ
    // =============================================================

    @Override
    public void onMouseButtonEvent(
            MouseButtonEvent evt
    ) {

        if (!isVisible) {
            return;
        }

        if (evt.getButtonIndex()
                != MouseInput.BUTTON_LEFT) {

            return;
        }

        if (evt.isPressed()) {

            float mouseX =
                    evt.getX();

            float mouseY =
                    evt.getY();

            if (isMouseOverSlider(
                    mouseX,
                    mouseY
            )) {

                draggingVolume = true;

                setVolumeFromMouse(
                        mouseX
                );
            }

        } else {

            draggingVolume = false;
        }
    }

    // =============================================================
    // ДВИЖЕНИЕ МЫШИ
    // =============================================================

    @Override
    public void onMouseMotionEvent(
            MouseMotionEvent evt
    ) {

        if (!isVisible) {
            return;
        }

        if (!draggingVolume) {
            return;
        }

        float mouseX =
                evt.getX();

        setVolumeFromMouse(
                mouseX
        );
    }

    // =============================================================
    // ПРОВЕРКА СЛАЙДЕРА
    // =============================================================

    private boolean isMouseOverSlider(
            float mouseX,
            float mouseY
    ) {

        if (windowNode == null) {
            return false;
        }

        Vector3f windowPosition =
                windowNode.getLocalTranslation();

        float localX =
                mouseX
                        - windowPosition.x;

        /*
         * jME экранные координаты имеют начало
         * снизу слева.
         */
        float localY =
                mouseY
                        - windowPosition.y;

        float extra =
                15f * scale;

        return localX >=
                sliderX - extra

                && localX <=
                sliderX
                        + sliderWidth
                        + extra

                && localY >=
                sliderY - extra

                && localY <=
                sliderY
                        + sliderHeight
                        + extra;
    }

    // =============================================================
    // KEY
    // =============================================================

    @Override
    public void onKeyEvent(
            KeyInputEvent evt
    ) {
        // Не используется.
    }

    // =============================================================
    // JOYSTICK AXIS
    // =============================================================

    @Override
    public void onJoyAxisEvent(
            JoyAxisEvent evt
    ) {
        // Не используется.
    }

    // =============================================================
    // JOYSTICK BUTTON
    // =============================================================

    @Override
    public void onJoyButtonEvent(
            JoyButtonEvent evt
    ) {
        // Не используется.
    }

    // =============================================================
    // UPDATE
    // =============================================================

    public void update() {

        /*
         * Slider обновляется непосредственно
         * через MouseMotionEvent.
         */
    }

    // =============================================================
    // LABEL
    // =============================================================

    private Label createLabel(
            String text,
            float fontSize
    ) {

        Label label =
                new Label(text);

        if (currentFont != null) {

            label.setFont(
                    currentFont
            );
        }

        label.setFontSize(
                fontSize * scale
        );

        label.setColor(
                ColorRGBA.White
        );

        return label;
    }

    // =============================================================
    // BUTTON
    // =============================================================

    private Button createButton(
            String text,
            float fontSize
    ) {

        Button button =
                new Button(text);

        if (currentFont != null) {

            button.setFont(
                    currentFont
            );
        }

        button.setFontSize(
                fontSize * scale
        );

        return button;
    }

    // =============================================================
    // СМЕНА ЯЗЫКА
    // =============================================================

    private void changeLanguage() {

        String currentLang =
                SettingsManager.getInstance()
                        .getLanguage();

        String newLang =
                "en".equalsIgnoreCase(
                        currentLang
                )
                        ? "ru"
                        : "en";

        SettingsManager.getInstance()
                .setLanguage(
                        newLang
                );

        LocalizationManager.getInstance()
                .loadLanguage(
                        newLang
                );

        loadCurrentFont();

        refreshUI();
        if (uiManager != null) {

        uiManager.showToast(
                getLocalized(
                        "settings.language_restart_required"
                )
        );
    }
    }

    // =============================================================
    // СБРОС
    // =============================================================

    private void resetSettings() {

        System.out.println(
                "[Settings] Reset settings"
        );

        SettingsManager.getInstance()
                .setSoundVolume(
                        0.5f
                );
        

        SettingsManager.getInstance()
                .setLanguage(
                        "en"
                );

        LocalizationManager.getInstance()
                .loadLanguage(
                        "en"
                );

        loadCurrentFont();

        refreshUI();

        System.out.println(
                "[Settings] Reset complete: "
                        + "volume=50%, language=en"
        );
    }

    // =============================================================
    // ТЕЛЕПОРТ
    // =============================================================

    private void teleportPlayer() {

        if (uiManager == null) {
            return;
        }

        PlayerManager pm =
                uiManager.getPlayerManager();

        if (pm == null) {

            uiManager.showToast(
                    getLocalized("settings.teleport_no_player")
            );

            return;
        }

        try {

            float x = Float.parseFloat(xField.getText().trim());
            float y = Float.parseFloat(yField.getText().trim());
            float z = Float.parseFloat(zField.getText().trim());

            pm.setPosition(new Vector3f(x, y, z));

            uiManager.showToast(
                    getLocalized("settings.teleport_success")
            );

        } catch (NumberFormatException e) {

            uiManager.showToast(
                    getLocalized("settings.teleport_invalid")
            );
        }
    }

    // =============================================================
    // ПОЗИЦИЯ ОКНА
    // =============================================================

    private void positionWindow() {

        float w =
                app.getCamera().getWidth();

        float h =
                app.getCamera().getHeight();

        float offsetY =
                40f * scale
                        + 90f * scale;

        windowNode.setLocalTranslation(
                (w - winW) / 2f,
                (h - winH) / 2f + offsetY,
                0f
        );
    }

    // =============================================================
    // REFRESH
    // =============================================================

    private void refreshUI() {

        boolean wasVisible =
                isVisible;

        if (windowNode != null) {

            if (windowNode.getParent() != null) {

                windowNode.getParent()
                        .detachChild(
                                windowNode
                        );
            }
        }

        isVisible = false;

        LocalizationManager.getInstance()
                .loadLanguage(
                        SettingsManager.getInstance()
                                .getLanguage()
                );

        loadCurrentFont();

        createWindow();

        if (wasVisible) {

            show();
        }
    }

    // =============================================================
    // ЛОКАЛИЗАЦИЯ
    // =============================================================

    private String getLocalized(
            String key
    ) {

        return LocalizationManager
                .getInstance()
                .get(key);
    }

    // =============================================================
    // SHOW
    // =============================================================

    public void show() {

        if (isVisible) {
            return;
        }

        isVisible = true;

        if (windowNode == null) {
            createWindow();
        }

        windowNode.setCullHint(
                Node.CullHint.Dynamic
        );

        uiManager.attachNode(
                windowNode
        );
    }

    // =============================================================
    // HIDE
    // =============================================================

    public void hide() {

        if (!isVisible) {
            return;
        }

        isVisible = false;

        draggingVolume = false;

        if (windowNode != null) {

            windowNode.setCullHint(
                    Node.CullHint.Always
            );
        }

        uiManager.detachNode(
                windowNode
        );
    }

    // =============================================================
    // VISIBLE
    // =============================================================

    public boolean isVisible() {

        return isVisible;
    }

    // =============================================================
    // CLAMP
    // =============================================================

    private float clamp(
            float value,
            float min,
            float max
    ) {

        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }

    @Override
    public void beginInput() {
    }

    @Override
    public void endInput() {
    }

    /**
     * Android: полноценная обработка тач-событий для слайдера
     * громкости.
     *
     * Раньше onTouchEvent был пустой — на телефоне слайдер
     * вообще не реагировал на палец, потому что синтезированные
     * mouse-события (которые ловит onMouseButtonEvent /
     * onMouseMotionEvent) приходят не всегда и иногда с задержкой.
     *
     * Оба пути безопасно сосуществуют: DOWN/MOVE/UP дублируются,
     * но setVolumeFromMouse идемпотентна, а draggingVolume
     * просто выставляется в true/false без побочных эффектов.
     */
    @Override
    public void onTouchEvent(TouchEvent te) {

        if (!isVisible) {
            return;
        }

        switch (te.getType()) {

            case DOWN: {

                float tx = te.getX();
                float ty = te.getY();

                if (isMouseOverSlider(tx, ty)) {
                    draggingVolume = true;
                    setVolumeFromMouse(tx);
                }
                break;
            }

            case MOVE: {

                if (!draggingVolume) {
                    break;
                }

                setVolumeFromMouse(te.getX());
                break;
            }

            case UP:
            case FLING: {

                draggingVolume = false;
                break;
            }

            default:
                break;
        }
    }
    
     // =============================================================
    // АВАРИЙНЫЙ ТЕЛЕПОРТ (фиксированная точка, на случай провала под пол)
    // =============================================================

    private void emergencyTeleport() {

        if (uiManager == null) {
            return;
        }

        PlayerManager pm =
                uiManager.getPlayerManager();

        if (pm == null) {

            uiManager.showToast(
                    getLocalized("settings.teleport_no_player")
            );

            return;
        }

        pm.setPosition(new Vector3f(0f, 0f, 5f));


    }
}