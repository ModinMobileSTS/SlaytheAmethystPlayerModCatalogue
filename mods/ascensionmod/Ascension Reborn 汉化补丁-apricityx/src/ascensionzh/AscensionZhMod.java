package ascensionzh;

import basemod.BaseMod;
import basemod.interfaces.PostInitializeSubscriber;
import com.badlogic.gdx.Gdx;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.RunModStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Two gaps are handled here, and only while the game runs in Simplified Chinese:
 *
 * 1. The parent mod ships a translated name for its own Custom Mode entry
 *    ("AscensionMinus:Blightless", the "Blightless" toggle) but never passes that file to BaseMod,
 *    so the game logged "RunModStrings: AscensionMinus:Blightless not found" and showed a
 *    placeholder. We register it when no real name is present yet.
 *
 * 2. Safety net for the redirect done by {@link AscensionModZhStringsPatch}: if the parent mod ended
 *    up loading its own English text anyway, the affected files are loaded from this patch mod
 *    instead. Doing this in receivePostInitialize means the language pack already exists, so we can
 *    check what was actually loaded before touching anything.
 */
@SpireInitializer
public class AscensionZhMod implements PostInitializeSubscriber {

    private static final Logger LOGGER = LogManager.getLogger(AscensionZhMod.class.getName());

    private static final String ZHS_DIR = "ascensionzh/localization/zhs/";

    private static final String BLIGHTLESS_ID = "AscensionMinus:Blightless";
    private static final String ENGLISH_NAME = "Blightless";
    private static final String CUSTOM_MOD_DESC_FILE = "CustomModDescription.json";

    public AscensionZhMod() {
        BaseMod.subscribe(this);
    }

    public static void initialize() {
        new AscensionZhMod();
    }

    @Override
    public void receivePostInitialize() {
        if (Settings.language != Settings.GameLanguage.ZHS) {
            return;
        }
        if (!hasChineseRunModName()) {
            loadStrings(RunModStrings.class, CUSTOM_MOD_DESC_FILE);
        }
        if (!hasChineseText("AscModScreenUiStrings")) {
            loadStrings(UIStrings.class, "AscModScreenStrings.json");
        }
        if (!hasChineseText("RenameBoxStrings")) {
            loadStrings(UIStrings.class, "RenameBoxStrings.json");
        }
        if (!hasChineseText("CustomAscensionScreen") && !hasChineseText("CharSelectStrings")) {
            loadStrings(UIStrings.class, "AscensionUiStrings.json");
        }
        if (!hasChineseText("ShortAscensionModeDescriptions")) {
            loadStrings(UIStrings.class, "AscensionDesc.json");
        }
        if (!hasChineseText("ShortAscensionMinusDescriptions")) {
            loadStrings(UIStrings.class, "AscensionMinusDesc.json");
        }
    }

    private static void loadStrings(Class<?> stringType, String fileName) {
        try {
            String json = Gdx.files.internal(ZHS_DIR + fileName).readString("UTF-8");
            BaseMod.loadCustomStrings(stringType, json);
            LOGGER.info("[AscensionZh] loaded translated text from " + fileName);
        } catch (Throwable t) {
            LOGGER.warn("[AscensionZh] could not load translated text from " + fileName, t);
        }
    }

    /** True when the Custom Mode entry already has a translated name. */
    private static boolean hasChineseRunModName() {
        RunModStrings strings;
        try {
            strings = CardCrawlGame.languagePack.getRunModString(BLIGHTLESS_ID);
        } catch (Throwable t) {
            return false;
        }
        if (strings == null) {
            return false;
        }
        return isChinese(strings.NAME);
    }

    /** True when the game already holds Chinese text for this UI string id. */
    private static boolean hasChineseText(String id) {
        UIStrings strings;
        try {
            strings = CardCrawlGame.languagePack.getUIString(id);
        } catch (Throwable t) {
            return false;
        }
        if (strings == null || strings.TEXT == null || strings.TEXT.length == 0) {
            return false;
        }
        String text = strings.TEXT[0];
        if (text != null && text.indexOf(id) >= 0) {
            return false;
        }
        return isChinese(text);
    }

    private static boolean isChinese(String text) {
        if (text == null || ENGLISH_NAME.equals(text)) {
            return false;
        }
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (c >= '\u4e00' && c <= '\u9fff') {
                return true;
            }
        }
        return false;
    }
}
