package omnipotencepatch;

import basemod.BaseMod;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.localization.CharacterStrings;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.localization.OrbStrings;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.localization.RelicStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Forces the Chinese (Simplified) localization to be loaded right after the parent
 * mod Omnipotence finishes loading its own strings.
 *
 * The parent constructs its cards in CardLibrary.initialize() which happens AFTER
 * publishEditStrings(), so overriding the strings here makes the cards' NAME and
 * DESCRIPTION use the Chinese text at construction time. This works regardless of
 * the current game language setting, because the strings are always re-applied.
 */
@SpirePatch2(
    cls = "omnipotence.Omnipotence",
    method = "receiveEditStrings",
    paramtypez = {}
)
public final class OmnipotenceChineseStringsPatch {
    private static final String BASE = "/omnipotencepatch/localization/zhs/";

    private OmnipotenceChineseStringsPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(omnipotence.Omnipotence __instance) {
        load(CardStrings.class, "CardStrings.json");
        load(CharacterStrings.class, "CharacterStrings.json");
        load(EventStrings.class, "EventStrings.json");
        load(OrbStrings.class, "OrbStrings.json");
        load(PotionStrings.class, "PotionStrings.json");
        load(PowerStrings.class, "PowerStrings.json");
        load(RelicStrings.class, "RelicStrings.json");
        load(UIStrings.class, "UIStrings.json");
    }

    private static void load(Class<?> type, String file) {
        try {
            String json = read(BASE + file);
            if (json != null && !json.isEmpty()) {
                BaseMod.loadCustomStrings(type, json);
            }
        } catch (Throwable ignored) {
        }
    }

    private static String read(String path) {
        InputStream is = OmnipotenceChineseStringsPatch.class.getResourceAsStream(path);
        if (is == null) {
            return null;
        }
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) {
                bos.write(buf, 0, n);
            }
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        } finally {
            try {
                is.close();
            } catch (Exception ignored) {
            }
        }
    }
}
