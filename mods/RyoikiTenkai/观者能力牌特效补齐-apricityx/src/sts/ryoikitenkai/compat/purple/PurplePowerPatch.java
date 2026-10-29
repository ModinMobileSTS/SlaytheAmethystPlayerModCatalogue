package sts.ryoikitenkai.compat.purple;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.powers.AbstractPower;

/**
 * 能力挂到生物身上时启动常驻光环，移除/死亡时停止。
 * <p>
 * 直接挂在 AbstractPower 上，而不是走父模组的 powerImpls 派发表 ——
 * 后者只包含父模组自己扫描到的实现，本补丁的类不在其内。
 * 只挂低频钩子（施加 / 移除 / 死亡），不挂每帧的 update。
 */
public final class PurplePowerPatch {
    private PurplePowerPatch() {
    }

    @SpirePatch2(
        cls = "com.megacrit.cardcrawl.powers.AbstractPower",
        method = "onInitialApplication",
        paramtypez = {}
    )
    public static class OnInitialApplication {
        @SpirePostfixPatch
        public static void Postfix(AbstractPower __instance) {
            PurpleEffectRegistry.onPowerApplied(__instance);
        }
    }

    @SpirePatch2(
        cls = "com.megacrit.cardcrawl.powers.AbstractPower",
        method = "onRemove",
        paramtypez = {}
    )
    public static class OnRemove {
        @SpirePostfixPatch
        public static void Postfix(AbstractPower __instance) {
            PurpleEffectRegistry.onPowerRemoved(__instance);
        }
    }

    @SpirePatch2(
        cls = "com.megacrit.cardcrawl.powers.AbstractPower",
        method = "onDeath",
        paramtypez = {}
    )
    public static class OnDeath {
        @SpirePostfixPatch
        public static void Postfix(AbstractPower __instance) {
            PurpleEffectRegistry.onPowerRemoved(__instance);
        }
    }
}
