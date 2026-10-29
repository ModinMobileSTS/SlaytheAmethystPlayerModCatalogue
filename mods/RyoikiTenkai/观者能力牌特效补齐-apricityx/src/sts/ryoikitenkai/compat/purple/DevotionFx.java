package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.AbstractPower;

/**
 * 「虔信」（Devotion）的规格、几何、配色与事件入口。
 * <p>
 * <h2>机制（规范 §2.3：视觉来自机制）</h2>
 * 已对照字节码核实。这张牌的核心是<b>一个累积过程</b>，不是一次性效果：
 * <pre>
 * // Devotion.use()  —— 一次性
 * ApplyPowerAction(DevotionPower(p, 2))       // 每回合 +2 真言（升级 +3）
 *
 * // DevotionPower.atStartOfTurnPostDraw()  —— 每回合
 * this.flash();
 * if (!player.hasPower("Mantra") &amp;&amp; this.amount &gt;= 10) {
 *     addToBot(new ChangeStanceAction("Divinity"));   // 满 10 直接进神性
 * } else {
 *     addToBot(new ApplyPowerAction(self, self,
 *             new MantraPower(owner, amount), amount));  // 否则累加真言
 * }
 *
 * // MantraPower.stackPower()
 * if (this.amount &gt;= 10) {
 *     addToTop(new ChangeStanceAction("Divinity"));      // 真言满 10 → 神性
 *     this.amount -= 10;
 *     ...
 * }
 * </pre>
 * 所以这张牌的视觉必须回答<b>四个</b>问题，对应三个时机：
 * <ol>
 *   <li><b>施放的这一刻，牌生效了吗？</b> → 十根刻度被刻下来（{@link #onCast}）</li>
 *   <li><b>现在积了多少真言？还剩几格？</b> → 十根刻度 + 进度弧（{@link #onTurnStart}）</li>
 *   <li><b>到点了没有？</b> → 满 10 时的向心收束（{@link #onAscend}）</li>
 * </ol>
 * <h2>本轮返工：整体加大加亮（用户反馈「特效不够明显」）</h2>
 * 上一版有<b>两个</b>独立的可见性缺陷，只修一个不够：
 * <h3>缺陷一：施放瞬间什么都没有（最严重）</h3>
 * {@code cast(CARD_DEVOTION)} 原来是直接 {@code return}，理由是
 * 「真言要等回合开始才到账，所以施放时不演」。那个理由只对了一半 ——
 * 不该演「加真言」，但<b>必须演「这张牌被挂上了」</b>。
 * 结果打出虔信时画面上<b>什么都没发生</b>，要等下回合才第一次看到环。
 * 现在补了 {@link DevotionSealEffect}：十根刻度逐根刻出 → 整圈锁死。
 * <h3>缺陷二：常驻环太小太淡</h3>
 * 三条尺寸问题：
 * <ol>
 *   <li><b>半径太小</b>：{@code halfWidth × 1.55 ≈ 93px}，只到肩膀宽度。
 *       现在同时受<b>身高</b>约束（{@code × 0.46}），并抬到 1.95 倍</li>
 *   <li><b>椭圆压得太扁</b>：{@code FLATTEN = 0.42} 让纵向只剩 39px，
 *       十根刻度在上下两端挤成一团。现在抬到 0.52</li>
 *   <li><b>刻度本身是 12×3 像素的扁点</b>：横放的小点在椭圆轨道上还是<b>错的方向</b>。
 *       现在改成<b>径向粗棒</b>（长度 ≥13px、厚度 ≥6px，与半径解耦）</li>
 * </ol>
 * 另外 {@link #DIM} 从 {@code (0.28,0.22,0.38)} 提亮到能看出轮廓 ——
 * 原来它比战斗背景还暗，加色混合下等于没画，「还剩几格」这条信息完全丢失。
 * <h2>设计：胸口刻度光轮 + 进度弧</h2>
 * <ul>
 *   <li><b>十根径向刻度</b>：已积的又亮又长，未积的暗而短 —— 一眼数得出还剩几格</li>
 *   <li><b>进度弧</b>：沿轨道铺一条最粗最亮的弧，弧长 = 已积真言所占角度</li>
 *   <li><b>内圈六段反向弧</b>：反向慢转，与外侧同向旋转叠出体积感</li>
 *   <li><b>绕行的诵经光</b>：沿轨道滑行、带拖尾，读作「持续诵经」</li>
 *   <li><b>随进度加速</b>：7°/s → 30°/s，颜色紫白 → 神性粉</li>
 *   <li><b>9 格预警</b>：整圈开始过曝呼吸，读作「下一句就要满了」</li>
 * </ul>
 * <h2>空间签名（规范 §2.2）</h2>
 * <pre>
 *   如水      水平铺展 + 下坠      流动的水（横的面，脚下绕身）
 *   战歌      头顶斜面            平面符号
 *   心之堡垒  绕身环绕的立方体    立体包壳，快速反转
 *   猛虎下山  落在敌人身上        斜向裂痕
 *   确立基础  身侧竖立的石碑      竖向的体，单侧、静止
 *   斋戒      能量槽 → 胸口的球   飞行轨迹 + 原版双圈
 *   虔信      胸口的慢速刻度光轮  极慢自转、可数刻度、向心收束
 * </pre>
 * 与心之堡垒的区分（两者都是「绕身」）：立方体是<b>快速反转的立体棱</b>、
 * 用姿态色、有深度排序；虔信是<b>极慢自转的平面刻度环</b>、
 * 用粉白色、有 10 个可数节点、且会随进度加速。一个是「壳」，一个是「环」。
 * <h2>颜色</h2>
 * 原版神性姿态用的是 {@code Color.PINK}（见 {@code DivinityStance.onEnterStance}），
 * 而真言（Mantra）本身是紫。所以：
 * <ul>
 *   <li>未满时 → <b>紫白</b>（真言的本色）</li>
 *   <li>接近满 / 满 10 → 逐渐转向 <b>神性粉</b></li>
 * </ul>
 */
