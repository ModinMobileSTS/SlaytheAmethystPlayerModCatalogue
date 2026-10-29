package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.OverlayMenu;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.ui.panels.DrawPilePanel;

/**
 * 「研习」（Study）的规格、几何、配色与事件入口。
 * <p>
 * <h2>机制（规范 §2.3：视觉来自机制）</h2>
 * 已对照字节码核实。这张牌<b>卡与力量两边都读了</b>：
 * <pre>
 * // Study.use()  —— 一次性，而且<b>完全没有 VFX</b>
 * this.addToBot(new ApplyPowerAction(p, p, new StudyPower(p, this.magicNumber), ...));
 *
 * // StudyPower.atEndOfTurn(boolean)  —— 每回合末
 * this.addToBot(new MakeTempCardInDrawPileAction(new Insight(), this.amount, true, true));
 * </pre>
 * 三个事实决定了整个画面：
 * <ol>
 *   <li><b>这张牌干什么</b>：每回合末把 <b>N 张「洞见」塞进抽牌堆</b>
 *       （N = 能力层数，原版 1）。洞见是 0 费、消耗、抽 2 张的临时牌</li>
 *   <li><b>生效时机是回合末</b>，不是施放时</li>
 *   <li><b>施放时原版一个特效都没有</b> —— 和先见之明相反，
 *       这里<b>必须自己补上施放那一下</b>，否则打出这张牌毫无反馈</li>
 * </ol>
 * <h2>旧实现为什么必须推翻</h2>
 * 原来走通用骨架的 {@link Motif#RUNE}：<b>身周浮起的灰白铭文 + 头顶三根橙刺</b>。
 * 三条问题：
 * <ol>
 *   <li><b>用了星芒</b>：{@code renderRune()} 里对每根橙刺的尖端调了 {@code Draw.star}。
 *       这是硬性禁止的元素（星芒是原版卡面风格，混进战斗特效里会读成「装饰」而不是「效果」）</li>
 *   <li><b>材质借错语汇</b>：橙刺借的是铁甲战士的火/赤焰语汇；这张牌讲的是<b>把知识写进牌堆</b>，
 *       与火无关。这是「按卡图猜」而不是「按机制」</li>
 *   <li><b>几何仍在「环绕 + 头顶」</b>：与心之堡垒（环绕）、先见之明（头顶）同构。
 *       规范 §2.2 的教训是「几何不换，颜色换多少次都没用」</li>
 * </ol>
 * <h2>设计：左下角的「待归档页堆」</h2>
 * 这张牌唯一的产物是<b>塞进抽牌堆的牌</b>，而抽牌堆在<b>屏幕左下角</b>。
 * 所以视觉不挂在身体上，而是挂在<b>它真正要去的地方</b>：
 * <ul>
 *   <li><b>一页纸 = 一张洞见</b>。页堆就停在抽牌堆图标的<b>正上方</b>，
 *       数量 = 每回合会塞进去的张数（规范 §2.7 一一对应）</li>
 *   <li><b>扇形摊开</b>：N 页从一个支点向上呈扇状展开。
 *       既不是闭合的环，也不是等距平行的方阵 —— 这是这套里唯一一个<b>扇面</b>几何</li>
 *   <li><b>纸在轻翻</b>：每页按各自相位绕竖轴来回翻（宽度在 1.0 ↔ 0.18 之间压缩），
 *       读作「纸页被风掀起」</li>
 *   <li><b>回合末：页一张张落进抽牌堆</b>。先微微抬起蓄势，再加速下坠、边转边缩小、
 *       没入图标时淡出，图标上同时扩一圈粗弧。
 *       这就是 {@code MakeTempCardInDrawPileAction} 的字面动作</li>
 *   <li><b>施放：页从胸口飞到左下角落位</b>。原版无 VFX，所以这一段要明确：
 *       让玩家看见「这套装置装在了哪里」，并且把「这些页属于我」这件事说清楚</li>
 * </ul>
 * <h2>空间签名（规范 §2.2）</h2>
 * <pre>
 *   如水      水平铺展 + 下坠      流动的水（横的面，脚下绕身）
 *   战歌      头顶斜面            平面符号
 *   心之堡垒  绕身环绕的立方体    立体包壳，快速反转
 *   猛虎下山  落在敌人身上        斜向裂痕
 *   确立基础  身侧竖立的石碑      竖向的体，单侧、贴地
 *   斋戒      能量槽 → 胸口的球   飞行轨迹 + 原版双圈
 *   虔信      胸口的慢速刻度光轮  极慢自转、可数刻度、向心收束
 *   先见之明  头顶右侧的光框斜列  空心矩形 ×N、悬空缓浮、扫描显影
 *   研习      左下角的待归档页堆  扇形摊开的实心纸页、轻翻、落进抽牌堆
 * </pre>
 * 四个维度逐项比：
 * <table border="1">
 *   <caption>与最接近的三张牌对比</caption>
 *   <tr><td></td><td>确立基础</td><td>先见之明</td><td>研习</td></tr>
 *   <tr><td>几何</td><td>尖顶方碑（实心竖块）</td><td>空心矩形框，等距斜列</td><td><b>扇形摊开的实心纸页</b></td></tr>
 *   <tr><td>位置</td><td>角色左侧、贴地</td><td>头顶右侧、悬空</td><td><b>屏幕左下角、抽牌堆正上方</b></td></tr>
 *   <tr><td>动静</td><td>静止</td><td>缓浮 + 扫描显影</td><td><b>纸页轻翻 + 落进抽牌堆</b></td></tr>
 *   <tr><td>材质</td><td>石（不透明，有暗侧面）</td><td>半透明光框</td><td><b>纸（不透明薄片，有翻面）</b></td></tr>
 * </table>
 * 关键在于<b>位置</b>：这是全套里唯一一个<b>不挂在角色身上</b>、而挂在
 * <b>屏幕 UI 区域</b>的特效。因为它的产物本来就不落在身上，而是落进牌堆。
 * <h2>为什么页堆必须走顶层队列</h2>
 * 它停在左下角，而底部 UI 面板（{@code bottomBgPanel}、抽牌堆图标）画在
 * {@code effectsQueue} <b>之上</b>。走普通层的话整堆页会被底部条完全盖住 ——
 * 这正是规范 §1.3 那条「会被遮住就走 {@code queueTop}」。
 * <h2>颜色</h2>
 * 纸是本补丁里<b>全新的材质</b>，所以身份主要靠「暖白纸面」承载；
 * 墨光取<b>淡青瓷</b>（{@code 0.60, 0.93, 0.90}），刻意落在
 * 如水的天蓝（{@code 0.44,0.84,1.00}）与斋戒的绿（{@code 0.46,0.96,0.48}）之间，
 * 两边都不重合。
 * <h2>频次分级（规范 §2.11）</h2>
 * <ul>
 *   <li>回合末归档：<b>每回合</b> —— 无音效、无震屏</li>
 *   <li>施放：一局几次 —— 轻震屏 + 音效（原版一点反馈都没有，这里补上）</li>
 * </ul>
 */
