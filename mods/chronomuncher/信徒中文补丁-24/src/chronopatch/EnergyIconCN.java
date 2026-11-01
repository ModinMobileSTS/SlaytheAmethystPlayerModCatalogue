package chronopatch;

import basemod.BaseMod;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.ByRef;
import com.evacipated.cardcrawl.modthespire.lib.LineFinder;
import com.evacipated.cardcrawl.modthespire.lib.Matcher;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertLocator;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import java.util.ArrayList;
import javassist.CtBehavior;

@SpirePatch(clz = AbstractCard.class, method = "renderDescriptionCN")
public class EnergyIconCN {
    private static final float ENERGY_W = 24.0f * Settings.scale;

    @SpireInsertPatch(locator = Locator.class, localvars = {"spacing", "i", "start_x", "font", "tmp"})
    public static void Insert(AbstractCard abstractCard, SpriteBatch spriteBatch, float f, int n, @ByRef float[] fArray, BitmapFont bitmapFont, @ByRef String[] stringArray) {
        try {
            if (stringArray[0].contains("[E]")) {
                float f2 = ENERGY_W * abstractCard.drawScale;
                abstractCard.renderSmallEnergy(spriteBatch, BaseMod.getCardSmallEnergy(abstractCard),
                        (fArray[0] - abstractCard.current_x) / Settings.scale / abstractCard.drawScale,
                        -100.0f - (((float) abstractCard.description.size() - 4.0f) / 2.0f - (float) n + 1.0f) * f);
                fArray[0] = fArray[0] + f2;
                stringArray[0] = "";
            }
        } catch (Throwable throwable) {}
    }

    private static class Locator extends SpireInsertLocator {
        private Locator() {}

        public int[] Locate(CtBehavior ctBehavior) throws Exception {
            int[] lines = LineFinder.findAllInOrder(ctBehavior, new ArrayList<>(),
                    new Matcher.MethodCallMatcher(GlyphLayout.class, "setText"));
            return new int[]{lines[lines.length - 1]};
        }
    }
}
