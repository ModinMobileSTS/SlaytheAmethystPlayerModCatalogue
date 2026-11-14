package cardrefreshtweak.patches;

import cardrefreshtweak.RefreshState;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;

/**
 * Marks that a run was restored from a save file.
 *
 * The refresh bookkeeping has to know that a load happened: the reward screen that was open when
 * the player saved is reopened afterwards, and it must be treated as the same reward. Within one
 * session a second card reward with the same floor and the same remaining count can only be a
 * genuinely new reward (for example the second card reward from Prayer Wheel or the second wave of
 * the Colosseum), which is told apart by the per session bookkeeping cleared here.
 */
@SpirePatch2(
    cls = "com.megacrit.cardcrawl.dungeons.AbstractDungeon",
    method = "loadSave",
    paramtypez = { com.megacrit.cardcrawl.saveAndContinue.SaveFile.class }
)
public final class RunLoadedPatch {
    private RunLoadedPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix() {
        RefreshState.onRunLoaded();
    }
}
