package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.material.Material;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.event.MouseEventControl;
import com.simsilica.lemur.event.MouseListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Числовая виртуальная клавиатура — плавающая панель СЛЕВА,
 * ПО ВЕРТИКАЛИ ПО ЦЕНТРУ экрана.
 *
 * Фон имеет отступ сверху и снизу по высоте одной кнопки
 * (KEY_HEIGHT) — нижний ряд кнопок (⌫ 0 OK) больше не упирается
 * в нижний край панели, у всей клавиатуры есть «воздух» вокруг.
 */
public class VirtualKeyboard {

    private SimpleApplication app;
    private Node guiNode;
    private Node keyboardNode;
    private Geometry background;
    private TextField activeField;
    private boolean isVisible = false;

    private float currentHeight = 0;

    // ============================================================
    // ГЕОМЕТРИЯ
    // ============================================================
    private static final float KEY_WIDTH = 80f;
    private static final float KEY_HEIGHT = 65f;
    private static final float KEY_SPACING = 6f;

    /** Отступ от левого края экрана. */
    private static final float LEFT_MARGIN = 15f;

    /**
     * Паддинг сверху и снизу панели.
     * Раньше фон был впритык к кнопкам, и нижний ряд визуально
     * «не помещался». Теперь сверху и снизу добавлена полоса
     * высотой в одну кнопку.
     */
    private static final float PADDING_Y = KEY_HEIGHT;

    public interface OnShowListener { void onShow(float keyboardHeight); }
    public interface OnHideListener { void onHide(); }
    private OnShowListener showListener;
    private OnHideListener hideListener;

    private static final String[][] NUM_ROWS = {
            {"1", "2", "3"},
            {"4", "5", "6"},
            {"7", "8", "9"}
    };

    private List<Button> allButtons = new ArrayList<>();

    public VirtualKeyboard(SimpleApplication app, Node guiNode) {
        this.app = app;
        this.guiNode = guiNode;
        keyboardNode = new Node("VirtualKeyboard");
        keyboardNode.setCullHint(Spatial.CullHint.Always);
        guiNode.attachChild(keyboardNode);
    }

    public void setOnShowListener(OnShowListener listener) { this.showListener = listener; }
    public void setOnHideListener(OnHideListener listener) { this.hideListener = listener; }

    public void show(TextField target) {
        if (target == null) return;
        this.activeField = target;
        buildKeyboard();
        keyboardNode.setCullHint(Spatial.CullHint.Dynamic);
        isVisible = true;
        if (showListener != null) showListener.onShow(currentHeight);
    }

    public void hide() {
        keyboardNode.setCullHint(Spatial.CullHint.Always);
        isVisible = false;
        activeField = null;
        if (hideListener != null) hideListener.onHide();
    }

    public boolean isVisible() { return isVisible; }

    public void rebuild() {
        if (isVisible && activeField != null) {
            keyboardNode.detachAllChildren();
            allButtons.clear();
            buildKeyboard();
            if (showListener != null) showListener.onShow(currentHeight);
        }
    }

    public Node getContainer() { return keyboardNode; }
    public float getHeight() { return currentHeight; }

    // ============================================================
    // ШИРИНА / ВЫСОТА GUI-ОБЛАСТИ
    // ============================================================
    private float getGuiWidth() {
        try {
            if (app.getGuiViewPort() != null
                    && app.getGuiViewPort().getCamera() != null) {
                float w = app.getGuiViewPort().getCamera().getWidth();
                if (w > 0) return w;
            }
        } catch (Exception ignored) {}
        try {
            if (app.getContext() != null && app.getContext().getSettings() != null) {
                float w = app.getContext().getSettings().getWidth();
                if (w > 0) return w;
            }
        } catch (Exception ignored) {}
        return 800f;
    }

    private float getGuiHeight() {
        try {
            if (app.getGuiViewPort() != null
                    && app.getGuiViewPort().getCamera() != null) {
                float h = app.getGuiViewPort().getCamera().getHeight();
                if (h > 0) return h;
            }
        } catch (Exception ignored) {}
        try {
            if (app.getContext() != null && app.getContext().getSettings() != null) {
                float h = app.getContext().getSettings().getHeight();
                if (h > 0) return h;
            }
        } catch (Exception ignored) {}
        return 600f;
    }

    // ============================================================
    // BINDING
    // ============================================================
    private void bindTouchAction(Button button, final Runnable action) {
        MouseEventControl.addListenersToSpatial(button, new MouseListener() {
            @Override
            public void mouseButtonEvent(MouseButtonEvent evt, Spatial s, Spatial t) {
                if (!isVisible) return;
                if (evt.getButtonIndex() != 0) return;
                if (!evt.isPressed()) return;
                action.run();
                evt.setConsumed();
            }
            @Override public void mouseEntered(MouseMotionEvent evt, Spatial s, Spatial t) {}
            @Override public void mouseExited(MouseMotionEvent evt, Spatial s, Spatial t) {}
            @Override public void mouseMoved(MouseMotionEvent evt, Spatial s, Spatial t) {}
        });
    }

