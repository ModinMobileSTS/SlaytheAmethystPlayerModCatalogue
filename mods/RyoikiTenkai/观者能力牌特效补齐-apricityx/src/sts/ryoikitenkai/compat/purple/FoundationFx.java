package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.AbstractPower;

/**
 * 「确立基础」（Establishment）的规格、几何、配色与事件入口。
 * <p>
 * <h2>机制（规范 §2.3：视觉来自机制）</h2>
 * 已对照字节码核实。这张牌有三个环节，必须先读全：
 * <pre>
 * // Establishment.use()  —— 一次性
 * addToBot(new ApplyPowerAction(p, p, new EstablishmentPower(p, magicNumber), magicNumber));
 * // magicNumber = 1；升级<b>不改</b> amount，只给 isInnate = true
 *
 * // EstablishmentPower.atEndOfTurn(boolean)
 * this.flash();
 * addToBot(new EstablishmentPowerAction(this.amount));
 *
 * // EstablishmentPowerAction.update()  —— 真正的效果
 * for (AbstractCard c : AbstractDungeon.player.hand.group) {
 *     if (!c.selfRetain &amp;&amp; !c.retain) continue;
 *     c.modifyCostForCombat(-this.discountAmount);   // ← 每张保留牌 −amount 费
 * }
 * </pre>
 * 所以有两件事必须分清：
 * <ol>
 *   <li><b>{@code amount} 是「每回合把每张保留牌降几费」</b>，<b>不是</b>「层数」。
 *       原版 = 1，叠加两张才是 2</li>
 *   <li><b>受影响的牌只有「保留」牌</b>（{@code selfRetain || retain}）。
 *       手牌没有保留牌时，原版遍历一遍<b>什么都不做</b></li>
 * </ol>
 * <h2>本轮重做的两个根本问题</h2>
 * <h3>问题一：「一块碑 = 一层能力」这个映射是错的</h3>
 * 旧实现用 {@code amount} 当碑数，并且写着「只增不减」。
 * 但 {@code amount} 是<b>降费点数</b>，不是层数：
 * <ul>
 *   <li>原版 amount = 1，玩家正常永远只看到<b>一块碑</b>，
 *       而「每回合把手里所有保留牌降 1 费」的效果强度完全没表达出来</li>
 *   <li>叠加到 amount = 2 时画两块碑，但读者会理解成「有两层能力」，
 *       而真实含义是「降 2 费」。<b>数字对得上，语义对不上</b></li>
 *   <li>「只增不减」是死代码：碑数只由能力层数决定，
 *       而能力层数在这局里几乎不会降，所以那段逻辑永远不会触发</li>
 * </ul>
 * 现在改成<b>一个有刻线的计量柱</b>（见下）：<b>一条刻线 = 每张保留牌降 1 费</b>。
 * 这样 amount 从 1 变 2 时是「柱子上多一道亮刻线」，而不是「多一块碑」——
 * 读作「强度增加了」，而不是「层数增加了」。
 * <h3>问题二：金光「流向手牌」到手牌正中央，落点是死的</h3>
 * 旧实现把金光的终点写成 {@code handX()/handY()} = 屏幕水平中点偏下。
 * 但：
 * <ul>
 *   <li>手牌是<b>扇形排布</b>的，中间的牌未必是保留牌，甚至可能<b>没有牌</b></li>
 *   <li>被降费的牌有具体的 N 张、具体的位置，而金光全都飞向同一个点</li>
 *   <li>于是「哪几张变便宜了」这条最重要的信息<b>完全没有传达</b></li>
 * </ul>
 * 现在改成<b>每张真的被降费的牌各收一道金光</b>，落点读
 * {@code card.current_x / card.current_y}（每张牌自己的实时位置）。
 * 数量与位置都由机制决定（规范 §2.7 一一对应）。
 * <h2>设计：一块计量碑 + 会流动的刻度</h2>
 * <ul>
 *   <li><b>立在角色左侧的一座方尖碑</b>。竖向、单侧、贴地，
 *       与如水的横面、心之堡垒的环绕、虔信 / 先见之明的离身元素全部分开</li>
 *   <li><b>碑面竖向刻线组</b>：一条刻线 = 每张保留牌降 1 费。
 *       亮刻线数 = {@code amount}，所以强度变化是「刻线多一道」，
 *       不是「多一块碑」</li>
 *   <li><b>回合末：刻线自上而下依次点亮</b>，同时<b>每张被降费的牌收一道金光</b>，
 *       落点各自不同 —— 这一段才是「便宜送到手上」的证据</li>
 *   <li><b>没有保留牌时</b>：只空敲一下，刻线不亮、金光不发
 *       （原版确实什么都没做，演得热闹就是骗人，规范 §2.9）</li>
 * </ul>
 * <h2>为什么保留「碑」这个母题</h2>
 * 石碑是本系列里唯一「单侧、竖向、实心、贴地」的形态，四个维度都独立，
 * 没有撞任何其它牌。要改的是<b>它承载的信息</b>（层数 → 降费强度），
 * 不是形态本身。这也符合规范 §2.6「状态耦合优先于叠加新元素」。
 * <h2>空间签名（规范 §2.2）</h2>
 * <pre>
 *   如水      水平铺展 + 下坠      流动的水（横的面，脚下绕身）
 *   战歌      头顶斜面            平面符号
 *   心之堡垒  绕身环绕的立方体    立体包壳，快速反转
 *   猛虎下山  落在敌人身上        斜向裂痕
 *   确立基础  身侧竖立的方尖碑    石（实心），单侧、贴地、静止
 *   斋戒      能量槽 → 胸口的球   飞行轨迹 + 原版双圈
 *   虔信      胸口的慢速刻度光轮  极慢自转、可数刻度、向心收束
 *   先见之明  身前上方的光框斜列  空心矩形 ×N、悬空缓浮、扫描显影
 *   研习      左下角的待归档页堆  扇形摊开的实心纸页、轻翻、落进抽牌堆
 *   涅槃      贴身的层叠六边护壁  多层棱壳、跟着格挡增减、受击变薄
 * </pre>
 * 与其余九张全部不撞：<b>只有这一张是实心、贴地、单侧、竖向、静止</b>。
 * <h2>颜色</h2>
 * 石材：{@link #STONE} 暖灰，暗部 {@link #STONE_DARK}。
 * 刻线金：{@link #GOLD}。与涅槃的青铜（偏绿）区分开 ——
 * 一个是「被压低的费用」（暖金），一个是「护住身体的甲」（冷青铜）。
 */
