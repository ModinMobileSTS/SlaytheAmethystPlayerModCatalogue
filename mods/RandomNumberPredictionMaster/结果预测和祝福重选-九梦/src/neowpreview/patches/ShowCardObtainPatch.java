package neowpreview.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;
import neowpreview.NeowState;

/** 撤回后，正在播放的“获得卡牌”特效不再把牌塞进卡组。 */
@SpirePatch2(clz = ShowCardAndObtainEffect.class, method = "update", paramtypez = {})
public class ShowCardObtainPatch {

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(ShowCardAndObtainEffect __instance) {
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
