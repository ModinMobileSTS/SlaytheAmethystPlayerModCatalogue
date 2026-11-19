package yakuzapatch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;
import theYakuza.cards.UncommonPowers.YakuzaEssenceOfRecycling;

/**
 * 「废品回收」/「废品回收+」：卡牌描述加入「一回合一次」；
 * 「废品回收+」费用改为 0 费，并移除「固有」效果与描述中的「固有」。
 */
public final class YakuzaEssenceOfRecyclingUpgradePatch {
    private YakuzaEssenceOfRecyclingUpgradePatch() {
    }

    private static String withOncePerTurn(String desc) {
        if (desc.contains("一回合一次") || desc.contains("Once per turn")) {
            return desc;
        }
        if (Settings.language == Settings.GameLanguage.ZHS) {
            return desc.replace("道具损坏时", "一回合一次，道具损坏时");
        }
        return desc.replace("When an item breaks", "Once per turn, when an item breaks");
    }

    @SpirePatch2(cls = "theYakuza.cards.UncommonPowers.YakuzaEssenceOfRecycling", method = "<ctor>", paramtypez = {})
    public static final class CtorPatch {
        private CtorPatch() {
        }

        @SpirePostfixPatch
        public static void Postfix(YakuzaEssenceOfRecycling __instance) {
            __instance.rawDescription = withOncePerTurn(__instance.rawDescription);
            __instance.initializeDescription();
        }
    }

    @SpirePatch2(cls = "theYakuza.cards.UncommonPowers.YakuzaEssenceOfRecycling", method = "upgrade", paramtypez = {})
    public static final class UpgradePatch {
        private UpgradePatch() {
        }

        @SpirePostfixPatch
        public static void Postfix(YakuzaEssenceOfRecycling __instance) {
            __instance.cost = 0;
            __instance.costForTurn = 0;
            __instance.isInnate = false;
            String desc = __instance.rawDescription;
            desc = desc.replace(" 固有 。 NL ", "");
            desc = desc.replace("Innate. NL ", "");
            __instance.rawDescription = withOncePerTurn(desc);
            __instance.initializeDescription();
        }
    }
}
