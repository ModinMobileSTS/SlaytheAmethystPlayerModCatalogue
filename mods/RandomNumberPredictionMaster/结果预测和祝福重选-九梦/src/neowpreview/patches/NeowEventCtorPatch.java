package neowpreview.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.neow.NeowEvent;
import neowpreview.NeowState;

/** 每次进入涅奥事件都清空上一局残留的状态。 */
@SpirePatch2(clz = NeowEvent.class, method = "<ctor>", paramtypez = {boolean.class})
public class NeowEventCtorPatch {

    @SpirePrefixPatch
    public static void Prefix() {
        try {
            NeowState.reset();
        } catch (Throwable ignored) {
        }
    }

    /** 读档直接进入“涅奥已经领过祝福”的界面时，把撤回选项放回去（SL 之后也能撤回）。 */
    @SpirePostfixPatch
    public static void Postfix(NeowEvent __instance) {
        try {
            NeowState.onCreated(__instance);
        } catch (Throwable ignored) {
        }
    }
}
