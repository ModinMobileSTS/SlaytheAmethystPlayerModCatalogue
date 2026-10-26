package sts.ryoikitenkai.compat.red;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.powers.MetallicizePower;

@SpirePatch2(
    cls = "com.megacrit.cardcrawl.powers.MetallicizePower",
    method = "atEndOfTurnPreEndTurnCards",
    paramtypez = { boolean.class }
)
public final class MetallicizeBlockPatch {
    private MetallicizeBlockPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(MetallicizePower __instance, Object[] __args) {
        if (__instance.owner == null || !__instance.owner.isPlayer) {
            return;
        }
        RedEffectRegistry.blockMetallicize(__instance.owner);
    }
}
