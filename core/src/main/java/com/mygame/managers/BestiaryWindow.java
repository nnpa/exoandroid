package com.mygame.managers;

import com.jme3.anim.AnimComposer;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.ui.Picture;

import com.simsilica.lemur.Button;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


/**
 * ============================================================
 * BESTIARY WINDOW
 * ============================================================
 *
 * Бестиарий без Container.
 *
 * 3D модель рисуется отдельным ViewPort.
 *
 * На экране остаются только:
 *
 *      <       >       X
 *
 * ViewPort автоматически центрируется относительно экрана.
 */
public class BestiaryWindow {

    // ============================================================
    // ОСНОВНЫЕ ОБЪЕКТЫ
    // ============================================================

    private final SimpleApplication app;
    private final UIManager uiManager;
    private final AssetManager assetManager;

    private final Node guiNode;

    // ============================================================
    // 3D
    // ============================================================

    private Node modelRootNode;

    private Camera modelCam;

    private ViewPort modelViewPort;

    private BestiaryAppState appState;

    // ============================================================
    // СВЕТ
    // ============================================================

    private AmbientLight ambientLight;
    private DirectionalLight directionalLight;

    // ============================================================
    // МОДЕЛЬ
    // ============================================================

    private Spatial currentModel;

    private AnimComposer currentAnimComposer;

    // ============================================================
    // МОДЕЛИ
    // ============================================================

    private final List<String> models =
            new ArrayList<>();

    private int currentIndex = 0;

    // ============================================================
    // GUI КНОПКИ
    // ============================================================

    private Button leftButton;
    private Button rightButton;
    private Button closeButton;

    // ============================================================
    // ПИКЧЕР ФОНА
    // ============================================================

    /*
     * Здесь намеренно НЕ создаём серый фон.
     *
     * ViewPort прозрачный.
     *
     * Поэтому кожаный фон GUI остаётся видимым.
     */
    private Picture backgroundPicture;

    // ============================================================
    // РАЗМЕР VIEWPORT
    // ============================================================

    /*
     * ViewPort занимает большую часть экрана.
     *
     * Размер вычисляется от текущего разрешения.
     */
    private float viewportWidth;
    private float viewportHeight;

    private float viewportX;
    private float viewportY;

    // ============================================================
    // ОТСТУПЫ
    // ============================================================

    private static final float SIDE_MARGIN = 100f;

    private static final float TOP_MARGIN = 80f;

    private static final float BOTTOM_MARGIN = 90f;

    // ============================================================
    // Z
    // ============================================================

    /*
     * Кнопки должны быть выше остальных GUI элементов.
     */
    private static final float BUTTON_Z = 5000f;

    // ============================================================
    // КОНСТРУКТОР
    // ============================================================

    public BestiaryWindow(
            SimpleApplication app,
            UIManager uiManager
    ) {

        this.app = app;
        this.uiManager = uiManager;
        this.assetManager = app.getAssetManager();
        this.guiNode = app.getGuiNode();

        // --------------------------------------------------------
        // Ищем модели
        // --------------------------------------------------------

        scanModelsFolder();

        // --------------------------------------------------------
        // Создаём 3D сцену
        // --------------------------------------------------------

        init3D();

        // --------------------------------------------------------
        // Создаём только кнопки
        // --------------------------------------------------------

        createButtons();

        // --------------------------------------------------------
        // Рассчитываем размер
        // --------------------------------------------------------

        recalculateViewport();

        // --------------------------------------------------------
        // Скрываем до открытия
        // --------------------------------------------------------

        setVisible(false);
    }


    // ============================================================
    // ПОИСК МОДЕЛЕЙ
    // ============================================================

