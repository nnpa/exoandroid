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
 * Числовая виртуальная клавиатура.
 *
 * Используется для ввода чисел (например, цены лота в AuctionWindow).
 * Содержит:
 *   - цифры 0..9
 *   - backspace (⌫)
 *   - кнопку закрытия (OK)
 *
 * Буквы, Shift и пробел удалены.
 */
public class VirtualKeyboard {

    private SimpleApplication app;
    private Node guiNode;
    private Node keyboardNode;
    private Geometry background;
    private TextField activeField;
    private boolean isVisible = false;

    private float currentHeight = 0;

    // Слушатели
    public interface OnShowListener {
        void onShow(float keyboardHeight);
    }
    public interface OnHideListener {
        void onHide();
    }
    private OnShowListener showListener;
    private OnHideListener hideListener;

    /**
     * Раскладка числовых клавиш: 3 ряда по 3 цифры (1-2-3, 4-5-6, 7-8-9).
     * Нижний ряд (⌫, 0, OK) создаётся отдельно — см. buildKeyboard().
     */
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

    public void setOnShowListener(OnShowListener listener) {
        this.showListener = listener;
    }

    public void setOnHideListener(OnHideListener listener) {
        this.hideListener = listener;
    }

    public void show(TextField target) {
        if (target == null) return;
        this.activeField = target;
        buildKeyboard();
        keyboardNode.setCullHint(Spatial.CullHint.Dynamic);
        isVisible = true;
        if (showListener != null) {
            showListener.onShow(currentHeight);
        }
    }

    public void hide() {
        keyboardNode.setCullHint(Spatial.CullHint.Always);
        isVisible = false;
        activeField = null;
        if (hideListener != null) {
            hideListener.onHide();
        }
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void rebuild() {
        if (isVisible && activeField != null) {
            keyboardNode.detachAllChildren();
            allButtons.clear();
            buildKeyboard();
            if (showListener != null) {
                showListener.onShow(currentHeight);
            }
        }
    }

    public Node getContainer() {
        return keyboardNode;
    }

    public float getHeight() {
        return currentHeight;
    }

    // ============================================================
    // ANDROID-SAFE BUTTON BINDING
    // ============================================================

    /**
     * Привязывает действие к кнопке через DOWN-событие.
     *
     * См. InventoryManager / TraderWindow / TalentWindow / AuctionWindow —
     * addClickCommands на Android срабатывает ненадёжно (UP-событие
     * при тапе нередко теряется), поэтому везде используем DOWN.
     */
    private void bindTouchAction(Button button, final Runnable action) {
        MouseEventControl.addListenersToSpatial(button, new MouseListener() {
            @Override
            public void mouseButtonEvent(MouseButtonEvent evt, Spatial s, Spatial t) {
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

    // ============================================================
    // BUILD
    // ============================================================

    private void buildKeyboard() {
        keyboardNode.detachAllChildren();
        allButtons.clear();

        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();

        // Ключи: 3 колонки. Ограничиваем сверху, чтобы на больших
        // экранах кнопки не растягивались до абсурда.
        float keyWidth = Math.min(screenWidth / 4.5f, 160f);
        float keyHeight = Math.min(screenHeight / 9f, 110f);
        float spacing = 8f;

        // 4 ряда клавиш (3 ряда цифр + нижний ряд)
        int rows = NUM_ROWS.length + 1;
        float totalHeight = rows * keyHeight + (rows + 1) * spacing + 20;
        currentHeight = totalHeight;

        // Фон
        Quad bgQuad = new Quad(screenWidth, totalHeight);
        background = new Geometry("KeyboardBg", bgQuad);
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0.15f, 0.15f, 0.2f, 0.95f));
        background.setMaterial(mat);
        background.setLocalTranslation(0, 0, -0.1f);
        keyboardNode.attachChild(background);

        float startY = totalHeight - keyHeight - spacing;

        // ============================================================
        // ЦИФРОВЫЕ РЯДЫ (1-2-3, 4-5-6, 7-8-9)
        // ============================================================

        for (int r = 0; r < NUM_ROWS.length; r++) {

            float rowWidth = NUM_ROWS[r].length * keyWidth
                    + (NUM_ROWS[r].length - 1) * spacing;
            float startX = (screenWidth - rowWidth) / 2f;

            for (int c = 0; c < NUM_ROWS[r].length; c++) {

                String digit = NUM_ROWS[r][c];
                Button btn = createDigitKey(digit, keyWidth, keyHeight);
                btn.setLocalTranslation(
                        startX + c * (keyWidth + spacing),
                        startY,
                        0.1f);
                keyboardNode.attachChild(btn);
                allButtons.add(btn);
            }

            startY -= keyHeight + spacing;
        }

        // ============================================================
        // НИЖНИЙ РЯД: ⌫ | 0 | OK
        // ============================================================

        float rowWidth = 3 * keyWidth + 2 * spacing;
        float startX = (screenWidth - rowWidth) / 2f;

        // Backspace
        Button backBtn = createSpecialKey("⌫", keyWidth, keyHeight);
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

        // 0
        Button zeroBtn = createDigitKey("0", keyWidth, keyHeight);
        zeroBtn.setLocalTranslation(startX + keyWidth + spacing, startY, 0.1f);
        keyboardNode.attachChild(zeroBtn);
        allButtons.add(zeroBtn);

        // OK (закрыть)
        Button okBtn = createSpecialKey("OK", keyWidth, keyHeight);
        okBtn.setLocalTranslation(startX + 2 * (keyWidth + spacing), startY, 0.1f);
        bindTouchAction(okBtn, () -> hide());
        keyboardNode.attachChild(okBtn);
        allButtons.add(okBtn);

        keyboardNode.setLocalTranslation(0, 0, 0);
    }

    // ============================================================
    // KEY FACTORIES
    // ============================================================

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