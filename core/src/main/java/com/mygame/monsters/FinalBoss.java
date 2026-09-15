package com.mygame.monsters;

import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.effect.shapes.EmitterShape;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.mygame.items.ItemGenerator;
import com.mygame.items.LootTable;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Финальный босс.
 *
 * Способности:
 *
 * 1. Круг:
 *    - красная зона на земле;
 *    - 2 секунды подготовки;
 *    - босс стоит;
 *    - после подготовки из всей площади круга
 *      вырываются красные частицы вверх.
 *
 * 2. Огненный треугольник:
 *    - красный треугольник на земле;
 *    - 2 секунды подготовки;
 *    - направление фиксируется в момент начала каста;
 *    - после подготовки огонь летит вперёд.
 *
 * После каста:
 *    - AI разблокируется;
 *    - если игрок убежал -> Run;
 *    - если игрок рядом -> Idle.
 */
public class FinalBoss extends MeleMonster {

    // ============================================================
    // НАСТРОЙКИ
    // ============================================================

    /**
     * Интервал между началами способностей.
     */
    private static final float ABILITY_INTERVAL = 5f;

    /**
     * Время предупреждения перед срабатыванием.
     */
    private static final float ABILITY_DURATION = 2.0f;

    /**
     * Радиус круга.
     */
    private static final float CIRCLE_RADIUS = 4.0f;

    /**
     * Длина огненного треугольника.
     */
    private static final float CONE_LENGTH = 6.0f;

    /**
     * Половина ширины треугольника в конце.
     */
    private static final float CONE_RADIUS = 3.0f;

    /**
     * Высота визуальных зон над землёй.
     */
    private static final float GROUND_HEIGHT = 0.06f;

    // ============================================================
    // ТАЙМЕРЫ
    // ============================================================

    private float abilityCooldownTimer = 3.5f;

    private float abilityDurationTimer = 0.0f;

    // ============================================================
    // СПОСОБНОСТИ
    // ============================================================

    /**
     * false = круг.
     * true  = треугольник.
     */
    private boolean nextConeAbility = false;

    /**
     * 0 = нет способности.
     * 1 = круг.
     * 2 = треугольник.
     */
    private int abilityType = 0;

    /**
     * Сейчас идёт подготовка способности.
     */
    private boolean isCasting = false;

    // ============================================================
    // ЗАФИКСИРОВАННОЕ СОСТОЯНИЕ КАСТА
    // ============================================================

    /**
     * Позиция босса в момент начала каста.
     *
     * Все визуальные эффекты и урон способности
     * используют именно эту позицию.
     */
    private Vector3f abilityStartPosition =
            Vector3f.ZERO.clone();

    /**
     * Направление треугольника.
     *
     * Фиксируется при начале каста.
     */
    private Vector3f abilityDirection =
            Vector3f.UNIT_Z.clone();

    // ============================================================
    // ВИЗУАЛЬНАЯ ЗОНА
    // ============================================================

    private Geometry areaMesh = null;

    // ============================================================
    // ЧАСТИЦЫ
    // ============================================================

    private ParticleEmitter particleEmitter = null;

    private Node particleNode = null;

    private float particleTimer = 0.0f;

    private Texture2D fireParticleTexture = null;

    // ============================================================
    // КОНСТРУКТОР
    // ============================================================

    public FinalBoss() {

        setId("finalboss");
        setName("finalboss");

        setLevel(1);

        setMaxHealth(1130);
        setHealth(1130);

        setDamage(15);

        /*
         * Босс начинает способность только когда
         * игрок находится на расстоянии атаки.
         */
        setAttackRange(1.5f);

        setMoveSpeed(2.0f);
        setAggroRange(8.0f);

        setBoss(true);

        // ========================================================
        // LOOT
        // ========================================================

       LootTable loot = new LootTable();
loot.addEntry("Weapon", 0.25f);
loot.addEntry("Helmet", 0.2f);
loot.addEntry("Chest", 0.2f);
loot.addEntry("Shield", 0.15f);
loot.addEntry("Legs", 0.2f);
loot.addEntry("Boots", 0.15f);
loot.addEntry("Gloves", 0.15f);
loot.addEntry("Gem", 1.0f);
loot.addEntry("Gem", 1.0f);
loot.addEntry("Gem", 1.0f);
setLootTable(loot);
    }