final class StudyFx {
    /** 纸面：暖白。这是这张牌的身份色（本补丁第一个「纸」材质）。 */
    static final Color PAPER = new Color(0.94f, 0.92f, 0.86f, 1.0f);
    /** 墨光：淡青瓷。落在如水的天蓝与斋戒的绿之间，两边都不撞。 */
    static final Color INK = new Color(0.60f, 0.93f, 0.90f, 1.0f);
    /** 白热：落进抽牌堆那一刻的最亮处。 */
    static final Color CORE = new Color(0.98f, 1.00f, 0.99f, 1.0f);

    /**
     * 最多画几页。
     * <p>
     * 原版每回合只塞 1 张，叠加到 3 张研习才会出现 3 页。
     * 3 页是视觉上限：再多扇面就会张到屏幕外。
     */
    static final int MAX_PAGES = 3;

    /** 页堆支点高出抽牌堆图标中心多少（基准像素）。 */
    private static final float DECK_LIFT = 118.0f;
    /** 扇面半径（基准像素）；对外暴露，供常驻与一次性演出共享。 */
    static final float FAN_RADIUS = 96.0f;
    /** 扇面张角范围（度，0 = 右，90 = 上）。从偏左上到偏右上。 */
    private static final float FAN_START = 108.0f;
    private static final float FAN_END = 58.0f;
    /** 单页尺寸（基准像素）。接近一张牌的宽高比。 */
    private static final float PAGE_W = 62.0f;
    private static final float PAGE_H = 82.0f;
    /** 页自身的倾角偏移（度），让它们看起来是「被摊开」而不是「插在扇柄上」。 */
    private static final float PAGE_TILT = -14.0f;

