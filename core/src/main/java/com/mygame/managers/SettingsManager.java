package com.mygame.managers;

import java.io.*;
import java.util.Properties;

public class SettingsManager {
    private static final String SETTINGS_FILE = "settings.properties";
    
    private int screenWidth = 1280;
    private int screenHeight = 720;
    private float soundVolume = 0.5f; // 0..1
    private String language = "en";   // "en" или "ru"
    private boolean hideControlsHelp = false;
    
    private static SettingsManager instance;
    
    private SettingsManager() {
        load();
    }
    
    public static SettingsManager getInstance() {
        if (instance == null) instance = new SettingsManager();
        return instance;
    }
    
public void load() {
    Properties props = new Properties();
    try (InputStream in = new FileInputStream(SETTINGS_FILE)) {
        props.load(in);
        screenWidth = Integer.parseInt(props.getProperty("screenWidth", "1280"));
        screenHeight = Integer.parseInt(props.getProperty("screenHeight", "720"));
        soundVolume = Float.parseFloat(props.getProperty("soundVolume", "0.5"));
        language = props.getProperty("language", "en"); // по умолчанию английский
        hideControlsHelp = Boolean.parseBoolean(props.getProperty("hideControlsHelp", "false"));
    } catch (IOException e) {
        // Файла нет – сохраняем дефолтные настройки
        save();
    }
}
    
    public void save() {
        Properties props = new Properties();
        props.setProperty("screenWidth", String.valueOf(screenWidth));
        props.setProperty("screenHeight", String.valueOf(screenHeight));
        props.setProperty("soundVolume", String.valueOf(soundVolume));
        props.setProperty("language", language);
        props.setProperty("hideControlsHelp", String.valueOf(hideControlsHelp));
        try (OutputStream out = new FileOutputStream(SETTINGS_FILE)) {
            props.store(out, "Game Settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Геттеры и сеттеры с автоматическим сохранением
    public int getScreenWidth() { return screenWidth; }
    public void setScreenWidth(int w) { screenWidth = w; save(); }
    public int getScreenHeight() { return screenHeight; }
    public void setScreenHeight(int h) { screenHeight = h; save(); }
    public float getSoundVolume() { return soundVolume; }
    public void setSoundVolume(float v) { 
        soundVolume = v; save(); 
        
    }
    public String getLanguage() { return language; }
    public void setLanguage(String lang) { language = lang; save(); }

    public boolean isHideControlsHelp() { return hideControlsHelp; }
    public void setHideControlsHelp(boolean hide) { hideControlsHelp = hide; save(); }
}