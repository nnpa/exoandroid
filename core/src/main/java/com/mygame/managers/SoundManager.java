package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.scene.Node;

import java.util.HashMap;
import java.util.Map;

/**
 * Менеджер звуков и музыки для игры "Экзорцист".
 *
 * Короткие звуковые эффекты загружаются в Buffer-режиме.
 * Фоновая музыка загружается в Stream-режиме.
 */
public class SoundManager {

    private static SimpleApplication app;
    private static Node rootNode;

    private static Map<String, AudioNode> soundEffects = new HashMap<>();

    private static AudioNode currentMusic = null;
    private static String currentMusicKey = null;

    private static float baseMusicVolume = 0.2f;
    private static float baseSfxVolume = 0.3f;
    private static float masterVolume = 1.0f;

    public static AudioNode getSoundNode(String key) {
        return soundEffects.get(key);
    }

    // ===================== КОНСТАНТЫ =====================

    // UI
    public static final String SOUND_CLICK = "ui_button_click";
    public static final String SOUND_LEVEL_UP = "ui_level_up";
    public static final String SOUND_PICKUP = "ui_pickup_item";
    public static final String SOUND_UNEQUIP = "ui_item_unequip";
    public static final String SOUND_WINDOW_TALENTS = "ui_window_open_talents_inventory";
    public static final String SOUND_WINDOW_TRADER = "ui_window_open_trader_auction";
    public static final String SOUND_WINDOW_CLOSE = "ui_window_close";

    // Skills
    public static final String SOUND_HEAL = "skill_heal";
    public static final String SOUND_SHIELD_BASH = "skill_shield_bash";
    public static final String SOUND_WHIRLWIND = "skill_whirlwind";
    public static final String SOUND_KICK = "skill_kick";

    // Combat
    public static final String SOUND_ATTACK_PLAYER = "combat_attack_player";
    public static final String SOUND_PLAYER_DIE = "combat_player_die";
    public static final String SOUND_MONSTER_DIE = "combat_monster_die";
    public static final String SOUND_FOOTSTEP = "ambient_footstep_player";

    // Music
    public static final String MUSIC_CITY = "music_city_loop";
    public static final String MUSIC_DUNGEON = "music_dungeon_loop";
    public static final String MUSIC_BOSS = "music_boss_battle";

    // ===================== ИНИЦИАЛИЗАЦИЯ =====================

    public static void initialize(SimpleApplication application) {
        app = application;
        rootNode = app.getRootNode();

        System.out.println("[SoundManager] Initializing...");
        loadAllSounds();
        System.out.println("[SoundManager] Ready. Loaded " + soundEffects.size() + " sounds.");
    }

    private static void loadAllSounds() {
        // UI
        loadSound(SOUND_CLICK, "sounds/ui/ui_button_click.ogg");
        loadSound(SOUND_LEVEL_UP, "sounds/ui/ui_level_up.ogg");
        loadSound(SOUND_PICKUP, "sounds/ui/ui_pickup_item.ogg");
        loadSound(SOUND_UNEQUIP, "sounds/ui/ui_item_unequip.ogg");
        loadSound(SOUND_WINDOW_TALENTS, "sounds/ui/ui_window_open_talents_inventory.ogg");
        loadSound(SOUND_WINDOW_TRADER, "sounds/ui/ui_window_open_trader_auction.ogg");
        loadSound(SOUND_WINDOW_CLOSE, "sounds/ui/ui_window_close.ogg");

        // Skills
        loadSound(SOUND_HEAL, "sounds/skills/skill_heal.ogg");
        loadSound(SOUND_SHIELD_BASH, "sounds/skills/skill_shield_bash.ogg");
        loadSound(SOUND_WHIRLWIND, "sounds/skills/skill_whirlwind.ogg");
        loadSound(SOUND_KICK, "sounds/skills/skill_kick.ogg");

        // Combat
        loadSound(SOUND_ATTACK_PLAYER, "sounds/combat/combat_attack_player.ogg");
        loadSound(SOUND_PLAYER_DIE, "sounds/combat/combat_player_die.ogg");
        loadSound(SOUND_MONSTER_DIE, "sounds/combat/combat_monster_die.ogg");

        // Ambient
        loadSound(SOUND_FOOTSTEP, "sounds/ambient/ambient_footstep_player.ogg");

        // Music
        loadMusic(MUSIC_CITY, "sounds/music/music_city_loop.ogg");
        loadMusic(MUSIC_DUNGEON, "sounds/music/music_dungeon_loop.ogg");
        loadMusic(MUSIC_BOSS, "sounds/music/music_boss_battle.ogg");
    }

