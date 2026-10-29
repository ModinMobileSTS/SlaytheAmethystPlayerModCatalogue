package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.AbstractPower;

/**
 * 「先见之明」（Wireheading）的规格、几何、配色与事件入口。
 * <p>
 * <h2>机制（规范 §2.3：视觉来自机制）</h2>
 * 已对照字节码核实。这张牌<b>卡与力量两边都读了</b>：
 * <pre>
 * // Foresight.use()  —— 一次性，而且原版自己已经有强视觉
 * addToBot(new VFXAction(new BorderFlashEffect(Color.VIOLET, true)));
 * addToBot(new VFXAction(new GiantEyeEffect(p.hb.cX, p.hb.cY + 300f * sc,
 *                                           new Color(1f, 0.8f, 1f, 0f))));
 * addToBot(new ApplyPowerAction(p, p, new ForesightPower(p, this.magicNumber), ...));
 *
 * // ForesightPower.atStartOfTurn()  —— 每回合
 * if (drawPile.isEmpty()) addToTop(new EmptyDeckShuffleAction());
 * this.flash();
 * addToBot(new ScryAction(this.amount));      // ← 占卜 amount 张
 * </pre>
 * 三个事实决定了整个画面：
 * <ol>
 *   <li><b>这张牌干什么</b>：每回合开始<b>占卜 N 张</b>（N = 能力层数，原版 3 / 升级 4）。
 *       占卜 = <b>提前看见还没抽到的牌</b>，可以弃掉任意张</li>
 *   <li><b>生效时机是回合开始</b>，不是施放时。施放只是挂上能力</li>
 *   <li><b>施放时原版已经有 BorderFlash 与 GiantEye</b>（一只巨眼在头顶 300px 处扫过）。
 *       所以<b>施放段不该再造一个大演出</b>去抢它的戏，只补互补的东西</li>
 * </ol>
 * <h2>这张牌缺的是什么</h2>
 * 缺的不是「施放那一下」—— 那一下原版已经很强。缺的是<b>两件事</b>：
 * <ul>
 *   <li><b>常驻</b>：能力挂着的时候，身上没有任何东西表明「我每回合会占卜」</li>
 *   <li><b>每回合占卜的那一刻</b>：真正生效时完全无反馈</li>
 * </ul>
 * <h2>为什么弃用原来的通用骨架（FLAME_EYE）</h2>
 * 之前的实现是 {@link Motif#FLAME_EYE}：<b>头顶紫红火焰环 + 中心一只金眼</b>。
 * 两条都站不住：
 * <ol>
 *   <li><b>材质不对</b>：火是铁甲战士的语汇（燃烧 / 喷火 / 灼烧）。这张牌的核心是
 *       「看见还没抽到的牌」，跟火焰没有任何关系，属于「按卡图猜」而不是「按机制」</li>
 *   <li><b>画了眼</b>：{@code Draw.ring(...)} + 一条竖线拼出来的是<b>眼睛</b>，
 *       属于规范明令禁止的具象物（兽 / 人 / 手 / 脸）。小尺寸下也读不出是眼，
 *       只会变成「一个里面有竖线的椭圆」</li>
 * </ol>
 * 所以这张牌改成独立实现，和虔信、如水、心之堡垒一样脱离通用骨架。
 * <h2>设计：身前上方的「预视光框」斜列</h2>
 * 主视觉是<b>悬在角色身前上方的一列空心光框</b>：
 * <ul>
 *   <li><b>一个框 = 每回合能占卜的一张牌</b>。N 张能力就给 N 个框，数量一一对应</li>
 *   <li>框是<b>空心的</b> —— 读作「还没显影、还没被看见的那一张」。
 *       内部极淡的填充既不让它变成纯线条，也不给它任何内容</li>
 *   <li>占卜那一刻，框被<b>依次横向扫描显影</b>：一道冷白光带自上而下扫过、
 *       框边加粗、内部亮起，然后退回未显影状态。
 *       这正是「把盖着的牌逐张翻开看一遍」</li>
 *   <li>平时每个框各自呼吸、整体极慢地缓浮，读作「一直悬在那里等着下一次预视」</li>
 * </ul>
 * <h2>空间签名（规范 §2.2）</h2>
 * <pre>
 *   如水      水平铺展 + 下坠      流动的水（横的面，脚下绕身）
 *   战歌      头顶斜面            平面符号
 *   心之堡垒  绕身环绕的立方体    立体包壳，快速反转
 *   猛虎下山  落在敌人身上        斜向裂痕
 *   确立基础  身侧竖立的石碑      竖向的体，单侧、不环绕
 *   斋戒      能量槽 → 胸口的球   飞行轨迹 + 原版双圈
 *   虔信      胸口的慢速刻度光轮  极慢自转、可数刻度、向心收束
 *   先见之明  身前上方的光框斜列  空心矩形 ×N、悬空缓浮、扫描显影
 * </pre>
 * 四个维度逐项比：
 * <table border="1">
 *   <caption>与最接近的两张牌对比</caption>
 *   <tr><td></td><td>确立基础</td><td>虔信</td><td>先见之明</td></tr>
 *   <tr><td>几何</td><td>尖顶方碑（实心立块）</td><td>圆环 + 径向刻度</td><td><b>空心矩形框</b></td></tr>
 *   <tr><td>位置</td><td>角色<b>左侧</b>、贴地</td><td>胸口<b>正中</b></td><td><b>身前上方</b>（右侧偏上）</td></tr>
 *   <tr><td>动静</td><td>静止，升起时才有动作</td><td>极慢自转</td><td><b>悬空缓浮，占卜时依次扫描</b></td></tr>
 *   <tr><td>材质</td><td>石（不透明，有暗侧面）</td><td>发光体（环）</td><td><b>半透明光框</b></td></tr>
 * </table>
 * 矩形与圆在最基本的形状上就不同，位置也分别落在「左侧 / 正中 / 右下偏上」三处。
 * <h2>数量来自机制（规范 §2.7「一一对应」+ §2.4「一层一个可数实体」）</h2>
 * <b>一个框 = 一张能占卜的牌</b>。这与确立基础「一块碑 = 一层能力」是同一套做法，
 * 但这里更精确：框数直接等于<b>这一次真的会被摊开的牌数</b>。
 * <h2>颜色</h2>
 * 原版 {@code Foresight.use()} 用 {@code Color.VIOLET} 做边框闪光、
 * {@code GiantEyeEffect} 传的是粉白。所以主色取<b>偏蓝的紫罗兰</b>，亮部取<b>冰白</b>：
 * <ul>
 *   <li>与虔信的「紫白 → 神性粉」区分：那边偏暖（终点是粉），这边偏冷（终点是冰白）</li>
 *   <li>冷色 + 半透明，天然读作「尚未落定的东西」，与「占卜」的语义一致</li>
 * </ul>
 * <h2>频次分级（规范 §2.11）</h2>
 * <ul>
 *   <li>占卜：<b>每回合</b> —— 无音效、无震屏</li>
 *   <li>施放：一局几次 —— 原版已有 BorderFlash 与 GiantEye，
 *       本补丁<b>不再加声音与震屏</b>，只让光框成形</li>
 * </ul>
 */
