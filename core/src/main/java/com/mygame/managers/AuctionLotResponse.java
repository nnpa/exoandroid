package com.mygame.managers;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AuctionLotResponse {
    private List<AuctionLot> lots = new ArrayList<>();
    private int totalPages;
    private int currentPage;

    public AuctionLotResponse(JSONObject response) {
        this.totalPages = response.optInt("totalPages", 1);
        this.currentPage = response.optInt("currentPage", 1);

        JSONArray itemsArray = response.optJSONArray("items");
        if (itemsArray != null) {
            for (int i = 0; i < itemsArray.length(); i++) {
                JSONObject lotObj = itemsArray.getJSONObject(i);
                lots.add(AuctionLot.fromMap(lotObj));
            }
        }
    }

    public List<AuctionLot> getLots() { return lots; }
    public int getTotalPages() { return totalPages; }
    public int getCurrentPage() { return currentPage; }
}