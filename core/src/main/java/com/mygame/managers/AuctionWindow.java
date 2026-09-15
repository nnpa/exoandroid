package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.event.MouseEventControl;
import com.simsilica.lemur.event.MouseListener;
import com.mygame.Main;
import com.mygame.items.Item;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AuctionWindow {

    private static final float FONT_MULT = 2f;

    private SimpleApplication app;
    private UIManager uiManager;
    private NetworkManager networkManager;
    private InventoryManager inventoryManager;
    private PlayerManager playerManager;

    // ================================================================
    // ВИРТУАЛЬНАЯ КЛАВИАТУРА (одна на всё окно)
    // ================================================================
    private VirtualKeyboard virtualKeyboard;

    private Node windowNode;
    private boolean isVisible = false;

    private List<Spatial> dynamicParts = new ArrayList<>();

    private Container listAndPaginationContainer;
    private Container paginationContainer;
    private Container tooltipContainer;
    private Label tooltipLabel;
    private Label goldLabel;
    private Label statusLabel;
    private TextField priceInput;

    // ================================================================
    // МОДАЛКА «ХАРАКТЕРИСТИКИ ПРЕДМЕТА»
    // Работает по тапу на лот — заменяет старый hover-tooltip,
    // которого на Android нет.
    // ================================================================
    private Node itemDetailOverlay;

    private int selectedSlot = -1;

    private List<AuctionLot> currentLots = new ArrayList<>();
    private int currentPage = 1;
    private int totalPages = 1;

    private String filterType = "";
    private String filterRarity = "";
    private int filterMinLevel = 1;
    private int filterMaxLevel = 100;

    private float scale = 1f;
    private float winW, winH;
    private float leftShift = 0f;

    private float ROW_HEIGHT = 44f;
    private float PAGINATION_HEIGHT = 44f;

    public AuctionWindow(SimpleApplication app, UIManager ui, InventoryManager im, PlayerManager pm) {
        this.app = app;
        this.uiManager = ui;
        this.inventoryManager = im;
        this.playerManager = pm;
        this.networkManager = Main.getInstance().getNetworkManager();

        this.virtualKeyboard = new VirtualKeyboard(app, ui.getGuiNode());
        this.virtualKeyboard.setOnShowListener(kbHeight -> {
            System.out.println("[AuctionWindow] VirtualKeyboard shown, height=" + kbHeight);
            repositionForKeyboard(true, kbHeight);
        });
        this.virtualKeyboard.setOnHideListener(() -> {
            System.out.println("[AuctionWindow] VirtualKeyboard hidden");
            repositionForKeyboard(false, 0f);
        });

        createWindow();
    }

    private String L(String key) {
        String v = getLocalized(key);
        return v != null ? v : key;
    }

    private String getLocalized(String key) {
        return LocalizationManager.getInstance().get(key);
    }

    private void applyBtnBackground(Button btn) {
        btn.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.3f, 0.3f, 0.3f, 0.8f)));
    }

    private void openBuyGoldPage() {
        Main main = (Main) app;
        if (main == null || main.getNetworkManager() == null) return;
        String serverUrl = main.getNetworkManager().getServerUrl();
        String token = main.getNetworkManager().getAuthToken();
        if (serverUrl == null || serverUrl.isEmpty() || token == null) return;
        String url = serverUrl + "payment/shop?token=" + token;
        try {
            if (java.awt.Desktop.isDesktopSupported()
                    && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            }
        } catch (Exception e) {
            System.err.println("[AuctionWindow] Failed to open shop: " + e.getMessage());
        }
    }

    private void updateScale() {
        float screenWidth = app.getCamera().getWidth();
        float screenHeight = app.getCamera().getHeight();
        float scaleX = screenWidth / 800f;
        float scaleY = screenHeight / 600f;
        scale = Math.min(scaleX, scaleY);
        scale = Math.max(0.5f, Math.min(scale, 1.5f));
    }

    // ================================================================
    // ВИРТУАЛЬНАЯ КЛАВИАТУРА — УСТАНОВКА НА ПОЛЕ
    // ================================================================
    private void installKeyboardTrigger(TextField field) {
        if (field == null || virtualKeyboard == null) return;
        MouseEventControl.addListenersToSpatial(field, new MouseListener() {
            @Override
            public void mouseButtonEvent(MouseButtonEvent evt, Spatial s, Spatial t) {
                if (evt.getButtonIndex() != 0) return;
                if (!evt.isPressed()) return;
                System.out.println("[AuctionWindow] TextField tap → open keyboard");
                virtualKeyboard.show(field);
            }
            @Override public void mouseEntered(MouseMotionEvent evt, Spatial s, Spatial t) {}
            @Override public void mouseExited(MouseMotionEvent evt, Spatial s, Spatial t) {}
            @Override public void mouseMoved(MouseMotionEvent evt, Spatial s, Spatial t) {}
        });
    }

    private void repositionForKeyboard(boolean keyboardVisible, float keyboardHeight) {
        System.out.println("[AuctionWindow] keyboard " 
            + (keyboardVisible ? "shown (h=" + keyboardHeight + ")" : "hidden"));
    }

    private void createWindow() {
        updateScale();
        windowNode = new Node("AuctionWindowNode");

        winW = 775 * scale;
        float desiredWinH = 780 * scale;
        float maxWinH = app.getCamera().getHeight() - 40;
        winH = Math.min(desiredWinH, maxWinH);
        System.out.println("[AuctionWindow] winH=" + winH
                + " (desired=" + desiredWinH + ", max=" + maxWinH + ")");
leftShift = 19 * scale;

        Geometry bg = uiManager.createBackgroundGeometry(winW, winH);
        bg.setLocalTranslation(0, 0, -0.1f);
        windowNode.attachChild(bg);

        Label title = new Label(L("auction.title"));
        title.setFontSize(20 * scale * FONT_MULT);
        title.setColor(ColorRGBA.White);
        title.setLocalTranslation(winW / 2 - 80 * scale + leftShift, winH - 45 * scale, 0.1f);
        windowNode.attachChild(title);

        Button closeBtn = new Button(L("ui.close_button"));
        closeBtn.setPreferredSize(new Vector3f(180 * scale, 50 * scale, 0));
        closeBtn.setFontSize(22 * scale);
        closeBtn.setColor(ColorRGBA.White);
        applyBtnBackground(closeBtn);
        closeBtn.setLocalTranslation(winW - 200 * scale + leftShift, winH - 55 * scale, 0.1f);
        closeBtn.addCommands(Button.ButtonAction.Down, s -> hide());
        windowNode.attachChild(closeBtn);

        Button buyTab = new Button(L("auction.tab.browse"));
        buyTab.setPreferredSize(new Vector3f(140 * scale, 42 * scale, 0));
        buyTab.setFontSize(20 * scale);
        buyTab.setColor(ColorRGBA.White);
        applyBtnBackground(buyTab);
        buyTab.setLocalTranslation(15 * scale + leftShift, winH - 120 * scale, 0.1f);
        buyTab.addCommands(Button.ButtonAction.Down, s -> showBrowseTab());
        windowNode.attachChild(buyTab);

        Button sellTab = new Button(L("auction.tab.sell"));
        sellTab.setPreferredSize(new Vector3f(140 * scale, 42 * scale, 0));
        sellTab.setFontSize(20 * scale);
        sellTab.setColor(ColorRGBA.White);
        applyBtnBackground(sellTab);
        sellTab.setLocalTranslation(165 * scale + leftShift, winH - 120 * scale, 0.1f);
        sellTab.addCommands(Button.ButtonAction.Down, s -> showSellTab());
        windowNode.attachChild(sellTab);

        goldLabel = new Label(L("gold.label") + playerManager.getGold());
        goldLabel.setFontSize(14 * scale * FONT_MULT);
        goldLabel.setColor(ColorRGBA.Yellow);
        goldLabel.setLocalTranslation(15 * scale + leftShift, winH - 80 * scale, 0.1f);
        windowNode.attachChild(goldLabel);

        statusLabel = new Label("");
        statusLabel.setFontSize(18 * scale);
        statusLabel.setColor(ColorRGBA.Red);
        statusLabel.setLocalTranslation(15 * scale + leftShift, 15 * scale, 0.5f);
        statusLabel.setCullHint(Node.CullHint.Always);
        windowNode.attachChild(statusLabel);

        positionWindow();
        showBrowseTab();
    }

    private void positionWindow() {
        float w = app.getCamera().getWidth();
        float h = app.getCamera().getHeight();
        float offsetY = 40 * scale + 60 * scale;
        windowNode.setLocalTranslation((w - winW) / 2, (h - winH) / 2 + offsetY, 0);
    }

    private void clearDynamicParts() {
        for (Spatial s : dynamicParts) {
            if (windowNode.hasChild(s)) windowNode.detachChild(s);
        }
        dynamicParts.clear();
    }

    // ================================================================
    // BROWSE TAB
    // ================================================================
    private void showBrowseTab() {
        clearDynamicParts();
        selectedSlot = -1;
        currentLots.clear();

        float y = winH - 140 * scale;

        goldLabel.setText(L("gold.label") + playerManager.getGold());
        goldLabel.setPreferredSize(new Vector3f(180 * scale, 34 * scale, 0));
        goldLabel.setLocalTranslation(15 * scale + leftShift, y, 0.1f);
        windowNode.attachChild(goldLabel);
        dynamicParts.add(goldLabel);

        y -= 50 * scale;

        Label typeLabel = new Label(L("auction.filter.type"));
        typeLabel.setFontSize(12 * scale * FONT_MULT);
        typeLabel.setColor(ColorRGBA.White);
        typeLabel.setPreferredSize(new Vector3f(winW - 30 * scale, 30 * scale, 0));
        typeLabel.setLocalTranslation(15 * scale + leftShift, y, 0.1f);
        windowNode.attachChild(typeLabel);
        dynamicParts.add(typeLabel);
        y -= 35 * scale;

        String[] typeOptions = {"All", "Weapon", "Helmet", "Chest", "Shield", "Legs", "Boots", "Gloves"};
        Container typeContainer = new Container();
        typeContainer.setLayout(new SpringGridLayout(Axis.X, Axis.Y));
        typeContainer.setPreferredSize(new Vector3f(winW - 30 * scale, 36 * scale, 0));
        typeContainer.setLocalTranslation(15 * scale + leftShift, y, 0.1f);

        for (String opt : typeOptions) {
            String displayText = opt.equals("All") ? L("auction.filter.all") : L("item.type." + opt);
            Button btn = new Button(displayText);
            btn.setFontSize(18 * scale);
            btn.setPreferredSize(new Vector3f(88 * scale, 32 * scale, 0));
            applyBtnBackground(btn);
            if (filterType.isEmpty() && opt.equals("All")) btn.setColor(ColorRGBA.Yellow);
            else if (opt.equals(filterType)) btn.setColor(ColorRGBA.Yellow);
            else btn.setColor(ColorRGBA.White);
            btn.addCommands(Button.ButtonAction.Down, s -> { filterType = opt.equals("All") ? "" : opt; loadLots(1); });
            typeContainer.addChild(btn);
        }
        windowNode.attachChild(typeContainer);
        dynamicParts.add(typeContainer);
        y -= 46 * scale;

        Label rarityLabel = new Label(L("auction.filter.rarity"));
        rarityLabel.setFontSize(12 * scale * FONT_MULT);
        rarityLabel.setColor(ColorRGBA.White);
        rarityLabel.setPreferredSize(new Vector3f(winW - 30 * scale, 30 * scale, 0));
        rarityLabel.setLocalTranslation(15 * scale + leftShift, y, 0.1f);
        windowNode.attachChild(rarityLabel);
        dynamicParts.add(rarityLabel);
        y -= 35 * scale;

        String[] rarityOptions = {"All", "COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY"};
        Container rarityContainer = new Container();
        rarityContainer.setLayout(new SpringGridLayout(Axis.X, Axis.Y));
        rarityContainer.setPreferredSize(new Vector3f(winW - 30 * scale, 36 * scale, 0));
        rarityContainer.setLocalTranslation(15 * scale + leftShift, y, 0.1f);

        for (String opt : rarityOptions) {
            String displayText = opt.equals("All") ? L("auction.filter.all") : L("rarity." + opt.toLowerCase());
            Button btn = new Button(displayText);
            btn.setFontSize(18 * scale);
            btn.setPreferredSize(new Vector3f(100 * scale, 32 * scale, 0));
            applyBtnBackground(btn);
            if (filterRarity.isEmpty() && opt.equals("All")) btn.setColor(ColorRGBA.Yellow);
            else if (opt.equals(filterRarity)) btn.setColor(ColorRGBA.Yellow);
            else btn.setColor(ColorRGBA.White);
            btn.addCommands(Button.ButtonAction.Down, s -> { filterRarity = opt.equals("All") ? "" : opt; loadLots(1); });
            rarityContainer.addChild(btn);
        }
        windowNode.attachChild(rarityContainer);
        dynamicParts.add(rarityContainer);
        y -= 46 * scale;

        Container lvlContainer = new Container();
        lvlContainer.setLayout(new SpringGridLayout(Axis.X, Axis.Y));
        lvlContainer.setPreferredSize(new Vector3f(winW - 30 * scale, 40 * scale, 0));
        lvlContainer.setLocalTranslation(15 * scale + leftShift, y, 0.1f);

        Label lvlLabel = new Label(L("auction.filter.minlevel"));
        lvlLabel.setFontSize(12 * scale * FONT_MULT);
        lvlLabel.setColor(ColorRGBA.White);
        lvlContainer.addChild(lvlLabel);

        TextField levelField = new TextField("1");
        levelField.setFontSize(20 * scale);
        levelField.setPreferredSize(new Vector3f(60 * scale, 34 * scale, 0));
        lvlContainer.addChild(levelField);
        installKeyboardTrigger(levelField);

        Button lvlApplyBtn = new Button(L("auction.filter.go"));
        lvlApplyBtn.setFontSize(20 * scale);
        lvlApplyBtn.setPreferredSize(new Vector3f(60 * scale, 34 * scale, 0));
        applyBtnBackground(lvlApplyBtn);
        lvlApplyBtn.addCommands(Button.ButtonAction.Down, s -> {
            try { filterMinLevel = Math.max(1, Integer.parseInt(levelField.getText())); loadLots(1); } catch (Exception ignored) {}
        });
        lvlContainer.addChild(lvlApplyBtn);

        windowNode.attachChild(lvlContainer);
        dynamicParts.add(lvlContainer);
        y -= 50 * scale;

        listAndPaginationContainer = new Container();
        listAndPaginationContainer.setLayout(new SpringGridLayout(Axis.Y, Axis.X));
        float containerWidth = winW - 40 * scale;
        listAndPaginationContainer.setPreferredSize(new Vector3f(containerWidth, 100 * scale, 0));
        listAndPaginationContainer.setLocalTranslation(15 * scale + leftShift, y, 0.1f);
        windowNode.attachChild(listAndPaginationContainer);
        dynamicParts.add(listAndPaginationContainer);

        paginationContainer = new Container();
        paginationContainer.setLayout(new SpringGridLayout(Axis.X, Axis.Y));
        paginationContainer.setPreferredSize(new Vector3f(containerWidth, PAGINATION_HEIGHT * scale, 0));

        tooltipContainer = new Container();
        float tooltipWidth = 280 * scale;
        float tooltipHeight = 300 * scale;
        tooltipContainer.setPreferredSize(new Vector3f(tooltipWidth, tooltipHeight, 0));
        tooltipContainer.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.05f, 0.05f, 0.1f, 0.95f)));
        float tooltipX = winW + 30 * scale + leftShift;
        float tooltipY = winH / 2 - tooltipHeight / 2;
        tooltipContainer.setLocalTranslation(tooltipX, tooltipY, 0.1f);
        tooltipContainer.setCullHint(Node.CullHint.Always);
        windowNode.attachChild(tooltipContainer);
        dynamicParts.add(tooltipContainer);

        tooltipLabel = new Label("");
        tooltipLabel.setFontSize(12 * scale * FONT_MULT);
        tooltipLabel.setColor(ColorRGBA.White);
        tooltipLabel.setPreferredSize(new Vector3f(tooltipWidth - 10 * scale, tooltipHeight - 10 * scale, 0));
        tooltipLabel.setInsets(new Insets3f(5 * scale, 5 * scale, 5 * scale, 5 * scale));
        tooltipContainer.addChild(tooltipLabel);

        loadLots(1);
    }

    private void loadLots(int page) {
        this.currentPage = page;
        if (networkManager == null) { updateStatus(L("error.no_network")); return; }
        networkManager.getAuctionList(page, filterType, filterRarity, filterMinLevel, filterMaxLevel)
                .thenAccept(response -> {
                    app.enqueue(() -> {
                        if (response == null) { updateStatus(L("error.load_failed")); return null; }
                        System.out.println("[AuctionWindow] getLots=" + response.getLots().size()
                                + " totalPages=" + response.getTotalPages()
                                + " currentPage=" + response.getCurrentPage());
                        for (AuctionLot lot : response.getLots()) {
                            System.out.println("[AuctionWindow]  lot#" + lot.getId()
                                    + " seller=" + lot.getSellerName()
                                    + " price=" + lot.getPrice()
                                    + " items=" + lot.getItems().size());
                        }
                        this.currentLots = response.getLots();
                        this.totalPages = response.getTotalPages();
                        updateLotList();
                        return null;
                    });
                });
    }

    private void updateLotList() {
        if (listAndPaginationContainer == null) return;
        listAndPaginationContainer.detachAllChildren();

        float rowWidth = listAndPaginationContainer.getPreferredSize().x - 10 * scale;
        if (rowWidth <= 0) rowWidth = 700 * scale;
        float rowHeightScaled = ROW_HEIGHT * scale;

        int displayCount = Math.min(currentLots.size(), 5);

        if (displayCount == 0) {
            Label empty = new Label(L("auction.empty"));
            empty.setFontSize(18 * scale);
            empty.setColor(ColorRGBA.Gray);
            listAndPaginationContainer.addChild(empty);
        } else {
            for (int i = 0; i < displayCount; i++) {
                final AuctionLot lot = currentLots.get(i);
                Container row = new Container();
                row.setLayout(new SpringGridLayout(Axis.X, Axis.Y));
                row.setPreferredSize(new Vector3f(rowWidth, rowHeightScaled, 0));
                row.setInsets(new Insets3f(2 * scale, 2 * scale, 2 * scale, 2 * scale));

                String itemName = lot.getItems().isEmpty() ? "Empty" : lot.getItems().get(0).getName();
                Label infoLabel = new Label("[" + lot.getItems().size() + "] " + itemName + " | " + lot.getPrice() + "g");
                infoLabel.setFontSize(18 * scale);
                infoLabel.setColor(ColorRGBA.White);
                infoLabel.setPreferredSize(new Vector3f(460 * scale, rowHeightScaled - 4 * scale, 0));

                // ============================================================
                // ТАП ПО СТРОКЕ ЛОТА → модалка с характеристиками.
                // Заменяет старый hover-tooltip, которого на Android нет.
                // ============================================================
                MouseEventControl.addListenersToSpatial(infoLabel, new MouseListener() {
                    @Override
                    public void mouseButtonEvent(MouseButtonEvent evt, Spatial s, Spatial t) {
                        if (evt.getButtonIndex() != 0) return;
                        if (!evt.isPressed()) return;
                        System.out.println("[AuctionWindow] lot#" + lot.getId() + " tapped → open detail");
                        showItemDetail(lot);
                    }
                    @Override public void mouseEntered(MouseMotionEvent evt, Spatial s, Spatial t) {}
                    @Override public void mouseExited(MouseMotionEvent evt, Spatial s, Spatial t) {}
                    @Override public void mouseMoved(MouseMotionEvent evt, Spatial s, Spatial t) {}
                });
                row.addChild(infoLabel);

                // Кнопка «Купить» показывается ВСЕГДА, включая свои лоты.
                Button buyBtn = new Button(L("auction.buy"));
                buyBtn.setFontSize(18 * scale);
                buyBtn.setPreferredSize(new Vector3f(100 * scale, rowHeightScaled - 4 * scale, 0));
                buyBtn.setColor(ColorRGBA.Green);
                applyBtnBackground(buyBtn);
                final int lotId = lot.getId();
                buyBtn.addCommands(Button.ButtonAction.Down, s -> handleBuyLot(lotId));
                row.addChild(buyBtn);

                listAndPaginationContainer.addChild(row);
            }
        }

        updatePagination();
        listAndPaginationContainer.addChild(paginationContainer);

        int childCount = listAndPaginationContainer.getChildren().size();
        float totalHeight = childCount * (rowHeightScaled + 2 * scale) + PAGINATION_HEIGHT * scale;
        totalHeight = Math.max(totalHeight, 120 * scale);
        listAndPaginationContainer.setPreferredSize(new Vector3f(
                listAndPaginationContainer.getPreferredSize().x, totalHeight, 0));
    }

    private void updatePagination() {
        paginationContainer.detachAllChildren();
        paginationContainer.setLayout(new SpringGridLayout(Axis.X, Axis.Y));

        Button prevBtn = new Button("<<");
        prevBtn.setFontSize(20 * scale);
        prevBtn.setPreferredSize(new Vector3f(70 * scale, 36 * scale, 0));
        prevBtn.setColor(ColorRGBA.White);
        applyBtnBackground(prevBtn);
        prevBtn.addCommands(Button.ButtonAction.Down, s -> { if (currentPage > 1) loadLots(currentPage - 1); });
        paginationContainer.addChild(prevBtn);

        Label pageNum = new Label(L("auction.page") + " " + currentPage + " / " + totalPages);
        pageNum.setFontSize(20 * scale);
        pageNum.setPreferredSize(new Vector3f(200 * scale, 36 * scale, 0));
        pageNum.setColor(ColorRGBA.White);
        paginationContainer.addChild(pageNum);

        Button nextBtn = new Button(">>");
        nextBtn.setFontSize(20 * scale);
        nextBtn.setPreferredSize(new Vector3f(70 * scale, 36 * scale, 0));
        nextBtn.setColor(ColorRGBA.White);
        applyBtnBackground(nextBtn);
        nextBtn.addCommands(Button.ButtonAction.Down, s -> { if (currentPage < totalPages) loadLots(currentPage + 1); });
        paginationContainer.addChild(nextBtn);
    }

    // ================================================================
    // МОДАЛКА «ХАРАКТЕРИСТИКИ ПРЕДМЕТА»
    // ================================================================

