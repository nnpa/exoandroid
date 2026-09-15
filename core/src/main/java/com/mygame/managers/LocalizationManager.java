package com.mygame.managers;

import com.jme3.asset.AssetManager;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class LocalizationManager {
    private static LocalizationManager instance;

    /*
     * Язык по умолчанию — русский.
     * Раньше было "en": при первом запуске (до того, как
     * SettingsManager сохранит выбор игрока) подгружался
     * английский. Теперь по умолчанию — "ru".
     */
    private String currentLanguage = "ru";

    private Map<String, String> strings = new HashMap<>();

    private LocalizationManager() {}

    public static LocalizationManager getInstance() {
        if (instance == null) instance = new LocalizationManager();
        return instance;
    }

    public void init(AssetManager assetManager) {
        // assetManager не используется, можно убрать параметр, или сохранить для других целей
        loadLanguage(currentLanguage);
    }

    public void loadLanguage(String lang) {
        currentLanguage = lang;
        strings.clear();
        String path = "Interface/Locale/" + lang + ".properties";
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                System.err.println("Resource not found: " + path);
                if (!lang.equals("en")) {
                    loadLanguage("en");
                }
                return;
            }
            Properties props = new Properties();
            props.load(in);
            for (String key : props.stringPropertyNames()) {
                strings.put(key, props.getProperty(key));
            }
        } catch (Exception e) {
            System.err.println("Failed to load locale: " + path);
            if (!lang.equals("en")) {
                loadLanguage("en");
            }
        }
    }

    public String get(String key) {
        return strings.getOrDefault(key, "???" + key + "???");
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }
}