final class ForesightFx {
    /** 主色：偏蓝的紫罗兰（原版 BorderFlash 用 Color.VIOLET，这里往冷里调一档）。 */
    static final Color VIOLET = new Color(0.66f, 0.58f, 1.00f, 1.0f);
    /** 亮部：冰白。显影扫描的最高亮处。 */
    static final Color ICE = new Color(0.92f, 0.98f, 1.00f, 1.0f);
    /**
     * 未显影的框：冷暗。
     * <p>
     * 不能太暗 —— 虔信踩过一次「未点亮刻度比背景还暗，等于没画」的坑。
     * 这里取到能在暗背景上看出「有一个框在那里」的亮度。
     */
    static final Color DORMANT = new Color(0.40f, 0.44f, 0.72f, 1.0f);

    /**
     * 最多画几个框。
     * <p>
     * 原版 3 / 升级 4，金色眼睛遗物再 +2 就是 6。再多就排到敌人身上了。
     */
    static final int MAX_FRAMES = 6;

    /** 整列的中心相对角色中轴的横向偏移（× 半宽）：往身前（右）偏。 */
    private static final float GROUP_OFFSET_X = 1.24f;
    /** 整列的基准高度（× 身高）：头顶附近，不与胸口正中、也不与左侧石碑重叠。 */
    private static final float BASE_Y = 0.94f;
    /** 整列的上扬角度（度）。斜向，与「确立基础」的水平成列区分开。 */
    private static final float SLOPE_DEG = 13.0f;
    /** 单个框的宽 / 高（× 半宽）。竖长方形，接近一张牌的比例。 */
    private static final float FRAME_W = 0.52f;
    private static final float FRAME_H = 0.76f;
    /** 相邻两框的间距（× 半宽）。间距 ÷ 框宽 ≈ 1.27，不会挤在一起。 */
    private static final float SPACING = 0.66f;
    /** 每个框自身的倾斜（度）。斜放的牌，比正放的更「未落定」。 */
    private static final float TILT_DEG = -9.0f;

