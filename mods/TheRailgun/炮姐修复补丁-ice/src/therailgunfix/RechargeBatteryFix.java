package therailgunfix;

import TheRailgun.cards.Recharge;
import basemod.abstracts.CustomCard;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;

/** Makes the battery symbol on "Recharge" white in both card and popup art. */
public final class RechargeBatteryFix {
    // SingleCardViewPopup disposes the texture returned by getPortraitImage()
    // when it closes, so every call must hand out a fresh, independent texture.
    private RechargeBatteryFix() {
    }

    @SpirePatch2(clz = Recharge.class, method = "<ctor>", paramtypez = {})
    public static class SmallPortrait {
        @SpirePostfixPatch
        public static void Postfix(Recharge __instance) {
            Texture texture = getSmall();
            __instance.portrait = new TextureAtlas.AtlasRegion(texture, 0, 0, texture.getWidth(), texture.getHeight());
        }
    }

    @SpirePatch2(clz = CustomCard.class, method = "getPortraitImage", paramtypez = {})
    public static class LargePortrait {
        @SpirePrefixPatch
        public static SpireReturn Prefix(CustomCard __instance) {
            if (__instance instanceof Recharge) {
                return SpireReturn.Return(getLarge());
            }
            return SpireReturn.Continue();
        }
    }

    private static Texture getSmall() {
        return whiteBattery("TheRailgunResources/images/cards/recharge.png");
    }

    private static Texture getLarge() {
        return whiteBattery("TheRailgunResources/images/cards/recharge_p.png");
    }

    private static Texture whiteBattery(String path) {
        Pixmap source = new Pixmap(Gdx.files.internal(path));
        Pixmap pixmap = new Pixmap(source.getWidth(), source.getHeight(), Pixmap.Format.RGBA8888);
        pixmap.drawPixmap(source, 0, 0);
        source.dispose();

        int w = pixmap.getWidth();
        int h = pixmap.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = pixmap.getPixel(x, y);
                int alpha = pixel & 0xFF;
                if (alpha < 128) {
                    continue;
                }
                int r = (pixel >>> 24) & 0xFF;
                int g = (pixel >>> 16) & 0xFF;
                int b = (pixel >>> 8) & 0xFF;
                if (r + g + b < 400) {
                    pixmap.drawPixel(x, y, 0xFFFFFFFF);
                } else {
                    pixmap.drawPixel(x, y, 0xFFFFFF00);
                }
            }
        }

        Texture texture = new Texture(pixmap, false);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }
}
