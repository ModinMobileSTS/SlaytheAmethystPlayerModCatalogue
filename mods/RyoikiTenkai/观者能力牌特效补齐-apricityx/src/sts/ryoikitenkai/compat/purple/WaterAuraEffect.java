package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;

/**
 * 「如水」的常驻表现：观者脚下一片由着色器逐像素算出的水面。
 * <p>
 * <b>坐标系约束</b>：只锚定 owner.hb（判定框）与地面，不绑定骨骼。
 * 早先的实现要从 spine 骨架里量出立绘那根 {@code staff} 再把水缠上去，
 * 换肤或自定义角色一旦没有同名骨头就整个失效，也会随待机动画抖动。
 * <p>
 * <b>为什么用着色器</b>：环形涟漪、焦散网纹、边缘柔化都是逐像素的细节。
 * CPU 画线画不出焦散，要做出涟漪层次则需成百上千段线条。
 * 交给 GPU 后每帧只有 <b>一次 draw call</b>。性能取舍见 {@link WaterShader} 的类注释。
 * <p>
 * <b>降级路径</b>：着色器编译失败、或噪声纹理始终拿不到时，退回
 * {@link #renderFallback} 的贴图版水面，保证永远有东西可看，也绝不让渲染中断。
 * 「暂时拿不到噪声」不会锁定降级：父模组在 receivePostInitialize 才加载噪声，
 * 每帧重试即可，等它到位自动切回着色器。
 * <p>
 * 水面之上仍叠几枚 CPU 画的碎光：它们稀疏、用量远小于逐像素计算，
 * 能给静态水面一点随机感。
 */
final class WaterAuraEffect extends PowerAuraEffect {
    /** 潮汐周期。 */
    private static final float TIDE_PERIOD = 3.6f;
    /** 水面游走的碎光数量。 */
    private static final int SPARKLES = 5;
    /** 平静姿态的 ID，原版 CalmStance.STANCE_ID。 */
    private static final String CALM_STANCE_ID = "Calm";

    private static final Color WATER = new Color(0.44f, 0.84f, 1.0f, 1.0f);
    private static final Color FOAM = new Color(0.86f, 0.98f, 1.0f, 1.0f);
    private static final Color DEEP = new Color(0.16f, 0.48f, 0.78f, 1.0f);

    private final float[] sparkleAngle = new float[SPARKLES];
    private final float[] sparkleRadius = new float[SPARKLES];
    private final float[] sparklePhase = new float[SPARKLES];

    /** 触发时的搅动强度，0~1，随时间衰减。 */
    private float churn;
    private float dropTimer;

    WaterAuraEffect(AbstractCreature owner, String cardId, String powerId) {
        super(owner, cardId, powerId);
        this.dropTimer = MathUtils.random(0.25f, 0.75f);
        for (int i = 0; i < SPARKLES; ++i) {
            this.sparkleAngle[i] = MathUtils.random(0.0f, MathUtils.PI2);
            this.sparkleRadius[i] = MathUtils.random(0.25f, 0.92f);
            this.sparklePhase[i] = MathUtils.random(0.0f, MathUtils.PI2);
        }
    }

    @Override
    void pulse() {
        this.churn = 1.0f;
    }

    /** 观者此刻是否处于平静姿态。 */
    private boolean calm() {
        if (!(this.owner instanceof com.megacrit.cardcrawl.characters.AbstractPlayer)) {
            return false;
        }
        com.megacrit.cardcrawl.characters.AbstractPlayer p =
                (com.megacrit.cardcrawl.characters.AbstractPlayer) this.owner;
        return p.stance != null && CALM_STANCE_ID.equals(p.stance.ID);
    }

