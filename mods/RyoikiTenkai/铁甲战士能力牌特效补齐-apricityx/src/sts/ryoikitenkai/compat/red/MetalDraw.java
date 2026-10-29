package sts.ryoikitenkai.compat.red;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;

/**
 * 「金属化」的画笔：<b>有厚度的分片护甲</b>。
 * <p>
 * <h2>为什么单独写一套</h2>
 * 这个补丁里另一张牌（进化）画的是「软、暖、半透、发光」的生物组织；
 * 金属化要画的是「硬、冷、实心、反光」的金属。两者的笔法正相反：
 * <ul>
 *   <li><b>生物</b>：{@link Draw#line}（实心条 + 大范围外发光），尖端更亮</li>
 *   <li><b>金属</b>：{@link Draw#bar}（<b>只有实心条</b>，无外发光），
 *       靠明暗分层与上沿受光边做体积</li>
 * </ul>
 * 用 {@code line} 画甲片会让整片甲发蓝光，读成「发光条」而不是「金属板」。
 * <h2>一片甲的五个部件</h2>
 * <pre>
 *   ① 底衬（暗钢，比甲体更长更厚）  把甲片「压」在身上，同时形成片间缝的深度
 *   ② 甲体（钢蓝，等宽实心条）      甲片本体
 *   ③ 上沿受光边（近白蓝，细）      唯一的高光来源，读作「上缘被光打到」
 *   ④ 下沿投影（暗钢，细，向下偏移）给厚度，避免看成贴纸
 *   ⑤ 两端铆钉（柔光点）            两枚小点，读作「铆上去的」
 * </pre>
 * <h2>体积感从哪来</h2>
 * 三层，缺一不可：
 * <ol>
 *   <li><b>深度缩小</b>：远侧甲片缩到 74%，近侧 100%</li>
 *   <li><b>深度明暗</b>：远侧不透明度只剩 45%，近侧 100%</li>
 *   <li><b>按深度排序绘制</b>：远侧先画，近侧压在上面 —— <b>这是「绕成一圈」的唯一来源</b></li>
 * </ol>
 * 只做前两条不做排序，甲片会按索引顺序叠，缝的遮挡关系会错，看起来是横排的贴纸。
 * <h2>高光沿环扫过</h2>
 * 每一片的高光强度 = {@code pow(max(0, cos(θ − sweep)), 10)}：一道窄高光
 * 随着扫描角绕环走，甲片依次亮一下。这是金属特有的静止状态，
 * 比「整体呼吸」更像金属，也比粒子省。
 * <h2>禁止项</h2>
 * <ul>
 *   <li>不用星芒 / 火星粒子（{@code ImpactSparkEffect}）—— 每回合都触发的东西不能放粒子</li>
 *   <li>不画细线：所有厚度经 {@link Draw#bar} 兜到 {@code ≥ 4.5px × scale}</li>
 *   <li>不用柔光点串成整圈（旧实现的 {@code GlowRing}）—— 那是「光环」不是「甲」</li>
 * </ul>
 * <h2>不每帧 new 数组</h2>
 * 排序用的索引与深度放在静态暂存里：同屏只有金属化的一个常驻实例在画，
 * 一次性演出与常驻不会嵌套调用（render 是串行的），所以复用安全。
 */
final class MetalDraw {
    /** 深度排序用的索引暂存。 */
    private static final int[] ORDER = new int[MetalFx.MAX_PLATES];
    /** 深度值暂存。 */
    private static final float[] DEPTH = new float[MetalFx.MAX_PLATES];

    private MetalDraw() {
    }

    private static float px(float base) {
        return base * Settings.scale;
    }

    /**
     * 建一次「先远后近」的绘制顺序，结果放在 {@link #ORDER} 里。
     * <p>
     * 甲片数 ≤ 8，用插入排序就够，且不产生任何分配。
     */
    private static void buildOrder(int n) {
        for (int i = 0; i < n; ++i) {
            ORDER[i] = i;
            DEPTH[i] = MetalFx.depthOf(MetalFx.plateAngle(i, n));
        }
        for (int i = 1; i < n; ++i) {
            int v = ORDER[i];
            float dv = DEPTH[v];
            int j = i - 1;
            while (j >= 0 && DEPTH[ORDER[j]] > dv) {
                ORDER[j + 1] = ORDER[j];
                j--;
            }
            ORDER[j + 1] = v;
        }
    }

