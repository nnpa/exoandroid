package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import java.util.HashMap;
import java.util.Map;

public class GameManager {
    private SimpleApplication app;
    private NetworkManager networkManager;
    private PlayerManager playerManager;
    private WorldManager worldManager;
    private UIManager uiManager;

    private GameState currentState = GameState.LOADING;
    private boolean isPaused = false;
    private Map<String, Object> globalData = new HashMap<>();

    public enum GameState {
        LOADING, LOGIN, CITY, DUNGEON, COMBAT
    }

    public GameManager(SimpleApplication app) {
        this.app = app;
    }

    public void initialize() {
        System.out.println("[GameManager] Init");
    }

    public void update(float tpf) {
        if (isPaused) return;
    }

    public void setState(GameState newState) {
        if (this.currentState == newState) {
            return;
        }
        this.currentState = newState;
        if (uiManager != null) {
            uiManager.onStateChanged(newState);
            System.out.println("[GameManager] UI уведомлён о смене состояния: " + newState);
        }
        if (worldManager != null) {
            worldManager.onStateChanged(newState);
        }
        if (newState == GameState.CITY && playerManager != null) {
            playerManager.attachToScene();
        }
    }

    public void cleanup() {
        if (networkManager != null) networkManager.cleanup();
        if (playerManager != null) playerManager.cleanup();
        if (worldManager != null) worldManager.cleanup();
        if (uiManager != null) uiManager.cleanup();
    }

    // --- Сеттеры ---
    public void setNetworkManager(NetworkManager nm) { this.networkManager = nm; }
    public void setPlayerManager(PlayerManager pm) { this.playerManager = pm; }
    public void setWorldManager(WorldManager wm) { this.worldManager = wm; }
    public void setUIManager(UIManager ui) { this.uiManager = ui; }

    // --- Геттеры ---
    public NetworkManager getNetworkManager() { return networkManager; }
    public PlayerManager getPlayerManager() { return playerManager; }
    public WorldManager getWorldManager() { return worldManager; }
    public UIManager getUIManager() { return uiManager; }
    public GameState getCurrentState() { return currentState; }

    public void setGlobalData(String key, Object value) { globalData.put(key, value); }
    public Object getGlobalData(String key) { return globalData.get(key); }
}