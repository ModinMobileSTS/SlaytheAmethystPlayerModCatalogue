package sts.ryoikitenkai.compat.red;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.powers.AbstractPower;

/**
 * 「金属化」（Metallicize）的规格、几何、配色与事件入口。
 * <p>
 * <h2>机制（规范 §2.3：视觉来自机制）</h2>
 * 卡与力量两边都按字节码核实过：
 * <pre>
 * // Metallicize.use()  —— 一次性，没有任何 VFX
 * addToBot(new ApplyPowerAction(p, p, new MetallicizePower(p, this.magicNumber), this.magicNumber));
 * // magicNumber 原版 3，升级 +1 变 4
 *
 * // MetallicizePower  —— POWER_ID = "Metallicize"（与卡牌 ID 相同）
 * public void playApplyPowerSfx() { CardCrawlGame.sound.play("POWER_METALLICIZE", 0.05f); }
 *
 * public void atEndOfTurnPreEndTurnCards(boolean isPlayer) {
 *     this.flash();
 *     addToBot(new GainBlockAction(this.owner, this.owner, this.amount));
 * }
 * </pre>
 * 三个必须分清的事实：
 * <ol>
 *   <li><b>触发点是「回合末结算前的力量回调」</b> {@code atEndOfTurnPreEndTurnCards}，
 *       不是 {@code atEndOfTurn}，也不是 {@code GainBlockAction}。挂错就永远不出现</li>
 *   <li><b>{@code amount} 是「每回合获得几点格挡」</b>，原版 3 / 升级 4。
 *       它是可数的「几点护甲」，不是层数百分比</li>
 *   <li><b>施放音效原版已经有了</b>：{@code MetallicizePower.playApplyPowerSfx()}
 *       会播 {@code POWER_METALLICIZE}。所以施放演出<b>不要再补第二个音效</b></li>
 * </ol>
 * <h2>旧实现的问题</h2>
 * <h3>一、它没有「甲」的形体，只有一团贴地柔光环</h3>
 * 旧常驻是一次 {@code GlowRing.ellipse(cx, gy+5, rx = hw×1.42, ry = rx×0.24, 钢蓝)} ——
 * 密排柔光点串成的贴地椭圆环。它没有边界、没有厚度、没有分片，
 * 读起来是「脚下有蓝光」，而不是「身上有护甲」。
 * <p>
 * 更严重的是：它和改版前的「进化」用的是<b>同一个函数、同一组参数</b>，只换颜色
 * （{@code hw×1.30} 绿 / {@code hw×1.42} 钢蓝）。进化已改成背后竖向光芽，
 * 金属化不能继续停在「贴地光带」这个通用 buff 造型上。
 * <h3>二、触发演出是一堵火星墙</h3>
 * 旧触发是「环向内收一下 + 两道斜掠高光 + <b>10 颗 {@code ImpactSparkEffect}</b>」。
 * 火星是原版<b>卡面 / 奖章</b>的装饰语汇；而且这条触发<b>每回合都会来一次</b>，
 * 等于每回合朝身上糊一层火星（规范 §2.11 频次分级）。
 * <h3>三、{@code amount} 完全看不出来</h3>
 * amount 是「每回合 +3 / +4 格挡」，是本作里最可数的数字之一。
 * 旧实现只有「反光间隔按层数缩短」，玩家看不出 3 与 4 的差别。
 * <h3>四、施放没有「怎么装上去」</h3>
 * 旧施放是「向外推开的环 + 斜掠高光 + 22 颗火星」，仍然没有甲片实体，
 * 也没说清「这套护甲是几片、怎么合上的」。
 * <h2>设计：腰肋处环绕身体的分片护甲带</h2>
 * 「金属化」的语义是<b>皮肤变成金属，每回合长出一层护甲</b>。
 * 所以常驻做成<b>围在腰肋处的一圈分片护甲</b>：
 * <ul>
 *   <li><b>甲片数 = amount</b>：原版 3 → 3 片，升级 4 → 4 片。
 *       <b>一片甲 = 每回合 1 点格挡</b></li>
 *   <li>每片是<b>有厚度的实心甲板</b>：底衬（暗钢）+ 甲体（钢蓝）+ 上沿受光边
 *       + 下沿投影 + 两端铆钉</li>
 *   <li>位置在<b>腰肋</b>（{@code height × 0.50}），<b>环绕</b>身体：
 *       按椭圆做透视（{@code rx = halfWidth × 1.16，ry = rx × 0.26}）</li>
 *   <li><b>按深度排序绘制</b>：远侧甲片先画、更暗更小；近侧后画、更亮更大 ——
 *       这是「看起来绕在身上一圈」的唯一来源（规范 §5.3）</li>
 *   <li>平时只有两件事：极轻的呼吸（半径 ±1.5%），
 *       以及<b>一道高光沿环缓慢扫过</b>（约 3.4s 一圈）——
 *       金属被光扫过，是金属特有的静止状态</li>
 * </ul>
 * <h2>材质：甲体走普通混合，光走加色混合</h2>
 * 这是本补丁第一个「实心材质」的牌，与进化的「纯发光」完全不同：
 * <ul>
 *   <li><b>甲体 / 底衬 / 投影 / 铆钉</b> → 普通混合（{@code 770 × 771}）。
 *       只有普通混合能让甲片<b>遮住</b>身体，读成「穿在身上」。
 *       加色混合画出来的甲片只能是一根发亮的蓝条</li>
 *   <li><b>接缝亮起</b> → 加色混合。它是光，不是材质</li>
 * </ul>
 * 所以本类的配色是按「普通混合下的固有色」定的：中灰蓝的钢，
 * 不是加色混合需要的那种高亮度值。
 * <h2>为什么甲片按总数重新分度</h2>
 * 甲片是<b>同一个环上的刻度</b>，不是各自独立的实体。
 * 所以 3 片变 4 片时应当<b>重新分度</b>（每片间隔从 120° 变 90°）。
 * 这与观者「确立基础」的刻线同理；而进化的三股芽是<b>彼此独立的实体</b>，
 * 所以用固定槽位取子集、只增不减（规范 §三.7 的两种情形）。
 * <h2>获得格挡：甲片合拢 + 接缝亮起</h2>
 * 回合末真正拿到格挡时（{@code atEndOfTurnPreEndTurnCards}）：
 * <ol>
 *   <li>整圈甲片<b>同时向内合拢约 7%</b>，然后弹回 —— 读作「护甲合上」</li>
 *   <li>甲片之间的<b>接缝</b>透出一圈钢蓝光，随合拢一起衰减</li>
 *   <li>每片的受光边与铆钉一起短暂增亮</li>
 * </ol>
 * <b>不放火星、不震屏、不出声</b>：这件事每回合都发生一次（规范 §2.11）。
 * <h2>施放：逐片飞来、逐片锁上</h2>
 * 原版 {@code Metallicize.use()} 没有 VFX，所以补一段：
 * 各片从 {@code 半径 × 1.65} 处<b>由外向内</b>飞来，依次<b>锁在环上</b>（错开 0.06s），
 * 锁上的一瞬间过曝；最后整圈接缝扫亮一遍。
 * 一局只几次 → 允许<b>轻震屏</b>；<b>音效由原版力量自己播</b>，这里不重复。
 * <h2>空间签名（规范 §2.2）</h2>
 * <pre>
 *   金属化   腰肋处横向环绕的分片甲带     金属，前方、有厚度
 *   进化     背后竖起的、会生长的三叉光芽  生物，背后、竖向
 * </pre>
 * 四条区分：
 * <table border="1">
 *   <tr><td></td><td>金属化</td><td>进化</td></tr>
 *   <tr><td>几何</td><td><b>横向的分片环带（离散甲板）</b></td><td>竖向的三叉光芽</td></tr>
 *   <tr><td>位置</td><td><b>腰肋、环住身体（前方）</b></td><td>背后、从腰到头</td></tr>
 *   <tr><td>动静</td><td><b>呼吸 + 高光沿环扫过 + 合拢</b></td><td>自下而上生长、尖端摆动</td></tr>
 *   <tr><td>材质</td><td><b>金属（冷钢蓝、硬边、实心、反光）</b></td><td>生物（荧光绿、软、半透）</td></tr>
 * </table>
 * 四条全不同。关键是<b>一横一竖、一个绕在身上一个从背后长出来</b>。
 * <h2>频次分级（规范 §2.11）</h2>
 * <ul>
 *   <li>获得格挡：<b>每回合一次</b> —— 静音、不震屏、不放粒子</li>
 *   <li>施放：一局几次 —— 轻震屏（音效由原版力量提供）</li>
 * </ul>
 */