    @Override
    protected void tick(float dt) {
        this.churn = Math.max(0.0f, this.churn - dt * 1.55f);
        boolean calm = calm();
        int stacks = PurpleEffectRegistry.stacksOf(this.owner, this.powerId);

        // 水滴：平静时更密，退潮后稀疏；搅动时额外加速
        this.dropTimer -= dt * (calm ? 1.0f : 0.40f) * (1.0f + 1.2f * this.churn);
        if (this.dropTimer <= 0.0f) {
            this.dropTimer = MathUtils.random(0.34f, 0.70f) * (5.0f / (3.0f + stacks));
            PurpleEffectRegistry.queue(new WaterDropEffect(this.owner));
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.fade <= 0.004f || this.owner == null) {
            return;
        }
        boolean calm = calm();
        // 姿态强度：平静满，非平静只剩淡痕 —— 这张牌「平静才结算格挡」的直接视觉化
        float strength = calm ? 1.0f : 0.40f;

        // 潮汐：水面整体上抬/下沉
        float tide = MathUtils.sin(this.time * MathUtils.PI2 / TIDE_PERIOD);
        float tide01 = 0.5f + 0.5f * tide;
        float lift = (2.0f + 4.5f * tide01) * Settings.scale * (calm ? 1.0f : 0.55f);

        float cx = BodyFx.centerX(this.owner);
        float cy = BodyFx.groundY(this.owner) + lift;
        float baseRx = BodyFx.halfWidth(this.owner) * (1.55f + 0.075f * tide) * (calm ? 1.0f : 0.90f);
        // 贴地透视：纵向压扁
        float squash = 0.28f;

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();

        // 着色器可用就走着色器，否则走贴图版
        ShaderProgram sp = WaterShader.program();
        Texture noise = WaterShader.noise();
        boolean shaded = false;
        if (sp != null && noise != null) {
            renderShader(sb, sp, noise, cx, cy, baseRx, squash, strength);
            shaded = true;
        }
        if (!shaded) {
            renderFallback(sb, cx, cy, baseRx, squash, strength);
        }

        // 水面之上：几枚游走闪烁的碎光（少量 CPU 绘制）
        sb.setBlendFunction(770, 1);
        for (int i = 0; i < SPARKLES; ++i) {
            float ang = this.sparkleAngle[i] + this.time * (0.32f + 0.10f * i);
            float rr = baseRx * this.sparkleRadius[i];
            float sx = cx + rr * MathUtils.cos(ang);
            float sy = cy + rr * squash * MathUtils.sin(ang);
            float twinkle = 0.5f + 0.5f * MathUtils.sin(this.time * 2.6f + this.sparklePhase[i]);
            float a = this.fade * strength * (0.16f + 0.44f * twinkle * twinkle) * (1.0f + 0.5f * this.churn);
            float s = (10.0f + 14.0f * twinkle) * Settings.scale;
            Draw.glow(sb, sx, sy, s, s, 0.0f, FOAM, a);
        }

        sb.setBlendFunction(src, dst);
    }

    /**
     * 着色器路径：一次 draw call 画出整个水面。
     * <p>
     * 用白方块当画布，四边形正好覆盖水面包围盒，片元里再把 [0,1] 的纹理坐标
     * 映射成椭圆坐标做计算。
     */
    private void renderShader(SpriteBatch sb, ShaderProgram sp, Texture noise,
                              float cx, float cy, float rx, float squash, float strength) {
        float w = rx * 2.0f;
        float h = w * squash;
        float x = cx - rx;
        float y = cy - h * 0.5f;

        ShaderProgram prev = sb.getShader();
        sb.setBlendFunction(770, 1);
        noise.bind(0);
        sb.setShader(sp);
        sp.setUniformi(WaterShader.uTexture(), 0);
        sp.setUniformf(WaterShader.uTime(), this.time);
        sp.setUniformf(WaterShader.uAlpha(), this.fade);
        sp.setUniformf(WaterShader.uStrength(), strength);
        sp.setUniformf(WaterShader.uChurn(), this.churn);
        // 水体与浪花各压暗一档：加色混合下过亮会糊成白斑
        sp.setUniformf(WaterShader.uWater(),
                WaterShader.R * 0.62f, WaterShader.G * 0.62f, WaterShader.B * 0.62f);
        sp.setUniformf(WaterShader.uFoam(),
                FOAM.r * 0.40f, FOAM.g * 0.40f, FOAM.b * 0.40f);
        sb.setColor(Color.WHITE);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x, y, w, h);
        sb.setShader(prev);
    }

    /**
     * 降级路径：着色器不可用时的贴图版水面。
     * 只用几张柔光椭圆 + 两圈环，保证「有东西可看」而不是一片空白。
     */
    private void renderFallback(SpriteBatch sb, float cx, float cy, float rx,
                                float squash, float strength) {
        sb.setBlendFunction(770, 1);
        Draw.glow(sb, cx, cy, rx * 2.0f, rx * 2.0f * squash, 0.0f, DEEP,
                this.fade * strength * 0.26f);
        Draw.glow(sb, cx, cy, rx * 1.5f, rx * 1.5f * squash, 0.0f, WATER,
                this.fade * strength * 0.24f);
        Draw.ring(sb, cx, cy, rx, rx * squash,
                Math.max(2.0f, 3.6f * Settings.scale), WATER,
                this.fade * strength * 0.50f, 56);
        Draw.ring(sb, cx, cy, rx * 0.62f, rx * 0.62f * squash,
                Math.max(1.6f, 2.6f * Settings.scale), WATER,
                this.fade * strength * 0.36f, 44);
    }
}
