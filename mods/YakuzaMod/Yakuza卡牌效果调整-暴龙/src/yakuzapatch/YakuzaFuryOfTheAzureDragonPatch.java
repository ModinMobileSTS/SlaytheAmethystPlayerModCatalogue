package yakuzapatch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import theYakuza.cards.RarePowers.YakuzaFuryOfTheAzureDragon;

/**
 * 「应龙之怒」/「应龙之怒+」：费用改为 1 费；升级版额外获得「抽2张牌」，并同步卡牌描述。
 */
public final class YakuzaFuryOfTheAzureDragonPatch {
    private YakuzaFuryOfTheAzureDragonPatch() {
    }

    private static String withDraw(String desc) {
        if (desc.contains("抽2张牌") || desc.contains("Draw 2 cards")) {
            return desc;
        }
        return desc + (Settings.language == Settings.GameLanguage.ZHS ? " NL 抽2张牌。" : " NL Draw 2 cards.");
    }

    @SpirePatch2(cls = "theYakuza.cards.RarePowers.YakuzaFuryOfTheAzureDragon", method = "<ctor>", paramtypez = {})
    public static final class CtorPatch {
        private CtorPatch() {
        }

        @SpirePostfixPatch
        public static void Postfix(YakuzaFuryOfTheAzureDragon __instance) {
            __instance.cost = 1;
            __instance.costForTurn = 1;
        }
    }

    @SpirePatch2(cls = "theYakuza.cards.RarePowers.YakuzaFuryOfTheAzureDragon", method = "upgrade", paramtypez = {})
    public static final class UpgradePatch {
        private UpgradePatch() {
        }

        @SpirePostfixPatch
        public static void Postfix(YakuzaFuryOfTheAzureDragon __instance) {
            __instance.rawDescription = withDraw(__instance.rawDescription);
            __instance.initializeDescription();
        }
    }

    @SpirePatch2(cls = "theYakuza.cards.RarePowers.YakuzaFuryOfTheAzureDragon", method = "use",
        paramtypez = {AbstractPlayer.class, AbstractMonster.class})
    public static final class UsePatch {
        private UsePatch() {
        }

        @SpirePostfixPatch
        public static void Postfix(YakuzaFuryOfTheAzureDragon __instance, Object[] __args) {
            if (__instance.upgraded) {
                AbstractDungeon.actionManager.addToBottom(new DrawCardAction(2));
            }
        }
    }
}
