package sts.ryoikitenkai.compat.red;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 「金属化」的施放演出：<b>甲片逐片飞来、逐片锁上、接缝扫亮</b>。
 * <p>
 * <h2>为什么要有这一段</h2>
 * 原版 {@code Metallicize.use()} 只有一句 {@code ApplyPowerAction}，<b>没有任何 VFX</b>。
 * 常驻那一圈甲如果在施放后凭空出现，玩家不会知道「这套甲是几片、是怎么装上的」，
 * 所以必须补出「锁甲」的过程（规范 §2.19）。
 * <h2>时间线（3 片，总长约 0.70s，{@code FAST_MODE} × 0.72）</h2>
 * <pre>
 *   0.00  第 1 片从 半径×1.65 处开始飞入
 *   0.06  第 2 片起飞（逐片错开 0.06s）
 *   0.24  第 1 片锁上：该片过曝一下
 *   0.24–0.54  接缝一圈扫亮（加色），随锁定逐片传过去
 *   0.54–0.70  收尾，整圈沉到位
 * </pre>
 * <h2>数量：甲片数 = amount</h2>
 * 甲片数由 {@link MetalFx#platesFromCard} 从卡片的 {@code magicNumber} 读
 * （prefix 阶段能力还没挂上，{@code getPower()} 一定返回 null）。
 * 所以 3 片 / 升级 4 片在施放这一刻就能看出来。
 * <h2>混合模式</h2>
 * 与常驻一致：<b>甲体普通混合</b>（实心、遮住身体），<b>接缝与入位闪光加色混合</b>。
 * 写死 {@code 770, 771} 会把别处的混合状态冲掉，所以用 get/set 还原。
 * <h2>音效</h2>
 * <b>不播</b>。原版 {@code MetallicizePower.playApplyPowerSfx()} 已经播
 * {@code POWER_METALLICIZE}，这里再补一个就会双响。
 * 一局只几次，所以允许轻震屏（在登记表里发）。
 */
final class MetalLockEffect extends AbstractGameEffect {
    /** 单片的飞入时长（标准模式）。 */
    private static final float LOCK_BASE = 0.26f;
    /** 逐片错开。 */
    private static final float STEP_BASE = 0.06f;
    /** 接缝扫亮的时长。 */
    private static final float SEAM_BASE = 0.30f;

    private final AbstractCreature owner;
    private final int plates;
    private final float lockTime;
    private final float step;
    private final float seamTime;
    private final float life;

    private float time;

    MetalLockEffect(AbstractCreature owner, int plates) {
        this.owner = owner;
        this.plates = MathUtils.clamp(plates, 1, MetalFx.MAX_PLATES);
        float scale = Settings.FAST_MODE ? 0.72f : 1.0f;
        this.lockTime = LOCK_BASE * scale;
        this.step = STEP_BASE * scale;
        this.seamTime = SEAM_BASE * scale;
        // 最后一片锁完 + 接缝走完
        this.life = (this.plates - 1) * this.step + this.lockTime + this.seamTime;
        this.startingDuration = this.duration = this.life;
        // 甲片在身上，画在人前面
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
        if (this.duration <= 0.0f || this.owner == null) {
            return;
        }
        float cx = BodyFx.centerX(this.owner);
        float cy = MetalFx.bandY(this.owner);
        float rx = MetalFx.bandRx(this.owner);
        float ry = MetalFx.bandRy(this.owner);
        float thick = MetalFx.plateThick(this.owner);
        int n = this.plates;

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();

        // ---- 甲片：普通混合，从外向内飞来（按深度顺序画）----
        sb.setBlendFunction(770, 771);
        MetalDraw.lockBand(sb, cx, cy, rx, ry, n, thick,
                this.time, this.lockTime, this.step);

        // ---- 接缝：加色混合，从第 1 片锁上的时刻开始扫亮 ----
        float seamStart = this.lockTime * 0.92f;
        if (this.time > seamStart) {
            float q = MathUtils.clamp(
                    (this.time - seamStart) / Math.max(0.001f, this.seamTime),
                    0.0f, 1.0f);
            float fade = 1.0f - q;
            // smoothstep：起手亮、末尾斜率为 0
            float a = fade * fade * (3.0f - 2.0f * fade) * 0.85f;
            sb.setBlendFunction(770, 1);
            MetalDraw.seamFlash(sb, cx, cy, rx, ry, thick, a, n);
        }

        sb.setBlendFunction(src, dst);
    }

    @Override
    public void dispose() {
    }
}