private void showItemDetail(AuctionLot lot) {
    if (lot == null || lot.getItems().isEmpty()) {
        System.out.println("[AuctionWindow] showItemDetail: no items in lot");
        return;
    }
    hideItemDetail();

    if (virtualKeyboard != null && virtualKeyboard.isVisible()) {
        virtualKeyboard.hide();
    }

    Item item = lot.getItems().get(0);

    float w = app.getCamera().getWidth();
    float h = app.getCamera().getHeight();

    Node overlay = new Node("ItemDetailOverlay");

    // Затемнённый фон на весь экран
    final Container backdrop = new Container();
    backdrop.setPreferredSize(new Vector3f(w, h, 0));
    backdrop.setBackground(new QuadBackgroundComponent(new ColorRGBA(0f, 0f, 0f, 0.65f)));
    backdrop.setLocalTranslation(0, 0, 0);
    MouseEventControl.addListenersToSpatial(backdrop, new MouseListener() {
        @Override
        public void mouseButtonEvent(MouseButtonEvent evt, Spatial s, Spatial t) {
            if (evt.getButtonIndex() != 0) return;
            if (!evt.isPressed()) return;
            hideItemDetail();
            evt.setConsumed();
        }
        @Override public void mouseEntered(MouseMotionEvent evt, Spatial s, Spatial t) {}
        @Override public void mouseExited(MouseMotionEvent evt, Spatial s, Spatial t) {}
        @Override public void mouseMoved(MouseMotionEvent evt, Spatial s, Spatial t) {}
    });
    overlay.attachChild(backdrop);

    // ============================================================
    // МОДАЛКА ПОДНЯТА К ВЕРХУ ЭКРАНА.
    // Было: panel по центру по вертикали — panelY = (h - ph) / 2.
    // Стало: панель прижата к верхней части экрана с отступом
    //        70*scale, чтобы не перекрывать HUD снизу и оставить
    //        место для плавающей клавиатуры в правом нижнем углу.
    // ============================================================
    float pw = Math.min(440 * scale, w - 40);
    float ph = Math.min(500 * scale, h - 80);

    float topMargin = 70 * scale;
    float panelY = h - ph - topMargin;
    if (panelY < 10f) panelY = 10f;   // страховка для очень низких экранов

    Container panel = new Container();
    panel.setPreferredSize(new Vector3f(pw, ph, 0));
    panel.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.1f, 0.1f, 0.18f, 0.98f)));
    panel.setLocalTranslation((w - pw) / 2, panelY, 0.1f);

    MouseEventControl.addListenersToSpatial(panel, new MouseListener() {
        @Override
        public void mouseButtonEvent(MouseButtonEvent evt, Spatial s, Spatial t) {
            if (evt.getButtonIndex() != 0) return;
            if (!evt.isPressed()) return;
            evt.setConsumed();
        }
        @Override public void mouseEntered(MouseMotionEvent evt, Spatial s, Spatial t) {}
        @Override public void mouseExited(MouseMotionEvent evt, Spatial s, Spatial t) {}
        @Override public void mouseMoved(MouseMotionEvent evt, Spatial s, Spatial t) {}
    });

    Label content = new Label(buildItemDetailText(lot, item));
    content.setFontSize(14 * scale * FONT_MULT);
    content.setColor(ColorRGBA.White);
    content.setPreferredSize(new Vector3f(pw - 20 * scale, ph - 20 * scale, 0));
    content.setInsets(new Insets3f(12 * scale, 12 * scale, 12 * scale, 12 * scale));
    panel.addChild(content);

    overlay.attachChild(panel);

    // Кнопка «Закрыть» — сразу под панелью
    Button closeBtn = new Button(L("ui.close_button"));
    closeBtn.setFontSize(18 * scale * FONT_MULT);
    closeBtn.setPreferredSize(new Vector3f(200 * scale, 50 * scale, 0));
    closeBtn.setColor(ColorRGBA.White);
    applyBtnBackground(closeBtn);
    closeBtn.setLocalTranslation(
            (w - 200 * scale) / 2,
            panelY - 60 * scale,
            0.2f);
    closeBtn.addCommands(Button.ButtonAction.Down, s -> hideItemDetail());
    overlay.attachChild(closeBtn);

    uiManager.getGuiNode().attachChild(overlay);
    itemDetailOverlay = overlay;

    System.out.println("[AuctionWindow] item detail opened for lot#" + lot.getId()
            + " at panelY=" + panelY);
}
    private void hideItemDetail() {
        if (itemDetailOverlay != null) {
            if (itemDetailOverlay.getParent() != null) {
                itemDetailOverlay.removeFromParent();
            }
            itemDetailOverlay = null;
            System.out.println("[AuctionWindow] item detail closed");
        }
    }

    /**
     * Собирает текст характеристик предмета.
     *
     * Через reflection — так код компилируется независимо от того,
     * какие именно геттеры определены в Item (getDamage / getAttack /
     * getDefense / getArmor / getHealthBonus / getHpBonus и т.п.).
     * Если геттера нет — просто пропускаем строку.
     */
    private String buildItemDetailText(AuctionLot lot, Item item) {
        StringBuilder sb = new StringBuilder();

        sb.append(value(item, "getName")).append("\n\n");

        String type = value(item, "getType");
        if (!type.isEmpty())     sb.append("Type: ").append(type).append("\n");

        String level = value(item, "getLevel");
        if (!level.isEmpty())    sb.append("Level: ").append(level).append("\n");

        String rarity = value(item, "getRarity");
        if (!rarity.isEmpty())   sb.append("Rarity: ").append(rarity).append("\n");

        sb.append("\n");

        appendIfPositive(sb, "Damage",   item, "getDamage");
        appendIfPositive(sb, "Attack",   item, "getAttack");
        appendIfPositive(sb, "Defense",  item, "getDefense");
        appendIfPositive(sb, "Armor",    item, "getArmor");
        appendIfPositive(sb, "Health",   item, "getHealthBonus");
        appendIfPositive(sb, "Mana",     item, "getManaBonus");
        appendIfPositive(sb, "Sockets",  item, "getSocketCount");

        String desc = value(item, "getDescription");
        if (!desc.isEmpty()) {
            sb.append("\n").append(desc).append("\n");
        }

        sb.append("\nPrice: ").append(lot.getPrice()).append(" g");
        if (lot.getEndTime() > 0) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                sb.append("\nEnds: ").append(sdf.format(new Date(lot.getEndTime())));
            } catch (Exception ignored) {}
        }

        return sb.toString();
    }

    private void appendIfPositive(StringBuilder sb, String label, Item item, String getter) {
        try {
            Object v = item.getClass().getMethod(getter).invoke(item);
            if (v instanceof Number) {
                int n = ((Number) v).intValue();
                if (n > 0) sb.append(label).append(": +").append(n).append("\n");
            }
        } catch (Exception ignored) {
            // геттера нет — молча пропускаем
        }
    }

    private String value(Item item, String getter) {
        try {
            Object v = item.getClass().getMethod(getter).invoke(item);
            return v == null ? "" : String.valueOf(v);
        } catch (Exception e) {
            return "";
        }
    }

    // ================================================================
    // SELL TAB
    // ================================================================
    private void showSellTab() {
        clearDynamicParts();
        float startY = winH - 140 * scale;
        float cellSize = 65 * scale;

        Label header = new Label(L("auction.sell.header"));
        header.setFontSize(14 * scale * FONT_MULT);
        header.setColor(ColorRGBA.White);
        header.setPreferredSize(new Vector3f(winW - 30 * scale, 40 * scale, 0));
        header.setLocalTranslation(15 * scale + leftShift, startY, 0.1f);
        windowNode.attachChild(header);
        dynamicParts.add(header);

        Container gridContainer = new Container();
        SpringGridLayout gridLayout = new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.None);
        gridContainer.setLayout(gridLayout);
        gridContainer.setPreferredSize(new Vector3f(600 * scale, 350 * scale, 0));
        gridContainer.setLocalTranslation(15 * scale + leftShift, startY - 50 * scale, 0.1f);
        windowNode.attachChild(gridContainer);
        dynamicParts.add(gridContainer);

        List<Item> items = inventoryManager.getItems();
        int itemIndex = 0;

        for (int row = 0; row < 5; row++) {
            Container rowContainer = new Container();
            SpringGridLayout rowLayout = new SpringGridLayout(Axis.X, Axis.Y, FillMode.None, FillMode.None);
            rowContainer.setLayout(rowLayout);
            rowContainer.setPreferredSize(new Vector3f(600 * scale, cellSize, 0));
            rowContainer.setInsets(new Insets3f(4f, 4f, 4f, 4f));
            gridContainer.addChild(rowContainer);

            for (int col = 0; col < 4; col++) {
                if (itemIndex >= 20) break;
                Item item = (itemIndex < items.size()) ? items.get(itemIndex) : null;
                int realSlot = (item != null) ? inventoryManager.getSlotIndex(item) : -1;

                Button cell = new Button("");
                cell.setPreferredSize(new Vector3f(cellSize, cellSize, 0));
                cell.setBackground(new QuadBackgroundComponent(new ColorRGBA(0.2f, 0.2f, 0.3f, 0.9f)));

                if (item != null) {
                    Texture tex = null;
                    try { tex = app.getAssetManager().loadTexture(item.getIconPath()); } catch (Exception ignored) {}
                    if (tex != null) cell.setBackground(new QuadBackgroundComponent(tex));
                    else cell.setBackground(new QuadBackgroundComponent(item.getFallbackColor()));

                    if (realSlot == selectedSlot) {
                        cell.setBackground(new QuadBackgroundComponent(ColorRGBA.Yellow));
                    }

                    final int slot = realSlot;
                    cell.addCommands(Button.ButtonAction.Down, s -> {
                        if (selectedSlot == slot) selectedSlot = -1;
                        else selectedSlot = slot;
                        app.enqueue(() -> { showSellTab(); return null; });
                    });
                }

                rowContainer.addChild(cell);
                itemIndex++;
            }
        }

        float gridBottomY = startY - 50 * scale - 350 * scale;
        float priceY = gridBottomY - 55 * scale;

        Container priceContainer = new Container();
        SpringGridLayout priceLayout = new SpringGridLayout(Axis.X, Axis.Y, FillMode.None, FillMode.None);
        priceContainer.setLayout(priceLayout);
        priceContainer.setPreferredSize(new Vector3f(600 * scale, 50 * scale, 0));
        priceContainer.setLocalTranslation(15 * scale + leftShift, priceY, 0.1f);
        windowNode.attachChild(priceContainer);
        dynamicParts.add(priceContainer);

        Label priceLabel = new Label(L("auction.sell.price"));
        priceLabel.setFontSize(14 * scale * FONT_MULT);
        priceContainer.addChild(priceLabel);

        priceInput = new TextField("100");
        priceInput.setPreferredSize(new Vector3f(120 * scale, 40 * scale, 0));
        priceInput.setFontSize(24 * scale);
        priceContainer.addChild(priceInput);
        installKeyboardTrigger(priceInput);

        Button sellNowBtn = new Button(L("auction.sell.list"));
        sellNowBtn.setFontSize(22 * scale);
        sellNowBtn.setPreferredSize(new Vector3f(200 * scale, 44 * scale, 0));
        sellNowBtn.setColor(ColorRGBA.Green);
        applyBtnBackground(sellNowBtn);
        sellNowBtn.addCommands(Button.ButtonAction.Down, s -> {
            System.out.println("[AuctionWindow] Sell clicked. selectedSlot=" + selectedSlot);
            if (selectedSlot == -1) { updateStatus(L("auction.sell.select")); return; }
            Item selectedItem = inventoryManager.getItemAtSlot(selectedSlot);
            System.out.println("[AuctionWindow] Sell item lookup: " + (selectedItem == null ? "null" : selectedItem.getName()));
            if (selectedItem == null) { updateStatus(L("auction.sell.empty")); return; }
            int price = 100;
            try { price = Integer.parseInt(priceInput.getText()); } catch (Exception ignored) {}
            System.out.println("[AuctionWindow] Sell proceeding: slot=" + selectedSlot + " price=" + price);
            handleCreateLot(Arrays.asList(selectedSlot), price);
        });
        priceContainer.addChild(sellNowBtn);

        if (statusLabel != null && statusLabel.getParent() != null) {
            statusLabel.removeFromParent();
            windowNode.attachChild(statusLabel);
            statusLabel.setLocalTranslation(15 * scale + leftShift, 15 * scale, 0.5f);
        }
    }

    private void handleBuyLot(int lotId) {
        hideItemDetail();
        networkManager.buyAuctionLot(lotId).thenAccept(response -> {
            if (response != null) {
                app.enqueue(() -> {
                    currentLots.removeIf(lot -> lot.getId() == lotId);
                    updateLotList();
                    updateStatus(L("auction.status.buy.success"));
                    uiManager.applyCharacterData(response);
                });
            } else {
                app.enqueue(() -> updateStatus(L("auction.status.buy.fail")));
            }
        }).exceptionally(ex -> {
            app.enqueue(() -> updateStatus(L("error.network") + ex.getMessage()));
            return null;
        });
    }

    private void handleCreateLot(List<Integer> slotIndices, int price) {
        System.out.println("[AuctionWindow] handleCreateLot slotIndices=" + slotIndices + " price=" + price);
        if (networkManager == null) { updateStatus(L("error.no_network")); return; }
        networkManager.createAuctionLot(slotIndices, price).thenAccept(response -> {
            app.enqueue(() -> {
                System.out.println("[AuctionWindow] createAuctionLot response: " + response);
                if (response == null) { updateStatus(L("auction.status.create.fail")); return; }
                if (response.containsKey("error")) {
                    updateStatus(L("auction.status.create.fail") + " " + response.get("error"));
                    return;
                }
                uiManager.applyCharacterData(response);
                updateStatus(L("auction.status.create.success") + " " + price + "g!");
                selectedSlot = -1;
                showSellTab();
            });
        }).exceptionally(ex -> {
            System.out.println("[AuctionWindow] createAuctionLot EXCEPTION: " + ex.getMessage());
            ex.printStackTrace(System.out);
            app.enqueue(() -> updateStatus(L("error.network") + ex.getMessage()));
            return null;
        });
    }

    private void updateStatus(String msg) {
        if (statusLabel == null) return;
        System.out.println("[AuctionWindow] status: " + msg);
        statusLabel.setText(msg == null ? "" : msg);
        if (statusLabel.getParent() == null) {
            windowNode.attachChild(statusLabel);
        }
        statusLabel.setCullHint(msg == null || msg.isEmpty()
                ? Node.CullHint.Always
                : Node.CullHint.Dynamic);
    }

    public void show() {
        if (isVisible) return;
        isVisible = true;
        updateGold();
        if (windowNode.getParent() == null) uiManager.getGuiNode().attachChild(windowNode);
        uiManager.onTraderOpened(windowNode);
    }

    public void hide() {
        if (!isVisible) return;
        isVisible = false;
        selectedSlot = -1;

        // Закрываем модалку предмета и клавиатуру — они не должны
        // висеть поверх HUD после закрытия аукциона.
        hideItemDetail();
        if (virtualKeyboard != null && virtualKeyboard.isVisible()) {
            virtualKeyboard.hide();
        }

        if (windowNode.getParent() != null) uiManager.getGuiNode().detachChild(windowNode);
        if (statusLabel != null) { statusLabel.setText(""); statusLabel.setCullHint(Node.CullHint.Always); }
        if (tooltipContainer != null) tooltipContainer.setCullHint(Node.CullHint.Always);
        uiManager.onTraderClosed(windowNode);
    }

    public void toggle() { if (isVisible) hide(); else show(); }
    public void updateGold() { if (goldLabel != null) goldLabel.setText(L("gold.label") + playerManager.getGold()); }
    public boolean isVisible() { return isVisible; }

    public void updateLayout(int screenWidth, int screenHeight) {
        if (isVisible) {
            windowNode.detachAllChildren();
            createWindow();
            uiManager.getGuiNode().attachChild(windowNode);
        }
    }
}