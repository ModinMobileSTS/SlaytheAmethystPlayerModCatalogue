package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.OverlayMenu;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.ui.panels.DiscardPilePanel;

/**
 * 「洞悉现实」（MasterReality）的规格、几何、配色与事件入口。
 * <p>
 * <h2>机制（三层都读过字节码）</h2>
 * <pre>
 * // MasterReality.use() —— 只有一次 ApplyPowerAction，<b>原版一个 VFX 都没有</b>
 * this.addToBot(new ApplyPowerAction(p, p, new MasterRealityPower(p)));
 *
 * // MasterRealityPower —— <b>没有任何回调</b>：无 amount、无 onXxx，纯被动的开关
 *
 * // 真正生效的地方不在能力里，而在「造牌」的三个 Action 上（原版自己就是这么写的）：
 * //   MakeTempCardInHandAction(card, …)        构造器里 upgrade()
 * //   MakeTempCardInDrawPileAction.update()    每次 makeStatEquivalentCopy() 后 upgrade()
 * //   MakeTempCardInDiscardAction(card, sameUUID)  构造器里 upgrade()
 * // 三个点的判据完全一致：
 * //   card.type != CURSE &amp;&amp; card.type != STATUS &amp;&amp; player.hasPower("MasterRealityPower")
 * </pre>
 * 三个事实决定了整个画面：
 * <ol>
 *   <li><b>它作用在哪</b>：只作用于「<b>战斗中被新造出来的牌</b>」，
 *       对已经在牌堆/手牌里的牌<b>什么都不做</b></li>
 *   <li><b>读数是什么</b>：能力本身<b>没有层数</b>。这张牌唯一有意义的数字是
 *       「<b>本场战斗它已经把几张新牌升成了 +</b>」—— 这是「几个东西」，
 *       所以做成<b>同一枚刻印下方的一列可数刻痕</b>（规范 §2.7 / §2.17）</li>
 *   <li><b>施放时机</b>：原版完全无反馈，所以施放这一段必须自己补（规范 §2.12）</li>
 * </ol>
 * <h2>旧实现为什么必须推翻</h2>
 * 原来走通用骨架的 {@link Motif#MIRROR}：<b>身周竖直光柱 + 两侧错位的人形镜面分身</b>。
 * 三条问题：
 * <ol>
 *   <li><b>画了人</b>：{@code Draw.silhouette()} 画的是头/肩/躯干/双腿，是本补丁
 *       检查清单里明令禁止的「具象物」</li>
 *   <li><b>几何与位置都撞</b>：竖直光柱 + 环绕错位 ＝ 与心之堡垒（环绕）、
 *       如水（竖面）同构；而且它整组挂在角色身上，但这张牌的作用对象
 *       <b>根本不是角色</b>，而是「新造出来的牌」</li>
 *   <li><b>没有读数</b>：这张牌的核心信息是「升级了几张」，MIRROR 一个数字都不读</li>
 * </ol>
 * <h2>设计：身侧的「校准刻印」</h2>
 * <ul>
 *   <li><b>形态</b>：一个「＋」刻印（两笔等宽实线交叉，<b>同一笔叠两档亮度</b> ——
 *       规范 §五「金属材质靠同棱两档亮度」，这样它读作「一枚被刻出来的印」，
 *       而不是「两条发光的线」）。「＋」正是本作「升级」的字面符号（卡名后的 +），
 *       所以母题读作<b>产物本身</b>（规范 §2.3）</li>
 *   <li><b>位置</b>：<b>角色右侧身外</b>、悬空。确立基础的石碑在<b>左侧、贴地</b>，
 *       两者正好左右对称、上下错层；欧米伽的 Ω 在<b>头顶</b>、先见之明的光框在
 *       <b>头顶右侧</b>，与这枚刻印都不同高度</li>
 *   <li><b>几何</b>：十字（本补丁唯一）。不是环（规范点名最拥挤的形状）、
 *       不是矩形框、不是扇面、不是棱壳</li>
 *   <li><b>读数</b>：刻印下方<b>六道刻痕</b>，点亮道数 ＝ 本场已升级的牌数；
 *       未点亮的槽位始终可见（规范 §4.10「未激活状态也要看得见」）。
 *       超过六张后整组保持点亮（读数饱和，不再加实体）</li>
 *   <li><b>触发</b>：每升级一张新牌 —— 刻印盖一下（收紧 + 白闪），
 *       一道短促的楔形光痕射向<b>那张牌真正去的地方</b>（手牌区 / 抽牌堆 / 弃牌堆），
 *       落点上落一枚小「＋」印后上扬散去（规范 §3.2 光材质 → 上浮）</li>
 *   <li><b>施放</b>：刻印在胸口右侧成形（{@code swingOut}），停一拍，再滑到右侧栖位；
 *       六道空槽跟着亮出来 —— 演的是「它的存在与读数方式」，不是「它的效果」
 *       （规范 §2.19）</li>
 * </ul>
 * <h2>空间签名（规范 §2.2）</h2>
 * <pre>
 *   确立基础  身侧竖立的石碑      石（实心）    左侧、贴地、静止
 *   洞悉现实  身侧的「＋」刻印    刻印（两档）  右侧、悬空、缓呼吸 + 盖印脉冲
 *   欧米伽    头顶的 Ω 徽记      金           头顶、字符徽记
 *   先见之明  头顶右侧的光框斜列  半透明框      身前上方、扫描
 * </pre>
 * <h2>频次分级（规范 §2.11）</h2>
 * <ul>
 *   <li>升级一张新牌：<b>每张牌一次</b>（战歌一回合能锻 2~3 张惩击）——
 *       <b>不震屏、不音效</b></li>
 *   <li>施放：一局几次 —— 轻震屏 LOW + 音效（原版一点反馈都没有，这里补上）</li>
 * </ul>
 */
