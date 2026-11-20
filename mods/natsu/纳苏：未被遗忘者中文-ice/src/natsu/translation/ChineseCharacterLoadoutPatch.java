package natsu.translation;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.screens.CharSelectInfo;

@SpirePatch2(cls = "natsu.Natsu", method = "getLoadout", paramtypez = {})
public final class ChineseCharacterLoadoutPatch {
    private ChineseCharacterLoadoutPatch() { }

    @SpirePostfixPatch
    public static CharSelectInfo Postfix(CharSelectInfo __result, natsu.Natsu __instance) {
        if (ChineseText.enabled() && __result != null) {
            __result.name = "未被遗忘者";
            __result.flavorText = "被宇宙乌贼所救，却被世界遗忘。 NL 她带着海洋，也带着渴望有人记起的思念。 NL 遗忘手牌，在气泡中寻求庇护，并将怀旧化为羁绊。";
        }
        return __result;
    }
}
