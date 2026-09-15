package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.*;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.event.MouseEventControl;
import com.simsilica.lemur.event.MouseListener;
import com.mygame.Main;
import com.mygame.items.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Окно кузнеца: вставка камней в сокеты предметов.
 *
 * Справа сверху — предметы с пустыми сокетами (не надетые).
 * Справа снизу — камни в инвентаре (не надетые).
 * Слева — "наковальня": сюда перетаскивается предмет,
 * после чего под ней появляются его сокеты — сюда перетаскиваются камни.
 * Клик по занятому сокету извлекает камень обратно в инвентарь.
 */
public class BlacksmithWindow {

    /**
     * Множитель размера шрифтов — все шрифты увеличены в 2 раза.
     */
    private static final float FONT_MULT = 2f;

    private final SimpleApplication app;
    private final UIManager uiManager;
    private final NetworkManager networkManager;

    private final Node node;
    private boolean visible = false;

    private List<Map<String, Object>> socketableItems = new ArrayList<>();
    private List<Map<String, Object>> gems = new ArrayList<>();

    private String anvilItemId = null;
    private List<Map<String, Object>> anvilSockets = new ArrayList<>();

    private final List<Spatial> uiElements = new ArrayList<>();
    private Geometry anvilSquare;
    private float anvilX, anvilY, anvilSize;

    private final List<SocketCell> socketCells = new ArrayList<>();

    private static final int GRID_COLS = 4;

    private enum DragType { NONE, ITEM, GEM }

    private DragType dragType = DragType.NONE;
    private String dragItemId = null;
    private float dragStartX, dragStartY;
    private boolean isDragging = false;

    /**
     * Минимальное расстояние, которое нужно пройти пальцу/мыши,
     * чтобы обычный тап превратился в drag.
     */
    private static final float DRAG_THRESHOLD = 30f;

    private Picture dragCursorPicture;
    private static final float DRAG_CURSOR_SIZE = 50f;

    private RawInputListener dragInputListener;

    private float lastMouseX;
    private float lastMouseY;

    private static class SocketCell {
        int index;
        float x, y, size;
        boolean occupied;
    }

    public BlacksmithWindow(SimpleApplication app, UIManager uiManager) {

        this.app = app;
        this.uiManager = uiManager;

        Main main = (Main) app;
        this.networkManager = main != null ? main.getNetworkManager() : null;

        node = new Node("BlacksmithWindow");
        node.setCullHint(Node.CullHint.Always);

        setupDragListener();
    }

    // ============================================================
    // SHOW / HIDE
    // ============================================================

    public void show() {

        if (!uiManager.getGuiNode().hasChild(node)) {
            uiManager.getGuiNode().attachChild(node);
        }

        node.setCullHint(Node.CullHint.Dynamic);
        visible = true;

        anvilItemId = null;
        anvilSockets.clear();

        refreshFromServer();
    }

