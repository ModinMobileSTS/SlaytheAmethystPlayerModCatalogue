package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.AbstractPower;

/**
 * 「涅槃」（Nirvana）的规格、几何、配色与事件入口。
 * <p>
 * <h2>机制（规范 §2.3：视觉来自机制）</h2>
 * 已对照字节码核实。这张牌<b>卡与力量两边都读了</b>：
 * <pre>
 * // Nirvana.use()  —— 一次性，没有任何 VFX
 * this.addToBot(new ApplyPowerAction(p, p, new NirvanaPower(p, this.magicNumber), ...));
 *
 * // NirvanaPower.onScry()  —— 每次占卜都会调一次
 * this.flash();
 * this.addToBot(new GainBlockAction(this.owner, this.amount));
 * </pre>
 * 两个事实：
 * <ol>
 *   <li><b>这张牌产出的东西是格挡</b>，不是伤害、不是牌、不是能量。
 *       所以视觉主体必须读作「<b>一层护住我的东西</b>」</li>
 *   <li><b>触发时机是「占卜」</b>，而占卜由别的牌引发（先见之明、洞见、
 *       任何 Scry 效果）。它是<b>副产物</b>，不是主角</li>
 * </ol>
 * <h2>关键约束：不得与「先见之明」抢同一个位置</h2>
 * {@code ScryAction.update()} 里会遍历<b>所有</b> power 调 {@code onScry()}：
 * <pre>
 * for (AbstractPower p : AbstractDungeon.player.powers) {
 *     p.onScry();
 * }
 * </pre>
 * 所以<b>涅槃与先见之明在同一帧、因同一件事触发</b>。
 * 先见之明的表现是「身前上方一列空心光框被扫描显影」。
 * 如果涅槃也画成「围绕身体的占卜图案」，两套特效会在同一位置叠在一起，
 * 玩家分不清哪个在演什么。
 * <p>
 * 结论：<b>涅槃只表达「格挡增加」，完全不画「占卜/预视」的语义</b>。
 * 分工是：
 * <ul>
 *   <li>先见之明 → 演「看见了什么」（扫描显影）</li>
 *   <li>涅槃 → 演「因此护住了自己」（护壁增厚）</li>
 * </ul>
 * <h2>旧实现为什么必须推翻</h2>
 * 原来是通用骨架的 {@link Motif#MANDALA}：<b>头顶金边紫心的八瓣曼陀罗</b>。
 * 五条问题：
 * <ol>
 *   <li><b>用了 {@code Draw.star}</b>：每个花瓣尖端一颗星芒。
 *       这是硬性禁止的元素（本补丁里最后一处未修的违规）</li>
 *   <li><b>画了细线</b>：{@code 3.4f * sc}、{@code 2.4f * sc}、{@code 1.4f * sc}、{@code 1.8f * sc}。
 *       底层 {@link Draw#MIN_LINE} 会把它们兜到 4.5px，但<b>粗细全部被拉平</b>，
 *       于是内外层失去了层次，只剩一团等宽线条</li>
 *   <li><b>八瓣莲花是具象物</b>：观者是佛教意象，画莲花很自然 ——
 *       但规范禁止具象物，而且小尺寸下一朵「八瓣花」会读成一团放射状碎线</li>
 *   <li><b>位置在头顶、几何是环绕</b>：与心之堡垒（环绕）、
 *       战歌（头顶斜面）叠在同一块画面</li>
 *   <li><b>完全没表达格挡</b>：曼陀罗是「冥想 / 圆满」的符号，
 *       但这张牌产出的是<b>格挡值</b>。符号对不上产物</li>
 * </ol>
 * <h2>设计：贴身的「层叠护壁」</h2>
 * 主视觉是<b>贴着身体轮廓、一层层向外叠开的六边护壁</b>：
 * <ul>
 *   <li><b>一层护壁 = 一次占卜给的格挡</b>。层数可数（规范 §2.7 一一对应）</li>
 *   <li><b>六边形</b>而不是圆形：六边形有明确的<b>棱</b>和<b>内外面</b>，
 *       天然读作「结构 / 甲壳」，而圆读作「光晕 / 环」</li>
 *   <li><b>贴身</b>而不是离身：最内层半径只有 {@code 0.62 × 半宽}，
 *       紧贴着立绘叠开 —— 这是全套里唯一<b>贴着身体轮廓</b>的常驻</li>
 *   <li><b>层数跟着格挡走</b>：受击变薄、回合清零只剩薄壳</li>
 * </ul>
 * <h2>为什么读 {@code currentBlock} 而不是累计触发次数</h2>
 * 涅槃产出的是<b>格挡值</b>，而格挡有三个特性决定了视觉必须跟着它走：
 * <ol>
 *   <li><b>会被打掉</b>：受击时护壁要变薄 —— 这才是「护壁」该有的反应</li>
 *   <li><b>回合开始清零</b>：护壁也要跟着退，而不是留在那里</li>
 *   <li><b>会被其它来源叠加</b>：不区分来源，读总值最诚实，
 *       玩家不会看到「护壁 3 层但格挡 40」这种矛盾</li>
 * </ol>
 * 所以层数 = {@code clamp(1 + currentBlock / 每层面值, 1, MAX_LAYERS)}。
 * 一层的"面值"取 {@code NirvanaPower.amount}（原版 3 / 升级 4），
 * 因为那正好是「一次占卜能加多少」，读起来是「现在相当于叠了几次占卜」。
 * <h2>空间签名（规范 §2.2）</h2>
 * <pre>
 *   如水      水平铺展 + 下坠      流动的水（横的面，脚下绕身）
 *   战歌      头顶斜面            平面符号
 *   心之堡垒  绕身环绕的立方体    立体包壳，快速反转
 *   猛虎下山  落在敌人身上        斜向裂痕
 *   确立基础  身侧竖立的石碑      竖向的体，单侧、贴地
 *   斋戒      能量槽 → 胸口的球   飞行轨迹 + 原版双圈
 *   虔信      胸口的慢速刻度光轮  极慢自转、可数刻度、向心收束
 *   先见之明  身前上方的光框斜列  空心矩形 ×N、悬空缓浮、扫描显影
 *   研习      左下角的待归档页堆  扇形摊开的实心纸页、轻翻、落进抽牌堆
 *   涅槃      贴身的层叠六边护壁  多层棱壳、跟着格挡增减、受击变薄
 * </pre>
 * 与心之堡垒（同样「绕身包壳」）的区分 —— 这是最需要说清的一对：
 * <table border="1">
 *   <caption>两者都是「包住身体」，必须靠四件事分开</caption>
 *   <tr><td></td><td>心之堡垒</td><td>涅槃</td></tr>
 *   <tr><td>几何</td><td>立方体（8 顶点 12 棱，有透视深度）</td>
 *       <td><b>六边形（平面棱壳，无深度排序）</b></td></tr>
 *   <tr><td>层数</td><td>单层，转速随姿态变</td><td><b>多层同心，层数 = 格挡量</b></td></tr>
 *   <tr><td>尺寸</td><td>比身体大（{@code 0.42 × 身高} 半边长）</td>
 *       <td><b>贴身体轮廓（{@code 0.62~1.05 × 半宽}）</b></td></tr>
 *   <tr><td>变化驱动</td><td>姿态切换</td><td><b>格挡增减（受击时变薄）</b></td></tr>
 * </table>
 * 四条里「层数 = 格挡」「受击变薄」是决定性的：
 * 立方体永远是单层、永远不变形；护壁会随战斗实时增减。
 * <h2>颜色</h2>
 * 取<b>青铜</b>（偏绿的金属色）：与石（灰）、刻痕金（暖黄）、
 * 紫（虔信 / 先见之明）都不同，且金属质感天然读作「护甲」。
 * <h2>频次分级（规范 §2.11）</h2>
 * <ul>
 *   <li>占卜得格挡：一次占卜一次，可能一回合好几次 —— 完全静音、不震屏</li>
 *   <li>施放：一局几次 —— 原版无 VFX，所以补一段成形 + 轻震屏 + 音效</li>
 *   <li>受击变薄：<b>不额外演</b>，护壁自己变薄就够（受击本身已有大量反馈）</li>
 * </ul>
 */
