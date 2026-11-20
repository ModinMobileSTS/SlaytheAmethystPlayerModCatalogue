package natsu.translation;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;

@SpirePatch2(cls = "natsu.Natsu", method = "getTitle", paramtypez = {com.megacrit.cardcrawl.characters.AbstractPlayer.PlayerClass.class})
public final class ChineseCharacterTitlePatch {
    private ChineseCharacterTitlePatch() { }

    @SpirePostfixPatch
    public static String Postfix(String __result, natsu.Natsu __instance, Object[] __args) {
        return ChineseText.enabled() ? "未被遗忘者" : __result;
    }
}
