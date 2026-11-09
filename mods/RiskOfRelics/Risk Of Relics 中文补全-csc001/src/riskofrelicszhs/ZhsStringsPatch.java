package riskofrelicszhs;

import basemod.BaseMod;
import com.badlogic.gdx.Gdx;
import com.evacipated.cardcrawl.mod.stslib.Keyword;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.google.gson.Gson;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.localization.RelicStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
import java.nio.charset.StandardCharsets;

/**
 * 在 Risk Of Relics 自身加载完语言文件之后，补齐简体中文文本。
 * 仅在游戏语言为简体中文时生效，避免影响其它语言。
 */
public final class ZhsStringsPatch {

    private static final String DIR = "riskofrelicszhs/localization/zhs/";
    private static final String KEYWORD_MODID = "riskofrelics";

    private ZhsStringsPatch() {
    }

    private static boolean isZhs() {
        return Settings.language == Settings.GameLanguage.ZHS;
    }

    @SpirePatch2(
        cls = "riskOfRelics.RiskOfRelics",
        method = "receiveEditStrings",
        paramtypez = {}
    )
    public static class EditStringsPatch {
        @SpirePostfixPatch
        public static void Postfix(riskOfRelics.RiskOfRelics __instance) {
            if (!isZhs()) {
                return;
            }
            BaseMod.loadCustomStringsFile(CardStrings.class, DIR + "Cards.json");
            BaseMod.loadCustomStringsFile(PowerStrings.class, DIR + "Powers.json");
            BaseMod.loadCustomStringsFile(RelicStrings.class, DIR + "Relics.json");
            BaseMod.loadCustomStringsFile(EventStrings.class, DIR + "Events.json");
            BaseMod.loadCustomStringsFile(UIStrings.class, DIR + "UI.json");
            BaseMod.loadCustomStringsFile(MonsterStrings.class, DIR + "Monsters.json");
        }
    }

    @SpirePatch2(
        cls = "riskOfRelics.RiskOfRelics",
        method = "receiveEditKeywords",
        paramtypez = {}
    )
    public static class EditKeywordsPatch {
        @SpirePostfixPatch
        public static void Postfix(riskOfRelics.RiskOfRelics __instance) {
            if (!isZhs()) {
                return;
            }
            Gson gson = new Gson();
            String json = Gdx.files.internal(DIR + "Keywords.json")
                    .readString(String.valueOf(StandardCharsets.UTF_8));
            Keyword[] keywords = gson.fromJson(json, Keyword[].class);
            if (keywords == null) {
                return;
            }
            for (Keyword keyword : keywords) {
                BaseMod.addKeyword(KEYWORD_MODID, keyword.PROPER_NAME, keyword.NAMES, keyword.DESCRIPTION);
            }
        }
    }
}
