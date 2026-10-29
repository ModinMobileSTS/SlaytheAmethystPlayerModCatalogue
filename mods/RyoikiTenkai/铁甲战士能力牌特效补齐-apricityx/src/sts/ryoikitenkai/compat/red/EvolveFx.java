package sts.ryoikitenkai.compat.red;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.powers.AbstractPower;

/**
 * 「进化」（Evolve）的规格、几何、配色与事件入口。
 * <p>
 * <h2>机制（规范 §2.3：视觉来自机制）</h2>
 * 卡与力量两边都按字节码核实过：
 * <pre>
 * // Evolve.use()  —— 一次性，没有任何 VFX
 * addToBot(new ApplyPowerAction(p, p, new EvolvePower(p, magicNumber), magicNumber));
 * // magicNumber 原版 1，升级 +1 变 2
 *
 * // EvolvePower.onCardDraw(AbstractCard card)  —— 每次抽牌都会调
 * if (card.type == CardType.STATUS &amp;&amp; !owner.hasPower("No Draw")) {
 *     this.flash();
 *     addToBot(new DrawCardAction(owner, this.amount));   // ← amount = 额外抽几张
 * }
 * </pre>
 * 三个事实必须先分清：
 * <ol>
 *   <li><b>触发条件是「抽到的是状态牌」</b>，不是任何一张牌。
 *       状态牌 = 伤口 / 灼伤 / 眩晕 / 黏液 —— 诅咒不算</li>
 *   <li><b>{@code amount} 是「额外抽几张」</b>，原版 1 / 升级 2。
 *       它不是层数，也不是强度百分比</li>
 *   <li><b>有一道前置闸门</b>：{@code owner.hasPower("No Draw")} 时不触发。
 *       这时原版什么都不做，演出也必须不发</li>
 * </ol>
 * <h2>旧实现的问题</h2>
 * <h3>一、它和「金属化」占同一块画面</h3>
 * 两张牌的常驻都是<b>贴地的一圈同心椭圆环</b>：
 * <pre>
 *   进化   GlowRing.ellipse(cx, gy+5, rx = hw×1.30, ry = rx×0.26, 绿)
 *   金属化 GlowRing.ellipse(cx, gy+5, rx = hw×1.42, ry = rx×0.24, 钢蓝)
 * </pre>
 * 几何（贴地椭圆环）、位置（脚下）、动静（环在转 + 呼吸起伏）三项完全相同，
 * 只有颜色不同 —— 正是规范 §2.2 反复点名的坑：<b>几何不换，颜色换多少次都没用。</b>
 * 两张牌同时挂着时，玩家看到两圈几乎重合的环，分不清哪圈是谁的。
 * <h3>二、完全没有表达「抽到的是状态牌」</h3>
 * 旧触发演出是「9 颗光点从脚下往上冲 + 一圈地面光带」。
 * 这次触发的语义是：<b>我抽到了一张脏牌 → 它反而让我多抽一张</b>。
 * 「从脚下往上冲」与「抽到一张牌」之间没有任何对应关系，
 * 而且它和施放演出（3 道地面光带 + 10 颗上冲光点）几乎一样，
 * 玩家看不出「刚才是触发还是刚打出来」。
 * <h3>三、用了星芒</h3>
 * {@code GlintEffect} 用 {@code ImageMaster.TINY_STAR} 画十字星。
 * 星芒是原版卡面 / 奖章的装饰语汇，混进战斗特效会读成「贴了个装饰」。
 * 现已改成柔光斑 + 细亮弧（规范 §1.9）。
 * <h3>四、施放没有演出</h3>
 * {@code Evolve.use()} 没有任何 VFX，旧 {@code castEvolve} 只画了
 * 「地面光带 + 上冲光点」，没说清「这张牌装上了什么」。
 * <h2>设计：背后向上生长的三叉光芽</h2>
 * 「进化」这个词的语义是<b>向上生长</b>，而它的效果是
 * <b>把一张坏牌转化成一个额外的机会</b>。所以常驻做成
 * <b>从背后升起的三股藤蔓状光芽</b>：
 * <ul>
 *   <li><b>三股向上生长、到顶分叉的光芽</b>，竖、贴身、在角色<b>背后</b>；
 *       背后是为了不挡住判定与卡面（{@code renderBehind = true}）</li>
 *   <li>每约 2.2s（兴奋时 1.1s）一道生长脉冲自下而上跑过三股芽，
 *       尖端随之轻轻摆动 —— 读作「一直在长」</li>
 *   <li>股数 = 1 + {@code amount}（原版 1 → 2 股，升级 2 → 3 股）。
 *       强度变化是<b>多一股芽</b>，而不是「光点更密」</li>
 * </ul>
 * <b>一个芽 = 这条能力能额外抽出的一张牌。</b>
 * 这是本补丁里第一条「一一对应」，与观者那边「一页纸 = 一张洞见」是同一套做法。
 * <h2>触发：脏牌 → 额外的机会</h2>
 * 抽到状态牌时：
 * <ol>
 *   <li>三股芽<b>同时向上窜一截</b>、分叉张开（「被喂了一口」）</li>
 *   <li>那张状态牌<b>周围箍上一圈绿弧</b>，而且弧会<b>跟着它飞</b>（「是这一张」）</li>
 *   <li>从那张牌<b>拉一道上弧汇进芽尖</b>（「被吃进去」）</li>
 *   <li>芽尖<b>绽出 {@code amount} 枚光苞</b>，再<b>抛向右下角的抽牌堆</b>
 *       （「换成了额外抽牌」）</li>
 * </ol>
 * 这条链把机制说全了：<b>脏牌在哪 → 被吃进去 → 换成额外抽牌</b>。
 * <h2>关键时序依据：能拿到那张牌的真实位置</h2>
 * {@code AbstractPlayer.draw(int)} 的顺序（已按字节码核实）：
 * <pre>
 * c.current_x = CardGroup.DRAW_PILE_X;      // ← 先摆到抽牌堆位置
 * c.current_y = CardGroup.DRAW_PILE_Y;
 * c.drawScale = 0.12f;                      // ← 此时还是缩小的
 * c.triggerWhenDrawn();
 * this.hand.addToHand(c);
 * this.drawPile.removeTopCard();
 * for (AbstractPower p : this.powers) p.onCardDraw(c);   // ← 我们挂在这里
 * </pre>
 * 所以 {@code onCardDraw} 被调用时，那张牌的 {@code current_x/current_y}
 * <b>已经等于抽牌堆坐标</b>，{@code drawScale} 才 0.12。
 * 而它随后会一路飞向手牌，所以我们<b>每帧读 card 的当前位置</b>，
 * 让标记与吸收弧跟着它走 —— 这样「是哪一张」不会因为牌飞走而失去指向。
 * <h2>为什么不读手牌位置（与「确立基础」相反）</h2>
 * 观者的确立基础读的是<b>手牌里每张保留牌的 {@code current_x/current_y}</b>，
 * 因为它的效果对象是「手里已有的那几张」。
 * <p>
 * 进化的效果对象是「<b>刚刚抽上来的那一张</b>」，那一张此刻<b>不在手牌布局里</b>。
 * 所以这里读的是<b>事件参数里的 card</b>，而不是去手牌里找。
 * <blockquote>
 * 两者并不矛盾，是同一条原则的两次应用：
 * <b>光要落到「效果作用的那张具体的牌」上</b> —— 只是那张牌在两次里不在同一个地方。
 * </blockquote>
 * <h2>空间签名（规范 §2.2）</h2>
 * <pre>
 *   进化      背后竖起的、会生长的三叉光芽    光（生物 / 蔓），贴身靠后
 *   金属化    横向环绕的钢蓝护甲光带          金属，贴身高亮
 * </pre>
 * 与金属化的四条区分：
 * <table border="1">
 *   <tr><td></td><td>金属化</td><td>进化</td></tr>
 *   <tr><td>几何</td><td>横向环绕的光带</td><td><b>竖向生长的三叉</b></td></tr>
 *   <tr><td>位置</td><td>贴地一圈</td><td><b>背后、从腰到头</b></td></tr>
 *   <tr><td>动静</td><td>环在转、甲面反光</td><td><b>自下而上生长、尖端摆动</b></td></tr>
 *   <tr><td>材质</td><td>金属（冷钢蓝、硬、反光）</td><td><b>生物（荧光绿、软、半透）</b></td></tr>
 * </table>
 * 四条全部不同。关键是<b>几何与位置</b>：一横一竖，一个贴地一个背后。
 * <h2>颜色</h2>
 * 沿用原版进化图标的荧光绿，但把旧版偏纯的 {@code (0.42, 1.00, 0.58)}
 * 往生物感调一档（加一点黄绿倾向、降低一点纯度），
 * 与金属化的冷钢蓝形成「暖生物 / 冷金属」的对照。
 * <h2>频次分级（规范 §2.11）</h2>
 * <ul>
 *   <li>抽到状态牌：<b>每回合可能好几次</b> —— 完全静音、不震屏</li>
 *   <li>施放：一局几次 —— 轻震屏 + 音效（原版无 VFX，必须补）</li>
 * </ul>
 */
