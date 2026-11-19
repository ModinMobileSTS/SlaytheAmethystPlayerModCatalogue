package yakuzapatch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.VulnerablePower;
import theYakuza.actions.MinigameAction;
import theYakuza.cards.RareSkills.YakuzaBakaMitai;
import theYakuza.powers.HeatLevelPower;

/**
 * 「像笨蛋一样」/「像笨蛋一样+」：新增「获得1层热血值」；升级版抽牌 2 张改为 3 张；
 * 施加的易伤由 3 层修正为 2 层以与卡牌描述一致；并同步卡牌描述。
 */
public final class YakuzaBakaMitaiPatch {
    private YakuzaBakaMitaiPatch() {
    }

    private static String withHeatGain(String desc) {
        if (Settings.language == Settings.GameLanguage.ZHS) {
            if (desc.contains("获得1层")) {
                return desc;
            }
            String heat = " NL 获得1层 theyakuza:热血值 。";
            return desc.contains(" NL 消耗 。")
                ? desc.replace(" NL 消耗 。", heat + " NL 消耗 。")
                : desc + heat;
        }
        if (desc.contains("Gain 1 theyakuza:Heat")) {
            return desc;
        }
        String heat = " NL Gain 1 theyakuza:Heat.";
        return desc.contains(" NL Exhaust.")
            ? desc.replace(" NL Exhaust.", heat + " NL Exhaust.")
            : desc + heat;
    }

    @SpirePatch2(cls = "theYakuza.cards.RareSkills.YakuzaBakaMitai", method = "<ctor>", paramtypez = {})
    public static final class CtorPatch {
        private CtorPatch() {
        }

        @SpirePostfixPatch
        public static void Postfix(YakuzaBakaMitai __instance) {
            __instance.rawDescription = withHeatGain(__instance.rawDescription);
            __instance.initializeDescription();
        }
    }

    @SpirePatch2(cls = "theYakuza.cards.RareSkills.YakuzaBakaMitai", method = "upgrade", paramtypez = {})
    public static final class UpgradePatch {
        private UpgradePatch() {
        }

        @SpirePostfixPatch
        public static void Postfix(YakuzaBakaMitai __instance) {
            String desc = __instance.rawDescription;
            desc = desc.replace("抽2张牌", "抽3张牌");
            desc = desc.replace("Draw 2 cards", "Draw 3 cards");
            __instance.rawDescription = desc;
            __instance.initializeDescription();
        }
    }

    @SpirePatch2(cls = "theYakuza.cards.RareSkills.YakuzaBakaMitai", method = "use",
        paramtypez = {AbstractPlayer.class, AbstractMonster.class})
    public static final class UsePatch {
        private UsePatch() {
        }

        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(YakuzaBakaMitai __instance, Object[] __args) {
            AbstractPlayer p = (AbstractPlayer) __args[0];
            CardCrawlGame.sound.playA("YAKUZA_DAME_DA_NE", 0.0f);
            AbstractDungeon.actionManager.addToBottom(new MinigameAction(p));
            AbstractDungeon.actionManager.addToBottom(new GainEnergyAction(__instance.magicNumber));
            AbstractDungeon.actionManager.addToBottom(new DrawCardAction(__instance.upgraded ? 3 : 2));
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(p, p, new VulnerablePower(p, 2, false)));
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(p, p, new HeatLevelPower(p, p, 1)));
            return SpireReturn.Return();
        }
    }
}
