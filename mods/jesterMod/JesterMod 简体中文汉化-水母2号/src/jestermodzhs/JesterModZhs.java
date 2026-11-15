package jestermodzhs;

import basemod.BaseMod;
import basemod.interfaces.EditKeywordsSubscriber;
import basemod.interfaces.EditStringsSubscriber;
import com.evacipated.cardcrawl.mod.stslib.Keyword;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.google.gson.Gson;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.localization.CharacterStrings;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.localization.OrbStrings;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.localization.RelicStrings;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Simplified Chinese localization for the Jester Mod (jesterMod).
 *
 * The parent mod only whitelists eng/fra/rus string files and falls back to English for every other
 * language, so Chinese players see English card, relic and power text. This companion mod ships its
 * own zhs text files and loads them on top of the strings the parent already registered, replacing
 * the Jester Mod text with Chinese whenever the game language is Chinese.
 *
 * Card text is formatted the way the game's Chinese renderer expects it: keywords, "[E]" and
 * coloured words each form their own space delimited token, and punctuation that follows such a
 * token stays a separate token. That is exactly how the official Chinese card text is written, and
 * it is what makes the game highlight keywords, show their tooltips, draw energy icons and colour
 * individual words instead of leaving markup visible in the text.
 */
@SpireInitializer
public class JesterModZhs implements EditStringsSubscriber, EditKeywordsSubscriber {

    public static final Logger logger = LogManager.getLogger(JesterModZhs.class.getName());

    /** Root of this mod's localization resources inside this mod's own jar. */
    private static final String RESOURCE_ROOT = "/jesterModZhsResources/localization/zhs/";

    /** Keyword prefix used by the parent mod: BaseMod.addKeyword(modID.toLowerCase(), ...). */
    private static final String KEYWORD_MOD_ID = "jestermod";

    public static void initialize() {
        new JesterModZhs();
    }

    public JesterModZhs() {
        BaseMod.subscribe(this);
    }

    @Override
    public void receiveEditStrings() {
        if (!isChinese()) {
            return;
        }
        logger.info("[JesterModZhs] Loading Simplified Chinese strings for the Jester Mod.");
        int loaded = 0;
        int failed = 0;
        if (loadStrings(CardStrings.class, "JesterMod-Card-Strings.json")) {
            loaded++;
        } else {
            failed++;
        }
        if (loadStrings(PowerStrings.class, "JesterMod-Power-Strings.json")) {
            loaded++;
        } else {
            failed++;
        }
        if (loadStrings(RelicStrings.class, "JesterMod-Relic-Strings.json")) {
            loaded++;
        } else {
            failed++;
        }
        if (loadStrings(EventStrings.class, "JesterMod-Event-Strings.json")) {
            loaded++;
        } else {
            failed++;
        }
        if (loadStrings(PotionStrings.class, "JesterMod-Potion-Strings.json")) {
            loaded++;
        } else {
            failed++;
        }
        if (loadStrings(CharacterStrings.class, "JesterMod-Character-Strings.json")) {
            loaded++;
        } else {
            failed++;
        }
        if (loadStrings(OrbStrings.class, "JesterMod-Orb-Strings.json")) {
            loaded++;
        } else {
            failed++;
        }
        logger.info("[JesterModZhs] Strings finished: loaded=" + loaded + " failed=" + failed);
    }

    @Override
    public void receiveEditKeywords() {
        if (!isChinese()) {
            return;
        }
        String json = read("JesterMod-Keyword-Strings.json");
        if (json == null) {
            return;
        }
        Keyword[] keywords;
        try {
            keywords = new Gson().fromJson(json, Keyword[].class);
        } catch (RuntimeException e) {
            logger.error("[JesterModZhs] Could not parse JesterMod-Keyword-Strings.json", e);
            return;
        }
        if (keywords == null) {
            return;
        }
        for (Keyword keyword : keywords) {
            BaseMod.addKeyword(KEYWORD_MOD_ID, keyword.PROPER_NAME, keyword.NAMES, keyword.DESCRIPTION);
        }
        logger.info("[JesterModZhs] Registered " + keywords.length + " Chinese keywords.");
    }

    /** Chinese language setting; Traditional Chinese reuses the Simplified text. */
    private static boolean isChinese() {
        return Settings.language == Settings.GameLanguage.ZHS
                || Settings.language == Settings.GameLanguage.ZHT;
    }

    /**
     * Loads one string file. Returns false when the file is missing or invalid, so a single broken
     * file can never take the whole mod (or the game) down.
     */
    private static boolean loadStrings(Class<?> type, String fileName) {
        String json = read(fileName);
        if (json == null) {
            return false;
        }
        try {
            BaseMod.loadCustomStrings(type, json);
            logger.info("[JesterModZhs] Loaded " + fileName);
            return true;
        } catch (Throwable t) {
            logger.error("[JesterModZhs] Could not load " + fileName, t);
            return false;
        }
    }

    /** Reads one of this mod's own localization files. */
    private static String read(String fileName) {
        InputStream stream = JesterModZhs.class.getResourceAsStream(RESOURCE_ROOT + fileName);
        if (stream == null) {
            logger.error("[JesterModZhs] Missing localization resource: " + RESOURCE_ROOT + fileName);
            return null;
        }
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = stream.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("[JesterModZhs] Could not read " + fileName, e);
            return null;
        } finally {
            try {
                stream.close();
            } catch (IOException ignored) {
                // nothing useful to do when a close fails
            }
        }
    }
}