final class EvolveFx {
    /** 主色：荧光绿（生物感，略偏黄绿）。 */
    static final Color BIO = new Color(0.50f, 0.98f, 0.52f, 1.0f);
    /** 亮芯：接近白绿的尖端。 */
    static final Color CORE = new Color(0.90f, 1.00f, 0.86f, 1.0f);
    /** 暗部：芽的背面与根部，给体积。 */
    static final Color DEEP = new Color(0.18f, 0.46f, 0.28f, 1.0f);

    /**
     * 最多画几股芽。
     * <p>
     * 股数 = 1 + {@code amount}：原版 1 → 2 股，升级 2 → 3 股。
     * 3 股是视觉上限，再多会在背后糊成一片。
     */
    static final int MAX_SPROUTS = 3;

    /** 芽离角色中轴的横向半跨（× 身体半宽）。 */
    private static final float SPAN = 0.52f;
    /** 芽的起点高度（× 身高）：腰以下，读作「从背后长出来」。 */
    private static final float BASE_RATIO = 0.30f;
    /** 芽的高度（× 身高）。 */
    private static final float HEIGHT_RATIO = 0.56f;

    /** 能力 ID（原版 {@code EvolvePower.POWER_ID} = Evolve，与卡牌 ID 相同）。 */
    private static final String POWER = RedEffectRegistry.EVOLVE_ID;

