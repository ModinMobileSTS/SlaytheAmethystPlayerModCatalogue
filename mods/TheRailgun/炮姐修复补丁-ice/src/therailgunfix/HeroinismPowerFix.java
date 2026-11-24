package therailgunfix;

import TheRailgun.powers.HeroinismPower;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

@SpirePatch2(clz = HeroinismPower.class, method = "atStartOfTurn", paramtypez = {})
public class HeroinismPowerFix {
    private static final int ENERGY_CAP = 10;

    @SpirePrefixPatch
    public static SpireReturn atStartOfTurn(HeroinismPower __instance) {
        int gain = Math.min(__instance.amount, ENERGY_CAP);
        AbstractDungeon.actionManager.addToTop(new GainEnergyAction(gain));
        if (__instance.amount < ENERGY_CAP) {
            __instance.amount++;
            __instance.updateDescription();
        }
        return SpireReturn.Return();
    }

    @SpirePatch2(clz = HeroinismPower.class, method = "updateDescription", paramtypez = {})
    public static class Description {
        @SpirePrefixPatch
        public static SpireReturn update(HeroinismPower __instance) {
            int gain = Math.min(__instance.amount, ENERGY_CAP);
            if (Settings.language.toString().equalsIgnoreCase("ZHS")) {
                __instance.description = "每回合开始时获得 " + gain + " 点能量（上限 10），随后层数 +1。";
            } else {
                __instance.description = "At the start of each turn, gain " + gain + " Energy (max 10), then increase this by 1.";
            }
            return SpireReturn.Return();
        }
    }
}
