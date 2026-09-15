package com.mygame.managers;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Node;

public class BestiaryAppState extends BaseAppState {

    private final Node modelRootNode;

    public BestiaryAppState(Node modelRootNode) {
        this.modelRootNode = modelRootNode;
    }

    @Override
    protected void initialize(Application app) {
        // В данном случае инициализация не требуется, так как нода уже создана
    }

    @Override
    public void update(float tpf) {
        // ИЗМЕНЕНИЕ 1: Обновляем логическое состояние вручную
        if (modelRootNode != null) {
            modelRootNode.updateLogicalState(tpf);
        }
    }

    @Override
    public void render(RenderManager renderManager) {
        // ИЗМЕНЕНИЕ 2: Обновляем геометрическое состояние перед рендерингом
        if (modelRootNode != null) {
            modelRootNode.updateGeometricState();
        }
    }

    @Override
    protected void cleanup(Application app) {
        // Дополнительная очистка не требуется, так как удаление ноды обрабатывается в BestiaryWindow
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }
}