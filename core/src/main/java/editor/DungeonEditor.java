package editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.mygame.Main;
import com.mygame.dungeons.Dungeon;
import com.mygame.monsters.Angel;
import com.mygame.monsters.Barbar;
import com.mygame.monsters.Demon;
import com.mygame.monsters.DragonBoss;
import com.mygame.monsters.FinalBoss;
import com.mygame.monsters.Goblin;
import com.mygame.monsters.Head;
import com.mygame.monsters.Ice;
import com.mygame.monsters.Imp;
import com.mygame.monsters.Inferno;
import com.mygame.monsters.Luk;
import com.mygame.monsters.Monster;
import com.mygame.monsters.Osa;
import com.mygame.monsters.Raptor;
import com.mygame.monsters.RobotBoss;
import com.mygame.monsters.Root;
import com.mygame.monsters.Scorpion;
import com.mygame.monsters.Sgolem;
import com.mygame.monsters.SkeletMag;
import com.mygame.monsters.SkeletonWarrior;
import com.mygame.monsters.Snake;
import com.mygame.monsters.SpiderBoss;
import com.mygame.monsters.Wolf;
import com.mygame.monsters.WormBoss;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.Insets3f;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DungeonEditor {
    private Main app;
    private Node editorNode;
    private Container mainContainer;
    private List<Dungeon.MonsterSpawn> spawns;
    private String currentDungeonId;
    private Runnable onSaveCallback;
    private Container rightPanel;

    public DungeonEditor(Main app) {
        this.app = app;
        editorNode = new Node("EditorNode");
        editorNode.setCullHint(Spatial.CullHint.Always);
        app.getGuiNode().attachChild(editorNode);
    }

    public void open(String dungeonId, List<Dungeon.MonsterSpawn> existingSpawns, Runnable onSave) {
        this.currentDungeonId = dungeonId;
        this.spawns = new ArrayList<>(existingSpawns);
        this.onSaveCallback = onSave;
        buildUI();
        editorNode.setCullHint(Spatial.CullHint.Dynamic);
        System.out.println("[DungeonEditor] Opened: " + dungeonId + ", spawns: " + spawns.size());
    }

    public void close() {
        editorNode.setCullHint(Spatial.CullHint.Always);
        editorNode.detachAllChildren();
        System.out.println("[DungeonEditor] Closed");
    }

    private void buildUI() {
        editorNode.detachAllChildren();

    float camW = app.getCamera().getWidth();
    float camH = app.getCamera().getHeight();

    float winW = Math.min(600, camW * 0.8f);
    float winH = Math.min(400, camH * 0.8f);
    if (winW < 300) winW = 300;
    if (winH < 200) winH = 200;

    mainContainer = new Container();
    mainContainer.setPreferredSize(new Vector3f(winW, winH, 0));
    SpringGridLayout mainLayout = new SpringGridLayout(Axis.Y, Axis.X);
    mainContainer.setLayout(mainLayout);

    // === ИСПРАВЛЕННАЯ ПОЗИЦИЯ (СВЕРХУ) ===
    float x = (camW - winW) / 2;
    float y = camH - winH - 20f; // отступ 20 пикселей сверху
    if (x < 0) x = 10;
    if (y < 0) y = 10;
    mainContainer.setLocalTranslation(x, y, 0);

        // Верхняя строка: левая + правая панели
        Container topRow = new Container();
        topRow.setLayout(new SpringGridLayout(Axis.X, Axis.Y));
        topRow.setPreferredSize(new Vector3f(winW, winH - 50, 0)); // отводим 50 для нижней панели

        // Левая панель с кнопками классов
        Container leftPanel = new Container();
        leftPanel.setLayout(new SpringGridLayout(Axis.Y, Axis.X));
        leftPanel.setPreferredSize(new Vector3f(winW * 0.35f, winH - 50, 0));

        for (Class<? extends Monster> cls : getMonsterClasses()) {
            Button btn = new Button(cls.getSimpleName());
            btn.setFontSize(14f);
            btn.addClickCommands((source) -> addMonster(cls));
            leftPanel.addChild(btn);
        }

        // Правая панель со списком спавнов
        rightPanel = new Container();
        rightPanel.setLayout(new SpringGridLayout(Axis.Y, Axis.X));
        rightPanel.setPreferredSize(new Vector3f(winW * 0.55f, winH - 50, 0));
        updateSpawnList();

        topRow.addChild(leftPanel);
        topRow.addChild(rightPanel);

        // Нижняя панель с кнопками
        Container bottomRow = new Container();
        bottomRow.setLayout(new SpringGridLayout(Axis.X, Axis.Y));
        bottomRow.setPreferredSize(new Vector3f(winW, 50, 0));

        Button saveBtn = new Button("Save");
        saveBtn.setFontSize(14f);
        saveBtn.addClickCommands((source) -> saveToFile());

        Button closeBtn = new Button("Close");
        closeBtn.setFontSize(14f);
        closeBtn.addClickCommands((source) -> close());

        bottomRow.addChild(saveBtn);
        bottomRow.addChild(closeBtn);

        // Собираем всё в mainContainer
        mainContainer.addChild(topRow);
        mainContainer.addChild(bottomRow);

        // Делаем фон для всего окна (прозрачный или полупрозрачный)
        mainContainer.setBackground(new QuadBackgroundComponent(new com.jme3.math.ColorRGBA(0.1f, 0.1f, 0.2f, 0.9f)));

        editorNode.attachChild(mainContainer);
    }

    private void updateSpawnList() {
        if (rightPanel == null) return;
        rightPanel.detachAllChildren();

        for (int i = 0; i < spawns.size(); i++) {
            Dungeon.MonsterSpawn spawn = spawns.get(i);
            String shortName = spawn.className.substring(spawn.className.lastIndexOf('.') + 1);
            String text = shortName + " (" + spawn.x + ", " + spawn.y + ", " + spawn.z + ")";

            Container row = new Container();
            row.setLayout(new SpringGridLayout(Axis.X, Axis.Y));
            row.setPreferredSize(new Vector3f(rightPanel.getPreferredSize().x, 26, 0));

            Label lbl = new Label(text);
            lbl.setFontSize(12f);
            lbl.setPreferredSize(new Vector3f(180, 26, 0));

            Button delBtn = new Button("X");
            delBtn.setFontSize(12f);
            delBtn.setPreferredSize(new Vector3f(30, 26, 0));
            final int index = i;
            delBtn.addClickCommands((source) -> {
                spawns.remove(index);
                updateSpawnList();
            });

            row.addChild(lbl);
            row.addChild(delBtn);
            rightPanel.addChild(row);
        }
    }

private void addMonster(Class<? extends Monster> cls) {
    Vector3f playerPos = app.getPlayerManager().getPosition();

    if (playerPos == null) {
        System.err.println("[DungeonEditor] Не удалось получить позицию персонажа.");
        return;
    }

    Dungeon.MonsterSpawn spawn = new Dungeon.MonsterSpawn(
            cls.getName(),
            playerPos.x,
            playerPos.y,
            playerPos.z,
            1,
            100,
            10,
            null,
            false,
            false,
            false
    );

    spawns.add(spawn);
    updateSpawnList();

    System.out.println(
            "[DungeonEditor] Added " + cls.getSimpleName()
            + " at (" + playerPos.x
            + ", " + playerPos.y
            + ", " + playerPos.z + ")"
    );
}

    private void saveToFile() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(spawns);
        String savePath = System.getProperty("user.home") + "/Exorcist/dungeons/" + currentDungeonId + ".json";
        File file = new File(savePath);
        file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(json);
            System.out.println("[DungeonEditor] Saved to " + savePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (onSaveCallback != null) onSaveCallback.run();
        close();
    }

    private List<Class<? extends Monster>> getMonsterClasses() {
        List<Class<? extends Monster>> classes = new ArrayList<>();
        classes.add(Head.class);
        classes.add(Demon.class);

        classes.add(SkeletonWarrior.class);

        
        return classes;
    }
}