package chronopatch;

import basemod.BaseMod;
import basemod.interfaces.EditKeywordsSubscriber;
import basemod.interfaces.EditStringsSubscriber;
import basemod.interfaces.ISubscriber;
import com.badlogic.gdx.Gdx;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.localization.Keyword;
import com.megacrit.cardcrawl.localization.OrbStrings;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.localization.RelicStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
import java.lang.reflect.Type;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SpireInitializer
public class ChronoZhStrings implements EditStringsSubscriber, EditKeywordsSubscriber {
    private static final Logger logger = LogManager.getLogger(ChronoZhStrings.class.getName());

    public ChronoZhStrings() {
        BaseMod.subscribe(this);
    }

    public static void initialize() {
        logger.info("ChronoZh: Loading Chinese translation for The Disciple");
        new ChronoZhStrings();
    }

    private String readResource(String path) {
        return Gdx.files.internal(path).readString("UTF-8");
    }

    public void receiveEditKeywords() {
        try {
            String keywordStrings = this.readResource("chronopatch/chronoKeywords_zh.json");
            Gson gson = new Gson();
            Type typeToken = new TypeToken<Map<String, Keyword>>(){}.getType();
            Map<String, Keyword> keywords = gson.fromJson(keywordStrings, typeToken);
            for (Keyword kw : keywords.values()) {
                logger.info("ChronoZh: Adding Chinese Keyword - " + kw.NAMES[0]);
                BaseMod.addKeyword(kw.NAMES, kw.DESCRIPTION);
            }
        } catch (Exception e) {
            logger.error("ChronoZh: Failed to load Chinese keywords", e);
        }
    }

    public void receiveEditStrings() {
        try {
            BaseMod.loadCustomStrings(CardStrings.class, this.readResource("chronopatch/chronoCards_zh.json"));
            BaseMod.loadCustomStrings(RelicStrings.class, this.readResource("chronopatch/chronoRelics_zh.json"));
            BaseMod.loadCustomStrings(OrbStrings.class, this.readResource("chronopatch/chronoOrbs_zh.json"));
            BaseMod.loadCustomStrings(PowerStrings.class, this.readResource("chronopatch/chronoPowers_zh.json"));
            BaseMod.loadCustomStrings(PotionStrings.class, this.readResource("chronopatch/chronoPotions_zh.json"));
            BaseMod.loadCustomStrings(EventStrings.class, this.readResource("chronopatch/chronoEvents_zh.json"));
            BaseMod.loadCustomStrings(UIStrings.class, this.readResource("chronopatch/chronoUI_zh.json"));
            logger.info("ChronoZh: All Chinese strings loaded successfully.");
        } catch (Exception e) {
            logger.error("ChronoZh: Failed to load Chinese strings", e);
        }
    }
}
