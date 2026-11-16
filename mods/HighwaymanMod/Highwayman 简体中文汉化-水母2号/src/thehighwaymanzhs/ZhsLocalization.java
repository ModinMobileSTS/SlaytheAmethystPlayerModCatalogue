package thehighwaymanzhs;

import basemod.BaseMod;
import com.badlogic.gdx.Gdx;
import com.evacipated.cardcrawl.mod.stslib.Keyword;
import com.google.gson.Gson;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.localization.CharacterStrings;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.localization.OrbStrings;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.localization.RelicStrings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import theHighwayman.DefaultMod;

/**
 * Highwayman Mod 简体中文（ZHS）文本。
 *
 * <p>本 Mod 本体只载入 theHighwaymanResources/localization/eng/ 下的英文文本，
 * 因此这里由补丁在游戏语言为简体中文时改为载入本 Mod 资源目录中的中文文本。</p>
 */
public final class ZhsLocalization {
    private static final Logger logger = LogManager.getLogger("HighwaymanZhs");

    /** 中文文本所放位置（本补丁 Mod 的 JAR 根目录下）。 */
    private static final String BASE = "highwaymanzhsResources/localization/zhs/";

    private static final String CARD_FILE = BASE + "DefaultMod-Card-Strings.json";
    private static final String POWER_FILE = BASE + "DefaultMod-Power-Strings.json";
    private static final String RELIC_FILE = BASE + "DefaultMod-Relic-Strings.json";
    private static final String EVENT_FILE = BASE + "DefaultMod-Event-Strings.json";
    private static final String POTION_FILE = BASE + "DefaultMod-Potion-Strings.json";
    private static final String CHARACTER_FILE = BASE + "DefaultMod-Character-Strings.json";
    private static final String ORB_FILE = BASE + "DefaultMod-Orb-Strings.json";
    private static final String KEYWORD_FILE = BASE + "DefaultMod-Keyword-Strings.json";

    private ZhsLocalization() {
    }

    /** 游戏当前是否为简体中文。 */
    public static boolean isChinese() {
        return Settings.language == Settings.GameLanguage.ZHS;
    }

    /**
     * 载入全部简体中文字符串。
     *
     * @return 全部文本都成功载入时返回 true；若有文件缺失或解析失败则返回 false，调用方应退回英文原文。
     */
    public static boolean loadAllStrings() {
        try {
            if (!exists(CARD_FILE) || !exists(POWER_FILE) || !exists(RELIC_FILE) || !exists(EVENT_FILE)
                    || !exists(POTION_FILE) || !exists(CHARACTER_FILE) || !exists(ORB_FILE)) {
                logger.warn("Highwayman 中文本地化文件缺失，退回英文原文。");
                return false;
            }
            BaseMod.loadCustomStringsFile(CardStrings.class, CARD_FILE);
            BaseMod.loadCustomStringsFile(PowerStrings.class, POWER_FILE);
            BaseMod.loadCustomStringsFile(RelicStrings.class, RELIC_FILE);
            BaseMod.loadCustomStringsFile(EventStrings.class, EVENT_FILE);
            BaseMod.loadCustomStringsFile(PotionStrings.class, POTION_FILE);
            BaseMod.loadCustomStringsFile(CharacterStrings.class, CHARACTER_FILE);
            BaseMod.loadCustomStringsFile(OrbStrings.class, ORB_FILE);
            logger.info("Highwayman 简体中文字符串载入完成。");
            return true;
        } catch (Throwable t) {
            logger.error("Highwayman 简体中文字符串载入失败，退回英文原文。", t);
            return false;
        }
    }

    /**
     * 载入简体中文关键字（Ammon 弹药、流血、开火……）。
     *
     * @return 成功时返回 true。
     */
    public static boolean loadKeywords() {
        try {
            if (!exists(KEYWORD_FILE)) {
                logger.warn("Highwayman 中文关键字文件缺失，退回英文关键字。");
                return false;
            }
            String json = Gdx.files.internal(KEYWORD_FILE).readString("UTF-8");
            Keyword[] keywords = new Gson().fromJson(json, Keyword[].class);
            if (keywords == null || keywords.length == 0) {
                logger.warn("Highwayman 中文关键字文件为空，退回英文关键字。");
                return false;
            }
            for (Keyword keyword : keywords) {
                if (keyword == null || keyword.NAMES == null || keyword.NAMES.length == 0) {
                    continue;
                }
                // 不带 modID 注册：关键字以中文本名写入 GameDictionary，
                // 这样卡牌描述里的「弹药」等词条才会被识别、高亮并显示提示框。
                BaseMod.addKeyword(keyword.PROPER_NAME, keyword.NAMES, keyword.DESCRIPTION);
            }
            logger.info("Highwayman 简体中文关键字载入完成。");
            return true;
        } catch (Throwable t) {
            logger.error("Highwayman 简体中文关键字载入失败，退回英文关键字。", t);
            return false;
        }
    }

    private static boolean exists(String path) {
        try {
            return Gdx.files.internal(path).exists();
        } catch (Throwable t) {
            return false;
        }
    }
}
