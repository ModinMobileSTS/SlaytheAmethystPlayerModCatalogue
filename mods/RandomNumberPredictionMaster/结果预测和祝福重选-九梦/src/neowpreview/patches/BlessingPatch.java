package neowpreview.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.neow.NeowEvent;
import neowpreview.NeowState;

/** 完整祝福（4 个选项）生成后立即算出每个选项的具体结果。 */
@SpirePatch2(clz = NeowEvent.class, method = "blessing", paramtypez = {})
public class BlessingPatch {

    @SpirePostfixPatch
    public static void Postfix(NeowEvent __instance) {
        try {
            NeowState.onOffered(__instance);
        } catch (Throwable ignored) {
        }
    }
}