    /**
     * 一片甲。
     *
     * @param cx,cy   环带中心
     * @param rx,ry   环带半径（已含呼吸 / 合拢的缩放）
     * @param angle   该片的中心角（弧度）
     * @param halfLen 沿切向的半长（像素）
     * @param thick   甲片厚度（像素）
     * @param depth   0 = 最远，1 = 最近
     * @param alpha   整体不透明度
     * @param spec    高光强度（扫光 / 合拢时提高）
     */
    static void plate(SpriteBatch sb, float cx, float cy, float rx, float ry,
                      float angle, float halfLen, float thick, float depth,
                      float alpha, float spec) {
        if (alpha <= 0.004f || halfLen <= 1.0f) {
            return;
        }
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f);
        float d = MathUtils.clamp(depth, 0.0f, 1.0f);
        // 深度决定大小与明暗
        float dScale = 0.74f + 0.26f * d;
        float len = halfLen * dScale;
        float t = Math.max(px(4.5f), thick * (0.78f + 0.22f * d));
        a *= 0.45f + 0.55f * d;
        if (a <= 0.004f) {
            return;
        }

        float cos = MathUtils.cos(angle);
        float sin = MathUtils.sin(angle);
        float platX = cx + cos * rx;
        float platY = cy + sin * ry;

        // 切向：椭圆的导数 (−rx·sin, ry·cos)
        float txx = -rx * sin;
        float tyy = ry * cos;
        float tlen = (float) Math.sqrt(txx * txx + tyy * tyy);
        if (tlen <= 0.0001f) {
            return;
        }
        txx /= tlen;
        tyy /= tlen;

        float x1 = platX - txx * len;
        float y1 = platY - tyy * len;
        float x2 = platX + txx * len;
        float y2 = platY + tyy * len;

        // ① 底衬：更长更厚的暗钢，形成片间缝的深度
        Draw.bar(sb, x1 - txx * len * 0.10f, y1 - tyy * len * 0.10f,
                x2 + txx * len * 0.10f, y2 + tyy * len * 0.10f,
                t * 1.45f, MetalFx.DEEP, a * 0.58f);

        // ② 甲体
        Draw.bar(sb, x1, y1, x2, y2, t,
                MetalFx.STEEL, a * (0.62f + 0.28f * spec));

        // ④ 下沿投影：向下偏移，给厚度
        Draw.bar(sb, x1, y1 - t * 0.34f, x2, y2 - t * 0.34f,
                Math.max(px(4.5f), t * 0.26f), MetalFx.DEEP, a * 0.52f);

        // ③ 上沿受光边：唯一的高光，扫光 / 合拢时最亮
        Draw.bar(sb, x1, y1 + t * 0.30f, x2, y2 + t * 0.30f,
                Math.max(px(4.5f), t * 0.30f),
                MetalFx.BRIGHT, a * (0.12f + 0.62f * spec));

