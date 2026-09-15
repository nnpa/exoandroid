package com.mygame.managers;

import java.util.ArrayList;
import java.util.List;

public class Talent {
    public enum Branch { DEFENSE, LIGHT, ATTACK }

    private String id;
    private String name;
    private String description;
    private Branch branch;
    private int row;
    private int column;
    private int maxLevel;
    private int cost;
    private List<String> prerequisites = new ArrayList<>();
    private List<TalentEffect> effects = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }

    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }

    public int getColumn() { return column; }
    public void setColumn(int column) { this.column = column; }

    public int getMaxLevel() { return maxLevel; }
    public void setMaxLevel(int maxLevel) { this.maxLevel = maxLevel; }

    public int getCost() { return cost; }
    public void setCost(int cost) { this.cost = cost; }

    public List<String> getPrerequisites() { return prerequisites; }
    public void setPrerequisites(List<String> prerequisites) { this.prerequisites = prerequisites; }

    public List<TalentEffect> getEffects() { return effects; }
    public void setEffects(List<TalentEffect> effects) { this.effects = effects; }

    // ---- ЛОКАЛИЗОВАННЫЕ ГЕТТЕРЫ ----
    public String getLocalizedName() {
        return LocalizationManager.getInstance().get("talent." + id + ".name");
    }

    public String getLocalizedDescription() {
        return LocalizationManager.getInstance().get("talent." + id + ".desc");
    }

    public static class TalentEffect {
        public String stat;
        public float value;
        public boolean isPercent;

        public TalentEffect(String stat, float value, boolean isPercent) {
            this.stat = stat;
            this.value = value;
            this.isPercent = isPercent;
        }
    }
}