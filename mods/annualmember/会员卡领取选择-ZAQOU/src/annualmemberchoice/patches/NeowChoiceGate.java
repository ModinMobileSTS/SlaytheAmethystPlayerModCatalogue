package annualmemberchoice.patches;

import annualmemberchoice.MembershipChoice;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.neow.NeowEvent;

/**
 * The first frame the Neow event updates is the moment the run really starts, so the prompt
 * is offered there. While the prompt is open the Neow dialog itself is frozen so no option
 * can be picked behind it.
 */
public class NeowChoiceGate {

    @SpirePatch2(clz = NeowEvent.class, method = "update", paramtypez = {})
    public static class Gate {
        @SpirePrefixPatch
        public static SpireReturn<Void> prefix() {
            MembershipChoice.consider();
            if (MembershipChoice.active) {
                return SpireReturn.Return();
            }
            return SpireReturn.Continue();
        }
    }
}
