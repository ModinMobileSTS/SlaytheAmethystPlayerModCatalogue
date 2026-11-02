package TheNecrobinderMod.Patches;

// ============================================================
// 修复点：原版用 @SpireInsertPatch(loc=304) 写死行号插入
// GameActionManager.getNextAction，对游戏版本极其敏感
// （mod 声明适配 12-18-2022，实机为 04-15-2026）。
// 改为 SpireInsertLocator 动态定位，锚点 =
// AbstractPlayer.useCard(...) 在 getNextAction 中的唯一调用处，
// 位于 cardQueue.remove(0) 之前（get(0) 取当前牌安全）。
// 锚点唯一性已实证：反编译中 "player.useCard" 在该方法仅出现 1 次。
// API 依据：ModTheSpire 源码 SpireInsertLocator / LineFinder /
// Matcher.MethodCallMatcher（均为真实存在的公开 API）。
// ============================================================

import TheNecrobinderMod.cards.uncommon.RightHandHand;
import com.evacipated.cardcrawl.modthespire.lib.LineFinder;
import com.evacipated.cardcrawl.modthespire.lib.Matcher;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertLocator;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardQueueItem;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import javassist.CtBehavior;

@SpirePatch(clz = GameActionManager.class, method = "getNextAction")
public class RightHandHandPatch {

    @SpireInsertPatch(locator = Locator.class)
    public static void insert(GameActionManager __instance) {
        for (AbstractCard c : AbstractDungeon.player.discardPile.group) {
            if (c instanceof RightHandHand) {
                ((RightHandHand) c).onCardPlayedWhileInDiscard(
                        ((CardQueueItem) __instance.cardQueue.get(0)).card,
                        ((CardQueueItem) __instance.cardQueue.get(0)).monster);
            }
        }
    }

    public static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            return LineFinder.findInOrder(ctMethodToPatch,
                    new Matcher.MethodCallMatcher(
                            "com.megacrit.cardcrawl.characters.AbstractPlayer",
                            "useCard"));
        }
    }
}
