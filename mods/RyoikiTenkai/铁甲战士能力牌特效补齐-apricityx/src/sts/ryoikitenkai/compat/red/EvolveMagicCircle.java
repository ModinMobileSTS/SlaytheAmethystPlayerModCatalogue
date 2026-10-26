package sts.ryoikitenkai.compat.red;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 「进化」的常驻表现：脚下平铺一座呼吸的魔法阵。
 * <p>
 * 魔法阵由几层构成，全部压扁成椭圆以贴合地面透视：
 * 1. 外圈、内圈、最内小圈三重圆环；
 * 2. 由直线连接顶点组成的星形多边形（按顶点跳跃连线，形成交错的星阵）；
 * 3. 环上均匀分布的小刻符。
 * <p>
 * 整体做缓慢呼吸（亮度与半径同步起伏）。呼吸的上升相触发 DNA 粒子涌现，
 * 于是「阵在呼吸、呼吸时冒出 DNA」这两个动作是同一个节拍。
 * <p>
 * 环与星阵都用 {@link VfxDraw#line}（实心贴图）绘制，保证线条连续不断。
 * <p>
 * 注意：update() 运行在游戏遍历 effectList 的过程中，
 * 派生粒子必须走 RedEffectRegistry.queue（effectsQueue），否则会抛 ConcurrentModificationException。
 */
public class EvolveMagicCircle extends AbstractGameEffect {
    private static final Color OUTER = new Color(0.42f, 1.0f, 0.70f, 0.0f);
    private static final Color INNER = new Color(0.62f, 1.0f, 0.86f, 0.0f);
    private static final Color STAR = new Color(0.50f, 1.0f, 0.78f, 0.0f);
    private static final Color FILL = new Color(0.26f, 0.92f, 0.58f, 0.0f);
    private static final Color SCRATCH = new Color();
    private static final TextureAtlas.AtlasRegion GLOW = ImageMaster.GLOW_SPARK_2;

    /** 呼吸周期（秒）。 */
    private static final float BREATH_PERIOD = 1.9f;
    /** 星形顶点数。 */
    private static final int STAR_POINTS = 7;
    /** 每次呼吸上升相涌现的 DNA 粒子数。 */
    private static final int PARTICLES_PER_BREATH = 3;

    private final AbstractCreature owner;
    private final float fallbackCx;
    private final float fallbackY;
    private final float[] runePhase = new float[8];

    private float time = 0.0f;
    private float fade = 0.0f;
    private float spin = 0.0f;
    /** 上一帧的呼吸相位，用于检测上升相。 */
    private float lastBreathPhase = 0.0f;
    private boolean emittedThisBreath = false;

    public boolean isEnd = false;

    public EvolveMagicCircle(AbstractCreature owner) {
        this.renderBehind = true;
        this.owner = owner;
        this.fallbackCx = owner != null ? owner.hb.cX : Settings.WIDTH * 0.25f;
        this.fallbackY = owner != null ? owner.hb.y : Settings.HEIGHT * 0.25f;
        for (int i = 0; i < this.runePhase.length; ++i) {
            this.runePhase[i] = MathUtils.random(0.0f, MathUtils.PI2);
        }
    }

    private float cx() {
        return this.owner != null ? this.owner.hb.cX : this.fallbackCx;
    }

    private float groundY() {
        return this.owner != null ? this.owner.hb.y : this.fallbackY;
    }

    private float baseRx() {
        float w = this.owner != null ? this.owner.hb.width : 120.0f * Settings.scale;
        return Math.max(78.0f * Settings.scale, w * 0.94f);
    }

    /** 呼吸值 0~1。 */
    private float breath() {
        return 0.5f + 0.5f * MathUtils.sin(this.time * MathUtils.PI2 / BREATH_PERIOD);
    }

    @Override
    public void update() {
        float dt = Gdx.graphics.getDeltaTime();
        this.time += dt;
        this.spin += dt * 0.22f;
        if (this.spin > MathUtils.PI2) {
            this.spin -= MathUtils.PI2;
        }

        if (!this.isEnd) {
            this.fade = Math.min(1.0f, this.fade + dt * 2.0f);

            // 检测呼吸的上升相，每次上升涌出一小簇 DNA 粒子
            float phase = this.time % BREATH_PERIOD;
            if (phase < this.lastBreathPhase) {
                this.emittedThisBreath = false;
            }
            this.lastBreathPhase = phase;
            if (!this.emittedThisBreath && phase > BREATH_PERIOD * 0.5f) {
                this.emittedThisBreath = true;
                for (int i = 0; i < PARTICLES_PER_BREATH; ++i) {
                    RedEffectRegistry.spawnMagicCircleHelix(this.owner);
                }
            }
        } else {
            this.fade = Math.max(0.0f, this.fade - dt * 2.2f);
            if (this.fade <= 0.001f) {
                this.isDone = true;
            }
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.fade <= 0.004f) {
            return;
        }
        float b = breath();
        // 呼吸同时改变亮度与半径，看起来像在「吸气-呼气」
        float pulse = 1.0f + 0.045f * b;
        float bright = 0.55f + 0.45f * b;
        float cx = cx();
        float gy = groundY();
        float rx = baseRx() * pulse;
        float squash = 0.32f;
        float a = this.fade * bright;

        sb.setBlendFunction(770, 1);

        // 阵心柔光
        float fillRx = rx * 0.92f;
        float fillRy = fillRx * squash;
        SCRATCH.set(FILL);
        SCRATCH.a = this.fade * (0.10f + 0.20f * b);
        sb.setColor(SCRATCH);
        sb.draw(GLOW, cx - fillRx, gy - fillRy,
                GLOW.packedWidth * 0.5f, GLOW.packedHeight * 0.5f,
                GLOW.packedWidth, GLOW.packedHeight,
                (fillRx * 2.0f) / GLOW.packedWidth, (fillRy * 2.0f) / GLOW.packedHeight, 0.0f);

        float thick = Math.max(2.6f, 4.0f * Settings.scale) * (0.78f + 0.32f * b);

        // 三重圆环，全部用实心线，保证连续
        VfxDraw.ellipseRing(sb, cx, gy, rx, rx * squash, 56, OUTER, a * 0.95f, thick, this.spin, 1.0f, true);
        VfxDraw.ellipseRing(sb, cx, gy, rx * 0.76f, rx * 0.76f * squash, 48, INNER, a * 0.80f, thick * 0.8f, -this.spin * 1.4f, 0.9f, true);
        VfxDraw.ellipseRing(sb, cx, gy, rx * 0.30f, rx * 0.30f * squash, 32, INNER, a * 0.64f, thick * 0.6f, this.spin * 2.0f, 0.6f, true);

        // 星形：按顶点跳跃连线，形成交错的七芒阵
        float prevX = 0.0f;
        float prevY = 0.0f;
        for (int i = 0; i <= STAR_POINTS; ++i) {
            float ang = this.spin + MathUtils.PI2 * ((i * 3) % STAR_POINTS) / STAR_POINTS;
            float sx = cx + rx * 0.86f * MathUtils.cos(ang);
            float sy = gy + rx * 0.86f * squash * MathUtils.sin(ang);
            if (i > 0) {
                VfxDraw.line(sb, prevX, prevY, sx, sy, STAR, a * 0.58f, thick * 0.6f);
            }
            prevX = sx;
            prevY = sy;
        }

        // 环上刻符：外圈均匀分布的小点，各自闪动
        SCRATCH.set(INNER);
        float dot = Math.max(2.6f, 4.2f * Settings.scale) * (0.8f + 0.5f * b) * this.fade;
        for (int i = 0; i < 12; ++i) {
            float ang = -this.spin * 0.6f + MathUtils.PI2 * i / 12;
            float rx1 = cx + rx * 0.88f * MathUtils.cos(ang);
            float ry1 = gy + rx * 0.88f * squash * MathUtils.sin(ang);
            float flick = 0.55f + 0.45f * MathUtils.sin(this.time * 3.0f + this.runePhase[i % this.runePhase.length]);
            SCRATCH.a = a * flick;
            sb.setColor(SCRATCH);
            sb.draw(GLOW, rx1 - dot, ry1 - dot,
                    GLOW.packedWidth * 0.5f, GLOW.packedHeight * 0.5f,
                    GLOW.packedWidth, GLOW.packedHeight,
                    (dot * 2.0f) / GLOW.packedWidth, (dot * 2.0f) / GLOW.packedHeight, 0.0f);
        }

        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}
