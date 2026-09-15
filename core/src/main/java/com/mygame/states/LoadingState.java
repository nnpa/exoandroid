package com.mygame.states;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.material.Material;
import com.jme3.font.BitmapText;
import com.mygame.Main;
import com.mygame.managers.GameManager;

public class LoadingState extends AbstractAppState {
    private Main app;
    private Node guiNode;
    private Geometry loadingBg;
    private BitmapText loadingText, progressText;
    private int screenWidth, screenHeight;

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (Main) app;
        this.guiNode = this.app.getGuiNode();
        this.screenWidth = (int) this.app.getCamera().getWidth();
        this.screenHeight = (int) this.app.getCamera().getHeight();
        createLoadingScreen();
        startLoading();
    }

    private void createLoadingScreen() {
        int winW = 350, winH = 120;
        float x = (screenWidth - winW) / 2f;
        float y = (screenHeight - winH) / 2f;
        Quad q = new Quad(winW, winH);
        loadingBg = new Geometry("LoadingBg", q);
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0.1f, 0.1f, 0.25f, 0.95f));
        loadingBg.setMaterial(mat);
        loadingBg.setLocalTranslation(x, y, 0);
        guiNode.attachChild(loadingBg);

        loadingText = new BitmapText(app.getAssetManager().loadFont("Interface/Fonts/Default.fnt"));
        loadingText.setText("Loading...");
        loadingText.setSize(24);
        loadingText.setColor(ColorRGBA.White);
        loadingText.setLocalTranslation(x + 110, y + 90, 0);
        guiNode.attachChild(loadingText);

        progressText = new BitmapText(app.getAssetManager().loadFont("Interface/Fonts/Default.fnt"));
        progressText.setText("0%");
        progressText.setSize(18);
        progressText.setColor(ColorRGBA.White);
        progressText.setLocalTranslation(x + 160, y + 50, 0);
        guiNode.attachChild(progressText);
    }

    private void startLoading() {
        new Thread(() -> {
            try {
                for (int i = 0; i <= 10; i++) {
                    Thread.sleep(300);
                    int p = i * 10;
                    if (progressText != null) progressText.setText(p + "%");
                }
                Thread.sleep(500);
                app.getGameManager().setState(GameManager.GameState.LOGIN);
                cleanup();
            } catch (Exception e) {
                app.getGameManager().setState(GameManager.GameState.LOGIN);
                cleanup();
            }
        }).start();
    }

    @Override
    public void cleanup() {
        super.cleanup();
        if (loadingBg != null) { guiNode.detachChild(loadingBg); loadingBg = null; }
        if (loadingText != null) { guiNode.detachChild(loadingText); loadingText = null; }
        if (progressText != null) { guiNode.detachChild(progressText); progressText = null; }
    }
}