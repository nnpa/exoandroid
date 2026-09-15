package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.BillboardControl;

/**
 * Всплывающие числа урона/лечения над персонажем и монстрами.
 *
 * Не требует ручных вызовов update() откуда-либо — каждое число
 * само управляет своим всплытием/исчезновением через собственный
 * AbstractControl и открепляется по завершении.
 */
public class DamageNumberManager {

    private static BitmapFont cachedFont;

    private static final float FLOAT_SPEED = 1.0f;
    private static final float LIFETIME = 1.0f;
    private static final float RANDOM_X_SPREAD = 0.4f;
    private static final float SPAWN_HEIGHT_OFFSET = 2.2f;

    public static void spawnDamage(
            SimpleApplication app,
            Node anchorNode,
            Vector3f worldPosition,
            int amount
    ) {
        if (amount <= 0) {
            return;
        }
        spawn(app, anchorNode, worldPosition, "-" + amount, new ColorRGBA(1f, 0.25f, 0.2f, 1f));
    }

    public static void spawnHeal(
            SimpleApplication app,
            Node anchorNode,
            Vector3f worldPosition,
            int amount
    ) {
        if (amount <= 0) {
            return;
        }
        spawn(app, anchorNode, worldPosition, "+" + amount, new ColorRGBA(0.3f, 1f, 0.3f, 1f));
    }

    private static void spawn(
            SimpleApplication app,
            Node anchorNode,
            Vector3f worldPosition,
            String text,
            ColorRGBA color
    ) {

        if (app == null || worldPosition == null) {
            return;
        }

        BitmapFont font = getFont(app);

        BitmapText bmp = new BitmapText(font);
        bmp.setText(text);
        bmp.setSize(0.5f);
        bmp.setColor(color);

        float textWidth = bmp.getLineWidth();
        bmp.setLocalTranslation(-textWidth / 2f, 0f, 0f);

        Node holder = new Node("DamageNumber");
        holder.attachChild(bmp);
        holder.addControl(new BillboardControl());

        float offsetX = (FastMath.nextRandomFloat() - 0.5f) * RANDOM_X_SPREAD;

        holder.setLocalTranslation(
                worldPosition.x + offsetX,
                worldPosition.y + SPAWN_HEIGHT_OFFSET,
                worldPosition.z
        );

        Node attachTarget = app.getRootNode();

        attachTarget.attachChild(holder);

        holder.addControl(new FloatingTextControl(color));
    }

    private static BitmapFont getFont(SimpleApplication app) {

        if (cachedFont != null) {
            return cachedFont;
        }

        String language = SettingsManager.getInstance().getLanguage();

        String path = "Interface/Fonts/ru.fnt";

        cachedFont = app.getAssetManager().loadFont(path);

        return cachedFont;
    }

    /**
     * Всплытие и затухание числа, затем самостоятельное открепление.
     */
    private static class FloatingTextControl extends AbstractControl {

        private float age = 0f;
        private final ColorRGBA baseColor;

        FloatingTextControl(ColorRGBA baseColor) {
            this.baseColor = baseColor.clone();
        }

        @Override
        protected void controlUpdate(float tpf) {

            if (spatial == null) {
                return;
            }

            age += tpf;

            spatial.move(0f, FLOAT_SPEED * tpf, 0f);

            float t = age / LIFETIME;

            if (t >= 1f) {

                if (spatial.getParent() != null) {
                    spatial.getParent().detachChild(spatial);
                }

                return;
            }

            float alpha = 1f - t;

            if (spatial instanceof Node) {

                for (Spatial child : ((Node) spatial).getChildren()) {

                    if (child instanceof BitmapText) {

                        ColorRGBA c = baseColor.clone();
                        c.a = alpha;
                        ((BitmapText) child).setColor(c);
                    }
                }
            }
        }

        @Override
        protected void controlRender(RenderManager rm, ViewPort vp) {
        }
    }
}