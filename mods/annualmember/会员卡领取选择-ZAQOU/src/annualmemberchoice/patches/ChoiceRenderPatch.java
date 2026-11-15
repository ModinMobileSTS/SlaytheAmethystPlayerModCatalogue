package annualmemberchoice.patches;

import annualmemberchoice.MembershipChoice;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

/** Draws the prompt above everything else the dungeon renders that frame. */
public class ChoiceRenderPatch {

    @SpirePatch2(clz = AbstractDungeon.class, method = "render", paramtypez = {SpriteBatch.class})
    public static class Render {
        @SpirePostfixPatch
        public static void postfix(Object[] __args) {
            if (__args != null && __args.length > 0 && __args[0] instanceof SpriteBatch) {
                MembershipChoice.render((SpriteBatch) __args[0]);
            }
        }
    }
}
