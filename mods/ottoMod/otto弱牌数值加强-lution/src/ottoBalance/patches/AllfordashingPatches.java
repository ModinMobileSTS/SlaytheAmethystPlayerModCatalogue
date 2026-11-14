package ottoBalance.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import ottoMod.cards.Allfordashing;
import ottoMod.effects.PushWheelEffect;

/**
 * 全力冲刺：原本每点能量只推轮 1 次并给 1 点怒气，收益远低于普通攻击。
 * 改为每点能量推轮 2 次（怒气不变），并同步修改卡面说明。
 */
public final class AllfordashingPatches {

    private static final String BASE_DESC = " ottomod:推轮  2X 次。获得 X 点 ottomod:怒气 。";
    private static final String UPGRADED_DESC = " ottomod:推轮  2X+2 次。获得 X+1 点 ottomod:怒气 。";

    private AllfordashingPatches() {
    }

    /** 构造时替换卡面说明，使其与翻倍后的推轮次数一致。 */
    @SpirePatch2(clz = Allfordashing.class, method = "<ctor>", paramtypez = {})
    public static final class AllfordashingText {
        @SpirePostfixPatch
        public static void Postfix(Allfordashing __instance) {
            __instance.rawDescription = BASE_DESC;
            __instance.initializeDescription();
        }
    }

    /** 升级会重写 rawDescription，这里再覆盖一次为升级后的说明。 */
    @SpirePatch2(clz = Allfordashing.class, method = "upgrade", paramtypez = {})
    public static final class AllfordashingUpgradeText {
        @SpirePostfixPatch
        public static void Postfix(Allfordashing __instance) {
            if (__instance.upgraded) {
                __instance.rawDescription = UPGRADED_DESC;
                __instance.initializeDescription();
            }
        }
    }

    /** 在原有推轮结算之后追加等量的推轮，使总次数翻倍（含化学 X 与升级加成）。 */
    @SpirePatch2(clz = Allfordashing.class, method = "use", paramtypez = {AbstractPlayer.class, AbstractMonster.class})
    public static final class AllfordashingExtraPush {
        @SpirePostfixPatch
        public static void Postfix(Allfordashing __instance) {
            int energyAmount = __instance.energyOnUse;
            if (AbstractDungeon.player != null && AbstractDungeon.player.hasRelic("Chemical X")) {
                energyAmount += 2;
            }
            if (__instance.upgraded) {
                energyAmount += 1;
            }
            if (energyAmount > 0) {
                new PushWheelEffect().trigger(energyAmount);
            }
        }
    }
}
