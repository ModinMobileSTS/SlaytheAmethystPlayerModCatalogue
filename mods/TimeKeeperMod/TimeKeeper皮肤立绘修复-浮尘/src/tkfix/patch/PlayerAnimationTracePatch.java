package tkfix.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import tkfix.Diag;

/**
 * 诊断：记录玩家角色每一次 loadAnimation 调用（谁在什么时候换掉了骨架）。
 */
@SpirePatch2(clz = AbstractCreature.class, method = "loadAnimation",
        paramtypez = {String.class, String.class, float.class})
public class PlayerAnimationTracePatch {

    @SpirePrefixPatch
    public static void Prefix(AbstractCreature __instance, Object[] __args) {
        try {
            if (!(__instance instanceof AbstractPlayer)) {
                return;
            }
            Diag.log("anim", __instance.getClass().getSimpleName() + " atlas=" + __args[0]
                    + " json=" + __args[1] + " scale=" + __args[2]);
        } catch (Throwable t) {
            Diag.error("anim", t);
        }
    }
}
