package com.mygame.monsters;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.mygame.managers.PlayerManager;
import com.mygame.managers.SoundManager;
import com.mygame.managers.WorldManager;

import java.util.ArrayList;
import java.util.List;

public class MonsterAI {

    private Monster monster;

    private PlayerManager playerManager;

    private enum State {
        WANDERING,
        CHASING,
        ATTACKING
    }

    private State currentState =
            State.WANDERING;

    private boolean isStunned = false;

    private boolean bossMusicStarted = false;

    private boolean stopped = false;

    // ============================================================
    // WANDER (блуждание вокруг точки спавна)
    // ============================================================

    /**
     * Радиус, на который монстр удаляется от точки спавна.
     */
    private static final float WANDER_RADIUS = 4f;

    /**
     * Высота, на которой пускается луч проверки стен
     * (чтобы не задевать пол).
     */
    private static final float RAY_HEIGHT_OFFSET = 1f;

    private static final float WANDER_PAUSE_MIN = 1.5f;
    private static final float WANDER_PAUSE_MAX = 4.0f;

    private boolean wanderInitialized = false;

    private final List<Vector3f> wanderDirections =
            new ArrayList<>();

    private Vector3f wanderOrigin;
    private Vector3f wanderTarget;

    private float wanderPauseTimer = 0f;

    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public MonsterAI(
            Monster monster
    ) {

        this.monster =
                monster;
    }

    // ============================================================
    // PLAYER
    // ============================================================

    public void setPlayerManager(
            PlayerManager pm
    ) {

        this.playerManager =
                pm;
    }

    // ============================================================
    // STUN
    // ============================================================

    public void setStunned(
            boolean stunned
    ) {

        if (this.isStunned == stunned) {
            return;
        }

        this.isStunned =
                stunned;

        if (stunned) {

            monster.stopWalking();
        }
    }

    // ============================================================
    // MUSIC
    // ============================================================

    public void resetMusicFlag() {

        bossMusicStarted =
                false;
    }

    // ============================================================
    // STOP
    // ============================================================

    public void stop() {

        stopped = true;

        monster = null;
        playerManager = null;

        currentState =
                State.WANDERING;

        isStunned = false;
    }

    // ============================================================
    // WANDER — ИНИЦИАЛИЗАЦИЯ (рэйкаст стен вокруг спавна)
    // ============================================================

    private void initializeWanderIfNeeded() {

        if (wanderInitialized) {
            return;
        }

        wanderInitialized = true;

        Vector3f pos =
                monster.getPosition();

        if (pos == null) {

            /*
             * Позиция ещё не установлена —
             * попробуем инициализировать позже.
             */
            wanderInitialized = false;

            return;
        }

        wanderOrigin =
                pos.clone();

        computeWanderDirections();

        pickNewWanderTarget();

        wanderPauseTimer = 0f;
    }

    /**
     * Пускает 8 лучей вокруг точки спавна (N, S, E, W и диагонали)
     * и оставляет только те направления, где на протяжении
     * WANDER_RADIUS нет препятствий.
     */
    private void computeWanderDirections() {

        wanderDirections.clear();

        PhysicsSpace space =
                getPhysicsSpace();

        Vector3f[] dirs = new Vector3f[]{

                new Vector3f(0f, 0f, 1f),                          // N  (+Z)
                new Vector3f(0f, 0f, -1f),                         // S  (-Z)
                new Vector3f(1f, 0f, 0f),                          // E  (+X)
                new Vector3f(-1f, 0f, 0f),                         // W  (-X)
                new Vector3f(1f, 0f, 1f).normalizeLocal(),         // NE
                new Vector3f(-1f, 0f, 1f).normalizeLocal(),        // NW
                new Vector3f(1f, 0f, -1f).normalizeLocal(),        // SE
                new Vector3f(-1f, 0f, -1f).normalizeLocal()        // SW
        };

        Vector3f rayStart =
                wanderOrigin.add(0f, RAY_HEIGHT_OFFSET, 0f);

        for (Vector3f dir : dirs) {

            boolean clear = true;

            if (space != null) {

                Vector3f rayEnd =
                        rayStart.add(
                                dir.mult(WANDER_RADIUS)
                        );

                List<PhysicsRayTestResult> results =
                        space.rayTest(rayStart, rayEnd);

                if (results != null && !results.isEmpty()) {

                    clear = false;
                }
            }

            if (clear) {

                wanderDirections.add(dir);
            }
        }

        /*
         * Защита от ситуации, когда монстр окружён стенами
         * со всех сторон (например, слишком маленькая комната) —
         * иначе он вообще не сможет бродить.
         */
        if (wanderDirections.isEmpty()) {

            for (Vector3f dir : dirs) {

                wanderDirections.add(dir);
            }
        }
    }

