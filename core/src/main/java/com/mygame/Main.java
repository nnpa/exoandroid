package com.mygame;

import com.atr.jme.font.TrueTypeFont;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.collision.CollisionResults;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.TouchInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.controls.TouchListener;
import com.jme3.input.controls.TouchTrigger;
import com.jme3.input.event.*;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.AppSettings;
import com.jme3.util.SkyFactory;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.style.Attributes;
import com.simsilica.lemur.style.Styles;
import com.mygame.managers.*;
import com.mygame.managers.GameManager.GameState;
import com.mygame.monsters.Monster;

import java.util.Map;

public class Main extends SimpleApplication {

    private static Main instance;
    private GameManager gameManager;
    private NetworkManager networkManager;
    private UIManager uiManager;
    private WorldManager worldManager;
    private PlayerManager playerManager;
    private InventoryManager inventoryManager;
    private DropManager dropManager;
    private boolean isInitialized = false;
    private boolean worldLoaded = false;
    private BulletAppState bulletAppState;
    private CameraFollowControl cameraControl;
    private boolean isRotating = false;
    private float lastMouseX = 0;

    private boolean touchActive = false;
    private boolean touchDragging = false;
    private float touchStartX = 0f;
    private float touchStartY = 0f;
    private float touchTravelDistance = 0f;
    private static final float TAP_MAX_DISTANCE = 35f;
    private static final float ROTATE_SENSITIVITY = 0.006f;
    private static final float ZOOM_SENSITIVITY = 10f;

    @Override
    public void simpleInitApp() {
        setPauseOnLostFocus(false);
        rootNode.setCullHint(Spatial.CullHint.Always);
        instance = this;

        Spatial sky = SkyFactory.createSky(assetManager, "Interface/Sky/sky.png", SkyFactory.EnvMapType.EquirectMap);
        rootNode.attachChild(sky);

        setDisplayFps(false);
        setDisplayStatView(false);
        viewPort.setBackgroundColor(ColorRGBA.Black);

        SoundManager.initialize(this);

        bulletAppState = new BulletAppState();
        bulletAppState.setEnabled(false);
        bulletAppState.setThreadingType(BulletAppState.ThreadingType.SEQUENTIAL);
        stateManager.attach(bulletAppState);

        setupLighting();
        GuiGlobals.initialize(this);
        applySkinStyle();
        applyTextFieldStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");

        flyCam.setEnabled(false);
        inputManager.setCursorVisible(true);

        // КРИТИЧНО: включает генерацию симулированных mouse-событий
        // из TouchEvent. Благодаря этому Lemur-кнопки работают как
        // обычные кнопки мыши, а наш ActionListener на "MouseClick"
        // получает тап по не-виджетам (ячейки инвентаря).
        inputManager.setSimulateMouse(true);

        initializeManagers();

        if (playerManager != null && playerManager.getPlayerNode() != null) {
            cameraControl = new CameraFollowControl(cam, playerManager.getPlayerNode());
            playerManager.getPlayerNode().addControl(cameraControl);
            cameraControl.setCameraAngle(0f);
        }
        if (playerManager != null) playerManager.setPhysicsSpace(bulletAppState.getPhysicsSpace());
        if (worldManager != null) worldManager.setBulletAppState(bulletAppState);
        if (uiManager != null && playerManager != null) uiManager.setPlayerManager(playerManager);
        if (uiManager != null && inventoryManager != null) uiManager.setInventoryManager(inventoryManager);
        if (dropManager != null && inventoryManager != null) dropManager.setInventoryManager(inventoryManager);
        if (playerManager != null) {
            playerManager.setWorldManager(worldManager);
            playerManager.setDropManager(dropManager);
        }

        setupInput();
        Monster.setApp(this);
        playerManager.setUIManager(uiManager);
        playerManager.setNetworkManager(networkManager);

        if (uiManager != null) uiManager.autoLoginDevice();
    }