final class MasterRealityFx {
    /** 刻印主色：冷白。刻意避开如水的天蓝、先见之明的冷紫、虔信的真言紫。 */
    static final Color PLATE = new Color(0.86f, 0.94f, 1.00f, 1.0f);
    /** 刻印亮档：近白。只用在「同一笔的细芯」与「已点亮的刻痕」上。 */
    static final Color EDGE = new Color(1.00f, 1.00f, 1.00f, 1.0f);

    /** 刻痕槽位上限。再多就保持全亮（读数饱和），不增加实体。 */
    static final int MAX_SCRATCH = 6;
    /** 一次事件里最多播几枚落点印（防止 6 张以上叠成一片）。 */
    static final int MAX_STAMP = 6;

    /** 落点类型：那张新牌到底去了哪里。 */
    enum Dest {
        HAND,
        DRAW,
        DISCARD
    }

    /** 本场战斗（严格说：这张牌被打出之后）已经升级过的牌数。 */
    private static int upgraded;

    private MasterRealityFx() {
    }

    // ---------------- 状态 ----------------

    /** 这张牌的特效是否开启（父模组设置面板里的开关）。 */
    static boolean isOn() {
        return PurpleEffectRegistry.isOn(PurpleEffectRegistry.CARD_MASTER_REALITY);
    }

    /** 重新开始计数。施放这张牌时调用 —— 计数从「打出它」这一刻起算。 */
    static void reset() {
        upgraded = 0;
    }

    /** 已升级张数（常驻读数用）。 */
    static int upgradedCount() {
        return upgraded;
    }

    /** 记一张。由登记表在真正观测到升级时调用。 */
    static void noteUpgraded() {
        if (upgraded < 999) {
            ++upgraded;
        }
    }

    /**
     * 这个「新牌升级」事件该不该播。
     * <p>
     * 判据与原版三个造牌 Action 里那段<b>逐字一致</b>，所以「没生效时不会演得像生效了」
     * （规范 §2.9）：原版不给升级的诅咒/状态牌，这里也不演。
     * <p>
     * 额外的两条只读本补丁自己的东西：能力确实挂着、开关确实开着。
     */
    static boolean shouldWitness(AbstractCard card) {
        AbstractPlayer p = AbstractDungeon.player;
        if (p == null || card == null || !isOn()) {
            return false;
        }
        if (!p.hasPower(PurpleEffectRegistry.POWER_MASTER_REALITY)) {
            return false;
        }
        if (card.type == AbstractCard.CardType.CURSE || card.type == AbstractCard.CardType.STATUS) {
            return false;
        }
        // 原版只对「刚出生的牌」做这件事；已经被升级过的牌调用 upgrade() 是空转
        return card.upgraded;
    }

    // ---------------- 几何（常驻与一次性演出共用同一组函数） ----------------

    /** 刻印半边长（像素，已缩放）。以判定框为基准，并带固定下限。 */
    static float markHalf(AbstractCreature owner) {
        float byWidth = BodyFx.halfWidth(owner) * 0.34f;
        float byHeight = BodyFx.height(owner) * 0.11f;
        return Math.max(15.0f * Settings.scale, Math.max(byWidth, byHeight));
    }

    /** 刻印中心 X：角色<b>右侧身外</b>（判定框边缘之外，不糊立绘）。 */
    static float markX(AbstractCreature owner) {
        return BodyFx.centerX(owner)
                + BodyFx.halfWidth(owner) * 1.10f
                + markHalf(owner) * 0.55f;
    }

    /** 刻印中心 Y：肩部高度，悬空。 */
    static float markY(AbstractCreature owner) {
        return BodyFx.groundY(owner) + BodyFx.height(owner) * 0.74f;
    }

    /** 刻痕竖向间距：≥ 2 倍线宽（规范 §4.6）。 */
    static float scratchStep(float half) {
        return Math.max(10.0f * Settings.scale, half * 0.34f);
    }

