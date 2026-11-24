package neowpreview.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.screens.select.GridCardSelectScreen;
import neowpreview.TransformPredictor;

/**
 * 变形选牌的确认界面：把原版右侧的“随机翻牌”动画换成真正的变形结果。
 * 同时记录网格选牌的开关，用于判断这次选牌是不是变形。
 */
public class GridTransformPatch {

    @SpirePatch2(clz = GridCardSelectScreen.class, method = "update", paramtypez = {})
    public static class UpdatePatch {

        @SpirePostfixPatch
        public static void Postfix(GridCardSelectScreen __instance) {
            try {
                TransformPredictor.patchConfirmCard(__instance);
            } catch (Throwable ignored) {
            }
        }
    }

    @SpirePatch2(clz = GridCardSelectScreen.class, method = "open",
            paramtypez = {CardGroup.class, int.class, String.class,
                    boolean.class, boolean.class, boolean.class, boolean.class})
    public static class OpenPatch {

        @SpirePostfixPatch
        public static void Postfix(GridCardSelectScreen __instance) {
            try {
                TransformPredictor.onGridOpened(__instance);
            } catch (Throwable ignored) {
            }
        }
    }

    @SpirePatch2(clz = GridCardSelectScreen.class, method = "hide", paramtypez = {})
    public static class HidePatch {

        @SpirePostfixPatch
        public static void Postfix() {
            try {
                TransformPredictor.onGridClosed();
            } catch (Throwable ignored) {
            }
        }
    }
}
