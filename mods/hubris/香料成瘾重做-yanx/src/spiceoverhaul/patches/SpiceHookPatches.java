package spiceoverhaul.patches;

import com.evacipated.cardcrawl.mod.hubris.relics.Spice;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.potions.PotionSlot;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoomBoss;
import com.megacrit.cardcrawl.rooms.MonsterRoomElite;
import com.megacrit.cardcrawl.ui.campfire.AbstractCampfireOption;
import java.util.ArrayList;
import spiceoverhaul.SpiceData;
import spiceoverhaul.SpiceTradeOption;

/** Relic hooks shared by AbstractRelic: turn energy, elite victory stacks and campfire trade. */
public class SpiceHookPatches {

    @SpirePatch2(clz = AbstractRelic.class, method = "atTurnStart", paramtypez = {})
    public static class TurnEnergy {
        @SpirePostfixPatch
        public static void Postfix(AbstractRelic __instance) {
            if (__instance instanceof Spice
                    && __instance.counter >= 1
                    && AbstractDungeon.player != null) {
                AbstractDungeon.actionManager.addToBottom(new GainEnergyAction(1));
            }
        }
    }

    @SpirePatch2(clz = AbstractRelic.class, method = "onVictory", paramtypez = {})
    public static class Victory {
        @SpirePostfixPatch
        public static void Postfix(AbstractRelic __instance) {
            if (!(__instance instanceof Spice) || SpiceData.loading) {
                return;
            }
            AbstractRoom room = AbstractDungeon.getCurrRoom();
            boolean hard = room instanceof MonsterRoomElite || room instanceof MonsterRoomBoss;
            if (hard) {
                SpiceData.gainLayer();
            }
            SpiceData.addRecovery(hard ? 2 : 1);
        }
    }

    @SpirePatch2(clz = AbstractRelic.class, method = "addCampfireOption", paramtypez = {ArrayList.class})
    public static class Campfire {
        @SpirePostfixPatch
        @SuppressWarnings("unchecked")
        public static void Postfix(AbstractRelic __instance, Object[] __args) {
            if (!(__instance instanceof Spice) || __args == null || __args.length < 1) {
                return;
            }
            if (!(__args[0] instanceof ArrayList)) {
                return;
            }
            ArrayList<AbstractCampfireOption> options = (ArrayList<AbstractCampfireOption>) __args[0];
            AbstractPlayer p = AbstractDungeon.player;
            if (p == null) {
                return;
            }
            for (int i = 0; i < p.potions.size(); i++) {
                AbstractPotion potion = p.potions.get(i);
                if (potion == null || potion instanceof PotionSlot) {
                    continue;
                }
                options.add(new SpiceTradeOption(potion, i));
            }
        }
    }
}