        // ⑤ 两端铆钉
        float rv = Math.max(px(4.5f), t * 0.52f);
        for (int side = -1; side <= 1; side += 2) {
            float rvpX = platX + txx * len * 0.60f * side;
            float rvpY = platY + tyy * len * 0.60f * side;
            Draw.glow(sb, rvpX, rvpY, rv * 2.1f, rv * 2.1f, 0.0f,
                    MetalFx.DEEP, a * 0.30f);
            Draw.glow(sb, rvpX, rvpY, rv, rv, 0.0f,
                    MetalFx.BRIGHT, a * (0.14f + 0.52f * spec));
        }
    }

    /**
     * 整圈甲带：<b>按深度排序</b>后逐片画。
     *
     * @param plates     甲片数（= amount）
     * @param thick      甲片厚度
     * @param breath     呼吸相位 0~1
     * @param sweepPhase 扫光相位 0~1（高光绕环一周）
     * @param clank      合拢强度 0~1（获得格挡时由常驻衰减）
     */
    static void band(SpriteBatch sb, float cx, float cy, float rx, float ry,
                     int plates, float thick, float breath,
                     float sweepPhase, float clank, float alpha) {
        int n = MathUtils.clamp(plates, 1, MetalFx.MAX_PLATES);
        if (alpha <= 0.004f) {
            return;
        }
        // 合拢：整圈向内收 7%，再随 clank 衰减弹回
        float squeeze = 1.0f - 0.07f * MathUtils.sin(
                MathUtils.clamp(clank, 0.0f, 1.0f) * MathUtils.PI);
        float br = 1.0f + 0.015f * MathUtils.clamp(breath, 0.0f, 1.0f);
        float baseRx = rx * br;
        float rxs = baseRx * squeeze;
        float rys = ry * br * squeeze;
        float a = MathUtils.clamp(alpha, 0.0f, 1.0f)
                * (0.86f + 0.14f * MathUtils.clamp(breath, 0.0f, 1.0f));

        float halfLen = MetalFx.plateHalfLength(baseRx, n);
        float sweep = MathUtils.clamp(sweepPhase, 0.0f, 1.0f) * MathUtils.PI2;
        float kick = 0.55f * MathUtils.clamp(clank, 0.0f, 1.0f);

        buildOrder(n);
        for (int i = 0; i < n; ++i) {
            int k = ORDER[i];
            float angle = MetalFx.plateAngle(k, n);
            float c = MathUtils.cos(angle - sweep);
            float spec = c > 0.0f ? (float) Math.pow(c, 10.0) : 0.0f;
            spec = MathUtils.clamp(spec + kick, 0.0f, 1.15f);
            plate(sb, cx, cy, rxs, rys, angle, halfLen, thick,
                    DEPTH[k], a, spec);
        }
    }

    /**
     * 接缝亮起：沿环带画一道细亮弧。
     * <p>
     * 只在「合拢」那一瞬（约 0.4s）出现，作用是<b>把各片连成一个整体</b>：
     * 平时甲片之间是缝，合上时缝里透出光。
     * <p>
     * 它不是常驻轮廓 —— 甲片本身才是主体（规范 §2.2：不要让通用环成为主要形体）。
     */
    static void seamFlash(SpriteBatch sb, float cx, float cy, float rx, float ry,
                          float thick, float alpha, int plates) {
        if (alpha <= 0.004f) {
            return;
        }
        Draw.arc(sb, cx, cy, rx * 1.005f, ry * 1.005f, 0.0f, 360.0f,
                Math.max(px(4.5f), thick * 0.16f),
                MetalFx.BRIGHT, alpha, Math.max(26, plates * 7));
    }

    /**
     * 施放：整圈甲片逐片从外向内飞来。
     * <p>
     * <b>同样按深度顺序画</b>：远侧先、近侧后，缝的遮挡关系才不会错
     * （与常驻用同一套排序）。
     *
     * @param elapsed 已过时间
     * @param lockTime 单片飞入时长
     * @param step     逐片错开
     */
    static void lockBand(SpriteBatch sb, float cx, float cy, float rx, float ry,
                         int plates, float thick, float elapsed,
                         float lockTime, float step) {
        int n = MathUtils.clamp(plates, 1, MetalFx.MAX_PLATES);
        float halfLen = MetalFx.plateHalfLength(rx, n);
        buildOrder(n);
        for (int i = 0; i < n; ++i) {
            int k = ORDER[i];
            float t0 = k * step;
            if (elapsed < t0) {
                continue;
            }
            float q = MathUtils.clamp((elapsed - t0) / Math.max(0.001f, lockTime),
                    0.0f, 1.0f);
            float angle = MetalFx.plateAngle(k, n);
            lockPlate(sb, cx, cy, rx, ry, angle, halfLen, thick, DEPTH[k], q);
        }
    }

    /**
     * 施放用：一片甲从外向内飞来，{@code q = 1} 时锁在环上。
     *
     * @param q 0~1，该片自己的进度
     */
    static void lockPlate(SpriteBatch sb, float cx, float cy, float rx, float ry,
                          float angle, float halfLen, float thick,
                          float depth, float q) {
        float t = MathUtils.clamp(q, 0.0f, 1.0f);
        if (t <= 0.0f) {
            return;
        }
        // pow2Out：飞进来时前快后慢，像被「吸」到位
        float e = 1.0f - (1.0f - t) * (1.0f - t);
        float scale = MathUtils.lerp(1.65f, 1.0f, e);
        // 入位瞬间过曝：越接近 1，高光越强
        float spec = 0.20f + 0.95f * MathUtils.clamp(t * 1.35f, 0.0f, 1.0f);
        plate(sb, cx, cy, rx * scale, ry * scale, angle, halfLen, thick,
                depth, t, spec);
        // 入位闪光
        if (t > 0.90f) {
            float f = (t - 0.90f) / 0.10f;
            float platX = cx + MathUtils.cos(angle) * rx * scale;
            float platY = cy + MathUtils.sin(angle) * ry * scale;
            Draw.glow(sb, platX, platY, thick * 2.6f * f, thick * 2.0f * f, 0.0f,
                    MetalFx.BRIGHT, 0.55f * f);
        }
    }
}
