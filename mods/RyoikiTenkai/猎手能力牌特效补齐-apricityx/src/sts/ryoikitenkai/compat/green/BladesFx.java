package sts.ryoikitenkai.compat.green;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.powers.AbstractPower;

/**
 * 「无限刀刃」（Infinite Blades）的规格、几何与配色。
 * <p>
 * <h2>机制（卡 / 能力两层都按字节码核实）</h2>
 * <pre>
 * // InfiniteBlades.use() —— 只有一个 ApplyPowerAction，原版一个 VFX 都没有
 * addToBot(new ApplyPowerAction(p, p, new InfiniteBladesPower(p, 1), 1));
 * // 注意：施加量恒为 1；升级只给 isInnate，不加 amount
 *
 * // InfiniteBladesPower —— POWER_ID = "Infinite Blades"（与卡牌 ID 相同）
 * public void atStartOfTurn() {
 *     if (!AbstractDungeon.getMonsters().areMonstersBasicallyDead()) {
 *         this.flash();
 *         this.addToBot(new MakeTempCardInHandAction(new Shiv(), this.amount, false));
 *     }
 * }
 * public void stackPower(int stackAmount) { this.amount += stackAmount; }   // 会叠加
 * </pre>
 * <h2>姿态：全部刃尖<b>朝右</b>，悬浮在人物<b>右侧</b>（按需求）</h2>
 * 上一版是「绕身一圈、刃尖朝外（法向）、缓慢自转」。
 * 现在改为：
 * <ul>
 *   <li><b>刃尖一律水平朝右</b>（{@value #DIR_DEG}°）—— 所有刀同向，读作「一排待发的刀」</li>
 *   <li>悬浮在人物<b>右侧身外</b>，横排成<b>上下错落的几列</b>，不绕身、不自转</li>
 *   <li>只有极轻的上下浮动（每把错开相位），读作「浮着」而不是「在转」</li>
 * </ul>
 * 这样与「精准」的静止标靶、卡影的浮动都不同：<b>它在人物右侧、一排同向的刀</b>。
 * <h2>尺寸：本轮放大</h2>
 * <pre>
 *   刀长  半宽 × 0.40（下限 13px）  →  半宽 × 0.62（下限 34px）
 *   刀宽  刀长 × 0.17              →  刀长 × 0.26
 * </pre>
 * 上一版的刀在手机上只有十几像素，读作「几根小刺」。
 * <h2>数量：{@code amount + 2}（3 ~ 6）</h2>
 * 严格 1:1 时单张只有 1 把，而这张牌叫 Infinite Blades、卡面是一整片匕首。
 * {@code +2} 后：<b>叠加仍然看得出来</b>（每叠一层多一把），单张也有「一排」的观感。
 * <h2>空间签名（规范 §2.2）</h2>
 * <table border="1">
 *   <tr><td></td><td>精准</td><td>工具贸易 / 蓄势待发</td><td>无限刀刃</td></tr>
 *   <tr><td>几何</td><td>同心光环 + 径向刻痕</td><td>浮着的卡影（矩形）</td>
 *       <td><b>一排同向的匕首，刃尖朝右</b></td></tr>
 *   <tr><td>位置</td><td>左侧身外</td><td>身体两侧</td><td><b>右侧身外、悬停</b></td></tr>
 *   <tr><td>动静</td><td>不转，呼吸 + 扫光</td><td>缓慢上下浮动</td>
 *       <td><b>不转不自转；每把错相轻浮</b></td></tr>
 *   <tr><td>语义</td><td>给所有小刀供能</td><td>过牌 / 留牌</td><td><b>每回合造刀</b></td></tr>
 * </table>
 * <h2>频次分级（规范 §2.11）</h2>
 * <ul>
 *   <li>回合开始获得小刀：<b>每回合一次</b> —— 静音、不震屏</li>
 *   <li>施放：一局几次 —— 轻震屏 LOW（音效由原版力量自己播 {@code BUFF_1~3}）</li>
 * </ul>
 */
final class BladesFx {
    /** 环绕匕首数上限：再多会挤成一片看不清的刀。 */
    static final int MAX_BLADES = 6;
    /** 常驻的「陪衬」刀数（见类注释）。 */
    static final int EXTRA_BLADES = 2;

    /** 主色：猎手绿。 */
    static final Color GREEN = new Color(0.45f, 0.95f, 0.55f, 1.0f);
    /** 亮色：刀刃。 */
    static final Color BLADE = new Color(0.82f, 0.96f, 1.00f, 1.0f);
    /** 裂纹 / 刃尖高光：更白更亮。 */
    static final Color CRACK = new Color(0.95f, 1.00f, 1.00f, 1.0f);
    /** 木柄：暗暖色。 */
    static final Color WOOD = new Color(0.58f, 0.36f, 0.20f, 1.0f);
    /** 护手与柄头：暗色金属。 */
    static final Color GUARD = new Color(0.34f, 0.38f, 0.44f, 1.0f);

