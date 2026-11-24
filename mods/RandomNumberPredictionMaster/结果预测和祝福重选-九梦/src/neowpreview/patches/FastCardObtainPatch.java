package neowpreview.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.vfx.FastCardObtainEffect;
import neowpreview.NeowState;

/** 同上，处理快速获得卡牌特效。 */
@SpirePatch2(clz = FastCardObtainEffect.class, method = "update", paramtypez = {})
public class FastCardObtainPatch {

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(FastCardObtainEffect __instance) {
        try {
            if (NeowState.isDropped(__instance)) {
                __instance.isDone = true;
                return SpireReturn.Return(null);
            }
        } catch (Throwable ignored) {
        }
        return SpireReturn.Continue();
    }
}
