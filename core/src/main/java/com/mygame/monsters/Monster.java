package com.mygame.monsters;

import com.jme3.anim.AnimComposer;
import com.jme3.anim.tween.Tween;
import com.jme3.anim.tween.Tweens;
import com.jme3.anim.tween.action.Action;
import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Quad;
import com.mygame.items.Item;
import com.mygame.items.LootTable;
import com.mygame.managers.DropManager;
import com.mygame.managers.PlayerManager;
import com.mygame.managers.SoundManager;
import com.mygame.managers.WorldManager;

import java.util.List;

public class Monster {

    // ============================================================
    // БАЗОВЫЕ ПОЛЯ
    // ============================================================

    private String id;
    private String name;
    private int level;

    private float health;
    private float maxHealth;

    private float damage;
    private float attackRange;
    private float moveSpeed;
    private float aggroRange;

    // ============================================================
    // МОДЕЛЬ
    // ============================================================

    protected Node modelNode;
    protected Node healthBarNode;

    protected Geometry hpBarBackground;
    protected Geometry hpBarForeground;

    protected AnimComposer animComposer;

    protected Vector3f spawnPosition;
    protected Vector3f currentPosition;

    // ============================================================
    // АНИМАЦИЯ
    // ============================================================

    /**
     * Последняя установленная анимация.
     *
     * Нужна для того, чтобы не вызывать
     * setCurrentAction() каждый кадр.
     */
    protected String currentAnimation = "";

    // ============================================================
    // СИСТЕМЫ
    // ============================================================

    protected LootTable lootTable;
    protected MonsterAI ai;
    protected DropManager dropManager;
    protected PlayerManager playerManager;
    protected WorldManager worldManager;

    // ============================================================
    // СОСТОЯНИЕ
    // ============================================================

    protected boolean isAlive = true;

    protected float deathTimer = -1f;

    protected static final float DEATH_DELAY_FALLBACK = 3.0f;

    // ============================================================
    // БОСС
    // ============================================================

    protected boolean isBoss = false;
    protected boolean isFinalBoss = false;

    protected String nextDungeonId = null;

    protected boolean increaseDifficultyOnDeath = false;

    protected float nextDungeonTimer = -1f;

    protected static final float NEXT_DUNGEON_DELAY = 10.0f;

    // ============================================================
    // APP
    // ============================================================

    protected static SimpleApplication app;

    // ============================================================
    // HP BAR
    // ============================================================

    protected static final float HP_BAR_WIDTH = 1.2f;
    protected static final float HP_BAR_HEIGHT = 0.08f;
    protected static final float HP_BAR_HEIGHT_OFFSET = 5.0f;

    protected float hpBarHeightOffset = HP_BAR_HEIGHT_OFFSET;

    protected static final float HP_BAR_LEFT_OFFSET = 0.6f;
    protected static final float HP_BAR_Z_OFFSET = 0.01f;

    // ============================================================
    // СТАТУСЫ
    // ============================================================

    public static boolean isGameRunning = true;

    protected float stunTimer = 0f;

    protected float bleedTimer = 0f;

    protected float bleedDamage = 0f;

    // ============================================================
    // STUN EFFECT
    // ============================================================

    protected Node stunEffectNode;

    protected Spatial[] stunParticles;

    protected boolean stunEffectActive = false;

    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public Monster() {

        ai = new MonsterAI(this);

        System.out.println(
                "[Monster] AI created"
        );
    }

    // ============================================================
    // APP
    // ============================================================

    public static void setApp(
            SimpleApplication application
    ) {

        app = application;
    }

