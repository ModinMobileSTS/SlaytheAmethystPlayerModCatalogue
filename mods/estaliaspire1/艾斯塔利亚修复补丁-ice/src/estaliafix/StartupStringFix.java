package estaliafix;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.core.CardCrawlGame;

/**
 * 启动崩溃之二（补丁阶段通过之后才暴露出来）：
 *
 * BaseMod 的 EditStrings 回调是挂在 com.megacrit.cardcrawl.localization.LocalizedStrings
 * 构造器末尾的 Postfix，而 CardCrawlGame.languagePack = new LocalizedStrings() 的赋值
 * 发生在构造器返回之后。所以回调期间 CardCrawlGame.languagePack 仍然是 null。
 *
 * 原模组的 receiveEditStrings 会遍历 estalia.Relic.DATA，这会把 estalia.Relic 连同父类
 * CustomRelic / AbstractRelic 一起类初始化，而 AbstractRelic 的静态字段
 *   tutorialStrings = CardCrawlGame.languagePack.getTutorialString("Relic Tip")
 * 直接在 null 上调用，抛出 NullPointerException（外层 ExceptionInInitializerError），
 * 游戏无法进入主菜单。
 *
 * 处理方式：receiveEditStrings 若早于 languagePack 赋值被调用，就先跳过原方法体；
 * 之后在 receiveEditCards / receiveEditCharacters / receiveEditRelics /
 * receiveEditKeywords / receivePostInitialize 之前补执行一次。这些回调都发生在
 * languagePack 就绪之后（RelicLibrary.initialize 里创建原版遗物本身就依赖它），
 * 并且早于任何卡牌、遗物、角色的创建，所以字符串不会缺席。
 */
public class StartupStringFix {

    private static boolean stringsReady = false;

    private static void log(String msg) {
        System.out.println("[EstaliaFix] " + msg);
    }

    private static SpireReturn needStrings(estalia.Mod mod, String where) {
        if (stringsReady) {
            return SpireReturn.Continue();
        }
        if (CardCrawlGame.languagePack == null) {
            log(where + " 时 languagePack 仍未就绪，继续推迟字符串加载");
            return SpireReturn.Return(null);
        }
        stringsReady = true;
        log(where + " 之前补执行 receiveEditStrings");
        mod.receiveEditStrings();
        return SpireReturn.Continue();
    }

    @SpirePatch2(clz = estalia.Mod.class, method = "receiveEditStrings", paramtypez = {})
    public static class DelayEditStrings {
        @SpirePrefixPatch
        public static SpireReturn Prefix(estalia.Mod __instance) {
            if (CardCrawlGame.languagePack != null) {
                stringsReady = true;
                return SpireReturn.Continue();
            }
            log("receiveEditStrings 早于 languagePack 赋值，推迟到卡牌/遗物注册之前执行");
            return SpireReturn.Return(null);
        }
    }

    @SpirePatch2(clz = estalia.Mod.class, method = "receiveEditCards", paramtypez = {})
    public static class BeforeEditCards {
        @SpirePrefixPatch
        public static SpireReturn Prefix(estalia.Mod __instance) {
            return needStrings(__instance, "receiveEditCards");
        }
    }

    @SpirePatch2(clz = estalia.Mod.class, method = "receiveEditCharacters", paramtypez = {})
    public static class BeforeEditCharacters {
        @SpirePrefixPatch
        public static SpireReturn Prefix(estalia.Mod __instance) {
            return needStrings(__instance, "receiveEditCharacters");
        }
    }

    @SpirePatch2(clz = estalia.Mod.class, method = "receiveEditRelics", paramtypez = {})
    public static class BeforeEditRelics {
        @SpirePrefixPatch
        public static SpireReturn Prefix(estalia.Mod __instance) {
            return needStrings(__instance, "receiveEditRelics");
        }
    }

    @SpirePatch2(clz = estalia.Mod.class, method = "receiveEditKeywords", paramtypez = {})
    public static class BeforeEditKeywords {
        @SpirePrefixPatch
        public static SpireReturn Prefix(estalia.Mod __instance) {
            return needStrings(__instance, "receiveEditKeywords");
        }
    }

    @SpirePatch2(clz = estalia.Mod.class, method = "receivePostInitialize", paramtypez = {})
    public static class BeforePostInitialize {
        @SpirePrefixPatch
        public static SpireReturn Prefix(estalia.Mod __instance) {
            return needStrings(__instance, "receivePostInitialize");
        }
    }
}
