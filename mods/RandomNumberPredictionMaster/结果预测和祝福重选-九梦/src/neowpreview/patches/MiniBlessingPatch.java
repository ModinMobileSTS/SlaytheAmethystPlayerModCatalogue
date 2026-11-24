package neowpreview.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.neow.NeowEvent;
import neowpreview.Config;
import neowpreview.NeowState;
import neowpreview.Reflect;

/**
 * 两选项的“小型祝福”（首次通关前原版只给两个）。
 *
 * <p>设置里开了「涅奥始终有四个选项」时，直接改走原版的 blessing()，四个选项；
 * 开关是按当前配置实时判断的，改完立刻生效，不用重开游戏。</p>
 */
@SpirePatch2(clz = NeowEvent.class, method = "miniBlessing", paramtypez = {})
public class MiniBlessingPatch {

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(NeowEvent __instance) {
        try {
            if (Config.alwaysFour()) {
                Reflect.call(__instance, "blessing");
                return SpireReturn.Return(null);
            }
        } catch (Throwable ignored) {
        }
        return SpireReturn.Continue();
    }

    @SpirePostfixPatch
    public static void Postfix(NeowEvent __instance) {
        try {
            NeowState.onOffered(__instance);
        } catch (Throwable ignored) {
        }
    }
}
