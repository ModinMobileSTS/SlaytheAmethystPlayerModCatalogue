package yakuzapatch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import theYakuza.cards.CommonSkills.YakuzaEssenceOfRepair;

/**
 * 「修缮」/「修缮+」：新增获得 5 点格挡，并同步卡牌描述。
 */
public final class YakuzaEssenceOfRepairBlockPatch {
    private YakuzaEssenceOfRepairBlockPatch() {
    }

    private static String withBlock(String desc) {
        if (desc.contains("获得5点 格挡") || desc.contains("Gain 5 Block")) {
            return desc;
        }
        return desc + (Settings.language == Settings.GameLanguage.ZHS ? " NL 获得5点 格挡 。" : " NL Gain 5 Block.");
    }

    @SpirePatch2(cls = "theYakuza.cards.CommonSkills.YakuzaEssenceOfRepair", method = "<ctor>", paramtypez = {})
    public static final class CtorPatch {
        private CtorPatch() {
        }

        @SpirePostfixPatch
        public static void Postfix(YakuzaEssenceOfRepair __instance) {
            __instance.rawDescription = withBlock(__instance.rawDescription);
            __instance.initializeDescription();
        }
    }

    @SpirePatch2(cls = "theYakuza.cards.CommonSkills.YakuzaEssenceOfRepair", method = "use",
        paramtypez = {AbstractPlayer.class, AbstractMonster.class})
    public static final class UsePatch {
        private UsePatch() {
        }

        @SpirePostfixPatch
        public static void Postfix(YakuzaEssenceOfRepair __instance, Object[] __args) {
            AbstractPlayer p = (AbstractPlayer) __args[0];
            AbstractDungeon.actionManager.addToBottom(new GainBlockAction(p, p, 5));
        }
    }
}
