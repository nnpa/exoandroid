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
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.event.MouseEventControl;
import com.simsilica.lemur.event.MouseListener;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.mygame.Main;
import com.mygame.items.Item;
import com.mygame.items.ItemGenerator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class InventoryManager {
    private static final float FONT_MULT = 2f;
    private static final float DRAG_THRESHOLD = 30f;
    private static final float DRAG_CURSOR_SIZE = 55f;
    
    private SimpleApplication app;
    private Node guiNode;
    private Node inventoryNode;
    private List<Spatial> uiElements = new ArrayList<>();
    private boolean isVisible = false;
    private Label tooltipLabel;
    private UIManager uiManager;
    private NetworkManager networkManager;
    
    private Item[] inventoryItems = new Item[20];
    private Item[] equipment = new Item[7];
    
    private float currentScreenWidth = 1280;
    private float currentScreenHeight = 720;
    private float scale = 1f;
    private boolean isProcessing = false;
    
    private int draggedInventorySlot = -1;
    private boolean isDraggingItem = false;
    private float dragStartX, dragStartY;
    private float inventoryWindowX, inventoryWindowY;
    private float inventoryWindowWidth, inventoryWindowHeight;
    private float[] cellX = new float[20];
    private float[] cellY = new float[20];
    private float[] cellSizeArr = new float[20];
    private Picture dragCursorPicture;
    private float lastMouseX, lastMouseY;
    
    // Tap-to-select logic для Android
    private int selectedInventorySlot = -1;
    
    private static final String[] EMPTY_SLOT_ICONS = {
        "Interface/Icons/empty_helmet.png",
        "Interface/Icons/empty_armor.png",
        "Interface/Icons/empty_weapon.png",
        "Interface/Icons/empty_shield.png",
        "Interface/Icons/empty_legs.png",
        "Interface/Icons/empty_boots.png",
        "Interface/Icons/empty_gloves.png"
    };

    public InventoryManager(SimpleApplication app, Node guiNode) {
        this.app = app;
        this.guiNode = guiNode;
        Main main = (Main) app;
        if (main != null) this.networkManager = main.getNetworkManager();
        
        inventoryNode = new Node("InventoryNode");
        inventoryNode.setName("InventoryNode");
        updateScreenSize();
        createUI(currentScreenWidth, currentScreenHeight);
        isVisible = false;
    }

    public void setUIManager(UIManager uiManager) { this.uiManager = uiManager; }
    public boolean isVisible() { return isVisible; }
    public Node getNode() { return inventoryNode; }
    private String getLocalized(String key) { return LocalizationManager.getInstance().get(key); }

    public boolean onTouchDown(float x, float y) {
        if (!isVisible) return false;
        for (int i = 0; i < 20; i++) {
            if (isInsideCell(x, y, i) && inventoryItems[i] != null) {
                draggedInventorySlot = i;
                dragStartX = x;
                dragStartY = y;
                isDraggingItem = false;
                return true;
            }
        }
        return false;
    }

    public boolean onTouchMove(float x, float y, boolean isDragging) {
        if (draggedInventorySlot < 0) return false;
        if (isDragging && !isDraggingItem) {
            isDraggingItem = true;
            Item item = inventoryItems[draggedInventorySlot];
            if (item != null) createDragCursor(item);
        }
        if (isDraggingItem) updateDragCursor(x, y);
        return true;
    }

    public boolean onTouchUp(float x, float y, boolean wasDragging) {
        if (draggedInventorySlot < 0) return false;
        int slot = draggedInventorySlot;
        boolean dragging = isDraggingItem;
        removeDragCursor();
        draggedInventorySlot = -1;
        isDraggingItem = false;
        
        if (dragging) {
            if (!isInsideInventoryWindow(x, y)) dropInventoryItem(slot);
        } else {
            handleSlotTap(slot);
        }
        return true;
    }

    private boolean isInsideCell(float x, float y, int idx) {
        if (idx < 0 || idx >= 20) return false;
        float cellX0 = cellX[idx];
        float cellY0 = cellY[idx];
        float cs = cellSizeArr[idx];
        return x >= cellX0 && x <= cellX0 + cs && y >= cellY0 && y <= cellY0 + cs;
    }

    // ============================================================
    // TAP-TO-SELECT LOGIC
    // ============================================================
    private void updateInventoryUI() { updateUI(); }

    private void equipItem(int slotIndex) {

        System.out.println("[InventoryManager] equipItem called, slot=" + slotIndex
                + " isVisible=" + isVisible
                + " isProcessing=" + isProcessing
                + " networkManager=" + (networkManager != null));

        if (!isVisible || isProcessing) return;
        if (slotIndex < 0 || slotIndex >= inventoryItems.length) return;
        Item item = inventoryItems[slotIndex];
        if (item == null || "Gem".equals(item.getType())) return;
        
        if (networkManager != null) {
            isProcessing = true;
            networkManager.equipItem(slotIndex).thenAccept(response -> {
                System.out.println("[InventoryManager] equipItem response: " + response);
                app.enqueue(() -> {
                    isProcessing = false;
                    if (response != null && uiManager != null) {
                        uiManager.applyCharacterData(response);
                        updateUI();
                        SoundManager.playSound(SoundManager.SOUND_CLICK);
                    } else {
                        requestInventoryRefresh();
                    }
                });
            }).exceptionally(ex -> {
                System.out.println("[InventoryManager] equipItem EXCEPTION: " + ex.getMessage());
                ex.printStackTrace();
                app.enqueue(() -> { isProcessing = false; requestInventoryRefresh(); });
                return null;
            });
        } else {
            System.out.println("[InventoryManager] equipItem: networkManager is NULL, cannot equip!");
        }
    }

    private void moveItem(int fromSlot, int toSlot) {
        if (!isVisible || isProcessing) return;
        if (fromSlot < 0 || fromSlot >= inventoryItems.length) return;
        if (toSlot < 0 || toSlot >= inventoryItems.length) return;
        if (fromSlot == toSlot) return;
        Item item = inventoryItems[fromSlot];
        if (item == null) return;
        inventoryItems[toSlot] = item;
        inventoryItems[fromSlot] = null;
        updateUI();
    }

    private void swapItems(int slot1, int slot2) {
        if (!isVisible || isProcessing) return;
        if (slot1 < 0 || slot1 >= inventoryItems.length) return;
        if (slot2 < 0 || slot2 >= inventoryItems.length) return;
        if (slot1 == slot2) return;
        Item item1 = inventoryItems[slot1];
        Item item2 = inventoryItems[slot2];
        inventoryItems[slot1] = item2;
        inventoryItems[slot2] = item1;
        updateUI();
    }

    private void dropItemFromSlot(int slotIndex) { dropInventoryItem(slotIndex); }

    private void handleSlotTap(int slotIndex) {
        System.out.println("[InventoryManager] handleSlotTap called, slot=" + slotIndex);

        if (!isVisible) return;
        Item clickedItem = getItemAtSlot(slotIndex);

        System.out.println("[InventoryManager] clickedItem=" + (clickedItem != null ? clickedItem.getName() : "null"));

        // Один тап по предмету — сразу одевание, без промежуточного
        // выделения жёлтым и без второго подтверждающего тапа.
        if (clickedItem != null) {
            equipItem(slotIndex);
        }
    }

    public void dropSelectedItem() {
        if (selectedInventorySlot != -1) {
            dropItemFromSlot(selectedInventorySlot);
            selectedInventorySlot = -1;
            updateInventoryUI();
        }
    }

    // ============================================================
    // DATA & UI
    // ============================================================
    public void loadFromServerData(List<Map<String, Object>> inventoryData) {
        Arrays.fill(inventoryItems, null);
        Arrays.fill(equipment, null);
        if (inventoryData == null || inventoryData.isEmpty()) { updateUI(); return; }
        
        for (Map<String, Object> data : inventoryData) {
            try {
                int slot = -1;
                Object slotObj = data.get("slot");
                if (slotObj instanceof Number) slot = ((Number) slotObj).intValue();
                else if (slotObj instanceof String) {
                    try { slot = Integer.parseInt((String) slotObj); } catch (NumberFormatException ignored) {}
                }
                
                boolean equipped = false;
                Object eqObj = data.get("equipped");
                if (eqObj instanceof Boolean) equipped = (Boolean) eqObj;
                else if (eqObj instanceof String) equipped = Boolean.parseBoolean((String) eqObj);
                else if (eqObj instanceof Number) equipped = ((Number) eqObj).intValue() != 0;
                
                String equippedSlot = null;
                Object eqSlotObj = data.get("equipped_slot");
                if (eqSlotObj == null) eqSlotObj = data.get("equippedSlot");
                if (eqSlotObj != null) equippedSlot = eqSlotObj.toString();
                
                Object itemMapObj = data.get("item");
                if (!(itemMapObj instanceof Map)) continue;
                
                @SuppressWarnings("unchecked")
                Map<String, Object> itemMap = (Map<String, Object>) itemMapObj;
                Item item = Item.fromMap(itemMap);
                if (item == null) continue;
                
                if (equipped) {
                    int equipIndex = getEquipIndexBySlot(equippedSlot);
                    if (equipIndex != -1 && equipIndex < equipment.length) equipment[equipIndex] = item;
                } else {
                    if (slot >= 0 && slot < inventoryItems.length) inventoryItems[slot] = item;
                }
            } catch (Exception e) {
                System.err.println("[InventoryManager] Cannot process item: " + e.getMessage());
            }
        }
        updateUI();
    }

    private int getEquipIndexBySlot(String slot) {
        if (slot == null) return -1;
        switch (slot.toLowerCase().trim()) {
            case "helmet": return 0; case "chest": return 1; case "weapon": return 2;
            case "shield": return 3; case "legs": return 4; case "boots": return 5;
            case "gloves": return 6; default: return -1;
        }
    }

    public void addItem(Item item) {
        if (item == null) return;
        for (int i = 0; i < inventoryItems.length; i++) {
            if (inventoryItems[i] == null) { inventoryItems[i] = item; updateUI(); return; }
        }
    }

    public void removeItem(Item item) {
        if (item == null) return;
        for (int i = 0; i < inventoryItems.length; i++) {
            if (inventoryItems[i] == item) { inventoryItems[i] = null; updateUI(); return; }
        }
    }

    public List<Item> getItems() {
        List<Item> result = new ArrayList<>();
        for (Item it : inventoryItems) if (it != null) result.add(it);
        return result;
    }

    public int getItemIndex(Item item) {
        if (item == null) return -1;
        for (int i = 0; i < inventoryItems.length; i++) if (inventoryItems[i] == item) return i;
        return -1;
    }

    public int getSlotIndex(Item item) { return getItemIndex(item); }
    public Item getItemAtSlot(int slot) { return (slot < 0 || slot >= inventoryItems.length) ? null : inventoryItems[slot]; }
    public boolean isFull() { for (Item item : inventoryItems) if (item == null) return false; return true; }

    private void updateScreenSize() {
        float w = app.getCamera().getWidth();
        float h = app.getCamera().getHeight();
        if (w > 0 && h > 0) { currentScreenWidth = w; currentScreenHeight = h; }
        float scaleX = currentScreenWidth / 800f;
        float scaleY = currentScreenHeight / 600f;
        scale = Math.max(0.5f, Math.min(Math.min(scaleX, scaleY), 4.0f));
    }

    public void updateLayout(int screenWidth, int screenHeight) {
        currentScreenWidth = screenWidth;
        currentScreenHeight = screenHeight;
        if (isVisible) { clearUI(); createUI(screenWidth, screenHeight); setVisible(true); }
    }

    private void clearUI() { inventoryNode.detachAllChildren(); uiElements.clear(); }

    private void createUI(float screenWidth, float screenHeight) {
        inventoryNode.detachAllChildren();
        uiElements.clear();
        
        float eqWidth = 250 * scale, eqHeight = 450 * scale;
        float invWidth = 400 * scale, invHeight = 450 * scale;
        inventoryWindowWidth = invWidth;
        inventoryWindowHeight = invHeight;
        
        float spacing = 30 * scale;
        float totalWidth = eqWidth + spacing + invWidth;
        float startX = (screenWidth - totalWidth) / 2;
        float startY = (screenHeight - (eqHeight + invHeight) / 2) / 2 - 100 * scale;
        float slotSize = 60 * scale;
        float shiftDown = slotSize;
        
        float eqX = startX, eqY = startY + 100 * scale;
        float invX = startX + eqWidth + spacing, invY = startY + 50 * scale;
        inventoryWindowX = invX;
        inventoryWindowY = invY;
        
        if (uiManager != null) {
            Geometry eqBg = uiManager.createBackgroundGeometry(eqWidth, eqHeight);
            eqBg.setLocalTranslation(eqX, eqY, -1f);
            eqBg.setUserData("pickable", false);
            inventoryNode.attachChild(eqBg);
            uiElements.add(eqBg);
            
            Geometry invBg = uiManager.createBackgroundGeometry(invWidth, invHeight);
            invBg.setLocalTranslation(invX, invY, -1f);
            invBg.setUserData("pickable", false);
            inventoryNode.attachChild(invBg);
            uiElements.add(invBg);
        }
        
        float offsetY = 70 * scale;
        createSlotGeometry(eqX + 95 * scale, eqY + 320 * scale + offsetY - shiftDown, 0, slotSize);
        createSlotGeometry(eqX + 160 * scale, eqY + 250 * scale + offsetY - shiftDown, 2, slotSize);
        createSlotGeometry(eqX + 30 * scale, eqY + 250 * scale + offsetY - shiftDown, 3, slotSize);
        createSlotGeometry(eqX + 95 * scale, eqY + 180 * scale + offsetY - shiftDown, 1, slotSize);
        createSlotGeometry(eqX + 95 * scale, eqY + 110 * scale + offsetY - shiftDown, 4, slotSize);
        createSlotGeometry(eqX + 95 * scale, eqY + 40 * scale + offsetY - shiftDown, 5, slotSize);
        createSlotGeometry(eqX + 30 * scale, eqY + 110 * scale + offsetY - shiftDown, 6, slotSize);
        
        Button closeEq = new Button(getLocalized("ui.close_button"));
        closeEq.setPreferredSize(new Vector3f(180 * scale, 50 * scale, 0));
        closeEq.setFontSize(20 * scale);
        closeEq.setColor(ColorRGBA.White);
        closeEq.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.3f, 0.3f, 0.3f, 0.8f)));
        closeEq.setLocalTranslation(eqX + eqWidth - 200 * scale, eqY + eqHeight - 60 * scale - shiftDown, 0);
        closeEq.addClickCommands(s -> hide());
        inventoryNode.attachChild(closeEq);
        uiElements.add(closeEq);
        
        float cellSize = 55 * scale;
        float spacingCell = 8 * scale;
        float paddingLeft = 40 * scale, paddingTop = 40 * scale;
        float startXCell = invX + paddingLeft;
        float startYCell = invY + invHeight - paddingTop - 50 * scale;
        
        for (int i = 0; i < 20; i++) {
            int col = i % 4;
            int row = i / 4;
            float x = startXCell + col * (cellSize + spacingCell);
            float y = startYCell - row * (cellSize + spacingCell);
            cellX[i] = x;
            cellY[i] = y;
            cellSizeArr[i] = cellSize;
            
            Geometry cell = new Geometry("InvCell_" + i, new Quad(cellSize, cellSize));
            cell.setLocalTranslation(x, y, 1f);
            
            Material cellMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            Item item = inventoryItems[i];
            
            if (item != null) {
                Texture tex = null;
                try { tex = app.getAssetManager().loadTexture(item.getIconPath()); } catch (Exception ignored) {}
                if (tex != null) cellMat.setTexture("ColorMap", tex);
                else cellMat.setColor("Color", item.getFallbackColor());
            } else {
                cellMat.setColor("Color", new ColorRGBA(0.2f, 0.2f, 0.3f, 0.9f));
            }
            cell.setMaterial(cellMat);
            
            if (i == selectedInventorySlot) {
                Geometry highlight = new Geometry("InvCellHighlight_" + i, new Quad(cellSize + 4, cellSize + 4));
                highlight.setLocalTranslation(x - 2, y - 2, 0.9f);
                Material highlightMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
                highlightMat.setColor("Color", new ColorRGBA(1f, 1f, 0f, 0.8f));
                highlight.setMaterial(highlightMat);
                inventoryNode.attachChild(highlight);
                uiElements.add(highlight);
            }
            
            final int slotIdx = i;
            MouseEventControl.addListenersToSpatial(cell, new MouseListener() {
                @Override public void mouseButtonEvent(MouseButtonEvent evt, Spatial s, Spatial t) {
                    if (!isVisible || evt.getButtonIndex() != 0 || !evt.isPressed()) return;
                    evt.setConsumed();
                    handleSlotTap(slotIdx);
                }
                @Override public void mouseEntered(MouseMotionEvent evt, Spatial s, Spatial t) {}
                @Override public void mouseExited(MouseMotionEvent evt, Spatial s, Spatial t) {}
                @Override public void mouseMoved(MouseMotionEvent evt, Spatial s, Spatial t) {}
            });
            
            addTooltipListener(cell, i, true);
            inventoryNode.attachChild(cell);
            uiElements.add(cell);
        }
        
        Button closeInv = new Button(getLocalized("ui.close_button"));
        closeInv.setPreferredSize(new Vector3f(180 * scale, 50 * scale, 0));
        closeInv.setFontSize(20 * scale);
        closeInv.setColor(ColorRGBA.White);
        closeInv.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.3f, 0.3f, 0.3f, 0.8f)));
        closeInv.setLocalTranslation(invX + invWidth - 200 * scale, invY + invHeight - 60 * scale, 0);
        closeInv.addClickCommands(s -> hide());
        inventoryNode.attachChild(closeInv);
        uiElements.add(closeInv);
        
        tooltipLabel = new Label("");
        tooltipLabel.setFontSize(14 * scale * FONT_MULT);
        tooltipLabel.setColor(ColorRGBA.White);
        tooltipLabel.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.1f, 0.1f, 0.2f, 0.95f)));
        tooltipLabel.setPreferredSize(new Vector3f(360 * scale, 160 * scale, 1f));
        tooltipLabel.setLocalTranslation(10 * scale, 200 * scale, 10f);
        tooltipLabel.setCullHint(Node.CullHint.Always);
        inventoryNode.attachChild(tooltipLabel);
        uiElements.add(tooltipLabel);
    }

    private void createSlotGeometry(float x, float y, int slotIndex, float slotSize) {
        Quad quad = new Quad(slotSize, slotSize);
        Geometry geo = new Geometry("slotGeo_" + slotIndex, quad);
        geo.setLocalTranslation(x, y, 1f);
        
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        if (equipment[slotIndex] != null) {
            Item item = equipment[slotIndex];
            Texture tex = null;
            try { tex = app.getAssetManager().loadTexture(item.getIconPath()); } catch (Exception ignored) {}
            if (tex != null) mat.setTexture("ColorMap", tex);
            else mat.setColor("Color", item.getFallbackColor());
        } else {
            Texture emptyTex = null;
            try { emptyTex = app.getAssetManager().loadTexture(EMPTY_SLOT_ICONS[slotIndex]); } catch (Exception ignored) {}
            if (emptyTex != null) mat.setTexture("ColorMap", emptyTex);
            else mat.setColor("Color", new ColorRGBA(0.2f, 0.2f, 0.3f, 0.9f));
        }
        geo.setMaterial(mat);
        
        final int idx = slotIndex;
        MouseEventControl.addListenersToSpatial(geo, new MouseListener() {
            @Override public void mouseButtonEvent(MouseButtonEvent evt, Spatial s, Spatial t) {
                if (!isVisible || !evt.isPressed() || evt.getButtonIndex() != 0) return;
                evt.setConsumed();
                handleEquipmentClick(idx);
            }
            @Override public void mouseEntered(MouseMotionEvent evt, Spatial s, Spatial t) {}
            @Override public void mouseExited(MouseMotionEvent evt, Spatial s, Spatial t) {}
            @Override public void mouseMoved(MouseMotionEvent evt, Spatial s, Spatial t) {}
        });
        addTooltipListener(geo, idx, false);
        inventoryNode.attachChild(geo);
        uiElements.add(geo);
    }

    private void addTooltipListener(Spatial target, int index, boolean isInventory) {
        MouseEventControl.removeListenersFromSpatial(target);
        MouseListener listener = new MouseListener() {
            @Override public void mouseButtonEvent(MouseButtonEvent evt, Spatial s, Spatial t) {}
            @Override public void mouseEntered(MouseMotionEvent evt, Spatial s, Spatial t) {
                if (!isVisible) return;
                String text = "";
                Item hoveredItem = null;
                if (isInventory && index < inventoryItems.length && inventoryItems[index] != null) {
                    hoveredItem = inventoryItems[index];
                    text = buildTooltip(hoveredItem);
                } else if (!isInventory && equipment[index] != null) {
                    hoveredItem = equipment[index];
                    text = buildTooltip(hoveredItem);
                }
                if (!text.isEmpty() && tooltipLabel != null) {
                    tooltipLabel.setText(text);
                    tooltipLabel.setColor(hoveredItem != null ? hoveredItem.getColor() : ColorRGBA.White);
                    tooltipLabel.setCullHint(Node.CullHint.Dynamic);
                }
            }
            @Override public void mouseExited(MouseMotionEvent evt, Spatial s, Spatial t) {
                if (tooltipLabel != null) tooltipLabel.setCullHint(Node.CullHint.Always);
            }
            @Override public void mouseMoved(MouseMotionEvent evt, Spatial s, Spatial t) {}
        };
        MouseEventControl.addListenersToSpatial(target, listener);
    }

    private String buildTooltip(Item item) {
        if (item == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(item.getName()).append("\n");
        sb.append(getLocalized("stat.level")).append(": ").append(item.getLevel()).append("\n");
        sb.append(getLocalized("stat.rarity")).append(": ").append(getLocalized("rarity." + item.getRarity().name().toLowerCase())).append("\n");
        if (item.getDamage() > 0) sb.append(getLocalized("stat.damage")).append(": +").append(item.getDamage()).append("\n");
        if (item.getDefense() > 0) sb.append(getLocalized("stat.defense")).append(": +").append(item.getDefense()).append("\n");
        if (item.getHealthBonus() > 0) sb.append(getLocalized("stat.health")).append(": +").append(item.getHealthBonus()).append("\n");
        if (item.getManaBonus() > 0) sb.append(getLocalized("stat.mana")).append(": +").append(item.getManaBonus()).append("\n");
        sb.append(getLocalized("stat.type")).append(": ").append(item.getLocalizedType());
        return sb.toString();
    }

    private void createDragCursor(Item item) {
        removeDragCursor();
        if (item == null) return;
        String iconPath = item.getIconPath();
        if (iconPath == null || iconPath.isEmpty()) return;
        try {
            dragCursorPicture = new Picture("DraggedItemCursor");
            Texture texture = app.getAssetManager().loadTexture(iconPath);
            dragCursorPicture.setTexture(app.getAssetManager(), (Texture2D) texture, true);
            float size = DRAG_CURSOR_SIZE * scale;
            dragCursorPicture.setWidth(size);
            dragCursorPicture.setHeight(size);
            dragCursorPicture.setLocalTranslation(lastMouseX - size / 2f, lastMouseY - size / 2f, 1000f);
            guiNode.attachChild(dragCursorPicture);
        } catch (Exception e) { dragCursorPicture = null; }
    }

    private void removeDragCursor() {
        if (dragCursorPicture != null) { dragCursorPicture.removeFromParent(); dragCursorPicture = null; }
    }

    private void updateDragCursor(float mouseX, float mouseY) {
        lastMouseX = mouseX; lastMouseY = mouseY;
        if (dragCursorPicture == null) return;
        float size = DRAG_CURSOR_SIZE * scale;
        dragCursorPicture.setLocalTranslation(mouseX - size / 2f, mouseY - size / 2f, 1000f);
    }

    private boolean isInsideInventoryWindow(float mouseX, float mouseY) {
        return mouseX >= inventoryWindowX && mouseX <= inventoryWindowX + inventoryWindowWidth
            && mouseY >= inventoryWindowY && mouseY <= inventoryWindowY + inventoryWindowHeight;
    }

    private void dropInventoryItem(int slotIndex) {
        if (!isVisible || slotIndex < 0 || slotIndex >= inventoryItems.length) return;
        Item item = inventoryItems[slotIndex];
        if (item == null || isProcessing) return;
        
        if (networkManager == null) {
            inventoryItems[slotIndex] = null;
            updateUI();
            return;
        }
        isProcessing = true;
        networkManager.dropItem(slotIndex).thenAccept(response -> {
            app.enqueue(() -> {
                isProcessing = false;
                if (response != null && !response.containsKey("error")) {
                    if (uiManager != null) uiManager.applyCharacterData(response);
                    updateUI();
                    SoundManager.playSound(SoundManager.SOUND_CLICK);
                } else {
                    requestInventoryRefresh();
                }
            });
        }).exceptionally(ex -> {
            app.enqueue(() -> { isProcessing = false; requestInventoryRefresh(); });
            return null;
        });
    }

    private void handleEquipmentClick(int slotIndex) {
        if (!isVisible || isProcessing) return;
        if (slotIndex < 0 || slotIndex >= equipment.length) return;
        if (equipment[slotIndex] == null) return;
        String slotName = getSlotName(slotIndex);
        if (slotName == null) return;
        
        if (networkManager != null) {
            isProcessing = true;
            networkManager.unequipItem(slotName).thenAccept(response -> {
                app.enqueue(() -> {
                    isProcessing = false;
                    if (response != null && !response.containsKey("error")) {
                        uiManager.applyCharacterData(response);
                        updateUI();
                        SoundManager.playSound(SoundManager.SOUND_UNEQUIP);
                    } else {
                        requestInventoryRefresh();
                    }
                });
            }).exceptionally(ex -> {
                app.enqueue(() -> { isProcessing = false; requestInventoryRefresh(); });
                return null;
            });
        }
    }

    private String getSlotName(int slotIndex) {
        switch (slotIndex) {
            case 0: return "helmet"; case 1: return "chest"; case 2: return "weapon";
            case 3: return "shield"; case 4: return "legs"; case 5: return "boots";
            case 6: return "gloves"; default: return null;
        }
    }

    public void requestInventoryRefresh() {
        if (networkManager != null && !isProcessing) {
            isProcessing = true;
            networkManager.loadCharacterData().thenAccept(data -> {
                app.enqueue(() -> {
                    isProcessing = false;
                    if (data != null && uiManager != null) {
                        uiManager.applyCharacterData(data);
                        updateUI();
                    }
                });
            }).exceptionally(ex -> {
                app.enqueue(() -> { isProcessing = false; });
                return null;
            });
        }
    }

    private void setVisible(boolean visible) {
        isVisible = visible;
        if (!visible) { draggedInventorySlot = -1; isDraggingItem = false; selectedInventorySlot = -1; }
        inventoryNode.setLocalScale(1f, 1f, 1f);
        inventoryNode.setLocalTranslation(0, 0, 0);
        inventoryNode.setCullHint(visible ? Node.CullHint.Dynamic : Node.CullHint.Always);
        
        if (visible) {
            if (guiNode.hasChild(inventoryNode)) guiNode.detachChild(inventoryNode);
            guiNode.attachChild(inventoryNode);
            if (uiManager != null) uiManager.onInventoryOpened(inventoryNode);
        } else {
            if (guiNode.hasChild(inventoryNode)) guiNode.detachChild(inventoryNode);
            if (uiManager != null) uiManager.onInventoryClosed(inventoryNode);
        }
        for (Spatial s : uiElements) s.setCullHint(visible ? Node.CullHint.Dynamic : Node.CullHint.Always);
        if (!visible && tooltipLabel != null) tooltipLabel.setCullHint(Node.CullHint.Always);
    }

public void show() {
    setVisible(true);
    SoundManager.playSound(SoundManager.SOUND_WINDOW_TALENTS);
    requestInventoryRefresh();
}   
    public void hide() { removeDragCursor(); draggedInventorySlot = -1; isDraggingItem = false; selectedInventorySlot = -1; SoundManager.playSound(SoundManager.SOUND_WINDOW_CLOSE); setVisible(false); }
    public void toggleVisibility() { if (isVisible) hide(); else show(); }

    public void updateUI() {
        clearUI();
        updateScreenSize();
        createUI(currentScreenWidth, currentScreenHeight);
        if (isVisible) setVisible(true);
        if (uiManager != null && uiManager.getPlayerManager() != null) {
            List<Item> equippedList = new ArrayList<>();
            for (Item item : equipment) if (item != null) equippedList.add(item);
            uiManager.getPlayerManager().recalculateEquipmentBonuses(equippedList);
            if (uiManager.getCharacterStatsWindow() != null && uiManager.getCharacterStatsWindow().isVisible()) {
                uiManager.getCharacterStatsWindow().refreshValues();
            }
        }
    }

    public void cleanup() {
        removeDragCursor();
        draggedInventorySlot = -1;
        isDraggingItem = false;
        selectedInventorySlot = -1;
        if (guiNode.hasChild(inventoryNode)) guiNode.detachChild(inventoryNode);
    }
}