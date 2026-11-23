package cafefix.patches;

import cafefix.CafeSafety;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import spireCafe.CafeRoom;
import spireCafe.abstracts.AbstractCafeInteractable;

/**
 * The original validation throws NameNotSetException / AuthorsNotSetException, which kills the run
 * while the room is being entered. The missing text only affects the hover tooltip, so it is now
 * filled with a placeholder and logged instead of aborting.
 */
@SpirePatch2(clz = CafeRoom.class, method = "checkNameAndAuthors",
        paramtypez = {AbstractCafeInteractable.class, Class.class})
public class SafeNameAndAuthors {

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(Object[] __args) {
        AbstractCafeInteractable interactable =
                __args.length > 0 && __args[0] instanceof AbstractCafeInteractable ? (AbstractCafeInteractable) __args[0] : null;
        Class<?> interactableClz = __args.length > 1 && __args[1] instanceof Class ? (Class<?>) __args[1] : null;
        if (interactable == null) {
            CafeSafety.LOG.warn("[CafeFix] checkNameAndAuthors() received a null interactable.");
            return SpireReturn.Return();
        }
        String fallbackId = interactableClz == null ? "UnknownInteractable" : interactableClz.getSimpleName();
        if (interactable.name == null || interactable.name.isEmpty()) {
            CafeSafety.LOG.warn("[CafeFix] " + fallbackId
                    + " does not set a name; using a placeholder instead of crashing the room.");
            interactable.name = interactable.id == null || interactable.id.isEmpty() ? fallbackId : interactable.id;
        }
        if (interactable.authors == null || interactable.authors.isEmpty()) {
            CafeSafety.LOG.warn("[CafeFix] " + fallbackId
                    + " does not set an author; using a placeholder instead of crashing the room.");
            interactable.authors = "???";
        }
        return SpireReturn.Return();
    }
}