final class NirvanaFx {
    /** 护壁主色：青铜。与石（灰）、刻痕金（暖黄）、紫（冷紫）都区分开。 */
    static final Color BRONZE = new Color(0.78f, 0.66f, 0.38f, 1.0f);
    /** 棱的亮部：接近白热，读作「被磨亮的棱」。 */
    static final Color EDGE_LIGHT = new Color(0.98f, 0.95f, 0.86f, 1.0f);
    /** 内衬的青玉：让护壁内部有「材质」而不只是棱。 */
    static final Color JADE = new Color(0.42f, 0.62f, 0.52f, 1.0f);

    /**
     * 最多画几层。
     * <p>
     * 层数 = 1 + 当前格挡 ÷ 每层面值。格挡 30 / 面值 3 = 11 层，但同心六边形
     * 叠到 4 层以上就糊成一团。3 层是视觉上限：再多就看不出「几层」了。
     */
    static final int MAX_LAYERS = 3;

    /** 第一层（最内）的半径（× 身体半宽）。贴身，正好贴住立绘轮廓。 */
    private static final float RADIUS_BASE = 0.62f;
    /** 每多一层向外让出多少（× 身体半宽）。 */
    private static final float RADIUS_STEP = 0.22f;
    /** 六边形的顶角数。 */
    static final int SIDES = 6;

