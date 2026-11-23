package estaliafix;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;

/**
 * 保留角色选择界面的原版 portrait.png；将战斗中的 combat.png 和 shoulder.png
 * 在加载时缩放为固定的 440x440 纹理。每次加载创建独立纹理，供角色自行释放。
 */
public class CharacterPortraitFix {
    private static final String COMBAT_PATH = "estalia/ui/combat.png";
    private static final String SHOULDER_PATH = "estalia/ui/shoulder.png";
    private static final int TARGET_WIDTH = 440;
    private static final int TARGET_HEIGHT = 440;
    @SpirePatch2(
            clz = com.megacrit.cardcrawl.helpers.ImageMaster.class,
            method = "loadImage",
            paramtypez = {String.class}
    )
    public static class LoadBattleImage {
        @SpirePrefixPatch
        public static SpireReturn prefix(String imgUrl) {
            return replace(imgUrl);
        }
    }

    @SpirePatch2(
            clz = com.megacrit.cardcrawl.helpers.ImageMaster.class,
            method = "loadImage",
            paramtypez = {String.class, boolean.class}
    )
    public static class LoadBattleImageWithFilter {
        @SpirePrefixPatch
        public static SpireReturn prefix(String imgUrl, boolean linearFiltering) {
            return replace(imgUrl);
        }
    }

    private static SpireReturn replace(String imgUrl) {
        if (!COMBAT_PATH.equals(imgUrl) && !SHOULDER_PATH.equals(imgUrl)) {
            return SpireReturn.Continue();
        }
        Texture texture = resize(imgUrl);
        return texture == null ? SpireReturn.Continue() : SpireReturn.Return(texture);
    }

    private static Texture resize(String path) {
        Pixmap source = null;
        Pixmap target = null;
        try {
            source = new Pixmap(Gdx.files.internal(path));
            target = new Pixmap(TARGET_WIDTH, TARGET_HEIGHT, Pixmap.Format.RGBA8888);
            Pixmap.setFilter(Pixmap.Filter.BiLinear);
            target.drawPixmap(source,
                    0, 0, source.getWidth(), source.getHeight(),
                    0, 0, TARGET_WIDTH, TARGET_HEIGHT);
            Texture result = new Texture(target);
            result.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            System.out.println("[EstaliaFix] 战斗贴图 " + path + " 已缩放为 " + TARGET_WIDTH + "x" + TARGET_HEIGHT);
            return result;
        } catch (Throwable t) {
            System.out.println("[EstaliaFix] 缩放战斗贴图失败 " + path + ": " + t);
            return null;
        } finally {
            if (source != null) {
                source.dispose();
            }
            if (target != null) {
                target.dispose();
            }
        }
    }
}
