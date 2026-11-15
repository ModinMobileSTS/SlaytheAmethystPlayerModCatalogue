package annualmemberchoice.patches;

import annualmemberchoice.MembershipChoice;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.relics.AbstractRelic;

/**
 * The parent mod hands out the membership card from a postfix on
 * AbstractPlayer.initializeStarterRelics. Arm a flag at the very start of that method and
 * swallow the membership card while it is being obtained, so the player gets to choose in
 * the Neow room instead.
 */
public class StarterCardSuppressor {

    @SpirePatch2(clz = AbstractPlayer.class, method = "initializeStarterRelics",
            paramtypez = {AbstractPlayer.PlayerClass.class})
    public static class Arm {
        @SpirePrefixPatch
        public static void prefix() {
            MembershipChoice.armForRun();
        }
    }

    @SpirePatch2(clz = AbstractRelic.class, method = "instantObtain",
            paramtypez = {AbstractPlayer.class, int.class, boolean.class})
    public static class Suppress {
        @SpirePrefixPatch
        public static SpireReturn<Void> prefix(AbstractRelic __instance) {
            if (MembershipChoice.shouldSuppress(__instance)) {
                return SpireReturn.Return();
            }
            return SpireReturn.Continue();
        }
    }
}
