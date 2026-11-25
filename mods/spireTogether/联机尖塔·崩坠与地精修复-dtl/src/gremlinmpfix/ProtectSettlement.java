package gremlinmpfix;

import basemod.BaseMod;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

@SpirePatch2(clz = BaseMod.class, method = "publishPostBattle", paramtypez = {AbstractRoom.class})
public final class ProtectSettlement {
    private ProtectSettlement() {}

    @SpireInstrumentPatch
    public static ExprEditor Instrument() {
        return new ExprEditor() {
            @Override
            public void edit(MethodCall call) throws CannotCompileException {
                if ("basemod.interfaces.PostBattleSubscriber".equals(call.getClassName())
                        && "receivePostBattle".equals(call.getMethodName())) {
                    call.replace("{ gremlinmpfix.GremlinBattleState.settle($0, $1); }");
                }
            }
        };
    }
}
