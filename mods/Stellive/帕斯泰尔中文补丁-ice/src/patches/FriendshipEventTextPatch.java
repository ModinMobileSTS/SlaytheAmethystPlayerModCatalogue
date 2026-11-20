package patches;

import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import javassist.expr.ExprEditor;

@SpirePatch2(cls = "code.Event.Event_Friendship", method = "upgradeOneCard", paramtypez = {})
public final class FriendshipEventTextPatch {
    @SpireInstrumentPatch
    public static ExprEditor Instrument() {
        return new TargetedTextEditor(false);
    }
}
