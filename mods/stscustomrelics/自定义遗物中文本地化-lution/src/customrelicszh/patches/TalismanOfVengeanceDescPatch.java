package customrelicszh.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import customrelicszh.ZhText;

@SpirePatch2(
    cls = "com.example.relicsmod.relics.TalismanOfVengeance",
    method = "getUpdatedDescription",
    paramtypez = {}
)
public final class TalismanOfVengeanceDescPatch {
    private TalismanOfVengeanceDescPatch() {
    }

    @SpirePrefixPatch
    public static SpireReturn Prefix() {
        if (!ZhText.isChinese()) {
            return SpireReturn.Continue();
        }
        return SpireReturn.Return(ZhText.TALISMAN_OF_VENGEANCE_DESC);
    }
}