   private void scanModelsFolder() {

    models.clear();

    String resourcePath = "Models/Monsters";

    try {

        URL url = getClass()
                .getClassLoader()
                .getResource(resourcePath);

        if (url == null) {

            System.err.println(
                    "[BestiaryWindow] Resource folder not found: "
                            + resourcePath
            );

            return;
        }

        if (!"file".equalsIgnoreCase(url.getProtocol())) {

            System.err.println(
                    "[BestiaryWindow] Monster folder is not a file resource."
            );

            return;
        }

        File directory = new File(url.toURI());

        if (!directory.exists() || !directory.isDirectory()) {

            System.err.println(
                    "[BestiaryWindow] Invalid monster directory: "
                            + directory.getAbsolutePath()
            );

            return;
        }

        scanModelsRecursive(
                directory,
                resourcePath
        );

        models.sort(
                String.CASE_INSENSITIVE_ORDER
        );

    } catch (Exception e) {

        System.err.println(
                "[BestiaryWindow] Error scanning models:"
        );

        e.printStackTrace();
    }

    System.out.println(
            "[BestiaryWindow] Models: "
                    + models.size()
    );
}

private void scanModelsRecursive(
        File directory,
        String resourcePath
) {

    File[] files = directory.listFiles();

    if (files == null) {
        return;
    }

    Arrays.sort(
            files,
            Comparator.comparing(
                    File::getName,
                    String.CASE_INSENSITIVE_ORDER
            )
    );

    for (File file : files) {

        if (file.isDirectory()) {

            scanModelsRecursive(
                    file,
                    resourcePath
                            + "/"
                            + file.getName()
            );

        } else if (
                file.isFile()
                        && file.getName()
                                .toLowerCase()
                                .endsWith(".gltf")
        ) {

            String modelPath =
                    resourcePath
                            + "/"
                            + file.getName();

            models.add(modelPath);

            System.out.println(
                    "[BestiaryWindow] Found model: "
                            + modelPath
            );
        }
    }
}


    // ============================================================
    // INIT 3D
    // ============================================================

    private void init3D() {

        // --------------------------------------------------------
        // ROOT
        // --------------------------------------------------------

        modelRootNode =
                new Node(
                        "BestiaryModelRoot"
                );

        // --------------------------------------------------------
        // CAMERA
        // --------------------------------------------------------

        modelCam =
                new Camera(
                        1600,
                        900
                );

        modelCam.setFrustumPerspective(
                40f,
                16f / 9f,
                0.01f,
                10000f
        );

        // --------------------------------------------------------
        // VIEWPORT
        // --------------------------------------------------------

        RenderManager renderManager =
                app.getRenderManager();

        modelViewPort =
                renderManager.createPostView(
                        "BestiaryModelView",
                        modelCam
                );

        /*
         * Никакого серого background.
         *
         * Color buffer НЕ очищаем.
         *
         * Поэтому GUI под ним остаётся видимым.
         */
        modelViewPort.setClearFlags(
                false,
                true,
                true
        );

        modelViewPort.setEnabled(
                false
        );

        modelViewPort.attachScene(
                modelRootNode
        );

        // --------------------------------------------------------
        // LIGHT
        // --------------------------------------------------------

        setupLighting();

        // --------------------------------------------------------
        // APP STATE
        // --------------------------------------------------------

        appState =
                new BestiaryAppState(
                        modelRootNode
                );

        app.getStateManager().attach(
                appState
        );
    }


    // ============================================================
    // LIGHT
    // ============================================================

    private void setupLighting() {

        ambientLight =
                new AmbientLight();

        ambientLight.setColor(
                new ColorRGBA(
                        0.85f,
                        0.85f,
                        0.85f,
                        1f
                )
        );

        modelRootNode.addLight(
                ambientLight
        );


        directionalLight =
                new DirectionalLight();

        directionalLight.setColor(
                ColorRGBA.White
        );

        directionalLight.setDirection(
                new Vector3f(
                        -1f,
                        -2f,
                        -1f
                ).normalizeLocal()
        );

        modelRootNode.addLight(
                directionalLight
        );
    }


    // ============================================================
    // КНОПКИ
    // ============================================================

    private void createButtons() {

        // ========================================================
        // LEFT
        // ========================================================

        leftButton =
                new Button("<");

        leftButton.setPreferredSize(
                new Vector3f(
                        80f,
                        60f,
                        0f
                )
        );

        leftButton.addClickCommands(
                source -> moveModel(-1)
        );

        guiNode.attachChild(
                leftButton
        );


        // ========================================================
        // RIGHT
        // ========================================================

        rightButton =
                new Button(">");

        rightButton.setPreferredSize(
                new Vector3f(
                        80f,
                        60f,
                        0f
                )
        );

        rightButton.addClickCommands(
                source -> moveModel(1)
        );

        guiNode.attachChild(
                rightButton
        );


        // ========================================================
        // CLOSE
        // ========================================================

        closeButton =
                new Button("X");

        closeButton.setPreferredSize(
                new Vector3f(
                        55f,
                        55f,
                        0f
                )
        );

        closeButton.addClickCommands(
                source -> hideView()
        );

        guiNode.attachChild(
                closeButton
        );
    }