final class FoundationFx {
    /** 石材亮部。 */
    static final Color STONE = new Color(0.80f, 0.76f, 0.68f, 1.0f);
    /** 石材暗部：碑体暗侧面与地面投影，体积感全靠它。 */
    static final Color STONE_DARK = new Color(0.32f, 0.28f, 0.24f, 1.0f);
    /** 刻线金：碑面刻线渗出，是「费用被压低」的唯一色彩信号。 */
    static final Color GOLD = new Color(1.00f, 0.80f, 0.40f, 1.0f);
    /** 受光棱与最亮处。 */
    static final Color CORE = new Color(1.00f, 0.96f, 0.88f, 1.0f);

    /**
     * 最多画几道刻线。
     * <p>
     * 刻线数 = {@code amount}（每张保留牌降几费）。原版 1，叠加两张才是 2。
     * 4 道是视觉上限：再多刻线间距就小于线宽，会糊成一片。
     */
    static final int MAX_MARKS = 4;

    /** 碑离角色中轴的横向偏移（× 身体半宽）。刚好在身体边缘之外、不挡立绘。 */
    private static final float OFFSET = 0.86f;
    /** 碑高（× 身高）。 */
    private static final float HEIGHT = 0.40f;
    /** 碑底半宽（× 身高）。 */
    private static final float WIDTH = 0.072f;

