package therailgunfix;

import TheRailgun.cards.Aerial_Shot;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;

@SpirePatch2(clz = Aerial_Shot.class, method = "<ctor>", paramtypez = {})
public class AerialShotPortraitFix {
    @SpirePostfixPatch
    public static void postfix(Aerial_Shot __instance) {
        // The large art is shipped as "aerial shot_p.png" (with a space), while
        // BaseMod derives it from the card's texture path "aerial_shot.png".
        __instance.textureImg = "TheRailgunResources/images/cards/aerial shot.png";
    }
}
