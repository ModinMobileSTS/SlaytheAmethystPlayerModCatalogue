package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 「研习」<b>回合末归档</b>的演出：<b>页一张张落进抽牌堆</b>。
 * <p>
 * <h2>这个动作对应什么</h2>
 * 原版 {@code StudyPower.atEndOfTurn(boolean)} 做的事是：
 * <pre>
 * this.addToBot(new MakeTempCardInDrawPileAction(new Insight(), this.amount, true, true));
 * </pre>
 * 也就是「<b>把 N 张洞见塞进抽牌堆</b>」。所以画面是
 * <b>页堆里的页依次抬起、加速下坠、边转边缩小、没入抽牌堆图标</b>，
 * 落点时图标扩一圈粗弧，读作「被收进去了」。
 * <h2>为什么是「依次」而不是同时</h2>
 * 原版的 {@code MakeTempCardInDrawPileAction} 会让 N 张牌
 * 连着进牌堆（每张有自己的飞行），所以画面也应该是<b>一张接一张</b>。
 * 同时落会读成「整体在闪」，看不出是 N 张。
 * <h2>时间线（单个页，{@code FAST_MODE} × 0.72）</h2>
 * <pre>
 *   0.00–0.22  抬起   从扇面位置微微上浮蓄势（读作「被抽出来」）
 *   0.22–0.78  下坠   加速落向抽牌堆图标，边转边缩小
 *   0.70–1.05  归档   图标上扩一圈粗弧，页淡出
 * </pre>
 * 页与页之间错开 0.16s。
 * <h2>为什么下坠用 {@code pow2In}</h2>
 * 规范 §三.5：{@code pow2In} 是「加速」，对应<b>重力 / 被吸下去</b>。
 * 用匀速会像漂浮，用 {@code pow2Out} 会像「飞过去后停住」。
 * 这里是从扇面落到图标，属于「被收走」，所以必须加速。
 * <h2>频次与队列</h2>
 * 每回合都会发生 —— <b>不震屏、不音效</b>。
 * 走 {@code queueTop}：整段动画在左下角 UI 区域，普通层会被底部面板盖住。
 */
final class StudyDropEffect extends AbstractGameEffect {
    /** 抬起时长（标准模式，会乘 timeScale）。 */
    private static final float LIFT_BASE = 0.22f;
    /** 下坠时长。 */
    private static final float FALL_BASE = 0.56f;
    /** 归档涟漪时长。 */
    private static final float SETTLE_BASE = 0.35f;
    /** 页与页之间错开。 */
    private static final float STAGGER_BASE = 0.16f;
    /** 抬起的高度（基准像素）。 */
    private static final float LIFT_PX = 16.0f;

    private final float timeScale;
    private final float liftTime;
    private final float fallTime;
    private final float settleTime;
    private final float stagger;
    private final int count;
    private final float life;

    // 每页的起点（构造时定死：用扇面几何，与常驻完全一致）
    private final float[] fromX;
    private final float[] fromY;
    private final float[] tilt;
    // 落点：抽牌堆图标中心（构造时定死，动画过程中图标不动）
    private final float deckX;
    private final float deckY;
    private final float baseR;

    private float time;
    private float excite;

    StudyDropEffect(int count) {
        this.timeScale = Settings.FAST_MODE ? 0.72f : 1.0f;
        this.liftTime = LIFT_BASE * this.timeScale;
        this.fallTime = FALL_BASE * this.timeScale;
        this.settleTime = SETTLE_BASE * this.timeScale;
        this.stagger = STAGGER_BASE * this.timeScale;
        this.count = MathUtils.clamp(count, 1, StudyFx.MAX_PAGES);

        this.fromX = new float[this.count];
        this.fromY = new float[this.count];
        this.tilt = new float[this.count];
        for (int i = 0; i < this.count; ++i) {
            this.fromX[i] = StudyFx.pageX(i, this.count);
            this.fromY[i] = StudyFx.pageY(i, this.count);
            this.tilt[i] = StudyFx.pageTilt(i, this.count);
        }
        this.deckX = StudyFx.deckX();
        this.deckY = StudyFx.deckY();
        this.baseR = Math.max(26.0f * Settings.scale, StudyFx.pageW() * 0.60f);

        // life 由时间轴反推：最后一页的归档涟漪必须走完
        this.life = this.liftTime
                + (this.count - 1) * this.stagger
                + this.fallTime
                + this.settleTime;
        this.startingDuration = this.duration = this.life;
        this.renderBehind = false;
    }