    /**
     * 刃尖朝向：<b>一律水平朝右</b>（度，数学方向 0 = 向右）。
     * <p>
     * 按需求，所有刀同向朝右 —— 读作「一排待发的刀」，
     * 而不是上一版那种「绕身、各朝各的外侧」。
     */
    static final float DIR_DEG = 0.0f;

    /** 悬浮中心高度（× 身高）：胸口偏上。 */
    private static final float CY_RATIO = 0.58f;
    /** 悬浮中心横向位置（× 身体半宽）：正数 = 右侧。 */
    private static final float CX_RATIO = 1.62f;
    /** 刀长（× 身体半宽）：本轮放大。 */
    private static final float LEN_RATIO = 0.62f;
    /** 刀长的像素下限。 */
    private static final float LEN_MIN = 34.0f;
    /** 每一列的纵向间隔（× 刀长）。 */
    private static final float ROW_GAP = 0.82f;
    /** 同一列内两把之间的横向错开（× 刀长）。 */
    private static final float COL_OFFSET = 0.42f;
    /** 轻浮幅度（像素，未乘 scale）与周期。 */
    static final float BOB = 5.0f;
    static final float BOB_PERIOD = 2.8f;

    private BladesFx() {
    }

    // ---------------- 状态 ----------------

    /** 能力层数（0 = 没有这个能力）。 */
    static int stacks(AbstractCreature owner) {
        if (owner == null || owner.powers == null) {
            return 0;
        }
        AbstractPower p = owner.getPower(GreenEffectRegistry.BLADES_ID);
        return p == null ? 0 : Math.max(0, Math.abs(p.amount));
    }

    /** 悬浮匕首数 = {@code amount + 2}，钳到 3~6。 */
    static int count(AbstractCreature owner) {
        return MathUtils.clamp(stacks(owner) + EXTRA_BLADES, 3, MAX_BLADES);
    }

    /** 这张牌的特效是否开启。 */
    static boolean isOn() {
        return GreenEffectRegistry.isOn(GreenEffectRegistry.BLADES_CARD);
    }

    // ---------------- 几何 ----------------

    /** 悬浮区中心 X（人物右侧）。 */
    static float centerX(AbstractCreature owner) {
        return BodyFx.centerX(owner) + BodyFx.halfWidth(owner) * CX_RATIO;
    }

    /** 悬浮区中心 Y（不含轻浮）。 */
    static float centerY(AbstractCreature owner) {
        return BodyFx.groundY(owner) + BodyFx.height(owner) * CY_RATIO;
    }

    /** 刀全长（本轮放大）。 */
    static float bladeLen(AbstractCreature owner) {
        return Math.max(LEN_MIN * Settings.scale, BodyFx.halfWidth(owner) * LEN_RATIO);
    }

    /** 刀根处厚度（跟着放大）。 */
    static float bladeThick(AbstractCreature owner) {
        return Math.max(GreenDraw.MIN_LINE * 1.3f * Settings.scale,
                bladeLen(owner) * 0.26f);
    }

    /**
     * 第 {@code i} 把刀的悬浮位置（相对于悬浮区中心）。
     * <p>
     * 单列不够排 6 把，所以按「列」分：每列 2 把，两列上下错开。
     * <pre>
     *   ═╪═ ═╪═      ← 第 1 行（列间横向错开）
     *   ═╪═ ═╪═      ← 第 2 行
     *   ═╪═          ← 第 3 行
     * </pre>
     * 位置只由索引与总数决定，叠加时按总数重新排 ——
     * 这一排是「同一个编队里的排布」，与精准的刻痕同理（规范 §三.7）。
     *
     * @param out 长度 2 的复用数组：[0] = dx，[1] = dy
     */
    static void slotOffset(int i, int total, float len, float[] out) {
        int n = Math.max(1, total);
        int cols = n <= 3 ? 1 : 2;
        int col = i % cols;
        int row = i / cols;
        int rowsInThisCol = (n - col + cols - 1) / cols;
        // 横向：第二列整体右移一段（刃尖朝右，右移不会撞到人物）
        float dx = col * len * COL_OFFSET;
        // 纵向：垂直居中排布该列
        float dy = (row - (rowsInThisCol - 1) * 0.5f) * len * ROW_GAP;
        out[0] = dx;
        out[1] = dy;
    }

    /** 每把刀的轻浮偏移（像素，已乘 scale，按索引错相）。 */
    static float bobOffset(int i, float time) {
        float phase = i * 1.7f;
        return MathUtils.sin(time * MathUtils.PI2 / BOB_PERIOD + phase) * BOB * Settings.scale;
    }

    /** 地面落点 Y（施放段坠落时刀尖所在高度）。 */
    static float groundY(AbstractCreature owner) {
        return BodyFx.groundY(owner) + 8.0f * Settings.scale;
    }
}