    // ============================================================
    // UPDATE
    // ============================================================

    @Override
    public void update(float tpf) {

        super.update(tpf);

        // --------------------------------------------------------
        // Частицы
        // --------------------------------------------------------

        updateParticles(tpf);

        // --------------------------------------------------------
        // Мёртв
        // --------------------------------------------------------

        if (!isAlive()) {

            removeAbilityVisual();
            removeParticles();

            return;
        }

        // --------------------------------------------------------
        // Оглушён
        // --------------------------------------------------------

        if (isStunned()) {

            return;
        }

        // --------------------------------------------------------
        // ИДЁТ КАСТ
        // --------------------------------------------------------

        if (isCasting) {

            /*
             * Босс должен стоять на месте всё время каста.
             *
             * Это также защищает от ситуации,
             * когда физика сдвигает босса.
             */
            setPosition(
                    abilityStartPosition
            );
            playAnimation("Idle");
            abilityDurationTimer -= tpf;

            if (abilityDurationTimer <= 0.0f) {

                finishAbility();
            }

            return;
        }

        // --------------------------------------------------------
        // ТАЙМЕР
        // --------------------------------------------------------

        abilityCooldownTimer -= tpf;

        if (abilityCooldownTimer <= 0.0f) {

            abilityCooldownTimer = 0.0f;

            /*
             * Способность начинается только вплотную.
             */
            if (isPlayerInAttackRange()) {

                startAbility();
            }
        }
    }

    // ============================================================
    // ПРОВЕРКА ДИСТАНЦИИ
    // ============================================================

    

    // ============================================================
    // НАЧАЛО СПОСОБНОСТИ
    // ============================================================

    private void startAbility() {

        if (isCasting) {

            return;
        }

        /*
         * Дополнительная защита.
         */
        if (!isPlayerInAttackRange()) {

            return;
        }

        // ========================================================
        // КАСТ
        // ========================================================

        isCasting = true;

        abilityDurationTimer =
                ABILITY_DURATION;

        // ========================================================
        // ФИКСИРУЕМ ПОЗИЦИЮ
        // ========================================================

        abilityStartPosition =
                getPosition().clone();

        // ========================================================
        // ОСТАНАВЛИВАЕМ AI
        // ========================================================

        if (getAI() != null) {

            getAI().setStunned(true);
        }

        /*
         * Только во время подготовки.
         */
        playAnimation("Idle");

        // ========================================================
        // ВЫБОР СПОСОБНОСТИ
        // ========================================================

        if (nextConeAbility) {

            abilityType = 2;

        } else {

            abilityType = 1;
        }

        nextConeAbility =
                !nextConeAbility;

        // ========================================================
        // ПОЗИЦИЯ
        // ========================================================

        Vector3f bossPos =
                abilityStartPosition.clone();

        Vector3f playerPos;

        if (getPlayerManager() != null) {

            playerPos =
                    getPlayerManager()
                            .getPosition()
                            .clone();

        } else {

            playerPos =
                    bossPos.add(
                            Vector3f.UNIT_Z
                    );
        }

        // ========================================================
        // КРУГ
        // ========================================================

        if (abilityType == 1) {

            createCircleVisual(
                    bossPos,
                    CIRCLE_RADIUS
            );

        }

        // ========================================================
        // ТРЕУГОЛЬНИК
        // ========================================================

        else {

            Vector3f direction =
                    playerPos.subtract(
                            bossPos
                    );

            direction.y = 0.0f;

            if (direction.lengthSquared() <
                    0.000001f) {

                direction.set(
                        Vector3f.UNIT_Z
                );

            } else {

                direction.normalizeLocal();
            }

            /*
             * Направление запоминаем.
             *
             * Если игрок убежит во время подготовки,
             * треугольник НЕ будет поворачиваться.
             */
            abilityDirection =
                    direction.clone();

            createConeVisual(
                    bossPos,
                    abilityDirection,
                    CONE_LENGTH,
                    CONE_RADIUS
            );
        }
    }

    // ============================================================
    // КРУГ НА ЗЕМЛЕ
    // ============================================================

