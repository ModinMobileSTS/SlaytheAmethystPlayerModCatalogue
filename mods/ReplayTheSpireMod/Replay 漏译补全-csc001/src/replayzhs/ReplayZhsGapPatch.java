package replayzhs;

import basemod.BaseMod;
import basemod.interfaces.EditStringsSubscriber;
import basemod.interfaces.PostInitializeSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.localization.RelicStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import replayTheSpire.ReplayTheSpireMod;
import replayTheSpire.ReplayUnlockAchieve;

@SpireInitializer
public class ReplayZhsGapPatch implements EditStringsSubscriber, PostInitializeSubscriber {

    private static final Logger logger = LogManager.getLogger("ReplayZhsGapPatch");
    private static final String DIR = "replayzhs/localization/";

    public static void initialize() {
        BaseMod.subscribe(new ReplayZhsGapPatch());
    }

    private static boolean isZhs() {
        return Settings.language != null && "ZHS".equals(Settings.language.toString());
    }

    @Override
    public void receiveEditStrings() {
        if (!isZhs()) {
            return;
        }
        try {
            BaseMod.loadCustomStringsFile(PowerStrings.class, DIR + "GapPowerStrings.json");
            BaseMod.loadCustomStringsFile(RelicStrings.class, DIR + "GapRelicStrings.json");
            BaseMod.loadCustomStringsFile(MonsterStrings.class, DIR + "GapMonsterStrings.json");
            BaseMod.loadCustomStringsFile(CardStrings.class, DIR + "GapCardStrings.json");
            BaseMod.loadCustomStringsFile(UIStrings.class, DIR + "GapUIStrings.json");
            logger.info("Replay gap strings loaded.");
        } catch (Throwable t) {
            logger.error("Failed to load Replay gap strings", t);
        }
    }

    @Override
    public void receivePostInitialize() {
        if (!isZhs()) {
            return;
        }
        try {
            UIStrings ui = CardCrawlGame.languagePack.getUIString("ReplayZhsGap:Unlocks");
            if (ui == null || ui.TEXT == null || ui.TEXT.length < 6) {
                return;
            }
            ArrayList<ReplayUnlockAchieve> list = ReplayTheSpireMod.unlockAchievements;
            if (list == null) {
                return;
            }
            for (ReplayUnlockAchieve ach : list) {
                String name = null;
                String req = null;
                if ("abe_win".equals(ach.achievementId)) {
                    name = ui.TEXT[0];
                    req = ui.TEXT[1];
                } else if ("abe_perfect".equals(ach.achievementId)) {
                    name = ui.TEXT[2];
                    req = ui.TEXT[3];
                } else if ("complete_eye".equals(ach.achievementId)) {
                    name = ui.TEXT[4];
                    req = ui.TEXT[5];
                }
                if (name == null || req == null) {
                    continue;
                }
                ach.name = name;
                ach.desc = req;
                if (ach.relicId != null) {
                    UnlockTracker.unlockReqs.put(ach.relicId, req);
                }
            }
            logger.info("Replay achievement unlock texts localized.");
        } catch (Throwable t) {
            logger.error("Failed to localize Replay achievement unlock texts", t);
        }
    }
}
