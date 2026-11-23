package cafefix.patches;

import cafefix.CafeSafety;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import spireCafe.CafeRoom;
import spireCafe.abstracts.AbstractCafeInteractable;

/**
 * Cafe interactables expose an optional static canSpawn(); the original implementation wraps any
 * exception from that hook in a RuntimeException, which aborts entering the room. A throwing hook
 * is now logged and treated as "do not spawn this one" instead.
 */
@SpirePatch2(clz = CafeRoom.class, method = "canSpawn", paramtypez = {Class.class})
public class SafeCanSpawn {

    @SpirePrefixPatch
    public static SpireReturn<Boolean> Prefix(Object[] __args) {
        Class<?> clz = __args.length > 0 && __args[0] instanceof Class ? (Class<?>) __args[0] : null;
        return SpireReturn.Return(Boolean.valueOf(CafeSafety.safeCanSpawn(clz)));
    }
}