    private StudyFx() {
    }

    // ---------------- 状态读取 ----------------

    /** 本补丁的卡牌 ID：研习（原版 {@code Study.ID} = Study）。 */
    private static final String CARD = PurpleEffectRegistry.CARD_STUDY;

    /** 能力 ID（原版 {@code StudyPower.POWER_ID} = Study，与卡牌 ID 相同）。 */
    private static final String POWER = PurpleEffectRegistry.POWER_STUDY;

    /**
     * 每回合会塞进抽牌堆几页 = 要画几页。
     * <p>
     * 直接读 {@code StudyPower.amount} —— 原版
     * {@code MakeTempCardInDrawPileAction(new Insight(), this.amount, ...)}
     * 的第一个数量参数就是它，所以页数与实际归档的牌数<b>严格相等</b>。
     */
    static int pages(AbstractCreature owner) {
        int n = 1;
        if (owner != null && owner.powers != null) {
            AbstractPower p = owner.getPower(POWER);
            if (p != null) {
                n = Math.max(1, Math.abs(p.amount));
            }
        }
        return MathUtils.clamp(n, 1, MAX_PAGES);
    }

    /**
     * 施放时的页数。
     * <p>
     * 施放发生在 {@code useCard} 的 <b>Prefix</b>，此刻 {@code ApplyPowerAction}
     * 还没执行、能力还没挂上，{@link #pages} 只能拿到默认的 1。
     * 所以这里改读卡面数字。研习的升级只降费（{@code upgradeBaseCost(1)}），
     * {@code magicNumber} 始终是 1，所以正常施放恒为 1 页。
     */
    static int pagesForCast(AbstractCard card) {
        int n = 1;
        if (card != null) {
            int mn = Math.max(card.magicNumber, card.baseMagicNumber);
            if (mn > 0) {
                n = mn;
            }
        }
        return MathUtils.clamp(n, 1, MAX_PAGES);
    }

    /** 研习这张牌的特效是否开启。 */
    static boolean isOn() {
        return PurpleEffectRegistry.isOn(CARD);
    }

    // ---------------- 几何（常驻与一次性演出共用） ----------------

    /** 扇面半径（像素，已缩放）。常驻与两个一次性演出都走它，不各自写一遍。 */
    static float fanRadius() {
        return FAN_RADIUS * Settings.scale;
    }

    /**
     * 抽牌堆图标中心的屏幕 X。
     * <p>
     * 镜像 {@code DrawPilePanel.render()} 的私有偏移：
     * <pre>
     * DECK_X = 76f * scale - 64f;
     * sb.draw(DECK_BTN_BASE, current_x + DECK_X, current_y + DECK_Y,
     *         64f, 64f, 128f, 128f, scale, scale, ...);
     * // 该绘制调用的视觉中心 = current_x + DECK_X + 64 * scale
     * //                        = current_x + 140 * scale - 64
     * </pre>
     * 那三个常量是 {@code private static final}，取不到，所以在这里按同一公式复算。
     * 取不到面板时回退到屏幕左下角的固定比例位置。
     */
    static float deckX() {
        DrawPilePanel panel = deckPanel();
        if (panel == null) {
            return Settings.WIDTH * 0.08f;
        }
        return panel.current_x + 140.0f * Settings.scale - 64.0f;
    }

