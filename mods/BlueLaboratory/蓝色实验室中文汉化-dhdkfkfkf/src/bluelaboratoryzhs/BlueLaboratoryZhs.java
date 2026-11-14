package bluelaboratoryzhs;

import basemod.BaseMod;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.localization.PowerStrings;

import java.lang.reflect.Field;

/**
 * The parent mod ships zhs localization files but DefaultMod.receiveEditStrings() only ever
 * loads the eng ones, so every potion and power shows English text even when the game
 * language is Chinese.
 *
 * This postfix runs immediately after the parent loads its own strings, still inside BaseMod's
 * edit-strings phase and before receiveEditCharacters registers the potions (which is when each
 * potion caches its NAME / DESCRIPTIONS static fields).
 */
@SpirePatch2(
    cls = "blueLaboratory.DefaultMod",
    method = "receiveEditStrings",
    paramtypez = {}
)
public final class BlueLaboratoryZhs {
    private static final String DIR = "blueLaboratoryZhsResources/localization/zhs/";

    private BlueLaboratoryZhs() {
    }

    @SpirePostfixPatch
    public static void Postfix(blueLaboratory.DefaultMod __instance) {
        if (!isChinese()) {
            return;
        }
        BaseMod.loadCustomStringsFile(PowerStrings.class, DIR + "BlueLaboratory-Power-Strings.json");
        BaseMod.loadCustomStringsFile(PotionStrings.class, DIR + "BlueLaboratory-Potion-Strings.json");
    }

    private static boolean isChinese() {
        try {
            Field languageField = Settings.class.getDeclaredField("language");
            languageField.setAccessible(true);
            Object language = languageField.get(null);
            if (language instanceof Enum) {
                String name = ((Enum<?>) language).name();
                return "ZHS".equals(name) || "ZHT".equals(name);
            }
        } catch (Throwable ignored) {
            // fall through to the localized-text probe below
        }
        try {
            return "打击".equals(CardCrawlGame.languagePack.getCardStrings("Strike_R").NAME);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