    /** 第一道刻痕的 Y：刻印正下方。 */
    static float scratchY(float markY, float half) {
        return markY - half * 1.35f;
    }

    /** 落点屏幕 X。 */
    static float destX(Dest dest) {
        if (dest == Dest.DRAW) {
            // 与研习共享同一个抽牌堆坐标函数（规范 §六「共享常量」）
            return StudyFx.deckX();
        }
        if (dest == Dest.DISCARD) {
            DiscardPilePanel panel = discardPanel();
            // 镜像 DiscardPilePanel 的 DECK_X = 180 * scale - 64（与 §四.9 同一套复算惯例）
            return panel == null ? Settings.WIDTH * 0.92f
                    : panel.current_x + 180.0f * Settings.scale - 64.0f;
        }
        return Settings.WIDTH * 0.5f;
    }

    /** 落点屏幕 Y。 */
    static float destY(Dest dest) {
        if (dest == Dest.DRAW) {
            return StudyFx.deckY();
        }
        if (dest == Dest.DISCARD) {
            DiscardPilePanel panel = discardPanel();
            // 镜像 DiscardPilePanel 的 DECK_Y = 70 * scale - 64
            return panel == null ? Settings.HEIGHT * 0.10f
                    : panel.current_y + 70.0f * Settings.scale - 64.0f;
        }
        // 手牌区中轴：与确立基础旧版用的是同一处口径（这里有明确兜底理由 ——
        // 新牌这一帧还没落位，只能落到「它要去的区域」）
        return Settings.HEIGHT * 0.155f;
    }

    private static DiscardPilePanel discardPanel() {
        OverlayMenu menu = AbstractDungeon.overlayMenu;
        return menu == null ? null : menu.discardPilePanel;
    }

    // ---------------- 事件入口 ----------------

    /**
     * 打出这张牌（{@code MasterReality.use()}）。
     * <p>
     * 原版这里<b>一个特效都没有</b>，所以必须补：刻印在胸口右侧成形、停一拍、
     * 滑到右侧栖位，六道空槽亮出来。计数也在这里归零 —— 读数从「打出它」起算。
     */
    static void onCast(AbstractCreature owner, AbstractCard card) {
        if (owner == null || !isOn()) {
            return;
        }
        reset();
        PurpleEffectRegistry.queue(new MasterRealityCastEffect(owner));
    }

    // ---------------- 绘制（常驻与施放共用） ----------------

    /**
     * 画一枚「＋」刻印。
     *
     * @param half  刻印半边长
     * @param alpha 总不透明度
     * @param hot   0~1：盖印脉冲，越高越亮、交点越白热
     */
    static void drawMark(SpriteBatch sb, float cx, float cy, float half, float alpha, float hot) {
        if (alpha <= 0.004f || half <= 1.0f) {
            return;
        }
        float bright = 0.84f + 0.16f * MathUtils.clamp(hot, 0.0f, 1.0f);

        // 竖笔：宽笔（暗档）+ 细芯（亮档）＝ 同一笔的两档亮度，读作「刻出来的印」
        Draw.line(sb, cx, cy - half * 1.00f, cx, cy + half * 1.00f,
                half * 0.42f, PLATE, alpha * 0.26f * bright);
        Draw.line(sb, cx, cy - half * 1.00f, cx, cy + half * 1.00f,
                half * 0.20f, EDGE, alpha * 0.58f * bright);
        // 横笔
        Draw.line(sb, cx - half * 0.92f, cy, cx + half * 0.92f, cy,
                half * 0.38f, PLATE, alpha * 0.24f * bright);
        Draw.line(sb, cx - half * 0.92f, cy, cx + half * 0.92f, cy,
                half * 0.18f, EDGE, alpha * 0.54f * bright);

        // 交点白热：盖印瞬间最亮
        Draw.glow(sb, cx, cy, half * 1.35f, half * 1.35f, 0.0f, EDGE,
                alpha * (0.10f + 0.30f * MathUtils.clamp(hot, 0.0f, 1.0f)));
    }

    /**
     * 画刻印下方的刻痕槽位。
     *
     * @param grow 每个槽位的点亮进度 0~1（长度与亮度共用）；{@code null} 表示全灭
     */
    static void drawScratchSlots(SpriteBatch sb, float cx, float cy, float half,
                                 float alpha, float[] grow) {
        if (alpha <= 0.004f || half <= 1.0f) {
            return;
        }
        float step = scratchStep(half);
        float y0 = scratchY(cy, half);
        for (int i = 0; i < MAX_SCRATCH; ++i) {
            float g = grow == null ? 0.0f : MathUtils.clamp(grow[i], 0.0f, 1.0f);
            float w = half * (0.26f + 0.34f * g);
            Draw.line(sb, cx - w, y0 - i * step, cx + w, y0 - i * step,
                    half * 0.20f, g > 0.5f ? EDGE : PLATE,
                    alpha * (0.16f + 0.44f * g));
        }
    }
}