    // ============================================================
    // GETTERS / SETTERS
    // ============================================================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public float getHealth() {
        return health;
    }

    public void setHealth(float health) {

        this.health = Math.max(
                0f,
                health
        );

        updateHealthBar();
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(float maxHealth) {

        this.maxHealth = Math.max(
                0f,
                maxHealth
        );

        updateHealthBar();
    }

    public float getDamage() {
        return damage;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public float getAttackRange() {
        return attackRange;
    }

    public void setAttackRange(float attackRange) {
        this.attackRange = attackRange;
    }

    public float getMoveSpeed() {
        return moveSpeed;
    }

    public void setMoveSpeed(float moveSpeed) {
        this.moveSpeed = moveSpeed;
    }

    public float getAggroRange() {
        return aggroRange;
    }

    public void setAggroRange(float aggroRange) {
        this.aggroRange = aggroRange;
    }

    // ============================================================
    // MODEL
    // ============================================================

    public Node getModelNode() {
        return modelNode;
    }

    public void setModelNode(Node modelNode) {

        this.modelNode = modelNode;

        if (modelNode == null) {
            return;
        }

        /*
         * Базовая ориентация модели.
         */
        modelNode.rotate(
                0f,
                -FastMath.HALF_PI,
                0f
        );

        modelNode.setName("Monster");

        if (spawnPosition != null) {

            modelNode.setLocalTranslation(
                    spawnPosition
            );
        }

        currentPosition =
                spawnPosition != null
                        ? spawnPosition.clone()
                        : new Vector3f(
                                0f,
                                0f,
                                0f
                        );

        // ========================================================
        // ПОИСК ANIMCOMPOSER
        // ========================================================

        animComposer =
                findAnimComposer(
                        modelNode
                );

        currentAnimation = "";

        if (animComposer != null) {

            System.out.println(
                    "[Monster] AnimComposer found for "
                    + name
            );

            /*
             * При создании всегда Idle.
             */
            playAnimation("Idle");

        } else {

            System.err.println(
                    "[Monster] AnimComposer NOT FOUND for "
                    + name
            );
        }

        createHealthBar();

        createStunEffect();
    }

    // ============================================================
    // POSITION
    // ============================================================

    public Vector3f getSpawnPosition() {
        return spawnPosition;
    }

    public void setSpawnPosition(
            Vector3f spawnPosition
    ) {

        if (spawnPosition == null) {

            this.spawnPosition = null;
            return;
        }

        this.spawnPosition =
                spawnPosition.clone();

        this.currentPosition =
                spawnPosition.clone();

        if (modelNode != null) {

            modelNode.setLocalTranslation(
                    spawnPosition
            );
        }
    }

    public Vector3f getPosition() {

        if (currentPosition == null) {

            currentPosition =
                    new Vector3f(
                            0f,
                            0f,
                            0f
                    );
        }

        return currentPosition;
    }

    public void setPosition(
            Vector3f position
    ) {

        if (position == null) {
            return;
        }

        currentPosition =
                position.clone();

        if (modelNode != null) {

            modelNode.setLocalTranslation(
                    position
            );
        }
    }

    // ============================================================
    // SYSTEMS
    // ============================================================

    public LootTable getLootTable() {
        return lootTable;
    }

    public void setLootTable(
            LootTable lootTable
    ) {

        this.lootTable = lootTable;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean alive) {
        isAlive = alive;
    }

    public void setDropManager(
            DropManager dropManager
    ) {

        this.dropManager = dropManager;
    }

    public void setPlayerManager(
            PlayerManager playerManager
    ) {

        this.playerManager =
                playerManager;

        if (ai != null) {

            ai.setPlayerManager(
                    playerManager
            );
        }
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public void setWorldManager(
            WorldManager worldManager
    ) {

        this.worldManager =
                worldManager;
    }

    public MonsterAI getAI() {
        return ai;
    }

    // ============================================================
    // BOSS
    // ============================================================

    public boolean isBoss() {
        return isBoss;
    }

    public void setBoss(boolean boss) {
        isBoss = boss;
    }

    public boolean isFinalBoss() {
        return isFinalBoss;
    }

    public void setFinalBoss(
            boolean finalBoss
    ) {

        isFinalBoss =
                finalBoss;
    }

    public String getNextDungeonId() {
        return nextDungeonId;
    }

    public void setNextDungeonId(
            String nextDungeonId
    ) {

        this.nextDungeonId =
                nextDungeonId;
    }

    public boolean isIncreaseDifficultyOnDeath() {
        return increaseDifficultyOnDeath;
    }

    public void setIncreaseDifficultyOnDeath(
            boolean increase
    ) {

        increaseDifficultyOnDeath =
                increase;
    }

    // ============================================================
    // ATTACK RANGE
    // ============================================================

    public boolean isPlayerInAttackRange() {

        if (playerManager == null) {
            return false;
        }

        Vector3f monsterPos =
                getPosition();

        Vector3f playerPos =
                playerManager.getPosition();

        if (monsterPos == null ||
                playerPos == null) {

            return false;
        }

        return getHorizontalDistance(
                monsterPos,
                playerPos
        ) <= attackRange;
    }

    protected float getHorizontalDistance(
            Vector3f a,
            Vector3f b
    ) {

        float dx =
                b.x - a.x;

        float dz =
                b.z - a.z;

        return FastMath.sqrt(
                dx * dx +
                dz * dz
        );
    }

    // ============================================================
    // STUN
    // ============================================================

    public void applyStun(float duration) {

        stunTimer =
                Math.max(
                        stunTimer,
                        duration
                );

        System.out.println(
                ">>> STUN: "
                + name
                + " "
                + duration
                + " sec"
        );

        showStunEffect(true);
    }

    public float getStunTimer() {
        return stunTimer;
    }

    public boolean isStunned() {
        return stunTimer > 0f;
    }

    // ============================================================
    // BLEED
    // ============================================================

    public void applyBleed(
            float duration,
            float damagePerSecond
    ) {

        bleedTimer =
                duration;

        bleedDamage =
                damagePerSecond;
    }

    // ============================================================
    // WALK / IDLE
    // ============================================================

    public void stopWalking() {

        playAnimation("Idle");
    }

    public void playWalk() {

        playAnimation("Walk");
    }

    public void playIdle() {

        playAnimation("Idle");
    }

    public void playAttack() {

        playAnimation("Attack");
    }

    // ============================================================
    // ANIMATION
    // ============================================================

    public void playAnimation(
            String animName
    ) {

        if (animComposer == null) {

            System.err.println(
                    "[Monster] Cannot play animation "
                    + animName
                    + " because AnimComposer == null"
            );

            return;
        }

        if (animName == null ||
                animName.isEmpty()) {

            return;
        }

        /*
         * НЕ ПЕРЕЗАПУСКАЕМ анимацию каждый кадр.
         */
        if (animName.equals(currentAnimation)) {
            return;
        }

        try {

            System.out.println(
                    "[Monster Animation] "
                    + name
                    + ": "
                    + currentAnimation
                    + " -> "
                    + animName
            );

            animComposer.setCurrentAction(
                    animName
            );

            currentAnimation =
                    animName;

        } catch (Exception e) {

            System.err.println(
                    "[Monster] ERROR playing animation: "
                    + animName
            );

            e.printStackTrace();
        }
    }

    public String getCurrentAnimation() {
        return currentAnimation;
    }

    // ============================================================
    // FIND ANIM COMPOSER
    // ============================================================

    protected AnimComposer findAnimComposer(
            Spatial spatial
    ) {

        AnimComposer composer =
                spatial.getControl(
                        AnimComposer.class
                );

        if (composer != null) {
            return composer;
        }

        if (spatial instanceof Node) {

            for (Spatial child :
                    ((Node) spatial).getChildren()) {

                AnimComposer found =
                        findAnimComposer(child);

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    // ============================================================
    // DAMAGE
    // ============================================================

    public void takeDamage(float amount) {

        if (!isAlive) {
            return;
        }

        if (amount <= 0f) {
            return;
        }

        health -= amount;

        if (health < 0f) {
            health = 0f;
        }

        
        Vector3f dmgPos = modelNode != null
                ? modelNode.getWorldTranslation()
                : getPosition();

        com.mygame.managers.DamageNumberManager.spawnDamage(
                app,
                modelNode,
                dmgPos,
                (int) amount
        );
        
        System.out.println(
                "[Monster] "
                + name
                + " took "
                + amount
                + " damage, HP: "
                + health
                + "/"
                + maxHealth
        );

        updateHealthBar();

        if (health <= 0f) {

            isAlive = false;

            onDeath();
        }
    }

    // ============================================================
    // HP BAR
    // ============================================================

    protected void createHealthBar() {

        if (app == null ||
                modelNode == null ||
                healthBarNode != null) {

            return;
        }

        healthBarNode =
                new Node(
                        "HealthBarNode"
                );

        healthBarNode.setQueueBucket(
                RenderQueue.Bucket.Transparent
        );

        healthBarNode.setLocalTranslation(
                HP_BAR_LEFT_OFFSET,
                hpBarHeightOffset,
                0f
        );

        BillboardControl billboard =
                new BillboardControl();

        healthBarNode.addControl(
                billboard
        );

        Material backgroundMaterial =
                new Material(
                        app.getAssetManager(),
                        "Common/MatDefs/Misc/Unshaded.j3md"
                );

        backgroundMaterial.setColor(
                "Color",
                new ColorRGBA(
                        0.12f,
                        0.12f,
                        0.12f,
                        0.95f
                )
        );

        Geometry background =
                new Geometry(
                        "HPBarBackground",
                        new Quad(
                                HP_BAR_WIDTH,
                                HP_BAR_HEIGHT
                        )
                );

        background.setMaterial(
                backgroundMaterial
        );

        background.setQueueBucket(
                RenderQueue.Bucket.Transparent
        );

        hpBarBackground =
                background;

        Material foregroundMaterial =
                new Material(
                        app.getAssetManager(),
                        "Common/MatDefs/Misc/Unshaded.j3md"
                );

        foregroundMaterial.setColor(
                "Color",
                ColorRGBA.Green
        );

        Geometry foreground =
                new Geometry(
                        "HPBarForeground",
                        new Quad(
                                HP_BAR_WIDTH,
                                HP_BAR_HEIGHT
                        )
                );

        foreground.setMaterial(
                foregroundMaterial
        );

        foreground.setQueueBucket(
                RenderQueue.Bucket.Transparent
        );

        foreground.setLocalTranslation(
                0f,
                0f,
                HP_BAR_Z_OFFSET
        );

        hpBarForeground =
                foreground;

        healthBarNode.attachChild(
                background
        );

        healthBarNode.attachChild(
                foreground
        );

        modelNode.attachChild(
                healthBarNode
        );

        updateHealthBar();
    }

    public void updateHealthBar() {

        if (hpBarForeground == null ||
                maxHealth <= 0f) {

            return;
        }

        float percent =
                health / maxHealth;

        percent =
                Math.max(
                        0f,
                        Math.min(
                                1f,
                                percent
                        )
                );

        hpBarForeground.setLocalScale(
                percent,
                1f,
                1f
        );
    }

    public void setHpBarHeightOffset(
            float offset
    ) {

        hpBarHeightOffset =
                offset;

        if (healthBarNode != null) {

            healthBarNode.setLocalTranslation(
                    HP_BAR_LEFT_OFFSET,
                    hpBarHeightOffset,
                    0f
            );
        }
    }

    // ============================================================
    // STUN EFFECT
    // ============================================================

    protected void createStunEffect() {

        if (app == null ||
                modelNode == null ||
                stunEffectNode != null) {

            return;
        }

        stunEffectNode =
                new Node(
                        "StunEffectNode"
                );

        stunEffectNode.setLocalTranslation(
                0f,
                3f,
                0f
        );

        stunEffectNode.setCullHint(
                Spatial.CullHint.Always
        );

        Material material =
                new Material(
                        app.getAssetManager(),
                        "Common/MatDefs/Misc/Unshaded.j3md"
                );

        material.setColor(
                "Color",
                new ColorRGBA(
                        0.5f,
                        0.5f,
                        0.5f,
                        0.9f
                )
        );

        stunParticles =
                new Spatial[4];

        float radius = 0.6f;

        for (int i = 0; i < 4; i++) {

            float angle =
                    i * FastMath.HALF_PI;

            float x =
                    radius *
                    FastMath.cos(angle);

            float z =
                    radius *
                    FastMath.sin(angle);

            Geometry particle =
                    new Geometry(
                            "StunParticle_" + i,
                            new Quad(
                                    0.2f,
                                    0.2f
                            )
                    );

            particle.setMaterial(material);

            particle.setLocalTranslation(
                    x,
                    0f,
                    z
            );

            particle.addControl(
                    new BillboardControl()
            );

            particle.setQueueBucket(
                    RenderQueue.Bucket.Transparent
            );

            stunParticles[i] =
                    particle;

            stunEffectNode.attachChild(
                    particle
            );
        }

        modelNode.attachChild(
                stunEffectNode
        );
    }

    protected void showStunEffect(
            boolean show
    ) {

        if (stunEffectNode == null) {
            return;
        }

        if (show && isAlive) {

            stunEffectNode.setCullHint(
                    Spatial.CullHint.Inherit
            );

            stunEffectActive = true;

        } else {

            stunEffectNode.setCullHint(
                    Spatial.CullHint.Always
            );

            stunEffectActive = false;
        }
    }

    // ============================================================
    // AI COMBAT HOOK
    // ============================================================

    /**
     * Вызывается MonsterAI, когда монстр находится
     * рядом с игроком.
     *
     * MeleMonster переопределяет этот метод.
     */
    public void updateCombat(
            float tpf
    ) {
        // Для обычного Monster ничего.
    }

    // ============================================================
    // DEATH
    // ============================================================

    protected void onDeath() {

        SoundManager.playSound(
                SoundManager.SOUND_MONSTER_DIE
        );

        showStunEffect(false);

        if (healthBarNode != null) {

            healthBarNode.setCullHint(
                    Spatial.CullHint.Always
            );
        }

        if (animComposer != null) {

            try {

                Action dieAction =
                        animComposer.makeAction(
                                "Die"
                        );

                Tween doneTween =
                        Tweens.callMethod(
                                this,
                                "removeModel"
                        );

                Action sequence =
                        animComposer.actionSequence(
                                "die_sequence",
                                dieAction,
                                doneTween
                        );

                currentAnimation =
                        "die_sequence";

                animComposer.setCurrentAction(
                        "die_sequence"
                );

            } catch (Exception e) {

                e.printStackTrace();

                removeModel();
            }

        } else {

            removeModel();
        }

        if (lootTable != null &&
        dropManager != null) {

    int difficulty =
            playerManager != null
                    ? playerManager.getCurrentDifficulty()
                    : 1;

    int playerLevel =
            playerManager != null
                    ? playerManager.getLevel()
                    : 1;

    List<Item> items =
            lootTable.rollForLoot(
                    playerLevel,
                    difficulty
            );

            if (!items.isEmpty()) {

                dropManager.spawnDrops(
                        currentPosition,
                        items
                );
            }
        }

        if ((isBoss || isFinalBoss)
                && nextDungeonId != null
                && worldManager != null) {

            nextDungeonTimer =
                    NEXT_DUNGEON_DELAY;
        }
    }

    // ============================================================
    // REMOVE MODEL
    // ============================================================

    public void removeModel() {

        if (modelNode != null &&
                modelNode.getParent() != null) {

            modelNode.getParent()
                    .detachChild(
                            modelNode
                    );
        }

        modelNode = null;

        stunEffectNode = null;
        stunParticles = null;
    }

    // ============================================================
    // STOP
    // ============================================================

    public void stop() {

        isAlive = false;

        showStunEffect(false);

        if (ai != null) {

            ai.stop();
            ai = null;
        }

        if (modelNode != null &&
                modelNode.getParent() != null) {

            modelNode.getParent()
                    .detachChild(
                            modelNode
                    );
        }

        modelNode = null;

        stunEffectNode = null;
        stunParticles = null;
    }
public void disableDungeonTransition() {
    this.nextDungeonId = null;
}
    // ============================================================
    // UPDATE
    // ============================================================

    public void update(float tpf) {

        if (!isGameRunning) {
            return;
        }

        // ========================================================
        // STUN
        // ========================================================

        if (isStunned()) {

            stunTimer -= tpf;

            if (stunTimer < 0f) {
                stunTimer = 0f;
            }

            if (ai != null) {
                ai.setStunned(true);
            }

            if (stunEffectActive &&
                    stunEffectNode != null) {

                stunEffectNode.rotate(
                        0f,
                        tpf * 4f,
                        0f
                );
            }

            if (stunTimer <= 0f) {

                showStunEffect(false);
            }

            return;
        }

        if (ai != null) {
            ai.setStunned(false);
        }

        if (stunEffectActive) {
            showStunEffect(false);
        }

        // ========================================================
        // BLEED
        // ========================================================

        if (bleedTimer > 0f) {

            bleedTimer -= tpf;

            takeDamage(
                    bleedDamage * tpf
            );
        }

        // ========================================================
        // DEATH
        // ========================================================

        if (!isAlive) {

            if (deathTimer > 0f) {

                deathTimer -= tpf;

                if (deathTimer <= 0f) {
                    removeModel();
                }
            }

            if (nextDungeonTimer > 0f) {

                nextDungeonTimer -= tpf;

                if (nextDungeonTimer <= 0f) {

                    nextDungeonTimer = -1f;

                    if (worldManager != null &&
                            nextDungeonId != null) {

                        int newDifficulty =
                                playerManager != null
                                        ? playerManager.getCurrentDifficulty()
                                        : 1;

                        if (increaseDifficultyOnDeath) {
                            newDifficulty++;
                        }

                        boolean returnToCity =
                                "city".equalsIgnoreCase(nextDungeonId);

                        if (playerManager != null) {

                            /*
                             * При возврате в город следующий забег начнётся
                             * снова с dungeon_1, только с увеличенной сложностью.
                             */
                            playerManager.updateDungeonProgress(
                                    returnToCity ? "dungeon_1" : nextDungeonId,
                                    newDifficulty
                            );
                        }

                        if (app != null) {

                            final int finalNewDifficulty = newDifficulty;

                            app.enqueue(() -> {

                                if (returnToCity) {

                                    /*
                                     * Возврат в город синхронный и надёжный
                                     * (в отличие от changeDungeon(), которая
                                     * откладывает постройку сцены на кадры
                                     * вперёд и может уронить персонажа под пол).
                                     */
                                    worldManager.returnToCity();

                                    if (playerManager != null
                                            && playerManager.getUiManager() != null) {

                                        playerManager.getUiManager().showToast(
                                                com.mygame.managers.LocalizationManager
                                                        .getInstance()
                                                        .get("dungeon.difficulty_increased")
                                                + " " + finalNewDifficulty
                                        );
                                    }

                                } else {

                                       worldManager.teleportToDungeon();

                                }

                                return null;
                            });
                        }
                    }
                }
            }

            return;
        }

        // ========================================================
        // AI
        // ========================================================

        if (ai != null) {
            ai.update(tpf);
        }
    }
    


public WorldManager getWorldManager() {
    return worldManager;
}
}