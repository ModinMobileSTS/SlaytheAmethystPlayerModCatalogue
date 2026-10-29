package sts.ryoikitenkai.compat.red;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import sts.ryoikitenkai.modcore.RyoikiTenkai;

import java.util.WeakHashMap;

/**
 * 铁甲战士两张能力牌（进化、金属化）的特效登记表与播放逻辑。
 * <p>
 * 刻意不经过父模组的 powerImpls 表：父模组用 AutoAdd 只扫描
 * {@code sts.ryoikitenkai.patch} 包，扫不到本补丁的类。若把特效挂在那张表上，
 * 一旦手动注册没生效就会一个特效都不出现。
 * <p>
 * 所有「派生」特效（在别的特效 update() 里生成的）都必须走 {@link #queue}，
 * 也就是放进 effectsQueue；直接往 effectList 里加会在游戏遍历 effectList 时抛出
 * ConcurrentModificationException。
 * <p>
 * <b>两个队列，按「画在哪里」选</b>：
 * <ul>
 *   <li>{@link #queue} → {@code effectsQueue}：常驻与贴身演出，画在角色身上</li>
 *   <li>{@link #queueTop} → {@code topLevelEffectsQueue}：会飞出手牌区、
 *       或横跨屏幕下半部分的特效用它（进化的吸收弧与抛出光苞走这里）</li>
 * </ul>
 * <p>
 * <b>坐标系约束</b>：特效一律只锚定在 {@code owner.hb}（判定框）与地面上，
 * <b>不绑定骨骼</b>。hitbox 在任何角色、任何换肤、自定义角色上都有，
 * 而骨骼名字各 mod 不同、数量不定，绑骨骼既容易失效，也会让特效随动画抖动。
 */
public final class RedEffectRegistry {
    public static final String EVOLVE_ID = "Evolve";
    public static final String METALLICIZE_ID = "Metallicize";

    /**
     * 进化施放音效：原版「打出能力牌」的那一声。
     * <p>
     * <b>金属化不在这里播</b>：{@code MetallicizePower.playApplyPowerSfx()} 已经播了
     * {@code POWER_METALLICIZE}，再补一个会双响。
     */
    private static final String SFX_EVOLVE = "CARD_POWER_2";
    /** 同一 key 的最短重响间隔（秒）。 */
    private static final float SFX_DEDUP = 0.25f;
    /** 最近一次播放时间，按 key 记。 */
    private static final WeakHashMap<String, Float> SFX_AT = new WeakHashMap<>();

    private static final WeakHashMap<AbstractCreature, EvolveAuraEffect> EVOLVES =
            new WeakHashMap<>();
    private static final WeakHashMap<AbstractCreature, MetalAuraEffect> METALS =
            new WeakHashMap<>();

    private RedEffectRegistry() {
    }

    /**
     * 安全入队：放进 effectsQueue。
     * 游戏的 effectsQueue 会在遍历 effectList 之外被结算，
     * 因此在特效的 update() 里调用它是安全的。
     */
    public static void queue(AbstractGameEffect effect) {
        if (effect != null) {
            AbstractDungeon.effectsQueue.add(effect);
        }
    }

    /**
     * 最顶队列：{@code topLevelEffectsQueue}。
     * 在手牌与 UI 之上渲染。会飞出手牌区、或横跨屏幕下半部分的特效走它。
     */
    public static void queueTop(AbstractGameEffect effect) {
        if (effect != null) {
            AbstractDungeon.topLevelEffectsQueue.add(effect);
        }
    }

    /** 能力是否由玩家开关打开（与父模组设置面板共用同一份开关）。 */
    public static boolean isOn(String cardId) {
        return RyoikiTenkai.isEnable(cardId);
    }

    /**
     * 带去重的单发音效。
     * <p>
     * 用 {@code System.nanoTime()} 记时间戳，不用游戏时钟：
     * 游戏时钟在暂停 / 过场时会停走，去重窗口会因此变成「永久静音」。
     */
    private static void playOnce(String key, float pitchVariation) {
        if (CardCrawlGame.sound == null || key == null) {
            return;
        }
        float now = (float) (System.nanoTime() / 1_000_000_000.0);
        Float last = SFX_AT.get(key);
        if (last != null && now - last < SFX_DEDUP) {
            return;
        }
        SFX_AT.put(key, now);
        CardCrawlGame.sound.play(key, pitchVariation);
    }

    // ---------------- 驱动 ----------------

    /** 能力挂上时启动常驻表现。幂等，重复调用无副作用。 */
    public static void onPowerApplied(AbstractPower power) {
        if (power == null || power.owner == null) {
            return;
        }
        String id = power.ID;
        if (EVOLVE_ID.equals(id)) {
            if (!EVOLVES.containsKey(power.owner)) {
                EvolveAuraEffect fx = new EvolveAuraEffect(power.owner);
                EVOLVES.put(power.owner, fx);
                queue(fx);
            }
        } else if (METALLICIZE_ID.equals(id)) {
            if (!METALS.containsKey(power.owner)) {
                MetalAuraEffect fx = new MetalAuraEffect(power.owner);
                METALS.put(power.owner, fx);
                queue(fx);
            }
        }
    }

