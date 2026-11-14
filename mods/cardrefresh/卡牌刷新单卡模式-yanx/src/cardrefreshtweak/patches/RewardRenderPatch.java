package cardrefreshtweak.patches;

import cardrefreshtweak.RefreshUi;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.screens.CardRewardScreen;

@SpirePatch2(
    cls = "com.megacrit.cardcrawl.screens.CardRewardScreen",
    method = "render",
    paramtypez = { SpriteBatch.class }
)
public final class RewardRenderPatch {

    private RewardRenderPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(CardRewardScreen __instance, Object[] __args) {
        SpriteBatch sb = __args != null && __args.length > 0 && __args[0] instanceof SpriteBatch
                ? (SpriteBatch) __args[0]
                : null;
        RefreshUi.inst().onRender(__instance, sb);
    }
}