    private void createCircleVisual(
            Vector3f center,
            float radius
    ) {

        removeAbilityVisual();

        /*
         * ВАЖНО:
         *
         * Здесь НЕ используется Cylinder.
         *
         * Мы создаём плоский диск непосредственно
         * в плоскости XZ.
         *
         * Поэтому он физически горизонтальный:
         *
         *             Y
         *             |
         *             |
         *             +-------- X
         *            /
         *           Z
         *
         * Никакого Quaternion для круга не требуется.
         */

        Mesh mesh =
                createHorizontalCircleMesh(
                        radius,
                        64
                );

        areaMesh =
                new Geometry(
                        "CircleAbility",
                        mesh
                );

        // ========================================================
        // МАТЕРИАЛ
        // ========================================================

        Material mat =
                new Material(
                        app.getAssetManager(),
                        "Common/MatDefs/Misc/Unshaded.j3md"
                );

        mat.setColor(
                "Color",
                new ColorRGBA(
                        1.0f,
                        0.0f,
                        0.0f,
                        0.42f
                )
        );

        mat.getAdditionalRenderState()
                .setBlendMode(
                        RenderState.BlendMode.Alpha
                );

        mat.getAdditionalRenderState()
                .setDepthWrite(false);

        mat.getAdditionalRenderState()
                .setDepthTest(true);

        /*
         * Видим сверху и снизу.
         */
        mat.getAdditionalRenderState()
                .setFaceCullMode(
                        RenderState.FaceCullMode.Off
                );

        areaMesh.setMaterial(mat);

        areaMesh.setQueueBucket(
                RenderQueue.Bucket.Translucent
        );

        // ========================================================
        // ПОЗИЦИЯ
        // ========================================================

        areaMesh.setLocalTranslation(
                center.x,
                GROUND_HEIGHT,
                center.z
        );

        /*
         * rotation НЕ задаём.
         *
         * Mesh уже создан горизонтально.
         */

        Node parent =
                getModelNode().getParent();

        if (parent != null) {

            parent.attachChild(
                    areaMesh
            );
        }
    }

    // ============================================================
    // СОЗДАНИЕ ГОРИЗОНТАЛЬНОГО КРУГА
    // ============================================================

    private Mesh createHorizontalCircleMesh(
            float radius,
            int segments
    ) {

        /*
         * Один центральный vertex.
         *
         * Остальные vertices идут по окружности.
         */
        float[] positions =
                new float[
                        (segments + 1) * 3
                ];

        short[] indices =
                new short[
                        segments * 3
                ];

        // --------------------------------------------------------
        // ЦЕНТР
        // --------------------------------------------------------

        positions[0] = 0.0f;
        positions[1] = 0.0f;
        positions[2] = 0.0f;

        // --------------------------------------------------------
        // ОКРУЖНОСТЬ
        // --------------------------------------------------------

        for (int i = 0; i < segments; i++) {

            float angle =
                    FastMath.TWO_PI *
                    ((float) i /
                            (float) segments);

            float x =
                    FastMath.cos(angle) *
                    radius;

            float z =
                    FastMath.sin(angle) *
                    radius;

            int index =
                    (i + 1) * 3;

            positions[index] =
                    x;

            positions[index + 1] =
                    0.0f;

            positions[index + 2] =
                    z;
        }

        // --------------------------------------------------------
        // ТРЕУГОЛЬНИКИ
        // --------------------------------------------------------

        for (int i = 0; i < segments; i++) {

            int next =
                    (i + 1) %
                            segments;

            int index =
                    i * 3;

            /*
             * Центр
             * текущая точка
             * следующая точка
             */
            indices[index] =
                    0;

            indices[index + 1] =
                    (short) (i + 1);

            indices[index + 2] =
                    (short) (next + 1);
        }

        Mesh mesh =
                new Mesh();

        mesh.setMode(
                Mesh.Mode.Triangles
        );

        mesh.setBuffer(
                VertexBuffer.Type.Position,
                3,
                positions
        );

        mesh.setBuffer(
                VertexBuffer.Type.Index,
                3,
                indices
        );

        mesh.updateBound();

        return mesh;
    }

    // ============================================================
    // ТРЕУГОЛЬНИК НА ЗЕМЛЕ
    // ============================================================

