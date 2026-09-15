package com.mygame.managers;

import com.jme3.anim.AnimComposer;
import com.jme3.anim.SkinningControl;
import com.jme3.anim.tween.action.Action;
import com.jme3.anim.tween.action.ClipAction;
import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.effect.influencers.ParticleInfluencer;
import com.jme3.effect.shapes.EmitterPointShape;
import com.jme3.effect.shapes.EmitterSphereShape;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.mygame.Main;
import com.mygame.items.Item;
import com.mygame.items.ItemGenerator;
import com.mygame.monsters.Monster;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Управляет игроком: модель, физика, характеристики, бой, таланты, анимации.
 */
public class PlayerManager {
private float equipmentDamage = 0f;
private float equipmentDefense = 0f;
/**
 * Пересчитывает суммарные бонусы урона/защиты от надетой
 * экипировки. Нужно вызывать при экипировке, снятии
 * предмета и при первоначальной загрузке персонажа —
 * иначе damage/defense с вещей не участвуют в бою вообще.
 *
 * @param equippedItems список надетых предметов (7 слотов)
 */
public void recalculateEquipmentBonuses(List<Item> equippedItems) {
    float dmg = 0f;
    float def = 0f;

    if (equippedItems != null) {
        for (Item item : equippedItems) {
            if (item == null) continue;
            dmg += item.getDamage();
            def += item.getDefense();
        }
    }

    this.equipmentDamage = dmg;
    this.equipmentDefense = def;

    recalculateStats();
}
    // ============================================================
    // КОНСТАНТЫ АНИМАЦИЙ
    // ============================================================
    private static final String ANIM_IDLE  = "Idle";
    private static final String ANIM_WALK  = "Walk";
    private static final String ANIM_RUN   = "Run";
    private static final String ANIM_ATTACK = "Attack";
    private static final String ANIM_BLOCK  = "Block";
    private static final String ANIM_SPIN   = "SpinAttack";
    private static final String ANIM_KICK   = "Kick";
    private static final String ANIM_HEAL   = "Heal";
    private static final String ANIM_DIE    = "Die";

    private static final float MODEL_SCALE = 2.5f;
    private static final float PLAYER_HEIGHT_ABOVE_GROUND = 1.4f;

    private static final float FOOTSTEP_INTERVAL = 0.45f;

    private static final int KILLS_PER_LEVEL = 6;

    // ============================================================
    // ПОЗИЦИЯ И ДАНЖ
    // ============================================================
    private Vector3f lastDungeonPosition = new Vector3f(0f, 2.5f, 0f);
    private int currentDifficulty = 1;
    private String currentDungeonId = "dungeon_1";

    // ============================================================
    // ДВИЖОК, МОДЕЛЬ
    // ============================================================
    private final SimpleApplication app;
    private Node playerNode;
    private BetterCharacterControl characterControl;
    private AnimComposer animComposer;
    private SkinningControl skinningControl;

    // ============================================================
    // ДАННЫЕ ИГРОКА (базовые)
    // ============================================================
    private String playerName = "Exorcist";
    private int baseLevel = 1;
    private int baseHealth = 100;
    private int baseMaxHealth = 100;
    private int baseMana = 50;
    private int baseMaxMana = 50;
    private int experience = 0;
    private int gold = 100;
    private int healthPotions = 0;
    private int manaPotions = 0;

    // ============================================================
    // ИТОГОВЫЕ СТАТЫ (пересчитываются из базовых + бонусы)
    // ============================================================
    private final Map<String, Float> statBonuses = new HashMap<>();

    private int finalMaxHealth;
    private int finalHealth;
    private int finalMaxMana;
    private int finalMana;

    private float physicalDefense = 0f;
    private float magicalDefense = 0f;
    private float armor = 0f;
    private float blockChance = 0f;
    private float critChance = 0f;
    private float critDamage = 0f;
    private float attackSpeed = 1f;
    private float baseDamage = 10f;
    private float healPower = 0f;
    private float incomingHeal = 0f;
    private float hitChance = 0f;
    private float holyDamagePercent = 0f;
    private float lightDamagePercent = 0f;
    private float shieldFromHeal = 0f;
    private float manaOnHealPercent = 0f;
    private float damageIgnored = 0f;
    private float critDamageReduction = 0f;
    private float whirlwindRadius = 0f;
    private float kickStunDuration = 0f;
    private float rageAttackSpeed = 0f;

    // ============================================================
    // СОСТОЯНИЕ
    // ============================================================
    private boolean isAlive = true;
    private boolean isMoving = false;
    private boolean isAttacking = false;
    private String currentAnimation = "";
    private boolean skillAnimationPlaying = false;
    private String skillAnimationName = null;
    private Action activeOneShotAction = null;
    private long animationGeneration = 0L;

    // ============================================================
    // ДВИЖЕНИЕ
    // ============================================================
    private final Vector3f position = new Vector3f(0f, 2.5f, 0f);
    private Vector3f targetPosition = null;
    private final float arrivalThreshold = 0.3f;
    private boolean isMovingToTarget = false;
    private final Vector3f smoothPosition = new Vector3f();
    private float interpolationSpeed = 0.5f;

    // ============================================================
    // БОЙ
    // ============================================================
    private Spatial currentTarget = null;
    private float attackRange = 1.6f;
    private float attackCooldown = 0.8f;
    private float attackTimer = 0f;

    // ============================================================
    // СМЕРТЬ / УРОВЕНЬ
    // ============================================================
    private float deathTimer = 0f;
    private boolean isRespawning = false;
    private int killsCounter = 0;

    // ============================================================
    // ВРЕМЕННЫЕ ЭФФЕКТЫ ТАЛАНТОВ
    // ============================================================
    private float rageTimer = 0f;
    private float angelicProtectionTimer = 0f;
    private float auraDefenseTimer = 0f;

    // ============================================================
    // РЕГЕНЕРАЦИЯ
    // ============================================================
    private float healthRegenerationAccumulator = 0f;

    // ============================================================
    // ЩИТ
    // ============================================================
    private float shieldAmount = 0f;

    // ============================================================
    // БОЖЕСТВЕННЫЙ ГНЕВ
    // ============================================================
    private boolean divineWrathReady = false;

    // ============================================================
    // LIGHT NOVA (флаг)
    // ============================================================
    private boolean lightNovaActive = false;

    // ============================================================
    // МЕНЕДЖЕРЫ (внедряются извне)
    // ============================================================
    private WorldManager worldManager;
    private DropManager dropManager;
    private TalentManager talentManager;
    private UIManager uiManager;
    private NetworkManager networkManager;

    // ============================================================
    // АУДИО
    // ============================================================
    private AudioNode footstepNode;
    private float footstepTimer = 0f;

    // ============================================================
    // ЭФФЕКТЫ (частицы)
    // ============================================================
    private Node effectNode;
    private ParticleEmitter healParticles;

    // Whirlwind
    private Node whirlwindEffectNode;
    private ParticleEmitter whirlwindParticles;
    private boolean whirlwindActive = false;

    // BLOOD (НОВОЕ)
    private Node bloodEffectNode;
    private ParticleEmitter bloodEmitter;
    private boolean bloodActive = false;

    // ============================================================
    // КОНСТРУКТОР
    // ============================================================
    public PlayerManager(SimpleApplication app) {
        this.app = app;
        playerNode = new Node("PlayerNode");
        playerNode.setLocalTranslation(position);
        smoothPosition.set(position);
    }

    // ============================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ============================================================
    public void initialize() {
        loadPlayerModel();
        loadPlayerData();
        attachToScene();
        createPhysicsBody();
        recalculateStats();

        healthPotions = 3;
        manaPotions = 3;

        currentDifficulty = 1;
        currentDungeonId = "dungeon_1";

        footstepNode = SoundManager.getSoundNode(SoundManager.SOUND_FOOTSTEP);
        if (footstepNode != null) {
            footstepNode.setLooping(false);
        }

        createHealEffect();
        createWhirlwindEffect();
        createBloodEffect(); // новый вызов
    }

    // ============================================================
    // TALENT MANAGER
    // ============================================================
    public void setTalentManager(TalentManager tm) {
        this.talentManager = tm;
        if (tm != null) {
            tm.addPointsForLevel(baseLevel);
            recalculateStats();
        }
    }

    public TalentManager getTalentManager() {
        return talentManager;
    }

    // ============================================================
    // ПРОВЕРКА ТАЛАНТОВ (вспомогательные)
    // ============================================================
    private boolean hasTalent(String talentId) {
        return talentManager != null && talentManager.hasTalent(talentId);
    }

    private int getTalentLevel(String talentId) {
        return talentManager != null ? talentManager.getTalentLevel(talentId) : 0;
    }

