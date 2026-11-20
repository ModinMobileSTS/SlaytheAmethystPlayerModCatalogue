package patches;

import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import javassist.expr.ExprEditor;

@SpirePatch2(cls = "code.Event.Event_Quiz", method = "buttonEffect", paramtypez = {int.class})
public final class QuizEventTextPatch {
    @SpireInstrumentPatch
    public static ExprEditor Instrument() {
        return new TargetedTextEditor(true);
    }
}