    private void createConeVisual(
            Vector3f origin,
            Vector3f direction,
            float length,
            float radius
    ) {

        removeAbilityVisual();

        Vector3f dir =
                direction.clone();

        dir.y = 0.0f;

        if (dir.lengthSquared() <
                0.000001f) {

            dir.set(
                    Vector3f.UNIT_Z
            );
        }

        dir.normalizeLocal();

        // ========================================================
        // ПЕРПЕНДИКУЛЯРНЫЙ ВЕКТОР
        // ========================================================

        Vector3f side =
                new Vector3f(
                        -dir.z,
                        0.0f,
                        dir.x
                );

        side.normalizeLocal();

        // ========================================================
        // ВЕРШИНА
        // ========================================================

        Vector3f p0 =
                dir.mult(
                        0.25f
                );

        // ========================================================
        // КОНЕЦ
        // ========================================================

        Vector3f forward =
                dir.mult(
                        length
                );

        Vector3f p1 =
                forward.add(
                        side.mult(radius)
                );

        Vector3f p2 =
                forward.subtract(
                        side.mult(radius)
                );

        // ========================================================
        // ВСЕ ТОЧКИ НА ЗЕМЛЕ
        // ========================================================

        p0.y = GROUND_HEIGHT;

        p1.y = GROUND_HEIGHT;

        p2.y = GROUND_HEIGHT;

        // ========================================================
        // MESH
        // ========================================================

        Mesh mesh =
                new Mesh();

        mesh.setMode(
                Mesh.Mode.Triangles
        );

        mesh.setBuffer(
                VertexBuffer.Type.Position,
                3,
                new float[]{

                        p0.x,
                        p0.y,
                        p0.z,

                        p1.x,
                        p1.y,
                        p1.z,

                        p2.x,
                        p2.y,
                        p2.z
                }
        );

        mesh.setBuffer(
                VertexBuffer.Type.Index,
                3,
                new short[]{
                        0,
                        1,
                        2
                }
        );

        mesh.updateBound();

        areaMesh =
                new Geometry(
                        "ConeAbility",
                        mesh
                );

        // ========================================================
        // MATERIAL
        // ========================================================

        Material mat =
                new Material(
                        app.getAssetManager(),
                        "Common/MatDefs/Misc/Unshaded.j3md"
                );

        mat.setColor(
                "Color",
                new ColorRGBA(
                        1.0f,
                        0.0f,
                        0.0f,
                        0.45f
                )
        );

        mat.getAdditionalRenderState()
                .setBlendMode(
                        RenderState.BlendMode.Alpha
                );

        mat.getAdditionalRenderState()
                .setDepthWrite(false);

        mat.getAdditionalRenderState()
                .setDepthTest(true);

        mat.getAdditionalRenderState()
                .setFaceCullMode(
                        RenderState.FaceCullMode.Off
                );

        areaMesh.setMaterial(mat);

        areaMesh.setQueueBucket(
                RenderQueue.Bucket.Translucent
        );

        // ========================================================
        // ПОЗИЦИЯ
        // ========================================================

        areaMesh.setLocalTranslation(
                origin.x,
                0.0f,
                origin.z
        );

        Node parent =
                getModelNode().getParent();

        if (parent != null) {

            parent.attachChild(
                    areaMesh
            );
        }
    }

    // ============================================================
    // ЗАВЕРШЕНИЕ СПОСОБНОСТИ
    // ============================================================

