package estaliafix;

import basemod.BaseMod;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * F6 切换异画之后小尺寸卡面空白的修复（普通卡与杰西斯卡片都覆盖）。
 *
 * 父模组开启异画时把卡面与边框换成 2x2 透明图（fullart/blank.png），
 * 期望 BaseMod 用整卡背景图来显示异画。但 BaseMod 的 RenderBgSwitch 只有在
 * CustomCard.textureBackgroundSmallImg 非空时才调用 getBackgroundSmallTexture()；
 * 父模组只重写了取图方法，没有设置该字段，于是小卡 = 普通卡背 + 透明卡面 + 透明边框，
 * 看起来就是空白。大图预览能显示，是因为那里的补丁无条件调用 getBackgroundLargeTexture()。
 *
 * 处理：
 * 1) refreshArtwork 之后同步 textureBackgroundSmallImg / textureBackgroundLargeImg 开关；
 * 2) 前置拦截两张卡的 getBackgroundSmallTexture：优先父模组自带小图
 *    estalia/ui/fullart/&lt;key&gt;.png，缺失时把 _p.png 大图等比缩小一半并缓存；
 * 3) 大图同样前置拦截，缺 _p.png 时用小图顶替，避免原实现缺图抛异常；
 * 4) 全部失败时回退普通卡背，不崩溃。
 */
public class FullArtSmallFix {

    private static final Map<String, Texture> SMALL_CACHE = new HashMap<String, Texture>();
    private static final Map<String, Texture> LARGE_CACHE = new HashMap<String, Texture>();

    @SpirePatch2(clz = estalia.Card.class, method = "refreshArtwork", paramtypez = {})
    public static class SyncBackgroundFields {
        @SpirePostfixPatch
        public static void postfix(estalia.Card __instance) {
            sync(__instance);
        }
    }

    @SpirePatch2(clz = estalia.TechnologyCard.class, method = "refreshArtwork", paramtypez = {})
    public static class SyncTechnologyBackgroundFields {
        @SpirePostfixPatch
        public static void postfix(estalia.TechnologyCard __instance) {
            sync(__instance);
        }
    }

    @SpirePatch2(clz = estalia.Card.class, method = "getBackgroundSmallTexture", paramtypez = {})
    public static class SmallBackground {
        @SpirePrefixPatch
        public static SpireReturn prefix(estalia.Card __instance) {
            if (!isFullArt(__instance)) {
                return SpireReturn.Continue();
            }
            return SpireReturn.Return(smallTexture(__instance));
        }
    }

    @SpirePatch2(clz = estalia.TechnologyCard.class, method = "getBackgroundSmallTexture", paramtypez = {})
    public static class TechnologySmallBackground {
        @SpirePrefixPatch
        public static SpireReturn prefix(estalia.TechnologyCard __instance) {
            if (!isFullArt(__instance)) {
                return SpireReturn.Continue();
            }
            return SpireReturn.Return(smallTexture(__instance));
        }
    }

    @SpirePatch2(clz = estalia.Card.class, method = "getBackgroundLargeTexture", paramtypez = {})
    public static class LargeBackground {
        @SpirePrefixPatch
        public static SpireReturn prefix(estalia.Card __instance) {
            if (!isFullArt(__instance)) {
                return SpireReturn.Continue();
            }
            return SpireReturn.Return(largeTexture(__instance));
        }
    }

    @SpirePatch2(clz = estalia.TechnologyCard.class, method = "getBackgroundLargeTexture", paramtypez = {})
    public static class TechnologyLargeBackground {
        @SpirePrefixPatch
        public static SpireReturn prefix(estalia.TechnologyCard __instance) {
            if (!isFullArt(__instance)) {
                return SpireReturn.Continue();
            }
            return SpireReturn.Return(largeTexture(__instance));
        }
    }

    private static void sync(AbstractCard card) {
        try {
            if (!(card instanceof basemod.abstracts.CustomCard)) {
                return;
            }
            basemod.abstracts.CustomCard custom = (basemod.abstracts.CustomCard) card;
            if (isFullArt(card)) {
                String key = cacheKey(card);
                custom.textureBackgroundSmallImg = smallPath(key);
                custom.textureBackgroundLargeImg = largePath(key);
            } else {
                custom.textureBackgroundSmallImg = null;
                custom.textureBackgroundLargeImg = null;
            }
        } catch (Throwable t) {
            log("同步异画背景开关失败: " + t);
        }
    }

    private static boolean isFullArt(AbstractCard card) {
        if (card instanceof estalia.Card) {
            estalia.Card c = (estalia.Card) card;
            return !c.spec.hidden && estalia.Mod.fullArtCards.contains(c.spec.id);
        }
        if (card instanceof estalia.TechnologyCard) {
            return estalia.Mod.fullArtCards.contains(settingKey((estalia.TechnologyCard) card));
        }
        return false;
    }

