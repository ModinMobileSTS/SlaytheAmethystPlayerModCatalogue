package sts.ryoikitenkai.compat.green;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.cardManip.CardFlashVfx;
import sts.ryoikitenkai.modcore.RyoikiTenkai;

import java.util.WeakHashMap;

/**
 * 猎手四张能力牌（精准、无限刀刃、工具贸易、蓄势待发）的特效登记表与播放逻辑。
 * <p>
 * 父模组的 {@code powerImpls} 表由 {@code AutoAdd} 只扫描 {@code sts.ryoikitenkai.patch} 包，
 * 扫不到本补丁的类，所以这里不依赖那张表：能力生命周期由本补丁自己的钩子驱动，按能力 ID 派发。
 * <p>
 * 所有「派生」特效（在别的特效 {@code update()} 里生成的）都必须走 {@link #queue}。
 * <p>
 * 配色取猎手的绿色系；例外是「精准」的标靶 —— 它取卡面的红白并压暗
 * （详见 {@link AccuracyFx}）。
 */
public final class GreenEffectRegistry {
    /** 能力 ID（与 {@code AbstractPower.ID} 完全一致，不是卡牌 ID）。 */
    public static final String ACCURACY_ID = "Accuracy";
    public static final String BLADES_ID = "Infinite Blades";
    public static final String TRADE_ID = "Tools Of The Trade";
    public static final String RETAIN_ID = "Retain Cards";

    /** 父模组设置面板里对应的卡牌 ID。 */
    public static final String ACCURACY_CARD = "Accuracy";
    public static final String BLADES_CARD = "Infinite Blades";
    public static final String TRADE_CARD = "Tools of the Trade";
    public static final String RETAIN_CARD = "Well Laid Plans";

    private static final Color GREEN = new Color(0.45f, 0.95f, 0.55f, 1.0f);
    private static final Color GREEN_BRIGHT = new Color(0.75f, 1.00f, 0.78f, 1.0f);

    private static final WeakHashMap<AbstractCreature, AccuracyEdgeAuraEffect> ACCURACY_AURAS = new WeakHashMap<>();
    /** 精准的常驻<b>曲线供能连线</b>（画在手牌之下，永不遮挡卡牌）。 */
    private static final WeakHashMap<AbstractCreature, AccuracyLinkEffect> ACCURACY_LINKS = new WeakHashMap<>();
    private static final WeakHashMap<AbstractCreature, BladesAuraEffect> BLADES_AURAS = new WeakHashMap<>();
    private static final WeakHashMap<AbstractCreature, CardFloatAuraEffect> TRADE_AURAS = new WeakHashMap<>();
    private static final WeakHashMap<AbstractCreature, CardFloatAuraEffect> RETAIN_AURAS = new WeakHashMap<>();

    private GreenEffectRegistry() {
    }

    /** 能力是否由玩家开关打开（与父模组设置面板共用同一份开关）。 */
    public static boolean enabled(String cardId) {
        return RyoikiTenkai.isEnable(cardId);
    }

    static boolean isOn(String cardId) {
        return enabled(cardId);
    }

    /**
     * 安全入队：放进 {@code effectsQueue}。
     * 游戏的 {@code effectsQueue} 会在遍历 {@code effectList} 之外被结算，
     * 因此在特效的 {@code update()} 里调用它是安全的。
     */
    public static void queue(AbstractGameEffect effect) {
        if (effect != null) {
            AbstractDungeon.effectsQueue.add(effect);
        }
    }

    /**
     * 放进 {@code topLevelEffectsQueue}：渲染在手牌与底部 UI <b>之上</b>。
     * <p>
     * <b>谨慎使用</b>：它压在手牌之上，会挡住卡牌。
     * 「精准」的连线与本补丁的卡框都不走这里 ——
     * 连线走 {@link #queue}（牌后），卡框由 {@link AccuracyCardRenderPatch} 画在卡牌自己身上。
     */
    public static void queueTop(AbstractGameEffect effect) {
        if (effect != null) {
            AbstractDungeon.topLevelEffectsQueue.add(effect);
        }
    }

    static Color silent() {
        return GREEN.cpy();
    }

    static Color silentBright() {
        return GREEN_BRIGHT.cpy();
    }

    /** 能力层数；没有这个能力时按 1 处理，避免除零与空指针。 */
    static int stacksOf(AbstractCreature owner, String powerId) {
        if (owner == null || owner.powers == null) {
            return 1;
        }
        AbstractPower power = owner.getPower(powerId);
        return power == null ? 1 : Math.max(1, Math.abs(power.amount));
    }