    public void hide() {

        removeDragCursor();
        dragType = DragType.NONE;
        dragItemId = null;
        isDragging = false;

        node.setCullHint(Node.CullHint.Always);
        visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    // ============================================================
    // ANDROID-SAFE BUTTON BINDING
    // ============================================================

    /**
     * Привязывает действие к кнопке через DOWN-событие.
     *
     * ВАЖНО: не используем button.addClickCommands(...) — этот путь
     * срабатывает по UP-событию, которое на Android при тапе
     * нередко теряется (та же проблема, что уже починена в
     * InventoryManager / TraderWindow / TalentWindow / AuctionWindow /
     * VirtualKeyboard). Кнопка мигает нажатой, но действие
     * не выполняется.
     */
    private void bindTouchAction(Button button, final Runnable action) {

        MouseEventControl.addListenersToSpatial(button, new MouseListener() {

            @Override
            public void mouseButtonEvent(MouseButtonEvent evt,
                                         Spatial spatial,
                                         Spatial target) {
                if (!visible) return;
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
    // ЗАГРУЗКА ДАННЫХ
    // ============================================================

    private void refreshFromServer() {

        if (networkManager == null) return;

        networkManager.getBlacksmithItems().thenAccept(items -> {

            app.enqueue(() -> {
                socketableItems = items != null ? items : new ArrayList<>();
                resyncAnvilSockets();
                rebuildUI();
                return null;
            });
        });

        networkManager.getBlacksmithGems().thenAccept(gemList -> {

            app.enqueue(() -> {
                gems = gemList != null ? gemList : new ArrayList<>();
                rebuildUI();
                return null;
            });
        });
    }

    // ============================================================
    // UI
    // ============================================================

    @SuppressWarnings("unchecked")
    private void rebuildUI() {

        node.detachAllChildren();
        uiElements.clear();
        socketCells.clear();

        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();

        /*
         * Окно немного расширено — под увеличенные ×2 шрифты
         * нужно больше места, иначе подписи налезают друг на друга.
         */
        float winW = 820f;
        float winH = 560f;

        float winX = (screenWidth - winW) / 2f;
        float winY = (screenHeight - winH) / 2f;

        Geometry bg = uiManager.createBackgroundGeometry(winW, winH);
        bg.setLocalTranslation(winX, winY, -1f);
        node.attachChild(bg);
        uiElements.add(bg);

        // ============================================================
        // НАКОВАЛЬНЯ
        // ============================================================

        anvilSize = 150f;
        anvilX = winX + 40f;
        anvilY = winY + winH - 60f - anvilSize;

        anvilSquare = new Geometry("Anvil", new Quad(anvilSize, anvilSize));
        Material anvilMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");

        if (anvilItemId != null) {

            Map<String, Object> entry = findSocketableById(anvilItemId);

            if (entry != null) {

                Item item = Item.fromMap((Map<String, Object>) entry.get("item"));
                Texture tex = tryLoadTexture(item != null ? item.getIconPath() : null);

                if (tex != null) {
                    anvilMat.setTexture("ColorMap", tex);
                } else {
                    anvilMat.setColor("Color", ColorRGBA.Gray);
                }
            }

        } else {

            anvilMat.setColor("Color", new ColorRGBA(0.15f, 0.15f, 0.15f, 0.9f));
        }

        anvilSquare.setMaterial(anvilMat);
        anvilSquare.setLocalTranslation(anvilX, anvilY, 1f);
        node.attachChild(anvilSquare);
        uiElements.add(anvilSquare);

        Label anvilLabel = new Label(getLocalized("blacksmith.anvil"));
        anvilLabel.setFontSize(20 * FONT_MULT);
        anvilLabel.setColor(ColorRGBA.White);
        anvilLabel.setLocalTranslation(anvilX, anvilY + anvilSize + 12f, 1f);
        node.attachChild(anvilLabel);
        uiElements.add(anvilLabel);

        // ============================================================
        // СОКЕТЫ ВЫБРАННОГО ПРЕДМЕТА
        // ============================================================

        if (anvilItemId != null && !anvilSockets.isEmpty()) {

            float socketSize = 50f;
            float socketSpacing = 8f;

            float socketStartX = anvilX;
            float socketStartY = anvilY - socketSize - 20f;

            for (int i = 0; i < anvilSockets.size(); i++) {

                Map<String, Object> socketData = anvilSockets.get(i);

                float sx = socketStartX + i * (socketSize + socketSpacing);
                float sy = socketStartY;

                Geometry socketGeom = new Geometry("Socket_" + i, new Quad(socketSize, socketSize));
                Material socketMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");

                Object gemObj = socketData.get("gem");
                boolean occupied = gemObj instanceof Map;

                if (occupied) {

                    Item gemItem = Item.fromMap((Map<String, Object>) gemObj);
                    Texture tex = tryLoadTexture(gemItem != null ? gemItem.getIconPath() : null);

                    if (tex != null) {
                        socketMat.setTexture("ColorMap", tex);
                    } else {
                        socketMat.setColor("Color", ColorRGBA.Yellow);
                    }

                } else {

                    socketMat.setColor("Color", new ColorRGBA(0.3f, 0.3f, 0.35f, 0.9f));
                }

                socketGeom.setMaterial(socketMat);
                socketGeom.setLocalTranslation(sx, sy, 1f);
                node.attachChild(socketGeom);
                uiElements.add(socketGeom);

                SocketCell cell = new SocketCell();
                cell.index = i;
                cell.x = sx;
                cell.y = sy;
                cell.size = socketSize;
                cell.occupied = occupied;
                socketCells.add(cell);

                final int socketIndex = i;

                MouseEventControl.addListenersToSpatial(socketGeom, new MouseListener() {
                    @Override
                    public void mouseButtonEvent(MouseButtonEvent evt, Spatial spatial, Spatial target) {
                        if (!visible) return;
                        if (evt.isPressed() && evt.getButtonIndex() == 0 && cell.occupied) {
                            removeGemFromSocket(socketIndex);
                        }
                    }
                    @Override public void mouseEntered(MouseMotionEvent evt, Spatial s, Spatial t) {}
                    @Override public void mouseExited(MouseMotionEvent evt, Spatial s, Spatial t) {}
                    @Override public void mouseMoved(MouseMotionEvent evt, Spatial s, Spatial t) {}
                });
            }
        }

        // ============================================================
        // ПРЕДМЕТЫ С СОКЕТАМИ (справа сверху)
        // ============================================================

        float rightX = winX + 280f;
        float rightTopY = winY + winH - 60f;

        float cellSize = 60f;
        float spacing = 10f;

        for (int i = 0; i < socketableItems.size(); i++) {

            Map<String, Object> entry = socketableItems.get(i);
            Item item = Item.fromMap((Map<String, Object>) entry.get("item"));
            if (item == null) continue;

            int col = i % GRID_COLS;
            int row = i / GRID_COLS;

            float x = rightX + col * (cellSize + spacing);
            float y = rightTopY - cellSize - row * (cellSize + spacing);

            Geometry cell = createIconCell("BlItem_" + i, item.getIconPath(), x, y, cellSize, item.getFallbackColor());
            node.attachChild(cell);
            uiElements.add(cell);

            attachDragSource(cell, DragType.ITEM, item.getId());
        }

        // ============================================================
        // КАМНИ (справа снизу)
        // ============================================================

        float gemsTopY = rightTopY - 4 * (cellSize + spacing) - 60f;

        for (int i = 0; i < gems.size(); i++) {

            Map<String, Object> entry = gems.get(i);
            Item gem = Item.fromMap((Map<String, Object>) entry.get("item"));
            if (gem == null) continue;

            int col = i % GRID_COLS;
            int row = i / GRID_COLS;

            float x = rightX + col * (cellSize + spacing);
            float y = gemsTopY - cellSize - row * (cellSize + spacing);

            Geometry cell = createIconCell("BlGem_" + i, gem.getIconPath(), x, y, cellSize, gem.getFallbackColor());
            node.attachChild(cell);
            uiElements.add(cell);

            attachDragSource(cell, DragType.GEM, gem.getId());
        }

        // ============================================================
        // CLOSE  (был «X» — теперь текстовая кнопка «ЗАКРЫТЬ»)
        //
        // Раньше висела на addClickCommands — на Android это UP-событие,
        // которое при тапе теряется. Теперь — через bindTouchAction,
        // реагирующий на DOWN.
        // ============================================================

        Button closeBtn = new Button(getLocalized("ui.close_button"));
        closeBtn.setPreferredSize(new Vector3f(200f, 50f, 0));
        closeBtn.setFontSize(22f);
        closeBtn.setColor(ColorRGBA.White);
        closeBtn.setLocalTranslation(
                winX + winW - 220f,
                winY + winH - 60f,
                1f);
        bindTouchAction(closeBtn, () -> hide());
        node.attachChild(closeBtn);
        uiElements.add(closeBtn);
    }

    private Geometry createIconCell(String name, String iconPath, float x, float y, float size, ColorRGBA fallback) {

        Geometry cell = new Geometry(name, new Quad(size, size));
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");

        Texture tex = tryLoadTexture(iconPath);

        if (tex != null) {
            mat.setTexture("ColorMap", tex);
        } else {
            mat.setColor("Color", fallback != null ? fallback : ColorRGBA.Gray);
        }

        cell.setMaterial(mat);
        cell.setLocalTranslation(x, y, 1f);

        return cell;
    }

    private Texture tryLoadTexture(String path) {

        if (path == null || path.isEmpty()) return null;

        try {
            return app.getAssetManager().loadTexture(path);
        } catch (Exception e) {
            return null;
        }
    }

    // ============================================================
    // DRAG SOURCES
    // ============================================================

    private void attachDragSource(Spatial spatial, DragType type, String id) {

        MouseEventControl.addListenersToSpatial(spatial, new MouseListener() {

            @Override
            public void mouseButtonEvent(MouseButtonEvent evt, Spatial s, Spatial target) {

                if (!visible) return;
                if (evt.getButtonIndex() != 0) return;

                if (evt.isPressed()) {

                    dragType = type;
                    dragItemId = id;
                    isDragging = false;
                    dragStartX = evt.getX();
                    dragStartY = evt.getY();

                    lastMouseX = evt.getX();
                    lastMouseY = evt.getY();
                }
            }

            @Override public void mouseEntered(MouseMotionEvent evt, Spatial s, Spatial t) {}
            @Override public void mouseExited(MouseMotionEvent evt, Spatial s, Spatial t) {}
            @Override public void mouseMoved(MouseMotionEvent evt, Spatial s, Spatial t) {}
        });
    }

    private void setupDragListener() {

        dragInputListener = new RawInputListener() {

            @Override
            public void onMouseButtonEvent(MouseButtonEvent evt) {

                if (!visible) return;
                if (evt.getButtonIndex() != 0) return;

                if (!evt.isPressed()) {

                    if (dragType != DragType.NONE) {

                        boolean wasDragging = isDragging;
                        float mx = evt.getX();
                        float my = evt.getY();

                        DragType type = dragType;
                        String id = dragItemId;

                        removeDragCursor();
                        dragType = DragType.NONE;
                        dragItemId = null;
                        isDragging = false;

                        if (wasDragging) {
                            handleDrop(type, id, mx, my);
                        }
                    }
                }
            }

            @Override
            public void onMouseMotionEvent(MouseMotionEvent evt) {

                if (!visible) return;

                lastMouseX = evt.getX();
                lastMouseY = evt.getY();

                if (dragType == DragType.NONE) return;

                float dx = evt.getX() - dragStartX;
                float dy = evt.getY() - dragStartY;

                if (!isDragging
                        && (dx * dx + dy * dy)
                        >= DRAG_THRESHOLD * DRAG_THRESHOLD) {

                    isDragging = true;
                    createDragCursor(dragType, dragItemId);
                }

                if (isDragging) {
                    updateDragCursor(evt.getX(), evt.getY());
                }
            }

            @Override public void beginInput() {}
            @Override public void endInput() {}
            @Override public void onJoyAxisEvent(JoyAxisEvent evt) {}
            @Override public void onJoyButtonEvent(JoyButtonEvent evt) {}
            @Override public void onKeyEvent(KeyInputEvent evt) {}

            @Override
            public void onTouchEvent(TouchEvent evt) {

                /*
                 * Дублирует логику onMouseButtonEvent/onMouseMotionEvent
                 * выше, но через настоящие тач-события — на Android
                 * синтезированное mouse-событие "отпускания" не всегда
                 * надёжно долетает до RawInputListener, из-за чего
                 * handleDrop(...) не вызывался при отпускании пальца
                 * за пределами исходной ячейки (ни на наковальню,
                 * ни на сокет).
                 */

                if (!visible) return;

                switch (evt.getType()) {

                    case MOVE: {

                        lastMouseX = evt.getX();
                        lastMouseY = evt.getY();

                        if (dragType == DragType.NONE) return;

                        float dx = evt.getX() - dragStartX;
                        float dy = evt.getY() - dragStartY;

                        if (!isDragging
                                && (dx * dx + dy * dy)
                                >= DRAG_THRESHOLD * DRAG_THRESHOLD) {

                            isDragging = true;
                            createDragCursor(dragType, dragItemId);
                        }

                        if (isDragging) {
                            updateDragCursor(evt.getX(), evt.getY());
                        }

                        break;
                    }

                    case UP: {

                        if (dragType != DragType.NONE) {

                            boolean wasDragging = isDragging;

                            float touchX = evt.getX();
                            float touchY = evt.getY();

                            DragType type = dragType;
                            String id = dragItemId;

                            removeDragCursor();

                            dragType = DragType.NONE;
                            dragItemId = null;
                            isDragging = false;

                            if (wasDragging) {
                                handleDrop(type, id, touchX, touchY);
                            }
                        }

                        break;
                    }

                    default:
                        break;
                }
            }
        };

        app.getInputManager().addRawInputListener(dragInputListener);
    }

    private void handleDrop(DragType type, String id, float mouseX, float mouseY) {

        if (type == DragType.ITEM) {

            if (isInside(mouseX, mouseY, anvilX, anvilY, anvilSize, anvilSize)) {
                selectItemForSocketing(id);
            }

        } else if (type == DragType.GEM) {

            for (SocketCell cell : socketCells) {

                if (cell.occupied) continue;

                if (isInside(mouseX, mouseY, cell.x, cell.y, cell.size, cell.size)) {
                    insertGemIntoSocket(cell.index, id);
                    return;
                }
            }
        }
    }

    private boolean isInside(float px, float py, float x, float y, float w, float h) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    // ============================================================
    // ДЕЙСТВИЯ
    // ============================================================

    @SuppressWarnings("unchecked")
    private void selectItemForSocketing(String itemId) {

        Map<String, Object> entry = findSocketableById(itemId);
        if (entry == null) return;

        anvilItemId = itemId;

        Object socketsObj = entry.get("sockets");
        anvilSockets = new ArrayList<>();

        if (socketsObj instanceof List) {
            for (Object o : (List<?>) socketsObj) {
                if (o instanceof Map) {
                    anvilSockets.add((Map<String, Object>) o);
                }
            }
        }

        rebuildUI();
    }

    private void insertGemIntoSocket(int socketIndex, String gemItemId) {

        if (anvilItemId == null || networkManager == null) return;

        networkManager.insertGem(anvilItemId, socketIndex, gemItemId).thenAccept(response -> {

            app.enqueue(() -> {

                if (response != null) {

                    if (uiManager != null) {
                        uiManager.applyCharacterData(response);
                    }

                    refreshFromServer();

                } else if (uiManager != null) {

                    uiManager.showToast(getLocalized("blacksmith.error.insert"));
                }

                return null;
            });
        });
    }

    private void removeGemFromSocket(int socketIndex) {

        if (anvilItemId == null || networkManager == null) return;

        networkManager.removeGem(anvilItemId, socketIndex).thenAccept(response -> {

            app.enqueue(() -> {

                if (response != null) {

                    if (uiManager != null) {
                        uiManager.applyCharacterData(response);
                    }

                    refreshFromServer();

                } else if (uiManager != null) {

                    uiManager.showToast(getLocalized("blacksmith.error.remove"));
                }

                return null;
            });
        });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findSocketableById(String itemId) {

        for (Map<String, Object> entry : socketableItems) {

            Map<String, Object> itemMap = (Map<String, Object>) entry.get("item");
            if (itemMap == null) continue;

            Object id = itemMap.get("id");
            if (itemId.equals(id)) return entry;
        }

        return null;
    }

    // ============================================================
    // DRAG CURSOR
    // ============================================================

    @SuppressWarnings("unchecked")
    private void createDragCursor(DragType type, String id) {

        removeDragCursor();

        String iconPath = null;

        if (type == DragType.ITEM) {

            Map<String, Object> entry = findSocketableById(id);
            if (entry != null) {
                Item item = Item.fromMap((Map<String, Object>) entry.get("item"));
                if (item != null) iconPath = item.getIconPath();
            }

        } else if (type == DragType.GEM) {

            for (Map<String, Object> entry : gems) {
                Map<String, Object> itemMap = (Map<String, Object>) entry.get("item");
                if (itemMap != null && id.equals(itemMap.get("id"))) {
                    Item gem = Item.fromMap(itemMap);
                    if (gem != null) iconPath = gem.getIconPath();
                    break;
                }
            }
        }

        if (iconPath == null || iconPath.isEmpty()) return;

        try {

            dragCursorPicture = new Picture("BlacksmithDragCursor");
            Texture texture = app.getAssetManager().loadTexture(iconPath);
            dragCursorPicture.setTexture(app.getAssetManager(), (Texture2D) texture, true);
            dragCursorPicture.setWidth(DRAG_CURSOR_SIZE);
            dragCursorPicture.setHeight(DRAG_CURSOR_SIZE);
            dragCursorPicture.setLocalTranslation(
                    lastMouseX - DRAG_CURSOR_SIZE / 2f,
                    lastMouseY - DRAG_CURSOR_SIZE / 2f,
                    1000f
            );
            uiManager.getGuiNode().attachChild(dragCursorPicture);

        } catch (Exception e) {
            dragCursorPicture = null;
        }
    }

    @SuppressWarnings("unchecked")
    private void resyncAnvilSockets() {

        if (anvilItemId == null) {
            return;
        }

        Map<String, Object> entry = findSocketableById(anvilItemId);

        if (entry == null) {
            /*
             * Предмет больше не в списке "с пустыми сокетами" —
             * значит все его сокеты теперь заняты.
             * Убираем его с наковальни.
             */
            anvilItemId = null;
            anvilSockets = new ArrayList<>();
            return;
        }

        Object socketsObj = entry.get("sockets");
        List<Map<String, Object>> freshSockets = new ArrayList<>();

        if (socketsObj instanceof List) {
            for (Object o : (List<?>) socketsObj) {
                if (o instanceof Map) {
                    freshSockets.add((Map<String, Object>) o);
                }
            }
        }

        anvilSockets = freshSockets;
    }

    private void updateDragCursor(float x, float y) {

        lastMouseX = x;
        lastMouseY = y;

        if (dragCursorPicture == null) return;

        dragCursorPicture.setLocalTranslation(
                x - DRAG_CURSOR_SIZE / 2f,
                y - DRAG_CURSOR_SIZE / 2f,
                1000f
        );
    }

    private void removeDragCursor() {

        if (dragCursorPicture != null) {
            dragCursorPicture.removeFromParent();
            dragCursorPicture = null;
        }
    }

    // ============================================================
    // CLEANUP
    // ============================================================

    public void cleanup() {

        removeDragCursor();

        if (dragInputListener != null) {
            app.getInputManager().removeRawInputListener(dragInputListener);
            dragInputListener = null;
        }

        if (uiManager != null && uiManager.getGuiNode().hasChild(node)) {
            uiManager.getGuiNode().detachChild(node);
        }
    }

    private String getLocalized(String key) {
        return LocalizationManager.getInstance().get(key);
    }
}