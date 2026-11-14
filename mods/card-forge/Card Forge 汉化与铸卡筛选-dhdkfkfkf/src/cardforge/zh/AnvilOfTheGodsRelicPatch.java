package cardforge.zh;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;

/** 众神之砧：遗物描述与选卡界面提示的汉化。 */
public final class AnvilOfTheGodsRelicPatch {
    private AnvilOfTheGodsRelicPatch() {
    }

    @SpirePatch2(
            cls = "com.eternal0.slay.the.spire.mods.relics.AnvilOfTheGodsRelic",
            method = "getUpdatedDescription",
            paramtypez = {}
    )
    public static final class Description {
        @SpirePostfixPatch
        public static String Postfix(String __result) {
            return Texts.useChinese() ? Texts.ANVIL_OF_THE_GODS_DESC_ZH : __result;
        }
    }

    @SpirePatch2(
            cls = "com.eternal0.slay.the.spire.mods.relics.AnvilOfTheGodsRelic",
            method = "getGridMessage",
            paramtypez = {}
    )
    public static final class GridMessage {
        @SpirePostfixPatch
        public static String Postfix(String __result) {
            return Texts.useChinese() ? Texts.ANVIL_OF_THE_GODS_GRID_ZH : __result;
        }
    }
}
