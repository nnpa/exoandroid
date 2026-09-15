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
 * Имп.
 *
 * При смерти:
 * - сразу появляется красный круг-предупреждение на земле;
 * - через EXPLOSION_DELAY секунд круг взрывается —
 *   наносится двойной урон игроку (если он в радиусе)
 *   и выпускается вспышка огненных частиц.
 */
public class Imp extends MeleMonster {

    // ============================================================
    // НАСТРОЙКИ ВЗРЫВА
    // ============================================================

    private static final float EXPLOSION_RADIUS = 3.0f;
    private static final float EXPLOSION_DELAY = 2.0f;
    private static final float GROUND_HEIGHT = 0.06f;

    // ============================================================
    // СОСТОЯНИЕ ОТЛОЖЕННОГО ВЗРЫВА
    // ============================================================

    private boolean deathSequenceStarted = false;
    private boolean exploded = false;
    private float explodeTimer = 0.0f;
    private Vector3f explosionPosition = null;

    // ============================================================
    // ВИЗУАЛ / ЧАСТИЦЫ
    // ============================================================

    private Geometry explosionAreaMesh = null;
    private ParticleEmitter particleEmitter = null;
    private Node particleNode = null;
    private float particleTimer = 0.0f;

    private static Texture2D fireParticleTexture = null;

    // ============================================================
    // КОНСТРУКТОР
    // ============================================================

    public Imp() {

        setId("imp");
        setName("imp");

        setLevel(1);

        setMaxHealth(30);
        setHealth(30);

        setDamage(5);

        setAttackRange(1.5f);
        setMoveSpeed(2.0f);
        setAggroRange(8.0f);

        // Таблица дропа – только одеваемые предметы

LootTable loot = new LootTable();
loot.addEntry("Weapon", 0.25f);
loot.addEntry("Helmet", 0.2f);
loot.addEntry("Chest", 0.2f);
loot.addEntry("Shield", 0.15f);
loot.addEntry("Legs", 0.2f);
loot.addEntry("Boots", 0.15f);
loot.addEntry("Gloves", 0.15f);
setLootTable(loot);
    }

    // ============================================================
    // СМЕРТЬ — ЗАПУСК ОТЛОЖЕННОГО ВЗРЫВА
    // ============================================================

    @Override
    protected void onDeath() {

        if (!deathSequenceStarted) {

            deathSequenceStarted = true;

            /*
             * Позицию фиксируем сразу,
             * т.к. modelNode вскоре будет удалён
             * стандартной логикой Monster.onDeath().
             */
            explosionPosition = getPosition().clone();

            createExplosionVisual(explosionPosition, EXPLOSION_RADIUS);

            explodeTimer = EXPLOSION_DELAY;
        }

        /*
         * Стандартная логика Monster:
         * анимация смерти, звук, дроп, удаление модели.
         */
        super.onDeath();
    }

    // ============================================================
    // ФАКТИЧЕСКИЙ ВЗРЫВ (урон + частицы)
    // ============================================================

    private void triggerExplosion() {

        if (explosionPosition == null) {
            return;
        }

        // ========================================================
        // УРОН ПО ИГРОКУ
        // ========================================================

        if (playerManager != null) {

            Vector3f playerPos = playerManager.getPosition();

            if (playerPos != null) {

                float dx = playerPos.x - explosionPosition.x;
                float dz = playerPos.z - explosionPosition.z;

                float distance = FastMath.sqrt(dx * dx + dz * dz);

                if (distance <= EXPLOSION_RADIUS) {

                    int damage = Math.round(getDamage() * 3);

                    playerManager.takeDamage(damage);
                }
            }
        }

        spawnExplosionParticles(explosionPosition);
    }

    // ============================================================
    // КРАСНЫЙ КРУГ НА ЗЕМЛЕ (телеграф, показывается сразу)
    // ============================================================

    private void createExplosionVisual(Vector3f center, float radius) {

        if (app == null) {
            return;
        }

        Mesh mesh = createHorizontalCircleMesh(radius, 32);

        explosionAreaMesh = new Geometry("ImpExplosionArea", mesh);

        Material mat = new Material(
                app.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md"
        );

        mat.setColor("Color", new ColorRGBA(1.0f, 0.0f, 0.0f, 0.42f));
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        mat.getAdditionalRenderState().setDepthWrite(false);
        mat.getAdditionalRenderState().setDepthTest(true);
        mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);