    // ============================================================
    // ФИЗИКА
    // ============================================================
private void createPhysicsBody() {
    float mass = 150f;

    characterControl = new BetterCharacterControl(CHARACTER_RADIUS, CHARACTER_HEIGHT, mass);
    characterControl.setGravity(new Vector3f(0, -9.81f * 3f, 0));

    /*
     * Устраняет дрожь/скольжение вдоль диагональных внутренних
     * швов треугольников mesh-коллизии (известный баг Bullet/Minie,
     * см. Minie issue #18 и #56, а также форум jME "Inner edge
     * collision troubles"). Обнуление maxUpwardVelocity — это
     * рекомендованное решение именно для BetterCharacterControl.
     */

    characterControl.warp(new Vector3f(0f, 2.5f, 0f));
    characterControl.setWalkDirection(Vector3f.ZERO);
    characterControl.setPhysicsDamping(0.95f);
    characterControl.getRigidBody().setDamping(0.15f, 0.95f);

    playerNode.addControl(characterControl);
}
private void clampUpwardVelocity() {
    if (characterControl == null) {
        return;
    }
    Vector3f vel = characterControl.getRigidBody().getLinearVelocity();
    if (vel.y > 0f) {
        vel.y = 0f;
        characterControl.getRigidBody().setLinearVelocity(vel);
    }
}


    // ============================================================
    // МОДЕЛЬ
    // ============================================================
    private void loadPlayerModel() {
        try {
            Spatial model = app.getAssetManager().loadModel("Models/Player/player.gltf");
            if (model == null) {
                createPlaceholderModel();
                return;
            }

            enableShadows(model);
            model.rotate(0, -FastMath.HALF_PI, 0);
            model.scale(MODEL_SCALE);
            model.updateModelBound();

            if (model.getWorldBound() instanceof BoundingBox) {
                BoundingBox bb = (BoundingBox) model.getWorldBound();
                float bottomY = bb.getCenter().y - bb.getYExtent();
                float offsetY = PLAYER_HEIGHT_ABOVE_GROUND - bottomY;
                model.move(0, offsetY, 0);
            }

            playerNode.detachAllChildren();
            playerNode.attachChild(model);
            playerNode.setLocalTranslation(position);

            animComposer = findAnimComposer(model);
            if (animComposer != null) {
                Set<String> clips = animComposer.getAnimClipsNames();
                System.out.println("[PlayerManager] Animations found:");
                for (String clip : clips) {
                    System.out.println("  - " + clip);
                }
                configureAnimationActions();
                if (clips.contains(ANIM_IDLE)) {
                    playBaseAnimation(ANIM_IDLE);
                } else if (!clips.isEmpty()) {
                    changeAnimation(clips.iterator().next(), true, false);
                }
            } else {
                System.err.println("[PlayerManager] AnimComposer NOT FOUND!");
            }

            skinningControl = findSkinningControl(model);

        } catch (Exception e) {
            e.printStackTrace();
            createPlaceholderModel();
        }
    }

    private void enableShadows(Spatial spatial) {
        if (spatial instanceof Geometry) {
            ((Geometry) spatial).setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        } else if (spatial instanceof Node) {
            for (Spatial child : ((Node) spatial).getChildren()) {
                enableShadows(child);
            }
        }
    }

