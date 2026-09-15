package com.mygame.managers;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;

public class CameraFollowControl extends AbstractControl {
    private Camera cam;
    private Node target;
    private float distance = 25f;
    private float height = 20f;
    private float cameraAngle = 0f;
    private float smoothness = 0.15f;
    private Vector3f targetOffset = new Vector3f(0, -0.5f, 0);
    private boolean enabled = true;

    // Минимальное и максимальное расстояние для зума
    private static final float MIN_DISTANCE = 20f;
    private static final float MAX_DISTANCE = 28f;

    public CameraFollowControl(Camera cam, Node target) {
        this.cam = cam;
        this.target = target;
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (!enabled || target == null || cam == null) return;
        Vector3f targetPos = target.getWorldTranslation();
        if (targetPos == null) return;

        float x = FastMath.sin(cameraAngle) * distance;
        float z = FastMath.cos(cameraAngle) * distance;
        Vector3f desiredPos = targetPos.add(new Vector3f(x, height, z));

        Vector3f currentPos = cam.getLocation();
        desiredPos.interpolateLocal(currentPos, smoothness);

        cam.setLocation(desiredPos);
        Vector3f lookTarget = targetPos.add(targetOffset);
        cam.lookAt(lookTarget, Vector3f.UNIT_Y);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        // не используется
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void rotate(float delta) {
        cameraAngle += delta;
    }

    public void setCameraAngle(float angle) {
        this.cameraAngle = angle;
    }

    /**
     * Изменяет расстояние камеры с ограничением MIN_DISTANCE .. MAX_DISTANCE.
     * @param delta положительное – отдаление, отрицательное – приближение.
     */
    public void zoom(float delta) {
        distance = Math.max(MIN_DISTANCE, Math.min(MAX_DISTANCE, distance + delta));
    }

    /**
     * Возвращает текущее расстояние камеры.
     */
    public float getDistance() {
        return distance;
    }
}