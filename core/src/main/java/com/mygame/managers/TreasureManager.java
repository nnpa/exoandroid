package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.mygame.dungeons.DungeonChestData;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Управляет сундуками с сокровищами в текущем данже.
 *
 * При загрузке данжа по dungeonId ищет класс-описание
 * координат сундуков (по рефлексии) и создаёт объекты Treasure.
 */
public class TreasureManager {

    /**
     * Пакет, в котором лежат классы вида Dungeon_1, Dungeon_2 и т.д.
     */
    private static final String CHEST_DATA_PACKAGE = "com.mygame.dungeons.";

    private static final float CLICK_PIXEL_THRESHOLD = 60f;

    private final SimpleApplication app;
    private final Node treasureNode;

    private final List<Treasure> treasures = new ArrayList<>();

    private DropManager dropManager;

    public TreasureManager(SimpleApplication app) {

        this.app = app;

        treasureNode = new Node("TreasureNode");

        app.getRootNode().attachChild(treasureNode);
    }

    public void setDropManager(DropManager dm) {
        this.dropManager = dm;
    }

    // ============================================================
    // ЗАГРУЗКА СУНДУКОВ ДЛЯ ДАНЖА
    // ============================================================

    public void loadDungeonTreasures(
            String dungeonId,
            int difficulty,
            int playerLevel
    ) {

        /*
         * Очищаем ноду сундуков на случай,
         * если предыдущий данж уже что-то в неё положил.
         */
        clearTreasures();

        List<Vector3f> positions =
                resolveChestPositions(dungeonId);

        if (positions == null || positions.isEmpty()) {

            System.out.println(
                    "[TreasureManager] No chest data for dungeon: "
                    + dungeonId
            );

            return;
        }

        for (Vector3f pos : positions) {

            Treasure treasure =
                    new Treasure(app, pos, difficulty, playerLevel);

            treasureNode.attachChild(treasure.getNode());

            treasures.add(treasure);
        }

        System.out.println(
                "[TreasureManager] Spawned "
                + treasures.size()
                + " chests for "
                + dungeonId
        );
    }

    // ============================================================
    // РЕФЛЕКСИЯ — ПОИСК КЛАССА С КООРДИНАТАМИ
    // ============================================================

    private List<Vector3f> resolveChestPositions(String dungeonId) {

        if (dungeonId == null || dungeonId.isEmpty()) {
            return null;
        }

        String className =
                CHEST_DATA_PACKAGE + capitalize(dungeonId);

        try {

            Class<?> clazz = Class.forName(className);

            Constructor<?> ctor = clazz.getDeclaredConstructor();

            Object instance = ctor.newInstance();

            if (instance instanceof DungeonChestData) {

                return ((DungeonChestData) instance).getChestPositions();

            } else {

                System.err.println(
                        "[TreasureManager] "
                        + className
                        + " does not implement DungeonChestData"
                );
            }

        } catch (ClassNotFoundException e) {

            /*
             * Нормальная ситуация: для этого данжа
             * просто нет сундуков.
             */
            System.out.println(
                    "[TreasureManager] No chest class found: "
                    + className
            );

        } catch (Exception e) {

            System.err.println(
                    "[TreasureManager] Error instantiating chest data: "
                    + e.getMessage()
            );

            e.printStackTrace();
        }

        return null;
    }

    private String capitalize(String s) {

        if (s == null || s.isEmpty()) {
            return s;
        }

        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ============================================================
    // UPDATE
    // ============================================================

    public void update(float tpf) {

        for (Treasure treasure : treasures) {
            treasure.update(tpf);
        }
    }

    // ============================================================
    // КЛИК ПО СУНДУКУ (по аналогии с DropManager.getDropAt)
    // ============================================================

    public Treasure getTreasureAt(float screenX, float screenY) {

        Camera cam = app.getCamera();

        for (Treasure treasure : treasures) {

            if (treasure.isOpened()) {
                continue;
            }

            Vector3f worldPos =
                    treasure.getNode().getWorldTranslation();

            Vector3f screenPos =
                    cam.getScreenCoordinates(worldPos);

            if (screenPos == null) {
                continue;
            }

            float dx = screenPos.x - screenX;
            float dy = screenPos.y - screenY;

            float dist = FastMath.sqrt(dx * dx + dy * dy);

            if (dist < CLICK_PIXEL_THRESHOLD) {
                return treasure;
            }
        }

        return null;
    }

    public void openTreasure(Treasure treasure) {

        if (treasure == null || treasure.isOpened()) {
            return;
        }

        treasure.open(dropManager);
    }

    // ============================================================
    // ОЧИСТКА
    // ============================================================

    /**
     * Очищает ноду сундуков.
     *
     * Вызывается перед загрузкой нового данжа,
     * а также как часть полного cleanup().
     */
    public void clearTreasures() {

        treasureNode.detachAllChildren();

        treasures.clear();
    }

    /**
     * Полная очистка перед закрытием игры.
     */
    public void cleanup() {

        clearTreasures();

        if (treasureNode.getParent() != null) {
            treasureNode.getParent().detachChild(treasureNode);
        }
    }
}