    /** 抽牌堆图标中心的屏幕 Y。公式来源同 {@link #deckX()}。 */
    static float deckY() {
        DrawPilePanel panel = deckPanel();
        if (panel == null) {
            return Settings.HEIGHT * 0.10f;
        }
        return panel.current_y + 138.0f * Settings.scale - 64.0f;
    }

    private static DrawPilePanel deckPanel() {
        OverlayMenu menu = AbstractDungeon.overlayMenu;
        if (menu == null) {
            return null;
        }
        return menu.combatDeckPanel;
    }

    /** 页堆支点 X：正对抽牌堆图标。 */
    static float stackX() {
        return deckX();
    }

    /** 页堆支点 Y：抽牌堆图标正上方。下坠动画因此是一条短而明确的直线。 */
    static float stackY() {
        return deckY() + DECK_LIFT * Settings.scale;
    }

    /** 第 {@code i} 页在扇面上的方向角（度）。 */
    static float fanDeg(int i, int n) {
        if (n <= 1) {
            // 只有一页时朝正上方，不偏
            return 90.0f;
        }
        float t = i / (float) (n - 1);
        return MathUtils.lerp(FAN_START, FAN_END, t);
    }

    /** 第 {@code i} 页的页心 X。 */
    static float pageX(int i, int n) {
        float rad = fanDeg(i, n) * MathUtils.degreesToRadians;
        return stackX() + MathUtils.cos(rad) * fanRadius();
    }

    /** 第 {@code i} 页的页心 Y。 */
    static float pageY(int i, int n) {
        float rad = fanDeg(i, n) * MathUtils.degreesToRadians;
        return stackY() + MathUtils.sin(rad) * fanRadius();
    }

    /** 单页宽度（像素，已缩放）。 */
    static float pageW() {
        return PAGE_W * Settings.scale;
    }

    /** 单页高度（像素，已缩放）。 */
    static float pageH() {
        return PAGE_H * Settings.scale;
    }

    /** 页自身的倾角：让页看起来是摊开的，而不是插在扇柄上。 */
    static float pageTilt(int i, int n) {
        // 以扇面中轴为基准，左右各自外倾一点
        float mid = (n - 1) * 0.5f;
        return PAGE_TILT + (i - mid) * 9.0f;
    }

    // ---------------- 事件入口 ----------------

    /**
     * 打出这张牌（{@code Study.use()}）。
     * <p>
     * 原版这里<b>一个特效都没有</b>，所以必须补：
     * 页从角色胸口飞向左下角、落成扇面。这样玩家立刻知道两件事 ——
     * 「装上了」以及「这套东西在哪里」。
     * <p>
     * 这是整张牌唯一允许出声的时刻（一局只几次），给轻震屏 + 音效。
     * 走 {@code queueTop}：飞行横跨屏幕下半部分，普通层会被底部 UI 与手牌盖住。
     *
     * @param owner 能力持有者（玩家）
     * @param card  刚被打出的这张牌，用来读它真实的页数
     */
    static void onCast(AbstractCreature owner, AbstractCard card) {
        if (owner == null || !isOn()) {
            return;
        }
        PurpleEffectRegistry.queueTop(new StudyAssignEffect(owner, pagesForCast(card)));
    }

    /**
     * 回合末归档（{@code StudyPower.atEndOfTurn} → {@code MakeTempCardInDrawPileAction}）。
     * <p>
     * 这是这张牌<b>真正生效</b>的地方，每回合一次：
     * 页堆里的页一张张落进抽牌堆，落点时图标扩一圈粗弧。
     * <p>
     * 每回合都会发生 —— <b>不震屏、不音效</b>。
     * 走 {@code queueTop}：整段动画都在左下角 UI 区域，普通层会被底部面板盖住。
     */
    static void onTurnEnd(AbstractCreature owner) {
        if (owner == null || !isOn()) {
            return;
        }
        PurpleEffectRegistry.queueTop(new StudyDropEffect(pages(owner)));
    }
}
