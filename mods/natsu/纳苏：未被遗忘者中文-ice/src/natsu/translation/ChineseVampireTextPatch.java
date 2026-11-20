package natsu.translation;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;

@SpirePatch2(cls = "natsu.Natsu", method = "getVampireText", paramtypez = {})
public final class ChineseVampireTextPatch {
    private ChineseVampireTextPatch() { }

    @SpirePostfixPatch
    public static String Postfix(String __result, natsu.Natsu __instance) {
        return ChineseText.enabled() ? "那些身影从你身旁移开了目光。接着，其中一人停下脚步。 NL ~你心中~ ~仍有~ ~一丝~ ~人性。~" : __result;
    }
}
