package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

/**
 * 「确立基础」的画笔：<b>计量方尖碑</b>。
 * <p>
 * <h2>为什么石头要单独写一套画笔</h2>
 * 观者另外几张牌全是「光」—— 水面、音符、立方体棱、抓痕、护壁，都是加色叠加的发光体。
 * 石头不能那样画：
 * <ul>
 *   <li><b>石头需要「遮挡」</b>：近处的暗侧面必须压住远处</li>
 *   <li><b>石头需要「暗部」</b>：加色混合里没有阴影，暗部要自己画一层</li>
 * </ul>
 * 所以 {@link #monolith} 的画法是「先铺暗侧面 → 再摆亮立面 → 最后勾棱与刻线」，
 * 而不是「一根线一根线地画」。
 * <h2>本轮重做：横向凹槽 → 竖向刻线组</h2>
 * 旧版碑面是三道<b>横向</b>凹槽，金光从中渗出。它只表达「碑在发光」，
 * 不表达「降了几费」。现在改成<b>竖向刻线组</b>：
 * <ul>
 *   <li><b>一条刻线 = 每张保留牌降 1 费</b>。原版画 1 道，叠加两张画 2 道</li>
 *   <li>刻线<b>自下而上</b>排列，亮起的道数 = {@code amount}</li>
 *   <li>结算时<b>自上而下</b>依次点亮，读作「一道一道压下去」
 *       （与施放的自下而上相反，正好把两件事区分开）</li>
 * </ul>
 * 为什么竖刻线比横向凹槽好：碑是<b>竖向</b>的，竖刻线顺着碑的走向，
 * 像量尺上的刻度；横线会把它切成几段，读成「砖块」。
 * <h2>一块碑的六个部件</h2>
 * <pre>
 *   ① 地面投影      扁椭圆暗影，把碑压在地上（没有它碑会浮起来）
 *   ② 暗侧面        右侧窄条，比立面暗 —— 这是「有厚度」的唯一证据
 *   ③ 亮立面        主块：上窄下宽、顶为尖顶（方尖碑）
 *   ④ 受光棱        左缘一道亮线，给方向光
 *   ⑤ 刻线组        竖向刻线，一条 = 降 1 费
 *   ⑥ 基线          碑底一道横线，读作「立在基座上」
 * </pre>
 * <h2>方尖碑的比例（模仿现实就要按现实比例，规范 §4.4）</h2>
 * <pre>
 *   底宽        0.185  × 碑高   （真实方尖碑底座约为高度的 1/10~1/5）
 *   尖顶长      0.16   × 碑高
 *   刻线间距    ≥ 2 × 刻线宽    （规范 §4.6）
 * </pre>
 * <h2>所有厚度都乘 {@code Settings.scale}</h2>
 * 旧版写的是 {@code Math.max(1.4f, halfW * 0.26f)}、{@code Math.max(1.2f, ...)} ——
 * <b>完全没有乘 scale</b>。在 2560 宽的屏上这些下限会整体缩水，
 * 于是「明明和别的牌写了差不多的值，却看起来更细」。这是虔信那轮查出的同一个 bug，
 * 这里一并修掉。
 * <h2>贴图选择（规范 §1.8）</h2>
 * 立面、侧面用 {@code Draw.glow} <b>拉伸成块</b>（石面是面，不是线）；
 * 受光棱与刻线用 {@code Draw.line}（等宽实线）。不使用星芒贴图。
 * 调用方负责开加色混合并用 get/set 还原。
 */
final class FoundationDraw {
    /** 立面透视：顶部比底部窄多少（0 = 不等宽）。 */
    private static final float TAPER = 0.26f;
    /** 暗侧面宽度占比（相对半宽）。 */
    private static final float SIDE_W = 0.52f;
    /** 尖顶高度占比。 */
    private static final float TIP_H = 0.16f;
    /** 刻线宽度（基准像素下限）。 */
    private static final float MARK_MIN_W = 5.0f;
    /** 碑底基线宽度（基准像素下限）。 */
    private static final float BASE_MIN_W = 6.0f;
    /** 受光棱宽度（基准像素下限）。 */
    private static final float RIDGE_MIN_W = 4.5f;

