package goldenglowpatch.cardtweaks;

import Goldenglow.cards.Skill.TargetLockOn;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

/**
 * 锁定攻击（TargetLockOn）调整：
 * 原版 1 费…这里仅调整升级版：跟踪锁定 2 层 -> 1 层，浮标 1 个 -> 2 个。
 */
public final class TargetLockOnPatch {

    private TargetLockOnPatch() {
    }

    /** 升级后跟踪锁定保持 1 层，并把说明改成"给予 2 个浮标"。 */
    @SpirePatch2(
        cls = "Goldenglow.cards.Skill.TargetLockOn",
        method = "limitedUpgrade",
        paramtypez = {}
    )
    public static final class UpgradePatch {
        private UpgradePatch() {
        }

        @SpirePostfixPatch
        public static void Postfix(TargetLockOn __instance) {
            __instance.baseMagicNumber = 1;
            __instance.magicNumber = 1;
            __instance.upgradedMagicNumber = false;
            __instance.rawDescription = upgradedDescription();
            // AbstractCard 的 upgrade() 会在 limitedUpgrade() 之后调用 initializeDescription()
        }
    }

    /** 升级后额外给予 1 个浮标：原版已给予 1 个，合计 2 个。 */
    @SpirePatch2(
        cls = "Goldenglow.cards.Skill.TargetLockOn",
        method = "use",
        paramtypez = {AbstractPlayer.class, AbstractMonster.class}
    )
    public static final class UsePatch {
        private UsePatch() {
        }

        @SpirePostfixPatch
        public static void Postfix(TargetLockOn __instance, Object[] __args) {
            AbstractMonster target = (AbstractMonster) __args[1];
            if (__instance.upgraded && target != null) {
                __instance.channelBuoy(1, target);
            }
        }
    }

    private static String upgradedDescription() {
        if (Settings.language == Settings.GameLanguage.ZHS || Settings.language == Settings.GameLanguage.ZHT) {
            return "给予 !M! 层 跟踪锁定 。 NL 给予 2 个 goldenglow:浮标 。";
        }
        if (Settings.language == Settings.GameLanguage.KOR) {
            return "조준을 !M! 부여합니다. NL 적에게 goldenglow:유닛 을 2개 영창합니다.";
        }
        return "Apply !M! Lock-On. NL Channel 2 goldenglow:Buoy for the enemy.";
    }
}