    private ForesightFx() {
    }

    // ---------------- 状态读取 ----------------

    /** 本补丁的卡牌 ID：先见之明（原版 {@code Foresight.ID} = Wireheading）。 */
    private static final String CARD = PurpleEffectRegistry.CARD_FORESIGHT;

    /** 能力 ID（原版 {@code ForesightPower.POWER_ID} = WireheadingPower）。 */
    private static final String POWER = PurpleEffectRegistry.POWER_FORESIGHT;

    /** 金色眼睛遗物：原版 {@code ScryAction} 的构造里会让占卜张数 +2。 */
    private static final String GOLDEN_EYE = "GoldenEye";

    /**
     * 这次占卜真的会摊开几张牌 = 要画几个框。
     * <p>
     * 口径已经与原版 {@code ScryAction} 的构造逐行对齐：
     * <pre>
     * // ForesightPower.atStartOfTurn() 传进来的就是这个 amount
     * new ScryAction(this.amount)
     *
     * // ForesightPower.amount 就是能力层数（3 / 升级 4）
     * // 然后 ScryAction 的构造里：
     * if (AbstractDungeon.player.hasRelic("GoldenEye")) {
     *     AbstractDungeon.player.getRelic("GoldenEye").flash();
     *     this.amount += 2;
     * }
     * </pre>
     * 所以：<b>能力层数 +（有金色眼睛 ? 2 : 0）</b>。
     * 不算遗物的话，带了这个遗物的玩家会看到「3 个框但摊开了 5 张」，
     * 数字对不上就失去了「一一对应」的可读性（规范 §2.7）。
     * <p>
     * 原版的 {@code Math.min(amount, drawPile.size())} 那一层<b>不在这里处理</b>：
     * 抽牌堆不够时读牌堆真实数量会在「每回合变化」中来回跳，
     * 框数跟着跳会读成 glitch。宁可保持「能占卜几张」这个稳定语义。
     */
    static int frames(AbstractCreature owner) {
        int n = 3;
        if (owner != null && owner.powers != null) {
            AbstractPower p = owner.getPower(POWER);
            if (p != null) {
                n = Math.max(1, Math.abs(p.amount));
            }
        }
        return withRelic(owner, n);
    }

    /**
     * 直接把卡面上的数字当层数用（施放时走这条）。
     * <p>
     * 施放发生在 {@code useCard} 的 Prefix，这时 {@code ApplyPowerAction}
     * 还没执行、能力<b>还没挂到身上</b>，所以 {@link #frames} 只能拿到默认的 3。
     * 而卡面数字（原版 3 / 升级 4）就是施加之后的能力层数，直接用它更准 ——
     * 否则「打出升级版」会画出 3 个框，而实际能力是 4 层。
     */
    static int framesForCast(AbstractCreature owner, AbstractCard card) {
        int n = 3;
        if (card != null) {
            int mn = Math.max(card.magicNumber, card.baseMagicNumber);
            if (mn > 0) {
                n = mn;
            }
        }
        return withRelic(owner, n);
    }

    /** 叠加金色眼睛遗物的额外 2 张。 */
    private static int withRelic(AbstractCreature owner, int base) {
        AbstractPlayer pl = AbstractDungeon.player;
        if (pl != null && owner == pl && pl.hasRelic(GOLDEN_EYE)) {
            base += 2;
        }
        return MathUtils.clamp(base, 1, MAX_FRAMES);
    }

    /** 先见之明这张牌的特效是否开启。 */
    static boolean isOn() {
        return PurpleEffectRegistry.isOn(CARD);
    }

    // ---------------- 几何（常驻与一次性演出共用） ----------------