    @Override
    public void update() {
        float dt = Gdx.graphics.getDeltaTime();
        this.time += dt;
        this.duration -= dt;
        if (this.duration <= 0.0f) {
            this.isDone = true;
            return;
        }
        // 状态：带阻尼。下坠期间拉满，之后回落。
        float target = this.time < this.liftTime + this.fallTime ? 1.0f : 0.28f;
        float speed = target > this.excite ? 16.0f : 2.4f;
        this.excite += (target - this.excite) * MathUtils.clamp(dt * speed, 0.0f, 1.0f);
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.duration <= 0.0f) {
            return;
        }
        float ex = MathUtils.clamp(this.excite, 0.0f, 1.0f);
        float punch = 0.88f + 0.24f * ex;
        float pw = StudyFx.pageW();
        float ph = StudyFx.pageH();

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        for (int i = 0; i < this.count; ++i) {
            float t0 = i * this.stagger;
            float local = this.time - t0;
            if (local <= 0.0f) {
                continue;
            }

            float fx = this.fromX[i];
            float fy = this.fromY[i];

            if (local <= this.liftTime) {
                // ---- 抬起：微微上浮蓄势 ----
                float q = local / Math.max(0.001f, this.liftTime);
                float e = Interpolation.pow2Out.apply(0.0f, 1.0f, q);
                float lift = LIFT_PX * Settings.scale * e;
                // 抬起时翻页相位从当前角度开始，读作「被从扇面里抽出来」
                float flip = MathUtils.lerp(0.15f, 0.70f, e);
                StudyDraw.page(sb, fx, fy + lift, pw, ph, this.tilt[i],
                        flip, 0.95f * punch);
                // 蓄势的一小团光
                Draw.glow(sb, fx, fy + lift, pw * 1.4f, ph * 1.4f, this.tilt[i],
                        StudyFx.INK, 0.18f * e * punch);
                continue;
            }

            float fallQ = MathUtils.clamp(
                    (local - this.liftTime) / Math.max(0.001f, this.fallTime),
                    0.0f, 1.0f);

            if (fallQ < 1.0f) {
                // ---- 下坠：pow2In 加速落向抽牌堆图标 ----
                float e = Interpolation.pow2In.apply(0.0f, 1.0f, fallQ);
                float px = MathUtils.lerp(fx, this.deckX, e);
                float py = MathUtils.lerp(fy + LIFT_PX * Settings.scale, this.deckY, e);

                // 边落边缩小、边转：读作「被吸进牌堆」
                float scale = MathUtils.lerp(1.0f, 0.35f, e);
                float flip = MathUtils.lerp(0.70f, 1.0f, e);
                float spin = this.tilt[i] + 120.0f * e;
                // 末端淡出：不要硬切
                float fade = fallQ < 0.72f ? 1.0f
                        : FastingDraw.cosineOut((fallQ - 0.72f) / 0.28f);

                StudyDraw.page(sb, px, py, pw * scale, ph * scale, spin,
                        flip, 0.95f * fade * punch);
                // 拖尾：读作「被吸下去了」
                for (int j = 1; j <= 2; ++j) {
                    float be = MathUtils.clamp(e - j * 0.10f, 0.0f, 1.0f);
                    float bx = MathUtils.lerp(fx, this.deckX, be);
                    float by = MathUtils.lerp(fy + LIFT_PX * Settings.scale,
                            this.deckY, be);
                    float bs = scale * (1.0f - j * 0.20f);
                    Draw.glow(sb, bx, by, pw * bs * 1.2f, ph * bs * 1.2f,
                            0.0f, StudyFx.INK,
                            fade * (0.16f - j * 0.05f) * punch);
                }
                continue;
            }

            // ---- 归档：图标上扩一圈粗弧 ----
            float settleQ = MathUtils.clamp(
                    (local - this.liftTime - this.fallTime)
                            / Math.max(0.001f, this.settleTime),
                    0.0f, 1.0f);
            if (settleQ < 1.0f) {
                StudyDraw.settle(sb, this.deckX, this.deckY, this.baseR,
                        settleQ, punch);
            }
        }

        sb.setBlendFunction(src, dst);
    }

    @Override
    public void dispose() {
    }
}
