package cafefix.patches;

import cafefix.CafeSafety;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import spireCafe.CafeRoom;
import spireCafe.abstracts.AbstractCafeInteractable;

/**
 * The original createInteractable() wraps every reflection failure (missing constructor, a
 * constructor that throws because of game state, a broken third party resident) in a
 * RuntimeException, which aborts the whole room entry. Now the failing resident is replaced by a
 * working one of the same category, and only if that is impossible the original behaviour applies.
 */
@SpirePatch2(clz = CafeRoom.class, method = "createInteractable",
        paramtypez = {Class.class, float.class, float.class})
public class SafeCreateInteractable {

    @SpirePrefixPatch
    @SuppressWarnings("unchecked")
    public static SpireReturn<AbstractCafeInteractable> Prefix(Object[] __args) {
        Class<? extends AbstractCafeInteractable> clz =
                __args.length > 0 && __args[0] instanceof Class ? (Class<? extends AbstractCafeInteractable>) __args[0] : null;
        float x = __args.length > 1 && __args[1] instanceof Float ? ((Float) __args[1]).floatValue() : 0.0f;
        float y = __args.length > 2 && __args[2] instanceof Float ? ((Float) __args[2]).floatValue() : 0.0f;
        AbstractCafeInteractable made = CafeSafety.instantiate(clz, x, y);
        if (made == null) {
            made = CafeSafety.fallback(clz, x, y);
        }
        if (made == null) {
            return SpireReturn.Continue();
        }
        return SpireReturn.Return(made);
    }
}
