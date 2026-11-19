package yakuzapatch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import theYakuza.cards.CommonAttacks.YakuzaEssenceOfSwiftStrikes;

/**
 * 「闪身击」/「闪身击+」：新增获得格挡效果（基础值普通版 1 点、升级版 3 点）。
 * 补丁为该牌补上原版卡牌的 baseBlock，使卡面描述使用 !B! 并按原版算法
 * （applyPowersToBlock 的 modifyBlock / modifyBlockLast）随热血值与敏捷实时变化，
 * 打出时也按同一算法结算实际格挡。
 */
public final class YakuzaEssenceOfSwiftStrikesBlockPatch {
    private YakuzaEssenceOfSwiftStrikesBlockPatch() {
    }

    private static final int BASE_BLOCK = 1;
    private static final int UPGRADED_BLOCK = 3;

    private static String withBlockText(String desc) {
        if (desc.contains("格挡") || desc.contains("Block")) {
            return desc;
        }
        return desc + (Settings.language == Settings.GameLanguage.ZHS ? " NL 获得 !B! 点 格挡 。" : " NL Gain !B! Block.");
    }

    @SpirePatch2(cls = "theYakuza.cards.CommonAttacks.YakuzaEssenceOfSwiftStrikes", method = "<ctor>", paramtypez = {})
    public static final class CtorPatch {
        private CtorPatch() {
        }

        @SpirePostfixPatch
        public static void Postfix(YakuzaEssenceOfSwiftStrikes __instance) {
            __instance.baseBlock = BASE_BLOCK;
            __instance.block = BASE_BLOCK;
            __instance.rawDescription = withBlockText(__instance.rawDescription);
            __instance.initializeDescription();
        }
    }

    @SpirePatch2(cls = "theYakuza.cards.CommonAttacks.YakuzaEssenceOfSwiftStrikes", method = "upgrade", paramtypez = {})
    public static final class UpgradePatch {
        private UpgradePatch() {
        }

        @SpirePostfixPatch
        public static void Postfix(YakuzaEssenceOfSwiftStrikes __instance) {
            __instance.baseBlock = UPGRADED_BLOCK;
            __instance.block = UPGRADED_BLOCK;
            __instance.upgradedBlock = true;
            __instance.rawDescription = withBlockText(__instance.rawDescription);
            __instance.initializeDescription();
        }
    }

    @SpirePatch2(cls = "theYakuza.cards.CommonAttacks.YakuzaEssenceOfSwiftStrikes", method = "use",
        paramtypez = {AbstractPlayer.class, AbstractMonster.class})
    public static final class UsePatch {
        private UsePatch() {
        }

        @SpirePostfixPatch
        public static void Postfix(YakuzaEssenceOfSwiftStrikes __instance, Object[] __args) {
            AbstractPlayer p = (AbstractPlayer) __args[0];
            float tmp = __instance.baseBlock;
            for (AbstractPower power : p.powers) {
                tmp = power.modifyBlock(tmp, __instance);
            }
            for (AbstractPower power : p.powers) {
                tmp = power.modifyBlockLast(tmp);
            }
            if (tmp < 0.0F) {
                tmp = 0.0F;
            }
            AbstractDungeon.actionManager.addToBottom(new GainBlockAction(p, p, (int) tmp));
        }
    }
}
