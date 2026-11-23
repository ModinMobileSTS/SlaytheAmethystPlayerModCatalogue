package estaliafix;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.ByRef;
import com.evacipated.cardcrawl.modthespire.lib.LineFinder;
import com.evacipated.cardcrawl.modthespire.lib.Matcher;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertLocator;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.helpers.GameDictionary;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.screens.SingleCardViewPopup;
import java.util.ArrayList;
import javassist.CtBehavior;

/**
 * 让本模组的关键词说明在所有界面显示（手牌悬停、卡牌选择、百科、大卡预览）。
 *
 * 关键词文字本身是写了的：estalia.Mod.editKeywords 注册了六条说明。但 BaseMod 把键名
 * 注册成“estalia:潮汐”这种带前缀形式，而中文卡面描述里的“潮汐”没有空格也没有前缀，
 * 原版 initializeDescription 不会把它放进 card.keywords。共用提示通道
 * TipHelper.renderKeywords 只认 card.keywords + GameDictionary.keywords，于是只有
 * 单独补过大卡预览的百科里能看到。
 *
 * 处理：
 * 1) 关键词注册后同时写入无前缀别名，供所有提示通道查询；
 * 2) 卡牌构建描述后，把描述里实际出现的关键词加入 card.keywords；
 * 3) 大卡预览保留兜底补充（会按标题去重，不会重复显示）。
 */
public class CardKeywordTooltipFix {

    private static final String[] KEYWORDS = {"潮汐", "祈祷", "星痕", "焚烧", "潮湿", "冻结"};

    /** 父模组注册完关键词后，补一份无前缀的键，供 TipHelper 与预览共用。 */
    @SpirePatch2(clz = estalia.Mod.class, method = "receiveEditKeywords", paramtypez = {})
    public static class RegisterAliases {
        @SpirePostfixPatch
        public static void postfix() {
            for (String name : KEYWORDS) {
                String body = GameDictionary.keywords.get("estalia:" + name);
                if (body != null && !body.isEmpty() && !GameDictionary.keywords.containsKey(name)) {
                    GameDictionary.keywords.put(name, body);
                }
            }
        }
    }

    /** 所有卡牌描述构建后都更新关键词列表，覆盖手牌/选牌/百科等共用提示通道。 */
    @SpirePatch2(clz = AbstractCard.class, method = "initializeDescription", paramtypez = {})
    public static class CollectCardKeywords {
        @SpirePostfixPatch
        public static void postfix(AbstractCard __instance) {
            try {
                // 不按 Card/TechnologyCard 类型过滤：两类卡分别继承 CustomCard，
                // Token 等衍生卡也需要同样的提示，统一按描述内容判断。
                String text = __instance.rawDescription == null ? "" : __instance.rawDescription;
                for (String name : KEYWORDS) {
                    if (!text.contains(name)) {
                        continue;
                    }
                    if (!GameDictionary.keywords.containsKey(name)) {
                        String body = GameDictionary.keywords.get("estalia:" + name);
                        if (body != null && !body.isEmpty()) {
                            GameDictionary.keywords.put(name, body);
                        }
                    }
                    if (GameDictionary.keywords.containsKey(name) && !__instance.keywords.contains(name)) {
                        __instance.keywords.add(name);
                    }
                }
            } catch (Throwable e) {
                log("补充卡牌关键词失败: " + e);
            }
        }
    }

    /** 大卡预览兜底：即使卡没有被上面的流程覆盖，也能显示已注册的关键词说明。 */
    @SpirePatch2(clz = SingleCardViewPopup.class, method = "renderTips",
            paramtypez = {SpriteBatch.class})
    public static class AddKeywordTips {

        @SpireInsertPatch(locator = Locator.class, localvars = {"t"})
        public static void insert(SingleCardViewPopup __instance, @ByRef ArrayList<PowerTip>[] t) {
            try {
                AbstractCard card = ReflectionHacks.getPrivate(__instance,
                        SingleCardViewPopup.class, "card");
                if (card == null) {
                    return;
                }
                String text = card.rawDescription == null ? "" : card.rawDescription;
                for (String name : KEYWORDS) {
                    if (!text.contains(name)) {
                        continue;
                    }
                    String body = GameDictionary.keywords.get(name);
                    if (body == null || body.isEmpty()) {
                        body = GameDictionary.keywords.get("estalia:" + name);
                    }
                    if (body == null || body.isEmpty()) {
                        continue;
                    }
                    boolean exists = false;
                    for (PowerTip tip : t[0]) {
                        if (tip != null && name.equals(tip.header)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        t[0].add(new PowerTip(name, body));
                    }
                }
            } catch (Throwable e) {
                log("预览关键词提示失败: " + e);
            }
        }

        public static class Locator extends SpireInsertLocator {
            @Override
            public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
                Matcher matcher = new Matcher.MethodCallMatcher(ArrayList.class, "isEmpty");
                return LineFinder.findInOrder(ctMethodToPatch, matcher);
            }
        }
    }

    private static void log(String msg) {
        System.out.println("[EstaliaFix] " + msg);
    }
}