    private FoundationDraw() {
    }

    private static float px(float base) {
        return base * Settings.scale;
    }

    /**
     * 画一座计量方尖碑。
     *
     * @param cx     碑基中心 X
     * @param baseY  碑基 Y（碑从这里立起来）
     * @param height 碑高
     * @param halfW  碑底半宽
     * @param alpha  整体不透明度
     * @param groove 刻线发光强度 0~1（结算时由调用方抬高）
     * @param lit    已经亮起的刻线道数（0 ~ marks）
     * @param marks  总刻线道数 = 降费点数
     * @param rise   这块碑的升起进度 0~1；小于 1 时只有下半截露出来
     */
    static void monolith(SpriteBatch sb, float cx, float baseY, float height,
                         float halfW, float alpha, float groove,
                         int lit, int marks, float rise) {
        if (alpha <= 0.004f || height <= 2.0f || halfW <= 0.5f) {
            return;
        }
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        float show = MathUtils.clamp(rise, 0.0f, 1.0f);
        if (show <= 0.0f) {
            return;
        }
        // 升起：碑从地里往上长，所以可用高度随进度增加，基座不动
        float h = height * show;
        float topW = halfW * (1.0f - TAPER);
        float tipY = baseY + h;
        float shoulderY = baseY + h * (1.0f - TIP_H);

        // ---------- ① 地面投影：把碑压在地上 ----------
        Draw.glow(sb, cx, baseY, halfW * 5.4f, halfW * 1.5f, 0.0f,
                FoundationFx.STONE_DARK, a * 0.42f);

        // ---------- ② 暗侧面：右侧窄条 ----------
        // 一条竖直的窄块，比立面暗 —— 有它才有厚度
        Draw.glow(sb, cx + halfW * 0.70f, baseY + h * 0.5f,
                halfW * SIDE_W * 1.7f, h,
                0.0f, FoundationFx.STONE_DARK, a * 0.80f);

        // ---------- ③ 亮立面 ----------
        // glow 是中心定位的椭圆贴图，拉长后边缘会发虚；
        // 所以叠三层：宽软层做「光晕」、中层做「面」、窄亮层做「受光中心」
        Draw.glow(sb, cx, baseY + h * 0.5f, halfW * 3.1f, h,
                0.0f, FoundationFx.STONE, a * 0.16f);
        Draw.glow(sb, cx, baseY + h * 0.5f, halfW * 2.3f, h * 0.98f,
                0.0f, FoundationFx.STONE, a * 0.34f);
        Draw.glow(sb, cx - halfW * 0.18f, baseY + h * 0.5f, halfW * 1.5f, h * 0.94f,
                0.0f, FoundationFx.STONE, a * 0.40f);

        // ---------- ⑥ 基线：碑底一道横线，读作「立在基座上」 ----------
        Draw.line(sb, cx - halfW, baseY, cx + halfW, baseY,
                Math.max(px(BASE_MIN_W), halfW * 0.26f),
                FoundationFx.STONE_DARK, a * 0.80f);

        // 肩线：把「一块石头」夹出来，避免糊成一团光
        Draw.line(sb, cx - topW, shoulderY, cx + topW, shoulderY,
                Math.max(px(4.5f), halfW * 0.22f), FoundationFx.STONE, a * 0.58f);

        // ---------- 尖顶：两条斜线夹出三角 ----------
        Draw.line(sb, cx - topW, shoulderY, cx, tipY,
                Math.max(px(5.0f), halfW * 0.30f), FoundationFx.STONE, a * 0.92f);
        Draw.line(sb, cx + topW, shoulderY, cx, tipY,
                Math.max(px(5.0f), halfW * 0.30f), FoundationFx.STONE, a * 0.66f);
        // 尖顶内部填一层淡亮，别让它空着
        Draw.glow(sb, cx, shoulderY + (tipY - shoulderY) * 0.45f,
                halfW * 1.5f, (tipY - shoulderY) * 1.6f, 0.0f,
                FoundationFx.STONE, a * 0.30f);

        // ---------- ④ 受光棱：左缘亮线 ----------
        Draw.line(sb, cx - halfW, baseY, cx - topW, shoulderY,
                Math.max(px(RIDGE_MIN_W), halfW * 0.24f),
                FoundationFx.CORE, a * 0.60f);

        // ---------- ⑤ 刻线组：一条 = 每张保留牌降 1 费 ----------
        marks(sb, cx, baseY, h, halfW, shoulderY, a, groove, lit, marks, show);
    }

