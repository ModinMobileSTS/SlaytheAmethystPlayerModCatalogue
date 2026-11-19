package spiceoverhaul;

import com.evacipated.cardcrawl.mod.hubris.relics.Spice;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.saveAndContinue.SaveFile;
import com.megacrit.cardcrawl.vfx.TextAboveCreatureEffect;
import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * Shared state and rules for the reworked Spice relic.
 *
 * Spice.counter is reused as the number of stacks.
 *
 * Stack 1 (first acquisition) costs 12 max HP and grants 1 energy every turn.
 * Every later stack follows a 3 step rotation and costs max HP depending on the
 * step:
 * <pre>
 *   step 0: +1 Str / +1 Dex                    -6 max HP
 *   step 1: +3 temporary HP, +1 Metallicize    -8 max HP
 *   step 2: +1 Regen                           -8 max HP
 * </pre>
 * The first time a stack cost cannot be paid in full the max HP simply stops
 * at the floor; that single "free" stack sets {@link #bottomedOnce}. From then
 * on normal sources stop offering Spice and only campfire trades (which must be
 * paid in full) can add stacks. After bottoming out, every combat starts with a
 * curse while max HP is at or below {@link #CURSE_MAX_HP}.
 */
public final class SpiceData {
    public static final String ID = "hubris:Spice";
    public static final int HP_FLOOR = 21;
    public static final int FIRST_HP_COST = 12;
    public static final int STRDEX_HP_COST = 6;
    public static final int OTHER_HP_COST = 8;
    public static final int NORMAL_FIGHT_MAX_HP = 2;
    public static final int HARD_FIGHT_MAX_HP = 4;
    /** While max HP is at or below this value every combat starts with a curse. */
    public static final int CURSE_MAX_HP = 30;

    /** True while CardCrawlGame.loadPlayerSave() restores relics from a save file. */
    public static boolean loading = false;

    /** Set when a campfire trade finished so the campfire stays usable. */
    public static boolean reopenCampfire = false;

    /** True once the max HP has hit the floor for the first time. Saved with the run. */
    public static boolean bottomedOnce = false;

    private static Boolean chinese = null;

    private SpiceData() {
    }

    public static Spice owned() {
        AbstractPlayer p = AbstractDungeon.player;
        if (p == null) {
            return null;
        }
        AbstractRelic r = p.getRelic(ID);
        return r instanceof Spice ? (Spice) r : null;
    }

    /** 0 = Str/Dex, 1 = temp HP + Metallicize, 2 = Regen. Returns -1 for stack 1. */
    public static int rotationType(int stack) {
        if (stack < 2) {
            return -1;
        }
        return (stack - 2) % 3;
    }

    public static int hpCost(int stack) {
        if (stack <= 1) {
            return FIRST_HP_COST;
        }
        return rotationType(stack) == 0 ? STRDEX_HP_COST : OTHER_HP_COST;
    }

    /** Counts how many times each rotation step happened for the given stack count. */
    public static int[] rotationCounts(int stacks) {
        int[] counts = new int[3];
        for (int stack = 2; stack <= stacks; stack++) {
            counts[rotationType(stack)]++;
        }
        return counts;
    }

    /** The full max HP cost of the next stack must be payable. */
    public static boolean canPayNextCost() {
        AbstractPlayer p = AbstractDungeon.player;
        Spice s = owned();
        if (p == null || s == null) {
            return false;
        }
        if (s.counter < 1) {
            return true;
        }
        int next = s.counter + 1;
        return p.maxHealth - hpCost(next) >= HP_FLOOR;
    }

    /** After the first bottoming out, normal sources no longer offer Spice. */
    public static boolean normalSpiceBlocked() {
        return bottomedOnce;
    }

    /** After bottoming out, low max HP curses the player at every combat start. */
    public static boolean curseActive() {
        AbstractPlayer p = AbstractDungeon.player;
        return bottomedOnce && p != null && owned() != null && p.maxHealth <= CURSE_MAX_HP;
    }

    /** A freshly obtained Spice starts at stack 1 and immediately costs 12 max HP. */
    public static void ensureFirstLayer() {
        if (loading) {
            return;
        }
        AbstractPlayer p = AbstractDungeon.player;
        Spice s = owned();
        if (p == null || s == null || s.counter >= 1) {
            return;
        }
        s.setCounter(1);
        bottomedOnce = false;
        applyHpCost(p, FIRST_HP_COST);
        s.flash();
    }

    /** Adds one stack from a normal source (elite victory, pickup, ...). */
    public static void gainLayer() {
        gainLayer(false);
    }

    /**
     * Adds one stack of Spice.
     *
     * @param fromCampfire campfire crafting bypasses the "normal source" block,
     *                     but it always has to pay the full cost
     */
    public static void gainLayer(boolean fromCampfire) {
        if (loading) {
            return;
        }
        AbstractPlayer p = AbstractDungeon.player;
        Spice s = owned();
        if (p == null || s == null) {
            return;
        }
        if (s.counter >= 1) {
            if (fromCampfire) {
                if (!canPayNextCost()) {
                    return;
                }
            } else {
                if (bottomedOnce) {
                    return;
                }
                if (!canPayNextCost()) {
                    // the single "free" stack that bottoms the player out
                    bottomedOnce = true;
                }
            }
        }
        int stack = s.counter < 1 ? 1 : s.counter + 1;
        s.setCounter(stack);
        applyHpCost(p, hpCost(stack));
        s.flash();
    }

    /** Every combat victory raises max HP; elite and boss fights give more. */
    public static void gainCombatMaxHp(boolean hardFight) {
        if (loading) {
            return;
        }
        AbstractPlayer p = AbstractDungeon.player;
        if (p == null || owned() == null) {
            return;
        }
        p.increaseMaxHp(hardFight ? HARD_FIGHT_MAX_HP : NORMAL_FIGHT_MAX_HP, true);
    }

    /** Applies the max HP cost, never dropping below the floor. */
    private static void applyHpCost(AbstractPlayer p, int cost) {
        int loss = Math.min(cost, Math.max(0, p.maxHealth - HP_FLOOR));
        if (loss <= 0) {
            return;
        }
        p.decreaseMaxHealth(loss);
        String text = "-" + loss;
        try {
            String label = AbstractCreature.TEXT[2];
            if (label != null && label.length() > 0) {
                text = label.substring(0, label.length() - 1) + "-" + loss;
            }
        } catch (Throwable ignored) {
        }
        AbstractDungeon.effectsQueue.add(new TextAboveCreatureEffect(
                p.hb.cX - p.animX, p.hb.cY, text, Settings.RED_TEXT_COLOR));
    }

    /** Combat start values derived from the current stack count. */
    public static final class BattleStats {
        public int strength;
        public int dexterity;
        public int tempHp;
        public int metallicize;
        public int regen;
    }

    public static BattleStats battleStats(int stacks) {
        BattleStats stats = new BattleStats();
        if (stacks < 1) {
            return stats;
        }
        int[] counts = rotationCounts(stacks);
        stats.strength = counts[0];
        stats.dexterity = counts[0];
        stats.tempHp = 3 * counts[1];
        stats.metallicize = counts[1];
        stats.regen = counts[2];
        return stats;
    }

    /** Keeps a single Spice relic in the player's relic list, merging stacks. */
    public static void dedupe(AbstractPlayer p) {
        if (p == null || p.relics == null) {
            return;
        }
        ArrayList<AbstractRelic> relics = p.relics;
        AbstractRelic keep = null;
        int best = Integer.MIN_VALUE;
        for (int i = 0; i < relics.size(); ) {
            AbstractRelic r = relics.get(i);
            if (r != null && ID.equals(r.relicId)) {
                if (keep == null) {
                    keep = r;
                    best = r.counter;
                    i++;
                } else {
                    if (r.counter > best) {
                        best = r.counter;
                    }
                    relics.remove(i);
                }
            } else {
                i++;
            }
        }
        if (keep != null && best > keep.counter) {
            keep.setCounter(best);
        }
    }

    /** Reconciles the relic list after a save file has been restored. */
    public static void afterLoad() {
        AbstractPlayer p = AbstractDungeon.player;
        if (p == null) {
            return;
        }
        int best = Integer.MIN_VALUE;
        SaveFile file = CardCrawlGame.saveFile;
        if (file != null && file.relics != null && file.relic_counters != null) {
            int size = Math.min(file.relics.size(), file.relic_counters.size());
            for (int i = 0; i < size; i++) {
                if (ID.equals(file.relics.get(i))) {
                    Integer c = file.relic_counters.get(i);
                    if (c != null && c > best) {
                        best = c;
                    }
                }
            }
        }
        dedupe(p);
        Spice s = owned();
        if (s != null) {
            int target = best >= 1 ? best : 1;
            if (s.counter != target) {
                s.setCounter(target);
            }
        }
    }

    public static int extraRelicChance(AbstractPotion.PotionRarity rarity) {
        if (rarity == null) {
            return 0;
        }
        switch (rarity) {
            case RARE:
                return 70;
            case UNCOMMON:
                return 40;
            case COMMON:
                return 20;
            default:
                return 0;
        }
    }

    public static String tradeDescription(AbstractPotion.PotionRarity rarity) {
        int chance = extraRelicChance(rarity);
        if (isChinese()) {
            return "消耗这瓶药水，制作 #b1 层 #y香料 （不消耗本次篝火行动）。 NL 需要付清下一层的最大生命代价，否则无法使用。 NL 这瓶药水有 #b"
                    + chance + "% 的概率额外给予一件随机遗物。";
        }
        return "Consume this potion to craft #b1 #ySpice stack (does not use up this campfire). NL Requires the full Max HP cost of the next stack. NL #b"
                + chance + "% chance to also gain a random relic.";
    }

    public static String description(Spice spice) {
        int stacks = Math.max(0, spice.counter);
        BattleStats stats = battleStats(stacks);
        int next = stacks + 1;
        StringBuilder sb = new StringBuilder();
        if (isChinese()) {
            sb.append("当前层数：#b").append(stacks).append(" 。 NL ");
            sb.append("首次获得：每回合获得 [E] ，最大生命 #r-").append(FIRST_HP_COST).append(" 。 NL ");
            sb.append("此后效果轮换： #b1力1敏（最大生命#r-").append(STRDEX_HP_COST).append("） → #b3临时生命+1金属化（#r-")
                    .append(OTHER_HP_COST).append("） → #b1再生（#r-").append(OTHER_HP_COST).append("） 。 NL ");
            if (stacks >= 1) {
                sb.append("当前战斗开始：");
                boolean any = false;
                if (stats.strength > 0) {
                    sb.append("#b").append(stats.strength).append(" 力量、#b").append(stats.dexterity).append(" 敏捷");
                    any = true;
                }
                if (stats.tempHp > 0) {
                    if (any) {
                        sb.append("、");
                    }
                    sb.append("#b").append(stats.tempHp).append(" 临时生命");
                    any = true;
                }
                if (stats.metallicize > 0) {
                    if (any) {
                        sb.append("、");
                    }
                    sb.append("#b").append(stats.metallicize).append(" 金属化");
                    any = true;
                }
                if (stats.regen > 0) {
                    if (any) {
                        sb.append("、");
                    }
                    sb.append("#b").append(stats.regen).append(" 再生");
                    any = true;
                }
                if (!any) {
                    sb.append("无额外战斗增益");
                }
                sb.append(" 。 NL ");
            }
            sb.append("每场战斗胜利后最大生命值提升：普通战斗 #b+").append(NORMAL_FIGHT_MAX_HP)
                    .append(" ，精英/Boss战斗 #b+").append(HARD_FIGHT_MAX_HP).append(" 。 NL ");
            if (normalSpiceBlocked()) {
                sb.append("#r已首次触底：普通途径不再出现香料，只能靠篝火制作（需付清最大生命代价）。 NL ");
            } else if (stacks >= 1 && !canPayNextCost()) {
                sb.append("#r下一层将触底（代价只扣到 ").append(HP_FLOOR).append("），这是唯一一次“白嫖”机会。 NL ");
            } else {
                sb.append("下一层：").append(effectName(next)).append("，最大生命 #r-").append(hpCost(next)).append(" 。 NL ");
            }
            if (bottomedOnce && AbstractDungeon.player != null && AbstractDungeon.player.maxHealth <= CURSE_MAX_HP) {
                sb.append("#r触底后最大生命 ≤ ").append(CURSE_MAX_HP)
                        .append(" ：每场战斗开始时，将 #b1 张 #r固有保留 的 #r死灵诅咒 放入抽牌堆。（最大生命 > ")
                        .append(CURSE_MAX_HP).append(" 时移除此效果） NL ");
            }
            sb.append("篝火处可用药水制作香料，且不消耗篝火行动。");
        } else {
            sb.append("Stacks: #b").append(stacks).append(" . NL ");
            sb.append("First stack: gain [E] each turn, lose #r").append(FIRST_HP_COST).append(" Max HP. NL ");
            sb.append("Later stacks rotate: #b1 Str/1 Dex (#r-").append(STRDEX_HP_COST).append("), #b3 temp HP + 1 Metallicize (#r-")
                    .append(OTHER_HP_COST).append("), #b1 Regen (#r-").append(OTHER_HP_COST).append("). NL ");
            if (stacks >= 1) {
                sb.append("Combat start now:");
                boolean any = false;
                if (stats.strength > 0) {
                    sb.append(" #b").append(stats.strength).append(" Str, #b").append(stats.dexterity).append(" Dex");
                    any = true;
                }
                if (stats.tempHp > 0) {
                    sb.append(any ? "," : "").append(" #b").append(stats.tempHp).append(" temp HP");
                    any = true;
                }
                if (stats.metallicize > 0) {
                    sb.append(any ? "," : "").append(" #b").append(stats.metallicize).append(" Metallicize");
                    any = true;
                }
                if (stats.regen > 0) {
                    sb.append(any ? "," : "").append(" #b").append(stats.regen).append(" Regen");
                    any = true;
                }
                if (!any) {
                    sb.append(" no extra combat buffs");
                }
                sb.append(" . NL ");
            }
            sb.append("After each combat victory gain Max HP: normal #b+").append(NORMAL_FIGHT_MAX_HP)
                    .append(" , elite/boss #b+").append(HARD_FIGHT_MAX_HP).append(" . NL ");
            if (normalSpiceBlocked()) {
                sb.append("#rAlready bottomed out once: Spice no longer appears from normal sources, only campfire crafting works (needs the full Max HP cost). NL ");
            } else if (stacks >= 1 && !canPayNextCost()) {
                sb.append("#rThe next stack bottoms you out (cost clamps at ").append(HP_FLOOR).append("); this is the only free one. NL ");
            } else {
                sb.append("Next stack: ").append(effectName(next)).append(", #r-").append(hpCost(next)).append(" Max HP. NL ");
            }
            if (bottomedOnce && AbstractDungeon.player != null && AbstractDungeon.player.maxHealth <= CURSE_MAX_HP) {
                sb.append("#rAfter bottoming out with Max HP ").append(CURSE_MAX_HP)
                        .append(" or lower: every combat starts with #b1 #rInnate Retain #rNecronomicurse. (Removed while Max HP > ")
                        .append(CURSE_MAX_HP).append(") NL ");
            }
            sb.append("Trade a potion for Spice at a campfire; it does not use up the campfire action.");
        }
        return sb.toString();
    }

    public static String effectName(int stack) {
        int type = rotationType(stack);
        if (isChinese()) {
            if (type < 0) {
                return "每回合 [E]";
            }
            switch (type) {
                case 0:
                    return "1力1敏";
                case 1:
                    return "3临时生命+1金属化";
                default:
                    return "1再生";
            }
        }
        if (type < 0) {
            return "[E] each turn";
        }
        switch (type) {
            case 0:
                return "1 Str / 1 Dex";
            case 1:
                return "3 temp HP + 1 Metallicize";
            default:
                return "1 Regen";
        }
    }

    private static boolean isChinese() {
        if (chinese == null) {
            boolean result = false;
            try {
                Field field = Settings.class.getDeclaredField("language");
                field.setAccessible(true);
                Object value = field.get(null);
                result = value != null && "ZHS".equals(String.valueOf(value));
            } catch (Throwable ignored) {
                result = false;
            }
            chinese = Boolean.valueOf(result);
        }
        return chinese.booleanValue();
    }
}
