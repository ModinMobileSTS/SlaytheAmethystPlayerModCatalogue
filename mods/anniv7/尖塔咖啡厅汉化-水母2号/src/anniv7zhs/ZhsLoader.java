package anniv7zhs;

import basemod.BaseMod;
import com.megacrit.cardcrawl.localization.BlightStrings;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.localization.CharacterStrings;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.localization.OrbStrings;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.localization.RelicStrings;
import com.megacrit.cardcrawl.localization.StanceStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
import spireCafe.util.cutsceneStrings.LocalizedCutsceneStrings;

/**
 * Loads the Simplified Chinese localization shipped by this patch mod.
 *
 * <p>Spire Cafe looks for its strings under {@code anniv7Resources/localization/<lang>/},
 * which resolves inside this patch mod's jar through the game's internal file handle.
 * The patch calls the loader explicitly right after the mod's own string loading so the
 * Chinese entries overwrite the English ones before cards/relics are constructed.</p>
 */
public final class ZhsLoader {

    public static final String ROOT = "anniv7Resources/localization/zhs/";

    private static final String[] INTERACTABLES = {
        "BookshelfAttraction",
        "ClericPatron",
        "CouncilOfGhostsPatron",
        "DandadanPatron",
        "DiscordStatueAttraction",
        "DrinkbotBartender",
        "EnchanterMerchant",
        "FleaMerchant",
        "GridDraftMerchant",
        "JukeboxAttraction",
        "KoishiPatron",
        "LoneTableAttraction",
        "LooterPatron",
        "MakeupTableAttraction",
        "MissingnoPatron",
        "PackmasterMerchant",
        "PackmistressPatron",
        "PeddlerMerchant",
        "PowerelicPatron",
        "PunchingBagAttraction",
        "PurpleTearPatron",
        "RedLousePatron",
        "RestingSlayerPatron",
        "SecretShopMerchant",
        "ShrineOfOrderAttraction",
        "SnackmasterMerchant",
        "SneckoBartender",
        "SpikeSlimePatron",
        "SpiomesManifestationPatron",
        "StarbucksBartender",
        "TheTransientPatron",
        "TrashKingPatron",
        "VampistaBartender"
    };

    private ZhsLoader() {
    }

    public static void loadAll() {
        loadStrings(ROOT);
        for (String interactable : INTERACTABLES) {
            loadStrings(ROOT + interactable + "/");
        }
    }

    private static void loadStrings(String dir) {
        loadType(CharacterStrings.class, dir + "Characterstrings.json");
        loadType(CardStrings.class, dir + "Cardstrings.json");
        loadType(RelicStrings.class, dir + "Relicstrings.json");
        loadType(PowerStrings.class, dir + "Powerstrings.json");
        loadType(UIStrings.class, dir + "UIstrings.json");
        loadType(StanceStrings.class, dir + "Stancestrings.json");
        loadType(OrbStrings.class, dir + "Orbstrings.json");
        loadType(PotionStrings.class, dir + "Potionstrings.json");
        loadType(MonsterStrings.class, dir + "Monsterstrings.json");
        loadType(BlightStrings.class, dir + "Blightstrings.json");
        loadCutscene(dir + "Cutscenestrings.json");
    }

    private static void loadType(Class<?> stringType, String path) {
        try {
            BaseMod.loadCustomStringsFile(stringType, path);
        } catch (Throwable ignored) {
            // That interactable simply does not ship this string type.
        }
    }

    private static void loadCutscene(String path) {
        try {
            LocalizedCutsceneStrings.loadCutsceneStringsFile(path);
        } catch (Throwable ignored) {
            // That interactable simply does not ship cutscene strings.
        }
    }
}
