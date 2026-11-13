package tkfix.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import java.util.IdentityHashMap;
import tkfix.Diag;
import tkfix.SkinApply;

/**
 * 保险钩子：
 * 1) 玩家角色第一次 update 时无条件把骨架重设为所选皮肤
 *    （母模组会在角色创建之后把骨架还原成默认外观，战士就是这样丢的）；
 * 2) 之后每隔一段时间复查一次，被还原就再补上，防止进战斗后又变回原版。
 */
@SpirePatch2(clz = AbstractPlayer.class, method = "update", paramtypez = {})
public class PlayerSkinEnforcePatch {

    private static final IdentityHashMap<Object, Boolean> DONE = new IdentityHashMap<Object, Boolean>();
    private static final IdentityHashMap<Object, Integer> TICK = new IdentityHashMap<Object, Integer>();

    @SpirePrefixPatch
    public static void Prefix(AbstractPlayer __instance) {
        try {
            if (__instance == null || !SkinApply.isVanillaSkinCharacter(__instance)) {
                return;
            }
            if (!DONE.containsKey(__instance)) {
                DONE.put(__instance, Boolean.TRUE);
                SkinApply.ensureOnce(__instance, "enforce", true);
                return;
            }
            Integer n = TICK.get(__instance);
            int tick = n == null ? 0 : n + 1;
            TICK.put(__instance, tick);
            if (tick % 45 == 0) {
                SkinApply.guard(__instance);
            }
        } catch (Throwable t) {
            Diag.error("enforce", t);
        }
    }
}