    private void pickNewWanderTarget() {

        if (wanderDirections.isEmpty() || wanderOrigin == null) {

            wanderTarget =
                    wanderOrigin != null
                            ? wanderOrigin.clone()
                            : monster.getPosition();

            return;
        }

        Vector3f dir =
                wanderDirections.get(
                        FastMath.nextRandomInt(
                                0,
                                wanderDirections.size() - 1
                        )
                );

        /*
         * Случайная дистанция в пределах разрешённого
         * направления, чтобы монстр не всегда ходил
         * ровно до края радиуса.
         */
        float dist =
                WANDER_RADIUS
                * (0.4f + FastMath.nextRandomFloat() * 0.6f);

        wanderTarget =
                wanderOrigin.add(
                        dir.mult(dist)
                );
    }

    private PhysicsSpace getPhysicsSpace() {

        if (monster == null) {
            return null;
        }

        WorldManager wm =
                monster.getWorldManager();

        if (wm == null) {
            return null;
        }

        return wm.getPhysicsSpace();
    }

    // ============================================================
    // UPDATE
    // ============================================================

    public void update(
            float tpf
    ) {

        if (stopped ||
                monster == null ||
                !monster.isAlive()) {

            return;
        }

        // ========================================================
        // STUN
        // ========================================================

        if (isStunned ||
                monster.getStunTimer() > 0f) {

            monster.stopWalking();

            return;
        }

        initializeWanderIfNeeded();

        // ========================================================
        // PLAYER
        // ========================================================

        Vector3f playerPos =
                getPlayerPosition();

        if (playerPos == null) {

            if (currentState == State.WANDERING) {

                updateWander(tpf);

            } else {

                monster.playIdle();
            }

            return;
        }

        Vector3f monsterPos =
                monster.getPosition();

        if (monsterPos == null) {
            return;
        }

        float distance =
                horizontalDistance(
                        monsterPos,
                        playerPos
                );

        // ========================================================
        // WANDERING (замена прежнего IDLE)
        // ========================================================

        if (currentState == State.WANDERING) {

            if (distance <=
                    monster.getAggroRange()) {

                currentState =
                        State.CHASING;

            } else {

                updateWander(tpf);

                return;
            }
        }

        // ========================================================
        // CHASING
        // ========================================================

        if (currentState ==
                State.CHASING) {

            /*
             * Уже подошли достаточно близко.
             */
            if (distance <=
                    monster.getAttackRange()) {

                currentState =
                        State.ATTACKING;

                monster.playIdle();

                return;
            }

            /*
             * Идём к игроку.
             */
            moveTowards(
                    playerPos,
                    tpf
            );

            monster.playWalk();

            return;
        }

        // ========================================================
        // ATTACKING
        // ========================================================

        if (currentState ==
                State.ATTACKING) {

            /*
             * Игрок отошёл.
             */
            if (distance >
                    monster.getAttackRange() * 1.15f) {

                currentState =
                        State.CHASING;

                monster.playWalk();

                return;
            }

            /*
             * Не двигаемся во время атаки.
             */
            rotateTowardsPlayer(
                    playerPos
            );

            /*
             * Босс музыка.
             */
            if (monster.isBoss() &&
                    !bossMusicStarted) {

                SoundManager.stopMusic();

                SoundManager.playMusic(
                        SoundManager.MUSIC_BOSS
                );

                bossMusicStarted =
                        true;
            }

            /*
             * Передаём управление конкретному
             * типу монстра.
             */
            monster.updateCombat(
                    tpf
            );
        }
    }

    // ============================================================
    // WANDER — ДВИЖЕНИЕ
    // ============================================================

