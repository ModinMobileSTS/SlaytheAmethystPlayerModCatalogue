package potionfix;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;

/**
 * 原版 ChaosPower 只有“回合结束减层”，本身没有任何实际效果，混乱药水等于白喝。
 * 这里在每次回合结束减层之前让该敌人重新决策一次，使它的意图变得不可预测。
 */
@SpirePatch2(
    cls = "potionmod.powers.ChaosPower",
    method = "atEndOfRound",
    paramtypez = {}
)
public final class ChaosPowerIntentPatch {
    private ChaosPowerIntentPatch() {
    }

    @SpirePrefixPatch
    public static void Prefix(potionmod.powers.ChaosPower __instance) {
        if (__instance == null) {
            return;
        }
        ChaosReroll.reroll(__instance.owner);
    }
}
