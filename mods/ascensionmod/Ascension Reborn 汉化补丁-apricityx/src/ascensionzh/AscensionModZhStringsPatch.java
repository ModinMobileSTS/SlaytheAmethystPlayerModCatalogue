package ascensionzh;

import com.badlogic.gdx.Gdx;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.core.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.technicalpi.AscensionMod;

/**
 * Ascension Reborn ships a localization/zhs/ folder that is only partly translated: the custom
 * ascension screen buttons, the short ascension level descriptions and the relic texts are still
 * English.
 *
 * AscensionMod.receiveEditStrings() asks AscensionMod.getLocalizationPath() which folder to read its
 * string files from, and then loads them itself. This patch points that folder at the complete
 * Simplified Chinese files bundled inside this patch mod while the game runs in Chinese, so the
 * parent mod keeps doing the loading and no string is ever registered twice.
 *
 * Any other language, or a missing translation file, falls through to the mod's original folder.
 */
@SpirePatch2(clz = AscensionMod.class, method = "getLocalizationPath")
public final class AscensionModZhStringsPatch {

    private static final Logger LOGGER = LogManager.getLogger(AscensionModZhStringsPatch.class.getName());

    /** Folder inside this patch mod holding the Simplified Chinese text files. */
    private static final String ZHS_DIR = "ascensionzh/localization/zhs/";

    /** Sanity check: without this file nothing is redirected. */
    private static final String GUARD_FILE = "AscensionDesc.json";

    private AscensionModZhStringsPatch() {
    }

    @SpirePrefixPatch
    public static SpireReturn<String> Prefix() {
        if (Settings.language != Settings.GameLanguage.ZHS) {
            return SpireReturn.Continue();
        }
        try {
            if (Gdx.files.internal(ZHS_DIR + GUARD_FILE).exists()) {
                LOGGER.info("[AscensionZh] reading Simplified Chinese text from " + ZHS_DIR);
                return SpireReturn.Return(ZHS_DIR);
            }
            LOGGER.warn("[AscensionZh] translated text files not found, keeping the mod's own files");
        } catch (Throwable t) {
            LOGGER.warn("[AscensionZh] could not check the translated text files, "
                    + "keeping the mod's own files", t);
        }
        return SpireReturn.Continue();
    }
}
