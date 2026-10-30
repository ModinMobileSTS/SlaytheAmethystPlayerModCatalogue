package umapyoi.patches;

import basemod.BaseMod;
import basemod.interfaces.PostInitializeSubscriber;
import basemod.interfaces.PostRenderSubscriber;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DescriptionLine;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.GameDictionary;
import com.megacrit.cardcrawl.localization.CardStrings;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 载入结束后的收尾修正：
 *
 * <p>1. 卡名订正（见 {@link #RENAMES}），同时改 CardStrings 与卡牌对象，后续生成的
 * 副本也会用新名字。
 *
 * <p>2. 把卡牌文本里残留的 {@code umapyoi:} / {@code stslib:} 前缀清掉（见 {@link ZhKeywords}）。
 * 卡牌对象在关键字注册之前就构造好了，那时 {@code umapyoi:心情}、{@code stslib:Soulbound}
 * 都还不是关键字，中文管线会把它们当普通文字画在卡面上。这里改掉共享的 CardStrings，
 * 后续新生成的卡牌副本也不会再带前缀。
 *
 * <p>3. 把改写后的裸中文关键字名补登记进关键字表，让它们继续有金色高亮与悬浮说明；
 * 然后重建所有 The Uma 卡牌的描述（旧描述是在关键字就绪前生成的），并对描述行做一次
 * 兜底清洗：万一某个词在管线里仍然没被识别成关键字，也不会把原始 ID 留在卡面上。
 */
@SpireInitializer
public final class UmaZhRuntimeFix implements PostInitializeSubscriber, PostRenderSubscriber {
    private static final Logger LOG = LogManager.getLogger("umapyoi-zh");

    /** 卡牌 ID → 中文卡名。 */
    private static final Map<String, String> RENAMES = new LinkedHashMap<>();

    /** 裸中文关键字名 → 悬浮说明（用于补登记，保留金色高亮与提示框）。 */
    private static final Map<String, String> KEYWORD_DESC = new LinkedHashMap<>();

    private static Method initializeTitle;

    private static int cleanedCards;
    private static int reinitializedCards;
    private static int sanitizedLines;
    private static boolean diagLogged;

    static {
        RENAMES.put("umapyoi:GDoubleZero", "G00 1st.F\u221e");
        RENAMES.put("umapyoi:RedAce", "赤色王牌");
        RENAMES.put("umapyoi:BlueRoseCloser", "蓝蔷薇追猎者");

        KEYWORD_DESC.put("心情", "你当前的 #y心情 会影响你造成的伤害和回合开始时获得的能量。"
                + "处于 #b绝不调 #y心情 [REMOVE_SPACE] 时，少获得 #b1 点能量。"
                + "处于 #b绝好调 #y心情 [REMOVE_SPACE] 时，多获得 #b1 点能量。"
                + "若你在 #b绝不调 的心情下结束战斗，有 #b1/4 的几率随机将一张 #y诅咒 加入你的牌组。");
        KEYWORD_DESC.put("悲伤", "当你的 #y心情 为负时会触发此效果。（ #b不调 或 #b绝不调 ）");
        KEYWORD_DESC.put("喜悦", "当你的 #y心情 为正时会触发此效果。（ #b好调 或 #b绝好调 ）");
        KEYWORD_DESC.put("非赛马娘", "不属于 #y赛马娘 本家的牌，例如其他角色的牌、无色牌、状态牌或诅咒牌。");
        KEYWORD_DESC.put("残影", "你的回合开始时，格挡不会消失。");
        KEYWORD_DESC.put("限制", "随机给予你一种限制行动的状态。（包括 #y无法抽牌 [REMOVE_SPACE]、"
                + "#y无法格挡 [REMOVE_SPACE]、#y无法增益 [REMOVE_SPACE]、#y缠绕 [REMOVE_SPACE] "
                + "与 #y失智 [REMOVE_SPACE]。）");
        KEYWORD_DESC.put("金属化", "在你的回合结束时获得格挡。");
        KEYWORD_DESC.put("狂暴", "在你的回合开始时获得能量。");
        KEYWORD_DESC.put("灵魂绑定", "无法从牌组中移除。");
        KEYWORD_DESC.put("抹除", "战斗结束前从牌堆中暂时移除，并 #r不会 进入消耗堆。");
        KEYWORD_DESC.put("即逝", "这张牌带有 #y抹除 并在打出后从牌组中移除。");
    }

    public static void initialize() {
        BaseMod.subscribe(new UmaZhRuntimeFix());
    }

    @Override
    public void receivePostInitialize() {
        try {
            applyFixes();
        } catch (Throwable t) {
            // 任何意外都不应影响游戏启动
            LOG.info("[umapyoi-zh] applyFixes failed: " + t);
        }
    }

    /** 只打印一次诊断，方便在主菜单出现后确认改写效果。 */
    @Override
    public void receivePostRender(SpriteBatch sb) {
        if (diagLogged) {
            return;
        }
        diagLogged = true;
        try {
            LOG.info("[umapyoi-zh] language=" + Settings.language
                    + " lineBreakViaCharacter=" + Settings.lineBreakViaCharacter
                    + " moodKey=" + GameDictionary.keywords.containsKey("umapyoi:心情")
                    + " restrictKey=" + GameDictionary.keywords.containsKey("umapyoi:限制")
                    + " soulboundKey=" + GameDictionary.keywords.containsKey("stslib:soulbound")
                    + " bareSoulbound=" + GameDictionary.keywords.containsKey("灵魂绑定")
                    + " cleaned=" + cleanedCards
                    + " reinit=" + reinitializedCards
                    + " lines=" + sanitizedLines
                    + " stripHits=" + CnKeywordPrefixFix.hits);
            LOG.info("[umapyoi-zh] Oversight=" + describe("umapyoi:Oversight"));
            LOG.info("[umapyoi-zh] LegFractureCurse=" + describe("umapyoi:LegFractureCurse"));
            LOG.info("[umapyoi-zh] TripleSevens=" + describe("umapyoi:TripleSevens"));
            LOG.info("[umapyoi-zh] LegaciesLiveOn=" + describe("umapyoi:LegaciesLiveOn"));
        } catch (Throwable t) {
            LOG.info("[umapyoi-zh] diag failed: " + t);
        }
    }

    private static String describe(String cardID) {
        try {
            AbstractCard card = CardLibrary.getCard(cardID);
            if (card == null) {
                return "<missing>";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("raw=[").append(card.rawDescription).append("] lines=[");
            for (DescriptionLine line : card.description) {
                sb.append(line.text).append(" / ");
            }
            return sb.append(']').toString();
        } catch (Throwable t) {
            return "<error " + t + ">";
        }
    }

    private static void applyFixes() {
        if (Settings.language != Settings.GameLanguage.ZHS) {
            return;
        }
        List<AbstractCard> cards = new ArrayList<>();
        for (Object o : CardLibrary.getAllCards()) {
            if (o instanceof AbstractCard) {
                cards.add((AbstractCard) o);
            }
        }

        // 1) 清洗共享的 CardStrings，之后新生成的卡牌副本也不会带前缀
        for (AbstractCard card : cards) {
            cleanCardStrings(card.cardID);
        }

        // 2) 把裸中文关键字名补登记进关键字表（金色高亮 + 悬浮说明）
        registerAliases();

        // 3) 卡名订正 + 清理 rawDescription + 重建描述
        for (AbstractCard card : cards) {
            String newName = RENAMES.get(card.cardID);
            if (newName != null) {
                card.name = newName;
                card.originalName = newName;
                refreshTitle(card);
            }
            String raw = card.rawDescription;
            String cleaned = ZhKeywords.clean(raw);
            if (cleaned != null && !cleaned.equals(raw)) {
                card.rawDescription = cleaned;
                cleanedCards++;
            }
            if (card.cardID != null && card.cardID.startsWith("umapyoi:")) {
                card.initializeDescription();
                reinitializedCards++;
            }
            sanitizedLines += sanitizeLines(card);
        }
    }

    /** 兜底：描述行里若还残留命名空间前缀，直接在行文本上改掉，卡面不会露出原始 ID。 */
    private static int sanitizeLines(AbstractCard card) {
        int changed = 0;
        try {
            for (DescriptionLine line : card.description) {
                if (line == null) {
                    continue;
                }
                String fixed = ZhKeywords.clean(line.text);
                if (fixed != null && !fixed.equals(line.text)) {
                    line.text = fixed;
                    changed++;
                }
            }
        } catch (Throwable ignored) {
            // 列表被并发修改之类的意外不影响启动
        }
        return changed;
    }

    private static void cleanCardStrings(String cardID) {
        if (cardID == null) {
            return;
        }
        try {
            CardStrings strings = CardCrawlGame.languagePack.getCardStrings(cardID);
            if (strings == null) {
                return;
            }
            strings.DESCRIPTION = ZhKeywords.clean(strings.DESCRIPTION);
            strings.UPGRADE_DESCRIPTION = ZhKeywords.clean(strings.UPGRADE_DESCRIPTION);
            if (strings.EXTENDED_DESCRIPTION != null) {
                for (int i = 0; i < strings.EXTENDED_DESCRIPTION.length; i++) {
                    strings.EXTENDED_DESCRIPTION[i] = ZhKeywords.clean(strings.EXTENDED_DESCRIPTION[i]);
                }
            }
        } catch (Throwable ignored) {
            // 取不到字符串就只依赖卡牌对象上的 rawDescription
        }
    }

    private static void registerAliases() {
        for (Map.Entry<String, String> entry : KEYWORD_DESC.entrySet()) {
            String name = entry.getKey();
            try {
                if (GameDictionary.keywords.containsKey(name)) {
                    continue;
                }
                BaseMod.addKeyword(name, new String[]{name}, entry.getValue());
            } catch (Throwable ignored) {
                // 登记失败只会失去高亮，文本本身已经是裸中文名
            }
        }
    }

    /** initializeTitle 是包私有的，只能反射调用；用于按新卡名重算标题字号。 */
    private static void refreshTitle(AbstractCard card) {
        try {
            if (initializeTitle == null) {
                initializeTitle = AbstractCard.class.getDeclaredMethod("initializeTitle");
                initializeTitle.setAccessible(true);
            }
            initializeTitle.invoke(card);
        } catch (Throwable ignored) {
            // 标题布局刷新失败不影响卡名本身
        }
    }
}
