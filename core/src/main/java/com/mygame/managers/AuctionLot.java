package com.mygame.managers;

import com.mygame.items.Item;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class AuctionLot {
    private int id;
    private String sellerName;
    private int price;
    private long endTime;
    private int status;
    private List<Item> items = new ArrayList<>();

    public static AuctionLot fromMap(JSONObject obj) {
        AuctionLot lot = new AuctionLot();
        lot.id = obj.optInt("id");

        lot.sellerName = obj.optString("sellerName");
        if (lot.sellerName == null || lot.sellerName.isEmpty()) {
            lot.sellerName = obj.optString("seller_id");
        }

        lot.price = obj.optInt("price");

        // Парсим endTime
        if (obj.has("endTime")) {
            lot.endTime = obj.optLong("endTime");
        } else if (obj.has("end_time")) {
            Object endTimeObj = obj.get("end_time");
            if (endTimeObj instanceof Number) {
                long val = ((Number) endTimeObj).longValue();
                lot.endTime = (val < 10000000000L) ? val * 1000L : val;
            } else if (endTimeObj instanceof String) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    lot.endTime = sdf.parse((String) endTimeObj).getTime();
                } catch (Exception e) {
                    lot.endTime = 0;
                }
            }
        }

        lot.status = obj.optInt("status");

        JSONArray itemsArray = obj.optJSONArray("items");
        if (itemsArray != null) {
            for (int i = 0; i < itemsArray.length(); i++) {
                try {
                    JSONObject itemObj = itemsArray.getJSONObject(i);

                    // ============================================================
                    // ИСПРАВЛЕНО: было itemObj.toMap().
                    // На Android у системного org.json.JSONObject нет метода
                    // toMap() — он есть только в desktop-сборке org.json:json.
                    // Вызов toMap() → NoSuchMethodError → падение всего
                    // AuctionLotResponse → browse показывает пусто.
                    // ============================================================
                    Map<String, Object> itemMap = jsonToMap(itemObj);
                    Item item = Item.fromMap(itemMap);
                    if (item != null) {
                        lot.items.add(item);
                    }
                } catch (Exception itemEx) {
                    System.out.println("[AuctionLot] Failed to parse item #" + i
                            + " in lot#" + lot.id + ": " + itemEx.getMessage());
                }
            }
        }
        return lot;
    }

    // ================================================================
    //   JSONObject → Map (Android-safe, без toMap())
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
            } else if (value == JSONObject.NULL) {
                list.add(null);
            } else {
                list.add(value);
            }
        }
        return list;
    }

    // Геттеры
    public int getId() { return id; }
    public String getSellerName() { return sellerName; }
    public int getPrice() { return price; }
    public long getEndTime() { return endTime; }
    public int getStatus() { return status; }
    public List<Item> getItems() { return items; }
}