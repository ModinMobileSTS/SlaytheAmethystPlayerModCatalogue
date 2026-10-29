/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch
 *  com.evacipated.cardcrawl.modthespire.lib.SpirePatch2
 *  javassist.CannotCompileException
 *  javassist.expr.ExprEditor
 *  javassist.expr.MethodCall
 */
package stslibpopupfix;

import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class SelectCardsCenteredActionPopupFix {

    @SpirePatch2(cls="com.evacipated.cardcrawl.mod.stslib.actions.common.SelectCardsCenteredAction", method="update")
    public static class NoDurationTickWhileGridSelectScreenIsUp {
        @SpireInstrumentPatch
        public static ExprEditor Instrument() {
            return new ExprEditor(){

                public void edit(MethodCall methodCall) throws CannotCompileException {
                    if (methodCall.getMethodName().equals("tickDuration")) {
                        methodCall.replace("if (com.megacrit.cardcrawl.dungeons.AbstractDungeon.screen != null && com.megacrit.cardcrawl.dungeons.AbstractDungeon.screen == com.megacrit.cardcrawl.dungeons.AbstractDungeon$CurrentScreen.GRID) { if (this.duration == this.startDuration) { this.duration -= 0.001f; }} else { $proceed();}");
                    }
                }
            };
        }
    }
}