    /** 能力被移除时让常驻表现淡出。 */
    public static void onPowerRemoved(AbstractPower power) {
        if (power == null || power.owner == null) {
            return;
        }
        String id = power.ID;
        if (EVOLVE_ID.equals(id)) {
            EvolveAuraEffect fx = EVOLVES.remove(power.owner);
            if (fx != null) {
                fx.isEnd = true;
            }
        } else if (METALLICIZE_ID.equals(id)) {
            MetalAuraEffect fx = METALS.remove(power.owner);
            if (fx != null) {
                fx.isEnd = true;
            }
        }
    }

    /**
     * 兜底取金属化常驻：能力在身但没有常驻实例时补建一个。
     * <p>
     * 场景：玩家先把特效关掉、施加了能力，再打开开关。
     * 这时若什么都不补，要等到能力重新施加才有画面；补建更符合预期。
     * 从力量钩子（回合末）调用，不在特效遍历中，所以 {@link #queue} 是安全的。
     */
    private static MetalAuraEffect ensureMetalAura(AbstractCreature owner) {
        MetalAuraEffect fx = METALS.get(owner);
        if (fx == null && owner.getPower(METALLICIZE_ID) != null) {
            fx = new MetalAuraEffect(owner);
            METALS.put(owner, fx);
            queue(fx);
        }
        return fx;
    }

    // ---------------- 进化 ----------------

    /**
     * 打出「进化」：芽从背后抽出、逐股长出、芽尖绽出光苞抛向抽牌堆。
     * <p>
     * 原版 {@code Evolve.use()} <b>没有任何 VFX</b>，所以这一段是补上的缺口。
     * 一局只几次，因此允许<b>轻震屏 + 音效</b>（规范 §2.11 的频次分级）；
     * 而「抽到状态牌」那一路每回合可能好几次，那里完全静音。
     */
    public static void castEvolve(AbstractCreature owner, AbstractCard card) {
        if (owner == null || !isOn(EVOLVE_ID)) {
            return;
        }
        EvolveFx.onCast(owner, card);
        // 施放时也让常驻的兴奋度顶一下，读作「刚种下去」
        EvolveAuraEffect fx = EVOLVES.get(owner);
        if (fx != null) {
            fx.pulse();
        }
        playOnce(SFX_EVOLVE, 0.06f);
        if (CardCrawlGame.screenShake != null) {
            CardCrawlGame.screenShake.shake(
                    ScreenShake.ShakeIntensity.LOW, ScreenShake.ShakeDur.SHORT, false);
        }
    }

    /**
     * 抽到一张牌。只有<b>状态牌</b>才触发。
     * <p>
     * 这个方法是钩子的唯一入口，所以「是不是状态牌」「有没有 No Draw」
     * 两个判断都收在 {@link EvolveFx} 里，与 {@code EvolvePower.onCardDraw}
     * 的原版条件逐字对齐。
     *
     * @param owner 能力持有者
     * @param card  刚被抽到的那张牌（此时 {@code current_x/y} 是抽牌堆位置）
     */
    public static void onCardDrawn(AbstractCreature owner, AbstractCard card) {
        if (owner == null || card == null || !isOn(EVOLVE_ID)) {
            return;
        }
        if (card.type != AbstractCard.CardType.STATUS) {
            // 原版条件是 STATUS，普通牌完全不触发 —— 演出也不发（规范 §2.9）
            return;
        }
        // 常驻兴奋度顶起：芽会窜一截、叉张得更开
        EvolveAuraEffect fx = EVOLVES.get(owner);
        if (fx != null) {
            fx.pulse();
        }
        // 闸门与 amount 读取都在 EvolveFx 里，与字节码对齐的那份判断只有一处
        EvolveFx.onStatusDrawn(owner, card);
    }

    // ---------------- 金属化 ----------------

    /**
     * 打出「金属化」：甲片逐片飞来、逐片锁上、接缝扫亮。
     * <p>
     * 原版 {@code Metallicize.use()} <b>没有任何 VFX</b>，这一段是补上的缺口。
     * 一局只几次 → 允许轻震屏；<b>不补音效</b>（原版力量自己播 {@code POWER_METALLICIZE}）。
     */
    public static void castMetallicize(AbstractCreature owner, AbstractCard card) {
        if (owner == null || !isOn(METALLICIZE_ID)) {
            return;
        }
        MetalFx.onCast(owner, card);
        // 常驻若已在（重复打出金属化），让它合拢一下，读作「又加了一层甲」
        MetalAuraEffect fx = METALS.get(owner);
        if (fx != null) {
            fx.clank();
        }
        if (CardCrawlGame.screenShake != null) {
            CardCrawlGame.screenShake.shake(
                    ScreenShake.ShakeIntensity.LOW, ScreenShake.ShakeDur.SHORT, false);
        }
    }

    /**
     * 回合末真正获得格挡时（{@code atEndOfTurnPreEndTurnCards}）：
     * 整圈甲片合拢 + 接缝亮起。
     * <p>
     * <b>静音、不震屏、不放火星</b>：这件事每回合都发生一次（规范 §2.11）。
     * 旧实现每次朝身上糊 10 颗 {@code ImpactSparkEffect}，已全部去掉。
     */
    public static void blockMetallicize(AbstractCreature owner) {
        if (owner == null || !isOn(METALLICIZE_ID)) {
            return;
        }
        MetalAuraEffect fx = ensureMetalAura(owner);
        if (fx != null) {
            fx.clank();
        }
    }
}
