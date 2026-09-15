package com.mygame.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TalentTree {
    private Talent.Branch branch;
    private List<Talent> talents = new ArrayList<>();

    public TalentTree(Talent.Branch branch) {
        this.branch = branch;
    }

    public void addTalent(Talent t) {
        talents.add(t);
    }

    public boolean isAvailable(Talent t, Map<String, Integer> learned) {
        for (String prereqId : t.getPrerequisites()) {
            if (!learned.containsKey(prereqId) || learned.get(prereqId) == 0) {
                return false;
            }
        }
        return true;
    }

    public boolean canLevelUp(Talent t, Map<String, Integer> learned, int availablePoints) {
        if (learned.getOrDefault(t.getId(), 0) >= t.getMaxLevel()) return false;
        if (availablePoints < t.getCost()) return false;
        return isAvailable(t, learned);
    }

    public List<Talent> getTalents() {
        return talents;
    }

    public Talent getTalentById(String id) {
        for (Talent t : talents) {
            if (t.getId().equals(id)) return t;
        }
        return null;
    }
}