final class DevotionFx {
    /** 真言未满时的主色：紫白（真言本色）。已提亮一档。 */
    static final Color MANTRA = new Color(0.86f, 0.72f, 1.00f, 1.0f);
    /** 真言的亮部。 */
    static final Color MANTRA_CORE = new Color(1.00f, 0.97f, 1.00f, 1.0f);
    /** 神性姿态色：原版 {@code DivinityStance} 用 Color.PINK。 */
    static final Color DIVINITY = new Color(1.00f, 0.58f, 0.90f, 1.0f);
    /** 神性的白热核心。 */
    static final Color DIVINITY_CORE = new Color(1.00f, 0.96f, 1.00f, 1.0f);
    /**
     * 未点亮的刻度。
     * <p>
     * 上一版是 {@code (0.28,0.22,0.38)} —— 比战斗背景还暗，加色混合下等于没画，
     * 于是「还剩几格」这个最重要的信息完全丢失。现在提亮到能在暗背景上看出轮廓。
     */
    static final Color DIM = new Color(0.52f, 0.44f, 0.72f, 1.0f);

    /** 真言满值（原版 {@code MantraPower.PRAYER_REQUIRED}）。 */
    static final int FULL = 10;

    /** 刻度数量 = 10，与真言需求一一对应。 */
    static final int TICKS = FULL;

    /** 光轮中心相对身高的比例：胸口偏上，不挡脸。 */
    static final float RING_HEIGHT = 0.62f;
    /** 光轮半径相对身体半宽的倍数。 */
    static final float RING_RX_MUL = 1.95f;
    /** 光轮半径相对身高的倍数（与半宽取大者，防止窄体型角色光圈过小）。 */
    static final float RING_RX_BY_HEIGHT = 0.46f;
    /**
     * 光轮的俯视压扁比例。
     * <p>
     * 0.42 太扁：纵向半径只剩 39px，十根刻度在椭圆上下两端会挤成一片。
     * 0.52 既保住「俯视椭圆」的读法，又给刻度留出可分辨的纵向间距。
     */
    static final float FLATTEN = 0.52f;

