package com.mygame.monsters;

import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
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
 * Босс-червь.
 *
 * Способность:
 *
 * 1. Босс подходит к игроку.
 *
 * 2. Когда игрок оказывается на расстоянии атаки,
 *    босс останавливается.
 *
 * 3. На земле появляется красный/кислотно-зелёный
 *    треугольник направления атаки.
 *
 * 4. Босс стоит 2 секунды.
 *
 * 5. После подготовки из точки каста вырываются
 *    кислотно-зелёные частицы.
 *
 * 6. Если игрок за время подготовки убежал,
 *    после каста босс снова начинает Run.
 */
public class Angel extends MeleMonster {

    // ============================================================
    // НАСТРОЙКИ СПОСОБНОСТИ
    // ============================================================

    /**
     * Через сколько секунд после завершения предыдущей
     * способности можно начинать следующую.
     */
    private static final float ABILITY_INTERVAL = 3.5f;

    /**
     * Сколько секунд босс стоит перед срабатыванием.
     */
    private static final float ABILITY_DURATION = 2.0f;

    /**
     * Максимальная длина треугольника.
     */
    private static final float CONE_LENGTH = 6.0f;

    /**
     * Половина ширины треугольника в конце.
     */
    private static final float CONE_RADIUS = 3.0f;

    /**
     * Высота треугольника над землёй.
     */
    private static final float GROUND_HEIGHT = 0.06f;

    /**
     * Урон способности.
     */
    private static final float ABILITY_DAMAGE_MULTIPLIER = 2.0f;


    // ============================================================
    // ЦВЕТ ЧАСТИЦ
    // ============================================================

    /**
     * Цвет частиц вынесен в отдельную переменную.
     *
     * Сейчас:
     * КИСЛОТНО-ЗЕЛЁНЫЙ.
     *
     * Если потом захочешь другой цвет,
     * достаточно изменить только эти значения.
     */
    private static final ColorRGBA PARTICLE_COLOR =
            new ColorRGBA(
                    0.20f,
                    1.00f,
                    0.00f,
                    1.00f
            );


    // ============================================================
    // ТАЙМЕРЫ
    // ============================================================

    /**
     * Таймер до следующей способности.
     */
    private float abilityCooldownTimer = 3.5f;

    /**
     * Оставшееся время подготовки способности.
     */
    private float abilityDurationTimer = 0.0f;


    // ============================================================
    // СОСТОЯНИЕ КАСТА
    // ============================================================

    /**
     * true = босс сейчас подготавливает способность.
     */
    private boolean isCasting = false;


    /**
     * Направление треугольника.
     *
     * Фиксируется в момент начала способности.
     *
     * Если игрок убежит, треугольник не будет
     * следовать за ним.
     */
    private Vector3f abilityDirection =
            Vector3f.UNIT_Z.clone();


    /**
     * Позиция босса в момент начала каста.
     *
     * ВАЖНО:
     *
     * Частицы и урон используют эту позицию,
     * а не текущую позицию босса.
     */
    private Vector3f abilityStartPosition =
            Vector3f.ZERO.clone();


    // ============================================================
    // ВИЗУАЛЬНЫЙ ТРЕУГОЛЬНИК
    // ============================================================

    private Geometry areaMesh = null;


    // ============================================================
    // ЧАСТИЦЫ
    // ============================================================

    private ParticleEmitter particleEmitter = null;

    private Node particleNode = null;

    private float particleTimer = 0.0f;

    private Texture2D acidParticleTexture = null;


    // ============================================================
    // КОНСТРУКТОР
    // ============================================================

    public Angel() {

        setId("angel");
        setName("angel");

        setLevel(1);

        setMaxHealth(1130);
        setHealth(1130);

        setDamage(15);

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
setLootTable(loot);

        setLootTable(loot);
    }


    // ============================================================
    // UPDATE
    // ============================================================

    @Override
    public void update(float tpf) {

        super.update(tpf);


        // --------------------------------------------------------
        // Обновляем частицы
        // --------------------------------------------------------

        updateParticles(tpf);


        // --------------------------------------------------------
        // БОСС МЁРТВ
        // --------------------------------------------------------

        if (!isAlive()) {

            removeAbilityVisual();

            removeParticles();

            return;
        }


        // --------------------------------------------------------
        // ОГЛУШЁН
        // --------------------------------------------------------

        if (isStunned()) {

            return;
        }


        // --------------------------------------------------------
        // СЕЙЧАС ИДЁТ КАСТ
        // --------------------------------------------------------

        if (isCasting) {

            /*
             * Пока способность подготавливается,
             * босс НЕ ДВИГАЕТСЯ.
             *
             * Возвращаем его в точку начала каста.
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
        // ОЖИДАНИЕ СЛЕДУЮЩЕЙ СПОСОБНОСТИ
        // --------------------------------------------------------

        abilityCooldownTimer -= tpf;


        if (abilityCooldownTimer <= 0.0f) {

            abilityCooldownTimer = 0.0f;


            /*
             * Босс начинает каст только тогда,
             * когда игрок находится в дистанции атаки.
             */
            if (isPlayerInAttackRange()) {

                startAbility();
            }
        }
    }


