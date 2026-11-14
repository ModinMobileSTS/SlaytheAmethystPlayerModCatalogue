package cardforge.zh;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;

/** 雷神之锤：遗物描述与选卡界面提示的汉化。 */
public final class HammerOfThorRelicPatch {
    private HammerOfThorRelicPatch() {
    }

    @SpirePatch2(
            cls = "com.eternal0.slay.the.spire.mods.relics.HammerOfThorRelic",
            method = "getUpdatedDescription",
            paramtypez = {}
    )
    public static final class Description {
        @SpirePostfixPatch
        public static String Postfix(String __result) {
            return Texts.useChinese() ? Texts.HAMMER_OF_THOR_DESC_ZH : __result;
        }
    }

    @SpirePatch2(
            cls = "com.eternal0.slay.the.spire.mods.relics.HammerOfThorRelic",
            method = "getGridMessage",
            paramtypez = {}
    )
    public static final class GridMessage {
        @SpirePostfixPatch
        public static String Postfix(String __result) {
            return Texts.useChinese() ? Texts.HAMMER_OF_THOR_GRID_ZH : __result;
        }
    }
}