    private FoundationFx() {
    }

    // ---------------- 状态读取 ----------------

    /** 本补丁的卡牌 ID：确立基础（原版 {@code Establishment.ID} = Establishment）。 */
    private static final String CARD = PurpleEffectRegistry.CARD_ESTABLISHMENT;

    /** 能力 ID（原版 {@code EstablishmentPower.POWER_ID}）。 */
    private static final String POWER = PurpleEffectRegistry.POWER_ESTABLISHMENT;

    /**
     * 每张保留牌每回合被降几费 = 要画几道亮刻线。
     * <p>
     * 直接读 {@code EstablishmentPower.amount}。原版 1，升级<b>不改</b>
     * （升级只给 {@code isInnate}），叠加两张才是 2。
     */
    static int discount(AbstractCreature owner) {
        int n = 1;
        if (owner != null && owner.powers != null) {
            AbstractPower p = owner.getPower(POWER);
            if (p != null) {
                n = Math.max(1, Math.abs(p.amount));
            }
        }
        return MathUtils.clamp(n, 1, MAX_MARKS);
    }

    /**
     * 施放时的降费点数。
     * <p>
     * 施放发生在 {@code useCard} 的 <b>Prefix</b>，此刻能力没挂上，
     * {@link #discount} 只能拿默认 1。改读卡面数字（与先见之明 / 研习 / 涅槃同一处理）。
     */
    static int discountForCast(AbstractCard card) {
        int n = 1;
        if (card != null) {
            int mn = Math.max(card.magicNumber, card.baseMagicNumber);
            if (mn > 0) {
                n = mn;
            }
        }
        return MathUtils.clamp(n, 1, MAX_MARKS);
    }

    /** 确立基础这张牌的特效是否开启。 */
    static boolean isOn() {
        return PurpleEffectRegistry.isOn(CARD);
    }

    // ---------------- 手牌里真正会被降费的牌 ----------------

    /**
     * 手牌里真正会被 {@code EstablishmentPowerAction} 降费的牌数。
     * <p>
     * 判据必须与原版<b>逐字一致</b>：{@code selfRetain || retain}。
     * 这是「视觉来自机制」的核心 —— 没有保留牌时原版什么都不做，
     * 演出也必须退化成空敲（规范 §2.9）。
     *
     * @return 会被降费的牌数；手牌为空或没有保留牌时返回 0
     */
    static int retainedInHand() {
        return retainedX().length;
    }

    /**
     * 会被降费的那几张牌的实时屏幕坐标。
     * <p>
     * 每两个一组：{@code [x0, y0, x1, y1, ...]}。
     * 用 {@code card.current_x / current_y} —— 这是原版自己画手牌用的位置，
     * 所以金光一定落在玩家看到的那张牌上（手牌是扇形排布，位置每帧都不同）。
     * <p>
     * 每次调用都会重新扫一遍手牌（手牌在结算过程中会变），
     * 但只在一次性演出里调用几次，不在每帧路径上。
     *
     * @return 扁平坐标数组；没有保留牌时返回长度 0 的数组
     */
    static float[] retainedX() {
        AbstractPlayer p = AbstractDungeon.player;
        if (p == null || p.hand == null) {
            return EMPTY;
        }
        CardGroup hand = p.hand;
        if (hand.group == null || hand.group.isEmpty()) {
            return EMPTY;
        }
        // 先数一遍，避免用 ArrayList 再转数组（不多分配集合对象）
        int n = 0;
        for (int i = 0; i < hand.group.size(); ++i) {
            AbstractCard c = hand.group.get(i);
            if (c != null && (c.selfRetain || c.retain)) {
                ++n;
            }
        }
        if (n <= 0) {
            return EMPTY;
        }
        // 上限：手牌最多 10 张，留一点余量
        int cap = Math.min(n, 12);
        float[] out = new float[cap * 2];
        int k = 0;
        for (int i = 0; i < hand.group.size() && k < cap; ++i) {
            AbstractCard c = hand.group.get(i);
            if (c == null || (!c.selfRetain && !c.retain)) {
                continue;
            }
            out[k * 2] = c.current_x;
            out[k * 2 + 1] = c.current_y;
            ++k;
        }
        // 刚好用完就不裁；返回整段有效长度，调用方按 len/2 算张数
        return out;
    }