    /** 最慢自转（度/秒）：进度 0 时。极慢，读作「诵经」。 */
    static final float SPIN_MIN = 7.0f;
    /** 最快自转：进度满时。 */
    static final float SPIN_MAX = 30.0f;
    /** 内圈的反向自转（度/秒），比外圈慢一点。 */
    static final float INNER_SPIN = -14.0f;

    private DevotionFx() {
    }

    // ---------------- 状态读取 ----------------

    /** 本补丁的卡牌 ID：虔信。 */
    private static final String CARD = PurpleEffectRegistry.CARD_DEVOTION;

    /** 虔信能力的 ID。 */
    private static final String POWER = PurpleEffectRegistry.POWER_DEVOTION;

    /** 真言能力的 ID。 */
    private static final String MANTRA_POWER = "Mantra";

    /**
     * 当前真言点数（0~10）。
     * <p>
     * 读的是 {@code MantraPower.amount}，也就是界面上那个真言数字。
     * 注意原版在满 10 时会立刻消耗掉并进神性，所以这里的值天然不会超过 10；
     * 但仍做钳制以防被其它 mod 改写。
     */
    static int mantra(AbstractCreature owner) {
        AbstractPower p = mantraPower();
        if (p == null) {
            return 0;
        }
        return MathUtils.clamp(Math.abs(p.amount), 0, FULL);
    }

    private static AbstractPower mantraPower() {
        if (AbstractDungeon.player == null || AbstractDungeon.player.powers == null) {
            return null;
        }
        return AbstractDungeon.player.getPower(MANTRA_POWER);
    }

    /** 虔信这张能力本身的层数（= 每回合加多少真言，原版 2 / 升级 3）。 */
    static int devotionAmount(AbstractCreature owner) {
        if (owner == null || owner.powers == null) {
            return 2;
        }
        AbstractPower p = owner.getPower(POWER);
        return p == null ? 2 : Math.max(1, Math.abs(p.amount));
    }

    /** 进度 0~1：真言 / 10。所有亮度、转速、颜色都由它驱动。 */
    static float progress(AbstractCreature owner) {
        return MathUtils.clamp(mantra(owner) / (float) FULL, 0.0f, 1.0f);
    }

    /** 虔信这张牌的特效是否开启。 */
    static boolean isOn() {
        return PurpleEffectRegistry.isOn(CARD);
    }

    // ---------------- 几何 ----------------

    /** 光轮中心 X。 */
    static float centerX(AbstractCreature owner) {
        return BodyFx.centerX(owner);
    }

    /** 光轮中心 Y：胸口偏上，落在胸前而不是脸上。 */
    static float centerY(AbstractCreature owner) {
        return BodyFx.groundY(owner) + BodyFx.height(owner) * RING_HEIGHT;
    }

    /**
     * 光轮横半径。
     * <p>
     * 同时受<b>身体半宽</b>与<b>身高</b>约束：只按半宽算的话，窄体型角色
     * （例如观者本体的判定框）光圈会小到看不出是一圈。
     */
    static float radiusX(AbstractCreature owner) {
        float byWidth = BodyFx.halfWidth(owner) * RING_RX_MUL;
        float byHeight = BodyFx.height(owner) * RING_RX_BY_HEIGHT;
        return Math.max(52.0f * Settings.scale, Math.max(byWidth, byHeight));
    }

    /** 光轮纵半径（俯视压扁）。 */
    static float radiusY(AbstractCreature owner) {
        return radiusX(owner) * FLATTEN;
    }

