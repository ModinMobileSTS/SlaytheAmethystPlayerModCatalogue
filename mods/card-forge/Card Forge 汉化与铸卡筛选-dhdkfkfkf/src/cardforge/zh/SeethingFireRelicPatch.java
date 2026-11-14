package cardforge.zh;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;

/** 灼热之火：遗物描述与选卡界面提示的汉化。 */
public final class SeethingFireRelicPatch {
    private SeethingFireRelicPatch() {
    }

    @SpirePatch2(
            cls = "com.eternal0.slay.the.spire.mods.relics.SeethingFireRelic",
            method = "getUpdatedDescription",
            paramtypez = {}
    )
    public static final class Description {
        @SpirePostfixPatch
        public static String Postfix(String __result) {
            return Texts.useChinese() ? Texts.SEETHING_FIRE_DESC_ZH : __result;
        }
    }

    @SpirePatch2(
            cls = "com.eternal0.slay.the.spire.mods.relics.SeethingFireRelic",
            method = "getGridMessage",
            paramtypez = {}
    )
    public static final class GridMessage {
        @SpirePostfixPatch
        public static String Postfix(String __result) {
            return Texts.useChinese() ? Texts.SEETHING_FIRE_GRID_ZH : __result;
        }
    }
}
