package patches;

import code.cards.HanakoNana.Na_01_Love;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;

@SpirePatch2(cls = "code.cards.HanakoNana.Na_01_Love", method = "updateDescription", paramtypez = {})
public final class LoveCardDescriptionPatch {
    @SpirePostfixPatch
    public static void Postfix(Na_01_Love __instance) {
        if (Settings.language != Settings.GameLanguage.ZHS || __instance.rawDescription == null) return;
        String text = __instance.rawDescription
                .replace("[마]지막 [탄]환", "stellive:魔弹")
                .replace("첫발은 공포탄!", "第一发是 stellive:空包弹 ！")
                .replace("소멸", "消耗");
        if (!text.equals(__instance.rawDescription)) {
            __instance.rawDescription = text;
            __instance.initializeDescription();
        }
    }
}
