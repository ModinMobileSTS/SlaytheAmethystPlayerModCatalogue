package sts.ryoikitenkai.compat.green;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 打出「精准」的施放演出：<b>标靶从人物身上凝出、飞到左侧悬停</b>。
 * <h2>为什么是「从身上飞出」</h2>
 * 这件能力的产出是<b>一件挂在身边的法器</b>，所以它应该
 * <b>从角色身上凝出来、再离开身体</b>到左侧栖位，而不是凭空虚现。
 * <h2>演什么、不演什么（规范 §2.19）</h2>
 * 施放这一刻还没有任何 {@code AccuracyPower} 被施加，所以<b>不能演「已经 +N」</b>。
 * 演的是「这件法器成形了」这个<b>过程</b>：
 * 胸口聚光 → 标靶撑开并飞出 → 落位回弹。
 * <p>
 * 点数由卡牌上的附魔边框表达，所以这里不演点数。
 * <h2>动画曲线</h2>
 * 撑开与飞出用同一条单调曲线（{@link GreenDraw#easeOut}），所以「边撑开边飞」；
 * 落位是抛物线包络（两端归零，不死平）；包络用
 * {@link GreenDraw#smoothstep} + 余弦淡出。
 * <b>不用</b> {@code swingOut}：它冲到 1 以上再 {@code clamp} 会留下一段死平。
 * <h2>交接不跳位（规范 §2.13）</h2>
 * 终点每帧重算 {@link AccuracyFx#targetX/targetY/radius}，与常驻同一组函数。
 */
final class AccuracyCastEffect extends AbstractGameEffect {
    /** 总时长。 */
    private static final float LIFE = 1.30f;

    private static final float P_GATHER_END = 0.22f;
    private static final float P_FORM_START = 0.10f;
    private static final float P_FORM_END = 0.52f;
    private static final float P_FADE_START = 0.90f;

    private final AbstractCreature owner;
    /** 起飞点：打出这张牌那一刻的胸口。 */
    private final float fromX;
    private final float fromY;

    private final Color ring = new Color();
    private final Color ringHi = new Color();
    private final Color core = new Color();
    private final Color link = new Color();

    private float p;

    AccuracyCastEffect(AbstractCreature owner) {
        this.owner = owner;
        this.fromX = BodyFx.centerX(owner) + BodyFx.halfWidth(owner) * 0.10f;
        this.fromY = BodyFx.chestY(owner);
        this.startingDuration = LIFE;
        this.duration = LIFE;
        this.renderBehind = false;
    }

    @Override
    public void update() {
        float dt = Gdx.graphics.getDeltaTime();
        this.p = Math.min(1.0f, this.p + dt / LIFE);
        this.duration = LIFE * (1.0f - this.p);
        if (this.p >= 1.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.owner == null) {
            return;
        }
        // ---- 阶段量：撑开与飞出用同一条单调曲线，所以「边撑开边飞」----
        float formT = MathUtils.clamp(
                (this.p - P_FORM_START) / (P_FORM_END - P_FORM_START), 0.0f, 1.0f);
        float grow = GreenDraw.easeOut(formT);

        // ---- 包络：smoothstep 淡入 + 余弦淡出 ----
        float fadeIn = GreenDraw.smoothstep(MathUtils.clamp(this.p / 0.08f, 0.0f, 1.0f));
        float outT = MathUtils.clamp(
                (this.p - P_FADE_START) / (1.0f - P_FADE_START), 0.0f, 1.0f);
        float alpha = fadeIn * (0.5f + 0.5f * MathUtils.cos(outT * MathUtils.PI));
        if (alpha <= 0.004f || grow <= 0.004f) {
            return;
        }

        this.ring.set(AccuracyFx.RING);
        this.ringHi.set(AccuracyFx.RING_HI);
        this.core.set(AccuracyFx.CORE);
        this.link.set(AccuracyFx.LINK);

        int blendSrc = sb.getBlendSrcFunc();
        int blendDst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        // ---- 1. 凝聚：胸口一团光聚起 ----
        if (this.p < P_GATHER_END + 0.10f) {
            float q = GreenDraw.easeOut(MathUtils.clamp(this.p / P_GATHER_END, 0.0f, 1.0f));
            float r = BodyFx.height(this.owner) * 0.11f * q;
            GreenDraw.glow(sb, this.fromX, this.fromY, r * 4.4f, r * 4.4f, 0.0f,
                    this.ring, alpha * 0.18f * q);
            GreenDraw.glow(sb, this.fromX, this.fromY, r * 2.0f, r * 2.0f, 0.0f,
                    this.ringHi, alpha * 0.42f * q);
        }

        // ---- 2. 标靶：起点在胸口，终点实时读共享几何 ----
        float endX = AccuracyFx.targetX(this.owner);
        float endY = AccuracyFx.targetY(this.owner);
        float cx = MathUtils.lerp(this.fromX, endX, grow);
        float cy = MathUtils.lerp(this.fromY, endY, grow);
        float unit = AccuracyFx.radius(this.owner) * grow;
        if (unit <= 3.0f) {
            sb.setBlendFunction(blendSrc, blendDst);
            return;
        }
        float offX = cx - endX;
        float offY = cy - endY;

        // 落位回弹：抛物线包络，两端归零
        float landT = MathUtils.clamp((this.p - P_FORM_END) / 0.22f, 0.0f, 1.0f);
        float kick = MathUtils.sin(landT * MathUtils.PI) * 0.70f;
        // 停留段轻微压亮
        float hold = MathUtils.clamp((this.p - P_FORM_END - 0.22f) / 0.16f, 0.0f, 1.0f);
        float peak = 0.86f + 0.14f * hold;

        AccuracyDraw.target(sb, this.owner, offX, offY, alpha * peak,
                grow, 0.0f, this.p * LIFE, kick,
                this.ring, this.ringHi, this.core, this.link);

        // ---- 3. 落位外扩光圈：读作「它落定了」----
        if (landT > 0.0f && landT < 1.0f) {
            float rr = unit * (1.0f + 1.0f * landT);
            GreenDraw.glow(sb, cx, cy, rr * 2.0f, rr * 1.9f, 0.0f,
                    this.ring, alpha * 0.20f * (1.0f - landT));
        }

        sb.setBlendFunction(blendSrc, blendDst);
    }

    @Override
    public void dispose() {
    }
}