    /** 「无法抽牌」状态：原版在这里直接 return，不触发。 */
    private static final String NO_DRAW = "No Draw";

    private EvolveFx() {
    }

    // ---------------- 状态读取 ----------------

    /**
     * 要画几股芽 = 1 + 额外抽牌数。
     * <p>
     * {@code amount} 是「抽到状态牌时额外抽几张」（原版 1 / 升级 2）。
     * 所以「一共能提供几个机会」= 自身 1 + 额外 amount。
     */
    static int sprouts(AbstractCreature owner) {
        int n = 1;
        if (owner != null) {
            AbstractPower p = owner.getPower(POWER);
            if (p != null) {
                n = 1 + Math.max(1, Math.abs(p.amount));
            }
        }
        return MathUtils.clamp(n, 2, MAX_SPROUTS);
    }

    /** 额外抽牌数（每次触发的强度）。 */
    static int drawAmount(AbstractCreature owner) {
        int n = 1;
        if (owner != null) {
            AbstractPower p = owner.getPower(POWER);
            if (p != null) {
                n = Math.max(1, Math.abs(p.amount));
            }
        }
        return n;
    }

    /** 进化这张牌的特效是否开启。 */
    static boolean isOn() {
        return RedEffectRegistry.isOn(RedEffectRegistry.EVOLVE_ID);
    }

    /**
     * 现在是否不能触发（{@code No Draw} 在身）。
     * <p>
     * 原版 {@code EvolvePower.onCardDraw} 里这是<b>硬闸门</b>：
     * 有它时直接不触发，什么都不做。演出必须跟着不发（规范 §2.9）。
     */
    static boolean blocked(AbstractCreature owner) {
        return owner != null && owner.hasPower(NO_DRAW);
    }

    // ---------------- 几何（常驻与一次性演出共用） ----------------

    /** 芽的根部 X 跨度（像素）。 */
    static float span(AbstractCreature owner) {
        return BodyFx.halfWidth(owner) * SPAN;
    }

    /** 芽的根部 Y（腰以下的背后）。 */
    static float baseY(AbstractCreature owner) {
        return BodyFx.groundY(owner) + BodyFx.height(owner) * BASE_RATIO;
    }

    /** 芽的高度。 */
    static float sproutHeight(AbstractCreature owner) {
        return BodyFx.height(owner) * HEIGHT_RATIO;
    }

    /**
     * 吸收弧的终点 X：芽簇的中轴。
     * <p>
     * 光要落到「芽尖」上，而不是落在一个随便取的身体比例点上（规范 §2.18）。
     */
    static float tipX(AbstractCreature owner) {
        return BodyFx.centerX(owner);
    }

    /**
     * 吸收弧的终点 Y：<b>完全长成后</b>的芽尖高度。
     * <p>
     * = 根部 Y + 芽高。用它而不是 {@code groundY + 0.80×身高}：
     * 后者与真实的芽尖差了一截，光会停在芽上方或下方。
     */
    static float tipY(AbstractCreature owner) {
        return baseY(owner) + sproutHeight(owner);
    }

    /**
     * 第 {@code k} 股的根部 X。
     * <p>
     * <b>位置只由索引决定，不随总股数变化</b> —— 这样从 2 股变 3 股时，
     * 原有的两股完全不动，只是中间多长出一股（规范 §三.7）。
     * <p>
     * 但「每股该占哪个位置」要按总数选一套算法：
     * 2 股分左右，3 股是左中右。所以用固定的三槽位
     * （−1 / 0 / +1）按总数取子集，而不是按 {@code k / (n−1)} 均分 ——
     * 后者会让 2 股时的左右两股在变 3 股时被挤到两边。
     */
    static float sproutX(AbstractCreature owner, int k, int n) {
        float cx = BodyFx.centerX(owner);
        float sp = span(owner);
        // 固定三槽位：2 股取 {左, 右}；3 股取 {左, 中, 右}
        float slot;
        if (n <= 2) {
            slot = (k == 0) ? -1.0f : 1.0f;
        } else {
            slot = (k == 0) ? -1.0f : (k == 1 ? 0.0f : 1.0f);
        }
        return cx + sp * slot;
    }