    /** 普通卡的 id；EST_U12 的异画使用 EST_R22 的图。 */
    private static String cardKey(estalia.Card card) {
        return "EST_U12".equals(card.spec.id) ? "EST_R22" : card.spec.id;
    }

    /** 杰西斯卡片的异画 key 已经是小写文件名（父模组的方法是包私有，这里按 star 复现）。 */
    private static String techKey(estalia.TechnologyCard card) {
        return card.star ? "jesys_star" : "jesys_grace";
    }

    private static String settingKey(estalia.TechnologyCard card) {
        return card.star ? "JesysStar" : "JesysGrace";
    }

    private static String path(String cacheKey, boolean large) {
        return "estalia/ui/fullart/" + cacheKey.substring(cacheKey.indexOf(':') + 1)
                .toLowerCase(Locale.ROOT) + (large ? "_p.png" : ".png");
    }

    private static String smallPath(String cacheKey) {
        return path(cacheKey, false);
    }

    private static String largePath(String cacheKey) {
        return path(cacheKey, true);
    }

    private static String cacheKey(AbstractCard card) {
        if (card instanceof estalia.Card) {
            return "card:" + cardKey((estalia.Card) card);
        }
        return "tech:" + techKey((estalia.TechnologyCard) card);
    }

    private static Texture smallTexture(AbstractCard card) {
        String key = cacheKey(card);
        if (SMALL_CACHE.containsKey(key)) {
            Texture cached = SMALL_CACHE.get(key);
            return cached != null ? cached : fallbackBackground(card);
        }
        Texture texture = null;
        try {
            String small = smallPath(key);
            if (Gdx.files.internal(small).exists()) {
                texture = estalia.Assets.texture(small);
            } else {
                texture = generateFromLarge(key);
            }
        } catch (Throwable t) {
            log("异画小图加载失败 " + key + ": " + t);
        }
        SMALL_CACHE.put(key, texture);
        return texture != null ? texture : fallbackBackground(card);
    }

    private static Texture largeTexture(AbstractCard card) {
        String key = cacheKey(card);
        if (LARGE_CACHE.containsKey(key)) {
            Texture cached = LARGE_CACHE.get(key);
            return cached != null ? cached : smallTexture(card);
        }
        Texture texture = null;
        try {
            String large = largePath(key);
            if (Gdx.files.internal(large).exists()) {
                texture = estalia.Assets.texture(large);
            }
        } catch (Throwable t) {
            log("异画大图加载失败 " + key + ": " + t);
        }
        LARGE_CACHE.put(key, texture);
        return texture != null ? texture : smallTexture(card);
    }

    /** 把 1024x1024 的大图等比缩小一半，与父模组自带小图同规格。 */
    private static Texture generateFromLarge(String key) {
        String large = largePath(key);
        if (!Gdx.files.internal(large).exists()) {
            log("缺少异画图 " + large);
            return null;
        }
        Pixmap.setFilter(Pixmap.Filter.BiLinear);
        Pixmap source = new Pixmap(Gdx.files.internal(large));
        Pixmap target = new Pixmap(Math.max(1, source.getWidth() / 2),
                Math.max(1, source.getHeight() / 2), Pixmap.Format.RGBA8888);
        target.drawPixmap(source,
                0, 0, source.getWidth(), source.getHeight(),
                0, 0, target.getWidth(), target.getHeight());
        Texture texture = new Texture(target);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        source.dispose();
        target.dispose();
        log("已由大图等比缩小生成异画小图 " + smallPath(key));
        return texture;
    }

    /** 普通卡背，作为缺图时的回退。 */
    private static Texture fallbackBackground(AbstractCard card) {
        try {
            AbstractCard.CardColor color = card.color;
            Texture texture;
            switch (card.type) {
                case ATTACK: {
                    texture = BaseMod.getAttackBgTexture(color);
                    if (texture == null) {
                        texture = ImageMaster.loadImage(BaseMod.getAttackBg(color));
                        BaseMod.saveAttackBgTexture(color, texture);
                    }
                    return texture;
                }
                case POWER: {
                    texture = BaseMod.getPowerBgTexture(color);
                    if (texture == null) {
                        texture = ImageMaster.loadImage(BaseMod.getPowerBg(color));
                        BaseMod.savePowerBgTexture(color, texture);
                    }
                    return texture;
                }
                default: {
                    texture = BaseMod.getSkillBgTexture(color);
                    if (texture == null) {
                        texture = ImageMaster.loadImage(BaseMod.getSkillBg(color));
                        BaseMod.saveSkillBgTexture(color, texture);
                    }
                    return texture;
                }
            }
        } catch (Throwable t) {
            return null;
        }
    }

    private static void log(String msg) {
        System.out.println("[EstaliaFix] " + msg);
    }
}
