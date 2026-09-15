package com.mygame.managers;

import com.jme3.app.SimpleApplication;
import com.jme3.system.JmeSystem;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class NetworkManager {

    public String getServerUrl() {
        return serverUrl;
    }

    private SimpleApplication app;
    private String serverUrl = "https://rpgexorcist.site/";
    private String authToken = null;
    private boolean isConnected = false;
    private static final String TOKEN_FILE_NAME = ".exorcist_token.txt";
    private static final String DEVICE_ID_FILE_NAME = ".exorcist_device_id.txt";

    public NetworkManager(SimpleApplication app) {
        this.app = app;
    }

    public void initialize() {
        System.out.println("[NetworkManager] Инициализация...");
        loadAuthToken();
    }

    // ================================================================
    //   JSON → Map/List (Android-safe)
    //
    //   На Android в системной библиотеке (core-libart.jar) класс
    //   org.json.JSONObject НЕ имеет метода toMap(). Метод toMap()
    //   есть только в desktop-сборке org.json:json, которую Android
    //   игнорирует из-за приоритета bootclasspath. Поэтому при
    //   вызове toMap() летит NoSuchMethodError — конвертируем вручную.
    // ================================================================

    private static Map<String, Object> jsonToMap(JSONObject obj) {
        Map<String, Object> map = new HashMap<>();
        if (obj == null) return map;
        Iterator<String> keys = obj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = obj.opt(key);
            if (value instanceof JSONObject) {
                map.put(key, jsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                map.put(key, jsonArrayToList((JSONArray) value));
            } else if (value == JSONObject.NULL) {
                map.put(key, null);
            } else {
                map.put(key, value);
            }
        }
        return map;
    }

    private static List<Object> jsonArrayToList(JSONArray arr) {
        List<Object> list = new ArrayList<>();
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) {
            Object value = arr.opt(i);
            if (value instanceof JSONObject) {
                list.add(jsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                list.add(jsonArrayToList((JSONArray) value));
            } else if (value == JSON_NULL(value)) {
                list.add(null);
            } else {
                list.add(value);
            }
        }
        return list;
    }

    // Вспомогательный метод, чтобы не сравнивать с JSONObject.NULL в двух местах
    private static Object JSON_NULL(Object candidate) {
        return JSONObject.NULL;
    }

    // ---- Auth ----
    public CompletableFuture<Boolean> register(String email, String login, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("email", email);
                json.put("login", login);
                json.put("password", password);
                String response = sendPostRequest("/auth/register", json.toString(), null);
                System.out.println("[NetworkManager] Register response: " + response);
                JSONObject result = new JSONObject(response);
                return result.optBoolean("success", false);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> login(String login, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("login", login);
                json.put("password", password);
                String response = sendPostRequest("/auth/login", json.toString(), null);
                System.out.println("[NetworkManager] Login response: " + response);
                JSONObject result = new JSONObject(response);
                if (result.optBoolean("success", false)) {
                    authToken = result.optString("token");
                    saveAuthToken(authToken);
                    isConnected = true;
                    return true;
                }
                return false;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> vkPlayLogin(String persId, String vkToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("persId", persId);
                json.put("token", vkToken != null ? vkToken : "");
                String response = sendPostRequest("/auth/vkplay-login", json.toString(), null);
                System.out.println("[NetworkManager] vkPlayLogin response: " + response);
                JSONObject result = new JSONObject(response);
                if (result.optBoolean("success", false)) {
                    authToken = result.optString("token");
                    saveAuthToken(authToken);
                    isConnected = true;
                    return true;
                }
                return false;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> checkToken() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (authToken == null) return false;
                String response = sendPostRequest("/auth/check", "{}", authToken);
                JSONObject result = new JSONObject(response);
                return result.optBoolean("success", false);
            } catch (Exception e) {
                return false;
            }
        });
    }

    // ---- Character ----
    public CompletableFuture<Map<String, Object>> loadCharacterData() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (authToken == null) return null;
                String response = sendGetRequest("/character", authToken);
                System.out.println("[NetworkManager] Character data response length=" + response.length());
                System.out.println("[NetworkManager] Character data response: " + response);
                JSONObject result = new JSONObject(response);
                return parseCharacterResponse(result);
            } catch (Exception e) {
                System.out.println("[NetworkManager] loadCharacterData EXCEPTION: "
                        + e.getClass().getName() + ": " + e.getMessage());
                e.printStackTrace(System.out);
                return null;
            }
        });
    }

    public CompletableFuture<Boolean> saveCharacter(Map<String, Object> characterData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (authToken == null) return false;
                JSONObject json = new JSONObject();
                if (characterData.containsKey("health")) json.put("health", characterData.get("health"));
                if (characterData.containsKey("mana")) json.put("mana", characterData.get("mana"));
                if (characterData.containsKey("maxHealth")) json.put("max_health", characterData.get("maxHealth"));
                if (characterData.containsKey("maxMana")) json.put("max_mana", characterData.get("maxMana"));
                if (characterData.containsKey("gold")) json.put("gold", characterData.get("gold"));
                if (characterData.containsKey("level")) json.put("level", characterData.get("level"));
                if (characterData.containsKey("experience")) json.put("experience", characterData.get("experience"));
                if (characterData.containsKey("currentDungeon")) json.put("current_dungeon", characterData.get("currentDungeon"));
                if (characterData.containsKey("difficulty")) json.put("difficulty", characterData.get("difficulty"));
                if (characterData.containsKey("lastX")) json.put("last_dungeon_position_x", characterData.get("lastX"));
                if (characterData.containsKey("lastY")) json.put("last_dungeon_position_y", characterData.get("lastY"));
                if (characterData.containsKey("lastZ")) json.put("last_dungeon_position_z", characterData.get("lastZ"));

                if (characterData.containsKey("healthPotions")) {
                    json.put("health_potions", characterData.get("healthPotions"));
                }
                if (characterData.containsKey("manaPotions")) {
                    json.put("mana_potions", characterData.get("manaPotions"));
                }

                String response = sendPostRequest("/character/save", json.toString(), authToken);
                JSONObject result = new JSONObject(response);
                return result.optBoolean("success", false);
            } catch (Exception e) {
                e.printStackTrace(System.out);
                return false;
            }
        });
    }

    // ---- Inventory ----
    public CompletableFuture<Map<String, Object>> pickupItem(Map<String, Object> itemData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (authToken == null) return null;
                JSONObject json = new JSONObject();
                json.put("itemData", new JSONObject(itemData));
                String response = sendPostRequest("/inventory/pickup", json.toString(), authToken);
                System.out.println("[NetworkManager] Pickup raw response: " + response);
                JSONObject result = new JSONObject(response);
                if (result.optBoolean("success", false)) {
                    return parseCharacterResponse(result);
                } else {
                    System.err.println("[NetworkManager] Pickup failed. Server message: " + result.optString("message"));
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
                return null;
            }
        });
    }

    public CompletableFuture<Map<String, Object>> equipItem(int slotIndex) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (authToken == null) return null;
                JSONObject json = new JSONObject();
                json.put("slot", slotIndex);
                String response = sendPostRequest("/inventory/equip", json.toString(), authToken);
                System.out.println("[NetworkManager] Equip response: " + response);
                JSONObject result = new JSONObject(response);
                if (result.optBoolean("success", false)) {
                    return parseCharacterResponse(result);
                }
                return null;
            } catch (Exception e) {
                e.printStackTrace(System.out);
                return null;
            }
        });
    }

    private String getSlotName(int slotIndex) {
        switch (slotIndex) {
            case 0: return "helmet";
            case 1: return "chest";
            case 2: return "weapon";
            case 3: return "shield";
            case 4: return "legs";
            case 5: return "boots";
            case 6: return "gloves";
            default: return null;
        }
    }

    public CompletableFuture<Map<String, Object>> unequipItem(String equippedSlot) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (authToken == null) return null;
                if (equippedSlot == null || equippedSlot.isEmpty()) {
                    System.err.println("[NetworkManager] equippedSlot is null or empty");
                    return null;
                }
                JSONObject json = new JSONObject();
                json.put("equipped_slot", equippedSlot);
                String response = sendPostRequest("/inventory/unequip", json.toString(), authToken);
                JSONObject result = new JSONObject(response);
                if (result.optBoolean("success", false)) {
                    return parseCharacterResponse(result);
                }
                return null;
            } catch (Exception e) {
                e.printStackTrace(System.out);
                return null;
            }
        });
    }

    public CompletableFuture<Map<String, Object>> dropItem(int slotIndex) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (authToken == null) return null;
                JSONObject json = new JSONObject();
                json.put("slot", slotIndex);
                String response = sendPostRequest("/inventory/drop", json.toString(), authToken);
                JSONObject result = new JSONObject(response);
                if (result.optBoolean("success", false)) {
                    return parseCharacterResponse(result);
                }
                return null;
            } catch (Exception e) {
                e.printStackTrace(System.out);
                return null;
            }
        });
    }

    public CompletableFuture<Map<String, Object>> sellItem(int slotIndex) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (authToken == null) return null;
                JSONObject json = new JSONObject();
                json.put("slot", slotIndex);
                String response = sendPostRequest("/inventory/sell", json.toString(), authToken);
                System.out.println("[NetworkManager] sellItem response: " + response);
                JSONObject result = new JSONObject(response);
                if (result.optBoolean("success", false)) {
                    return parseCharacterResponse(result);
                }
                return null;
            } catch (Exception e) {
                e.printStackTrace(System.out);
                return null;
            }
        });
    }

    // ================================================================
    //   АУКЦИОН
    // ================================================================

    public CompletableFuture<AuctionLotResponse> getAuctionList(int page, String type, String rarity, int minLevel, int maxLevel) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (authToken == null) return null;
                StringBuilder query = new StringBuilder("/auction/list?page=" + page);
                if (type != null && !type.isEmpty()) query.append("&type=").append(type);
                if (rarity != null && !rarity.isEmpty()) query.append("&rarity=").append(rarity);
                if (minLevel > 0) query.append("&minLevel=").append(minLevel);
                if (maxLevel < 100) query.append("&maxLevel=").append(maxLevel);

                String response = sendGetRequest(query.toString(), authToken);
                System.out.println("[NetworkManager] getAuctionList response: " + response);
                JSONObject result = new JSONObject(response);
                if (result.optBoolean("success", false)) {
                    return new AuctionLotResponse(result);
                } else {
                    System.err.println("[NetworkManager] Server error: " + result.optString("message"));
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
                return null;
            }
        });
    }