    private void finishAbility() {

        /*
         * Используем ТОЧКУ НАЧАЛА КАСТА.
         *
         * Не текущую позицию босса.
         */
        Vector3f abilityPos =
                abilityStartPosition.clone();

        // ========================================================
        // УДАЛЯЕМ ЗОНУ
        // ========================================================

        removeAbilityVisual();

        // ========================================================
        // ПОЗИЦИЯ ИГРОКА
        // ========================================================

        Vector3f playerPos;

        if (getPlayerManager() != null) {

            playerPos =
                    getPlayerManager()
                            .getPosition()
                            .clone();

        } else {

            playerPos =
                    abilityPos.clone();
        }

        // ========================================================
        // КРУГ
        // ========================================================

        if (abilityType == 1) {

            float dx =
                    playerPos.x -
                    abilityPos.x;

            float dz =
                    playerPos.z -
                    abilityPos.z;

            float distance =
                    FastMath.sqrt(
                            dx * dx +
                            dz * dz
                    );

            if (distance <=
                    CIRCLE_RADIUS) {

                applyDamageToPlayer();
            }

            // ----------------------------------------------------
            // ОГОНЬ
            // ----------------------------------------------------

            spawnFireParticles(
                    true,
                    abilityPos,
                    Vector3f.UNIT_Y
            );
        }

        // ========================================================
        // ТРЕУГОЛЬНИК
        // ========================================================

        else {

            Vector3f toPlayer =
                    playerPos.subtract(
                            abilityPos
                    );

            toPlayer.y = 0.0f;

            float dist =
                    toPlayer.length();

            if (
                    dist <= CONE_LENGTH
                    &&
                    dist > 0.0001f
            ) {

                Vector3f dir =
                        toPlayer.normalizeLocal();

                float angle =
                        dir.angleBetween(
                                abilityDirection
                        );

                float coneAngle =
                        FastMath.atan(
                                CONE_RADIUS /
                                        CONE_LENGTH
                        );

                if (angle <=
                        coneAngle) {

                    applyDamageToPlayer();
                }
            }

            spawnFireParticles(
                    false,
                    abilityPos,
                    abilityDirection
            );
        }

        // ========================================================
        // КАСТ ЗАКОНЧЕН
        // ========================================================

        isCasting = false;

        // ========================================================
        // РАЗБЛОКИРУЕМ AI
        // ========================================================

        if (getAI() != null) {

            getAI().setStunned(false);
        }

        // ========================================================
        // ВОЗВРАЩАЕМ АНИМАЦИЮ
        // ========================================================

        /*
         * Очень важно.
         *
         * Если игрок убежал за время каста,
         * босс должен сразу начать Run.
         *
         * Если игрок остался рядом,
         * оставляем Idle.
         */
        if (!isPlayerInAttackRange()) {

            playAnimation("Walk");

        } else {

            playAnimation("Idle");
        }

        // ========================================================
        // ТАЙМЕР
        // ========================================================

        abilityCooldownTimer =
                ABILITY_INTERVAL -
                        ABILITY_DURATION;

        if (abilityCooldownTimer < 0.0f) {

            abilityCooldownTimer = 0.0f;
        }
    }

    // ============================================================
    // УРОН
    // ============================================================

    private void applyDamageToPlayer() {

        if (getPlayerManager() == null) {

            return;
        }

        int damage =
                Math.round(
                        getDamage() * 2
                );

        getPlayerManager().takeDamage(
                damage
        );
    }

    // ============================================================
    // ОГНЕННЫЕ ЧАСТИЦЫ
    // ============================================================

