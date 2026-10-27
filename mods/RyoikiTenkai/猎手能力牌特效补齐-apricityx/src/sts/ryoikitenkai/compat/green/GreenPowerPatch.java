package sts.ryoikitenkai.compat.green;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.powers.AbstractPower;

/**
 * 能力挂到生物身上时启动常驻特效，移除/死亡时停止。
 * <p>
 * 直接挂在 AbstractPower 上，而不是走父模组的 powerImpls 派发表 ——
 * 后者只包含父模组自己扫描到的实现，本补丁的类不在其内。
 * 只挂低频钩子（施加 / 移除 / 死亡），不挂每帧的 update。
 */
public final class GreenPowerPatch {
    private GreenPowerPatch() {
    }

    /** 首次施加能力：立即建立常驻特效。 */
    @SpirePatch2(
        cls = "com.megacrit.cardcrawl.powers.AbstractPower",
        method = "onInitialApplication",
        paramtypez = {}
    )
    public static class OnInitialApplication {
        @SpirePostfixPatch
        public static void Postfix(AbstractPower __instance) {
            GreenEffectRegistry.onPowerApplied(__instance);
        }
    }

    /** 能力被移除时停止常驻特效。 */
    @SpirePatch2(
        cls = "com.megacrit.cardcrawl.powers.AbstractPower",
        method = "onRemove",
        paramtypez = {}
    )
    public static class OnRemove {
        @SpirePostfixPatch
        public static void Postfix(AbstractPower __instance) {
            GreenEffectRegistry.onPowerRemoved(__instance);
        }
    }

    /** 生物死亡时也要清掉，否则尸体上会残留特效。 */
    @SpirePatch2(
        cls = "com.megacrit.cardcrawl.powers.AbstractPower",
        method = "onDeath",
        paramtypez = {}
    )
    public static class OnDeath {
        @SpirePostfixPatch
        public static void Postfix(AbstractPower __instance) {
            GreenEffectRegistry.onPowerRemoved(__instance);
        }
    }
}
