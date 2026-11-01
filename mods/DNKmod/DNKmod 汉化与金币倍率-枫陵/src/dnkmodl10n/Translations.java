package dnkmodl10n;

import java.util.HashMap;
import java.util.Map;

/**
 * English -> Chinese text table for DNKmod.
 * Only exact matches are replaced, so unknown strings are left untouched.
 */
public final class Translations {

    private static final Map<String, String> PANEL_LABELS = new HashMap<>();
    private static final Map<String, String> MONSTER_NAMES = new HashMap<>();
    private static final Map<String, String> MOVE_NAMES = new HashMap<>();
    private static final Map<String, String> MOVE_DESCRIPTIONS = new HashMap<>();

    static {
        // Mod config panel toggles
        PANEL_LABELS.put("StartingHP", "初始生命值");
        PANEL_LABELS.put("StartingGold", "初始金币");
        PANEL_LABELS.put("StartingEnergy", "初始能量");
        PANEL_LABELS.put("Enable Familiar", "启用随从");
        PANEL_LABELS.put("Enable FamiliarTank", "启用坦克随从");
        PANEL_LABELS.put("MaxPotions", "药水槽上限");
        PANEL_LABELS.put("RandomPotions", "初始随机药水");
        PANEL_LABELS.put("RandomRelics", "初始随机遗物");
        PANEL_LABELS.put("MaxHand", "手牌上限");

        // Companion names
        MONSTER_NAMES.put("Familiar", "随从");
        MONSTER_NAMES.put("FamiliarTank", "坦克随从");

        // Companion move buttons (tooltip header)
        MOVE_NAMES.put("Attack", "攻击");
        MOVE_NAMES.put("Defend", "防御");
        MOVE_NAMES.put("Taunt", "嘲讽");

        // Companion move tooltips (tooltip body)
        MOVE_DESCRIPTIONS.put("Deal 5 damage", "造成 5 点伤害");
        MOVE_DESCRIPTIONS.put("Gain 5 block", "获得 5 点格挡");
        MOVE_DESCRIPTIONS.put("Taunt a monster", "嘲讽一名敌人");
    }

    private Translations() {
    }

    public static String panelLabel(String original) {
        return lookup(PANEL_LABELS, original);
    }

    public static String monsterName(String original) {
        return lookup(MONSTER_NAMES, original);
    }

    public static String moveName(String original) {
        return lookup(MOVE_NAMES, original);
    }

    public static String moveDescription(String original) {
        return lookup(MOVE_DESCRIPTIONS, original);
    }

    private static String lookup(Map<String, String> table, String original) {
        if (original == null) {
            return null;
        }
        return table.get(original);
    }
}