    private NirvanaFx() {
    }

    // ---------------- 状态读取 ----------------

    /** 本补丁的卡牌 ID：涅槃（原版 {@code Nirvana.ID} = Nirvana）。 */
    private static final String CARD = PurpleEffectRegistry.CARD_NIRVANA;

    /** 能力 ID（原版 {@code NirvanaPower.POWER_ID} = Nirvana，与卡牌 ID 相同）。 */
    private static final String POWER = PurpleEffectRegistry.POWER_NIRVANA;

    /**
     * 一层的"面值"：一次占卜能加多少格挡。
     * <p>
     * 取 {@code NirvanaPower.amount}（原版 3 / 升级 4）。
     * 用它做分母，层数就正好读作「现在相当于叠了几次占卜」。
     */
    static int perLayer(AbstractCreature owner) {
        int n = 3;
        if (owner != null && owner.powers != null) {
            AbstractPower p = owner.getPower(POWER);
            if (p != null) {
                n = Math.max(1, Math.abs(p.amount));
            }
        }
        return n;
    }

    /**
     * 当前应该画几层护壁。
     * <p>
     * 来源是 {@code owner.currentBlock}（当前格挡值），理由见类注释：
     * 格挡会被打掉、会回合清零、会被其它来源叠加，
     * 直接读总值最诚实，不会出现「护壁还在但格挡没了」的矛盾。
     *
     * @return 1~{@link #MAX_LAYERS}；至少 1 层，因为「能力还在装着」
     */
    static int layers(AbstractCreature owner) {
        if (owner == null) {
            return 1;
        }
        int block = Math.max(0, owner.currentBlock);
        int per = perLayer(owner);
        int n = block <= 0 ? 1 : 1 + block / per;
        return MathUtils.clamp(n, 1, MAX_LAYERS);
    }

    /**
     * 施放时的每层面值。
     * <p>
     * 施放发生在 {@code useCard} 的 <b>Prefix</b>，此刻能力没挂上、
     * {@link #perLayer} 只能拿默认值 3，而升级版是 4。
     * 所以改读卡面数字（与先见之明、研习同一处理，规范 §六）。
     */
    static int perLayerForCast(AbstractCard card) {
        int n = 3;
        if (card != null) {
            int mn = Math.max(card.magicNumber, card.baseMagicNumber);
            if (mn > 0) {
                n = mn;
            }
        }
        return n;
    }

