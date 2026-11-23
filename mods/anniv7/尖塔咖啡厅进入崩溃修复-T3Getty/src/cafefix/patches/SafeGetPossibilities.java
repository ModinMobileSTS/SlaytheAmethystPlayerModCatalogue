package cafefix.patches;

import cafefix.CafeSafety;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import java.util.List;
import spireCafe.CafeRoom;
import spireCafe.abstracts.AbstractCafeInteractable;

/**
 * Replaces the candidate gathering of the cafe room with a version that cannot throw.
 *
 * The original builds the list from a stream whose filter calls canSpawn(), and the room entry
 * code then takes element 0. Two things used to abort the room: an exception escaping canSpawn()
 * and a completely filtered out list (IndexOutOfBoundsException after the "seen" set was cleared).
 * Here the filter is applied in tiers, so a category still yields a candidate when everything
 * was already seen or temporarily unavailable.
 */
@SpirePatch2(clz = CafeRoom.class, method = "getPossibilities", paramtypez = {Class.class})
public class SafeGetPossibilities {

    @SpirePrefixPatch
    @SuppressWarnings("unchecked")
    public static SpireReturn<List> Prefix(Object[] __args) {
        Class<? extends AbstractCafeInteractable> requested =
                __args.length > 0 && __args[0] instanceof Class ? (Class<? extends AbstractCafeInteractable>) __args[0] : null;
        List<Class<? extends AbstractCafeInteractable>> candidates = CafeSafety.pick(requested, true, true);
        if (candidates.isEmpty()) {
            candidates = CafeSafety.pick(requested, false, true);
        }
        if (candidates.isEmpty()) {
            candidates = CafeSafety.pick(requested, false, false);
        }
        if (candidates.isEmpty()) {
            CafeSafety.LOG.error("[CafeFix] No registered interactable of type "
                    + (requested == null ? "<null>" : requested.getName()) + " is available.");
        }
        return SpireReturn.Return(candidates);
    }
}
