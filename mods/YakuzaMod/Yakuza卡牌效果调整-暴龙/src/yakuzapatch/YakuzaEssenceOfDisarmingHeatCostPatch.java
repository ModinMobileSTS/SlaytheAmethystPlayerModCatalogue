package yakuzapatch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import theYakuza.cards.UncommonAttacks.YakuzaEssenceOfDisarming;

/**
 * 「缴械的精髓」/「缴械的精髓+」：消耗热血值 2 点改为 1 点，并同步卡牌描述。
 */
public final class YakuzaEssenceOfDisarmingHeatCostPatch {
    private YakuzaEssenceOfDisarmingHeatCostPatch() {
    }

    @SpirePatch2(cls = "theYakuza.cards.UncommonAttacks.YakuzaEssenceOfDisarming", method = "<ctor>", paramtypez = {})
    public static final class CtorPatch {
        private CtorPatch() {
        }

        @SpirePostfixPatch
        public static void Postfix(YakuzaEssenceOfDisarming __instance) {
            __instance.baseHeatCost = 1;
            __instance.heatCost = 1;
            String desc = __instance.rawDescription;
            desc = desc.replace("使用2层", "使用1层");
            desc = desc.replace("Spend 2 theyakuza:Heat", "Spend 1 theyakuza:Heat");
            __instance.rawDescription = desc;
            __instance.initializeDescription();
        }
    }
}
