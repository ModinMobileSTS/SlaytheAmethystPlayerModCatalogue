package yakuzapatch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import theYakuza.cards.TemporaryCards.YakuzaGrabBrokenPipe;

/**
 * 「抓取破管道」/「抓取破管道+」：新增抽 1 张牌，并同步卡牌描述。
 */
public final class YakuzaGrabBrokenPipeDrawPatch {
    private YakuzaGrabBrokenPipeDrawPatch() {
    }

    private static String withDraw(String desc) {
        if (desc.contains("抽1张牌") || desc.contains("Draw 1 card")) {
            return desc;
        }
        if (Settings.language == Settings.GameLanguage.ZHS) {
            String add = " NL 抽1张牌。";
            return desc.contains(" NL 消耗 。") ? desc.replace(" NL 消耗 。", add + " NL 消耗 。") : desc + add;
        }
        String add = " NL Draw 1 card.";
        return desc.contains(" NL Exhaust.") ? desc.replace(" NL Exhaust.", add + " NL Exhaust.") : desc + add;
    }

    @SpirePatch2(cls = "theYakuza.cards.TemporaryCards.YakuzaGrabBrokenPipe", method = "<ctor>", paramtypez = {})
    public static final class CtorPatch {
        private CtorPatch() {
        }

        @SpirePostfixPatch
        public static void Postfix(YakuzaGrabBrokenPipe __instance) {
            __instance.rawDescription = withDraw(__instance.rawDescription);
            __instance.initializeDescription();
        }
    }

    @SpirePatch2(cls = "theYakuza.cards.TemporaryCards.YakuzaGrabBrokenPipe", method = "upgrade", paramtypez = {})
    public static final class UpgradePatch {
        private UpgradePatch() {
        }

        @SpirePostfixPatch
        public static void Postfix(YakuzaGrabBrokenPipe __instance) {
            __instance.rawDescription = withDraw(__instance.rawDescription);
            __instance.initializeDescription();
        }
    }

    @SpirePatch2(cls = "theYakuza.cards.TemporaryCards.YakuzaGrabBrokenPipe", method = "use",
        paramtypez = {AbstractPlayer.class, AbstractMonster.class})
    public static final class UsePatch {
        private UsePatch() {
        }

        @SpirePostfixPatch
        public static void Postfix(YakuzaGrabBrokenPipe __instance, Object[] __args) {
            AbstractDungeon.actionManager.addToBottom(new DrawCardAction(1));
        }
    }
}
