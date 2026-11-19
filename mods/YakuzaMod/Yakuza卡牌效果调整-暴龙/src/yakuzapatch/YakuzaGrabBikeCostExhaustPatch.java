package yakuzapatch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;
import theYakuza.cards.UncommonSkills.YakuzaGrabBike;

/**
 * 「抓取单车」/「抓取单车+」：费用改为 1 费，并加上原版「消耗」词条（打出后进入消耗牌堆）。
 */
public final class YakuzaGrabBikeCostExhaustPatch {
    private YakuzaGrabBikeCostExhaustPatch() {
    }

    private static String withExhaustText(String desc) {
        if (desc.contains("消耗") || desc.contains("Exhaust")) {
            return desc;
        }
        return desc + (Settings.language == Settings.GameLanguage.ZHS ? " NL 消耗 。" : " NL Exhaust.");
    }

    @SpirePatch2(cls = "theYakuza.cards.UncommonSkills.YakuzaGrabBike", method = "<ctor>", paramtypez = {})
    public static final class CtorPatch {
        private CtorPatch() {
        }

        @SpirePostfixPatch
        public static void Postfix(YakuzaGrabBike __instance) {
            __instance.cost = 1;
            __instance.costForTurn = 1;
            __instance.exhaust = true;
            __instance.rawDescription = withExhaustText(__instance.rawDescription);
            __instance.initializeDescription();
        }
    }

    @SpirePatch2(cls = "theYakuza.cards.UncommonSkills.YakuzaGrabBike", method = "upgrade", paramtypez = {})
    public static final class UpgradePatch {
        private UpgradePatch() {
        }

        @SpirePostfixPatch
        public static void Postfix(YakuzaGrabBike __instance) {
            __instance.exhaust = true;
            __instance.rawDescription = withExhaustText(__instance.rawDescription);
            __instance.initializeDescription();
        }
    }
}