final class MetalFx {
    /**
     * 甲体：中灰蓝的钢。
     * <p>
     * <b>按普通混合下的固有色定</b>（甲体不发光）：比「加色混合用的亮蓝」暗一档，
     * 这样叠在角色身上才像金属板，而不是一条发亮的蓝带。
     */
    static final Color STEEL = new Color(0.55f, 0.64f, 0.74f, 1.0f);
    /** 受光边：接近白的冷光，甲片上缘唯一的高光。 */
    static final Color BRIGHT = new Color(0.93f, 0.97f, 1.00f, 1.0f);
    /** 暗钢：底衬、投影、铆钉外圈。 */
    static final Color DEEP = new Color(0.19f, 0.24f, 0.31f, 1.0f);

    /**
     * 最多画几片甲。
     * <p>
     * 原版 3 / 升级 4；再往上（mod 叠层）最多 8 片 —— 超过 8 片，
     * 甲片在腰肋这个尺度上会窄到读不出是「甲」。
     */
    static final int MAX_PLATES = 8;

    /** 环带高度（× 身高）：腰肋处，读作「护甲束在身上」。 */
    private static final float BAND_RATIO = 0.50f;
    /** 环带横向半径（× 身体半宽）：略微超出身体，才是「绕在外面」。 */
    private static final float RX_RATIO = 1.16f;
    /** 椭圆透视压扁比：正面看一圈环绕带，纵向必然被压扁。 */
    private static final float SQUASH = 0.26f;
    /** 甲片厚度（× 身高）。 */
    private static final float THICK_RATIO = 0.085f;
    /** 甲片厚度的基准下限（像素）。 */
    private static final float THICK_MIN = 9.0f;
    /** 一片甲占所用弧长的比例：剩下的就是片间缝。 */
    private static final float COVER = 0.62f;

