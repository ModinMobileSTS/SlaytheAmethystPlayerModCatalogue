package sts.ryoikitenkai.compat.red;

import basemod.ReflectionHacks;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.Skeleton;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;

/**
 * 「金属化」的剪影描边。
 * <p>
 * 思路：在角色本体被绘制之前，把角色自己用「纯色剪影」着色器往多个方向各偏几像素重复绘制一遍，
 * 随后正常路径画出角色本体，被盖住的部分只留下最外圈 —— 得到严格贴合外形、连续无断口的描边。
 * <p>
 * 为什么不用几何拟合：角色是骨骼动画，手臂、披风会变形，任何固定形状的框都对不上剪影。
 * 重复绘制本体则天然跟随形变。
 * <p>
 * 为什么不用 skeleton.setColor：那是把颜色与贴图相乘，深色部位依然发暗，边不匀。
 * 这里着色器丢弃贴图 RGB、只保留 alpha，所以描边颜色均匀。
 * <p>
 * skeleton 是 protected 字段，通过 ReflectionHacks 读取（父模组也是这么取的）。
 * 插桩点选在 renderPlayerImage 的入口（Prefix），绘制完立即还原位置与着色器，
 * 不影响紧接着的正常绘制。
 */
public final class ArmorSilhouettePatch {
    private ArmorSilhouettePatch() {
    }

    /** 描边基础扩张像素数（未乘 scale）。 */
    private static final float BASE_OUTLINE_PX = 3.6f;
    /** 剪影重复绘制方向数：越多越圆滑连续。 */
    private static final int DIRECTIONS = 16;

    private static final Color OUTLINE_COLOR = new Color(0.80f, 0.93f, 1.0f, 1.0f);

    @SpirePatch2(
        cls = "com.megacrit.cardcrawl.characters.AbstractPlayer",
        method = "renderPlayerImage",
        paramtypez = { com.badlogic.gdx.graphics.g2d.SpriteBatch.class }
    )
    public static class Outline {
        @SpirePrefixPatch
        public static void Prefix(AbstractPlayer __instance, Object[] __args) {
            ArmorSilhouettePatch.renderOutline(__instance, (SpriteBatch) __args[0]);
        }
    }

    /** 满足条件时绘制外扩剪影。任何异常都吞掉：宁可没描边，也不能崩游戏。 */
    public static void renderOutline(AbstractPlayer player, SpriteBatch sb) {
        try {
            if (player == null || !RedEffectRegistry.shouldOutline(player)) {
                return;
            }
            ShaderProgram sp = SilhouetteShader.program();
            if (sp == null) {
                return;
            }
            // flash 每帧衰减一次，放在真正的绘制路径里做，避免额外挂钩子
            RedEffectRegistry.tickMetallicizeFlash(Gdx.graphics.getDeltaTime());
            float flash = RedEffectRegistry.metallicizeFlash();

            Skeleton skeleton = skeletonOf(player);
            if (skeleton != null) {
                renderSkeletonOutline(player, skeleton, sb, sp, flash);
            } else if (player.img != null) {
                renderSpriteOutline(player, sb, sp, flash);
            }
        } catch (Throwable ignored) {
            // 纯视觉增强，失败不应影响游戏
        }
    }

    private static Skeleton skeletonOf(AbstractPlayer player) {
        try {
            return ReflectionHacks.getPrivate(player, AbstractCreature.class, "skeleton");
        } catch (Throwable t) {
            return null;
        }
    }

