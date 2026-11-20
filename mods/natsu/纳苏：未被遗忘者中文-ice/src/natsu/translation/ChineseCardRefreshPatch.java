package natsu.translation;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;

@SpirePatch2(cls = "natsu.NatsuCard", method = "refreshDescription", paramtypez = {})
public final class ChineseCardRefreshPatch {
    private ChineseCardRefreshPatch() { }

    @SpirePrefixPatch
    public static void Prefix(natsu.NatsuCard __instance) {
        if (!ChineseText.enabled() || CardCrawlGame.languagePack == null) return;
        String id = __instance.cardID.substring("natsu:".length());
        String name = ChineseText.cardName(id);
        CardStrings strings = CardCrawlGame.languagePack.getCardStrings(__instance.cardID);
        if (strings != null) ChineseCardDescriptions.apply(id, strings);
        if (name != null) {
            __instance.name = name + (__instance.upgraded ? "+" : "");
            __instance.originalName = name;
        }
    }
}