    private void updateWander(float tpf) {

        if (wanderTarget == null) {

            pickNewWanderTarget();
        }

        Vector3f current =
                monster.getPosition();

        float dx =
                wanderTarget.x - current.x;

        float dz =
                wanderTarget.z - current.z;

        float distToTarget =
                FastMath.sqrt(dx * dx + dz * dz);

        /*
         * Дошли до цели — стоим, ждём, потом выбираем новую точку.
         */
        if (distToTarget < 0.3f) {

            monster.playIdle();

            wanderPauseTimer -= tpf;

            if (wanderPauseTimer <= 0f) {

                pickNewWanderTarget();

                wanderPauseTimer =
                        WANDER_PAUSE_MIN
                        + FastMath.nextRandomFloat()
                        * (WANDER_PAUSE_MAX - WANDER_PAUSE_MIN);
            }

            return;
        }

        Vector3f direction =
                new Vector3f(dx, 0f, dz)
                        .normalizeLocal();

        /*
         * Бродит медленнее, чем преследует игрока.
         */
        float wanderSpeed =
                monster.getMoveSpeed() * 0.5f;

        float movement =
                wanderSpeed * tpf;

        movement =
                Math.min(movement, distToTarget);

        if (movement > 0f) {

            Vector3f newPos =
                    current.add(
                            direction.mult(movement)
                    );

            monster.setPosition(newPos);
        }

        rotateTowardsDirection(direction);

        monster.playWalk();
    }

    // ============================================================
    // MOVEMENT
    // ============================================================

    private void moveTowards(
            Vector3f target,
            float tpf
    ) {

        Vector3f current =
                monster.getPosition();

        Vector3f direction =
                new Vector3f(
                        target.x - current.x,
                        0f,
                        target.z - current.z
                );

        float distance =
                direction.length();

        if (distance < 0.001f) {

            monster.playIdle();

            return;
        }

        direction.normalizeLocal();

        float stopDistance =
                monster.getAttackRange() * 0.85f;

        if (distance <= stopDistance) {

            monster.playIdle();

            return;
        }

        float movement =
                monster.getMoveSpeed()
                * tpf;

        /*
         * Не перелетаем через игрока.
         */
        movement =
                Math.min(
                        movement,
                        distance - stopDistance
                );

        if (movement > 0f) {

            Vector3f newPosition =
                    current.add(
                            direction.mult(
                                    movement
                            )
                    );

            monster.setPosition(
                    newPosition
            );
        }

        rotateTowardsDirection(
                direction
        );
    }

    // ============================================================
    // ROTATION
    // ============================================================

    private void rotateTowardsPlayer(
            Vector3f playerPosition
    ) {

        Vector3f monsterPosition =
                monster.getPosition();

        float dx =
                playerPosition.x
                - monsterPosition.x;

        float dz =
                playerPosition.z
                - monsterPosition.z;

        if (FastMath.abs(dx) < 0.000001f &&
                FastMath.abs(dz) < 0.000001f) {

            return;
        }

        Vector3f direction =
                new Vector3f(
                        dx,
                        0f,
                        dz
                );

        direction.normalizeLocal();

        rotateTowardsDirection(
                direction
        );
    }

    private void rotateTowardsDirection(
            Vector3f direction
    ) {

        if (monster.getModelNode() == null) {
            return;
        }

        if (direction.lengthSquared() <
                0.000001f) {

            return;
        }

        Quaternion rotation =
                new Quaternion();

        rotation.lookAt(
                direction,
                Vector3f.UNIT_Y
        );

        /*
         * Базовая модель уже имеет -90°.
         */
        rotation.multLocal(
                new Quaternion().fromAngleAxis(
                        -FastMath.HALF_PI,
                        Vector3f.UNIT_Y
                )
        );

        monster.getModelNode()
                .setLocalRotation(
                        rotation
                );
    }

    // ============================================================
    // DISTANCE
    // ============================================================

    private float horizontalDistance(
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
    // PLAYER POSITION
    // ============================================================

    private Vector3f getPlayerPosition() {

        if (playerManager != null) {

            return playerManager.getPosition();
        }

        return null;
    }
}