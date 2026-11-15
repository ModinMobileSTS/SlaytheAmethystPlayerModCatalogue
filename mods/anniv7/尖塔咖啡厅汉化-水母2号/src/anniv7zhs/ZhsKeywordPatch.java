package anniv7zhs;

import basemod.BaseMod;
import com.badlogic.gdx.Gdx;
import com.evacipated.cardcrawl.mod.stslib.Keyword;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.google.gson.Gson;
import com.megacrit.cardcrawl.core.Settings;
import java.nio.charset.StandardCharsets;
import spireCafe.Anniv7Mod;

/**
 * Registers the Simplified Chinese version of the mod's custom keyword.
 */
@SpirePatch2(
    cls = "spireCafe.Anniv7Mod",
    method = "receiveEditKeywords",
    paramtypez = {}
)
public final class ZhsKeywordPatch {

    private ZhsKeywordPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(spireCafe.Anniv7Mod __instance) {
        if (Settings.language != Settings.GameLanguage.ZHS) {
            return;
        }
        try {
            String path = ZhsLoader.ROOT + "Keywordstrings.json";
            String json = Gdx.files.internal(path).readString(StandardCharsets.UTF_8.name());
            Keyword[] keywords = new Gson().fromJson(json, Keyword[].class);
            if (keywords == null) {
                return;
            }
            for (Keyword keyword : keywords) {
                if (keyword == null || keyword.PROPER_NAME == null || keyword.NAMES == null) {
                    continue;
                }
                BaseMod.addKeyword(Anniv7Mod.modID, keyword.PROPER_NAME, keyword.NAMES, keyword.DESCRIPTION);
                if (keyword.ID != null && !keyword.ID.isEmpty()) {
                    Anniv7Mod.keywords.put(keyword.ID, keyword);
                }
            }
        } catch (Throwable ignored) {
            // Keep the English keyword if the Chinese file cannot be read.
        }
    }
}
