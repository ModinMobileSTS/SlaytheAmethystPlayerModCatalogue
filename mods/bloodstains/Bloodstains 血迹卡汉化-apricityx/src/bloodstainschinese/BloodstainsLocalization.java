package bloodstainschinese;

import basemod.ReflectionHacks;
import com.badlogic.gdx.Gdx;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.localization.LocalizedStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
import java.lang.reflect.Type;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Chinese localization for the content added by the Bloodstains mod (a homage to the Dark Souls
 * series): the "Bloodstain" card, the map node tooltip and the mod settings label.
 *
 * <p>The parent mod only ships English strings and loads them from
 * {@code Bloodstains.receiveEditStrings()}. This patch postfixes exactly that method, so the Chinese
 * text is written into {@link LocalizedStrings} after the English one and therefore wins.</p>
 *
 * <p>The entries are pushed straight into the {@code cards} / {@code ui} maps under their fully
 * qualified {@code bloodstains:} keys instead of going through
 * {@code BaseMod.loadCustomStringsFile}: that helper prefixes keys with the <em>calling</em> mod's id,
 * so it would store {@code <this patch>:bloodstains:Bloodstain} and never touch the parent's entry.</p>
 *
 * <p>Only one language pack is applied - {@code zhs} for Simplified Chinese, {@code zht} for
 * Traditional Chinese, nothing for any other language - so the two files can never overwrite each
 * other. All Chinese text lives in the JSON resources; this file stays ASCII only.</p>
 */
@SpirePatch2(
    cls = "bloodstains.Bloodstains",
    method = "receiveEditStrings",
    paramtypez = {}
)
public final class BloodstainsLocalization {

    private static final Logger LOGGER = LogManager.getLogger(BloodstainsLocalization.class.getName());

    /** Packaged tree first, then the layout of packagers that keep a leading {@code resources/}. */
    private static final String[] RESOURCE_ROOTS = {
        "bloodstainszh/localization/",
        "resources/bloodstainszh/localization/"
    };

    private static final String CARDS_FILE = "/cards.json";
    private static final String UI_FILE = "/ui.json";

    private static final String CARDS_FIELD = "cards";
    private static final String UI_FIELD = "ui";

    private static final String CARD_ID = "bloodstains:Bloodstain";
    private static final String CONFIG_ID = "bloodstains:ModConfigs";

    private BloodstainsLocalization() {
    }

    @SpirePostfixPatch
    public static void Postfix(bloodstains.Bloodstains __instance) {
        String folder = chineseFolder();
        if (folder == null) {
            LOGGER.info("Bloodstains localization: game language is not Chinese, keeping the parent English text.");
            return;
        }
        if (apply(folder)) {
            LOGGER.info("Bloodstains localization: " + folder + " text applied.");
        } else {
            LOGGER.warn("Bloodstains localization: could not apply the " + folder + " text.");
        }
    }

    /** @return the localization folder matching the active game language, or null when not Chinese. */
    private static String chineseFolder() {
        Settings.GameLanguage language = Settings.language;
        if (language == null) {
            return null;
        }
        if (language == Settings.GameLanguage.ZHS) {
            return "zhs";
        }
        if (language == Settings.GameLanguage.ZHT) {
            return "zht";
        }
        return null;
    }

    private static boolean apply(String folder) {
        Gson gson = new Gson();
        Type cardsType = new TypeToken<Map<String, CardStrings>>() {}.getType();
        Type uiType = new TypeToken<Map<String, UIStrings>>() {}.getType();

        for (String root : RESOURCE_ROOTS) {
            String cardsPath = root + folder + CARDS_FILE;
            String uiPath = root + folder + UI_FILE;
            if (!Gdx.files.internal(cardsPath).exists() || !Gdx.files.internal(uiPath).exists()) {
                continue;
            }
            try {
                Map<String, CardStrings> cards = gson.fromJson(read(cardsPath), cardsType);
                Map<String, UIStrings> ui = gson.fromJson(read(uiPath), uiType);
                if (cards == null || ui == null) {
                    continue;
                }
                Map<String, CardStrings> liveCards =
                    ReflectionHacks.getPrivateStatic(LocalizedStrings.class, CARDS_FIELD);
                Map<String, UIStrings> liveUi =
                    ReflectionHacks.getPrivateStatic(LocalizedStrings.class, UI_FIELD);
                if (liveCards == null || liveUi == null) {
                    LOGGER.warn("Bloodstains localization: the game's localization maps are not ready yet.");
                    return false;
                }
                liveCards.putAll(cards);
                liveUi.putAll(ui);
                ReflectionHacks.setPrivateStatic(LocalizedStrings.class, CARDS_FIELD, liveCards);
                ReflectionHacks.setPrivateStatic(LocalizedStrings.class, UI_FIELD, liveUi);

                CardStrings card = liveCards.get(CARD_ID);
                UIStrings tip = liveUi.get(CARD_ID);
                UIStrings config = liveUi.get(CONFIG_ID);
                if (card == null || card.NAME == null || card.DESCRIPTION == null
                    || tip == null || tip.TEXT == null || tip.TEXT.length < 3
                    || config == null || config.TEXT == null || config.TEXT.length < 2) {
                    LOGGER.warn("Bloodstains localization: " + cardsPath + " is missing an expected entry.");
                    return false;
                }
                LOGGER.info("Bloodstains localization: loaded " + cardsPath + " and " + uiPath);
                return true;
            } catch (Throwable t) {
                LOGGER.warn("Bloodstains localization: failed to read " + cardsPath, t);
            }
        }
        return false;
    }

    private static String read(String path) {
        return Gdx.files.internal(path).readString("UTF-8");
    }
}
