package natsu.translation;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;

@SpirePatch2(cls = "natsu.Natsu", method = "getLocalizedCharacterName", paramtypez = {})
public final class ChineseLocalizedCharacterNamePatch {
    private ChineseLocalizedCharacterNamePatch() { }

    @SpirePostfixPatch
    public static String Postfix(String __result, natsu.Natsu __instance) {
        return ChineseText.enabled() ? "未被遗忘者" : __result;
    }
}
