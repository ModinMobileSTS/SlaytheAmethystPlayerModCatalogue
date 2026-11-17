package wizardrobe;

import basemod.BaseMod;
import basemod.helpers.RelicType;
import basemod.interfaces.EditRelicsSubscriber;
import basemod.interfaces.EditStringsSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.RelicStrings;
import wizardrobe.relics.WizardRobe;

/**
 * Downfall add-on that registers the "Wizard's Robe" relic.
 *
 * The relic grants 3 stacks of Downfall's Mana power (Gremlin:Wiz) at the start of every
 * turn. The grant happens in WizardRobe#atTurnStart, i.e. the game's own relic turn-start
 * pass, so it fires on the first turn of a combat as well.
 */
@SpireInitializer
public class WizardRobeMod implements EditStringsSubscriber, EditRelicsSubscriber {

    public static final String MOD_ID = "wizardrobe";

    public WizardRobeMod() {
        // Subscribe once, from the main mod class only.
        BaseMod.subscribe(this);
    }

    public static void initialize() {
        new WizardRobeMod();
    }

    public static String makeID(String id) {
        return MOD_ID + ":" + id;
    }

    @Override
    public void receiveEditStrings() {
        String lang;
        switch (Settings.language) {
            case ZHS:
            case ZHT:
                lang = "zhs";
                break;
            default:
                lang = "eng";
                break;
        }
        BaseMod.loadCustomStringsFile(RelicStrings.class, "wizardrobe/localization/" + lang + "/RelicStrings.json");
    }

    @Override
    public void receiveEditRelics() {
        // SHARED keeps the relic usable by every character, which is what the co-op
        // starting relic list needs, since each player may pick a different character.
        BaseMod.addRelic(new WizardRobe(), RelicType.SHARED);
    }
}