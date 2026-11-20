package natsu.translation;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;

@SpirePatch2(cls = "natsu.Natsu", method = "getSpireHeartText", paramtypez = {})
public final class ChineseHeartTextPatch {
    private ChineseHeartTextPatch() { }

    @SpirePostfixPatch
    public static String Postfix(String __result, natsu.Natsu __instance) {
        return ChineseText.enabled() ? "NL 它并不记得她。 NL 但她依然伸出手。" : __result;
    }
}