    // ============================================================
    // ПРОВЕРКА ДИСТАНЦИИ ДО ИГРОКА
    // ============================================================

    


    // ============================================================
    // НАЧАЛО СПОСОБНОСТИ
    // ============================================================

    private void startAbility() {

        if (isCasting) {

            return;
        }


        /*
         * Дополнительная проверка.
         *
         * Даже если таймер закончился,
         * способность не начнётся,
         * если игрок далеко.
         */
        if (!isPlayerInAttackRange()) {

            return;
        }


        // ========================================================
        // НАЧИНАЕМ КАСТ
        // ========================================================

        isCasting = true;

        abilityDurationTimer =
                ABILITY_DURATION;


        // ========================================================
        // ЗАПОМИНАЕМ ПОЗИЦИЮ
        // ========================================================

        abilityStartPosition =
                getPosition().clone();


        // ========================================================
        // ЗАПОМИНАЕМ НАПРАВЛЕНИЕ
        // ========================================================

        Vector3f playerPos;

        if (getPlayerManager() != null) {

            playerPos =
                    getPlayerManager()
                            .getPosition()
                            .clone();

        } else {

            playerPos =
                    abilityStartPosition
                            .add(
                                    Vector3f.UNIT_Z
                            );
        }


        Vector3f direction =
                playerPos.subtract(
                        abilityStartPosition
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
         * НАПРАВЛЕНИЕ ФИКСИРУЕТСЯ.
         *
         * Игрок может убежать.
         * Треугольник останется на старом месте.
         */
        abilityDirection =
                direction.clone();


        // ========================================================
        // ОСТАНАВЛИВАЕМ AI
        // ========================================================

        if (getAI() != null) {

            getAI().setStunned(true);
        }


        // ========================================================
        // АНИМАЦИЯ IDLE
        // ========================================================

        playAnimation("Idle");


        // ========================================================
        // СОЗДАЁМ ТРЕУГОЛЬНИК
        // ========================================================

        createConeVisual(
                abilityStartPosition,
                abilityDirection,
                CONE_LENGTH,
                CONE_RADIUS
        );
    }


    // ============================================================
    // СОЗДАНИЕ ТРЕУГОЛЬНИКА
    // ============================================================

    private void createConeVisual(
            Vector3f origin,
            Vector3f direction,
            float length,
            float radius
    ) {

        removeAbilityVisual();


        // ========================================================
        // НОРМАЛИЗУЕМ НАПРАВЛЕНИЕ
        // ========================================================

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
        // БОКОВОЙ ВЕКТОР
        // ========================================================

        /*
         * Получаем перпендикулярный вектор
         * в горизонтальной плоскости.
         */
        Vector3f side =
                new Vector3f(
                        -dir.z,
                        0.0f,
                        dir.x
                );


        side.normalizeLocal();


        // ========================================================
        // ВЕРШИНА ТРЕУГОЛЬНИКА
        // ========================================================

        Vector3f p0 =
                dir.mult(
                        0.25f
                );


        // ========================================================
        // КОНЕЦ ТРЕУГОЛЬНИКА
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
        // КЛАДЁМ ВСЕ ТОЧКИ НА ЗЕМЛЮ
        // ========================================================

        p0.y =
                GROUND_HEIGHT;

        p1.y =
                GROUND_HEIGHT;

        p2.y =
                GROUND_HEIGHT;


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
                        "WormBossConeAbility",
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


        /*
         * Пока оставляем саму зону красной.
         *
         * Если хочешь, её тоже можно вынести
         * в отдельную переменную.
         */
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


        /*
         * Видим треугольник с обеих сторон.
         */
        mat.getAdditionalRenderState()
                .setFaceCullMode(
                        RenderState.FaceCullMode.Off
                );


        areaMesh.setMaterial(
                mat
        );


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


        // ========================================================
        // ПРИКРЕПЛЯЕМ К МИРУ
        // ========================================================

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
         * Используем именно позицию,
         * где способность была начата.
         */
        Vector3f abilityPos =
                abilityStartPosition.clone();


        // ========================================================
        // УДАЛЯЕМ ТРЕУГОЛЬНИК
        // ========================================================

        removeAbilityVisual();


        // ========================================================
        // ПОЛУЧАЕМ ИГРОКА
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
        // ПРОВЕРЯЕМ ПОПАДАНИЕ
        // ========================================================

        Vector3f toPlayer =
                playerPos.subtract(
                        abilityPos
                );


        toPlayer.y = 0.0f;


        float distance =
                toPlayer.length();


        if (
                distance <= CONE_LENGTH
                        &&
                        distance > 0.0001f
        ) {

            Vector3f playerDirection =
                    toPlayer.normalizeLocal();


            float angle =
                    playerDirection.angleBetween(
                            abilityDirection
                    );


            /*
             * Угол треугольника.
             */
            float coneAngle =
                    FastMath.atan(
                            CONE_RADIUS /
                                    CONE_LENGTH
                    );


            if (angle <= coneAngle) {

                applyDamageToPlayer();
            }
        }


        // ========================================================
        // ОГОНЬ
        // ========================================================

        spawnFireParticles(
                abilityPos,
                abilityDirection
        );


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
        // ВОЗВРАЩАЕМ ДВИЖЕНИЕ
        // ========================================================

        /*
         * Самая важная часть.
         *
         * Если игрок убежал за 2 секунды каста,
         * босс сразу начинает Run.
         */
        if (!isPlayerInAttackRange()) {

            playAnimation("Walk");

        } else {

            playAnimation("Idle");
        }


        // ========================================================
        // НОВЫЙ ТАЙМЕР
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
                        getDamage() *
                                ABILITY_DAMAGE_MULTIPLIER
                );


        getPlayerManager().takeDamage(
                damage
        );
    }


