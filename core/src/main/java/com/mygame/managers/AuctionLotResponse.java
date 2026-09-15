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
        System.out.println("[AuctionLotResponse] items array length = "
                + (itemsArray != null ? itemsArray.length() : "null"));

        if (itemsArray != null) {
            for (int i = 0; i < itemsArray.length(); i++) {
                try {
                    JSONObject lotObj = itemsArray.getJSONObject(i);
                    AuctionLot lot = AuctionLot.fromMap(lotObj);
                    if (lot != null) {
                        lots.add(lot);
                    }
                } catch (Exception lotEx) {
                    System.out.println("[AuctionLotResponse] Failed to parse lot #" + i
                            + ": " + lotEx.getMessage());
                    lotEx.printStackTrace(System.out);
                }
            }
        }

        System.out.println("[AuctionLotResponse] parsed lots = " + lots.size());
    }

    public List<AuctionLot> getLots() { return lots; }
    public int getTotalPages() { return totalPages; }
    public int getCurrentPage() { return currentPage; }
}