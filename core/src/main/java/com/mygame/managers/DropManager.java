package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.mygame.Main;
import com.mygame.items.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DropManager {

    private SimpleApplication app;
    private Node dropNode;
    private List<DropItem> drops = new ArrayList<>();

    private InventoryManager inventoryManager;
    private NetworkManager networkManager;
    private UIManager uiManager;

    // ============================================================
    // РАЗМЕРЫ
    // ============================================================

    private static final float ICON_SIZE = 0.8f;
    private static final float BORDER_SIZE = 0.06f;
    private static final float TEXT_OFFSET = 1.4f;
    private static final float PIXEL_THRESHOLD = 80f;

    // ============================================================
    // ВРЕМЯ ЖИЗНИ ПРЕДМЕТА
    // ============================================================

    /**
     * Через сколько секунд выпавший предмет исчезает с земли.
     */
    private static final float DROP_LIFETIME = 10.0f;

    public DropManager(SimpleApplication app, Node guiNode) {

        this.app = app;

        dropNode = new Node("DropNode");

        app.getRootNode().attachChild(dropNode);

        // Получаем NetworkManager из Main
        Main main = (Main) app;

        if (main != null) {
            this.networkManager = main.getNetworkManager();
        }
    }

    // ============================================================
    // UPDATE
    // ============================================================

    /**
     * Вызывать каждый кадр из update().
     *
     * Например:
     *
     * dropManager.update(tpf);
     */
    public void update(float tpf) {

        if (drops.isEmpty()) {
            return;
        }

        /*
         * Используем отдельный список для удаления,
         * чтобы не изменять drops прямо во время foreach.
         */
        List<DropItem> expiredDrops = new ArrayList<>();

        for (DropItem drop : drops) {

            if (drop == null) {
                continue;
            }

            // Уменьшаем оставшееся время жизни
            drop.lifeTime -= tpf;

            // ========================================================
            // ВРЕМЯ ВЫШЛО
            // ========================================================

            if (drop.lifeTime <= 0f) {

                expiredDrops.add(drop);
            }
        }

        // ============================================================
        // УДАЛЯЕМ ПРОСРОЧЕННЫЕ ПРЕДМЕТЫ
        // ============================================================

        for (DropItem drop : expiredDrops) {

            removeExpiredDrop(drop);
        }
    }

    // ============================================================
    // УДАЛЕНИЕ ПРОСРОЧЕННОГО DROP
    // ============================================================

    private void removeExpiredDrop(DropItem drop) {

        if (drop == null) {
            return;
        }

        if (!drops.contains(drop)) {
            return;
        }

        String itemName =
                drop.item != null
                        ? drop.item.getName()
                        : "Unknown";

        // Удаляем Node со сцены
        if (drop.node != null) {

            dropNode.detachChild(
                    drop.node
            );
        }

        // Удаляем из списка
        drops.remove(drop);

        System.out.println(
                "[DropManager] Drop expired after "
                        + DROP_LIFETIME
                        + " seconds: "
                        + itemName
        );
    }

    // ============================================================
    // SET INVENTORY
    // ============================================================

    public void setInventoryManager(
            InventoryManager im) {

        this.inventoryManager = im;
    }

    // ============================================================
    // SET UI
    // ============================================================

    public void setUIManager(UIManager ui) {

        this.uiManager = ui;
    }

    // ============================================================
    // SPAWN DROPS
    // ============================================================

    public void spawnDrops(
            Vector3f center,
            List<Item> items) {

        if (items == null || items.isEmpty()) {
            return;
        }

        int count = items.size();

        float radius = 1.5f;

        for (int i = 0; i < count; i++) {

            float angle =
                    (float) (
                            i * 2 * Math.PI / count
                                    + Math.random() * 0.5
                    );

            float offsetX =
                    (float) (
                            radius * Math.cos(angle)
                                    + (Math.random() - 0.5) * 0.5
                    );

            float offsetZ =
                    (float) (
                            radius * Math.sin(angle)
                                    + (Math.random() - 0.5) * 0.5
                    );

            Vector3f pos =
                    new Vector3f(
                            center.x + offsetX,
                            0,
                            center.z + offsetZ
                    );

            spawnDrop(
                    pos,
                    items.get(i)
            );
        }
    }

    // ============================================================
    // SPAWN DROP
    // ============================================================

    public void spawnDrop(
            Vector3f position,
            Item item) {

        if (item == null) {
            return;
        }

        DropItem drop =
                new DropItem(
                        position,
                        item
                );

        dropNode.attachChild(
                drop.node
        );

        drops.add(drop);

        System.out.println(
                "[DropManager] Drop spawned: "
                        + item.getName()
                        + " | lifetime="
                        + DROP_LIFETIME
                        + " sec"
        );
    }

    // ============================================================
    // GET DROP AT SCREEN
    // ============================================================

public DropItem getDropAt(float screenX, float screenY) {

    Camera cam = app.getCamera();
    float threshold = getPixelThreshold();
    float thresholdSq = threshold * threshold;

    DropItem best = null;
    float bestDistSq = Float.MAX_VALUE;

    for (DropItem drop : drops) {

        if (drop == null || drop.node == null) {
            continue;
        }

        /*
         * ВАЖНО: берём мировую позицию САМОЙ иконки
         * (iconGeom), а не drop.node — потому что иконка
         * смещена вверх относительно origin узла на iconYOffset.
         * На Windows это компенсировалось большим порогом,
         * на Android — нет.
         */
        Vector3f worldPos = drop.getIconWorldPosition();

        if (worldPos == null) {
            continue;
        }

        Vector3f screenPos = cam.getScreenCoordinates(worldPos);

        if (screenPos == null) {
            continue;
        }

        float dx = screenPos.x - screenX;
        float dy = screenPos.y - screenY;
        float distSq = dx * dx + dy * dy;

        if (distSq < thresholdSq && distSq < bestDistSq) {
            bestDistSq = distSq;
            best = drop;
        }
    }

    return best;
}

    // ============================================================
    // PICKUP ITEM
    // ============================================================

    public void pickupDrop(
            DropItem drop) {

        if (drop == null) {
            return;
        }

        // ========================================================
        // ПРОВЕРКА ИНВЕНТАРЯ
        // ========================================================

        if (inventoryManager != null
                && inventoryManager.isFull()) {

            System.out.println(
                    "[DropManager] Inventory is full!"
            );

            if (uiManager != null) {

                uiManager.showToast(
                        "Inventory is full!"
                );
            }

            return;
        }

        // ========================================================
        // OFFLINE
        // ========================================================

        if (networkManager == null) {

            if (inventoryManager != null) {

                inventoryManager.addItem(
                        drop.item
                );

                removeDrop(drop);

                System.out.println(
                        "[DropManager] "
                                + "(offline) Picked up: "
                                + drop.item.getName()
                );
            }

            return;
        }

        // ========================================================
        // ДАННЫЕ ПРЕДМЕТА
        // ========================================================

        Map<String, Object> itemData =
                new HashMap<>();

        itemData.put(
                "id",
                drop.item.getId()
        );

        itemData.put(
                "name",
                drop.item.getName()
        );

        itemData.put(
                "type",
                drop.item.getType()
        );

        itemData.put(
                "level",
                drop.item.getLevel()
        );

        itemData.put(
                "rarity",
                drop.item.getRarity().name()
        );

        itemData.put(
                "description",
                drop.item.getDescription()
        );

        itemData.put(
                "damage",
                drop.item.getDamage()
        );

        itemData.put(
                "defense",
                drop.item.getDefense()
        );

        itemData.put(
                "healthBonus",
                drop.item.getHealthBonus()
        );

        itemData.put(
                "manaBonus",
                drop.item.getManaBonus()
        );

        itemData.put(
                "iconPath",
                drop.item.getIconPath()
        );

        itemData.put(
                "socketCount",
                drop.item.getSocketCount()
        );

        itemData.put(
                "difficulty",
                drop.item.getDifficulty()
        );

        // ========================================================
        // PICKUP REQUEST
        // ========================================================

        networkManager
                .pickupItem(itemData)
                .thenAccept(response -> {

                    app.enqueue(() -> {

                        System.out.println(
                                "[DropManager] "
                                        + "Pickup response: "
                                        + response
                        );

                        if (response != null) {

                            // ====================================
                            // ОБНОВЛЯЕМ ПЕРСОНАЖА
                            // ====================================

                            Main main =
                                    (Main) app;

                            if (main != null) {

                                UIManager ui =
                                        main.getUIManager();

                                if (ui != null) {

                                    ui.applyCharacterData(
                                            response
                                    );
                                }
                            }

                            // ====================================
                            // УДАЛЯЕМ DROP
                            // ====================================

                            removeDrop(drop);

                            SoundManager.playSound(
                                    SoundManager.SOUND_PICKUP
                            );

                            System.out.println(
                                    "[DropManager] "
                                            + "Picked up (server): "
                                            + drop.item.getName()
                            );

                        } else {

                            System.err.println(
                                    "[DropManager] "
                                            + "Server rejected pickup. "
                                            + "Check server logs."
                            );

                            if (uiManager != null) {

                                uiManager.showToast(
                                        "Failed to pick up item."
                                );
                            }
                        }
                    });
                })
                .exceptionally(ex -> {

                    app.enqueue(() -> {

                        System.err.println(
                                "[DropManager] "
                                        + "Network error: "
                                        + ex.getMessage()
                        );

                        if (uiManager != null) {

                            uiManager.showToast(
                                    "Network error: "
                                            + ex.getMessage()
                            );
                        }
                    });

                    return null;
                });
    }

    // ============================================================
    // REMOVE DROP
    // ============================================================

    /**
     * Полностью удаляет предмет с земли.
     *
     * Используется:
     * - при подборе;
     * - при истечении 10 секунд.
     */
    private void removeDrop(
            DropItem drop) {

        if (drop == null) {
            return;
        }

        if (drop.node != null) {

            dropNode.detachChild(
                    drop.node
            );
        }

        drops.remove(drop);
    }

    // ============================================================
    // GET DROP COUNT
    // ============================================================

    public int getDropCount() {

        return drops.size();
    }
private float getPixelThreshold() {
    float base = 100f; // Чуть увеличим базу для тач-экранов, палец менее точен, чем мышь
    float screenWidth = app.getCamera().getWidth();
    if (screenWidth <= 0) screenWidth = 1280f;
    // Масштабируем порог в зависимости от разрешения
    return base * Math.max(1f, screenWidth / 1280f);
}    // ============================================================
    // DROP ITEM
    // ============================================================

    public class DropItem {

        public Node node;
        public Item item;

        private Geometry iconGeom;
        private BitmapText labelText;

        /**
         * Оставшееся время жизни предмета.
         *
         * После 10 секунд становится <= 0.
         */
        private float lifeTime;

        public Vector3f getIconWorldPosition() {
            if (iconGeom == null) {
                return null;
            }
            // getWorldTranslation возвращает свежий Vector3f —
            // клонировать не обязательно.
            return iconGeom.getWorldTranslation();
        }
        
        public DropItem(
                Vector3f position,
                Item item) {

            this.item = item;

            // ========================================================
            // ВРЕМЯ ЖИЗНИ
            // ========================================================

            lifeTime =
                    DROP_LIFETIME;

            node =
                    new Node(
                            "DropNode_"
                                    + item.getName()
                    );

            // ========================================================
            // ВЫСОТА НАД ЗЕМЛЁЙ
            // ========================================================

            float heightAboveGround = 2.8f;

            node.setLocalTranslation(
                    position.x,
                    position.y + heightAboveGround,
                    position.z
            );

            // ========================================================
            // ICON
            // ========================================================

            Quad quad =
                    new Quad(
                            ICON_SIZE,
                            ICON_SIZE
                    );

            iconGeom =
                    new Geometry(
                            "DropIcon",
                            quad
                    );

            Material mat =
                    new Material(
                            app.getAssetManager(),
                            "Common/MatDefs/Misc/Unshaded.j3md"
                    );

            Texture tex = null;

            try {

                tex =
                        app.getAssetManager()
                                .loadTexture(
                                        item.getIconPath()
                                );

            } catch (Exception e) {

                System.err.println(
                        "[DropManager] "
                                + "Cannot load icon: "
                                + item.getIconPath()
                );
            }

            if (tex != null) {

                mat.setTexture(
                        "ColorMap",
                        tex
                );

                mat.setColor(
                        "Color",
                        ColorRGBA.White
                );

            } else {

                mat.setColor(
                        "Color",
                        item.getColor()
                );
            }

            iconGeom.setMaterial(mat);

            // ========================================================
            // ПОЗИЦИЯ ICON
            // ========================================================

            float iconYOffset = 0.5f;

            iconGeom.setLocalTranslation(
                    -ICON_SIZE / 2,
                    iconYOffset - ICON_SIZE / 2,
                    -ICON_SIZE / 2
            );

            node.attachChild(
                    iconGeom
            );

            // ========================================================
            // BORDER
            // ========================================================

            Quad borderQuad =
                    new Quad(
                            ICON_SIZE + BORDER_SIZE,
                            ICON_SIZE + BORDER_SIZE
                    );

            Geometry borderGeom =
                    new Geometry(
                            "DropBorder",
                            borderQuad
                    );

            Material borderMat =
                    new Material(
                            app.getAssetManager(),
                            "Common/MatDefs/Misc/Unshaded.j3md"
                    );

            borderMat.setColor(
                    "Color",
                    new ColorRGBA(
                            0.1f,
                            0.1f,
                            0.1f,
                            0.8f
                    )
            );

            borderGeom.setMaterial(
                    borderMat
            );

            borderGeom.setLocalTranslation(
                    -(ICON_SIZE + BORDER_SIZE) / 2,
                    iconYOffset
                            - BORDER_SIZE / 2
                            - ICON_SIZE / 2,
                    -(ICON_SIZE + BORDER_SIZE) / 2
            );

            node.attachChild(
                    borderGeom
            );

            // ========================================================
            // TEXT
            // ========================================================

            labelText =
                    new BitmapText(
                            app.getAssetManager()
                                    .loadFont(
                                            "Interface/Fonts/ru.fnt"
                                    )
                    );

            labelText.setText(
                    item.getName()
            );

            labelText.setSize(
                    0.5f
            );

            labelText.setColor(
                    item.getColor()
            );

            float textWidth =
                    labelText.getLineWidth();

            float textOffsetY =
                    0.7f;

            labelText.setLocalTranslation(
                    -textWidth / 2,
                    iconYOffset
                            + ICON_SIZE / 2
                            + textOffsetY,
                    0
            );

            node.attachChild(
                    labelText
            );

            // ========================================================
            // BILLBOARD
            // ========================================================

            BillboardControl billboard =
                    new BillboardControl();

            node.addControl(
                    billboard
            );
        }

        // ============================================================
        // GET REMAINING TIME
        // ============================================================

        public float getLifeTime() {
            return lifeTime;
        }

        // ============================================================
        // GET REMAINING TIME IN SECONDS
        // ============================================================

        public float getRemainingSeconds() {

            return Math.max(
                    0f,
                    lifeTime
            );
        }
    }
}