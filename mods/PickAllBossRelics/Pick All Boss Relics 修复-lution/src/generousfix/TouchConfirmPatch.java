package generousfix;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.blights.AbstractBlight;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.screens.select.BossRelicSelectScreen;
import com.megacrit.cardcrawl.ui.buttons.ConfirmButton;
import generousrelics.Config;

/**
 * In touchscreen mode vanilla only shows the confirm button and stores the relic in
 * BossRelicSelectScreen.touchRelic; the relic is obtained from the overridden
 * updateConfirmButton(). The parent mod replaces that method and never calls
 * touchRelic.bossObtainLogic(), so confirming a boss relic closed the whole selection
 * without giving the player anything.
 *
 * This prefix runs before BossRelicSelectScreen.update()'s body (so before the parent
 * mod's updateConfirmButton patch) and performs the vanilla confirm handling itself.
 */
@SpirePatch2(clz = BossRelicSelectScreen.class, method = "update", paramtypez = {})
public class TouchConfirmPatch {

    @SpirePrefixPatch
    public static void Prefix(BossRelicSelectScreen __instance) {
        if (__instance.touchRelic == null && __instance.touchBlight == null) {
            return;
        }
        if (!Config.isMultiBossRelicEnabled()) {
            return;
        }
        ConfirmButton confirm = __instance.confirmButton;
        if (confirm == null) {
            return;
        }
        confirm.isDisabled = false;
        confirm.show();
        confirm.update();
        if (confirm.hb.clicked) {
            confirm.hb.clicked = false;
            // Consume the release so the parent mod's patch cannot close the selection
            // with the same input this frame.
            InputHelper.justReleasedClickLeft = false;
            AbstractRelic relic = __instance.touchRelic;
            AbstractBlight blight = __instance.touchBlight;
            __instance.touchRelic = null;
            __instance.touchBlight = null;
            confirm.hide();
            if (relic != null) {
                relic.bossObtainLogic();
            }
            if (blight != null) {
                blight.bossObtainLogic();
            }
            return;
        }
        if (InputHelper.justReleasedClickLeft) {
            __instance.touchRelic = null;
            __instance.touchBlight = null;
            confirm.hide();
        }
    }
}