    /**
     * 「精准」当前层数（0 表示没有这个能力）。
     * <p>
     * 本轮标靶上<b>不再画随层数增长的横条</b>，层数只用于两件事：
     * <ul>
     *   <li>判断「有没有精准」→ 决定要不要画连线与卡框（§2.9）</li>
     *   <li>层数很高时让标靶轻微泛亮（{@link AccuracyFx#overdrive}）</li>
     * </ul>
     * 仍然<b>不 clamp</b>：叠加多少层都要能读出来。
     */
    static int stacks(AbstractCreature owner) {
        if (owner == null || owner.powers == null) {
            return 0;
        }
        AbstractPower power = owner.getPower(ACCURACY_ID);
        return power == null ? 0 : Math.max(0, Math.abs(power.amount));
    }

    /** 取无限刀刃的常驻实例（施放要用它当前的相位做交接）。 */
    static BladesAuraEffect bladesAura(AbstractCreature owner) {
        return owner == null ? null : BLADES_AURAS.get(owner);
    }

    // ---------------- 常驻光效 ----------------

    /**
     * 精准的常驻：<b>左侧的红白标靶</b> + <b>常驻曲线供能连线</b>。
     * <pre>
     *   标靶        → {@link #queue}（角色层）
     *   曲线连线    → {@link #queue}（角色层 / 手牌之下）：永不遮挡卡牌
     *   卡牌附魔框  → 不在任何队列里，由 {@link AccuracyCardRenderPatch}
     *                 在卡牌自己的渲染流程里画（沿用原版发光的参数）
     * </pre>
     */
    public static void startAccuracy(AbstractCreature owner) {
        if (owner == null || !isOn(ACCURACY_CARD)) {
            return;
        }
        stopAccuracy(owner);
        AccuracyEdgeAuraEffect aura = new AccuracyEdgeAuraEffect(owner);
        ACCURACY_AURAS.put(owner, aura);
        queue(aura);

        AccuracyLinkEffect link = new AccuracyLinkEffect();
        ACCURACY_LINKS.put(owner, link);
        queue(link);
    }

    public static void stopAccuracy(AbstractCreature owner) {
        if (owner == null) {
            return;
        }
        AccuracyEdgeAuraEffect aura = ACCURACY_AURAS.remove(owner);
        if (aura != null) {
            aura.isEnd = true;
        }
        AccuracyLinkEffect link = ACCURACY_LINKS.remove(owner);
        if (link != null) {
            link.isEnd = true;
        }
    }

    /** 无限刀刃的常驻：一排匕首悬浮在人物右侧，刃尖朝右。 */
    public static void startBlades(AbstractCreature owner) {
        if (owner == null || !isOn(BLADES_CARD)) {
            return;
        }
        BladesAuraEffect old = BLADES_AURAS.remove(owner);
        if (old != null) {
            old.isEnd = true;
        }
        BladesAuraEffect aura = new BladesAuraEffect(owner);
        BLADES_AURAS.put(owner, aura);
        queue(aura);
    }

    public static void stopBlades(AbstractCreature owner) {
        if (owner == null) {
            return;
        }
        BladesAuraEffect aura = BLADES_AURAS.remove(owner);
        if (aura != null) {
            aura.isEnd = true;
        }
    }

    public static void startTrade(AbstractCreature owner) {
        if (owner == null || !isOn(TRADE_CARD)) {
            return;
        }
        CardFloatAuraEffect old = TRADE_AURAS.remove(owner);
        if (old != null) {
            old.isEnd = true;
        }
        CardFloatAuraEffect aura = new CardFloatAuraEffect(owner, true);
        TRADE_AURAS.put(owner, aura);
        queue(aura);
    }

    public static void stopTrade(AbstractCreature owner) {
        if (owner == null) {
            return;
        }
        CardFloatAuraEffect aura = TRADE_AURAS.remove(owner);
        if (aura != null) {
            aura.isEnd = true;
        }
    }

    public static void startRetain(AbstractCreature owner) {
        if (owner == null || !isOn(RETAIN_CARD)) {
            return;
        }
        CardFloatAuraEffect old = RETAIN_AURAS.remove(owner);
        if (old != null) {
            old.isEnd = true;
        }
        CardFloatAuraEffect aura = new CardFloatAuraEffect(owner, false);
        RETAIN_AURAS.put(owner, aura);
        queue(aura);
    }

    public static void stopRetain(AbstractCreature owner) {
        if (owner == null) {
            return;
        }
        CardFloatAuraEffect aura = RETAIN_AURAS.remove(owner);
        if (aura != null) {
            aura.isEnd = true;
        }
    }

    // ---------------- 施放 ----------------

