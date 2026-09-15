package com.mygame.managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TalentManager {

    private final PlayerManager playerManager;

    private final Map<Talent.Branch, TalentTree> trees =
            new HashMap<>();

    private final Map<String, Integer> learned =
            new HashMap<>();

    private int availablePoints = 20;

    private final NetworkManager networkManager;

    public TalentManager(
            PlayerManager playerManager,
            NetworkManager networkManager) {

        this.playerManager = playerManager;

        this.networkManager = networkManager;

        initializeTalents();

        System.out.println(
                "[TalentManager] Initialized with 20 points"
        );
    }

    // ============================================================
    // СОЗДАНИЕ ДЕРЕВЬЕВ
    // ============================================================

    private void initializeTalents() {

        for (Talent.Branch branch :
                Talent.Branch.values()) {

            trees.put(
                    branch,
                    new TalentTree(branch)
            );
        }

        // ========================================================
        // DEFENSE
        // ========================================================

        addTalent(
                Talent.Branch.DEFENSE,
                "def_1",
                "Toughness",
                "+5% physical defense",
                0,
                0,
                5,
                1,
                new ArrayList<>(),
                Arrays.asList(
                        new Talent.TalentEffect(
                                "physical_defense",
                                5f,
                                true
                        )
                )
        );

        addTalent(
                Talent.Branch.DEFENSE,
                "def_2",
                "Iron Skin",
                "+10% max health",
                0,
                1,
                3,
                1,
                new ArrayList<>(),
                Arrays.asList(
                        new Talent.TalentEffect(
                                "max_health",
                                10f,
                                true
                        )
                )
        );

        addTalent(
                Talent.Branch.DEFENSE,
                "def_3",
                "Magic Barrier",
                "+5% magical defense",
                1,
                0,
                3,
                1,
                Arrays.asList("def_1"),
                Arrays.asList(
                        new Talent.TalentEffect(
                                "magical_defense",
                                5f,
                                true
                        )
                )
        );

        addTalent(
                Talent.Branch.DEFENSE,
                "def_4",
                "Shield Block",
                "+10% block chance",
                1,
                1,
                3,
                1,
                Arrays.asList("def_2"),
                Arrays.asList(
                        new Talent.TalentEffect(
                                "block_chance",
                                10f,
                                true
                        )
                )
        );

        addTalent(
                Talent.Branch.DEFENSE,
                "def_5",
                "Resilience",
                "-15% critical damage taken",
                2,
                0,
                2,
                1,
                Arrays.asList("def_3"),
                Arrays.asList(
                        new Talent.TalentEffect(
                                "crit_damage_reduction",
                                15f,
                                true
                        )
                )
        );

        addTalent(
                Talent.Branch.DEFENSE,
                "def_6",
                "Regeneration",
                "+2% HP regeneration",
                2,
                1,
                3,
                1,
                Arrays.asList("def_4"),
                Arrays.asList(
                        new Talent.TalentEffect(
                                "health_regen_out_of_combat",
                                2f,
                                true
                        )
                )
        );

        addTalent(
                Talent.Branch.DEFENSE,
                "def_7",
                "Fortify",
                "+20% armor",
                3,
                0,
                2,
                1,
                new ArrayList<>(),
                Arrays.asList(
                        new Talent.TalentEffect(
                                "armor",
                                20f,
                                true
                        )
                )
        );

        addTalent(
                Talent.Branch.DEFENSE,
                "def_8",
                "Impervious",
                "Ignore 10% damage",
                3,
                1,
                1,
                1,
                new ArrayList<>(),
                Arrays.asList(
                        new Talent.TalentEffect(
                                "damage_ignored",
                                10f,
                                true
                        )
                )
        );

        addTalent(
                Talent.Branch.DEFENSE,
                "def_9",
                "Undying",
                "Survive fatal damage",
                4,
                0,
                1,
                1,
                Arrays.asList("def_7"),
                Arrays.asList(
                        new Talent.TalentEffect(
                                "death_defiance",
                                1f,
                                false
                        )
                )
        );

        addTalent(
                Talent.Branch.DEFENSE,
                "def_10",
                "Defense Aura",
                "+15% defense aura",
                4,
                1,
                2,
                1,
                Arrays.asList("def_8"),
                Arrays.asList(
                        new Talent.TalentEffect(
                                "aura_defense",
                                15f,
                                true
                        )
                )
        );

        // ========================================================
        // LIGHT
        // ========================================================

        addTalent(
                Talent.Branch.LIGHT,
                "light_1",
                "Healing",
                "+15% healing power",
                0,
                0,
                5,
                1,
                new ArrayList<>(),
                Arrays.asList(
                        new Talent.TalentEffect(
                                "heal_power",
                                15f,
                                true
                        )
                )
        );

        addTalent(
                Talent.Branch.LIGHT,
                "light_2",
                "Divine Shield",
                "Healing gives 20% shield",
                0,
                1,
                3,
                1,
                new ArrayList<>(),
                Arrays.asList(
                        new Talent.TalentEffect(
                                "shield_from_heal",
                                0.2f,
                                false
                        )
                )
        );

        addTalent(
                Talent.Branch.LIGHT,
                "light_3",
                "Holy Strike",
                "+10% holy damage",
                1,
                0,
                3,
                1,
                Arrays.asList("light_1"),
                Arrays.asList(
                        new Talent.TalentEffect(
                                "holy_damage_percent",
                                10f,
                                true
                        )
                )
        );

        addTalent(
                Talent.Branch.LIGHT,
                "light_4",
                "Cleansing",
                "-20% debuff duration",
                1,
                1,
                2,
                1,
                Arrays.asList("light_2"),
                Arrays.asList(
                        new Talent.TalentEffect(
                                "debuff_duration_reduction",
                                20f,
                                true
                        )
                )
        );

        addTalent(
                Talent.Branch.LIGHT,
                "light_5",
                "Holy Power",
                "+20% light damage",
                2,
                0,
                3,
                1,
                Arrays.asList("light_3"),
                Arrays.asList(
                        new Talent.TalentEffect(
                                "light_beam_damage",
                                20f,
                                true
                        )
                )
        );

        addTalent(
                Talent.Branch.LIGHT,
                "light_6",
                "Angelic Protection",
                "+15% defense on heal",
                2,
                1,
                2,
                1,
                Arrays.asList("light_4"),
                Arrays.asList(
                        new Talent.TalentEffect(
                                "holy_shield_defense",
                                15f,
                                true
                        )
                )
        );

        addTalent(
                Talent.Branch.LIGHT,
                "light_7",
                "Blessing",
                "+20% incoming healing",
                3,
                0,
                3,
                1,
                new ArrayList<>(),
                Arrays.asList(
                        new Talent.TalentEffect(
                                "incoming_heal",
                                20f,
                                true
                        )
                )
        );

        addTalent(
                Talent.Branch.LIGHT,
                "light_8",
                "Inspiration",
                "Heal restores 5% mana",
                3,
                1,
                2,
                1,
                new ArrayList<>(),
                Arrays.asList(
                        new Talent.TalentEffect(
                                "mana_on_heal_percent",
                                5f,
                                true
                        )
                )
        );

        addTalent(
                Talent.Branch.LIGHT,
                "light_9",
                "Light Nova",
                "Healing deals damage",
                4,
                0,
                2,
                1,
                Arrays.asList("light_7"),
                Arrays.asList(
                        new Talent.TalentEffect(
                                "light_nova_damage",
                                10f,
                                false
                        )
                )
        );

        addTalent(
                Talent.Branch.LIGHT,
                "light_10",
                "Resurrection",
                "Restore 30% HP on death",
                4,
                1,
                1,
                1,
                Arrays.asList("light_8"),
                Arrays.asList(
                        new Talent.TalentEffect(
                                "resurrection_hp",
                                30f,
                                true
                        )
                )
        );

        // ========================================================
        // ATTACK
        // ========================================================

        addTalent(
                Talent.Branch.ATTACK,
                "attack_1",
                "Shield Bash",
                "+20% bash damage",
                0,
                0,
                5,
                1,
                new ArrayList<>(),
                Arrays.asList(
                        new Talent.TalentEffect(
                                "shield_bash_damage",
                                20f,
                                true
                        )
                )
        );

        addTalent(
                Talent.Branch.ATTACK,
                "attack_2",
                "Sweep",
                "+30% whirlwind radius",
                0,
                1,
                3,
                1,
                new ArrayList<>(),
                Arrays.asList(
                        new Talent.TalentEffect(
                                "whirlwind_radius",
                                30f,
                                true
                        )
                )
        );

        addTalent(
                Talent.Branch.ATTACK,
                "attack_3",
                "Kick",
                "+0.5s stun",
                1,
                0,
                3,
                1,
                Arrays.asList("attack_1"),
                Arrays.asList(
                        new Talent.TalentEffect(
                                "kick_stun_duration",
                                0.5f,
                                false
                        )
                )
        );

        addTalent(
                Talent.Branch.ATTACK,
                "attack_4",
                "Divine Wrath",
                "+50% next attack",
                1,
                1,
                2,
                1,
                Arrays.asList("attack_2"),
                Arrays.asList(
                        new Talent.TalentEffect(
                                "divine_wrath_damage",
                                50f,
                                true
                        )
                )
        );

        addTalent(
                Talent.Branch.ATTACK,
                "attack_5",
                "Critical Strike",
                "+10% crit chance",
                2,
                0,
                4,
                1,
                Arrays.asList("attack_3"),
                Arrays.asList(
                        new Talent.TalentEffect(
                                "crit_chance",
                                10f,
                                true
                        )
                )
        );

        addTalent(
                Talent.Branch.ATTACK,
                "attack_6",
                "Mastery",
                "+15% attack speed",
                2,
                1,
                3,
                1,
                Arrays.asList("attack_4"),
                Arrays.asList(
                        new Talent.TalentEffect(
                                "attack_speed",
                                15f,
                                true
                        )
                )
        );

        addTalent(
                Talent.Branch.ATTACK,
                "attack_7",
                "Strength",
                "+10% base damage",
                3,
                0,
                5,
                1,
                new ArrayList<>(),
                Arrays.asList(
                        new Talent.TalentEffect(
                                "base_damage",
                                10f,
                                true
                        )
                )
        );

        addTalent(
                Talent.Branch.ATTACK,
                "attack_8",
                "Precision",
                "+10% hit chance",
                3,
                1,
                3,
                1,
                new ArrayList<>(),
                Arrays.asList(
                        new Talent.TalentEffect(
                                "hit_chance",
                                10f,
                                true
                        )
                )
        );

        addTalent(
                Talent.Branch.ATTACK,
                "attack_9",
                "Lethal Strike",
                "+30% crit damage",
                4,
                0,
                3,
                1,
                Arrays.asList("attack_7"),
                Arrays.asList(
                        new Talent.TalentEffect(
                                "crit_damage",
                                30f,
                                true
                        )
                )
        );

        addTalent(
                Talent.Branch.ATTACK,
                "attack_10",
                "Rage",
                "+20% attack speed on kill",
                4,
                1,
                2,
                1,
                Arrays.asList("attack_8"),
                Arrays.asList(
                        new Talent.TalentEffect(
                                "rage_attack_speed",
                                20f,
                                true
                        )
                )
        );
    }

    // ============================================================
    // ADD TALENT
    // ============================================================

    private void addTalent(
            Talent.Branch branch,
            String id,
            String name,
            String desc,
            int row,
            int col,
            int maxLevel,
            int cost,
            List<String> prereqs,
            List<Talent.TalentEffect> effects) {

        Talent t = new Talent();

        t.setId(id);
        t.setName(name);
        t.setDescription(desc);
        t.setBranch(branch);
        t.setRow(row);
        t.setColumn(col);
        t.setMaxLevel(maxLevel);
        t.setCost(cost);
        t.setPrerequisites(prereqs);
        t.setEffects(effects);

        trees.get(branch)
                .addTalent(t);
    }

    // ============================================================
    // LOCAL
    // ============================================================

    public boolean hasTalent(String talentId) {

        return learned.containsKey(talentId)
                && learned.get(talentId) > 0;
    }

    public int getTalentLevel(
            String talentId) {

        return learned.getOrDefault(
                talentId,
                0
        );
    }

    public boolean levelUpTalent(
            String talentId) {

        Talent t =
                findTalent(talentId);

        if (t == null) return false;

        if (availablePoints
                < t.getCost()) {

            return false;
        }

        int currentLevel =
                learned.getOrDefault(
                        talentId,
                        0
                );

        if (currentLevel
                >= t.getMaxLevel()) {

            return false;
        }

        TalentTree tree =
                trees.get(
                        t.getBranch()
                );

        if (!tree.isAvailable(
                t,
                learned
        )) {

            return false;
        }

        learned.put(
                talentId,
                currentLevel + 1
        );

        availablePoints -=
                t.getCost();

        recalcAllBonuses();

        return true;
    }

    // ============================================================
    // RESET
    // ============================================================

    public void resetTalents() {

        for (String id :
                new ArrayList<>(learned.keySet())) {

            Talent t =
                    findTalent(id);

            if (t != null) {

                availablePoints +=
                        t.getCost()
                                * learned.get(id);
            }
        }

        learned.clear();

        recalcAllBonuses();
    }

    // ============================================================
    // BONUS
    // ============================================================

    public void recalcAllBonuses() {

        Map<String, Float> total =
                new HashMap<>();

        for (String id :
                learned.keySet()) {

            Talent t =
                    findTalent(id);

            if (t == null) continue;

            int level =
                    learned.get(id);

            for (Talent.TalentEffect effect :
                    t.getEffects()) {

                float add =
                        effect.value
                                * level;

                total.put(
                        effect.stat,
                        total.getOrDefault(
                                effect.stat,
                                0f
                        ) + add
                );
            }
        }

        if (playerManager != null) {

            playerManager.applyTalentBonuses(
                    total
            );
        }
    }

    // ============================================================
    // FIND
    // ============================================================

    private Talent findTalent(
            String id) {

        for (TalentTree tree :
                trees.values()) {

            Talent t =
                    tree.getTalentById(id);

            if (t != null) {

                return t;
            }
        }

        return null;
    }

    // ============================================================
    // SERVER
    // ============================================================

    public CompletableFuture<Boolean>
    levelUpTalentAsync(
            String talentId) {

        if (networkManager == null) {

            return CompletableFuture.completedFuture(
                    levelUpTalent(talentId)
            );
        }

        return networkManager
                .learnTalent(talentId)
                .thenApply(response -> {

                    if (response == null) {

                        return false;
                    }

                    if (response.containsKey(
                            "error"
                    )) {

                        System.err.println(
                                "[TalentManager] Server error: "
                                        + response.get("error")
                        );

                        return false;
                    }

                    Object talentsObject =
                            response.get("talents");

                    Object pointsObject =
                            response.get(
                                    "availablePoints"
                            );

                    if (!(talentsObject
                            instanceof Map)) {

                        return false;
                    }

                    Map<?, ?> rawTalents =
                            (Map<?, ?>)
                                    talentsObject;

                    Map<String, Integer>
                            talents =
                            new HashMap<>();

                    for (Map.Entry<?, ?> entry :
                            rawTalents.entrySet()) {

                        talents.put(
                                String.valueOf(
                                        entry.getKey()
                                ),
                                ((Number)
                                        entry.getValue())
                                        .intValue()
                        );
                    }

                    int points =
                            pointsObject instanceof Number
                                    ? ((Number)
                                    pointsObject)
                                    .intValue()
                                    : 0;

                    this.learned.clear();

                    this.learned.putAll(
                            talents
                    );

                    this.availablePoints =
                            points;

                    recalcAllBonuses();

                    return true;
                });
    }

    // ============================================================
    // RESET SERVER
    // ============================================================

    public CompletableFuture<Boolean>
    resetTalentsAsync() {

        if (networkManager == null) {

            resetTalents();

            return CompletableFuture.completedFuture(
                    true
            );
        }

        return networkManager
                .resetTalents()
                .thenApply(response -> {

                    if (response == null
                            || response.containsKey(
                            "error"
                    )) {

                        return false;
                    }

                    Object talentsObject =
                            response.get("talents");

                    Object pointsObject =
                            response.get(
                                    "availablePoints"
                            );

                    Map<String, Integer>
                            talents =
                            new HashMap<>();

                    if (talentsObject
                            instanceof Map) {

                        Map<?, ?> raw =
                                (Map<?, ?>)
                                        talentsObject;

                        for (Map.Entry<?, ?> entry :
                                raw.entrySet()) {

                            talents.put(
                                    String.valueOf(
                                            entry.getKey()
                                    ),
                                    ((Number)
                                            entry.getValue())
                                            .intValue()
                            );
                        }
                    }

                    int points =
                            pointsObject instanceof Number
                                    ? ((Number)
                                    pointsObject)
                                    .intValue()
                                    : 0;

                    this.learned.clear();

                    this.learned.putAll(
                            talents
                    );

                    this.availablePoints =
                            points;

                    recalcAllBonuses();

                    return true;
                });
    }

    // ============================================================
    // LOAD SERVER
    // ============================================================

    public void loadFromServer(
            Map<String, Integer> serverTalents,
            int serverPoints) {

        this.learned.clear();

        this.availablePoints =
                serverPoints;

        if (serverTalents != null) {

            for (Map.Entry<String, Integer> entry :
                    serverTalents.entrySet()) {

                learned.put(
                        entry.getKey(),
                        entry.getValue()
                );
            }
        }

        recalcAllBonuses();
    }

    // ============================================================
    // GETTERS
    // ============================================================

    public int getAvailablePoints() {
        return availablePoints;
    }

    public Map<String, Integer>
    getLearned() {

        return learned;
    }

    public Map<Talent.Branch, TalentTree>
    getTrees() {

        return trees;
    }

    // ============================================================
    // LEVEL POINTS
    // ============================================================

    public void addPointsForLevel(
            int level) {

        if (level % 5 == 0
                && level > 0) {

            availablePoints++;

            System.out.println(
                    "[TalentManager] +1 point for reaching level "
                            + level
                            + "! Total: "
                            + availablePoints
            );
        }
    }
}