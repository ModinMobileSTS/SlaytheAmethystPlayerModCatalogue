package sts.ryoikitenkai.compat.red;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 从脚下魔法阵里升起的一小段 DNA 双链。
 * <p>
 * 刻意做得「矮而窄」：高度只到角色小腿到大腿之间，横向半径远小于角色宽度，
 * 因此它贴着地面往上冒，不会罩住身体、也不挡脸。
 * <p>
 * 两条链用相位 θ 与 θ+π 保证在投影上关于中轴镜像（x = cx ± R·cosθ），
 * 所以连接它们的碱基对始终水平，长度随 |cosθ| 变化，读起来就是标准的双螺旋。
 * <p>
 * 所有线段都用 {@link VfxDraw#line}（实心贴图）绘制：
 * 柔光贴图两端会衰减，拼接处会留下暗缝，整条链看起来断断续续。
 */
public class EvolveHelix extends AbstractGameEffect {
    private static final Color STRAND = new Color(0.55f, 1.0f, 0.82f, 0.0f);
    private static final Color RUNG = new Color(0.88f, 1.0f, 0.62f, 0.0f);
    private static final int STEPS = 26;

    private final AbstractCreature owner;
    private final float fallbackCx;
    private final float fallbackY;

    private final float radius;
    private final float height;
    private final float turns;
    private final float thickness;
    private final float baseAlpha;
    private final float spinRate;
    private final float offsetX;

    private float spin;
    private float time;

    private EvolveHelix(AbstractCreature owner, float radius, float height, float turns,
                        float duration, float thickness, float baseAlpha, float offsetX) {
        this.renderBehind = false;
        this.owner = owner;
        this.radius = radius;
        this.height = height;
        this.turns = turns;
        this.duration = duration;
        this.startingDuration = duration;
        this.thickness = thickness;
        this.baseAlpha = baseAlpha;
        this.offsetX = offsetX;
        this.spinRate = MathUtils.random(2.2f, 3.4f);
        this.spin = MathUtils.random(0.0f, MathUtils.PI2);
        this.fallbackCx = owner != null ? owner.hb.cX : Settings.WIDTH * 0.25f;
        this.fallbackY = owner != null ? owner.hb.y : Settings.HEIGHT * 0.25f;
    }

    /** 常驻循环里周期性冒出的小螺旋，offsetX 是相对角色中轴的横向偏移。 */
    public static EvolveHelix periodic(AbstractCreature owner, float offsetX) {
        return new EvolveHelix(owner,
                24.0f * Settings.scale,
                106.0f * Settings.scale,
                1.4f,
                1.15f,
                Math.max(2.2f, 3.2f * Settings.scale),
                0.95f,
                offsetX);
    }

    /** 施放时冒出的一批更高更亮的螺旋。 */
    public static EvolveHelix burst(AbstractCreature owner, int index) {
        float scale = 1.0f + 0.16f * index;
        return new EvolveHelix(owner,
                28.0f * Settings.scale * scale,
                (128.0f + 22.0f * index) * Settings.scale,
                1.5f,
                1.3f,
                Math.max(2.8f, 4.2f * Settings.scale),
                1.0f,
                (index - 1) * 30.0f * Settings.scale);
    }

    private float cx() {
        return (this.owner != null ? this.owner.hb.cX : this.fallbackCx) + this.offsetX;
    }

    private float baseY() {
        return this.owner != null ? this.owner.hb.y : this.fallbackY;
    }

    @Override
    public void update() {
        float dt = Gdx.graphics.getDeltaTime();
        this.time += dt;
        this.duration -= dt;
        this.spin += this.spinRate * dt;
        if (this.duration <= 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        float p = MathUtils.clamp(this.time / this.startingDuration, 0.0f, 1.0f);
        // 螺旋向上「长」出来：前 72% 的时间里 head 从 0 推到 1.15，之后整段淡出
        float head = MathUtils.clamp(Interpolation.pow2Out.apply(0.0f, 1.15f, p / 0.72f), 0.0f, 1.15f);
        float alpha = p < 0.18f ? p / 0.18f : MathUtils.clamp(1.0f - (p - 0.18f) / 0.82f, 0.0f, 1.0f);
        if (alpha <= 0.01f || head <= 0.0f) {
            return;
        }
        float a = alpha * this.baseAlpha;
        float cx = cx();
        float base = baseY();

        sb.setBlendFunction(770, 1);

        float prevX1 = 0.0f, prevX2 = 0.0f, prevY = 0.0f;
        boolean hasPrev = false;
        for (int i = 0; i <= STEPS; ++i) {
            float u = (float) i / STEPS;
            if (u > head) {
                break;
            }
            float y = base + this.height * u;
            float phase = u * this.turns * MathUtils.PI2 + this.spin;
            float cos = MathUtils.cos(phase);
            float x1 = cx + this.radius * cos;
            float x2 = cx - this.radius * cos;

            // 螺旋头部附近收窄，避免出现生硬的断口
            float taper = u > head - 0.16f ? Math.max(0.0f, (head - u) / 0.16f) : 1.0f;
            float segA = a * taper * Math.min(1.0f, u * 5.0f);

            if (hasPrev) {
                // 实心线：首尾相接，整条链连续
                VfxDraw.line(sb, prevX1, prevY, x1, y, STRAND, segA, this.thickness);
                VfxDraw.line(sb, prevX2, prevY, x2, y, STRAND, segA, this.thickness);
            }
            // 碱基对：每隔几步画一根水平的横档
            if (i % 3 == 0 && Math.abs(cos) > 0.26f) {
                VfxDraw.line(sb, x1, y, x2, y, RUNG, segA * 0.85f * Math.abs(cos), this.thickness * 0.8f);
            }
            prevX1 = x1;
            prevX2 = x2;
            prevY = y;
            hasPrev = true;
        }

        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}