    private AnimComposer findAnimComposer(Spatial spatial) {
        if (spatial instanceof Node) {
            for (Spatial child : ((Node) spatial).getChildren()) {
                AnimComposer found = findAnimComposer(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return spatial.getControl(AnimComposer.class);
    }

    private SkinningControl findSkinningControl(Spatial spatial) {
        if (spatial instanceof Node) {
            for (Spatial child : ((Node) spatial).getChildren()) {
                SkinningControl found = findSkinningControl(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return spatial.getControl(SkinningControl.class);
    }

    // ============================================================
    // ПЛЕЙСХОЛДЕР (если модель не загружена)
    // ============================================================
    private void createPlaceholderModel() {
        Node modelNode = new Node("Placeholder");

        Geometry body = new Geometry("Body", new Box(0.4f, 0.6f, 0.3f));
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        body.setMaterial(mat);
        body.move(0, 0.6f, 0);
        modelNode.attachChild(body);

        Geometry head = new Geometry("Head", new Sphere(8, 8, 0.2f));
        Material headMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        headMat.setColor("Color", new ColorRGBA(1f, 0.8f, 0.6f, 1f));
        head.setMaterial(headMat);
        head.move(0, 1.2f, 0);
        modelNode.attachChild(head);

        Geometry leftLeg = new Geometry("LeftLeg", new Box(0.12f, 0.4f, 0.12f));
        Material legMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        legMat.setColor("Color", ColorRGBA.DarkGray);
        leftLeg.setMaterial(legMat);
        leftLeg.move(-0.15f, 0.2f, 0);
        modelNode.attachChild(leftLeg);

        Geometry rightLeg = new Geometry("RightLeg", new Box(0.12f, 0.4f, 0.12f));
        rightLeg.setMaterial(legMat);
        rightLeg.move(0.15f, 0.2f, 0);
        modelNode.attachChild(rightLeg);

        modelNode.scale(MODEL_SCALE);
        modelNode.move(0, PLAYER_HEIGHT_ABOVE_GROUND, 0);
        playerNode.attachChild(modelNode);
    }

    // ============================================================
    // СЦЕНА
    // ============================================================
    public void attachToScene() {
        if (playerNode != null && !app.getRootNode().hasChild(playerNode)) {
            app.getRootNode().attachChild(playerNode);
        }
    }

    // ============================================================
    // АНИМАЦИИ
    // ============================================================
    private void configureAnimationActions() {
        if (animComposer == null) {
            return;
        }
        for (String clipName : animComposer.getAnimClipsNames()) {
            try {
                Action action = animComposer.action(clipName);
                if (action instanceof ClipAction) {
                    ((ClipAction) action).setTransitionLength(0.0d);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private boolean hasAnimation(String animationName) {
        return animComposer != null && animComposer.getAnimClipsNames().contains(animationName);
    }

    private void playBaseAnimation(String animationName) {
        if (!isAlive || animComposer == null || animationName == null || animationName.isEmpty()) {
            return;
        }
        if (skillAnimationPlaying) {
            return;
        }
        if (!hasAnimation(animationName)) {
            return;
        }
        if (animationName.equals(currentAnimation) && animComposer.getCurrentAction(AnimComposer.DEFAULT_LAYER) != null) {
            return;
        }
        changeAnimation(animationName, true, false);
    }

    private boolean playSkillAnimation(String animationName) {
        if (animComposer == null || !hasAnimation(animationName) || !isAlive) {
            return false;
        }
        if (skillAnimationPlaying) {
            return false;
        }

        stopAnimationInternal();
        final long generation = ++animationGeneration;

        try {
            Action action = animComposer.setCurrentAction(animationName, AnimComposer.DEFAULT_LAYER, false);
            if (action == null) {
                return false;
            }
            activeOneShotAction = action;
            skillAnimationName = animationName;
            skillAnimationPlaying = true;
            currentAnimation = animationName;

            if (generation != animationGeneration) {
                finishSkillAnimation();
                return false;
            }
            return true;

        } catch (Exception e) {
            activeOneShotAction = null;
            skillAnimationPlaying = false;
            skillAnimationName = null;
            currentAnimation = "";
            return false;
        }
    }

    private void stopAnimationInternal() {
        if (animComposer == null) {
            return;
        }
        try {
            animComposer.removeCurrentAction(AnimComposer.DEFAULT_LAYER);
        } catch (Exception ignored) {
        }
    }

    private boolean changeAnimation(String animationName, boolean loop, boolean interruptOneShot) {
        if (animComposer == null || !hasAnimation(animationName)) {
            return false;
        }
        if (skillAnimationPlaying && !interruptOneShot) {
            return false;
        }
        if (interruptOneShot) {
            animationGeneration++;
            skillAnimationPlaying = false;
            skillAnimationName = null;
            activeOneShotAction = null;
        }

        stopAnimationInternal();
        try {
            Action action = animComposer.setCurrentAction(animationName, AnimComposer.DEFAULT_LAYER, loop);
            if (action == null) {
                currentAnimation = "";
                return false;
            }
            currentAnimation = animationName;
            return true;
        } catch (Exception e) {
            currentAnimation = "";
            return false;
        }
    }

    private boolean isActiveOneShot() {
        if (!skillAnimationPlaying || animComposer == null) {
            return false;
        }
        Action current = animComposer.getCurrentAction(AnimComposer.DEFAULT_LAYER);
        return current != null && current == activeOneShotAction;
    }

    private void finishSkillAnimation() {
        if (!skillAnimationPlaying) {
            return;
        }

        // Отключаем эффект Whirlwind, если анимация была SpinAttack
        if (skillAnimationName != null && skillAnimationName.equals(ANIM_SPIN)) {
            if (whirlwindParticles != null) {
                whirlwindParticles.killAllParticles();
                whirlwindParticles.setEnabled(false);
                whirlwindActive = false;
            }
        }

        animationGeneration++;
        skillAnimationPlaying = false;
        skillAnimationName = null;
        activeOneShotAction = null;
        stopAnimationInternal();
        currentAnimation = "";

        if (!isAlive) {
            return;
        }
        updateBaseAnimationFromState();
    }

    private void updateBaseAnimationFromState() {
        if (!isAlive || skillAnimationPlaying) {
            return;
        }
        if (currentTarget != null) {
            return;
        }
        if (isMovingToTarget || isMoving) {
            playBaseAnimation(ANIM_WALK);
        } else {
            playBaseAnimation(ANIM_IDLE);
        }
    }

    // ============================================================
    // ДВИЖЕНИЕ
    // ============================================================
    public void moveTo(Vector3f target) {
        if (playerNode == null || target == null || characterControl == null) {
            return;
        }
        if (skillAnimationPlaying) {
            return;
        }
        if (currentTarget != null) {
            currentTarget = null;
            isAttacking = false;
        }

        targetPosition = new Vector3f(target.x, 0, target.z);
        isMovingToTarget = true;
        setMoving(true);
        lookAt(target);
        stuckCheckTimer = 0f;
lastStuckDist = -1f;
    }

    public void stopMoving() {
        if (characterControl != null) {
            characterControl.setWalkDirection(Vector3f.ZERO);
            characterControl.getRigidBody().setLinearVelocity(Vector3f.ZERO);
        }
        setMoving(false);
        targetPosition = null;
        isMovingToTarget = false;

        if (!skillAnimationPlaying) {
            isAttacking = false;
            currentTarget = null;
            playBaseAnimation(ANIM_IDLE);
        }
    }

    public void lookAt(Vector3f target) {
        if (playerNode == null || characterControl == null || target == null) {
            return;
        }
        Vector3f currentPos = playerNode.getWorldTranslation();
        Vector3f direction = new Vector3f(target.x - currentPos.x, 0, target.z - currentPos.z);
        if (direction.lengthSquared() > 0.0001f) {
            direction.normalizeLocal();
            characterControl.setViewDirection(direction);
        }
    }

    public void setMoving(boolean moving) {
        if (this.isMoving && !moving) {
            footstepTimer = 0f;
        }
        this.isMoving = moving;
    }

    // ============================================================
    // АТАКА (команда)
    // ============================================================
    public void attackTarget(Spatial target) {
        if (target == null) {
            return;
        }
        if (skillAnimationPlaying) {
            return;
        }
        currentTarget = target;
        isAttacking = true;

        if (characterControl != null) {
            characterControl.setWalkDirection(Vector3f.ZERO);
            characterControl.getRigidBody().setLinearVelocity(Vector3f.ZERO);
        }
        setMoving(false);
        isMovingToTarget = false;
        targetPosition = null;

        lookAt(target.getWorldTranslation());
        attackTimer = 0f;
    }

    // ============================================================
    // РАСЧЁТ ФИЗИЧЕСКОГО УРОНА
    // ============================================================
    private float calculatePhysicalDamage() {
        float damage = baseDamage;

        if (hasTalent("attack_7")) {
            damage *= 1f + (0.10f * getTalentLevel("attack_7"));
        }

        // Rage влияет только на скорость атаки, урон не меняет
        return damage;
    }

    // ============================================================
    // ОБЫЧНАЯ АТАКА
    // ============================================================
    private void performAttack() {
        if (currentTarget == null || skillAnimationPlaying) {
            return;
        }

        lookAt(currentTarget.getWorldTranslation());
        float damage = calculatePhysicalDamage();

        // Divine Wrath
        if (divineWrathReady && hasTalent("attack_4")) {
            float bonus = 0.50f * getTalentLevel("attack_4");
            damage *= 1f + bonus;
            divineWrathReady = false;
            System.out.println("[Player] Divine Wrath activated: +" + (bonus * 100f) + "% damage");
        }

        // Holy Strike
        if (hasTalent("light_3")) {
            damage *= 1f + (holyDamagePercent / 100f);
        }

        // Critical Strike
        boolean isCrit = false;
        float actualCritChance = critChance;
        if (actualCritChance > 0f && Math.random() * 100f < actualCritChance) {
            isCrit = true;
            float criticalMultiplier = 1.5f + critDamage / 100f;
            damage *= criticalMultiplier;
            System.out.println("[Player] Critical hit!");
        }

        // Precision (попадание)
        float actualHitChance = 100f;
        if (hasTalent("attack_8")) {
            actualHitChance = Math.min(100f, 100f + hitChance);
        }
        if (Math.random() * 100f >= actualHitChance) {
            System.out.println("[Player] Attack missed!");
            return;
        }

        // Анимация
        playSkillAnimation(ANIM_ATTACK);
        attackTimer = getCurrentAttackCooldown();

        // Нанесение урона
        if (worldManager == null) {
            return;
        }

        // Испускаем кровь в сторону цели
        emitBloodTowardsTarget();

        Monster monster = worldManager.getMonsterByModel(currentTarget);
        if (monster != null && monster.isAlive()) {
            SoundManager.playSound(SoundManager.SOUND_ATTACK_PLAYER);
            monster.takeDamage(damage);

            if (!monster.isAlive()) {
                onMonsterKilled();
                currentTarget = null;
                isAttacking = false;
                setMoving(false);
                stopCharacterPhysics();
            }
            return;
        }

        // Старая система через Geometry
        if (currentTarget instanceof Geometry) {
            Geometry geom = (Geometry) currentTarget;
            WorldManager.MonsterData md = worldManager.getMonsterByGeometry(geom);
            if (md != null && !md.isDead) {
                md.hp -= (int) damage;
                if (md.hp <= 0) {
                    md.isDead = true;
                    Vector3f pos = geom.getWorldTranslation();
                    List<Item> items = ItemGenerator.generateDrop(baseLevel, 3, getCurrentDifficulty());
                    if (dropManager != null) {
                        dropManager.spawnDrops(pos, items);
                    }
                    geom.setCullHint(Node.CullHint.Always);
                    currentTarget = null;
                    isAttacking = false;
                    setMoving(false);
                    stopCharacterPhysics();
                    onMonsterKilled();
                }
            }
        }
    }

    private float getCurrentAttackCooldown() {
        float speed = getTotalAttackSpeedPercent();
        if (speed < 0f) {
            speed = 0f;
        }
        return attackCooldown / (1f + speed / 100f);
    }

    private float getTotalAttackSpeedPercent() {
        float result = getBonusStat("attack_speed");
        if (hasTalent("attack_6")) {
            result += 15f * getTalentLevel("attack_6");
        }
        if (rageTimer > 0f && hasTalent("attack_10")) {
            result += rageAttackSpeed;
        }
        return result;
    }

    private void stopCharacterPhysics() {
        if (characterControl == null) {
            return;
        }
        characterControl.setWalkDirection(Vector3f.ZERO);
        characterControl.getRigidBody().setLinearVelocity(Vector3f.ZERO);
    }

    // ============================================================
    // СКИЛЛЫ
    // ============================================================
    public void castSkill(String skillName) {
        if (skillAnimationPlaying) {
            System.out.println("[Player] Skill already playing: " + skillAnimationName);
            return;
        }
        if (animComposer == null || skillName == null) {
            return;
        }

        switch (skillName) {
            case "Heal":
                if (finalMana < 10) {
                    return;
                }
                if (!playSkillAnimation(ANIM_HEAL)) {
                    return;
                }
                SoundManager.playSound(SoundManager.SOUND_HEAL);
                useMana(10);

                float healAmount = 20f;
                if (hasTalent("light_1")) {
                    healAmount *= 1f + (0.15f * getTalentLevel("light_1"));
                }
                if (hasTalent("light_7")) {
                    healAmount *= 1f + (0.20f * getTalentLevel("light_7"));
                }
                heal((int) healAmount);

                if (hasTalent("light_2")) {
                    float shield = healAmount * 0.20f * getTalentLevel("light_2");
                    applyShieldToPlayer(shield);
                }

                if (hasTalent("light_6")) {
                    angelicProtectionTimer = 10f;
                    System.out.println("[Player] Angelic Protection active for 10 sec.");
                }

                if (hasTalent("light_8")) {
                    float manaPercent = 0.05f * getTalentLevel("light_8");
                    int manaRestore = Math.max(1, (int) (finalMaxMana * manaPercent));
                    finalMana = Math.min(finalMana + manaRestore, finalMaxMana);
                    System.out.println("[Player] Inspiration restored " + manaRestore + " mana.");
                }

                if (hasTalent("light_9") && currentTarget != null) {
                    float novaDamage = 10f * getTalentLevel("light_9");
                    if (hasTalent("light_5")) {
                        novaDamage *= 1f + (0.20f * getTalentLevel("light_5"));
                    }
                    dealDamageToTarget(novaDamage);
                }

                emitHealParticles();
                updateUI();
                break;

            case "ShieldBash":
                if (finalMana < 15) {
                    return;
                }
                if (currentTarget == null) {
                    return;
                }
                if (!hasAnimation(ANIM_BLOCK)) {
                    return;
                }
                if (!playSkillAnimation(ANIM_BLOCK)) {
                    return;
                }
                SoundManager.playSound(SoundManager.SOUND_SHIELD_BASH);
                useMana(15);

                float bashDamage = 15f;
                if (hasTalent("attack_1")) {
                    bashDamage *= 1f + (0.20f * getTalentLevel("attack_1"));
                }
                dealDamageToTarget(bashDamage);
                break;

            case "Whirlwind":
                if (finalMana < 20) {
                    return;
                }
                if (currentTarget == null) {
                    return;
                }
                if (!hasAnimation(ANIM_SPIN)) {
                    return;
                }
                if (!playSkillAnimation(ANIM_SPIN)) {
                    return;
                }
                SoundManager.playSound(SoundManager.SOUND_WHIRLWIND);
                useMana(20);

                float whirlwindDamage = 25f;
                if (hasTalent("attack_2")) {
                    whirlwindRadius = 30f * getTalentLevel("attack_2");
                    whirlwindDamage *= 1f + (whirlwindRadius / 100f);
                }
                if (hasTalent("attack_7")) {
                    whirlwindDamage *= 1f + (0.10f * getTalentLevel("attack_7"));
                }
                if (hasTalent("light_5")) {
                    whirlwindDamage *= 1f + (0.20f * getTalentLevel("light_5"));
                }
                dealDamageToTarget(whirlwindDamage);

                if (hasTalent("attack_4")) {
                    divineWrathReady = true;
                    System.out.println("[Player] Divine Wrath ready.");
                }

                // Активируем визуальный эффект Whirlwind
                if (whirlwindParticles != null) {
                    whirlwindParticles.setEnabled(true);
                    whirlwindActive = true;
                }
                break;

            case "Kick":
                if (finalMana < 10) {
                    return;
                }
                if (currentTarget == null) {
                    return;
                }
                if (!hasAnimation(ANIM_KICK)) {
                    return;
                }
                if (!playSkillAnimation(ANIM_KICK)) {
                    return;
                }
                SoundManager.playSound(SoundManager.SOUND_KICK);
                useMana(10);

                float kickDamage = 10f;
                if (hasTalent("attack_7")) {
                    kickDamage *= 1f + (0.10f * getTalentLevel("attack_7"));
                }
                dealDamageToTarget(kickDamage);

                if (hasTalent("attack_3")) {
                    Monster targetMonster = null;
                    if (worldManager != null) {
                        targetMonster = worldManager.getMonsterByModel(currentTarget);
                    }
                    if (targetMonster != null && targetMonster.isAlive()) {
                        float stunDuration = 3f + 0.5f * getTalentLevel("attack_3");
                        targetMonster.applyStun(stunDuration);
                        System.out.println("[Player] Kick stun: " + stunDuration + " sec");
                    }
                }
                break;

            default:
                System.out.println("[Player] Unknown skill: " + skillName);
        }
    }

    // ============================================================
    // УРОН ПО ЦЕЛИ (общая логика)
    // ============================================================
    private void dealDamageToTarget(float amount) {
        if (currentTarget == null || worldManager == null) {
            return;
        }

        Monster monster = worldManager.getMonsterByModel(currentTarget);
        if (monster != null && monster.isAlive()) {
            SoundManager.playSound(SoundManager.SOUND_ATTACK_PLAYER);
            monster.takeDamage(amount);
            if (!monster.isAlive()) {
                onMonsterKilled();
                currentTarget = null;
                isAttacking = false;
                setMoving(false);
                stopCharacterPhysics();
            }
            return;
        }

        if (currentTarget instanceof Geometry) {
            Geometry geom = (Geometry) currentTarget;
            WorldManager.MonsterData md = worldManager.getMonsterByGeometry(geom);
            if (md != null && !md.isDead) {
                md.hp -= (int) amount;
                if (md.hp <= 0) {
                    md.hp = 0;
                    md.isDead = true;
                    Vector3f pos = geom.getWorldTranslation();
                    List<Item> items = ItemGenerator.generateDrop(baseLevel, 3, getCurrentDifficulty());
                    if (dropManager != null) {
                        dropManager.spawnDrops(pos, items);
                    }
                    geom.setCullHint(Node.CullHint.Always);
                    currentTarget = null;
                    isAttacking = false;
                    setMoving(false);
                    stopCharacterPhysics();
                    onMonsterKilled();
                }
            }
        }
    }

    // ============================================================
    // ПОЛУЧЕНИЕ УРОНА
    // ============================================================
    public void takeDamage(int damage) {
        if (!isAlive || damage <= 0) {
            return;
        }

        float incomingDamage = damage;

        // Impervious (def_8)
        if (hasTalent("def_8")) {
            float reduction = 0.10f * getTalentLevel("def_8");
            incomingDamage *= 1f - Math.min(0.90f, reduction);
            System.out.println("[Player] Impervious reduced damage.");
        }

        // Shield
        if (shieldAmount > 0f) {
            float absorbed = Math.min(shieldAmount, incomingDamage);
            shieldAmount -= absorbed;
            incomingDamage -= absorbed;
            System.out.println("[Player] Shield absorbed " + absorbed + " damage.");
        }
        if (incomingDamage <= 0f) {
            return;
        }

        // Shield Block (def_4)
        if (hasTalent("def_4")) {
            float chance = 10f * getTalentLevel("def_4");
            if (Math.random() * 100f < chance) {
                incomingDamage *= 0.5f;
                System.out.println("[Player] Shield Block!");
            }
        }

        // Физическая защита + бонусы
float totalPhysicalDefense = physicalDefense + armor + equipmentDefense;

        // DEF 10 — Defense Aura (постоянный бонус)
        if (hasTalent("def_10")) {
            totalPhysicalDefense += 15f * getTalentLevel("def_10");
        }

        // Angelic Protection (light_6)
        if (angelicProtectionTimer > 0f && hasTalent("light_6")) {
            totalPhysicalDefense += 15f * getTalentLevel("light_6");
        }

        // Magical Barrier (def_3) — добавляем магическую защиту к физической
        if (hasTalent("def_3")) {
            totalPhysicalDefense += magicalDefense;
        }

        float reduction = Math.min(0.80f, totalPhysicalDefense / 100f);
        incomingDamage *= 1f - reduction;

        // Resilience (def_5) — снижение критического урона (здесь работает как общее снижение)
        if (hasTalent("def_5")) {
            float critReduction = 0.15f * getTalentLevel("def_5");
            incomingDamage *= 1f - Math.min(0.50f, critReduction);
        }

        int actualDamage = Math.max(0, (int) Math.ceil(incomingDamage));
        finalHealth -= actualDamage;
        System.out.println("[Player] Damage: " + actualDamage + ", HP: " + finalHealth + "/" + finalMaxHealth);

        DamageNumberManager.spawnDamage(app, playerNode, playerNode.getWorldTranslation(), actualDamage);
        // Undying (def_9)
        if (finalHealth <= 0 && hasTalent("def_9")) {
            finalHealth = Math.max(1, (int) (finalMaxHealth * 0.30f));
            System.out.println("[Player] Undying triggered! " + finalHealth + " HP restored.");
            isAlive = true;
            return;
        }

        // Resurrection (light_10)
        if (finalHealth <= 0 && hasTalent("light_10")) {
            finalHealth = Math.max(1, (int) (finalMaxHealth * 0.30f));
            System.out.println("[Player] Resurrection triggered!");
            isAlive = true;
            return;
        }

        // Смерть
        if (finalHealth <= 0) {
            finalHealth = 0;
            isAlive = false;
            changeAnimation(ANIM_DIE, false, true);
            deathTimer = 0f;
            System.out.println("[Player] Player died!");
        }

        updateUI();
    }

    // ============================================================
    // ЛЕЧЕНИЕ
    // ============================================================
    public void heal(int amount) {
        if (!isAlive) {
            return;
        }
        float multiplier = 1f;
        if (hasTalent("light_7")) {
            multiplier += 0.20f * getTalentLevel("light_7");
        }
        int healAmount = Math.max(0, (int) (amount * multiplier));
        finalHealth = Math.min(finalHealth + healAmount, finalMaxHealth);
        System.out.println("[Player] Healed: " + healAmount + ", HP: " + finalHealth + "/" + finalMaxHealth);

        DamageNumberManager.spawnHeal(app, playerNode, playerNode.getWorldTranslation(), healAmount);

        updateUI();
    }

    // ============================================================
    // ЩИТ
    // ============================================================
    public void applyShieldToPlayer(float amount) {
        if (amount <= 0f) {
            return;
        }
        shieldAmount = Math.max(shieldAmount, amount);
        System.out.println("[Player] Shield applied: " + shieldAmount);
    }

    public float getShieldAmount() {
        return shieldAmount;
    }

    // ============================================================
    // MANA
    // ============================================================
    public void useMana(int amount) {
        if (finalMana < amount) {
            return;
        }
        finalMana -= amount;
        System.out.println("[Player] Mana used: " + amount + ", remaining: " + finalMana);
        updateUI();
    }

    // ============================================================
    // ЗЕЛЬЯ
    // ============================================================
    public int getHealthPotions() {
        return healthPotions;
    }

    public int getManaPotions() {
        return manaPotions;
    }

    public void setHealthPotions(int count) {
        healthPotions = Math.max(0, count);
    }

    public void setManaPotions(int count) {
        manaPotions = Math.max(0, count);
    }

    public void addHealthPotions(int count) {
        healthPotions += count;
    }

    public void addManaPotions(int count) {
        manaPotions += count;
    }

    public void useHealthPotion() {
        if (healthPotions <= 0) {
            System.out.println("[Player] No health potions!");
            return;
        }
        healthPotions--;
        heal(50);
        System.out.println("[Player] Used health potion. Remaining: " + healthPotions);
        updateUI();
        savePotionsToServer();
    }

    public void useManaPotion() {
        if (manaPotions <= 0) {
            System.out.println("[Player] No mana potions!");
            return;
        }
        manaPotions--;
        finalMana = Math.min(finalMana + 30, finalMaxMana);
        System.out.println("[Player] Used mana potion. Remaining: " + manaPotions);
        updateUI();
        savePotionsToServer();
    }

    private void savePotionsToServer() {
        if (networkManager == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("healthPotions", healthPotions);
        data.put("manaPotions", manaPotions);
        networkManager.saveCharacter(data)
                .thenAccept(success -> {
                    if (success) {
                        System.out.println("[PlayerManager] Potions saved.");
                    } else {
                        System.err.println("[PlayerManager] Failed to save potions.");
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("[PlayerManager] Error saving potions: " + ex.getMessage());
                    return null;
                });
    }

    // ============================================================
    // ДАННЫЕ ИГРОКА (загрузка / обновление)
    // ============================================================
    private void loadPlayerData() {
        playerName = "Test Player";
        baseLevel = 1;
        baseHealth = 100;
        baseMaxHealth = 100;
        baseMana = 50;
        baseMaxMana = 50;
        finalHealth = baseMaxHealth;
        finalMana = baseMaxMana;
        gold = 100;
    }

    public void updatePlayerData(Map<String, Object> data) {
        if (data == null) {
            return;
        }
        if (data.containsKey("name")) {
            playerName = String.valueOf(data.get("name"));
        }
        if (data.containsKey("level")) {
            baseLevel = ((Number) data.get("level")).intValue();
        }
        if (data.containsKey("health")) {
            baseHealth = ((Number) data.get("health")).intValue();
        }
        if (data.containsKey("maxHealth")) {
            baseMaxHealth = ((Number) data.get("maxHealth")).intValue();
        }
        if (data.containsKey("mana")) {
            baseMana = ((Number) data.get("mana")).intValue();
        }
        if (data.containsKey("maxMana")) {
            baseMaxMana = ((Number) data.get("maxMana")).intValue();
        }
        if (data.containsKey("experience")) {
            experience = ((Number) data.get("experience")).intValue();
        }
        if (data.containsKey("gold")) {
            gold = ((Number) data.get("gold")).intValue();
        }
        recalculateStats();
    }

    // ============================================================
    // УБИЙСТВО МОНСТРА
    // ============================================================
    public void onMonsterKilled() {
        // Rage (attack_10)
        if (hasTalent("attack_10")) {
            rageAttackSpeed = 20f * getTalentLevel("attack_10");
            rageTimer = 10f;
            System.out.println("[Player] Rage activated for 10 seconds. Attack speed +" + rageAttackSpeed + "%");
        }

        if (baseLevel >= 50) {
            System.out.println("[PlayerManager] Max level reached.");
            return;
        }

        killsCounter++;
        System.out.println("[PlayerManager] Kills: " + killsCounter + "/" + KILLS_PER_LEVEL);

        if (killsCounter >= KILLS_PER_LEVEL) {
            killsCounter = 0;
            if (networkManager != null) {
                SoundManager.playSound(SoundManager.SOUND_LEVEL_UP);
                networkManager.levelUp()
                        .thenAccept(response -> {
                            app.enqueue(() -> {
                                if (response != null && uiManager != null) {
                                    uiManager.applyCharacterData(response);
                                    if (uiManager.getTalentManager() != null) {
                                        networkManager.loadTalents()
                                                .thenAccept(talentData -> {
                                                    app.enqueue(() -> {
                                                        if (talentData != null) {
                                                            Map<String, Integer> talents =
                                                                    (Map<String, Integer>) talentData.get("talents");
                                                            int points = ((Number) talentData.get("availablePoints")).intValue();
                                                            uiManager.getTalentManager().loadFromServer(talents, points);
                                                            if (uiManager.getTalentWindow() != null) {
                                                                uiManager.getTalentWindow().updateUI();
                                                            }
                                                        }
                                                        return null;
                                                    });
                                                });
                                    }
                                }
                                return null;
                            });
                        });
            }
        }
    }

    public void resetKillsCounter() {
        killsCounter = 0;
    }

    // ============================================================
    // РЕСПАВН
    // ============================================================
    private void respawnPlayer() {
        if (isRespawning) {
            return;
        }
        isRespawning = true;
        SoundManager.playSound(SoundManager.SOUND_PLAYER_DIE);
        System.out.println("[PlayerManager] Respawning player...");

        animationGeneration++;
        skillAnimationPlaying = false;
        skillAnimationName = null;
        activeOneShotAction = null;
        stopAnimationInternal();

        // Отключаем эффект Whirlwind при респавне
        if (whirlwindParticles != null) {
            whirlwindParticles.killAllParticles();
            whirlwindParticles.setEnabled(false);
            whirlwindActive = false;
        }

        setHealth(getMaxHealth());
        setMana(getMaxMana());
        setAlive(true);
        setMoving(false);
        targetPosition = null;
        isMovingToTarget = false;
        currentTarget = null;
        isAttacking = false;
        divineWrathReady = false;
        shieldAmount = 0f;
        rageTimer = 0f;
        angelicProtectionTimer = 0f;
        auraDefenseTimer = 0f;
        stopCharacterPhysics();

        if (worldManager != null) {
            worldManager.returnToCity();
        }

        currentAnimation = "";
        playBaseAnimation(ANIM_IDLE);
        deathTimer = 0f;
        isRespawning = false;
        System.out.println("[PlayerManager] Respawn complete.");
    }

    // ============================================================
    // ТАЛАНТЫ (применение бонусов)
    // ============================================================
    public void applyTalentBonuses(Map<String, Float> bonuses) {
        statBonuses.clear();
        if (bonuses != null) {
            statBonuses.putAll(bonuses);
        }
        recalculateStats();
    }
public UIManager getUiManager() {
    return uiManager;
}
    // ============================================================
    // ПЕРЕСЧЁТ ИТОГОВЫХ СТАТОВ
    // ============================================================
    private void recalculateStats() {
        // Health
        float healthBonus = statBonuses.getOrDefault("max_health", 0f);
        finalMaxHealth = (int) (baseMaxHealth * (1f + healthBonus / 100f));
        if (finalMaxHealth < 1) {
            finalMaxHealth = 1;
        }
        if (finalHealth > finalMaxHealth) {
            finalHealth = finalMaxHealth;
        } else if (finalHealth <= 0) {
            finalHealth = finalMaxHealth;
        }

        // Mana
        float manaBonus = statBonuses.getOrDefault("max_mana", 0f);
        finalMaxMana = (int) (baseMaxMana * (1f + manaBonus / 100f));
        if (finalMaxMana < 1) {
            finalMaxMana = 1;
        }
        if (finalMana > finalMaxMana) {
            finalMana = finalMaxMana;
        } else if (finalMana <= 0) {
            finalMana = finalMaxMana;
        }

        // Defense
        physicalDefense = statBonuses.getOrDefault("physical_defense", 0f);
        magicalDefense = statBonuses.getOrDefault("magical_defense", 0f);
        armor = statBonuses.getOrDefault("armor", 0f);

        // Block
        blockChance = statBonuses.getOrDefault("block_chance", 0f);

        // Critical
        critChance = statBonuses.getOrDefault("crit_chance", 0f);
        critDamage = statBonuses.getOrDefault("crit_damage", 0f);

        // Attack speed
        attackSpeed = 1f + statBonuses.getOrDefault("attack_speed", 0f) / 100f;

        // Base damage (без учёта attack_7 – применяется в calculatePhysicalDamage)
baseDamage = 10f + equipmentDamage;

        // Heal
        healPower = statBonuses.getOrDefault("heal_power", 0f);
        incomingHeal = statBonuses.getOrDefault("incoming_heal", 0f);

        // Hit
        hitChance = statBonuses.getOrDefault("hit_chance", 0f);

        // Light
        holyDamagePercent = statBonuses.getOrDefault("holy_damage_percent", 0f);
        lightDamagePercent = statBonuses.getOrDefault("light_beam_damage", 0f);

        // Others
        shieldFromHeal = statBonuses.getOrDefault("shield_from_heal", 0f);
        manaOnHealPercent = statBonuses.getOrDefault("mana_on_heal_percent", 0f);
        damageIgnored = statBonuses.getOrDefault("damage_ignored", 0f);
        critDamageReduction = statBonuses.getOrDefault("crit_damage_reduction", 0f);
        whirlwindRadius = statBonuses.getOrDefault("whirlwind_radius", 0f);
        kickStunDuration = statBonuses.getOrDefault("kick_stun_duration", 0f);
        rageAttackSpeed = statBonuses.getOrDefault("rage_attack_speed", 0f);
    }
/**
 * Проверяет реальным рейкастом, есть ли уже физическая
 * коллизия (пол) в точке будущего спавна — а не просто
 * "появился хоть один дочерний объект в dungeonNode"
 * (монстры/декор могут появиться раньше пола, что делает
 * старую проверку по числу детей ненадёжной).
 */
private boolean isFloorReadyAt(Vector3f target) {

    if (physicsSpace == null || target == null) {
        return false;
    }

    Vector3f from = new Vector3f(target.x, target.y + 20f, target.z);
    Vector3f to = new Vector3f(target.x, target.y - 20f, target.z);

    List<PhysicsRayTestResult> results = physicsSpace.rayTest(from, to);

    return results != null && !results.isEmpty();
}

    private float getBonusStat(String key) {
        return statBonuses.getOrDefault(key, 0f);
    }
private Vector3f lockedApproachDir = null;
    // ============================================================
    // UPDATE (вызывается каждый кадр)
    // ============================================================
    public void update(float tpf) {

        clampUpwardVelocity();

        if (!isAlive) {
            deathTimer += tpf;
            if (deathTimer >= 0.3f && !isRespawning) {
                respawnPlayer();
            }
            return;
        }

        if (pendingTeleportPosition != null) {

            pendingTeleportSafetyTimer -= tpf;

            boolean floorReady = isFloorReadyAt(pendingTeleportPosition);

            if (floorReady || pendingTeleportSafetyTimer <= 0f) {

                setPosition(pendingTeleportPosition);

                if (characterControl != null) {
                    characterControl.getRigidBody().setLinearVelocity(Vector3f.ZERO);
                    characterControl.setWalkDirection(Vector3f.ZERO);
                }

                pendingTeleportPosition = null;
            }
        }

        // Временные эффекты талантов
        if (rageTimer > 0f) {
            rageTimer -= tpf;
            if (rageTimer <= 0f) {
                rageTimer = 0f;
                System.out.println("[Player] Rage ended.");
            }
        }
        if (angelicProtectionTimer > 0f) {
            angelicProtectionTimer -= tpf;
            if (angelicProtectionTimer < 0f) {
                angelicProtectionTimer = 0f;
            }
        }
        if (auraDefenseTimer > 0f) {
            auraDefenseTimer -= tpf;
            if (auraDefenseTimer < 0f) {
                auraDefenseTimer = 0f;
            }
        }

        // Вращение эффекта Whirlwind
        if (whirlwindActive && whirlwindEffectNode != null) {
            whirlwindEffectNode.rotate(0, tpf * 8f, 0);
        }

        // Skill animation
        if (skillAnimationPlaying) {
            if (!isActiveOneShot()) {
                finishSkillAnimation();
            }
            updatePositionOnly();
            return;
        }

        updatePositionOnly();

        // Regeneration (def_6)
        if (hasTalent("def_6") && currentTarget == null && !isMoving && finalHealth < finalMaxHealth) {
            float regenPerSecond = 0.02f * getTalentLevel("def_6") * finalMaxHealth;
            healthRegenerationAccumulator += regenPerSecond * tpf;
            if (healthRegenerationAccumulator >= 1f) {
                int healAmount = (int) healthRegenerationAccumulator;
                healthRegenerationAccumulator -= healAmount;
                finalHealth = Math.min(finalHealth + healAmount, finalMaxHealth);
            }
        } else {
            healthRegenerationAccumulator = 0f;
        }

        // Движение к целевой точке
// Движение к целевой точке
if (currentTarget == null && isMovingToTarget && targetPosition != null && characterControl != null) {
    Vector3f currentPos = characterControl.getRigidBody().getPhysicsLocation();

    float dx = targetPosition.x - currentPos.x;
    float dz = targetPosition.z - currentPos.z;
    float dist = FastMath.sqrt(dx * dx + dz * dz);

    float softArrivalRadius = 0.6f;

    /*
     * ОСТАНОВКА ПРИ ПРИБЫТИИ — этого блока не хватало,
     * из-за чего персонаж никогда не завершал движение
     * и вечно дрожал на месте на минимальной скорости.
     */
    if (dist < softArrivalRadius) {

        characterControl.setWalkDirection(Vector3f.ZERO);
        characterControl.getRigidBody().setLinearVelocity(Vector3f.ZERO);

        setMoving(false);
        isMovingToTarget = false;
        targetPosition = null;
        lockedApproachDir = null;
        stuckCheckTimer = 0f;
        lastStuckDist = -1f;
        playBaseAnimation(ANIM_IDLE);
        return;
    }

    /*
     * ДЕТЕКТОР ЗАСТРЕВАНИЯ — если прогресс к цели почти
     * не идёт (например, упёрлись в стену), останавливаемся
     * вместо бесконечной дрожи.
     */
    stuckCheckTimer += tpf;

    if (stuckCheckTimer >= STUCK_CHECK_INTERVAL) {

        if (lastStuckDist >= 0f) {

            float progress = lastStuckDist - dist;

            if (progress < STUCK_MIN_PROGRESS) {

                characterControl.setWalkDirection(Vector3f.ZERO);
                characterControl.getRigidBody().setLinearVelocity(Vector3f.ZERO);

                setMoving(false);
                isMovingToTarget = false;
                targetPosition = null;
                lockedApproachDir = null;
                stuckCheckTimer = 0f;
                lastStuckDist = -1f;

                playBaseAnimation(ANIM_IDLE);
                return;
            }
        }

        lastStuckDist = dist;
        stuckCheckTimer = 0f;
    }

    float slowdownZone = 1.5f;
    float speed = 6f;

    Vector3f dir;

    if (dist < slowdownZone) {

        if (lockedApproachDir == null) {

            Vector3f initialDir = new Vector3f(dx, 0, dz);

            if (initialDir.lengthSquared() > 0.0001f) {
                initialDir.normalizeLocal();
            }

            lockedApproachDir = initialDir;
        }

        dir = lockedApproachDir;

        float t = (dist - softArrivalRadius) / (slowdownZone - softArrivalRadius);
        t = Math.max(0.15f, Math.min(1f, t));
        speed *= t;

    } else {

        dir = new Vector3f(dx, 0, dz);

        if (dir.lengthSquared() > 0.0001f) {
            dir.normalizeLocal();
        }
    }

    if (dir != null && dir.lengthSquared() > 0.0001f) {

        characterControl.setWalkDirection(dir.mult(speed));
        setMoving(true);
        playBaseAnimation(ANIM_WALK);

    } else {

        characterControl.setWalkDirection(Vector3f.ZERO);
        characterControl.getRigidBody().setLinearVelocity(Vector3f.ZERO);
        setMoving(false);
        isMovingToTarget = false;
        targetPosition = null;
        lockedApproachDir = null;
        stuckCheckTimer = 0f;
        lastStuckDist = -1f;
        playBaseAnimation(ANIM_IDLE);
    }
    return;
}
if (currentTarget != null) {
            Vector3f targetPos = currentTarget.getWorldTranslation();
            Vector3f currentPos = characterControl != null
                    ? characterControl.getRigidBody().getPhysicsLocation()
                    : position;
            float dist = targetPos.distance(currentPos);

            if (dist <= attackRange) {
                if (isMoving) {
                    setMoving(false);
                    stopCharacterPhysics();
                }
                isAttacking = true;
                attackTimer -= tpf;
                if (attackTimer <= 0f) {
                    performAttack();
                    attackTimer = getCurrentAttackCooldown();
                }
            } else {
                Vector3f dir = new Vector3f(targetPos.x - currentPos.x, 0, targetPos.z - currentPos.z);
                if (dir.lengthSquared() > 0.0001f) {
                    dir.normalizeLocal();
                    characterControl.setWalkDirection(dir.mult(6f));
                    setMoving(true);
                    playBaseAnimation(ANIM_WALK);
                }
                isAttacking = false;
                attackTimer = 0f;
            }
            return;
        }

        // Idle
        if (!isMoving && !isAttacking) {
            playBaseAnimation(ANIM_IDLE);
        }
    }
private float stuckCheckTimer = 0f;
private float lastStuckDist = -1f;

private static final float STUCK_CHECK_INTERVAL = 0.4f;
private static final float STUCK_MIN_PROGRESS = 0.08f;
    // ============================================================
    // ПОЗИЦИЯ (обновление плавного перемещения)
    // ============================================================
    private void updatePositionOnly() {
        if (characterControl == null) {
            return;
        }
        Vector3f physPos = playerNode.getWorldTranslation();
        smoothPosition.interpolateLocal(physPos, interpolationSpeed);
        playerNode.setLocalTranslation(smoothPosition);
        position.set(smoothPosition);
    }

    // ============================================================
    // ЭФФЕКТЫ (Heal)
    // ============================================================
    private void createHealEffect() {
        if (effectNode == null) {
            effectNode = new Node("EffectNode");
            playerNode.attachChild(effectNode);
        }

        Material particleMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Particle.j3md");
healParticles = new ParticleEmitter("HealParticles", ParticleMesh.Type.Triangle, 220);
healParticles.setMaterial(particleMat);
        healParticles.setImagesX(1);
        healParticles.setImagesY(1);
        healParticles.setShape(new EmitterSphereShape(Vector3f.ZERO, 1.2f));
healParticles.setStartSize(0.05f);
healParticles.setEndSize(0.01f);
        healParticles.setStartColor(new ColorRGBA(0.2f, 1f, 0.2f, 0.8f));
        healParticles.setEndColor(new ColorRGBA(0f, 0.5f, 0f, 0f));

        ParticleInfluencer influencer = healParticles.getParticleInfluencer();
        influencer.setInitialVelocity(new Vector3f(0, -1.5f, 0));
        influencer.setVelocityVariation(0.5f);

        healParticles.setLowLife(0.6f);
        healParticles.setHighLife(1.0f);
        healParticles.setGravity(Vector3f.ZERO);
        healParticles.setParticlesPerSec(0);
        healParticles.setInWorldSpace(false);
        healParticles.setLocalTranslation(0, 1.5f, 0);
        healParticles.setEnabled(false);

        effectNode.attachChild(healParticles);
    }

    private void emitHealParticles() {
        if (healParticles == null) {
            return;
        }
        healParticles.killAllParticles();
        healParticles.setEnabled(true);
        healParticles.emitAllParticles();

        new Thread(() -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {
            }
            app.enqueue(() -> {
                if (healParticles != null) {
                    healParticles.setEnabled(false);
                    healParticles.killAllParticles();
                }
                return null;
            });
        }).start();
    }

    // ============================================================
    // ЭФФЕКТ WHIRLWIND
    // ============================================================
    private void createWhirlwindEffect() {
        if (effectNode == null) {
            effectNode = new Node("EffectNode");
            playerNode.attachChild(effectNode);
        }

        whirlwindEffectNode = new Node("WhirlwindEffectNode");
        effectNode.attachChild(whirlwindEffectNode);
        whirlwindEffectNode.setLocalTranslation(0, 1.2f, 0);

        Material particleMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Particle.j3md");

        whirlwindParticles = new ParticleEmitter("WhirlwindParticles", ParticleMesh.Type.Triangle, 220);        whirlwindParticles.setMaterial(particleMat);
        whirlwindParticles.setImagesX(1);
        whirlwindParticles.setImagesY(1);
        whirlwindParticles.setStartColor(new ColorRGBA(1f, 1f, 1f, 0.7f));
        whirlwindParticles.setEndColor(new ColorRGBA(1f, 1f, 1f, 0f));
whirlwindParticles.setStartSize(0.05f);
whirlwindParticles.setEndSize(0.01f);
        whirlwindParticles.setShape(new EmitterSphereShape(Vector3f.ZERO, 0.1f));

        ParticleInfluencer inf = whirlwindParticles.getParticleInfluencer();
        inf.setInitialVelocity(new Vector3f(0, 2.5f, 0));
        inf.setVelocityVariation(0.6f);

        whirlwindParticles.setGravity(new Vector3f(0, -2.0f, 0));
        whirlwindParticles.setLowLife(0.6f);
        whirlwindParticles.setHighLife(1.2f);
        whirlwindParticles.setParticlesPerSec(15);
        whirlwindParticles.setEnabled(false);
        whirlwindParticles.setLocalTranslation(1.8f, 0, 0);

        whirlwindEffectNode.attachChild(whirlwindParticles);
    }

    // ============================================================
    // ЭФФЕКТ КРОВИ (НОВОЕ)
    // ============================================================
    private void createBloodEffect() {
        if (effectNode == null) {
            effectNode = new Node("EffectNode");
            playerNode.attachChild(effectNode);
        }

        bloodEffectNode = new Node("BloodEffectNode");
        effectNode.attachChild(bloodEffectNode);
        bloodEffectNode.setLocalTranslation(0, 1.0f, 0); // примерно на уровне груди

        Material particleMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Particle.j3md");
        // Можно загрузить текстуру крови, но оставим точки

        bloodEmitter = new ParticleEmitter("BloodEmitter", ParticleMesh.Type.Triangle, 30);
        bloodEmitter.setMaterial(particleMat);
        bloodEmitter.setImagesX(1);
        bloodEmitter.setImagesY(1);
        bloodEmitter.setStartColor(new ColorRGBA(0.8f, 0.0f, 0.0f, 0.9f)); // красный
        bloodEmitter.setEndColor(new ColorRGBA(0.4f, 0.0f, 0.0f, 0.0f));
        bloodEmitter.setStartSize(0.12f);
        bloodEmitter.setEndSize(0.04f);

        // Используем точечный эмиттер (из одной точки)
        bloodEmitter.setShape(new EmitterPointShape(Vector3f.ZERO));

        // Скорость будет задаваться динамически, поэтому оставим стандартный Influencer
        // и будем менять initialVelocity при каждом выбросе
        ParticleInfluencer inf = bloodEmitter.getParticleInfluencer();
        inf.setInitialVelocity(new Vector3f(0, 0, 0)); // временно
        inf.setVelocityVariation(0.3f);

        bloodEmitter.setGravity(new Vector3f(0, -5.0f, 0)); // сильная гравитация для падения
        bloodEmitter.setLowLife(0.3f);
        bloodEmitter.setHighLife(0.8f);
        bloodEmitter.setParticlesPerSec(0); // не непрерывный, а однократный выброс
        bloodEmitter.setEnabled(false);

        // Устанавливаем в мировое пространство, чтобы частицы не двигались с персонажем?
        // В данном случае лучше false, т.к. эмиттер привязан к персонажу, но направление будем задавать локально.
        // Однако гравитация действует в мировом пространстве, поэтому частицы будут падать вниз.
        bloodEmitter.setInWorldSpace(false);

        bloodEffectNode.attachChild(bloodEmitter);
    }

    private void emitBloodTowardsTarget() {
        if (bloodEmitter == null || currentTarget == null) return;
        if (!isAlive) return;

        // Получаем позицию персонажа и цели
        Vector3f playerPos = getPosition();
        Vector3f targetPos = currentTarget.getWorldTranslation();

        // Направление от персонажа к цели (нормализованное)
        Vector3f direction = targetPos.subtract(playerPos);
        direction.y = 0; // игнорируем разницу по высоте для направления
        direction.normalizeLocal();

        // Добавляем небольшой вертикальный компонент вверх, чтобы струя немного взлетала
        Vector3f velocity = direction.mult(4.0f).add(0, 1.5f, 0);

        // Настраиваем скорость частиц
        ParticleInfluencer inf = bloodEmitter.getParticleInfluencer();
        inf.setInitialVelocity(velocity);

        // Включаем и выбрасываем частицы
        bloodEmitter.killAllParticles();
        bloodEmitter.setEnabled(true);
        bloodEmitter.emitAllParticles();

        // Автоматически отключаем через некоторое время (как в heal)
        new Thread(() -> {
            try {
                Thread.sleep(600); // 0.6 сек
            } catch (InterruptedException ignored) {}
            app.enqueue(() -> {
                if (bloodEmitter != null) {
                    bloodEmitter.setEnabled(false);
                    bloodEmitter.killAllParticles();
                }
                return null;
            });
        }).start();
    }

    // ============================================================
    // UI
    // ============================================================
    private void updateUI() {
        if (uiManager != null) {
            uiManager.updatePlayerStats();
            uiManager.updatePotionCounts();
        }
    }

    // ============================================================
    // GETTERS / SETTERS
    // ============================================================
    public Node getPlayerNode() {
        return playerNode;
    }

    public Vector3f getPosition() {
        return position;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getLevel() {
        return baseLevel;
    }

    public int getHealth() {
        return finalHealth;
    }

    public int getMaxHealth() {
        return finalMaxHealth;
    }

    public int getMana() {
        return finalMana;
    }

    public int getMaxMana() {
        return finalMaxMana;
    }

    public int getExperience() {
        return experience;
    }

    public int getGold() {
        return gold;
    }

    public void setGold(int gold) {
        this.gold = Math.max(0, gold);
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean alive) {
        this.isAlive = alive;
    }

    public AnimComposer getAnimComposer() {
        return animComposer;
    }

    public SkinningControl getSkinningControl() {
        return skinningControl;
    }

    public Spatial getCurrentTarget() {
        return currentTarget;
    }

    public BetterCharacterControl getCharacterControl() {
        return characterControl;
    }

    public boolean isSkillAnimationPlaying() {
        return skillAnimationPlaying;
    }

    public String getSkillAnimationName() {
        return skillAnimationName;
    }

    public String getCurrentAnimation() {
        return currentAnimation;
    }

    public float getPhysicalDefense() {
        return physicalDefense;
    }

    public float getMagicalDefense() {
        return magicalDefense;
    }

    public float getArmor() {
        return armor;
    }

    public float getCritChance() {
        return critChance;
    }

    public float getCritDamage() {
        return critDamage;
    }

    public float getAttackSpeed() {
        return attackSpeed;
    }

    public float getBaseDamage() {
        return baseDamage;
    }

    public void setPlayerName(String name) {
        this.playerName = name;
    }

    public void setLevel(int level) {
        this.baseLevel = level;
    }

    public void setHealth(int hp) {
        this.finalHealth = Math.max(0, Math.min(hp, finalMaxHealth));
    }

    public void setMaxHealth(int maxHp) {
        this.baseMaxHealth = Math.max(1, maxHp);
        recalculateStats();
    }

    public void setMana(int mana) {
        this.finalMana = Math.max(0, Math.min(mana, finalMaxMana));
    }

    public void setMaxMana(int maxMana) {
        this.baseMaxMana = Math.max(1, maxMana);
        recalculateStats();
    }

    public void setExperience(int exp) {
        this.experience = exp;
    }

public void setPosition(Vector3f pos) {
    if (pos == null) {
        return;
    }

    /*
     * Любая прямая установка позиции отменяет ранее
     * запрошенный отложенный телепорт (requestTeleport).
     * Иначе "зависший" pending-запрос со старыми
     * координатами (например, от входа в данж, который
     * не успел завершиться до смерти игрока) может
     * перезаписать корректную позицию после респавна
     * и утащить персонажа в пустоту несуществующей сцены.
     */
    pendingTeleportPosition = null;

    this.position.set(pos);
    smoothPosition.set(pos);
    if (characterControl != null) {
        characterControl.warp(pos);
    }
    playerNode.setLocalTranslation(pos);
}

    public void setWorldManager(WorldManager wm) {
        this.worldManager = wm;
    }

    public void setDropManager(DropManager dm) {
        this.dropManager = dm;
    }

    public void setUIManager(UIManager ui) {
        this.uiManager = ui;
    }

    public void setNetworkManager(NetworkManager nm) {
        this.networkManager = nm;
    }

    public String getCurrentDungeon() {
        return currentDungeonId;
    }

    public void setCurrentDungeon(String dungeonId) {
        this.currentDungeonId = dungeonId;
    }

    public int getCurrentDifficulty() {
        return currentDifficulty;
    }

    public void setCurrentDifficulty(int difficulty) {
        this.currentDifficulty = difficulty;
    }

    // ============================================================
    // DUNGEON PROGRESS
    // ============================================================
    public void updateDungeonProgress(String newDungeonId, int newDifficulty) {
        this.currentDungeonId = newDungeonId;
        this.currentDifficulty = newDifficulty;
        if (networkManager != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("currentDungeon", currentDungeonId);
            data.put("difficulty", currentDifficulty);
            networkManager.saveCharacter(data);
        }
        System.out.println("[PlayerManager] Progress updated: Dungeon=" + currentDungeonId + ", Difficulty=" + currentDifficulty);
    }

    // ============================================================
    // ОЧИСТКА РЕСУРСОВ
    // ============================================================
    public void cleanup() {
        if (characterControl != null && characterControl.getPhysicsSpace() != null) {
            characterControl.getPhysicsSpace().remove(characterControl);
        }
        if (playerNode != null) {
            app.getRootNode().detachChild(playerNode);
        }
    }
    public Vector3f getViewDirection() {
    if (characterControl != null) {
        return characterControl.getViewDirection();
    }
    return Vector3f.UNIT_Z;
}
    // ============================================================
    // КОНСТАНТЫ РАЗМЕРА ПЕРСОНАЖА (используются в createPhysicsBody)
    // ============================================================

    private static final float CHARACTER_RADIUS = 0.4f;
    private static final float CHARACTER_HEIGHT = 1.8f;

    public float getBlockChance() {
        return blockChance;
    }

    public float getHealPower() {
        return healPower;
    }

    public float getIncomingHeal() {
        return incomingHeal;
    }

    public float getHitChance() {
        return hitChance;
    }

    public float getHolyDamagePercent() {
        return holyDamagePercent;
    }

    public float getLightDamagePercent() {
        return lightDamagePercent;
    }

    public float getShieldFromHeal() {
        return shieldFromHeal;
    }

    public float getManaOnHealPercent() {
        return manaOnHealPercent;
    }

    public float getDamageIgnored() {
        return damageIgnored;
    }

    public float getCritDamageReduction() {
        return critDamageReduction;
    }

    public float getWhirlwindRadius() {
        return whirlwindRadius;
    }

    public float getKickStunDuration() {
        return kickStunDuration;
    }

    public float getRageAttackSpeed() {
        return rageAttackSpeed;
    }
    // ============================================================
    // ТЕЛЕПОРТАЦИЯ (ожидание готовности пола перед применением)
    // ============================================================

    private PhysicsSpace physicsSpace;
    private Vector3f pendingTeleportPosition = null;
    private float pendingTeleportSafetyTimer = 0f;
    private String lastDungeonPositionId = null;

    public void setPhysicsSpace(PhysicsSpace space) {
        this.physicsSpace = space;
        if (characterControl != null && space != null) {
            space.add(characterControl);
        }
    }

    /**
     * Запрашивает телепортацию, которая применится не сразу,
     * а как только физика пола в целевой точке реально появится
     * в сцене (проверяется рейкастом каждый кадр в update()).
     * Решает гонку с асинхронной загрузкой данжа.
     */
    public void requestTeleport(Vector3f pos) {
        if (pos == null) return;
        pendingTeleportPosition = pos.clone();
        pendingTeleportSafetyTimer = 3f;
    }

    /**
     * Сохраняет позицию ТОЛЬКО вместе с ID данжа, к которому
     * она относится — координаты из одной сцены никогда
     * не используются как точка спавна в другой.
     */
    public void setLastDungeonPosition(String dungeonId, Vector3f pos) {
        if (pos != null && dungeonId != null) {
            lastDungeonPosition.set(pos);
            lastDungeonPositionId = dungeonId;
        }
    }

    /**
     * Возвращает сохранённую позицию, только если она относится
     * именно к запрошенному данжу — иначе null (нужно использовать
     * безопасную точку спавна по умолчанию).
     */
    public Vector3f getLastDungeonPositionFor(String dungeonId) {
        if (dungeonId != null && dungeonId.equals(lastDungeonPositionId)) {
            return lastDungeonPosition;
        }
        return null;
    }
}