    /**
     * 碑面的竖向刻线组。单独抽出来，因为一次性演出里也要复用同样的几何。
     *
     * @param lit   已亮起的道数
     * @param marks 总道数
     */
    static void marks(SpriteBatch sb, float cx, float baseY, float h, float halfW,
                      float shoulderY, float alpha, float groove,
                      int lit, int marks, float show) {
        int n = MathUtils.clamp(marks, 0, FoundationFx.MAX_MARKS);
        if (n <= 0 || alpha <= 0.004f) {
            return;
        }
        float markW = Math.max(px(MARK_MIN_W), halfW * 0.34f);
        // 刻线的横向范围：居中，左右各留出碑宽的 55%
        float mx0 = cx - halfW * 0.55f;
        float mx1 = cx + halfW * 0.55f;
        int litN = MathUtils.clamp(lit, 0, n);

        for (int k = 0; k < n; ++k) {
            float ratio = FoundationFx.markRatio(k, n);
            float my = baseY + h * ratio;
            // 只有露出来的那部分才画（升起过程中刻线跟着露头）
            if (my > shoulderY || ratio > show) {
                continue;
            }
            boolean on = k < litN;
            if (on) {
                // 已亮：金色刻线 + 槽底光晕
                float gA = alpha * MathUtils.clamp(groove, 0.0f, 1.0f);
                Draw.line(sb, mx0, my, mx1, my, markW, FoundationFx.GOLD, gA * 0.95f);
                Draw.line(sb, mx0, my, mx1, my,
                        Math.max(px(4.5f), markW * 0.45f),
                        FoundationFx.CORE, gA * 0.70f);
                Draw.glow(sb, cx, my, halfW * 2.9f, halfW * 1.05f, 0.0f,
                        FoundationFx.GOLD, gA * 0.28f);
            } else {
                // 未亮：暗刻线，仍要看得见「这里有一条刻度」
                Draw.line(sb, mx0, my, mx1, my, markW,
                        FoundationFx.STONE_DARK, alpha * 0.70f);
            }
        }
    }

    /**
     * 碑升起时从地里迸出的碎石。
     * <p>
     * 位置与方向由 {@code seed} 正弦散列决定，同一特效每帧稳定，
     * 不会「每帧换一批石头」（规范 §三.7）。
     * <p>
     * 本轮修正：碎石原先用 {@code Math.max(1.0f, size * 0.42f)} 画极细的斜线，
     * 兜底后全被拉到 4.5px，看起来像一堆等粗短棍。改成<b>两层圆形光点</b>，
     * 既符合「与其变细不如换成点」（补充规范 §四），也更像石屑。
     *
     * @param progress 0~1，升起进度
     */
    static void debris(SpriteBatch sb, float cx, float baseY, float spread,
                       float alpha, float progress, int count, float seed) {
        if (alpha <= 0.004f || count <= 0) {
            return;
        }
        float q = MathUtils.clamp(progress, 0.0f, 1.0f);
        for (int i = 0; i < count; ++i) {
            // 左右交替向外抛
            float dir = (i % 2 == 0) ? -1.0f : 1.0f;
            float liftAng = MathUtils.lerp(0.30f, 1.15f,
                    0.5f + 0.5f * MathUtils.sin(seed + i * 3.7f));
            float speed = spread * (0.35f + 0.65f
                    * (0.5f + 0.5f * MathUtils.sin(seed * 2.1f + i * 5.3f)));
            float px0 = cx + dir * MathUtils.cos(liftAng) * speed * q;
            // 抛物线：先上后下
            float py = baseY + MathUtils.sin(liftAng) * speed * (q - q * q) * 2.6f;
            float size = Math.max(px(5.0f),
                    px(4.0f) * (0.7f + 0.6f * (0.5f + 0.5f * MathUtils.sin(i * 2.1f))));
            float a = alpha * (1.0f - 0.70f * q);
            // 石屑：外层柔光 + 内层实心暗点
            Draw.glow(sb, px0, py, size * 2.2f, size * 2.2f, 0.0f,
                    FoundationFx.STONE, a * 0.55f);
            Draw.glow(sb, px0, py, size * 0.9f, size * 0.9f, 0.0f,
                    FoundationFx.STONE_DARK, a * 0.65f);
        }
    }