    /** 当前自转速度（度/秒）：进度越高转得越快。 */
    static float spinSpeed(float progress) {
        return MathUtils.lerp(SPIN_MIN, SPIN_MAX,
                MathUtils.clamp(progress, 0.0f, 1.0f));
    }

    /**
     * 主色：进度越高越从「紫白」转向「神性粉」。
     * <p>
     * 这样「快要升入神性」在颜色上有预兆，与神性姿态的粉光呼应。
     */
    static Color tint(float progress) {
        float t = MathUtils.clamp(progress, 0.0f, 1.0f);
        // 前 60% 保持紫，最后 40% 转粉 —— 读作「临界了」
        float pink = MathUtils.clamp((t - 0.60f) / 0.40f, 0.0f, 1.0f);
        CACHE.set(MANTRA).lerp(DIVINITY, pink);
        CACHE.a = 1.0f;
        return CACHE;
    }

    /** 亮部颜色，同上逻辑。 */
    static Color coreTint(float progress) {
        float t = MathUtils.clamp(progress, 0.0f, 1.0f);
        float pink = MathUtils.clamp((t - 0.60f) / 0.40f, 0.0f, 1.0f);
        CACHE_CORE.set(MANTRA_CORE).lerp(DIVINITY_CORE, pink);
        CACHE_CORE.a = 1.0f;
        return CACHE_CORE;
    }

    /** 颜色缓存的临时对象：不污染 Draw 的共享色。 */
    private static final Color CACHE = new Color();
    private static final Color CACHE_CORE = new Color();

    // ---------------- 事件入口 ----------------

    /**
     * 打出这张牌（{@code Devotion.use()} → {@code ApplyPowerAction(DevotionPower)}）。
     * <p>
     * <b>真言还没有到账</b>（要到回合开始的 {@code atStartOfTurnPostDraw}），
     * 所以这里不演「加真言」，只演「<b>刻度被刻下来、整圈锁死</b>」——
     * 这是玩家能确认「牌生效了」的唯一时机。
     * <p>
     * 走<b>最顶层队列</b>：刻画会画到胸口上方，普通层会被手牌压住。
     *
     * @param owner 能力持有者（玩家）
     */
    static void onCast(AbstractCreature owner) {
        if (owner == null || !isOn()) {
            return;
        }
        PurpleEffectRegistry.queueTop(new DevotionSealEffect(owner));
    }

    /**
     * 回合开始（{@code DevotionPower.atStartOfTurnPostDraw}）：真言累加了一次。
     * <p>
     * 这是这张牌<b>持续起作用</b>的地方 —— 每回合都会发生。
     * 演出强度由「加了多少真言」和「加完之后离满值多近」共同决定。
     * <p>
     * 走<b>最顶层队列</b>：光点会一路飞到胸口高度，而胸口上半截正好在手牌区之上，
     * 普通层会被手牌压掉一半（战歌曾经踩过同一个坑，见规范 §1.3）。
     *
     * @param owner 能力持有者（玩家）
     */
    static void onTurnStart(AbstractCreature owner) {
        if (owner == null || !isOn()) {
            return;
        }
        PurpleEffectRegistry.queueTop(new DevotionChantEffect(owner,
                devotionAmount(owner), mantra(owner)));
    }

    /**
     * 真言满 10、即将升入神性：十根刻度同时炸亮，整圈向心收束。
     * <p>
     * 挂在 {@code MantraPower.stackPower} 的满值分支上，
     * 也就是原版真正执行 {@code ChangeStanceAction("Divinity")} 的那一刻。
     * <b>走最顶层队列</b>：爆发段会漫到角色上方，普通层会被手牌压住。
     *
     * @param owner 能力持有者（玩家）
     */
    static void onAscend(AbstractCreature owner) {
        if (owner == null || !isOn()) {
            return;
        }
        PurpleEffectRegistry.queueTop(new DevotionAscendEffect(owner));
    }
}
