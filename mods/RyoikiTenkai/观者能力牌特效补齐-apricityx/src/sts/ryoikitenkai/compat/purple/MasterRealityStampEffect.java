package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 「洞悉现实」的<b>触发</b>演出：一道光痕从刻印射向那张新牌真正去的地方，
 * 落点上落一枚小「＋」印，然后上扬散去。
 * <p>
 * <h2>为什么落点不是角色身上</h2>
 * 这张牌作用在<b>具体对象</b>上 —— 那一张张被升级的新牌（规范 §2.18：
 * 「效果作用在哪些具体对象上，光就该落到那些对象上」）。新牌可能进手牌、
 * 抽牌堆或弃牌堆，所以落点由调用方按 Action 的真实去向给出：
 * <ul>
 *   <li>手牌 —— 手牌区中轴（这一帧新牌还没落位，只能落到「它要去的区域」）</li>
 *   <li>抽牌堆 —— 与研习共享 {@code StudyFx.deckX/deckY}</li>
 *   <li>弃牌堆 —— 镜像 {@code DiscardPilePanel} 的按钮中心公式</li>
 * </ul>
 * <h2>时间轴（单枚落点印）</h2>
 * <pre>
 *   0.00–0.24  抽出   一条楔形光痕从刻印射向落点，头部有行进光点
 *   0.16–0.42  盖章   「＋」两笔用 pow3Out 从中心长出（起手极快）
 *   0.46–1.00  消散   上扬 + 余弦淡出（光材质 → 上浮，规范 §3.2）
 * </pre>
 * <h2>错拍与频次</h2>
 * 同时升级多张牌时按 {@code index * 0.075s} 错开出场，否则会读成「一个图案」（规范 §3.1）。
 * 这个动作每回合可能发生 1~3 次（战歌锻惩击、研习塞洞见都会经过这里），
 * 属于高频动作 —— <b>不震屏、不音效</b>（规范 §2.11）。
 * <h2>队列</h2>
 * 落点在手牌区 / 底部 UI 区，普通层会被手牌与底部面板盖住，所以走
 * {@code queueTop()}（规范 §1.3：按「画在哪里」选队列）。
 * <h2>尺寸</h2>
 * 落点印用<b>固定尺寸</b>，不按判定框缩放 —— 它作用在牌上，而牌的大小是恒定的
 * （规范 §4.7「一次性特效不要按目标判定框缩放」）。
 */
final class MasterRealityStampEffect extends AbstractGameEffect {
    /** 单枚落点印的时长（秒，标准模式）。 */
    private static final float LIFE = 0.80f;
    /** 光痕抽出的阶段占比。 */
    private static final float BEAM = 0.24f;
    /** 落点印的半边长（基准像素）。 */
    private static final float STAMP_HALF = 14.0f;

    private final float fromX;
    private final float fromY;
    private final float toX;
    private final float toY;
    private final float delay;

    private float time;

    MasterRealityStampEffect(float fromX, float fromY, float toX, float toY, float delay) {
        this.fromX = fromX;
        this.fromY = fromY;
        this.toX = toX;
        this.toY = toY;
        this.delay = Math.max(0.0f, delay);
        this.startingDuration = this.duration = (LIFE + this.delay)
                * (Settings.FAST_MODE ? 0.80f : 1.0f);
        this.renderBehind = false;
    }

    @Override
    public void update() {
        float dt = Gdx.graphics.getDeltaTime();
        this.time += dt;
        this.duration -= dt;
        if (this.duration <= 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        float local = this.time - this.delay;
        if (local <= 0.0f || this.duration <= 0.0f) {
            return;
        }
        float q = MathUtils.clamp(local / LIFE, 0.0f, 1.0f);
        float sc = Settings.scale;

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        // ---- 1. 光痕：从刻印射向落点（起手快、末端停）----
        if (q < BEAM + 0.10f) {
            float grown = 1.0f - FastingDraw.cosineOut(MathUtils.clamp(q / BEAM, 0.0f, 1.0f));
            float tipX = this.fromX + (this.toX - this.fromX) * grown;
            float tipY = this.fromY + (this.toY - this.fromY) * grown;
            float beamA = MathUtils.clamp(1.0f - (q - BEAM) / 0.10f, 0.0f, 1.0f);
            if (beamA > 0.004f) {
                Draw.line(sb, this.fromX, this.fromY, tipX, tipY,
                        Math.max(4.5f, 7.0f * sc), MasterRealityFx.PLATE, beamA * 0.34f);
                Draw.glow(sb, tipX, tipY, 26.0f * sc, 26.0f * sc, 0.0f,
                        MasterRealityFx.EDGE, beamA * 0.30f);
            }
        }

        // ---- 2. 落点「＋」印：两笔从中心长出 → 停留 → 上扬散去 ----
        float stampQ = MathUtils.clamp((q - 0.16f) / 0.26f, 0.0f, 1.0f);
        if (stampQ > 0.0f) {
            float grow = Interpolation.pow3Out.apply(0.0f, 1.0f, stampQ);
            float tailQ = MathUtils.clamp((q - 0.46f) / 0.54f, 0.0f, 1.0f);
            float fade = FastingDraw.cosineOut(tailQ);
            if (fade > 0.004f) {
                float half = STAMP_HALF * sc;
                float x = this.toX;
                float y = this.toY + tailQ * 20.0f * sc;
                // 竖笔：宽笔暗档 + 细芯亮档（与常驻刻印同一套材质语汇）
                Draw.line(sb, x, y - half * grow, x, y + half * grow,
                        half * 0.40f, MasterRealityFx.PLATE, fade * 0.30f);
                Draw.line(sb, x, y - half * grow, x, y + half * grow,
                        half * 0.18f, MasterRealityFx.EDGE, fade * 0.62f);
                // 横笔
                Draw.line(sb, x - half * 0.90f * grow, y, x + half * 0.90f * grow, y,
                        half * 0.36f, MasterRealityFx.PLATE, fade * 0.28f);
                Draw.line(sb, x - half * 0.90f * grow, y, x + half * 0.90f * grow, y,
                        half * 0.16f, MasterRealityFx.EDGE, fade * 0.58f);
                Draw.glow(sb, x, y, half * 1.60f, half * 1.60f, 0.0f,
                        MasterRealityFx.EDGE, fade * 0.20f);
            }
        }

        sb.setBlendFunction(src, dst);
    }

    @Override
    public void dispose() {
    }
}