    /**
     * 打出「精准」：标靶从人物身上凝出、飞到左侧悬停。
     * <p>
     * 原版 {@code Accuracy.use()} 只有一个 {@code ApplyPowerAction}，<b>一个特效都没有</b>。
     * 施放这一刻能力还没生效，所以只演「这件法器成形了」这个过程，
     * <b>不演点数</b>（点数由卡牌上的附魔边框表达）。
     * <b>不补音效</b>：{@code AccuracyPower} 没重写 {@code playApplyPowerSfx}。
     */
    public static void castAccuracy(AbstractCreature owner, AbstractCard card) {
        if (owner == null || !isOn(ACCURACY_CARD)) {
            return;
        }
        if (card != null) {
            queue(new CardFlashVfx(card, silent(), false));
        }
        queue(new AccuracyCastEffect(owner));
        if (CardCrawlGame.screenShake != null) {
            CardCrawlGame.screenShake.shake(
                    ScreenShake.ShakeIntensity.LOW, ScreenShake.ShakeDur.SHORT, false);
        }
    }

    /**
     * 打出「无限刀刃」：匕首坠落地面 → 浮起转向 → 悬停人物右侧。
     * <p>
     * 数量：原版施加量恒为 {@code 1}（升级只给 {@code isInnate}），
     * 而 {@code InfiniteBladesPower.stackPower} 会累加。所以首次打出按 1 层、
     * 叠加打出按「现有 + 1」层算，再取 {@link BladesFx#count} 的 {@code amount + 2}。
     */
    public static void castInfiniteBlades(AbstractCreature owner, AbstractCard card) {
        if (owner == null || !isOn(BLADES_CARD)) {
            return;
        }
        if (card != null) {
            queue(new CardFlashVfx(card, silent(), false));
        }
        int willBe = Math.max(1, BladesFx.stacks(owner) + 1);
        int n = MathUtils.clamp(willBe + BladesFx.EXTRA_BLADES, 3, BladesFx.MAX_BLADES);
        queue(new BladesSummonEffect(owner, n));
        BladesAuraEffect aura = BLADES_AURAS.get(owner);
        if (aura != null) {
            aura.pulse();
        }
        if (CardCrawlGame.screenShake != null) {
            CardCrawlGame.screenShake.shake(
                    ScreenShake.ShakeIntensity.LOW, ScreenShake.ShakeDur.SHORT, false);
        }
    }

    /** 打出「工具贸易」：绿色卡片轮廓闪光 + 一抽一弃两张卡影 + 胸口一圈。 */
    public static void castToolsOfTheTrade(AbstractCreature owner, AbstractCard card) {
        if (owner == null || !isOn(TRADE_CARD)) {
            return;
        }
        float cx = BodyFx.centerX(owner);
        float gy = BodyFx.groundY(owner);
        float hw = BodyFx.halfWidth(owner);
        float h = BodyFx.height(owner);
        float chest = BodyFx.chestY(owner);

        if (card != null) {
            queue(new CardFlashVfx(card, silent(), false));
        }
        queue(new RingPulseEffect(cx, chest, hw * 0.25f, hw * 0.95f,
                0.92f, silentBright(), 0.32f, 7.0f * Settings.scale, 32, 0.00f, 0.46f));
        queue(new CardGlyphEffect(cx - hw * 0.80f, gy + h * 0.15f, hw * 0.55f, h * 0.55f,
                0.36f * Settings.scale, -8.0f, 14.0f, silentBright(), 0.85f, 0.80f));
        queue(new CardGlyphEffect(cx + hw * 0.85f, chest, hw * 0.30f, -h * 0.50f,
                0.34f * Settings.scale, 9.0f, -16.0f, silent(), 0.65f, 0.85f));
    }

    /** 打出「蓄势待发」：绿色卡片轮廓闪光 + 一张被留下的卡影移到身侧。 */
    public static void castRetainCards(AbstractCreature owner, AbstractCard card) {
        if (owner == null || !isOn(RETAIN_CARD)) {
            return;
        }
        float cx = BodyFx.centerX(owner);
        float gy = BodyFx.groundY(owner);
        float hw = BodyFx.halfWidth(owner);
        float h = BodyFx.height(owner);
        float chest = BodyFx.chestY(owner);

        if (card != null) {
            queue(new CardFlashVfx(card, silent(), false));
        }
        queue(new RingPulseEffect(cx, chest, hw * 0.85f, hw * 0.35f,
                0.92f, silentBright(), 0.34f, 7.5f * Settings.scale, 32, 0.00f, 0.48f));
        queue(new CardGlyphEffect(cx, chest, hw * 0.78f, -h * 0.10f,
                0.40f * Settings.scale, 0.0f, 6.0f, silentBright(), 0.80f, 0.70f));
        for (int i = 0; i < 4; ++i) {
            queue(new RisingMoteEffect(BodyFx.bodyX(owner, 0.70f),
                    gy + MathUtils.random(0.10f, 0.30f) * h,
                    h * MathUtils.random(0.45f, 0.75f),
                    MathUtils.random(5.5f, 9.0f) * Settings.scale, silent()));
        }
    }

