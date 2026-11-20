package patches;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.megacrit.cardcrawl.core.Settings;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

@SpirePatch2(cls = "code.ui.TutorialButton", method = "render", paramtypez = {SpriteBatch.class})
public final class TutorialButtonTextPatch {
    @SpireInstrumentPatch
    public static ExprEditor Instrument() {
        return new ExprEditor() {
            @Override
            public void edit(MethodCall call) throws CannotCompileException {
                if (call.getClassName().equals("com.megacrit.cardcrawl.helpers.FontHelper")
                        && call.getMethodName().equals("renderFontCentered")
                        && call.getSignature().equals("(Lcom/badlogic/gdx/graphics/g2d/SpriteBatch;Lcom/badlogic/gdx/graphics/g2d/BitmapFont;Ljava/lang/String;FFLcom/badlogic/gdx/graphics/Color;)V")) {
                    call.replace("{ $3 = patches.TutorialButtonTextPatch.label($3); $proceed($$); }");
                }
            }
        };
    }

    public static String label(String original) {
        return Settings.language == Settings.GameLanguage.ZHS && "튜토리얼".equals(original) ? "教程" : original;
    }
}
