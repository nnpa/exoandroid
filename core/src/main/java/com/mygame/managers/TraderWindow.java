package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.mygame.Main;
import com.mygame.items.Item;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TraderWindow {
    private static final float FONT_MULT = 2f;
    private SimpleApplication app;
    private PlayerManager playerManager;
    private InventoryManager inventoryManager;
    private UIManager uiManager;
    private NetworkManager networkManager;
    private Node windowNode;
    private boolean isVisible = false;
    private Label goldLabel;
    private Node contentNode;
    private float scale = 1f;
    private float windowWidth = 560;
    private float windowHeight = 540;
    private float leftShift = 20f;

    public TraderWindow(SimpleApplication app, PlayerManager pm, InventoryManager im, UIManager ui) {
        this.app = app;
        this.playerManager = pm;
        this.inventoryManager = im;
        this.uiManager = ui;
        this.networkManager = Main.getInstance().getNetworkManager();
        createWindow();
        positionWindow();
    }

    public Node getNode() { return windowNode; }
    private String getLocalized(String key) { return LocalizationManager.getInstance().get(key); }

    // ============================================================
    // ПРОЗРАЧНЫЙ ФОН КНОПОК
    //
    // Раньше здесь был серый QuadBackgroundComponent
    // (0.3, 0.3, 0.3, 0.8) — именно он давал «большой серый
    // квадрат» под каждой кнопкой. Теперь фон полностью
    // прозрачный, но область клика сохраняется (Lemur считает
    // её по background, поэтому null ставить нельзя).
    // ============================================================
    private void applyBtnBackground(Button btn) {
        btn.setBackground(new QuadBackgroundComponent(new ColorRGBA(0f, 0f, 0f, 0f)));
    }

    private void updateScale() {
        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();
        float scaleX = screenWidth / 800f;
        float scaleY = screenHeight / 600f;
        scale = Math.min(scaleX, scaleY);
        scale = Math.max(0.5f, Math.min(scale, 1.5f));
    }

    private void createWindow() {
        updateScale();
        leftShift = 20 * scale;
        windowWidth = 560 * scale;
        windowHeight = 540 * scale;
        windowNode = new Node("TraderWindowNode");
        windowNode.setName("TraderWindowNode");

        if (uiManager != null) {
            Geometry bgGeom = uiManager.createBackgroundGeometry(windowWidth, windowHeight);
            bgGeom.setLocalTranslation(0, 0, -0.1f);
            windowNode.attachChild(bgGeom);
        } else {
            Quad bgQuad = new Quad(windowWidth, windowHeight);
            Geometry bgGeom = new Geometry("TraderBg", bgQuad);
            Material bgMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            bgMat.setColor("Color", new ColorRGBA(0.08f, 0.08f, 0.15f, 0.97f));
            bgGeom.setMaterial(bgMat);
            bgGeom.setLocalTranslation(0, 0, -0.1f);
            windowNode.attachChild(bgGeom);
        }

        Label title = new Label(getLocalized("trader.title"));
        title.setFontSize(20 * scale * FONT_MULT);
        title.setColor(ColorRGBA.White);
        title.setLocalTranslation(20 * scale + leftShift, windowHeight - 60 * scale, 0.1f);
        windowNode.attachChild(title);

        Button closeButton = new Button(getLocalized("ui.close_button"));
        closeButton.setPreferredSize(new Vector3f(180 * scale, 50 * scale, 0));
        closeButton.setFontSize(24 * scale);
        closeButton.setColor(ColorRGBA.White);
        applyBtnBackground(closeButton);
        closeButton.setLocalTranslation(windowWidth - 200 * scale, windowHeight - 60 * scale, 0.1f);
        closeButton.addCommands(Button.ButtonAction.Down, s -> hide());
        windowNode.attachChild(closeButton);

        goldLabel = new Label(getLocalized("gold.label") + "0");
        goldLabel.setFontSize(14 * scale * FONT_MULT);
        goldLabel.setColor(ColorRGBA.Yellow);
        goldLabel.setLocalTranslation(20 * scale + leftShift, windowHeight - 115 * scale, 0.1f);
        windowNode.attachChild(goldLabel);

        // Вкладки Buy / Sell
        Button buyTab = new Button(getLocalized("trader.tab.buy"));
        buyTab.setPreferredSize(new Vector3f(130 * scale, 50 * scale, 0));
        buyTab.setFontSize(22 * scale);
        buyTab.setColor(ColorRGBA.White);
        applyBtnBackground(buyTab);
        buyTab.setLocalTranslation(20 * scale + leftShift, windowHeight - 185 * scale, 0.1f);
        buyTab.addCommands(Button.ButtonAction.Down, s -> showBuyTab());
        windowNode.attachChild(buyTab);

        Button sellTab = new Button(getLocalized("trader.tab.sell"));
        sellTab.setPreferredSize(new Vector3f(130 * scale, 50 * scale, 0));
        sellTab.setFontSize(22 * scale);
        sellTab.setColor(ColorRGBA.White);
        applyBtnBackground(sellTab);
        sellTab.setLocalTranslation(165 * scale + leftShift, windowHeight - 185 * scale, 0.1f);
        sellTab.addCommands(Button.ButtonAction.Down, s -> showSellTab());
        windowNode.attachChild(sellTab);

        // ============================================================
        // contentNode смещён НИЖЕ вкладок. Заголовки контента
        // (Buy Items / Sell Items) и строки товаров начинаются
        // под Y вкладок, а не наезжают на них.
        //
        // Было: contentNode на (20+leftShift, 20*scale),
        //       header внутри — на 320*scale → абсолют Y ≈ 340,
        //       вкладки — на 355 → наезд.
        //
        // Стало: contentNode на (20+leftShift, -70*scale),
        //        header внутри всё ещё на 320*scale →
        //        абсолют Y ≈ 250, запас до вкладок — 55*scale.
        // ============================================================
        contentNode = new Node("ContentNode");
        contentNode.setLocalTranslation(20 * scale + leftShift, -70 * scale, 0.1f);
        windowNode.attachChild(contentNode);

        positionWindow();
        showBuyTab();
        updateGold();
    }

    private void positionWindow() {
        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();
        float x = (screenWidth - windowWidth) / 2;
        float y = (screenHeight - windowHeight) / 2;
        if (y < 0) y = 0;
        windowNode.setLocalTranslation(x, y, 0);
    }

    private void clearContent() {
        List<Spatial> children = new ArrayList<>(contentNode.getChildren());
        for (Spatial s : children) contentNode.detachChild(s);
    }

    private void saveToServer() {
        if (networkManager == null || playerManager == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("gold", playerManager.getGold());
        data.put("healthPotions", playerManager.getHealthPotions());
        data.put("manaPotions", playerManager.getManaPotions());
        networkManager.saveCharacter(data).thenAccept(success -> {
            app.enqueue(() -> { if (!success) System.err.println("[TraderWindow] Failed to save!"); });
        });
    }

    private void showBuyTab() {
        clearContent();
        Label header = new Label(getLocalized("trader.buy.header"));
        header.setFontSize(14 * scale * FONT_MULT);
        header.setColor(ColorRGBA.White);
        header.setLocalTranslation(0, 320 * scale, 0.1f);
        contentNode.attachChild(header);

        Label hpLabel = new Label(getLocalized("trader.buy.hp"));
        hpLabel.setFontSize(12 * scale * FONT_MULT);
        hpLabel.setColor(ColorRGBA.White);
        hpLabel.setLocalTranslation(0, 250 * scale, 0.1f);
        contentNode.attachChild(hpLabel);

        Button hpBuy = new Button(getLocalized("trader.buy.button"));
        hpBuy.setPreferredSize(new Vector3f(120 * scale, 44 * scale, 0));
        hpBuy.setFontSize(22 * scale);
        hpBuy.setColor(ColorRGBA.White);
        applyBtnBackground(hpBuy);
        hpBuy.setLocalTranslation(300 * scale, 240 * scale, 0.1f);
        hpBuy.addCommands(Button.ButtonAction.Down, s -> {
            if (playerManager.getGold() >= 10) {
                playerManager.setGold(playerManager.getGold() - 10);
                playerManager.addHealthPotions(1);
                updateGold();
                if (uiManager != null) uiManager.updatePotionCounts();
                showBuyTab();
                saveToServer();
            }
        });
        contentNode.attachChild(hpBuy);

        Label mpLabel = new Label(getLocalized("trader.buy.mp"));
        mpLabel.setFontSize(12 * scale * FONT_MULT);
        mpLabel.setColor(ColorRGBA.White);
        mpLabel.setLocalTranslation(0, 170 * scale, 0.1f);
        contentNode.attachChild(mpLabel);

        Button mpBuy = new Button(getLocalized("trader.buy.button"));
        mpBuy.setPreferredSize(new Vector3f(120 * scale, 44 * scale, 0));
        mpBuy.setFontSize(22 * scale);
        mpBuy.setColor(ColorRGBA.White);
        applyBtnBackground(mpBuy);
        mpBuy.setLocalTranslation(300 * scale, 160 * scale, 0.1f);
        mpBuy.addCommands(Button.ButtonAction.Down, s -> {
            if (playerManager.getGold() >= 10) {
                playerManager.setGold(playerManager.getGold() - 10);
                playerManager.addManaPotions(1);
                updateGold();
                if (uiManager != null) uiManager.updatePotionCounts();
                showBuyTab();
                saveToServer();
            }
        });
        contentNode.attachChild(mpBuy);
    }

    private void showSellTab() {
        clearContent();
        Label header = new Label(getLocalized("trader.sell.header"));
        header.setFontSize(14 * scale * FONT_MULT);
        header.setColor(ColorRGBA.White);
        header.setLocalTranslation(0, 320 * scale, 0.1f);
        contentNode.attachChild(header);

        List<Item> items = inventoryManager.getItems();
        if (items.isEmpty()) {
            Label empty = new Label(getLocalized("trader.sell.empty"));
            empty.setFontSize(12 * scale * FONT_MULT);
            empty.setColor(ColorRGBA.Gray);
            empty.setLocalTranslation(0, 280 * scale, 0.1f);
            contentNode.attachChild(empty);
            return;
        }

        float yPos = 260 * scale;
        float stepY = 70 * scale;
        for (Item item : items) {
            Label nameLabel = new Label(item.getName() + " (" + item.getLocalizedType() + ")");
            nameLabel.setFontSize(22 * scale);
            nameLabel.setColor(ColorRGBA.White);
            nameLabel.setLocalTranslation(0, yPos, 0.1f);
            contentNode.attachChild(nameLabel);

            int price = Math.max(1, item.getLevel() * 5);
            Button sellBtn = new Button(getLocalized("trader.sell.button") + price + "g");
            sellBtn.setPreferredSize(new Vector3f(180 * scale, 50 * scale, 0));
            sellBtn.setFontSize(20 * scale);
            sellBtn.setColor(ColorRGBA.White);
            applyBtnBackground(sellBtn);
            sellBtn.setLocalTranslation(290 * scale, yPos - 10 * scale, 0.1f);

            sellBtn.addCommands(Button.ButtonAction.Down, s -> performSell(item, price));
            contentNode.attachChild(sellBtn);

            yPos -= stepY;
            if (yPos < 20 * scale) break;
        }
    }

    private void performSell(Item item, int price) {
        if (item == null) return;
        int slotIndex = inventoryManager.getItemIndex(item);
        if (slotIndex == -1 && item.getId() != null) {
            for (int i = 0; i < 20; i++) {
                Item slotItem = inventoryManager.getItemAtSlot(i);
                if (slotItem != null && item.getId().equals(slotItem.getId())) {
                    slotIndex = i; break;
                }
            }
        }
        if (slotIndex == -1) {
            System.err.println("[TraderWindow] Item not found: " + item.getName());
            return;
        }
        final int finalSlot = slotIndex;

        if (networkManager != null) {
            networkManager.dropItem(finalSlot).thenAccept(response -> {
                app.enqueue(() -> {
                    if (response != null && uiManager != null) {
                        uiManager.applyCharacterData(response);
                        updateGold();
                        uiManager.updatePotionCounts();
                        if (isVisible) showSellTab();
                    } else {
                        networkManager.loadCharacterData().thenAccept(data -> {
                            app.enqueue(() -> {
                                if (data != null && uiManager != null) {
                                    uiManager.applyCharacterData(data);
                                    if (isVisible) showSellTab();
                                }
                            });
                        });
                    }
                });
            }).exceptionally(ex -> {
                app.enqueue(() -> System.err.println("[TraderWindow] Network error: " + ex.getMessage()));
                return null;
            });
        } else {
            playerManager.setGold(playerManager.getGold() + price);
            inventoryManager.removeItem(item);
            updateGold();
            if (uiManager != null) uiManager.updatePotionCounts();
            if (isVisible) showSellTab();
        }
    }

    private void updateGold() {
        if (goldLabel != null) goldLabel.setText(getLocalized("gold.label") + playerManager.getGold());
    }

    public void show() {
        isVisible = true;
        if (uiManager != null) uiManager.onTraderOpened(windowNode);
        else if (!app.getGuiNode().hasChild(windowNode)) app.getGuiNode().attachChild(windowNode);
        positionWindow();
        updateGold();
        showBuyTab();
    }

    public void hide() {
        isVisible = false;
        if (uiManager != null) uiManager.onTraderClosed(windowNode);
        else if (app.getGuiNode().hasChild(windowNode)) app.getGuiNode().detachChild(windowNode);
    }

    public void toggle() { if (isVisible) hide(); else show(); }
    public boolean isVisible() { return isVisible; }

    public void updateLayout(int screenWidth, int screenHeight) {
        if (isVisible) {
            if (uiManager != null) uiManager.onTraderClosed(windowNode);
            windowNode.detachAllChildren();
            createWindow();
            if (uiManager != null) uiManager.onTraderOpened(windowNode);
        }
    }
}