    /** 能力 ID（原版 {@code MetallicizePower.POWER_ID} = Metallicize，与卡牌 ID 相同）。 */
    private static final String POWER = RedEffectRegistry.METALLICIZE_ID;

    private MetalFx() {
    }

    // ---------------- 状态读取 ----------------

    /** 甲片数 = 每回合获得的格挡点数。 */
    static int plates(AbstractCreature owner) {
        int n = 3;
        if (owner != null) {
            AbstractPower p = owner.getPower(POWER);
            if (p != null) {
                n = Math.abs(p.amount);
            }
        }
        return MathUtils.clamp(n, 1, MAX_PLATES);
    }

    /**
     * 施放时的甲片数：<b>此刻能力还没挂上</b>，只能读卡片的 {@code magicNumber}。
     * <p>
     * 与进化 / 观者那几张一样：{@code use()} 的 prefix 里 {@code getPower()} 一定返回 null，
     * 所以这里读 {@code max(magicNumber, baseMagicNumber)}。
     */
    static int platesFromCard(AbstractCard card) {
        int n = 3;
        if (card != null) {
            n = Math.max(card.magicNumber, card.baseMagicNumber);
        }
        return MathUtils.clamp(n, 1, MAX_PLATES);
    }

    /** 金属化这张牌的特效是否开启。 */
    static boolean isOn() {
        return RedEffectRegistry.isOn(RedEffectRegistry.METALLICIZE_ID);
    }

    // ---------------- 几何（常驻、施放、合拢共用） ----------------

    /** 环带中心 Y（腰肋）。 */
    static float bandY(AbstractCreature owner) {
        return BodyFx.groundY(owner) + BodyFx.height(owner) * BAND_RATIO;
    }

    /** 环带横向半径。 */
    static float bandRx(AbstractCreature owner) {
        return BodyFx.halfWidth(owner) * RX_RATIO;
    }

    /** 环带纵向半径（透视压扁后的那一半）。 */
    static float bandRy(AbstractCreature owner) {
        return bandRx(owner) * SQUASH;
    }

    /** 甲片厚度（像素）。 */
    static float plateThick(AbstractCreature owner) {
        return Math.max(THICK_MIN * Settings.scale,
                BodyFx.height(owner) * THICK_RATIO);
    }

    /**
     * 第 {@code k} 片的<b>中心角</b>（弧度，数学方向：0 向右、逆时针增大）。
     * <p>
     * 从 {@code −90°}（椭圆最低点 = 正对镜头）开始，按 {@code 360/n} 均分。
     * <b>角度依赖总数</b>：甲片是同一个环上的刻度，3 片变 4 片时应当重新分度
     * （与「确立基础」的刻线同理）。
     */
    static float plateAngle(int k, int n) {
        int count = Math.max(1, n);
        return -MathUtils.PI * 0.5f
                + MathUtils.PI2 * ((k % count) / (float) count);
    }

    /**
     * 深度：1 = 最近（椭圆最低点，正对镜头），0 = 最远（椭圆最高点）。
     * <p>
     * 环绕带的「一圈感」全靠它：远侧更小更暗、近侧更大更亮，并按它排序绘制。
     * 没有它，8 片甲会糊成一条横线。
     */
    static float depthOf(float angleRad) {
        return MathUtils.clamp((1.0f - MathUtils.sin(angleRad)) * 0.5f, 0.0f, 1.0f);
    }

    /**
     * 单片甲沿切向的半长（像素）。
     * <p>
     * 按 {@code n} 分度后的弧长乘 {@link #COVER}：片与片之间留缝，
     * 才读得出「分片」，也才能让接缝亮起时有意义。
     */
    static float plateHalfLength(float rx, int n) {
        int count = Math.max(1, n);
        float arc = MathUtils.PI2 * rx / count;
        return arc * COVER * 0.5f;
    }

    // ---------------- 事件入口 ----------------

    /**
     * 打出这张牌（{@code Metallicize.use()}）。
     * <p>
     * 原版<b>没有 VFX</b>，所以补：各片从外向内飞来、逐片锁上、最后接缝扫亮。
     * 这一段要说清「这套护甲是几片」（= amount），否则常驻出现时会显得没来由。
     * <p>
     * <b>不播第二遍音效</b>：{@code MetallicizePower.playApplyPowerSfx()} 已经播了
     * {@code POWER_METALLICIZE}；再补一个会变成双响。
     *
     * @param owner 能力持有者
     * @param card  刚被打出的这张牌，用来读它真实的护甲点数
     */
    static void onCast(AbstractCreature owner, AbstractCard card) {
        if (owner == null || !isOn()) {
            return;
        }
        RedEffectRegistry.queue(new MetalLockEffect(owner, platesFromCard(card)));
    }
}
