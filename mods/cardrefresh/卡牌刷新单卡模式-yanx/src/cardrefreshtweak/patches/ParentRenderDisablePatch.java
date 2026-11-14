package cardrefreshtweak.patches;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;

@SpirePatch2(
    cls = "cardrefresh.RefreshButton",
    method = "onScreenRender",
    paramtypez = { SpriteBatch.class }
)
public final class ParentRenderDisablePatch {

    private ParentRenderDisablePatch() {
    }

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix() {
        return SpireReturn.Return();
    }
}
