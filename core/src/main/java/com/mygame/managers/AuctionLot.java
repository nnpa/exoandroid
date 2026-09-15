package com.mygame.managers;

import com.mygame.items.Item;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

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
        if (lot.sellerName.isEmpty() && obj.has("seller_id")) {
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
                JSONObject itemObj = itemsArray.getJSONObject(i);
                Item item = Item.fromMap(itemObj.toMap());
                if (item != null) {
                    lot.items.add(item);
                }
            }
        }
        return lot;
    }

    // Геттеры
    public int getId() { return id; }
    public String getSellerName() { return sellerName; }
    public int getPrice() { return price; }
    public long getEndTime() { return endTime; }
    public int getStatus() { return status; }
    public List<Item> getItems() { return items; }
}