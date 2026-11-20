package skulzh.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import skulzh.SkulChinese;

@SpirePatch2(
    cls = "skulmod.util.Patches.RenderPatches.AbstractCardRenderTypePatch",
    method = "Insert",
    paramtypez = { com.megacrit.cardcrawl.cards.AbstractCard.class, com.badlogic.gdx.graphics.g2d.SpriteBatch.class, String[].class }
)
public final class SkullTypeTranslation {
    @SpirePostfixPatch
    public static void Postfix(Object[] __args) {
        String[] text = (String[]) __args[2];
        if (SkulChinese.enabled() && "SKULL".equals(text[0])) text[0] = "头骨";
    }
}
