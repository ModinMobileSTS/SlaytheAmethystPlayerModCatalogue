package patches;

import code.cards.AokumoRin.Ao_11_Moschoko;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpirePatch2(cls = "code.cards.AokumoRin.Ao_11_Moschoko", method = "applyPowers", paramtypez = {})
public final class ChocolateApplyTextPatch {
    private static final Pattern DETAILS = Pattern.compile("\\[(\\d+) 피해를 (\\d+)번 반복\\]");

    @SpirePostfixPatch
    public static void Postfix(Ao_11_Moschoko __instance) {
        translate(__instance);
    }

    static void translate(Ao_11_Moschoko card) {
        if (Settings.language != Settings.GameLanguage.ZHS || card.rawDescription == null) return;
        Matcher match = DETAILS.matcher(card.rawDescription);
        if (match.find()) {
            String replacement = "[造成 " + match.group(1) + " 点伤害，重复 " + match.group(2) + " 次]";
            card.rawDescription = match.replaceFirst(Matcher.quoteReplacement(replacement));
            card.initializeDescription();
        }
    }
}
