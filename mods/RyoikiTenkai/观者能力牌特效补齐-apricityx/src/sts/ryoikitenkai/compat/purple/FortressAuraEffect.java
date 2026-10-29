package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

/**
 * 「心之堡垒」的常驻表现：<b>绕角色旋转的立方体护壁</b>。
 * <p>
 * <h2>机制驱动</h2>
 * 这张牌在<b>任意姿态切换</b>时获得格挡，所以立方体的转速、棱线硬度、
 * 呼吸周期都跟着<b>当前姿态</b>变 —— 玩家能从护壁的「性格」看出自己处在什么姿态。
 * 切换瞬间旧壳炸开、新壳成形（见 {@code FortressBurstEffect}）。
 * <h2>本轮修正：入场与退场都放慢（用户反馈「太快、消失不丝滑」）</h2>
 * <ul>
 *   <li>出现 0.40s → <b>0.70s</b>，走 {@code pow2Out}</li>
 *   <li>消失 0.30s → <b>0.85s</b>，走 {@code pow2In}，
 *       并叠加「整壳向内收拢到 62% + 上浮」</li>
 *   <li>姿态切换后的形变衰减 1.9 → <b>1.3</b>（过渡更缓）</li>
 * </ul>
 * 立方体是「立体包壳」，收场必须是<b>整壳向内塌缩</b>才合理 ——
 * 单纯降不透明度会读成「被关掉了」，而不是「壳散了」。
 * <h2>深度排序是体积感的唯一来源</h2>
 * 十二条棱按平均 z 排序后从远到近绘制；远端暗细、近端亮粗。
 * 不排序的话立方体会像一堆交错的线，看不出前后。
 * <h2>不绑定骨骼与性能</h2>
 * 坐标只来自 {@link BodyFx}；投影、排序索引都复用固定数组，每帧不 new；
 * 混合函数 get/set 还原。
 */
final class FortressAuraEffect extends PowerAuraEffect {
    /** 立方体的八个顶点（物体空间，−1~1）。 */
    private static final float[][] V = {
            {-1, -1, -1}, {1, -1, -1}, {1, 1, -1}, {-1, 1, -1},
            {-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1}
    };
    /** 十二条棱。 */
    private static final int[][] E = {
            {0, 1}, {1, 2}, {2, 3}, {3, 0},
            {4, 5}, {5, 6}, {6, 7}, {7, 4},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };
    /** 弱透视焦距。 */
    private static final float FOCAL = 4.2f;

    /** 出现 / 消失时长。都比原来慢一倍以上。 */
    private static final float APPEAR_TIME = 0.70f;
    private static final float VANISH_TIME = 0.85f;

    private final float[][] proj = new float[8][3];
    private final int[] edgeOrder = new int[12];
    private final float[] edgeDepth = new float[12];

    private float appear;
    private float disappear;
    private float morph;
    private String lastStance;

    FortressAuraEffect(AbstractCreature owner, String cardId, String powerId) {
        super(owner, cardId, powerId);
        // 立方体画在人前面，否则会被立绘遮掉
        this.renderBehind = false;
        this.appear = 0.0f;
        this.disappear = 0.0f;
        this.morph = 0.0f;
        this.lastStance = null;
        for (int i = 0; i < 12; ++i) {
            this.edgeOrder[i] = i;
        }
    }

    /** 能力触发（姿态切换）时调用：形变顶一下，读作「换了层壳」。 */
    @Override
    void pulse() {
        this.morph = 1.0f;
    }

    /** 当前姿态 ID；拿不到时返回空串。 */
    private static String stanceOf(AbstractCreature owner) {
        AbstractPlayer p = AbstractDungeon.player;
        if (p == null || owner != p || p.stance == null || p.stance.ID == null) {
            return "";
        }
        return p.stance.ID;
    }

    /** 姿态 → 主色。 */
    private static Color stanceColor(String stance) {
        if ("Wrath".equals(stance)) {
            return new Color(1.00f, 0.52f, 0.34f, 1.0f);
        }
        if ("Calm".equals(stance)) {
            return new Color(0.48f, 0.86f, 1.00f, 1.0f);
        }
        if ("Divinity".equals(stance)) {
            return new Color(1.00f, 0.86f, 0.42f, 1.0f);
        }
        return new Color(0.68f, 0.62f, 0.96f, 1.0f);
    }

    /** 姿态 → 呼吸周期（秒）。姿态越激烈呼吸越快。 */
    private static float breathPeriod(String stance) {
        if ("Wrath".equals(stance)) {
            return 0.85f;
        }
        if ("Calm".equals(stance)) {
            return 2.40f;
        }
        if ("Divinity".equals(stance)) {
            return 0.60f;
        }
        return 1.55f;
    }