    /** 整列的中心 X：身前（右侧），不压在立绘上。 */
    static float groupX(AbstractCreature owner) {
        return BodyFx.centerX(owner) + BodyFx.halfWidth(owner) * GROUP_OFFSET_X;
    }

    /** 整列的基准 Y：接近头顶，但不盖脸。 */
    static float groupY(AbstractCreature owner) {
        return BodyFx.groundY(owner) + BodyFx.height(owner) * BASE_Y;
    }

    /** 单框宽度。 */
    static float frameW(AbstractCreature owner) {
        return Math.max(22.0f * Settings.scale, BodyFx.halfWidth(owner) * FRAME_W);
    }

    /** 单框高度。 */
    static float frameH(AbstractCreature owner) {
        return Math.max(30.0f * Settings.scale, BodyFx.halfWidth(owner) * FRAME_H);
    }

    /** 相邻两框的间距（像素）。 */
    static float spacing(AbstractCreature owner) {
        return Math.max(26.0f * Settings.scale, BodyFx.halfWidth(owner) * SPACING);
    }

    /** 每个框自身的倾角（度）。 */
    static float tilt() {
        return TILT_DEG;
    }

    /**
     * 第 {@code i} 个框的中心 X。
     * <p>
     * 以整列中心<b>对称排布</b>（{@code i − (n−1)/2}），而不是从固定起点往右排 ——
     * 这样框数从 3 变成 5 时，整列是「向两侧长开」而不是「整体右移」，
     * 玩家的参照点不会跳动（规范 §三.7「数量变化不能跳位」）。
     */
    static float slotX(AbstractCreature owner, int i, int n) {
        float u = i - (n - 1) * 0.5f;
        float rad = SLOPE_DEG * MathUtils.degreesToRadians;
        return groupX(owner) + MathUtils.cos(rad) * spacing(owner) * u;
    }

    /** 第 {@code i} 个框的中心 Y。整列上扬 13°，越往右越高。 */
    static float slotY(AbstractCreature owner, int i, int n) {
        float u = i - (n - 1) * 0.5f;
        float rad = SLOPE_DEG * MathUtils.degreesToRadians;
        return groupY(owner) + MathUtils.sin(rad) * spacing(owner) * u;
    }

    // ---------------- 事件入口 ----------------

    /**
     * 打出这张牌（{@code Foresight.use()} → {@code ApplyPowerAction(ForesightPower)}）。
     * <p>
     * 原版在这里已经有 {@code BorderFlashEffect(VIOLET)} 与 {@code GiantEyeEffect}，
     * 所以本补丁<b>不再造第二个大演出</b>（规范 §2.5：同一件事演两遍会互相稀释）。
     * 这里只做一件互补的小事：让光框<b>从无到有地弹出来</b>，
     * 补上「这套预视装置被装上了」这一层 —— 位置在身前，与头顶的巨眼不重叠。
     *
     * @param owner 能力持有者（玩家）
     * @param card  刚被打出的这张牌，用来读它真实的占卜张数（升级后是 4）
     */
    static void onCast(AbstractCreature owner, AbstractCard card) {
        if (owner == null || !isOn()) {
            return;
        }
        // 强度 0.75：弱于每回合的占卜，因为原版已经放了边框闪光与巨眼
        PurpleEffectRegistry.queue(new ForesightScryEffect(owner,
                framesForCast(owner, card), 0.75f));
    }

    /**
     * 回合开始占卜（{@code ForesightPower.atStartOfTurn} → {@code ScryAction(amount)}）。
     * <p>
     * 这是这张牌<b>真正生效</b>的地方，每回合一次：
     * 一列光框被<b>依次横向扫描显影</b>，读作「把盖着的牌一张张翻开看」。
     * <p>
     * 走<b>普通队列</b>（{@link PurpleEffectRegistry#queue}）而不是顶层：
     * {@code ScryAction} 会在同一帧打开选牌界面，顶层特效会压在选牌界面上显得很脏；
     * 光框位于头顶上方，本来也不会被手牌遮住。
     * <p>
     * 每回合都会发生 —— <b>不震屏、不音效</b>。
     *
     * @param owner 能力持有者（玩家）
     */
    static void onScry(AbstractCreature owner) {
        if (owner == null || !isOn()) {
            return;
        }
        PurpleEffectRegistry.queue(new ForesightScryEffect(owner,
                frames(owner), 1.0f));
    }
}
