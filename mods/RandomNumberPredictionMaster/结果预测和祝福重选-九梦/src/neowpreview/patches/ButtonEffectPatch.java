package neowpreview.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.events.RoomEventDialog;
import com.megacrit.cardcrawl.neow.NeowEvent;
import neowpreview.NeowState;

/**
 * 拦截涅奥事件的选项点击：
 * 选项生效前存快照，生效后加一行“撤回并重新选择”，点它就还原状态并回到祝福列表。
 */
@SpirePatch2(clz = NeowEvent.class, method = "buttonEffect", paramtypez = {int.class})
public class ButtonEffectPatch {

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(NeowEvent __instance, Object[] __args) {
        int buttonPressed = __args.length > 0 && __args[0] instanceof Integer ? (Integer) __args[0] : -1;
        try {
            if (!NeowState.isActive(__instance)) {
                if (NeowState.phase != NeowState.Phase.NONE) {
                    NeowState.reset();
                }
                return SpireReturn.Continue();
            }
            if (NeowState.handleUndoClick(__instance, buttonPressed)) {
                return SpireReturn.Return(null);
            }
            if (NeowState.phase == NeowState.Phase.CHOOSING
                    && buttonPressed >= 0
                    && buttonPressed < NeowState.previews.size()
                    && buttonPressed < RoomEventDialog.optionList.size()) {
                NeowState.beforeChoice(__instance);
            }
        } catch (Throwable ignored) {
        }
        return SpireReturn.Continue();
    }

    @SpirePostfixPatch
    public static void Postfix(NeowEvent __instance) {
        try {
            NeowState.afterChoice(__instance);
        } catch (Throwable ignored) {
        }
    }
}
