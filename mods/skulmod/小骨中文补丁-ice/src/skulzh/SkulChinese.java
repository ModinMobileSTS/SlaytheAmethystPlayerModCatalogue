package skulzh;

import basemod.BaseMod;
import basemod.interfaces.EditKeywordsSubscriber;
import basemod.interfaces.EditStringsSubscriber;
import com.badlogic.gdx.Gdx;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.google.gson.Gson;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.localization.CharacterStrings;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.localization.OrbStrings;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.localization.RelicStrings;
import com.megacrit.cardcrawl.localization.StanceStrings;
import com.megacrit.cardcrawl.localization.TutorialStrings;
import com.megacrit.cardcrawl.localization.UIStrings;

@SpireInitializer
public final class SkulChinese implements EditStringsSubscriber, EditKeywordsSubscriber {
    private static final String ROOT = "skulzh/localization/";

    public static void initialize() {
        BaseMod.subscribe(new SkulChinese());
    }

    public static boolean enabled() {
        return Settings.language == Settings.GameLanguage.ZHS;
    }

    @Override
    public void receiveEditStrings() {
        if (!enabled()) return;
        for (int i = 1; i <= 4; i++) {
            BaseMod.loadCustomStringsFile(CardStrings.class, ROOT + String.format("Cards%02d.json", i));
        }
        BaseMod.loadCustomStringsFile(CharacterStrings.class, ROOT + "CharacterStrings.json");
        BaseMod.loadCustomStringsFile(EventStrings.class, ROOT + "EventStrings.json");
        BaseMod.loadCustomStringsFile(MonsterStrings.class, ROOT + "MonsterStrings.json");
        BaseMod.loadCustomStringsFile(OrbStrings.class, ROOT + "OrbStrings.json");
        BaseMod.loadCustomStringsFile(PotionStrings.class, ROOT + "PotionStrings.json");
        BaseMod.loadCustomStringsFile(PowerStrings.class, ROOT + "PowerStrings.json");
        BaseMod.loadCustomStringsFile(PowerStrings.class, ROOT + "LegacyPowerStrings.json");
        BaseMod.loadCustomStringsFile(RelicStrings.class, ROOT + "RelicStrings.json");
        BaseMod.loadCustomStringsFile(StanceStrings.class, ROOT + "StanceStrings.json");
        BaseMod.loadCustomStringsFile(TutorialStrings.class, ROOT + "TutorialStrings.json");
        BaseMod.loadCustomStringsFile(UIStrings.class, ROOT + "UIStrings.json");
    }

    @Override
    public void receiveEditKeywords() {
        if (!enabled()) return;
        String json = Gdx.files.internal(ROOT + "Keywords.json").readString("UTF-8");
        for (Keyword keyword : new Gson().fromJson(json, Keyword[].class)) {
            BaseMod.addKeyword("skulmod", keyword.name, keyword.aliases, keyword.description);
        }
    }

    private static final class Keyword {
        String name;
        String[] aliases;
        String description;
    }
}