    private static final float[] EMPTY = new float[0];

    // ---------------- 几何（常驻与一次性演出共用） ----------------

    /** 碑的横向中心：角色左侧，身体边缘之外。 */
    static float monoX(AbstractCreature owner) {
        return BodyFx.centerX(owner) - BodyFx.halfWidth(owner) * OFFSET;
    }

    /** 碑的底面 Y：正好在脚下，不悬浮。 */
    static float monoBaseY(AbstractCreature owner) {
        return BodyFx.groundY(owner);
    }

    /** 碑高。 */
    static float monoHeight(AbstractCreature owner) {
        return BodyFx.height(owner) * HEIGHT;
    }

    /** 碑底半宽。 */
    static float monoWidth(AbstractCreature owner) {
        return Math.max(8.0f * Settings.scale, BodyFx.height(owner) * WIDTH);
    }

    /** 第 {@code k} 条刻线的纵向位置（碑底起算的相对高度 0~1）。 */
    static float markRatio(int k, int total) {
        // 从下往上排，越上面越晚点亮（读作「一层层压上去」）
        int n = Math.max(1, total);
        float bottom = 0.22f;
        float top = 0.72f;
        if (n == 1) {
            return (bottom + top) * 0.5f;
        }
        return MathUtils.lerp(bottom, top, k / (float) (n - 1));
    }

    /** 手牌区中心的兜底坐标（只在读不到具体牌时用）。 */
    static float handX() {
        return Settings.WIDTH * 0.5f;
    }

    static float handY() {
        return Settings.HEIGHT * 0.155f;
    }

    // ---------------- 事件入口 ----------------

    /**
     * 打出这张牌（{@code Establishment.use()}）。
     * <p>
     * 原版<b>没有任何 VFX</b>，所以必须补：碑从地里升起、刻线逐道亮起。
     * 这一段要说清「这块碑是什么、上面的刻线是什么」，否则常驻出现时会显得没来由。
     * <p>
     * 一局只几次 —— 允许轻震屏 + 音效。
     *
     * @param owner 能力持有者（玩家）
     * @param card  刚被打出的这张牌，用来读它真实的降费点数
     */
    static void onCast(AbstractCreature owner, AbstractCard card) {
        if (owner == null || !isOn()) {
            return;
        }
        PurpleEffectRegistry.queueTop(new FoundationRiseEffect(owner,
                discountForCast(card)));
    }

    /**
     * 回合末结算（{@code EstablishmentPower.atEndOfTurn}）。
     * <p>
     * 演出<b>严格分两种</b>（规范 §2.9）：
     * <ul>
     *   <li><b>手牌有保留牌</b>：刻线自上而下依次点亮 + 每张被降费的牌各收一道金光</li>
     *   <li><b>手牌没有保留牌</b>：只空敲一下 —— 碑微沉、一圈极淡的环，
     *       刻线不亮、金光完全不发。因为原版此刻确实什么都没发生</li>
     * </ul>
     * 每回合都会发生 —— <b>不震屏、不音效</b>。
     */
    static void onTurnEnd(AbstractCreature owner) {
        if (owner == null || !isOn()) {
            return;
        }
        PurpleEffectRegistry.queue(new FoundationBurstEffect(owner,
                retainedX().length, discount(owner)));
    }
}