        explosionAreaMesh.setMaterial(mat);
        explosionAreaMesh.setQueueBucket(RenderQueue.Bucket.Translucent);

        explosionAreaMesh.setLocalTranslation(center.x, GROUND_HEIGHT, center.z);

        app.getRootNode().attachChild(explosionAreaMesh);
    }

    private Mesh createHorizontalCircleMesh(float radius, int segments) {

        float[] positions = new float[(segments + 1) * 3];
        short[] indices = new short[segments * 3];

        positions[0] = 0.0f;
        positions[1] = 0.0f;
        positions[2] = 0.0f;

        for (int i = 0; i < segments; i++) {

            float angle = FastMath.TWO_PI * ((float) i / (float) segments);

            float x = FastMath.cos(angle) * radius;
            float z = FastMath.sin(angle) * radius;

            int index = (i + 1) * 3;

            positions[index] = x;
            positions[index + 1] = 0.0f;
            positions[index + 2] = z;
        }

        for (int i = 0; i < segments; i++) {

            int next = (i + 1) % segments;
            int index = i * 3;

            indices[index] = 0;
            indices[index + 1] = (short) (i + 1);
            indices[index + 2] = (short) (next + 1);
        }

        Mesh mesh = new Mesh();
        mesh.setMode(Mesh.Mode.Triangles);
        mesh.setBuffer(VertexBuffer.Type.Position, 3, positions);
        mesh.setBuffer(VertexBuffer.Type.Index, 3, indices);
        mesh.updateBound();

        return mesh;
    }

    // ============================================================
    // ЧАСТИЦЫ ВЗРЫВА
    // ============================================================

    private void spawnExplosionParticles(Vector3f pos) {

        if (app == null) {
            return;
        }

        if (fireParticleTexture == null) {
            fireParticleTexture = createFireParticleTexture();
        }

        particleEmitter = new ParticleEmitter(
                "ImpExplosionFire",
                ParticleMesh.Type.Triangle,
                80
        );

        Material mat = new Material(
                app.getAssetManager(),
                "Common/MatDefs/Misc/Particle.j3md"
        );

        mat.setTexture("Texture", fireParticleTexture);

        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        mat.getAdditionalRenderState().setDepthWrite(false);
        mat.getAdditionalRenderState().setDepthTest(true);

        particleEmitter.setMaterial(mat);
        particleEmitter.setQueueBucket(RenderQueue.Bucket.Translucent);

        particleEmitter.setStartColor(new ColorRGBA(1.0f, 0.5f, 0.0f, 1.0f));
        particleEmitter.setEndColor(new ColorRGBA(0.6f, 0.0f, 0.0f, 0.0f));

        particleEmitter.setStartSize(0.6f);
        particleEmitter.setEndSize(0.1f);

        particleEmitter.setLowLife(0.4f);
        particleEmitter.setHighLife(0.9f);

        particleEmitter.setParticlesPerSec(0.0f);

        particleEmitter.setShape(
                new CircleAreaEmitterShape(EXPLOSION_RADIUS)
        );

        particleEmitter.setLocalTranslation(pos.x, 0.1f, pos.z);

        particleEmitter.getParticleInfluencer()
                .setInitialVelocity(new Vector3f(0.0f, 2.5f, 0.0f));

        particleEmitter.getParticleInfluencer()
                .setVelocityVariation(0.5f);

        particleEmitter.setGravity(0.0f, 2.0f, 0.0f);

        /*
         * Мировое пространство — частицы остаются
         * на месте взрыва, монстра уже нет.
         */
        particleEmitter.setInWorldSpace(true);

        particleEmitter.setRandomAngle(true);
        particleEmitter.setRotateSpeed(FastMath.nextRandomFloat() * 4.0f);

        particleNode = new Node("ImpExplosionParticles");
        particleNode.attachChild(particleEmitter);

        app.getRootNode().attachChild(particleNode);

        particleEmitter.setEnabled(true);
        particleEmitter.emitAllParticles();

        particleTimer = 1.2f;
    }

    // ============================================================
    // ФОРМА ЭМИТТЕРА — ПЛОЩАДЬ КРУГА
    // ============================================================

    private static class CircleAreaEmitterShape implements EmitterShape {

        private float radius;

        public CircleAreaEmitterShape() {
            this.radius = 1.0f;
        }

        public CircleAreaEmitterShape(float radius) {
            this.radius = radius;
        }

        @Override
        public void getRandomPoint(Vector3f store) {

            float angle = FastMath.nextRandomFloat() * FastMath.TWO_PI;
            float r = FastMath.sqrt(FastMath.nextRandomFloat()) * radius;

            store.set(
                    FastMath.cos(angle) * r,
                    0.0f,
                    FastMath.sin(angle) * r
            );
        }

        @Override
        public void getRandomPointAndNormal(Vector3f store, Vector3f normal) {

            getRandomPoint(store);

            if (normal != null) {
                normal.set(0.0f, 1.0f, 0.0f);
            }
        }

        @Override
        public EmitterShape deepClone() {
            return new CircleAreaEmitterShape(radius);
        }

        @Override
        public Object jmeClone() {
            return new CircleAreaEmitterShape(radius);
        }

        @Override
        public void cloneFields(com.jme3.util.clone.Cloner cloner, Object original) {
            // Ничего не требуется.
        }

        @Override
        public void write(JmeExporter ex) throws IOException {

            OutputCapsule capsule = ex.getCapsule(this);
            capsule.write(radius, "radius", 1.0f);
        }

        @Override
        public void read(JmeImporter im) throws IOException {

            InputCapsule capsule = im.getCapsule(this);
            radius = capsule.readFloat("radius", 1.0f);
        }
    }

    // ============================================================
    // ТЕКСТУРА ЧАСТИЦ
    // ============================================================

    private Texture2D createFireParticleTexture() {

        final int size = 32;

        ByteBuffer buffer = ByteBuffer.allocateDirect(size * size * 4);

        float center = (size - 1) * 0.5f;
        float maxDistance = center;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {

                float dx = x - center;
                float dy = y - center;

                float distance = FastMath.sqrt(dx * dx + dy * dy);
                float normalized = distance / maxDistance;

                float alpha = 1.0f - normalized;
                alpha = FastMath.clamp(alpha, 0.0f, 1.0f);
                alpha = FastMath.pow(alpha, 1.5f);

                int a = (int) (alpha * 255.0f);

                buffer.put((byte) 255);
                buffer.put((byte) 120);
                buffer.put((byte) 0);
                buffer.put((byte) a);
            }
        }

        buffer.flip();

        Image image = new Image(Image.Format.RGBA8, size, size, buffer);

        Texture2D texture = new Texture2D(image);

        texture.setMagFilter(com.jme3.texture.Texture.MagFilter.Bilinear);
        texture.setMinFilter(com.jme3.texture.Texture.MinFilter.BilinearNoMipMaps);

        return texture;
    }

    // ============================================================
    // UPDATE — отсчёт до взрыва + очистка эффектов
    // ============================================================

    @Override
    public void update(float tpf) {

        super.update(tpf);

        // ========================================================
        // ОТСЧЁТ ДО ВЗРЫВА
        // ========================================================

        if (deathSequenceStarted && !exploded) {

            explodeTimer -= tpf;

            if (explodeTimer <= 0.0f) {

                exploded = true;

                if (explosionAreaMesh != null) {

                    explosionAreaMesh.removeFromParent();
                    explosionAreaMesh = null;
                }

                triggerExplosion();
            }
        }

        // ========================================================
        // ОЧИСТКА ЧАСТИЦ ПОСЛЕ ВЗРЫВА
        // ========================================================

        if (particleEmitter == null) {
            return;
        }

        particleTimer -= tpf;

        if (particleTimer <= 0.0f) {

            particleEmitter.killAllParticles();
            particleEmitter.removeFromParent();
            particleEmitter = null;

            if (particleNode != null) {
                particleNode.removeFromParent();
                particleNode = null;
            }
        }
    }
}