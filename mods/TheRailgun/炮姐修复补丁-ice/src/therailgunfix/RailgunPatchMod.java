package therailgunfix;

import basemod.BaseMod;
import basemod.interfaces.EditStringsSubscriber;
import com.badlogic.gdx.Gdx;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.CardStrings;

@SpireInitializer
public class RailgunPatchMod implements EditStringsSubscriber {
    private static final String RESOURCE_ROOT = "TheRailgunPatchResources/localization/";

    public static void initialize() {
        new RailgunPatchMod();
    }

    public RailgunPatchMod() {
        BaseMod.subscribe(this);
    }

    @Override
    public void receiveEditStrings() {
        String language = Settings.language.toString().toLowerCase();
        String localized = RESOURCE_ROOT + language + "/Card-Strings.json";
        if (Gdx.files.internal(localized).exists()) {
            BaseMod.loadCustomStringsFile(CardStrings.class, localized);
        } else {
            BaseMod.loadCustomStringsFile(CardStrings.class, RESOURCE_ROOT + "eng/Card-Strings.json");
        }
    }
}
