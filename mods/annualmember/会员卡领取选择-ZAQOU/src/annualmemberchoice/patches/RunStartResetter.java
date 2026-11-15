package annualmemberchoice.patches;

import annualmemberchoice.MembershipChoice;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import java.util.ArrayList;

/** Clears the per-run state when a brand new run is created (continuing a save is not touched). */
public class RunStartResetter {

    @SpirePatch2(clz = AbstractDungeon.class, method = "<ctor>",
            paramtypez = {String.class, String.class, AbstractPlayer.class, ArrayList.class})
    public static class Reset {
        @SpirePrefixPatch
        public static void prefix() {
            MembershipChoice.resetForRun();
        }
    }
}