    // ---------------- 触发 ----------------

    /**
     * 打出一把小刀（精准真正生效的时刻）。
     * <h2>只做一次脉动，不射弧</h2>
     * 「标靶 → 小刀」的供能关系由<b>常驻</b>表达：曲线连线（牌后）+ 卡牌上的附魔边框。
     * 所以这里只让标靶脉动一下、并把整组连线提亮一档。
     * <p>
     * 频次：一回合可能打好几张 → <b>不震屏、不音效</b>。
     */
    public static void shivUsed(AbstractCreature owner, AbstractCard shiv) {
        if (owner == null || !isOn(ACCURACY_CARD)) {
            return;
        }
        if (owner.powers == null || !owner.hasPower(ACCURACY_ID)) {
            // 没有精准就没有标靶，什么都不演（规范 §2.9）
            return;
        }
        AccuracyEdgeAuraEffect aura = ACCURACY_AURAS.get(owner);
        if (aura != null) {
            aura.pulse();
        }
        AccuracyLinkEffect link = ACCURACY_LINKS.get(owner);
        if (link != null) {
            link.flash();
        }
    }

    /** 回合开始获得小刀：整排刀亮一档（无限刀刃）。 */
    public static void bladeGained(AbstractCreature owner) {
        if (owner == null || !isOn(BLADES_CARD)) {
            return;
        }
        BladesAuraEffect aura = BLADES_AURAS.get(owner);
        if (aura != null) {
            aura.pulse();
        }
    }

    /** 回合开始抽一弃一：一升一降两张卡影。 */
    public static void toolsUpkeep(AbstractCreature owner) {
        if (owner == null || !isOn(TRADE_CARD)) {
            return;
        }
        float cx = BodyFx.centerX(owner);
        float gy = BodyFx.groundY(owner);
        float hw = BodyFx.halfWidth(owner);
        float h = BodyFx.height(owner);
        float chest = BodyFx.chestY(owner);

        queue(new CardGlyphEffect(cx - hw * 0.75f, gy + h * 0.18f, hw * 0.45f, h * 0.50f,
                0.34f * Settings.scale, -7.0f, 12.0f, silentBright(), 0.75f, 0.75f));
        queue(new CardGlyphEffect(cx + hw * 0.80f, chest, hw * 0.25f, -h * 0.45f,
                0.32f * Settings.scale, 8.0f, -14.0f, silent(), 0.55f, 0.80f));
        queue(new RingPulseEffect(cx, chest, hw * 0.60f, hw * 0.95f,
                0.92f, silent(), 0.22f, 6.0f * Settings.scale, 26, 0.00f, 0.36f));
    }

    /** 回合末保留卡牌：一张卡影被轻轻推到身侧。 */
    public static void retainTurn(AbstractCreature owner) {
        if (owner == null || !isOn(RETAIN_CARD)) {
            return;
        }
        float cx = BodyFx.centerX(owner);
        float hw = BodyFx.halfWidth(owner);
        float chest = BodyFx.chestY(owner);

        queue(new RingPulseEffect(cx, chest, hw * 0.75f, hw * 0.35f,
                0.92f, silentBright(), 0.28f, 7.0f * Settings.scale, 28, 0.00f, 0.36f));
        queue(new CardGlyphEffect(cx, chest, hw * 0.72f, -BodyFx.height(owner) * 0.08f,
                0.36f * Settings.scale, 0.0f, 5.0f, silentBright(), 0.70f, 0.60f));
    }

    // ---------------- 驱动 ----------------

    /** 能力刚被施加时启动常驻特效。 */
    public static void onPowerApplied(AbstractPower power) {
        if (power == null || power.owner == null) {
            return;
        }
        if (ACCURACY_ID.equals(power.ID)) {
            startAccuracy(power.owner);
        } else if (BLADES_ID.equals(power.ID)) {
            startBlades(power.owner);
        } else if (TRADE_ID.equals(power.ID)) {
            startTrade(power.owner);
        } else if (RETAIN_ID.equals(power.ID)) {
            startRetain(power.owner);
        }
    }

    /** 能力被移除 / 生物死亡时停掉常驻特效。 */
    public static void onPowerRemoved(AbstractPower power) {
        if (power == null || power.owner == null) {
            return;
        }
        if (ACCURACY_ID.equals(power.ID)) {
            stopAccuracy(power.owner);
        } else if (BLADES_ID.equals(power.ID)) {
            stopBlades(power.owner);
        } else if (TRADE_ID.equals(power.ID)) {
            stopTrade(power.owner);
        } else if (RETAIN_ID.equals(power.ID)) {
            stopRetain(power.owner);
        }
    }
}