    // ============================================================
    // BUILD — панель СЛЕВА, ПО ВЕРТИКАЛИ ПО ЦЕНТРУ
    // ============================================================
    private void buildKeyboard() {
        keyboardNode.detachAllChildren();
        allButtons.clear();

        keyboardNode.setLocalTranslation(0, 0, 0);

        float screenWidth  = getGuiWidth();
        float screenHeight = getGuiHeight();

        int rows = NUM_ROWS.length + 1;                       // 4
        float kbWidth  = 3 * KEY_WIDTH + 4 * KEY_SPACING;

        // ============================================================
        // ВЫСОТА ПАНЕЛИ:
        //   rows * KEY_HEIGHT      — все 4 ряда кнопок,
        //   (rows - 1) * SPACING   — зазоры МЕЖДУ рядами,
        //   2 * PADDING_Y          — полоса сверху и снизу
        //                            размером в одну кнопку.
        // Раньше padding не было, и нижний ряд визуально прилипал
        // к нижнему краю фона — теперь фон выше и вмещает всё.
        // ============================================================
        float kbHeight = rows * KEY_HEIGHT
                + (rows - 1) * KEY_SPACING
                + 2 * PADDING_Y;
        currentHeight = kbHeight;

        // X — к левому краю, Y — по центру экрана по вертикали
        float anchorX = LEFT_MARGIN;
        float anchorY = (screenHeight - kbHeight) / 2f;
        if (anchorX < 5f) anchorX = 5f;
        if (anchorY < 5f) anchorY = 5f;

        System.out.println("[VirtualKeyboard] screenW=" + screenWidth
                + " screenH=" + screenHeight
                + " kbW=" + kbWidth + " kbH=" + kbHeight
                + " anchorX=" + anchorX + " anchorY=" + anchorY);

        keyboardNode.setLocalTranslation(anchorX, anchorY, 0);

        // Фон панели
        Quad bgQuad = new Quad(kbWidth, kbHeight);
        background = new Geometry("KeyboardBg", bgQuad);
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0.15f, 0.15f, 0.2f, 0.95f));
        background.setMaterial(mat);
        background.setLocalTranslation(0, 0, -0.1f);
        keyboardNode.attachChild(background);

        // Верхняя строка кнопок с учётом верхнего padding
        float startY = kbHeight - PADDING_Y - KEY_HEIGHT;

        // Цифровые ряды
        for (int r = 0; r < NUM_ROWS.length; r++) {
            float rowWidth = NUM_ROWS[r].length * KEY_WIDTH
                    + (NUM_ROWS[r].length - 1) * KEY_SPACING;
            float startX = (kbWidth - rowWidth) / 2f;

            for (int c = 0; c < NUM_ROWS[r].length; c++) {
                String digit = NUM_ROWS[r][c];
                Button btn = createDigitKey(digit, KEY_WIDTH, KEY_HEIGHT);
                btn.setLocalTranslation(startX + c * (KEY_WIDTH + KEY_SPACING), startY, 0.1f);
                keyboardNode.attachChild(btn);
                allButtons.add(btn);
            }
            startY -= KEY_HEIGHT + KEY_SPACING;
        }

        // Нижний ряд: ⌫ | 0 | OK
        float rowWidth = 3 * KEY_WIDTH + 2 * KEY_SPACING;
        float startX = (kbWidth - rowWidth) / 2f;

        Button backBtn = createSpecialKey("⌫", KEY_WIDTH, KEY_HEIGHT);
        backBtn.setLocalTranslation(startX, startY, 0.1f);
        bindTouchAction(backBtn, () -> {
            if (activeField != null) {
                String text = activeField.getText();
                if (text == null) text = "";
                if (text.length() > 0) {
                    activeField.setText(text.substring(0, text.length() - 1));
                }
            }
        });
        keyboardNode.attachChild(backBtn);
        allButtons.add(backBtn);

        Button zeroBtn = createDigitKey("0", KEY_WIDTH, KEY_HEIGHT);
        zeroBtn.setLocalTranslation(startX + KEY_WIDTH + KEY_SPACING, startY, 0.1f);
        keyboardNode.attachChild(zeroBtn);
        allButtons.add(zeroBtn);

        Button okBtn = createSpecialKey("OK", KEY_WIDTH, KEY_HEIGHT);
        okBtn.setLocalTranslation(startX + 2 * (KEY_WIDTH + KEY_SPACING), startY, 0.1f);
        bindTouchAction(okBtn, this::hide);
        keyboardNode.attachChild(okBtn);
        allButtons.add(okBtn);
    }

    private Button createDigitKey(String label, float w, float h) {
        Button btn = new Button(label);
        btn.setPreferredSize(new Vector3f(w, h, 0));
        btn.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.3f, 0.3f, 0.4f, 0.95f)));
        btn.setColor(ColorRGBA.White);
        btn.setFontSize(Math.min(w, h) * 0.55f);

        bindTouchAction(btn, () -> {
            if (activeField != null && label.length() == 1) {
                String current = activeField.getText();
                if (current == null) current = "";
                activeField.setText(current + label);
            }
        });
        return btn;
    }

    private Button createSpecialKey(String label, float w, float h) {
        Button btn = new Button(label);
        btn.setPreferredSize(new Vector3f(w, h, 0));
        btn.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.4f, 0.4f, 0.55f, 0.95f)));
        btn.setColor(ColorRGBA.White);
        btn.setFontSize(Math.min(w, h) * 0.45f);
        return btn;
    }
}