    // ===================== ЗАГРУЗКА ЗВУКОВ =====================

    /**
     * Загружает короткий звуковой эффект в Buffer-режиме.
     */
    private static void loadSound(String key, String path) {
        try {
            AudioNode node = new AudioNode(app.getAssetManager(), path);
            node.setPositional(false);
            node.setLooping(false);
            node.setVolume(baseSfxVolume * masterVolume);
            rootNode.attachChild(node);
            soundEffects.put(key, node);

            System.out.println("[SoundManager] Loaded sound: " + key + " (BUFFER)");
        } catch (Exception e) {
            System.err.println("[SoundManager] Failed to load sound: " + key + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Загружает фоновую музыку в Stream-режиме.
     */
    private static void loadMusic(String key, String path) {
        try {
            AudioNode node = new AudioNode(app.getAssetManager(), path, AudioData.DataType.Stream);
            node.setPositional(false);
            node.setLooping(true);
            node.setVolume(baseMusicVolume * masterVolume);
            rootNode.attachChild(node);
            soundEffects.put(key, node);

            System.out.println("[SoundManager] Loaded music: " + key + " (STREAM)");
        } catch (Exception e) {
            System.err.println("[SoundManager] Failed to load music: " + key + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===================== ВОСПРОИЗВЕДЕНИЕ =====================

    public static void playSound(String key) {
        AudioNode node = soundEffects.get(key);

        if (node == null) {
            System.err.println("[SoundManager] Sound not found: " + key);
            return;
        }

        node.playInstance();
    }

    public static void playSoundDelayed(String key, float delaySec) {
        if (delaySec <= 0) {
            playSound(key);
            return;
        }

        new Thread(() -> {
            try {
                Thread.sleep((long) (delaySec * 1000));
                playSound(key);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    // ===================== УПРАВЛЕНИЕ МУЗЫКОЙ =====================

    public static void playMusic(String key) {
        if (currentMusic != null && currentMusicKey != null && currentMusicKey.equals(key)) {
            return;
        }

        stopMusic();

        AudioNode node = soundEffects.get(key);

        if (node == null) {
            System.err.println("[SoundManager] Music not found: " + key);
            return;
        }

        currentMusic = node;
        currentMusic.setVolume(baseMusicVolume * masterVolume);
        currentMusic.play();
        currentMusicKey = key;

        System.out.println("[SoundManager] Playing music: " + key);
    }

    public static void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic = null;
            currentMusicKey = null;
        }
    }

    // ===================== УПРАВЛЕНИЕ ГРОМКОСТЬЮ =====================

    public static void setMusicVolume(float volume) {
        baseMusicVolume = Math.max(0, Math.min(1, volume));
        applyMasterVolumeToMusic();
    }

    public static void setSfxVolume(float volume) {
        baseSfxVolume = Math.max(0, Math.min(1, volume));
        applyMasterVolumeToSfx();
    }

    public static void setMasterVolume(float volume) {
        masterVolume = Math.max(0, Math.min(1, volume));
        applyMasterVolumeToSfx();
        applyMasterVolumeToMusic();
    }

    private static void applyMasterVolumeToSfx() {
        for (Map.Entry<String, AudioNode> entry : soundEffects.entrySet()) {
            AudioNode node = entry.getValue();

            if (node != currentMusic) {
                node.setVolume(baseSfxVolume * masterVolume);
            }
        }
    }

    private static void applyMasterVolumeToMusic() {
        if (currentMusic != null) {
            currentMusic.setVolume(baseMusicVolume * masterVolume);
        }

        for (Map.Entry<String, AudioNode> entry : soundEffects.entrySet()) {
            AudioNode node = entry.getValue();

            if (node.isLooping()) {
                node.setVolume(baseMusicVolume * masterVolume);
            }
        }
    }

    public static float getMusicVolume() {
        return baseMusicVolume;
    }

    public static float getSfxVolume() {
        return baseSfxVolume;
    }

    public static float getMasterVolume() {
        return masterVolume;
    }

    // ===================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =====================

    public static boolean isSoundLoaded(String key) {
        return soundEffects.containsKey(key);
    }

    public static void cleanup() {
        stopMusic();

        for (AudioNode node : soundEffects.values()) {
            node.stop();
        }

        soundEffects.clear();

        System.out.println("[SoundManager] Cleanup complete.");
    }
}