public CompletableFuture<Map<String, Object>> createAuctionLot(List<Integer> slotIndices, int price) {
    return CompletableFuture.supplyAsync(() -> {
        try {
            if (authToken == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Not authenticated");
                return error;
            }
            JSONObject json = new JSONObject();

            // ============================================================
            // ИСПРАВЛЕНО: было json.put("slotIndices", slotIndices);
            // На Android у org.json.JSONObject НЕТ перегрузки
            // put(String, Collection) — она есть только в desktop-сборке
            // org.json:json. Собираем JSONArray вручную.
            // ============================================================
            JSONArray slotsArr = new JSONArray();
            if (slotIndices != null) {
                for (Integer s : slotIndices) {
                    if (s != null) slotsArr.put(s.intValue());
                }
            }
            json.put("slotIndices", slotsArr);
            json.put("price", price);

            System.out.println("[NetworkManager] Sending createAuctionLot: " + json);
            String response = sendPostRequest("/auction/create", json.toString(), authToken);
            System.out.println("[NetworkManager] createAuctionLot response: " + response);
            JSONObject result = new JSONObject(response);
            if (result.optBoolean("success", false)) {
                return parseCharacterResponse(result);
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("error", result.optString("message", "Unknown error"));
                return error;
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    });
}
    public CompletableFuture<Map<String, Object>> buyAuctionLot(int lotId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (authToken == null) {
                    System.err.println("[NetworkManager] buyAuctionLot: authToken is null");
                    return null;
                }
                JSONObject json = new JSONObject();
                json.put("lotId", lotId);
                System.out.println("[NetworkManager] Sending buy request: " + json);
                String response = sendPostRequest("/auction/buy", json.toString(), authToken);
                System.out.println("[NetworkManager] buyAuctionLot response: " + response);
                JSONObject result = new JSONObject(response);
                if (result.optBoolean("success", false)) {
                    return parseCharacterResponse(result);
                } else {
                    System.err.println("[NetworkManager] Server error: " + result.optString("message"));
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
                return null;
            }
        });
    }

    public CompletableFuture<List<AuctionLot>> getMyLots() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (authToken == null) return null;
                String response = sendGetRequest("/auction/my", authToken);
                JSONObject result = new JSONObject(response);
                JSONArray items = result.optJSONArray("items");
                List<AuctionLot> myLots = new ArrayList<>();
                if (items != null) {
                    for (int i = 0; i < items.length(); i++) {
                        myLots.add(AuctionLot.fromMap(items.getJSONObject(i)));
                    }
                }
                return myLots;
            } catch (Exception e) {
                e.printStackTrace(System.out);
                return null;
            }
        });
    }

    // ---- Helpers ----
    private String sendPostRequest(String endpoint, String jsonBody, String token) throws Exception {
        URL url = new URL(serverUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Connection", "close");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);
        if (token != null && !token.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();

        InputStream stream = responseCode >= 200 && responseCode < 300
                ? conn.getInputStream()
                : conn.getErrorStream();

        if (stream == null) {
            conn.disconnect();
            return "{\"success\":false,\"error\":\"Empty response, HTTP " + responseCode + "\"}";
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        conn.disconnect();
        return sb.toString();
    }

    private String sendGetRequest(String endpoint, String token) throws Exception {
        URL url = new URL(serverUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Connection", "close");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);
        if (token != null && !token.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }

        int responseCode = conn.getResponseCode();

        InputStream stream = responseCode >= 200 && responseCode < 300
                ? conn.getInputStream()
                : conn.getErrorStream();

        if (stream == null) {
            conn.disconnect();
            return "{\"success\":false,\"error\":\"Empty response, HTTP " + responseCode + "\"}";
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        conn.disconnect();
        return sb.toString();
    }

    private Map<String, Object> parseCharacterResponse(JSONObject response) {
        JSONObject character = response.optJSONObject("character");
        if (character == null) character = response;
        if (character == null) return null;

        Map<String, Object> data = new HashMap<>();

        // ===== Базовые параметры =====
        data.put("id", character.optString("id"));
        data.put("name", character.optString("name"));
        data.put("level", character.optInt("level", 1));
        data.put("experience", character.optInt("experience", 0));
        data.put("health", character.optInt("health", 100));
        data.put("maxHealth", character.optInt("maxHealth", 100));
        data.put("mana", character.optInt("mana", 50));
        data.put("maxMana", character.optInt("maxMana", 50));
        data.put("gold", character.optInt("gold", 100));
        data.put("killCounter", character.optInt("killCounter", 0));

        // ===== Зелья =====
        data.put("healthPotions", character.optInt("healthPotions", 0));
        data.put("manaPotions", character.optInt("manaPotions", 0));

        // ===== Данж и сложность =====
        data.put("currentDungeon", character.optString("currentDungeon"));
        data.put("difficulty", character.optInt("difficulty", 1));

        // ===== Координаты =====
        JSONObject pos = character.optJSONObject("lastDungeonPosition");
        if (pos != null) {
            data.put("lastX", pos.optDouble("x", 0));
            data.put("lastY", pos.optDouble("y", 0));
            data.put("lastZ", pos.optDouble("z", 0));
        }

        // ===== Инвентарь =====
        JSONArray inv = character.optJSONArray("inventory");
        System.out.println("[NetworkManager] Inventory array length: " + (inv != null ? inv.length() : "null"));
        if (inv != null) {
            List<Map<String, Object>> invList = new ArrayList<>();
            for (int i = 0; i < inv.length(); i++) {
                try {
                    JSONObject invItem = inv.getJSONObject(i);
                    Map<String, Object> map = new HashMap<>();
                    map.put("slot", invItem.optInt("slot"));
                    map.put("equipped", invItem.optBoolean("equipped"));

                    String eqSlot = invItem.optString("equipped_slot");
                    if (eqSlot.isEmpty()) {
                        eqSlot = invItem.optString("equippedSlot");
                    }
                    map.put("equippedSlot", eqSlot);

                    JSONObject itemObj = invItem.optJSONObject("item");
                    if (itemObj != null) {
                        // ИСПРАВЛЕНО: было itemObj.toMap() —
                        // на Android у org.json.JSONObject нет метода toMap().
                        map.put("item", jsonToMap(itemObj));
                    }
                    invList.add(map);
                } catch (Exception itemEx) {
                    System.out.println("[NetworkManager] Failed to parse inventory item at index "
                            + i + ": " + itemEx.getMessage());
                }
            }
            data.put("inventory", invList);
        }
        return data;
    }

    private void saveAuthToken(String token) {
        try {
            Path path = getTokenPath();

            File parent = path.toFile().getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            Files.write(path, token.getBytes(StandardCharsets.UTF_8));
            System.out.println("[NetworkManager] Token saved.");
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    private void loadAuthToken() {
        try {
            Path path = getTokenPath();
            if (Files.exists(path)) {
                authToken = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                isConnected = true;
                System.out.println("[NetworkManager] Token loaded.");
            }
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    private Path getTokenPath() {
        File storageFolder = JmeSystem.getStorageFolder();
        return Paths.get(storageFolder.getAbsolutePath(), TOKEN_FILE_NAME);
    }

    private Path getDeviceIdPath() {
        File storageFolder = JmeSystem.getStorageFolder();
        return Paths.get(storageFolder.getAbsolutePath(), DEVICE_ID_FILE_NAME);
    }

    public String getOrCreateDeviceId() {
        try {
            Path path = getDeviceIdPath();

            if (Files.exists(path)) {
                String existing = new String(Files.readAllBytes(path), StandardCharsets.UTF_8).trim();
                if (!existing.isEmpty()) {
                    return existing;
                }
            }

            String newId = java.util.UUID.randomUUID().toString();

            File parent = path.toFile().getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            Files.write(path, newId.getBytes(StandardCharsets.UTF_8));
            System.out.println("[NetworkManager] New deviceId generated: " + newId);
            return newId;

        } catch (IOException e) {
            e.printStackTrace(System.out);
            return null;
        }
    }

    public CompletableFuture<Boolean> deviceLogin(String deviceId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("deviceId", deviceId);
                String response = sendPostRequest("/auth/device-login", json.toString(), null);
                System.out.println("[NetworkManager] deviceLogin response: " + response);
                JSONObject result = new JSONObject(response);
                if (result.optBoolean("success", false)) {
                    authToken = result.optString("token");
                    saveAuthToken(authToken);
                    isConnected = true;
                    return true;
                }
                return false;
            } catch (Exception e) {
                e.printStackTrace(System.out);
                return false;
            }
        });
    }

    public String getAuthToken() { return authToken; }
    public boolean isConnected() { return isConnected; }
    public void cleanup() { isConnected = false; }

    // ================================================================
    //   ТАЛАНТЫ
    // ================================================================

    public CompletableFuture<Map<String, Object>> loadTalents() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (authToken == null) return null;
                String response = sendGetRequest("/talents", authToken);
                System.out.println("[NetworkManager] loadTalents raw response: " + response);
                if (response == null || response.isEmpty() || !response.trim().startsWith("{")) {
                    System.err.println("[NetworkManager] Invalid JSON response from /talents: " + response);
                    return null;
                }
                JSONObject result = new JSONObject(response);
                if (result.optBoolean("success", false)) {
                    Map<String, Object> data = new HashMap<>();
                    JSONObject talents = result.optJSONObject("talents");
                    Map<String, Integer> talentMap = new HashMap<>();
                    if (talents != null) {
                        for (String key : talents.keySet()) {
                            talentMap.put(key, talents.optInt(key, 0));
                        }
                    }
                    data.put("talents", talentMap);
                    data.put("availablePoints", result.optInt("availablePoints", 0));
                    return data;
                } else {
                    System.err.println("[NetworkManager] loadTalents failed: " + result.optString("message"));
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
                return null;
            }
        });
    }

    public CompletableFuture<Map<String, Object>> learnTalent(String talentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (authToken == null) return null;
                JSONObject json = new JSONObject();
                json.put("talentId", talentId);
                String response = sendPostRequest("/talents/learn", json.toString(), authToken);
                System.out.println("[NetworkManager] learnTalent raw response: " + response);

                JSONObject result = new JSONObject(response);
                if (result.optBoolean("success", false)) {
                    JSONObject talents = result.optJSONObject("talents");
                    Map<String, Integer> talentMap = new HashMap<>();
                    if (talents != null) {
                        for (String key : talents.keySet()) {
                            talentMap.put(key, talents.optInt(key, 0));
                        }
                    }
                    Map<String, Object> data = new HashMap<>();
                    data.put("talents", talentMap);
                    data.put("availablePoints", result.optInt("availablePoints", 0));
                    return data;
                } else {
                    String error = result.optString("message", "Unknown error");
                    Map<String, Object> data = new HashMap<>();
                    data.put("error", error);
                    return data;
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
                return null;
            }
        });
    }

    public CompletableFuture<Map<String, Object>> resetTalents() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (authToken == null) return null;
                String response = sendPostRequest("/talents/reset", "{}", authToken);
                System.out.println("[NetworkManager] resetTalents raw response: " + response);

                JSONObject result = new JSONObject(response);
                if (result.optBoolean("success", false)) {
                    JSONObject talents = result.optJSONObject("talents");
                    Map<String, Integer> talentMap = new HashMap<>();
                    if (talents != null) {
                        for (String key : talents.keySet()) {
                            talentMap.put(key, talents.optInt(key, 0));
                        }
                    }
                    Map<String, Object> data = new HashMap<>();
                    data.put("talents", talentMap);
                    data.put("availablePoints", result.optInt("availablePoints", 0));
                    return data;
                } else {
                    Map<String, Object> data = new HashMap<>();
                    data.put("error", result.optString("message", "Unknown error"));
                    return data;
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
                return null;
            }
        });
    }

    public CompletableFuture<Map<String, Object>> levelUp() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (authToken == null) return null;
                String response = sendPostRequest("/character/levelup", "{}", authToken);
                JSONObject result = new JSONObject(response);
                if (result.optBoolean("success", false)) {
                    return parseCharacterResponse(result);
                }
                return null;
            } catch (Exception e) {
                e.printStackTrace(System.out);
                return null;
            }
        });
    }

    // ================================================================
    //   КУЗНЕЦ
    // ================================================================

    public CompletableFuture<List<Map<String, Object>>> getBlacksmithItems() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (authToken == null) return null;
                String response = sendGetRequest("/blacksmith/items", authToken);
                System.out.println("[NetworkManager] getBlacksmithItems response: " + response);
                JSONObject result = new JSONObject(response);
                if (!result.optBoolean("success", false)) return null;

                JSONArray arr = result.optJSONArray("items");
                List<Map<String, Object>> list = new ArrayList<>();
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        // ИСПРАВЛЕНО: было arr.getJSONObject(i).toMap()
                        list.add(jsonToMap(arr.getJSONObject(i)));
                    }
                }
                return list;
            } catch (Exception e) {
                e.printStackTrace(System.out);
                return null;
            }
        });
    }

    public CompletableFuture<List<Map<String, Object>>> getBlacksmithGems() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (authToken == null) return null;
                String response = sendGetRequest("/blacksmith/gems", authToken);
                System.out.println("[NetworkManager] getBlacksmithGems response: " + response);
                JSONObject result = new JSONObject(response);
                if (!result.optBoolean("success", false)) return null;

                JSONArray arr = result.optJSONArray("gems");
                List<Map<String, Object>> list = new ArrayList<>();
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        // ИСПРАВЛЕНО: было arr.getJSONObject(i).toMap()
                        list.add(jsonToMap(arr.getJSONObject(i)));
                    }
                }
                return list;
            } catch (Exception e) {
                e.printStackTrace(System.out);
                return null;
            }
        });
    }

    public CompletableFuture<Map<String, Object>> insertGem(String itemId, int socketIndex, String gemItemId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (authToken == null) return null;
                JSONObject json = new JSONObject();
                json.put("itemId", itemId);
                json.put("socketIndex", socketIndex);
                json.put("gemItemId", gemItemId);
                String response = sendPostRequest("/blacksmith/insert", json.toString(), authToken);
                System.out.println("[NetworkManager] insertGem response: " + response);
                JSONObject result = new JSONObject(response);
                if (result.optBoolean("success", false)) {
                    return parseCharacterResponse(result);
                }
                System.err.println("[NetworkManager] insertGem failed: " + result.optString("message"));
                return null;
            } catch (Exception e) {
                e.printStackTrace(System.out);
                return null;
            }
        });
    }

    public CompletableFuture<Map<String, Object>> removeGem(String itemId, int socketIndex) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (authToken == null) return null;
                JSONObject json = new JSONObject();
                json.put("itemId", itemId);
                json.put("socketIndex", socketIndex);
                String response = sendPostRequest("/blacksmith/remove", json.toString(), authToken);
                System.out.println("[NetworkManager] removeGem response: " + response);
                JSONObject result = new JSONObject(response);
                if (result.optBoolean("success", false)) {
                    return parseCharacterResponse(result);
                }
                System.err.println("[NetworkManager] removeGem failed: " + result.optString("message"));
                return null;
            } catch (Exception e) {
                e.printStackTrace(System.out);
                return null;
            }
        });
    }

    public CompletableFuture<Map<String, List<Map<String, Object>>>> getAllItemSockets() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (authToken == null) return null;
                String response = sendGetRequest("/blacksmith/all-sockets", authToken);
                System.out.println("[NetworkManager] getAllItemSockets response: " + response);
                JSONObject result = new JSONObject(response);
                if (!result.optBoolean("success", false)) return null;

                JSONObject socketsObj = result.optJSONObject("sockets");
                Map<String, List<Map<String, Object>>> map = new HashMap<>();

                if (socketsObj != null) {
                    for (String itemId : socketsObj.keySet()) {
                        JSONArray arr = socketsObj.optJSONArray(itemId);
                        List<Map<String, Object>> list = new ArrayList<>();
                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                // ИСПРАВЛЕНО: было arr.getJSONObject(i).toMap()
                                list.add(jsonToMap(arr.getJSONObject(i)));
                            }
                        }
                        map.put(itemId, list);
                    }
                }

                return map;
            } catch (Exception e) {
                e.printStackTrace(System.out);
                return null;
            }
        });
    }
}