    /** 涅槃这张牌的特效是否开启。 */
    static boolean isOn() {
        return PurpleEffectRegistry.isOn(CARD);
    }

    // ---------------- 几何（常驻与一次性演出共用） ----------------

    /** 护壁中心 X：身体中轴。 */
    static float centerX(AbstractCreature owner) {
        return BodyFx.centerX(owner);
    }

    /** 护壁中心 Y：胸口偏下，让六边形能同时兜住肩与腹。 */
    static float centerY(AbstractCreature owner) {
        return BodyFx.groundY(owner) + BodyFx.height(owner) * 0.50f;
    }

    /**
     * 第 {@code i} 层的半径（像素，已缩放）。
     * <p>
     * <b>只由 {@code i} 决定，不随总层数变化</b>，所以从 2 层变 3 层时
     * 原有的两层完全不动，只是外面多出一层 —— 玩家参照点不跳（规范 §三.7）。
     */
    static float radius(AbstractCreature owner, int i) {
        float hw = BodyFx.halfWidth(owner);
        float base = Math.max(34.0f * Settings.scale, hw * RADIUS_BASE);
        float step = Math.max(14.0f * Settings.scale, hw * RADIUS_STEP);
        return base + step * i;
    }

    /** 第 {@code i} 层的椭圆压扁比例：越外层越接近正六边形。 */
    static float flatten(int i) {
        return MathUtils.lerp(0.88f, 1.0f, i / (float) Math.max(1, MAX_LAYERS - 1));
    }

    /** 六边形第 {@code k} 个顶点的角度（度）。尖端朝上。 */
    static float vertexDeg(int k) {
        return 90.0f + 360.0f * k / SIDES;
    }

    /** 当前是否有格挡。没有时只能画一圈薄壳，不能什么都不画。 */
    static boolean hasBlock(AbstractCreature owner) {
        return owner != null && owner.currentBlock > 0;
    }

    // ---------------- 事件入口 ----------------

    /**
     * 打出这张牌（{@code Nirvana.use()}）。
     * <p>
     * 原版<b>完全没有 VFX</b>，所以必须补：护壁一圈圈<b>从体内向外撑开</b>。
     * 这一段也要说清「这套护壁是什么形状」，否则常驻出现时会显得没来由。
     * <p>
     * 一局只几次，允许轻震屏 + 音效。
     * 走 {@code queueTop}：护壁会漫到身体上下，普通层可能被手牌压住下缘。
     *
     * @param owner 能力持有者（玩家）
     * @param card  刚被打出的这张牌，用来读它真实的每层面值
     */
    static void onCast(AbstractCreature owner, AbstractCard card) {
        if (owner == null || !isOn()) {
            return;
        }
        PurpleEffectRegistry.queueTop(new NirvanaShellEffect(owner,
                perLayerForCast(card), 1.0f));
    }

    /**
     * 占卜成功、获得格挡（{@code NirvanaPower.onScry()}）。
     * <p>
     * 每占卜一次触发一次，可能一回合好几次 —— <b>完全静音、不震屏</b>。
     * <p>
     * 表现是「<b>新的一层从内侧顶出、棱上扫过一圈亮环</b>」，
     * 同时把常驻的兴奋度顶起（护壁整体亮一档）。
     * <b>不画任何「占卜/预视」的语义</b> —— 那是先见之明的工作（见类注释）。
     * <p>
     * 走 {@code queueTop} 与常驻保持一致，避免两者错位。
     */
    static void onScry(AbstractCreature owner) {
        if (owner == null || !isOn()) {
            return;
        }
        // 强度由「这一层加了多少」决定：升级版 4 点比 3 点更实
        float power = MathUtils.clamp(perLayer(owner) / 4.0f, 0.75f, 1.15f);
        PurpleEffectRegistry.queueTop(new NirvanaShellEffect(owner,
                perLayer(owner), power));
    }
}