    private void spawnFireParticles(
            boolean isCircle,
            Vector3f pos,
            Vector3f direction
    ) {

        if (app == null) {

            return;
        }

        // ========================================================
        // УДАЛЯЕМ СТАРЫЕ
        // ========================================================

        removeParticles();

        // ========================================================
        // ТЕКСТУРА
        // ========================================================

        if (fireParticleTexture == null) {

            fireParticleTexture =
                    createFireParticleTexture();
        }

        // ========================================================
        // КОЛИЧЕСТВО
        // ========================================================

        int particleCount;

        if (isCircle) {

            particleCount = 220;

        } else {

            particleCount = 220;
        }

        // ========================================================
        // EMITTER
        // ========================================================

        particleEmitter =
                new ParticleEmitter(
                        "FinalBossFire",
                        ParticleMesh.Type.Triangle,
                        particleCount
                );

        // ========================================================
        // MATERIAL
        // ========================================================

        Material mat =
                new Material(
                        app.getAssetManager(),
                        "Common/MatDefs/Misc/Particle.j3md"
                );

        mat.setTexture(
                "Texture",
                fireParticleTexture
        );

        mat.getAdditionalRenderState()
                .setBlendMode(
                        RenderState.BlendMode.Alpha
                );

        mat.getAdditionalRenderState()
                .setDepthWrite(false);

        mat.getAdditionalRenderState()
                .setDepthTest(true);

        particleEmitter.setMaterial(mat);

        particleEmitter.setQueueBucket(
                RenderQueue.Bucket.Translucent
        );

        // ========================================================
        // КРАСНЫЙ
        // ========================================================

        particleEmitter.setStartColor(
                new ColorRGBA(
                        1.0f,
                        0.0f,
                        0.0f,
                        1.0f
                )
        );

        particleEmitter.setEndColor(
                new ColorRGBA(
                        0.65f,
                        0.0f,
                        0.0f,
                        0.0f
                )
        );

        // ========================================================
        // РАЗМЕР
        // ========================================================

        if (isCircle) {

            particleEmitter.setStartSize(
                    1.15f
            );

            particleEmitter.setEndSize(
                    0.20f
            );

        } else {

            particleEmitter.setStartSize(
                    1.0f
            );

            particleEmitter.setEndSize(
                    0.18f
            );
        }

        // ========================================================
        // ЖИЗНЬ
        // ========================================================

        particleEmitter.setLowLife(
                0.8f
        );

        particleEmitter.setHighLife(
                1.8f
        );

        // ========================================================
        // ОДНОРАЗОВЫЙ ВЫБРОС
        // ========================================================

        particleEmitter.setParticlesPerSec(
                0.0f
        );

        // ========================================================
        // КРУГ
        // ========================================================

        if (isCircle) {

            /*
             * Частицы рождаются по ВСЕЙ площади круга.
             *
             * Эмиттер находится в зафиксированной точке
             * начала способности.
             */
            particleEmitter.setShape(
                    new CircleAreaEmitterShape(
                            CIRCLE_RADIUS
                    )
            );

            particleEmitter.setLocalTranslation(
                    pos.x,
                    0.10f,
                    pos.z
            );

            /*
             * Все частицы идут вверх.
             */
            particleEmitter
                    .getParticleInfluencer()
                    .setInitialVelocity(
                            new Vector3f(
                                    0.0f,
                                    3.2f,
                                    0.0f
                            )
                    );

            particleEmitter
                    .getParticleInfluencer()
                    .setVelocityVariation(
                            0.35f
                    );

            particleEmitter.setGravity(
                    0.0f,
                    1.8f,
                    0.0f
            );
        }

        // ========================================================
        // ТРЕУГОЛЬНИК
        // ========================================================

        else {

            particleEmitter.setLocalTranslation(
                    pos.x,
                    pos.y + 0.8f,
                    pos.z
            );

            particleEmitter
                    .getParticleInfluencer()
                    .setInitialVelocity(
                            direction.mult(
                                    5.0f
                            )
                    );

            particleEmitter
                    .getParticleInfluencer()
                    .setVelocityVariation(
                            0.45f
                    );

            particleEmitter.setGravity(
                    0.0f,
                    -0.5f,
                    0.0f
            );
        }

        // ========================================================
        // WORLD SPACE
        // ========================================================

        /*
         * Это критически важно.
         *
         * Частицы находятся в мировых координатах
         * и НЕ следуют за боссом.
         */
        particleEmitter.setInWorldSpace(
                true
        );

        // ========================================================
        // ВРАЩЕНИЕ
        // ========================================================

        particleEmitter.setRandomAngle(
                true
        );

        particleEmitter.setRotateSpeed(
                FastMath.nextRandomFloat()
                        * 4.0f
        );

        // ========================================================
        // NODE
        // ========================================================

        particleNode =
                new Node(
                        "FinalBossFireParticles"
                );

        particleNode.attachChild(
                particleEmitter
        );

        /*
         * ВАЖНО:
         *
         * Частицы не являются дочерними
         * узла босса.
         */
        app.getRootNode().attachChild(
                particleNode
        );

        // ========================================================
        // ВЫПУСК
        // ========================================================

        particleEmitter.setEnabled(
                true
        );

        particleEmitter.emitAllParticles();

        // ========================================================
        // ТАЙМЕР
        // ========================================================

        particleTimer = 2.2f;
    }

    // ============================================================
    // ФОРМА ЭМИТТЕРА — ПЛОЩАДЬ КРУГА
    // ============================================================