    /** 第 {@code k} 股的根部 Y。中间那股略高一点，读作「主芽」。 */
    static float sproutBaseY(AbstractCreature owner, int k, int n) {
        float by = baseY(owner);
        if (n >= 3 && k == 1) {
            by += BodyFx.height(owner) * 0.04f;
        }
        return by;
    }

    /**
     * 第 {@code k} 股的「向哪边弯」。
     * <p>
     * 左右两股向外弯，中间那股直着长 —— 三股一起看像一簇草，
     * 而不是三根平行竖线。
     */
    static float sproutLean(int k, int n) {
        if (n <= 2) {
            return (k == 0) ? -1.0f : 1.0f;
        }
        return (k == 0) ? -1.0f : (k == 1 ? 0.0f : 1.0f);
    }

    // ---------------- 事件入口 ----------------

    /**
     * 打出这张牌（{@code Evolve.use()}）。
     * <p>
     * 原版<b>一个 VFX 都没有</b>，所以必须补：根部先亮一小片、
     * 芽逐股从背后抽上来、尖端绽开光苞并抛向抽牌堆。
     * 这一段要说清「这股芽是什么、有几股」，否则常驻出现时会显得没来由（规范 §2.19）。
     * <p>
     * 一局只几次 —— 允许轻震屏 + 音效（音效与震屏在登记表里发）。
     *
     * @param owner 能力持有者
     * @param card  刚被打出的这张牌，用来读它真实的额外抽牌数
     */
    static void onCast(AbstractCreature owner, AbstractCard card) {
        if (owner == null || !isOn()) {
            return;
        }
        int amount = 1;
        if (card != null) {
            int mn = Math.max(card.magicNumber, card.baseMagicNumber);
            if (mn > 0) {
                amount = mn;
            }
        }
        int sprouts = MathUtils.clamp(1 + amount, 2, MAX_SPROUTS);
        RedEffectRegistry.queueTop(new EvolveGrowthEffect(owner, sprouts, amount, 1.0f));
    }

    /**
     * 抽到一张<b>状态牌</b>（{@code EvolvePower.onCardDraw}）。
     * <p>
     * 这是这张牌<b>真正生效</b>的地方。演出是完整的一条因果链：
     * <ol>
     *   <li>那张状态牌的周围箍上一圈绿弧，并<b>跟着它飞</b>（「是这一张」）</li>
     *   <li>从它拉一道向上的弧汇进芽尖（「被吃进去」）</li>
     *   <li>芽自下而上窜一截、分叉张开（「转化」，见 {@link EvolveGrowthEffect}）</li>
     *   <li>芽尖绽出 {@code amount} 枚光苞，抛向抽牌堆方向（「换成了额外抽牌」）</li>
     * </ol>
     * 一回合可能好几次 —— <b>完全静音、不震屏</b>（规范 §2.11）。
     * <p>
     * 走 {@code queueTop}：光弧会从抽牌堆一路飞到身体上方，
     * 横跨手牌区与屏幕下半部分，走 {@code effectsQueue} 会被手牌盖住。
     *
     * @param owner 能力持有者
     * @param card  刚被抽到的那张状态牌（{@code current_x/y} 此时是抽牌堆坐标）
     */
    static void onStatusDrawn(AbstractCreature owner, AbstractCard card) {
        if (owner == null || card == null || !isOn()) {
            return;
        }
        // 原版的硬闸门：有 No Draw 时不触发，画面也不发
        if (blocked(owner)) {
            return;
        }
        RedEffectRegistry.queueTop(new EvolveGrowthEffect(owner,
                sprouts(owner), drawAmount(owner), 0.72f));
        // 弧的起点每帧读 card 的当前位置：牌会一路飞向手牌，标记要跟着它
        RedEffectRegistry.queueTop(new EvolveAbsorbEffect(owner, card,
                tipX(owner), tipY(owner), drawAmount(owner)));
    }

    /**
     * 抽牌堆在屏幕上的位置：光苞抛向那里。
     * <p>
     * 额外抽的牌确实来自抽牌堆，所以「换来的东西」往牌堆飞是有依据的落点，
     * 而不是往「手牌区中心」那种死点飞（规范 §2.18）。
     */
    static float drawPileX() {
        return CardGroup.DRAW_PILE_X;
    }

    static float drawPileY() {
        return CardGroup.DRAW_PILE_Y;
    }

    /** 供画笔用的统一缩放像素。 */
    static float px(float base) {
        return base * Settings.scale;
    }
}