    @Override
    protected void tick(float dt) {
        // ---- 出现：时间驱动，pow2Out 缓入 ----
        if (!this.isEnd) {
            if (this.disappear > 0.0f) {
                this.disappear = Math.max(0.0f, this.disappear - dt / VANISH_TIME);
            }
            if (this.appear < 1.0f) {
                this.appear = Math.min(1.0f, this.appear + dt / APPEAR_TIME);
            }
        } else {
            // ---- 消失：时间驱动，壳向内塌缩 ----
            if (this.appear > 0.0f) {
                this.appear = Math.max(0.0f, this.appear - dt / APPEAR_TIME);
            }
            this.disappear = Math.min(1.0f, this.disappear + dt / VANISH_TIME);
        }

        // 姿态切换的形变衰减更缓，读作「换了个性格」
        this.morph = Math.max(0.0f, this.morph - dt * 1.3f);

        // 自己检测姿态变化（双保险，幂等）
        String stance = stanceOf(this.owner);
        if (this.lastStance == null) {
            this.lastStance = stance;
        } else if (!this.lastStance.equals(stance)) {
            this.lastStance = stance;
            this.morph = 1.0f;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.fade <= 0.004f || this.owner == null || this.appear <= 0.004f) {
            return;
        }
        float sc = Settings.scale;
        float hw = BodyFx.halfWidth(this.owner);
        float h = BodyFx.height(this.owner);

        // 出现：缓动放大；消失：缓动收拢 + 上浮
        float growIn = Interpolation.pow2Out.apply(0.0f, 1.0f,
                MathUtils.clamp(this.appear, 0.0f, 1.0f));
        float out = Interpolation.pow2In.apply(0.0f, 1.0f,
                MathUtils.clamp(this.disappear, 0.0f, 1.0f));
        // 整壳向内收拢：尺寸缩到 62%，同时淡出
        float shell = growIn * (1.0f - 0.38f * out);
        float a = this.fade * (1.0f - out) * MathUtils.clamp(growIn, 0.0f, 1.0f);
        if (a <= 0.004f || shell <= 0.02f) {
            return;
        }
        // 消失时上浮一点点，读作「壳升上去散了」
        float liftY = out * h * 0.16f;

        String stance = stanceOf(this.owner);
        Color color = stanceColor(stance);
        float breath = 0.5f + 0.5f * MathUtils.sin(
                this.time * MathUtils.PI2 / breathPeriod(stance));

        float cx = BodyFx.centerX(this.owner);
        // 立方体中心：胸口高度（BodyFx 没有 centerY，用 chestY）
        float cy = BodyFx.chestY(this.owner) + liftY;
        // 立方体半边长
        float size = Math.max(hw * 1.35f, h * 0.42f) * shell;

        // 姿态相关：转速随形变加快
        float spinSpeed = 0.55f + 0.55f * this.morph;
        float ang = this.time * spinSpeed;
        float tilt = 0.42f + 0.16f * MathUtils.sin(this.time * 0.7f);

        int src = sb.getBlendSrcFunc();
        int dst = sb.getBlendDstFunc();
        sb.setBlendFunction(770, 1);

        // ---- 1. 中心体积光 ----
        Draw.glow(sb, cx, cy, size * 2.6f, size * 2.4f, 0.0f,
                color, a * (0.16f + 0.06f * breath));

        // ---- 2. 投影八个顶点 + 深度 ----
        float ca = MathUtils.cos(ang);
        float sa = MathUtils.sin(ang);
        float ct = MathUtils.cos(tilt);
        float st = MathUtils.sin(tilt);
        float safeSize = Math.max(1.0f, size);
        for (int i = 0; i < 8; ++i) {
            float x0 = V[i][0] * size;
            float y0 = V[i][1] * size;
            float z0 = V[i][2] * size;
            // 绕 Y 轴转
            float x1 = x0 * ca - z0 * sa;
            float z1 = x0 * sa + z0 * ca;
            // 绕 X 轴倾
            float y1 = y0 * ct - z1 * st;
            float z2 = y0 * st + z1 * ct;
            // 弱透视
            float kk = FOCAL / (FOCAL + z2 / safeSize);
            this.proj[i][0] = cx + x1 * kk;
            this.proj[i][1] = cy + y1 * kk;
            this.proj[i][2] = z2;
        }

        // ---- 3. 棱按平均深度排序：先画远的 ----
        for (int i = 0; i < 12; ++i) {
            this.edgeDepth[i] = (this.proj[E[i][0]][2] + this.proj[E[i][1]][2]) * 0.5f;
            this.edgeOrder[i] = i;
        }
        // 插入排序即可（12 个元素）
        for (int i = 1; i < 12; ++i) {
            int key = this.edgeOrder[i];
            float kv = this.edgeDepth[key];
            int j = i - 1;
            while (j >= 0 && this.edgeDepth[this.edgeOrder[j]] > kv) {
                this.edgeOrder[j + 1] = this.edgeOrder[j];
                --j;
            }
            this.edgeOrder[j + 1] = key;
        }

        // ---- 4. 画棱：远端暗细、近端亮粗（体积感的唯一来源）----
        float thickBase = Math.max(5.0f, hw * 0.055f);
        for (int idx = 0; idx < 12; ++idx) {
            int e = this.edgeOrder[idx];
            int i0 = E[e][0];
            int i1 = E[e][1];
            float d = this.edgeDepth[e] / safeSize;
            // near: 1 = 最近, 0 = 最远
            float near = 1.0f - MathUtils.clamp((d + 1.4f) / 2.8f, 0.0f, 1.0f);
            float thick = Math.max(4.5f, thickBase * (0.55f + 0.75f * near));
            float ea = a * (0.22f + 0.62f * near);
            Draw.line(sb, this.proj[i0][0], this.proj[i0][1],
                    this.proj[i1][0], this.proj[i1][1], thick, color, ea);
        }

        // ---- 5. 顶点节点：近端亮、远端暗 ----
        for (int i = 0; i < 8; ++i) {
            float d = this.proj[i][2] / safeSize;
            float near = 1.0f - MathUtils.clamp((d + 1.4f) / 2.8f, 0.0f, 1.0f);
            float r = Math.max(2.0f, hw * 0.055f * (0.5f + 0.8f * near));
            Draw.glow(sb, this.proj[i][0], this.proj[i][1], r * 3.0f, r * 3.0f, 0.0f,
                    color, a * (0.18f + 0.52f * near) * (0.8f + 0.2f * breath));
        }

        sb.setBlendFunction(src, dst);
    }
}