    // ============================================================
    // РАСЧЁТ VIEWPORT
    // ============================================================

    public void recalculateViewport() {

        int screenWidth =
                Math.max(
                        1,
                        app.getCamera().getWidth()
                );

        int screenHeight =
                Math.max(
                        1,
                        app.getCamera().getHeight()
                );


        /*
         * --------------------------------------------------------
         * Считаем размер viewport.
         * --------------------------------------------------------
         *
         * Он занимает практически весь экран.
         */

        viewportWidth =
                screenWidth
                        - SIDE_MARGIN * 2f;

        viewportHeight =
                screenHeight
                        - TOP_MARGIN
                        - BOTTOM_MARGIN;


        viewportWidth =
                Math.max(
                        400f,
                        viewportWidth
                );

        viewportHeight =
                Math.max(
                        300f,
                        viewportHeight
                );


        /*
         * --------------------------------------------------------
         * ЦЕНТР ЭКРАНА
         * --------------------------------------------------------
         */

        viewportX =
                (screenWidth
                        - viewportWidth)
                        / 2f;

        viewportY =
                (screenHeight
                        - viewportHeight)
                        / 2f;


        /*
         * --------------------------------------------------------
         * CAMERA VIEWPORT
         * --------------------------------------------------------
         */

        float left =
                viewportX
                        / screenWidth;

        float right =
                (viewportX + viewportWidth)
                        / screenWidth;

        float bottom =
                viewportY
                        / screenHeight;

        float top =
                (viewportY + viewportHeight)
                        / screenHeight;


        left =
                FastMath.clamp(
                        left,
                        0f,
                        1f
                );

        right =
                FastMath.clamp(
                        right,
                        0f,
                        1f
                );

        bottom =
                FastMath.clamp(
                        bottom,
                        0f,
                        1f
                );

        top =
                FastMath.clamp(
                        top,
                        0f,
                        1f
                );


        modelCam.setViewPort(
                left,
                right,
                bottom,
                top
        );


        modelCam.resize(
                Math.max(
                        1,
                        (int) viewportWidth
                ),
                Math.max(
                        1,
                        (int) viewportHeight
                ),
                true
        );


        modelCam.setFrustumPerspective(
                40f,
                viewportWidth
                        / viewportHeight,
                0.01f,
                10000f
        );


        /*
         * --------------------------------------------------------
         * КНОПКИ
         * --------------------------------------------------------
         */

        float centerY =
                screenHeight / 2f;


        leftButton.setLocalTranslation(
                viewportX - 70f,
                centerY - 30f,
                BUTTON_Z
        );


        rightButton.setLocalTranslation(
                viewportX
                        + viewportWidth
                        - 10f,
                centerY - 30f,
                BUTTON_Z
        );


        closeButton.setLocalTranslation(
                viewportX
                        + viewportWidth
                        - 55f,
                viewportY
                        + viewportHeight
                        - 60f,
                BUTTON_Z
        );


        /*
         * Очень важно:
         *
         * Кнопки заново помещаем последними.
         *
         * Они будут выше других GUI элементов.
         */

        bringButtonsToFront();


        System.out.println(
                "[BestiaryWindow] ================================="
        );

        System.out.println(
                "[BestiaryWindow] Screen: "
                        + screenWidth
                        + " x "
                        + screenHeight
        );

        System.out.println(
                "[BestiaryWindow] Viewport: "
                        + viewportWidth
                        + " x "
                        + viewportHeight
        );

        System.out.println(
                "[BestiaryWindow] Position: "
                        + viewportX
                        + ", "
                        + viewportY
        );

        System.out.println(
                "[BestiaryWindow] ================================="
        );
    }


    // ============================================================
    // КНОПКИ ПОВЕРХ ВСЕГО
    // ============================================================

    private void bringButtonsToFront() {

        if (leftButton != null) {

            leftButton.removeFromParent();

            guiNode.attachChild(
                    leftButton
            );
        }


        if (rightButton != null) {

            rightButton.removeFromParent();

            guiNode.attachChild(
                    rightButton
            );
        }


        if (closeButton != null) {

            closeButton.removeFromParent();

            guiNode.attachChild(
                    closeButton
            );
        }
    }