    private void setupInput() {

        // ============================================================
        // MOUSE CLICK (десктоп + симулированный тач)
        //
        // Приоритет:
        //   1. Инвентарь   — обрабатываем сами (Geometry не виджеты Lemur)
        //   2. Другие окна — Lemur уже consumed, событие сюда не дойдёт
        //   3. Мир         — движение / атака / подбор дропа
        // ============================================================
        inputManager.addMapping("MouseClick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {

                if (!isPressed || !"MouseClick".equals(name)) return;

                // ----------------------------------------------------
                // 1. ИНВЕНТАРЬ
                // ----------------------------------------------------
                if (inventoryManager != null && inventoryManager.isVisible()) {
                    Vector2f cursor = inputManager.getCursorPosition();
                    inventoryManager.onTouchDown(cursor.x, cursor.y);
                    return;
                }

                // ----------------------------------------------------
                // 2. ДРУГИЕ ОКНА — их обрабатывает Lemur сам
                // ----------------------------------------------------
                if (uiManager != null && uiManager.isAnyWindowOpen()) {
                    return;
                }

                // ----------------------------------------------------
                // 3. МИР
                // ----------------------------------------------------
                if (playerManager == null) return;
                if (touchActive) return;

                Vector2f cursor = inputManager.getCursorPosition();
                if (isOverGui(cursor.x, cursor.y)) return;
                handleClick(cursor.x, cursor.y);
            }
        }, "MouseClick");

        // ============================================================
        // RIGHT MOUSE - вращение камеры
        // ============================================================
        inputManager.addMapping("RotateCamera", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if ("RotateCamera".equals(name)) {
                    isRotating = isPressed;
                    if (isPressed) lastMouseX = inputManager.getCursorPosition().x;
                }
            }
        }, "RotateCamera");

        // ============================================================
        // RAW INPUT LISTENER (мышь на десктопе)
        // ============================================================
        inputManager.addRawInputListener(new RawInputListener() {
            @Override public void beginInput() {}
            @Override public void endInput() {}
            @Override public void onJoyAxisEvent(JoyAxisEvent evt) {}
            @Override public void onJoyButtonEvent(JoyButtonEvent evt) {}
            @Override public void onKeyEvent(KeyInputEvent evt) {}
            @Override public void onMouseButtonEvent(MouseButtonEvent evt) {}
            @Override public void onTouchEvent(TouchEvent evt) {}

            @Override
            public void onMouseMotionEvent(MouseMotionEvent evt) {
                if (isRotating && cameraControl != null) {
                    float currentX = inputManager.getCursorPosition().x;
                    float deltaX = currentX - lastMouseX;
                    cameraControl.rotate(deltaX * 0.005f);
                    lastMouseX = currentX;
                }
            }
        });

        // ============================================================
        // TOUCH LISTENER — только для МИРА
        //
        // Инвентарь обрабатывается через ActionListener на MouseClick
        // (симулированные события мыши). Здесь — только движение,
        // атака, подбор дропа, вращение камеры одним пальцем и зум.
        //
        // Y уже в системе jME (origin снизу-слева), дополнительной
        // инверсии НЕ нужно.
        // ============================================================
        inputManager.addMapping("TouchAll", new TouchTrigger(TouchInput.ALL));
        inputManager.addListener(new TouchListener() {
            @Override
            public void onTouch(String name, TouchEvent evt, float tpf) {

                /*
                 * ВАЖНО: тут раньше стояли два ранних return —
                 * "if (evt.isConsumed()) return;" и
                 * "if (isAnyWindowOpen()) return;" — оба выполнялись
                 * ДО switch, а значит блокировали ВООБЩЕ ВСЁ, включая
                 * диспетчеризацию в инвентарь, всякий раз когда открыто
                 * окно (что как раз и есть момент, когда клики по
                 * инвентарю обязаны доходить). Именно поэтому
                 * прошлая правка не дала эффекта — код ниже физически
                 * не мог выполниться. Теперь эти проверки применяются
                 * точечно — только там, где реально нужны (вращение
                 * камеры и мировой клик), а не глобально.
                 */

                boolean windowOpen = uiManager != null && uiManager.isAnyWindowOpen();

                float tx = evt.getX();
                float ty = evt.getY();

                switch (evt.getType()) {

                    case DOWN: {
                        touchActive = true;
                        touchDragging = false;
                        touchTravelDistance = 0f;
                        touchStartX = tx;
                        touchStartY = ty;
                        break;
                    }

                    case MOVE: {
                        if (!touchActive) break;
                        float dx = evt.getDeltaX();
                        float totalDx = tx - touchStartX;
                        float totalDy = ty - touchStartY;
                        touchTravelDistance = (float) Math.sqrt(totalDx * totalDx + totalDy * totalDy);
                        if (touchTravelDistance > TAP_MAX_DISTANCE) {
                            touchDragging = true;
                            /*
                             * Камера крутится только если НЕ открыто
                             * окно — иначе драг внутри инвентаря
                             * (например, свайп по списку) случайно
                             * крутил бы камеру в мире позади него.
                             */
                            if (!windowOpen && cameraControl != null) {
                                cameraControl.rotate(dx * ROTATE_SENSITIVITY);
                            }
                        }
                        break;
                    }

                    case UP: {

                        /*
                         * Используем tx/ty — настоящие координаты
                         * ИЗ САМОГО touch-события, без посредников
                         * вроде inputManager.getCursorPosition()
                         * (которая на Android при первом касании
                         * нового жеста иногда ещё не успевает
                         * обновиться до реальной точки пальца).
                         */
                        if (!touchDragging) {

                            if (inventoryManager != null && inventoryManager.isVisible()) {

                                // Инвентарь — обрабатываем ВСЕГДА,
                                // именно ради этого случая и открыто окно.
                                inventoryManager.onTouchDown(tx, ty);

                            } else if (!windowOpen) {

                                if (playerManager != null && !isOverGui(tx, ty)) {
                                    handleClick(tx, ty);
                                }
                            }
                        }

                        touchActive = false;
                        touchDragging = false;
                        touchTravelDistance = 0f;
                        break;
                    }

                    case SCALE_MOVE: {
                        if (!windowOpen && cameraControl != null) {
                            float scaleFactor = evt.getScaleFactor();
                            float delta = (1f - scaleFactor) * ZOOM_SENSITIVITY;
                            cameraControl.zoom(delta);
                        }
                        touchDragging = true;
                        break;
                    }

                    default: break;
                }
            }
        }, "TouchAll");

        // ============================================================
        // Клавиши
        // ============================================================
        inputManager.addMapping("ReturnToCity", new KeyTrigger(KeyInput.KEY_N));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (isPressed && "ReturnToCity".equals(name) && worldLoaded
                        && gameManager != null
                        && gameManager.getCurrentState() == GameState.DUNGEON) {
                    worldManager.returnToCity();
                    gameManager.setState(GameState.CITY);
                }
            }
        }, "ReturnToCity");

        inputManager.addMapping("PrintCoordinates", new KeyTrigger(KeyInput.KEY_Z));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (isPressed && "PrintCoordinates".equals(name) && playerManager != null) {
                    Vector3f pos = playerManager.getPosition();
                    System.out.println("[Player] x=" + pos.x + ", y=" + pos.y + ", z=" + pos.z);
                }
            }
        }, "PrintCoordinates");

        inputManager.addMapping("OpenEditor", new KeyTrigger(KeyInput.KEY_F12));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (isPressed && worldLoaded && worldManager != null) {
                    worldManager.openEditorForCurrentDungeon();
                }
            }
        }, "OpenEditor");

        inputManager.addMapping("ZoomIn", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        inputManager.addMapping("ZoomOut", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
        inputManager.addListener(new AnalogListener() {
            @Override
            public void onAnalog(String name, float value, float tpf) {
                if (uiManager != null && uiManager.isAnyWindowOpen()) return;
                if (cameraControl == null) return;
                if ("ZoomIn".equals(name)) cameraControl.zoom(-1.5f);
                else if ("ZoomOut".equals(name)) cameraControl.zoom(1.5f);
            }
        }, "ZoomIn", "ZoomOut");
    }

    private boolean isOverGui(float sx, float sy) {
        CollisionResults results = new CollisionResults();
        com.jme3.math.Ray ray = new com.jme3.math.Ray(
                new Vector3f(sx, sy, 1000f),
                new Vector3f(0f, 0f, -1f));
        guiNode.collideWith(ray, results);
        for (int i = 0; i < results.size(); i++) {
            Spatial hit = results.getCollision(i).getGeometry();
            Boolean pickable = hit.getUserData("pickable");
            if (pickable != null && !pickable) continue;
            if (!isEffectivelyVisible(hit)) continue;
            return true;
        }
        return false;
    }

    private boolean isEffectivelyVisible(Spatial s) {
        Spatial cur = s;
        while (cur != null) {
            if (cur.getCullHint() == Spatial.CullHint.Always) return false;
            if (cur.getCullHint() == Spatial.CullHint.Never) return true;
            cur = cur.getParent();
        }
        return true;
    }

    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);
        if (!Monster.isGameRunning) return;
        if (gameManager != null) gameManager.update(tpf);
        if (playerManager != null) playerManager.update(tpf);
        if (worldManager != null) worldManager.update(tpf);
        if (uiManager != null) { uiManager.update(tpf); uiManager.updateMap(tpf); }
        if (dropManager != null) dropManager.update(tpf);
    }

    private TrueTypeFont ttfFont;

    public void loadTalentsFromServer() {
        if (networkManager == null || uiManager == null) return;
        networkManager.loadTalents().thenAccept(data -> {
            if (data == null) return;
            @SuppressWarnings("unchecked")
            Map<String, Integer> talents = (Map<String, Integer>) data.get("talents");
            int points = (int) data.get("availablePoints");
            this.enqueue(() -> {
                TalentManager tm = uiManager.getTalentManager();
                TalentWindow tw = uiManager.getTalentWindow();
                if (tm != null) {
                    tm.loadFromServer(talents, points);
                    if (tw != null && tw.isVisible()) tw.updateUI();
                }
                return null;
            });
        }).exceptionally(ex -> {
            System.err.println("[Main] Failed to load talents: " + ex.getMessage());
            return null;
        });
    }

    public void loadGameWorld() {
        rootNode.setCullHint(Spatial.CullHint.Never);
        if (worldLoaded) return;
        System.out.println("[Main] ===== ЗАГРУЗКА МИРА =====");
        bulletAppState.setEnabled(true);
        PhysicsSpace space = bulletAppState.getPhysicsSpace();
        space.setAccuracy(1f / 60f);
        space.setMaxSubSteps(4);
        space.setGravity(new Vector3f(0, -30f, 0));
        bulletAppState.setSpeed(0.8f);

        worldManager.loadCityWithPhysics();

        if (playerManager != null) {
            Node playerNode = playerManager.getPlayerNode();
            if (playerNode != null && !rootNode.hasChild(playerNode)) rootNode.attachChild(playerNode);
            Vector3f spawnPos = new Vector3f(0f, 0.5f, -8f);
            playerManager.setPosition(spawnPos);
            if (playerManager.getCharacterControl() != null) {
                playerManager.getCharacterControl().warp(spawnPos);
                playerManager.getCharacterControl().setWalkDirection(Vector3f.ZERO);
            }
        }
        gameManager.setState(GameState.CITY);
        worldManager.switchToCity();
        worldLoaded = true;
        loadTalentsFromServer();
        if (worldManager != null && worldManager.getDungeonNode() != null) {
            uiManager.initMap(worldManager.getDungeonNode(), playerManager);
        }
    }

    public void applyResolution(int width, int height) {
        AppSettings settings = new AppSettings(true);
        settings.setWidth(width);
        settings.setHeight(height);
        setSettings(settings);
        restart();
    }

    @Override
    public void stop() {
        Monster.isGameRunning = false;
        if (worldManager != null) worldManager.cleanup();
        super.stop();
    }

    private void handleClick(float screenX, float screenY) {
        if (!worldLoaded || playerManager == null) return;

        if (worldManager != null && worldManager.getTreasureManager() != null) {
            Treasure treasure = worldManager.getTreasureManager().getTreasureAt(screenX, screenY);
            if (treasure != null) { worldManager.getTreasureManager().openTreasure(treasure); return; }
        }

        if (dropManager != null) {
            DropManager.DropItem drop = dropManager.getDropAt(screenX, screenY);
            if (drop != null) { dropManager.pickupDrop(drop); return; }
        }

        Vector3f groundPoint = getGroundPoint(screenX, screenY);
        if (groundPoint == null) return;

        boolean inCity = gameManager != null && gameManager.getCurrentState() == GameState.CITY;
        if (inCity) {
            Spatial teleporter = findNpc(worldManager.getCityNode(), "Teleporter", groundPoint);
            if (teleporter != null) { uiManager.showTeleporterDialog(); return; }
            Spatial npc = findNpc(worldManager.getNpcNode(), "NPC_Trader", groundPoint);
            if (npc != null) { uiManager.openTrader(); return; }
            Spatial auc = findNpc(worldManager.getNpcNode(), "NPC_Auctioneer", groundPoint);
            if (auc != null) { uiManager.openAuction(); return; }
            Spatial smith = findNpc(worldManager.getNpcNode(), "NPC_Blacksmith", groundPoint);
            if (smith != null) { uiManager.toggleBlacksmith(); return; }
        }

        Spatial clicked = worldManager.getClosestInteractiveObject(groundPoint, 5.5f);
        if (clicked != null) {
            String n = clicked.getName();
            if ("TestMonster".equals(n) || "Monster".equals(n)) {
                playerManager.attackTarget(clicked);
                return;
            }
        }
        playerManager.moveTo(groundPoint);
    }

    private Spatial findNpc(Node parent, String name, Vector3f point) {
        if (parent == null) return null;
        for (Spatial c : parent.getChildren()) {
            if (name.equals(c.getName()) && c.getWorldTranslation().distance(point) < 2.5f) return c;
        }
        return null;
    }

    private Vector3f getGroundPoint(float sx, float sy) {
        Vector3f origin = cam.getWorldCoordinates(new Vector2f(sx, sy), 0f);
        Vector3f dir = cam.getWorldCoordinates(new Vector2f(sx, sy), 1f).subtract(origin).normalizeLocal();
        if (dir.y == 0) return null;
        float t = -origin.y / dir.y;
        if (t < 0) return null;
        Vector3f hit = origin.add(dir.mult(t));
        hit.y = 0;
        return hit;
    }

    private DirectionalLightShadowRenderer shadowRenderer;

    private void setupLighting() {
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-1f, -2f, -1f).normalizeLocal());
        sun.setColor(ColorRGBA.White.mult(1.2f));
        rootNode.addLight(sun);

        shadowRenderer = new DirectionalLightShadowRenderer(assetManager, 2048, 3);
        shadowRenderer.setLight(sun);
        shadowRenderer.setEdgeFilteringMode(EdgeFilteringMode.PCFPOISSON);
        shadowRenderer.setShadowZExtend(100f);
        viewPort.addProcessor(shadowRenderer);

        DirectionalLight fill = new DirectionalLight();
        fill.setDirection(new Vector3f(1f, -1f, 1f).normalizeLocal());
        fill.setColor(new ColorRGBA(0.6f, 0.6f, 0.7f, 1f).mult(0.8f));
        rootNode.addLight(fill);

        AmbientLight amb = new AmbientLight();
        amb.setColor(new ColorRGBA(0.6f, 0.6f, 0.6f, 1f));
        rootNode.addLight(amb);
    }

    private void applyTextFieldStyle() {
        Styles styles = GuiGlobals.getInstance().getStyles();
        Attributes attrs = styles.getSelector(TextField.ELEMENT_ID, null);
        attrs.set("background", new QuadBackgroundComponent(new ColorRGBA(0.25f, 0.25f, 0.35f, 0.9f)));
        attrs.set("color", ColorRGBA.Black);
        attrs.set("fontSize", 18f);
        attrs.set("insets", new Insets3f(2, 6, 2, 6));
    }

    private void applySkinStyle() {
        Styles styles = GuiGlobals.getInstance().getStyles();
        Attributes la = styles.getSelector(Label.ELEMENT_ID, null);
        la.set("fontSize", 18f);
        la.set("color", new ColorRGBA(0.9f, 0.7f, 0.4f, 1f));

        Attributes tfa = styles.getSelector(TextField.ELEMENT_ID, null);
        tfa.set("background", new QuadBackgroundComponent(new ColorRGBA(0.2f, 0.1f, 0.03f, 0.9f)));
        tfa.set("color", ColorRGBA.White);
        tfa.set("fontSize", 18f);
    }

    private MapRenderer mapRenderer;
    private MapWindow mapWindow;

    private void initializeManagers() {
        if (isInitialized) return;

        networkManager = new NetworkManager(this);
        networkManager.initialize();
        gameManager = new GameManager(this);
        gameManager.initialize();
        playerManager = new PlayerManager(this);
        playerManager.initialize();
        worldManager = new WorldManager(this);
        worldManager.initialize();
        uiManager = new UIManager(this);
        uiManager.initialize();
        inventoryManager = new InventoryManager(this, guiNode);
        dropManager = new DropManager(this, guiNode);

        uiManager.setPlayerManager(playerManager);
        uiManager.setInventoryManager(inventoryManager);

        TalentManager talentManager = new TalentManager(playerManager, networkManager);
        uiManager.setTalentManager(talentManager);
        playerManager.setTalentManager(talentManager);

        worldManager.setNetworkManager(networkManager);
        worldManager.setPlayerManager(playerManager);
        worldManager.setDropManager(dropManager);
        worldManager.setUIManager(uiManager);

        playerManager.setWorldManager(worldManager);
        playerManager.setDropManager(dropManager);

        dropManager.setInventoryManager(inventoryManager);

        gameManager.setNetworkManager(networkManager);
        gameManager.setPlayerManager(playerManager);
        gameManager.setWorldManager(worldManager);
        gameManager.setUIManager(uiManager);

        uiManager.setWorldManager(worldManager);

        mapRenderer = new MapRenderer(this);
        mapRenderer.initialize();
        mapWindow = new MapWindow(this, mapRenderer, playerManager);

        isInitialized = true;
    }

    public static Main getInstance() { return instance; }
    public GameManager getGameManager() { return gameManager; }
    public NetworkManager getNetworkManager() { return networkManager; }
    public UIManager getUIManager() { return uiManager; }
    public WorldManager getWorldManager() { return worldManager; }
    public PlayerManager getPlayerManager() { return playerManager; }
    public InventoryManager getInventoryManager() { return inventoryManager; }
    public DropManager getDropManager() { return dropManager; }
    public boolean isWorldLoaded() { return worldLoaded; }

    @Override
    public void destroy() {
        SoundManager.cleanup();
        super.destroy();
    }
}