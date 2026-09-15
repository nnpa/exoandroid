package com.mygame.managers;

import com.jme3.anim.AnimComposer;
import com.jme3.anim.tween.action.Action;
import com.jme3.app.SimpleApplication;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.mygame.items.Item;
import com.mygame.items.ItemGenerator;

import java.util.List;
import java.util.Set;

public class Treasure {

    private static final String MODEL_PATH =
            "Models/chest/chest.gltf";

    private static final String ANIM_CLOSE = "Close";
    private static final String ANIM_OPEN = "Open";

    private static final float REMOVE_DELAY_AFTER_OPEN = 1.3f;

    private static final int MIN_ITEMS = 2;
    private static final int MAX_ITEMS = 3;

    private final SimpleApplication app;
    private final Node node;

    private AnimComposer animComposer;

    /*
     * Общий механизм "проиграть один раз и остановить"
     * используется и для Close (при спавне),
     * и для Open (при клике).
     */
    private boolean animationPlaying = false;
    private float animPlayTimer = 0f;
    private float currentAnimLength = 1.5f; // запасное значение

    private boolean opened = false;
    private float removeTimer = -1f;

    private final int difficulty;
    private final int playerLevel;

    public Treasure(
            SimpleApplication app,
            Vector3f position,
            int difficulty,
            int playerLevel
    ) {

        this.app = app;
        this.difficulty = Math.max(1, difficulty);
        this.playerLevel = Math.max(1, playerLevel);

        node = new Node("Treasure");
        node.setLocalTranslation(position);

        loadModel();
        playAnimationOnce(ANIM_CLOSE);
    }

    // ============================================================
    // МОДЕЛЬ
    // ============================================================

    private void loadModel() {

        try {

            Spatial model =
                    app.getAssetManager().loadModel(MODEL_PATH);

            node.attachChild(model);

            animComposer = findAnimComposer(model);

            if (animComposer == null) {

                System.err.println(
                        "[Treasure] AnimComposer not found in chest model"
                );

            } else {

                Set<String> clips = animComposer.getAnimClipsNames();

                System.out.println(
                        "[Treasure] Available animations: " + clips
                );
            }

        } catch (Exception e) {

            System.err.println(
                    "[Treasure] Failed to load chest model: "
                    + e.getMessage()
            );
        }
    }

    private AnimComposer findAnimComposer(Spatial spatial) {

        AnimComposer composer =
                spatial.getControl(AnimComposer.class);

        if (composer != null) {
            return composer;
        }

        if (spatial instanceof Node) {

            for (Spatial child : ((Node) spatial).getChildren()) {

                AnimComposer found = findAnimComposer(child);

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    // ============================================================
    // ПРОИГРАТЬ АНИМАЦИЮ ОДИН РАЗ (Close или Open)
    // ============================================================

    private void playAnimationOnce(String animName) {

        if (animComposer == null) {
            return;
        }

        if (!animComposer.getAnimClipsNames().contains(animName)) {

            System.err.println(
                    "[Treasure] Animation \""
                    + animName
                    + "\" not found on chest model"
            );

            return;
        }

        try {

            Action action =
                    animComposer.setCurrentAction(
                            animName,
                            AnimComposer.DEFAULT_LAYER,
                            false
                    );

            if (action != null && action.getLength() > 0f) {

                currentAnimLength = (float) action.getLength();

            } else {

                currentAnimLength = 1.5f;
            }

            animationPlaying = true;
            animPlayTimer = 0f;

        } catch (Exception e) {

            System.err.println(
                    "[Treasure] Failed to play animation \""
                    + animName
                    + "\": "
                    + e.getMessage()
            );
        }
    }

    /**
     * Явно останавливает текущую анимацию,
     * чтобы AnimComposer не запускал её заново
     * и модель замерла на последнем кадре.
     */
    private void stopAnimation() {

        if (animComposer == null) {
            return;
        }

        try {

            animComposer.removeCurrentAction(
                    AnimComposer.DEFAULT_LAYER
            );

        } catch (Exception e) {

            System.err.println(
                    "[Treasure] Failed to stop animation: "
                    + e.getMessage()
            );
        }

        animationPlaying = false;
    }

    // ============================================================
    // ОТКРЫТИЕ (клик игрока)
    // ============================================================

    public void open(DropManager dropManager) {

        if (opened) {
            return;
        }

        opened = true;

        //playAnimationOnce(ANIM_OPEN);
        spawnLoot(dropManager);

        removeTimer = REMOVE_DELAY_AFTER_OPEN;
    }

    private void spawnLoot(DropManager dropManager) {

        if (dropManager == null) {
            return;
        }

        int itemCount =
                MIN_ITEMS
                + (int) (Math.random() * (MAX_ITEMS - MIN_ITEMS + 1));

        List<Item> items =
                ItemGenerator.generateDrop(
                        playerLevel,
                        itemCount,
                        difficulty
                );

        Vector3f worldPos = node.getWorldTranslation();

        dropManager.spawnDrops(worldPos, items);
    }

    // ============================================================
    // UPDATE
    // ============================================================

    public void update(float tpf) {

        // ========================================================
        // ОСТАНОВКА АНИМАЦИИ ПОСЛЕ ОДНОКРАТНОГО ПРОИГРЫВАНИЯ
        // (работает и для Close, и для Open)
        // ========================================================

        if (animationPlaying) {

            animPlayTimer += tpf;

            if (animPlayTimer >= currentAnimLength) {

                stopAnimation();
            }
        }

        // ========================================================
        // УДАЛЕНИЕ ПОСЛЕ ОТКРЫТИЯ
        // ========================================================

        if (removeTimer > 0f) {

            removeTimer -= tpf;

            if (removeTimer <= 0f) {

                removeTimer = -1f;

                if (node.getParent() != null) {
                    node.getParent().detachChild(node);
                }
            }
        }
    }

    // ============================================================
    // GETTERS
    // ============================================================

    public Node getNode() {
        return node;
    }

    public boolean isOpened() {
        return opened;
    }
}