    // ============================================================
    // SHOW
    // ============================================================

    public void showView() {

        recalculateViewport();

        setVisible(true);

        if (modelViewPort != null) {

            modelViewPort.setEnabled(
                    true
            );
        }


        bringButtonsToFront();


        if (currentModel == null &&
                !models.isEmpty()) {

            showModel(
                    currentIndex
            );
        }
    }


    // ============================================================
    // HIDE
    // ============================================================

    public void hideView() {

        if (modelViewPort != null) {

            modelViewPort.setEnabled(
                    false
            );
        }

        setVisible(false);
    }


    // ============================================================
    // VISIBLE
    // ============================================================

    private void setVisible(
            boolean visible
    ) {

        Node.CullHint hint =
                visible
                        ? Node.CullHint.Never
                        : Node.CullHint.Always;


        if (leftButton != null) {

            leftButton.setCullHint(
                    hint
            );
        }

        if (rightButton != null) {

            rightButton.setCullHint(
                    hint
            );
        }

        if (closeButton != null) {

            closeButton.setCullHint(
                    hint
            );
        }
    }


    // ============================================================
    // SHOW MODEL
    // ============================================================

    public void showModel(
            int index
    ) {

        if (models.isEmpty()) {

            return;
        }

        if (index < 0 ||
                index >= models.size()) {

            return;
        }

        currentIndex =
                index;

        String modelPath =
                models.get(index);


        app.enqueue(() -> {

            try {

                // ------------------------------------------------
                // УДАЛЯЕМ СТАРУЮ
                // ------------------------------------------------

                if (currentModel != null) {

                    modelRootNode.detachChild(
                            currentModel
                    );

                    currentModel = null;
                }

                currentAnimComposer = null;


                // ------------------------------------------------
                // LOAD
                // ------------------------------------------------

                System.out.println(
                        "[BestiaryWindow] Loading: "
                                + modelPath
                );

                Spatial model =
                        assetManager.loadModel(
                                modelPath
                        );


                if (model == null) {

                    System.err.println(
                            "[BestiaryWindow] Model is NULL."
                    );

                    return;
                }


                currentModel =
                        model;


                modelRootNode.attachChild(
                        currentModel
                );


                // ------------------------------------------------
                // UPDATE
                // ------------------------------------------------

                modelRootNode.updateLogicalState(
                        0f
                );

                modelRootNode.updateGeometricState();


                // ------------------------------------------------
                // ANIMATION
                // ------------------------------------------------

                currentAnimComposer =
                        findAnimComposer(
                                currentModel
                        );


                if (currentAnimComposer != null) {

                    System.out.println(
                            "[BestiaryWindow] AnimComposer found."
                    );


                    try {

                        currentAnimComposer
                                .setCurrentAction(
                                        "Attack"
                                );

                        System.out.println(
                                "[BestiaryWindow] Attack started."
                        );

                    } catch (Exception e) {

                        System.out.println(
                                "[BestiaryWindow] Attack not found. Trying Idle."
                        );

                        try {

                            currentAnimComposer
                                    .setCurrentAction(
                                            "Idle"
                                    );

                        } catch (Exception ignored) {
                        }
                    }
                }


                // ------------------------------------------------
                // CAMERA
                // ------------------------------------------------

                fitCameraToModel(
                        currentModel
                );


                // ------------------------------------------------
                // VIEWPORT
                // ------------------------------------------------

                recalculateViewport();


                // ------------------------------------------------
                // SHOW
                // ------------------------------------------------

                modelViewPort.setEnabled(
                        true
                );

                setVisible(true);

                bringButtonsToFront();


                System.out.println(
                        "[BestiaryWindow] Model displayed."
                );


            } catch (Exception e) {

                System.err.println(
                        "[BestiaryWindow] FAILED:"
                );

                e.printStackTrace();
            }
        });
    }


    // ============================================================
    // ПЕРЕКЛЮЧЕНИЕ
    // ============================================================

    private void moveModel(
            int direction
    ) {

        if (models.isEmpty()) {

            return;
        }


        currentIndex =
                (
                        currentIndex
                                + direction
                                + models.size()
                )
                        % models.size();


        showModel(
                currentIndex
        );
    }


