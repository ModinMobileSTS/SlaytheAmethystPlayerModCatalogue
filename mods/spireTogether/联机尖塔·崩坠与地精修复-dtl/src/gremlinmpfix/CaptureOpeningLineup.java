package gremlinmpfix;

import basemod.BaseMod;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

@SpirePatch2(clz = BaseMod.class, method = "publishStartBattle", paramtypez = {AbstractRoom.class})
public final class CaptureOpeningLineup {
    private CaptureOpeningLineup() {}

    @SpirePostfixPatch
    public static void Postfix(Object[] __args) {
        GremlinBattleState.capture((AbstractRoom) __args[0]);
    }
}
