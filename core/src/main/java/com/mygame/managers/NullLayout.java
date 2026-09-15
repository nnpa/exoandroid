package com.mygame.managers;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.simsilica.lemur.core.GuiControl;
import com.simsilica.lemur.core.GuiLayout;

import java.util.Collection;
import java.util.Collections;

/**
 * Layout, который не управляет позиционированием дочерних элементов.
 * Позволяет использовать addChild() + setLocalTranslation() для ручного позиционирования.
 */
public class NullLayout implements GuiLayout {

    @Override
    public <T extends Node> T addChild(T child, Object... constraints) {
        return child;
    }

    @Override
    public void removeChild(Node child) {}

    @Override
    public Collection<Node> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public void clearChildren() {}

    @Override
    public void calculatePreferredSize(Vector3f size) {
        // Размер задаётся вручную через setPreferredSize
    }

    @Override
    public void reshape(Vector3f pos, Vector3f size) {
        // Ничего не делаем
    }

    @Override
    public void attach(GuiControl parent) {}

    @Override
    public void detach(GuiControl parent) {}

    @Override
    public boolean isAttached() {
        return false;
    }

    @Override
    public GuiControl getGuiControl() {
        return null;
    }

    @Override
    public GuiLayout clone() {
        return new NullLayout();
    }
}