    // ============================================================
    // КАМЕРА
    // ============================================================

private void fitCameraToModel(Spatial model) {

    if (model == null) {
        return;
    }

    model.updateLogicalState(0f);
    model.updateGeometricState();

    // ============================================================
    // ОДИНАКОВАЯ КАМЕРА ДЛЯ ВСЕХ МОДЕЛЕЙ
    // ============================================================

    float aspect =
            viewportWidth / viewportHeight;

    float fov =
            40f;

    float cameraDistance =
            8f;

    // ============================================================
    // ЦЕНТР СЦЕНЫ
    // ============================================================

    Vector3f target =
            new Vector3f(
                    0f,
                    1.5f,
                    0f
            );

    // ============================================================
    // CAMERA
    // ============================================================

    Vector3f cameraPosition =
            new Vector3f(
                    0f,
                    target.y,
                    cameraDistance
            );

    modelCam.setLocation(
            cameraPosition
    );

    modelCam.lookAt(
            target,
            Vector3f.UNIT_Y
    );

    // ============================================================
    // FRUSTUM
    // ============================================================

    modelCam.setFrustumPerspective(
            fov,
            aspect,
            0.01f,
            1000f
    );

    System.out.println(
            "[BestiaryWindow] Camera fixed."
    );

    System.out.println(
            "[BestiaryWindow] Position = "
                    + cameraPosition
    );

    System.out.println(
            "[BestiaryWindow] Target = "
                    + target
    );
}    // ============================================================
    // FIND ANIM COMPOSER
    // ============================================================

    private AnimComposer findAnimComposer(
            Spatial spatial
    ) {

        if (spatial == null) {

            return null;
        }


        AnimComposer composer =
                spatial.getControl(
                        AnimComposer.class
                );


        if (composer != null) {

            return composer;
        }


        if (spatial instanceof Node node) {

            for (Spatial child :
                    node.getChildren()) {

                AnimComposer found =
                        findAnimComposer(
                                child
                        );

                if (found != null) {

                    return found;
                }
            }
        }


        return null;
    }


    // ============================================================
    // RESIZE
    // ============================================================

    public void onResize() {

        recalculateViewport();

        if (currentModel != null) {

            fitCameraToModel(
                    currentModel
            );
        }
    }


    // ============================================================
    // CLEANUP
    // ============================================================

    public void cleanup() {

        hideView();


        if (leftButton != null) {

            leftButton.removeFromParent();
            leftButton = null;
        }


        if (rightButton != null) {

            rightButton.removeFromParent();
            rightButton = null;
        }


        if (closeButton != null) {

            closeButton.removeFromParent();
            closeButton = null;
        }


        if (currentModel != null &&
                modelRootNode != null) {

            modelRootNode.detachChild(
                    currentModel
            );

            currentModel = null;
        }


        currentAnimComposer = null;


        if (modelRootNode != null) {

            if (ambientLight != null) {

                modelRootNode.removeLight(
                        ambientLight
                );
            }

            if (directionalLight != null) {

                modelRootNode.removeLight(
                        directionalLight
                );
            }
        }


        if (appState != null) {

            app.getStateManager().detach(
                    appState
            );

            appState = null;
        }


        modelRootNode = null;
        modelCam = null;
        modelViewPort = null;
        ambientLight = null;
        directionalLight = null;
    }


    // ============================================================
    // APP STATE
    // ============================================================

    private static class BestiaryAppState
            extends com.jme3.app.state.BaseAppState {

        private final Node modelRootNode;


        BestiaryAppState(
                Node modelRootNode
        ) {

            this.modelRootNode =
                    modelRootNode;
        }


        @Override
        protected void initialize(
                Application app
        ) {
        }


        @Override
        public void update(
                float tpf
        ) {

            if (modelRootNode != null) {

                modelRootNode.updateLogicalState(
                        tpf
                );
            }
        }


        @Override
        public void render(
                RenderManager renderManager
        ) {

            if (modelRootNode != null) {

                modelRootNode.updateGeometricState();
            }
        }


        @Override
        protected void cleanup(
                Application app
        ) {
        }


        @Override
        protected void onEnable() {
        }


        @Override
        protected void onDisable() {
        }
    }
}