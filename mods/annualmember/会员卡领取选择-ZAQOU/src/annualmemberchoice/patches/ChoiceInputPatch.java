package annualmemberchoice.patches;

import annualmemberchoice.MembershipChoice;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

/** Feeds the prompt its input before the dungeon, top panel and Neow dialog look at it. */
public class ChoiceInputPatch {

    @SpirePatch2(clz = AbstractDungeon.class, method = "update", paramtypez = {})
    public static class Input {
        @SpirePrefixPatch
        public static void prefix() {
            MembershipChoice.tick();
        }
    }
}
