package com.mygame.monsters;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public class MeleMonster extends Monster {

    // ============================================================
    // АТАКА
    // ============================================================

    private float attackTimer = 0f;

    private float attackDelay = 0.35f;

    private float attackCooldown = 0.8f;

    private boolean attacking = false;

    private boolean damageApplied = false;

    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public MeleMonster() {

        super();
    }

    // ============================================================
    // COMBAT
    // ============================================================

    @Override
    public void updateCombat(
            float tpf
    ) {

        if (!isAlive()) {
            return;
        }

        if (getPlayerManager() == null) {
            playIdle();
            return;
        }

        if (isStunned()) {

            attacking = false;

            attackTimer = 0f;

            damageApplied = false;

            playIdle();

            return;
        }

        // ========================================================
        // ТАЙМЕР
        // ========================================================

        if (attackTimer > 0f) {

            attackTimer -= tpf;

            if (attackTimer < 0f) {
                attackTimer = 0f;
            }
        }

        // ========================================================
        // ИДЁМ К ИГРОКУ / ДИСТАНЦИЯ
        // ========================================================

        if (!isPlayerInAttackRange()) {

            attacking = false;

            damageApplied = false;

            playWalk();

            return;
        }

        // ========================================================
        // АТАКА В ПРОЦЕССЕ
        // ========================================================

        if (attacking) {

            /*
             * Наносим урон в середине атаки.
             */
            if (!damageApplied &&
                    attackTimer <= 0f) {

                applyMeleeDamage();

                return;
            }

            /*
             * Урон уже был.
             *
             * Ждём cooldown.
             */
            if (damageApplied) {

                if (attackTimer <= 0f) {

                    attacking = false;

                    damageApplied = false;

                    startAttack();

                }

                return;
            }

            return;
        }

        // ========================================================
        // НОВАЯ АТАКА
        // ========================================================

        if (attackTimer <= 0f) {

            startAttack();

        } else {

            playIdle();
        }
    }

    // ============================================================
    // START ATTACK
    // ============================================================

    private void startAttack() {

        if (!isAlive()) {
            return;
        }

        if (!isPlayerInAttackRange()) {

            attacking = false;

            damageApplied = false;

            playWalk();

            return;
        }

        attacking = true;

        damageApplied = false;

        /*
         * Сначала запускаем Attack.
         */
        playAttack();

        /*
         * Через attackDelay наносится урон.
         */
        attackTimer =
                attackDelay;

        System.out.println(
                "[MeleMonster] "
                + getName()
                + " ATTACK START"
        );
    }

    // ============================================================
    // DAMAGE
    // ============================================================

    private void applyMeleeDamage() {

        if (damageApplied) {
            return;
        }

        if (!isAlive()) {
            return;
        }

        if (getPlayerManager() == null) {
            return;
        }

        /*
         * Проверяем дистанцию ещё раз.
         */
        if (!isPlayerInAttackRange()) {

            attacking = false;

            damageApplied = false;

            playWalk();

            return;
        }

        int damage =
                Math.round(
                        getDamage()
                );

        if (damage <= 0) {

            damageApplied = true;

        } else {

            getPlayerManager()
                    .takeDamage(
                            damage
                    );

            damageApplied = true;

            System.out.println(
                    "[MeleMonster] "
                    + getName()
                    + " hit player for "
                    + damage
            );
        }

        /*
         * Теперь cooldown до следующей атаки.
         */
        attackTimer =
                attackCooldown;
    }

    // ============================================================
    // ATTACK ANIMATION
    // ============================================================

    @Override
    public void playAttack() {

        /*
         * Сбрасываем состояние анимации,
         * чтобы следующая Attack снова запускалась.
         */
        currentAnimation = "";

        playAnimation("Attack");
    }

    // ============================================================
    // SETTERS
    // ============================================================

    public float getMeleeAttackDelay() {

        return attackDelay;
    }

    public void setMeleeAttackDelay(
            float delay
    ) {

        attackDelay =
                Math.max(
                        0f,
                        delay
                );
    }

    public float getMeleeAttackCooldown() {

        return attackCooldown;
    }

    public void setMeleeAttackCooldown(
            float cooldown
    ) {

        attackCooldown =
                Math.max(
                        0f,
                        cooldown
                );
    }

    public boolean isMeleeAttacking() {

        return attacking;
    }
}