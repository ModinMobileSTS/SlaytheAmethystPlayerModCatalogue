package natsu.translation;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;

@SpirePatch2(cls = "natsu.Natsu", method = "getSensoryStoneText", paramtypez = {})
public final class ChineseSensoryStonePatch {
    private ChineseSensoryStonePatch() { }

    @SpirePostfixPatch
    public static String Postfix(String __result, natsu.Natsu __instance) {
        return ChineseText.enabled() ? "一间摆着两把椅子的房间。一个叫不出名字的声音。 NL 海洋带走了名字，却带不走那份温暖。 NL 你会再次为某个人留出位置。" : __result;
    }
}
