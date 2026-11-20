package skulzh.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import skulzh.SkulChinese;

@SpirePatch2(
    cls = "skulmod.util.Patches.RenderPatches.SingleCardViewPopupRenderCardTypeTextPatch",
    method = "Insert",
    paramtypez = { com.megacrit.cardcrawl.screens.SingleCardViewPopup.class, com.badlogic.gdx.graphics.g2d.SpriteBatch.class, String[].class }
)
public final class SkullPopupTypeTranslation {
    @SpirePostfixPatch
    public static void Postfix(Object[] __args) {
        String[] label = (String[]) __args[2];
        if (SkulChinese.enabled() && "SKULL".equals(label[0])) label[0] = "头骨";
    }
}
