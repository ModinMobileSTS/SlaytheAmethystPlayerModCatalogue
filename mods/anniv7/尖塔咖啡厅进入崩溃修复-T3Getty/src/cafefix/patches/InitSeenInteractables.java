package cafefix.patches;

import cafefix.CafeSafety;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import spireCafe.CafeRoom;

/**
 * Anniv7Mod.currentRunSeenInteractables is only built by the generateSeeds hook and the save field.
 * When the cafe is entered before either ran (unusual load orders, a save made by an older version),
 * the candidate stream dereferenced it and threw a NullPointerException. Rebuild it first.
 */
@SpirePatch2(clz = CafeRoom.class, method = "onEnterRoom", paramtypez = {})
public class InitSeenInteractables {

    @SpirePrefixPatch
    public static void Prefix() {
        CafeSafety.ensureSeenInteractables();
    }
}
