package goldenglowpatch.buoyvalue;

import Goldenglow.orbs.Buoy;
import basemod.ReflectionHacks;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.orbs.AbstractOrb;

/**
 * 调整浮标(Buoy)充能球的伤害/格挡数值。
 * 原版：被动 2（玩家获得格挡 / 敌人受到伤害），激发 5。
 * 现在：被动 3，激发 6。
 * 浮标的数值都取自 AbstractOrb 的 basePassiveAmount / baseEvokeAmount，
 * 因此只需要在构造完成后覆盖这两个基础值并刷新描述文本；
 * 后续的数值强化（如火花凝集、驭械）仍在其上正常叠加。
 */
@SpirePatch2(
    cls = "Goldenglow.orbs.Buoy",
    method = "<ctor>",
    paramtypez = {}
)
public final class BuoyValuePatch {
    public static final int BUOY_PASSIVE_VALUE = 3;
    public static final int BUOY_EVOKE_VALUE = 6;

    private BuoyValuePatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(Buoy __instance) {
        ReflectionHacks.setPrivate(__instance, AbstractOrb.class, "basePassiveAmount", BUOY_PASSIVE_VALUE);
        ReflectionHacks.setPrivate(__instance, AbstractOrb.class, "baseEvokeAmount", BUOY_EVOKE_VALUE);
        __instance.passiveAmount = BUOY_PASSIVE_VALUE;
        __instance.evokeAmount = BUOY_EVOKE_VALUE;
        __instance.updateDescription();
    }
}