    /**
     * Равномерное распределение частиц
     * по площади круга.
     *
     * Используется:
     *
     * r = sqrt(random) * radius
     *
     * а не:
     *
     * r = random * radius
     *
     * потому что второй вариант даёт слишком много
     * частиц возле центра.
     */
    private static class CircleAreaEmitterShape
            implements EmitterShape {

        private float radius;

        public CircleAreaEmitterShape() {

            this.radius = 1.0f;
        }

        public CircleAreaEmitterShape(
                float radius
        ) {

            this.radius = radius;
        }

        @Override
        public void getRandomPoint(
                Vector3f store
        ) {

            float angle =
                    FastMath.nextRandomFloat()
                            * FastMath.TWO_PI;

            float r =
                    FastMath.sqrt(
                            FastMath.nextRandomFloat()
                    ) *
                            radius;

            store.set(
                    FastMath.cos(angle) * r,
                    0.0f,
                    FastMath.sin(angle) * r
            );
        }

        @Override
        public void getRandomPointAndNormal(
                Vector3f store,
                Vector3f normal
        ) {

            getRandomPoint(
                    store
            );

            if (normal != null) {

                normal.set(
                        0.0f,
                        1.0f,
                        0.0f
                );
            }
        }

        @Override
        public EmitterShape deepClone() {

            return new CircleAreaEmitterShape(
                    radius
            );
        }

        @Override
        public Object jmeClone() {

            return new CircleAreaEmitterShape(
                    radius
            );
        }

        @Override
        public void cloneFields(
                com.jme3.util.clone.Cloner cloner,
                Object original
        ) {
            // Ничего не требуется.
        }

        @Override
        public void write(
                JmeExporter ex
        ) throws IOException {

            OutputCapsule capsule =
                    ex.getCapsule(this);

            capsule.write(
                    radius,
                    "radius",
                    1.0f
            );
        }

        @Override
        public void read(
                JmeImporter im
        ) throws IOException {

            InputCapsule capsule =
                    im.getCapsule(this);

            radius =
                    capsule.readFloat(
                            "radius",
                            1.0f
                    );
        }
    }

    // ============================================================
    // ТЕКСТУРА ЧАСТИЦ
    // ============================================================

    private Texture2D createFireParticleTexture() {

        final int size = 32;

        ByteBuffer buffer =
                ByteBuffer.allocateDirect(
                        size *
                                size *
                                4
                );

        float center =
                (size - 1) *
                        0.5f;

        float maxDistance =
                center;

        for (int y = 0;
             y < size;
             y++) {

            for (int x = 0;
                 x < size;
                 x++) {

                float dx =
                        x -
                                center;

                float dy =
                        y -
                                center;

                float distance =
                        FastMath.sqrt(
                                dx * dx +
                                        dy * dy
                        );

                float normalized =
                        distance /
                                maxDistance;

                float alpha =
                        1.0f -
                                normalized;

                alpha =
                        FastMath.clamp(
                                alpha,
                                0.0f,
                                1.0f
                        );

                alpha =
                        FastMath.pow(
                                alpha,
                                1.5f
                        );

                int a =
                        (int)
                                (
                                        alpha *
                                                255.0f
                                );

                buffer.put(
                        (byte) 255
                );

                buffer.put(
                        (byte) 255
                );

                buffer.put(
                        (byte) 255
                );

                buffer.put(
                        (byte) a
                );
            }
        }

        buffer.flip();

        Image image =
                new Image(
                        Image.Format.RGBA8,
                        size,
                        size,
                        buffer
                );

        Texture2D texture =
                new Texture2D(
                        image
                );

        texture.setMagFilter(
                com.jme3.texture.Texture.MagFilter.Bilinear
        );

        texture.setMinFilter(
                com.jme3.texture.Texture.MinFilter.BilinearNoMipMaps
        );

        return texture;
    }

    // ============================================================
    // UPDATE PARTICLES
    // ============================================================

    private void updateParticles(
            float tpf
    ) {

        if (particleEmitter == null) {

            return;
        }

        particleTimer -= tpf;

        if (particleTimer <= 0.0f) {

            removeParticles();
        }
    }

    // ============================================================
    // REMOVE PARTICLES
    // ============================================================

    private void removeParticles() {

        if (particleEmitter != null) {

            particleEmitter.killAllParticles();

            particleEmitter.removeFromParent();

            particleEmitter = null;
        }

        if (particleNode != null) {

            particleNode.removeFromParent();

            particleNode = null;
        }

        particleTimer = 0.0f;
    }

    // ============================================================
    // REMOVE ABILITY VISUAL
    // ============================================================

    private void removeAbilityVisual() {

        if (areaMesh != null) {

            areaMesh.removeFromParent();

            areaMesh = null;
        }
    }

    // ============================================================
    // DISPOSE
    // ============================================================

    public void disposeFinalBossEffects() {

        removeAbilityVisual();

        removeParticles();

        isCasting = false;

        if (getAI() != null) {

            getAI().setStunned(false);
        }
    }
}