    // ============================================================
    // ЧАСТИЦЫ
    // ============================================================

    private void spawnFireParticles(
            Vector3f pos,
            Vector3f direction
    ) {

        if (app == null) {

            return;
        }


        // ========================================================
        // УДАЛЯЕМ СТАРЫЕ ЧАСТИЦЫ
        // ========================================================

        removeParticles();


        // ========================================================
        // СОЗДАЁМ ТЕКСТУРУ
        // ========================================================

        if (acidParticleTexture == null) {

            acidParticleTexture =
                    createAcidParticleTexture();
        }


        // ========================================================
        // EMITTER
        // ========================================================

        particleEmitter =
                new ParticleEmitter(
                        "WormBossAcid",
                        ParticleMesh.Type.Triangle,
                        220
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
                acidParticleTexture
        );


        mat.getAdditionalRenderState()
                .setBlendMode(
                        RenderState.BlendMode.Alpha
                );


        mat.getAdditionalRenderState()
                .setDepthWrite(false);


        mat.getAdditionalRenderState()
                .setDepthTest(true);


        particleEmitter.setMaterial(
                mat
        );


        particleEmitter.setQueueBucket(
                RenderQueue.Bucket.Translucent
        );


        // ========================================================
        // ЦВЕТ
        // ========================================================

        /*
         * Весь цвет задаётся через одну переменную.
         */
        particleEmitter.setStartColor(
                PARTICLE_COLOR.clone()
        );


        /*
         * На конце частица становится прозрачной,
         * но сохраняет тот же кислотно-зелёный оттенок.
         */
        ColorRGBA endColor =
                PARTICLE_COLOR.clone();

        endColor.a = 0.0f;


        particleEmitter.setEndColor(
                endColor
        );


        // ========================================================
        // РАЗМЕР
        // ========================================================

        particleEmitter.setStartSize(
                1.0f
        );


        particleEmitter.setEndSize(
                0.18f
        );


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
        // НАПРАВЛЕНИЕ
        // ========================================================

        Vector3f velocity =
                direction
                        .mult(
                                5.0f
                        );


        particleEmitter
                .getParticleInfluencer()
                .setInitialVelocity(
                        velocity
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


        // ========================================================
        // ПОЗИЦИЯ
        // ========================================================

        /*
         * Используется pos из начала каста.
         *
         * НЕ getPosition().
         *
         * Поэтому частицы не будут следовать
         * за движущимся боссом.
         */
        particleEmitter.setLocalTranslation(
                pos.x,
                pos.y + 0.8f,
                pos.z
        );


        // ========================================================
        // WORLD SPACE
        // ========================================================

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
                        "WormBossAcidParticles"
                );


        particleNode.attachChild(
                particleEmitter
        );


        /*
         * Прикрепляем к rootNode,
         * а не к боссу.
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

        particleTimer =
                2.2f;
    }


    // ============================================================
    // ТЕКСТУРА ЧАСТИЦ
    // ============================================================

    private Texture2D createAcidParticleTexture() {

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


                /*
                 * Белая маска.
                 *
                 * Реальный цвет задаётся
                 * через PARTICLE_COLOR.
                 */
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

    public void disposeWormBossEffects() {

        removeAbilityVisual();

        removeParticles();

        isCasting = false;


        if (getAI() != null) {

            getAI().setStunned(false);
        }
    }
}