    private static void renderSkeletonOutline(AbstractPlayer player, Skeleton skeleton, SpriteBatch sb,
                                              ShaderProgram sp, float flash) {
        float baseX = skeleton.getX();
        float baseY = skeleton.getY();
        float radius = outlineRadius(player, flash);

        sb.setShader(sp);
        sp.setUniformf(SilhouetteShader.colorUniform(), outlineColor(flash));

        for (int i = 0; i < DIRECTIONS; ++i) {
            float ang = MathUtils.PI2 * i / DIRECTIONS;
            skeleton.setPosition(baseX + radius * MathUtils.cos(ang),
                    baseY + radius * MathUtils.sin(ang));
            drawSkeleton(skeleton, sb);
        }

        // 还原，让紧接着的正常绘制不受影响
        skeleton.setPosition(baseX, baseY);
        sb.setShader(null);
        sb.setColor(Color.WHITE);
    }

    private static void renderSpriteOutline(AbstractPlayer player, SpriteBatch sb, ShaderProgram sp,
                                            float flash) {
        float baseX = player.drawX;
        float baseY = player.drawY;
        float radius = outlineRadius(player, flash);

        sb.setShader(sp);
        sp.setUniformf(SilhouetteShader.colorUniform(), outlineColor(flash));

        for (int i = 0; i < DIRECTIONS; ++i) {
            float ang = MathUtils.PI2 * i / DIRECTIONS;
            player.drawX = baseX + radius * MathUtils.cos(ang);
            player.drawY = baseY + radius * MathUtils.sin(ang);
            drawSprite(player, sb);
        }

        player.drawX = baseX;
        player.drawY = baseY;
        sb.setShader(null);
        sb.setColor(Color.WHITE);
    }

    /** 叠层越多、施放冲量越大，描边越外扩。 */
    private static float outlineRadius(AbstractPlayer player, float flash) {
        int stacks = stackCount(player);
        return (BASE_OUTLINE_PX + 0.22f * Math.max(0, stacks - 3) + flash * 1.6f) * Settings.scale;
    }

    private static int stackCount(AbstractPlayer player) {
        if (player == null || player.powers == null) {
            return 3;
        }
        com.megacrit.cardcrawl.powers.AbstractPower p = player.getPower("Metallicize");
        return p == null ? 3 : Math.abs(p.amount);
    }

    private static Color outlineColor(float flash) {
        OUTLINE_COLOR.a = 1.0f;
        if (flash > 0.001f) {
            // 施放时向纯白推进，读起来像金属被点亮
            float w = MathUtils.clamp(flash, 0.0f, 1.0f) * 0.75f;
            OUTLINE_COLOR.r = Interpolation.fade.apply(0.80f, 1.0f, w);
            OUTLINE_COLOR.g = Interpolation.fade.apply(0.93f, 1.0f, w);
            OUTLINE_COLOR.b = 1.0f;
        } else {
            OUTLINE_COLOR.r = 0.80f;
            OUTLINE_COLOR.g = 0.93f;
            OUTLINE_COLOR.b = 1.0f;
        }
        return OUTLINE_COLOR;
    }

    /**
     * 复刻 renderPlayerImage 的骨骼绘制分支：
     * 结束当前 SpriteBatch、用多边形批次画一次骨骼、再切回 SpriteBatch。
     * 若不切回，紧接着的正常绘制会因批次未开始而丢帧。
     */
    private static void drawSkeleton(Skeleton skeleton, SpriteBatch sb) {
        PolygonSpriteBatch psb = CardCrawlGame.psb;
        sb.end();
        psb.begin();
        AbstractPlayer.sr.draw(psb, skeleton);
        psb.end();
        sb.begin();
    }

    /** 复刻 renderPlayerImage 的贴图绘制分支。 */
    private static void drawSprite(AbstractPlayer player, SpriteBatch sb) {
        float w = player.img.getWidth();
        float h = player.img.getHeight();
        sb.setColor(Color.WHITE);
        sb.draw(player.img,
                player.drawX - w * Settings.scale / 2.0f + player.animX,
                player.drawY + player.animY,
                w * Settings.scale,
                h * Settings.scale,
                0, 0, player.img.getWidth(), player.img.getHeight(),
                player.flipHorizontal, player.flipVertical);
    }
}
