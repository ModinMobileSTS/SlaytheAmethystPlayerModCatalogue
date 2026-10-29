package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 「洞悉现实」的<b>施放</b>演出：刻印在胸口右侧成形，停一拍，再滑到右侧栖位。
 * <p>
 * <h2>为什么必须补这一段</h2>
 * 原版 {@code MasterReality.use()} 只有一次 {@code ApplyPowerAction}，<b>一个 VFX 都没有</b>
 * （规范 §2.12：原版完全没演的必须补足）。不补的话打出这张牌毫无反馈。
 * <h2>它演的是「存在」，不是「效果」（规范 §2.19）</h2>
 * 施放这一刻还没有任何牌被升级，所以这里<b>不演升级</b>；演的是：
 * <ol>
 *   <li>「这枚刻印挂上了」——成形那一下</li>
 *   <li>「它在哪里」——从胸口滑到右侧栖位，终点与常驻<b>取自同一组函数</b>
 *       （{@code MasterRealityFx.markX/markY}），交接时不会跳位（规范 §2.13）</li>
 *   <li>「读数怎么读」——六道空刻痕同时亮出来（此刻全是灭的，因为还没升级过任何牌）</li>
 * </ol>
 * <h2>时间轴</h2>
 * <pre>
 *   0.00–0.20  起势   胸口右侧由 0.45 弹到 1.0（swingOut），透明度 0 → 1
 *   0.20–0.40  停留   交点白热最亮（读作「印面压住」）
 *   0.40–0.92  位移   pow2Out 滑到右侧栖位；六道空槽淡入
 *   0.76–1.00  消散   余弦淡出（末端斜率为 0，不硬切）
 * </pre>
 * <h2>队列与频次</h2>
 * 走普通层 {@code queue()}：整段演出都在角色身侧，不进入手牌/底部 UI 区。
 * 施放一局只几次 —— 轻震屏 + 音效由登记表统一给（规范 §2.11）。
 */
final class MasterRealityCastEffect extends AbstractGameEffect {
    private static final float LIFE = 1.10f;

    private final AbstractCreature owner;
    private float time;

    MasterRealityCastEffect(AbstractCreature owner) {
        this.owner = owner;
        this.startingDuration = this.duration = LIFE * (Settings.FAST_MODE ? 0.80f : 1.0f);
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
        if (this.owner == null || this.duration <= 0.0f) {
            return;
        }
        float p = MathUtils.clamp(1.0f - this.duration / Math.max(0.001f, this.startingDuration),
                0.0f, 1.0f);

        float half = MasterRealityFx.markHalf(this.owner);
        // 起点：胸口右侧（不是身体正中，避免和虔信的胸口刻度环同位置起势）
        float fromX = BodyFx.centerX(this.owner) + BodyFx.halfWidth(this.owner) * 0.55f;
        float fromY = BodyFx.chestY(this.owner) + BodyFx.height(this.owner) * 0.08f;
        // 终点：常驻的栖位 —— 同一组函数，交接不跳位
        float toX = MasterRealityFx.markX(this.owner);
        float toY = MasterRealityFx.markY(this.owner);

        float appear = MathUtils.clamp(p / 0.20f, 0.0f, 1.0f);
        float move = MathUtils.clamp((p - 0.40f) / 0.52f, 0.0f, 1.0f);
        float ease = Interpolation.pow2Out.apply(0.0f, 1.0f, move);
        float x = fromX + (toX - fromX) * ease;
        float y = fromY + (toY - fromY) * ease;

        // 出现：swingOut 从 0.45 弹到 1.0（带过冲，读作「被放到位置上」）
        float grow = Interpolation.swingOut.apply(0.45f, 1.0f, appear);
        float in = 1.0f - FastingDraw.cosineOut(MathUtils.clamp(p / 0.20f, 0.0f, 1.0f));
        float tail = FastingDraw.cosineOut(MathUtils.clamp((p - 0.76f) / 0.24f, 0.0f, 1.0f));
        float alpha = in * tail;
        // 停留段：交点白热（0.32 处最亮）
        float hot = MathUtils.clamp(1.0f - Math.abs(p - 0.32f) / 0.18f, 0.0f, 1.0f);
        if (alpha <= 0.004f) {
            return;
        }

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        // 位移段：一缕拖尾，读作「它被搬过去」
        if (move > 0.001f && move < 1.0f) {
            Draw.line(sb, x, y, fromX, fromY, half * 0.22f,
                    MasterRealityFx.PLATE, alpha * 0.16f * (1.0f - move));
        }

        MasterRealityFx.drawMark(sb, x, y, half * grow, alpha, hot);
        // 六道空槽随位移淡入：告诉玩家「读数在下面这六格」
        MasterRealityFx.drawScratchSlots(sb, x, y, half * grow, alpha * ease, null);

        sb.setBlendFunction(src, dst);
    }

    @Override
    public void dispose() {
    }
}
