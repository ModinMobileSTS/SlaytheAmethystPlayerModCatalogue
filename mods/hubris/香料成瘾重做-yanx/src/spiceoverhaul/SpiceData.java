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
 * Spice.counter is reused as the number of stacks.  Stacks are gained by
 * obtaining Spice, by winning an elite / boss fight and by trading a potion at
 * a campfire.  All vanilla saving / loading of relic counters keeps working.
 *
 * A separate recovery counter (saved through BaseMod) fills up with combat
 * victories: +1 for a normal fight, +2 for an elite / boss fight.  Once it
 * reaches RECOVERY_NEED while the player's max HP already hit the floor, all
 * HP is restored and the counter resets.
 */
public final class SpiceData {
    public static final String ID = "hubris:Spice";
    public static final int HP_FLOOR = 21;
    public static final int HP_LOSS = 5;
    public static final int RECOVERY_NEED = 4;

    /** True while CardCrawlGame.loadPlayerSave() restores relics from a save file. */
    public static boolean loading = false;

    /** Combat based counter that triggers the full heal. */
    public static int recoveryCharge = 0;

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

    public static int strength(int stacks) {
        return stacks <= 0 ? 0 : 2 * stacks;
    }

    public static int dexterity(int stacks) {
        return stacks <= 0 ? 0 : stacks + 1;
    }

    /** A freshly obtained Spice must start at stack 1 so its effects work. */
    public static void ensureFirstLayer() {
        if (loading) {
            return;
        }
        Spice s = owned();
        if (s != null && s.counter < 1) {
            s.setCounter(1);
            recoveryCharge = 0;
        }
    }

    /** Adds one stack of Spice with all of its consequences. */
    public static void gainLayer() {
        if (loading) {
            return;
        }
        AbstractPlayer p = AbstractDungeon.player;
        Spice s = owned();
        if (p == null || s == null) {
            return;
        }
        int stacks = s.counter < 1 ? 1 : s.counter + 1;
        s.setCounter(stacks);
        if (stacks >= 2) {
            int loss = Math.min(HP_LOSS, Math.max(0, p.maxHealth - HP_FLOOR));
            if (loss > 0) {
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
        }
        s.flash();
    }

    /**
     * Adds progress to the recovery counter and heals to full HP once the
     * counter is full AND the max HP has already been pushed down to the
     * floor by Spice.
     */
    public static void addRecovery(int points) {
        if (loading) {
            return;
        }
        AbstractPlayer p = AbstractDungeon.player;
        Spice s = owned();
        if (p == null || s == null) {
            return;
        }
        recoveryCharge = Math.min(RECOVERY_NEED, recoveryCharge + Math.max(0, points));
        if (recoveryCharge >= RECOVERY_NEED && p.maxHealth <= HP_FLOOR) {
            healToFull(p);
            recoveryCharge = 0;
            s.flash();
        }
    }

    private static void healToFull(AbstractPlayer p) {
        int missing = p.maxHealth - p.currentHealth;
        if (missing > 0) {
            p.heal(missing);
        }
        if (p.currentHealth > p.maxHealth) {
            p.currentHealth = p.maxHealth;
        }
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
            return "消耗这瓶药水，获得 #b1 层 #y香料 。 NL 这瓶药水有 #b" + chance + "% 的概率额外给予一件随机遗物。";
        }
        return "Consume this potion to gain #b1 #ySpice stack. NL #b" + chance
                + "% chance to also gain a random relic.";
    }

    public static String description(Spice spice) {
        int stacks = Math.max(0, spice.counter);
        int effective = Math.max(1, stacks);
        int str = strength(effective);
        int dex = dexterity(effective);
        int charge = Math.min(RECOVERY_NEED, Math.max(0, recoveryCharge));
        StringBuilder sb = new StringBuilder();
        if (isChinese()) {
            sb.append("当前层数：#b").append(stacks).append(" 。 NL ");
            sb.append("恢复计数：#b").append(charge).append("/").append(RECOVERY_NEED).append(" 。 NL ");
            sb.append("每击败一名 #y精英 或 #yBoss ，以及每次获得香料时，层数 #b+1 。 NL ");
            sb.append("每回合开始时，获得 [E] 。 NL ");
            sb.append("每场战斗开始时，获得 #b").append(str).append(" 点 #y力量 和 #b").append(dex).append(" 点 #y敏捷 。 NL ");
            sb.append("#r第2层起：每层最大生命值 #r-").append(HP_LOSS).append("（最低 #b").append(HP_FLOOR).append("）。 NL ");
            sb.append("每场普通战斗胜利：恢复计数 #b+1 ；精英/Boss战斗：#b+2 。 NL ");
            sb.append("#b最大生命值已降至 ").append(HP_FLOOR).append(" 且恢复计数满 ").append(RECOVERY_NEED).append(" 时：回复全部生命并清零。 NL ");
            sb.append("在篝火处可以用药水交换香料。");
        } else {
            sb.append("Stacks: #b").append(stacks).append(" . NL ");
            sb.append("Recovery: #b").append(charge).append("/").append(RECOVERY_NEED).append(" . NL ");
            sb.append("Gain #b1 stack whenever you defeat an #yElite or #yBoss, and whenever you obtain Spice. NL ");
            sb.append("At the start of each turn, gain [E] . NL ");
            sb.append("At the start of each combat, gain #b").append(str).append(" #yStrength and #b").append(dex).append(" #yDexterity. NL ");
            sb.append("From stack 2: lose #r").append(HP_LOSS).append(" Max HP per stack (minimum #b").append(HP_FLOOR).append("). NL ");
            sb.append("Normal combat victory: recovery #b+1 ; elite/boss: #b+2 . NL ");
            sb.append("#bWhen Max HP already reached ").append(HP_FLOOR).append(" and recovery is full (").append(RECOVERY_NEED).append("): heal to full HP and reset. NL ");
            sb.append("Trade a potion for Spice at a campfire.");
        }
        return sb.toString();
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
