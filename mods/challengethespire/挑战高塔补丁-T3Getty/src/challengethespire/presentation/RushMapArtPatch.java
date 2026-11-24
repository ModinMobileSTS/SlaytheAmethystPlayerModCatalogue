package challengethespire.presentation;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.map.DungeonMap;
import com.megacrit.cardcrawl.screens.DungeonMapScreen;
import java.lang.reflect.Field;

/**
 * Aligns the scroll (map parchment) art with the Challenge The Spire rush map.
 *
 * <p>The parent mod builds its rush maps from fixed rows: 19 rows for the Boss Rush / Elite Rush
 * maps instead of the 15 rows {@code AbstractDungeon.generateMap()} produces. The game, however,
 * positions the parchment art from a hard coded row count:
 * {@code DungeonMap.calculateMapSize()} returns {@code MAP_DST_Y * 16 - 1380 * scale} for a normal
 * act (and {@code MAP_DST_Y * 4 - 1380 * scale} for the final act), i.e. it always assumes a
 * 15-row map, and {@code DungeonMap.show()} derives the art offset {@code mapOffsetY} from that
 * value.</p>
 *
 * <p>The node icons are drawn on the real row grid ({@code y * MAP_DST_Y}), so with four extra
 * rows the art ends up 4 rows too low: the last rooms (the final act bosses and the victory node)
 * are drawn on - and partly above - the parchment's top border, which looks like the icons are
 * offset from the map background.</p>
 *
 * <p>Fixing the offset alone is not enough: the bottom parchment texture is drawn at
 * {@code -mapMidDist + mapOffsetY}, where {@code mapOffsetY = mapMidDist - 120 * scale}, so it
 * stays put no matter what {@code calculateMapSize()} returns; only the middle and top textures
 * move. Moving them up would therefore leave a hole in the middle of the map. The middle texture
 * is a plain parchment section, so the second patch redraws it stretched to cover everything
 * between the bottom and the top texture. The parchment then has the same margins around the rooms
 * as a vanilla 15-row map, and every room icon sits on the map background.</p>
 *
 * <p>Only rush maps that are taller than a vanilla act are affected; every other map keeps the
 * vanilla values. The final act layout ({@code TheEnding}) is left alone because it draws a
 * different set of textures.</p>
 */
public final class RushMapArtPatch {

    /** Rows of a vanilla, non-final act map ({@code AbstractDungeon.MAP_HEIGHT}). */
    private static final int VANILLA_MAP_HEIGHT = 15;
    private static final String THE_ENDING = "TheEnding";
    /** Height of one map texture in the game's own coordinate space. */
    private static final float TEXTURE_HEIGHT = 1080.0f;

    private static Field mapMidDistField;
    private static Field mapOffsetYField;
    private static Field hField;
    private static Field midField;

    private RushMapArtPatch() {
    }

    private static boolean isModActive(String id) {
        return (CardCrawlGame.trial != null && CardCrawlGame.trial.dailyModIDs().contains(id))
                || ModHelper.isModEnabled(id);
    }

    private static boolean isRush() {
        return isModActive("challengethespire:Elite Rush")
                || isModActive("challengethespire:Modded Elite Rush")
                || isModActive("challengethespire:Boss Rush")
                || isModActive("challengethespire:Modded Boss Rush");
    }

    /** Number of rows the rush map has beyond the 15 rows the map art is laid out for. */
    private static int extraRows() {
        if (!isRush() || AbstractDungeon.map == null || AbstractDungeon.map.isEmpty()) {
            return 0;
        }
        // The final act renders the map with its own textures and never draws the middle one.
        if (THE_ENDING.equals(AbstractDungeon.id)) {
            return 0;
        }
        int rows = AbstractDungeon.map.size();
        return rows > VANILLA_MAP_HEIGHT ? rows - VANILLA_MAP_HEIGHT : 0;
    }

    private static float readFloat(Field field, Object instance) {
        if (field == null) {
            return Float.NaN;
        }
        try {
            return field.getFloat(instance);
        } catch (Exception e) {
            return Float.NaN;
        }
    }

    private static Field field(String name) {
        try {
            Field f = DungeonMap.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (Exception e) {
            return null;
        }
    }

    /** Moves the middle and top parchment textures up by the rows the rush map adds. */
    @SpirePatch2(clz = DungeonMap.class, method = "calculateMapSize", paramtypez = {})
    public static class FitArtToMapHeight {
        @SpirePostfixPatch
        public static float after(float __result) {
            int extra = extraRows();
            if (extra <= 0) {
                return __result;
            }
            return __result + (float) extra * Settings.MAP_DST_Y;
        }
    }

    /**
     * Redraws the plain middle parchment section stretched over the room grid, so the space that
     * the shifted top texture left behind is filled with parchment instead of a hole.
     */
    @SpirePatch2(clz = DungeonMap.class, method = "renderMapCenters",
            paramtypez = {SpriteBatch.class})
    public static class StretchMapCenter {
        @SpirePrefixPatch
        public static SpireReturn before(DungeonMap __instance, Object[] __args) {
            if (extraRows() <= 0) {
                return SpireReturn.Continue();
            }
            SpriteBatch sb = __args != null && __args.length > 0 && __args[0] instanceof SpriteBatch
                    ? (SpriteBatch) __args[0] : null;
            if (sb == null) {
                return SpireReturn.Continue();
            }
            if (mapMidDistField == null) {
                mapMidDistField = field("mapMidDist");
                mapOffsetYField = field("mapOffsetY");
                hField = field("H");
                midField = field("mid");
            }
            Texture mid = null;
            if (midField != null) {
                try {
                    mid = (Texture) midField.get(null);
                } catch (Exception ignored) {
                    mid = null;
                }
            }
            float mapMidDist = readFloat(mapMidDistField, __instance);
            float mapOffsetY = readFloat(mapOffsetYField, null);
            float textureTop = readFloat(hField, null);
            if (mid == null || Float.isNaN(mapMidDist) || Float.isNaN(mapOffsetY)
                    || Float.isNaN(textureTop)) {
                return SpireReturn.Continue();
            }
            float scaledHeight = TEXTURE_HEIGHT * Settings.scale;
            // Bottom edge of the top texture, and top edge of the bottom texture.
            float topTextureBottom = DungeonMapScreen.offsetY + mapOffsetY + textureTop;
            float bottomTextureTop = -mapMidDist + DungeonMapScreen.offsetY + mapOffsetY + 1.0f
                    + scaledHeight;
            float height = topTextureBottom - bottomTextureTop;
            if (height <= scaledHeight) {
                // Nothing was shifted, the vanilla single draw already covers the gap.
                return SpireReturn.Continue();
            }
            float x = 0.0f;
            float width = Settings.WIDTH;
            if (Settings.isMobile) {
                x = (float) -Settings.WIDTH * 0.05f;
                width = (float) Settings.WIDTH * 1.1f;
            }
            sb.draw(mid, x, bottomTextureTop - 2.0f * Settings.scale, width,
                    height + 4.0f * Settings.scale);
            return SpireReturn.Return(null);
        }
    }
}