    /**
     * 金光从碑面沿一道弧线流到<b>某一张具体的牌</b>上。
     * <p>
     * 这是本轮重做的第二个要点：旧版所有金光都飞向「手牌区正中央」这一个死点，
     * 而手牌是扇形排布的、被降费的牌有具体位置。现在每张牌各收一道，
     * 「哪几张变便宜了」才读得出来（规范 §2.7 一一对应）。
     *
     * @param toX,toY 目标牌的中心（{@code card.current_x / current_y}）
     * @param progress 0~1，流动进度
     */
    static void tollFlow(SpriteBatch sb, float fromX, float fromY,
                         float toX, float toY,
                         float alpha, float progress, int dots) {
        if (alpha <= 0.004f || dots <= 0) {
            return;
        }
        float q = MathUtils.clamp(progress, 0.0f, 1.0f);
        // 控制点：中点略向外，弧线先扬起来再落到牌上
        float ctrlX = (fromX + toX) * 0.5f - Math.abs(fromX - toX) * 0.10f;
        float ctrlY = Math.max(fromY, toY) + Math.abs(fromY - toY) * 0.18f;

        float baseSize = Math.max(px(6.0f), px(4.0f));
        for (int i = 0; i < dots; ++i) {
            // 每个光点落后一点，形成一串
            float t = MathUtils.clamp(q * 1.25f - i * 0.075f, 0.0f, 1.0f);
            if (t <= 0.0f) {
                continue;
            }
            float u = 1.0f - t;
            float px0 = u * u * fromX + 2.0f * u * t * ctrlX + t * t * toX;
            float py = u * u * fromY + 2.0f * u * t * ctrlY + t * t * toY;
            float fade = (1.0f - t) * 0.85f + 0.15f;
            float size = baseSize * (1.0f - 0.35f * t);
            Draw.glow(sb, px0, py, size * 3.2f, size * 2.4f, 0.0f,
                    FoundationFx.GOLD, alpha * fade * 0.30f);
            Draw.glow(sb, px0, py, size * 1.2f, size * 1.0f, 0.0f,
                    FoundationFx.CORE, alpha * fade * 0.55f);
        }
    }

    /**
     * 一张被降费的牌在收到金光时，自己在原地亮一下。
     * <p>
     * 这一笔是因果链的落点确认 —— 只有「光飞过去 + 牌自己亮了」两件事都发生，
     * 玩家才能确定<b>是这一张</b>变便宜了。
     *
     * @param q 0~1，落地闪光的进度
     */
    static void tollHit(SpriteBatch sb, float cx, float cy, float size,
                        float alpha, float q) {
        if (alpha <= 0.004f || q <= 0.0f || q >= 1.0f) {
            return;
        }
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        // 末端斜率为 0：余弦淡出，不硬切
        float fade = FastingDraw.cosineOut(q);
        float grow = 1.0f - (1.0f - q) * (1.0f - q);
        float r = Math.max(px(18.0f), size) * (0.60f + 0.85f * grow);

        // 牌周围一圈粗弧 + 中心一团金光
        Draw.arc(sb, cx, cy, r, r * 0.78f, 0.0f, 360.0f,
                Math.max(px(6.0f), r * 0.16f), FoundationFx.GOLD,
                a * 0.80f * fade, 28);
        Draw.arc(sb, cx, cy, r * 0.93f, r * 0.73f, 0.0f, 360.0f,
                Math.max(px(4.5f), r * 0.07f), FoundationFx.CORE,
                a * 0.55f * fade, 24);
        Draw.glow(sb, cx, cy, r * 1.4f, r * 1.1f, 0.0f,
                FoundationFx.GOLD, a * 0.22f * fade);
    }
}
