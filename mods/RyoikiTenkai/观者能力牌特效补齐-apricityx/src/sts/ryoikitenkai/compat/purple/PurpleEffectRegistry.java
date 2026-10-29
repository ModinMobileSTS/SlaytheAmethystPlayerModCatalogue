package sts.ryoikitenkai.compat.purple;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import sts.ryoikitenkai.modcore.RyoikiTenkai;

import java.util.HashMap;
import java.util.WeakHashMap;

/**
 * 观者缺失特效的登记表（12 张<b>能力牌</b>）。
 * <p>
 * 父模组只做了观者的两张能力（神相 DevaForm、真言 Mantra 的紫色风粒子），
 * 其余这些牌打出去没有任何视觉。这里按机制给每张牌定一个专属表现。
 * <p>
 * <b>范围说明</b>：「以手拒之」（{@code TalkToTheHand}）是<b>攻击牌</b>，
 * 它贴在敌人身上的 {@code BlockReturnPower} 特效已整体移出本补丁；
 * 「欧米伽」（{@code Omega}）虽然是无色衍生牌，但类型仍是能力牌，所以仍在表内。
 * <p>
 * 能力的生命周期由本补丁自己的钩子驱动：父模组的 powerImpls 表只扫
 * {@code sts.ryoikitenkai.patch} 包，扫不到本补丁的类。
 * <p>
 * <h2>两个特效队列，按<b>画在哪里</b>选（不是按「是不是常驻」）</h2>
 * <ul>
 *   <li>{@link #queue} → {@code effectsQueue}（普通层）。
 *       在角色、怪物的层次里渲染，<b>会被手牌与底部 UI 盖住</b>。</li>
 *   <li>{@link #queueTop} → {@code topLevelEffectsQueue}（最顶层）。
 *       在手牌、底部面板与 UI 之上渲染。</li>
 * </ul>
 * <h2>没有常驻表现的两张牌</h2>
 * <ul>
 *   <li><b>猛虎下山</b>：全部视觉落在<b>敌人身上</b>（{@link RushdownClawEffect}）</li>
 *   <li><b>斋戒</b>：全部视觉是<b>两条单向因果链</b></li>
 * </ul>
 * <h2>已脱离通用骨架的牌</h2>
 * 通用骨架（{@link MotifAuraEffect} / {@link MotifBurstEffect}）只适合形状简单、
 * 不需要状态联动的牌。<b>现在只剩欧米伽还走它</b>；其余这些各自有专用实现：
 * <ul>
 *   <li>如水 —— {@code WaterAuraEffect}：自写 GLSL 逐像素画水面</li>
 *   <li>心之堡垒 —— {@code FortressAuraEffect}：立体立方体 + 深度排序</li>
 *   <li>确立基础 —— {@code FoundationAuraEffect}：一座碑 + 刻线 = 降费点数</li>
 *   <li>虔信 —— {@code DevotionAuraEffect}：10 根刻度 = 真言进度</li>
 *   <li>先见之明 —— {@code ForesightAuraEffect}：一个框 = 一张要占卜的牌</li>
 *   <li>研习 —— {@code StudyAuraEffect}：一页纸 = 一张洞见；
 *       唯一一个挂在<b>屏幕 UI 区</b>的常驻</li>
 *   <li>涅槃 —— {@code NirvanaAuraEffect}：一层护壁 = 一次占卜的格挡；
 *       唯一一个层数<b>与战斗状态实时联动</b>的常驻</li>
 *   <li>洞悉现实 —— {@code MasterRealityAuraEffect}：右侧身外的「＋」刻印 +
 *       六道刻痕 = 本场已升级的牌数；触发演出按 Action 的真实去向落到具体牌堆</li>
 * </ul>
 */
public final class PurpleEffectRegistry {
    // ---------------- 卡牌 ID（同时也是父模组设置面板的开关键） ----------------
    public static final String CARD_BATTLE_HYMN = "BattleHymn";
    public static final String CARD_DEVOTION = "Devotion";
    public static final String CARD_ESTABLISHMENT = "Establishment";
    public static final String CARD_FASTING = "Fasting2";
    public static final String CARD_LIKE_WATER = "LikeWater";
    public static final String CARD_MASTER_REALITY = "MasterReality";
    public static final String CARD_MENTAL_FORTRESS = "MentalFortress";
    public static final String CARD_NIRVANA = "Nirvana";
    public static final String CARD_OMEGA = "Omega";
    public static final String CARD_RUSHDOWN = "Adaptation";
    public static final String CARD_STUDY = "Study";
    /** 先见之明。原版 {@code Foresight.ID} 是 "Wireheading"，不是 "Foresight"。 */
    public static final String CARD_FORESIGHT = "Wireheading";
    /** 战歌锻出的临时牌「惩击」。不是本补丁的能力牌，但它的演出属于战歌。 */
    public static final String CARD_SMITE = "Smite";

    // ---------------- 能力 ID（与 AbstractPower.ID 完全一致） ----------------
    public static final String POWER_BATTLE_HYMN = "BattleHymn";
    /** 虔信的能力 ID（原版 {@code DevotionPower.POWER_ID}）。 */
    public static final String POWER_DEVOTION = "DevotionPower";
    public static final String POWER_ESTABLISHMENT = "EstablishmentPower";
    public static final String POWER_FASTING = "EnergyDownPower";
    public static final String POWER_LIKE_WATER = "LikeWaterPower";
    public static final String POWER_MASTER_REALITY = "MasterRealityPower";
    /** 心之堡垒的力量 ID 是 Controlled，与原版 MentalFortressPower.POWER_ID 一致。 */
    public static final String POWER_MENTAL_FORTRESS = "Controlled";
    /** 涅槃的能力 ID。与卡牌 ID 恰好相同（原版 {@code NirvanaPower.POWER_ID}）。 */
    public static final String POWER_NIRVANA = "Nirvana";
    public static final String POWER_OMEGA = "OmegaPower";
    /**
     * 「猛虎下山」的能力 ID。注意它与卡牌 ID 恰好同为 {@code Adaptation}，
     * 这只是原版起名巧合，代码里仍然分成两个常量，不依赖它们相等。
     */
    public static final String POWER_RUSHDOWN = "Adaptation";
    /** 研习的能力 ID。与卡牌 ID 恰好相同（原版 {@code StudyPower.POWER_ID}）。 */
    public static final String POWER_STUDY = "Study";
    /** 先见之明的能力 ID（原版 {@code ForesightPower.POWER_ID}）。 */
    public static final String POWER_FORESIGHT = "WireheadingPower";

    /** 音效键。低频动作才出声。 */
    private static final String SFX_STUDY = "CARD_POWER_2";
    private static final String SFX_NIRVANA = "BLOCK_GAIN_1";
    private static final String SFX_ESTABLISHMENT = "CARD_POWER_2";
    private static final String SFX_MASTER_REALITY = "CARD_POWER_2";
    /** 音效去重表：避免多张同类牌同时触发时炸成一片。 */
    private static final HashMap<String, Float> SFX_AT = new HashMap<>();
    private static final float SFX_DEDUP = 0.20f;

    /** 每张牌的表现定义。独立实现的牌 motif 为 null。 */
    private static final class Spec {
        final Motif motif;
        final Color color;
        final Color accent;

        Spec(Motif motif, Color color, Color accent) {
            this.motif = motif;
            this.color = color;
            this.accent = accent;
        }
    }

    private static final HashMap<String, Spec> SPECS = new HashMap<>();
    private static final HashMap<String, String> POWER_TO_CARD = new HashMap<>();
    /** 卡牌 ID → 该能力的常驻表现（按生物索引，WeakHashMap 避免泄漏）。 */
    private static final HashMap<String, WeakHashMap<AbstractCreature, PowerAuraEffect>> AURAS = new HashMap<>();

    static {
        // 独立实现的牌：motif 留空（Spec 只为通过 onPowerApplied 的登记检查，
        // 具体画什么由 make() 里的专用类决定）
        SPECS.put(CARD_LIKE_WATER, new Spec(null,
                new Color(0.44f, 0.84f, 1.00f, 1.0f), new Color(0.80f, 0.96f, 1.00f, 1.0f)));
        SPECS.put(CARD_RUSHDOWN, new Spec(null, RushdownFx.FUR, RushdownFx.CORE));
        SPECS.put(CARD_MENTAL_FORTRESS, new Spec(null,
                new Color(0.55f, 0.88f, 1.00f, 1.0f), new Color(0.92f, 0.99f, 1.00f, 1.0f)));
        SPECS.put(CARD_BATTLE_HYMN, new Spec(null,
                new Color(0.42f, 0.84f, 1.00f, 1.0f), new Color(0.86f, 0.97f, 1.00f, 1.0f)));
        // 确立基础：主色石、副色刻线金 —— 本补丁唯一的「石」材质
        SPECS.put(CARD_ESTABLISHMENT, new Spec(null,
                FoundationFx.STONE, FoundationFx.GOLD));
        // 斋戒：主色力量红、副色敏捷绿 —— 与它两个光圈的颜色一致
        SPECS.put(CARD_FASTING, new Spec(null,
                FastingFx.STRENGTH, FastingFx.DEXTERITY));
        // 虔信：主色真言紫、副色神性粉 —— 与刻度环的颜色演进一致
        SPECS.put(CARD_DEVOTION, new Spec(null,
                DevotionFx.MANTRA, DevotionFx.DIVINITY));
        // 先见之明：主色冷紫、副色冰白 —— 与它光框的颜色演进一致
        SPECS.put(CARD_FORESIGHT, new Spec(null,
                ForesightFx.VIOLET, ForesightFx.ICE));
        // 研习：主色纸白、副色淡青瓷 —— 本补丁唯一的「纸」材质
        SPECS.put(CARD_STUDY, new Spec(null,
                StudyFx.PAPER, StudyFx.INK));
        // 涅槃：主色青铜、副色棱亮白 —— 本补丁唯一的「金属甲壳」材质
        SPECS.put(CARD_NIRVANA, new Spec(null,
                NirvanaFx.BRONZE, NirvanaFx.EDGE_LIGHT));
        // 洞悉现实：主色冷白、副色近白 —— 刻印材质（同一笔两档亮度）
        SPECS.put(CARD_MASTER_REALITY, new Spec(null,
                MasterRealityFx.PLATE, MasterRealityFx.EDGE));
        // 通用图案骨架：只剩欧米伽（金色 Ω 徽记）
        SPECS.put(CARD_OMEGA, new Spec(Motif.OMEGA,
                new Color(1.00f, 0.84f, 0.42f, 1.0f), new Color(1.00f, 0.95f, 0.70f, 1.0f)));

        POWER_TO_CARD.put(POWER_BATTLE_HYMN, CARD_BATTLE_HYMN);
        POWER_TO_CARD.put(POWER_DEVOTION, CARD_DEVOTION);
        POWER_TO_CARD.put(POWER_ESTABLISHMENT, CARD_ESTABLISHMENT);
        POWER_TO_CARD.put(POWER_FASTING, CARD_FASTING);
        POWER_TO_CARD.put(POWER_LIKE_WATER, CARD_LIKE_WATER);
        POWER_TO_CARD.put(POWER_MASTER_REALITY, CARD_MASTER_REALITY);
        POWER_TO_CARD.put(POWER_MENTAL_FORTRESS, CARD_MENTAL_FORTRESS);
        POWER_TO_CARD.put(POWER_NIRVANA, CARD_NIRVANA);
        POWER_TO_CARD.put(POWER_OMEGA, CARD_OMEGA);
        POWER_TO_CARD.put(POWER_RUSHDOWN, CARD_RUSHDOWN);
        POWER_TO_CARD.put(POWER_STUDY, CARD_STUDY);
        POWER_TO_CARD.put(POWER_FORESIGHT, CARD_FORESIGHT);
    }

    private PurpleEffectRegistry() {
    }

    // ---------------- 基础 ----------------

    static boolean isOn(String cardId) {
        return RyoikiTenkai.isEnable(cardId);
    }

    /**
     * 普通队列：{@code effectsQueue}。
     * 在角色/怪物层次里渲染，<b>会被手牌与底部 UI 盖住</b>。
     */
    public static void queue(AbstractGameEffect effect) {
        if (effect != null) {
            AbstractDungeon.effectsQueue.add(effect);
        }
    }

    /**
     * 最顶队列：{@code topLevelEffectsQueue}。
     * 在手牌、底部面板与 UI 之上渲染。<b>要跨越手牌区 / UI 区的特效走它。</b>
     */
    public static void queueTop(AbstractGameEffect effect) {
        if (effect != null) {
            AbstractDungeon.topLevelEffectsQueue.add(effect);
        }
    }

    /** 带去重的单发音效，避免多张同类牌同时施加时炸成一片。 */
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

    static int stacksOf(AbstractCreature owner, String powerId) {
        if (owner == null || owner.powers == null) {
            return 1;
        }
        AbstractPower power = owner.getPower(powerId);
        return power == null ? 1 : Math.max(1, Math.abs(power.amount));
    }

    // ---------------- 常驻表现的建立/停止 ----------------

    /**
     * 建立常驻表现。
     * <p>
     * 返回 {@code null} 表示这张牌<b>没有常驻表现</b>：
     * <ul>
     *   <li>{@link #CARD_RUSHDOWN}：抓痕全部画在敌人身上</li>
     *   <li>{@link #CARD_FASTING}：只有两条单向因果链</li>
     * </ul>
     */
    private static PowerAuraEffect make(AbstractCreature owner, String cardId, String powerId, Spec spec) {
        if (CARD_RUSHDOWN.equals(cardId) || CARD_FASTING.equals(cardId)) {
            return null;
        }
        if (CARD_LIKE_WATER.equals(cardId)) {
            return new WaterAuraEffect(owner, cardId, powerId);
        }
        if (CARD_ESTABLISHMENT.equals(cardId)) {
            return new FoundationAuraEffect(owner, cardId, powerId);
        }
        if (CARD_DEVOTION.equals(cardId)) {
            return new DevotionAuraEffect(owner, cardId, powerId);
        }
        if (CARD_FORESIGHT.equals(cardId)) {
            return new ForesightAuraEffect(owner, cardId, powerId);
        }
        if (CARD_STUDY.equals(cardId)) {
            return new StudyAuraEffect(owner, cardId, powerId);
        }
        if (CARD_NIRVANA.equals(cardId)) {
            return new NirvanaAuraEffect(owner, cardId, powerId);
        }
        if (CARD_MENTAL_FORTRESS.equals(cardId)) {
            return new FortressAuraEffect(owner, cardId, powerId);
        }
        if (CARD_BATTLE_HYMN.equals(cardId)) {
            return new HymnAuraEffect(owner, cardId, powerId);
        }
        if (CARD_MASTER_REALITY.equals(cardId)) {
            return new MasterRealityAuraEffect(owner, cardId, powerId);
        }
        return new MotifAuraEffect(owner, cardId, powerId, spec.motif, spec.color, spec.accent);
    }

    /**
     * 常驻表现的投递队列 —— 按<b>「画在哪里」</b>分流，不按「是不是常驻」。
     * <ul>
     *   <li><b>研习</b>：挂在左下角抽牌堆上方，底部 UI 画在 {@code effectsQueue} 之上</li>
     *   <li><b>涅槃</b>：护壁漫到身体上下，下缘会压进手牌区</li>
     *   <li><b>确立基础</b>：碑在左侧、金光会飞向手牌区，
     *       常驻本身不出手牌区，但<b>一次性演出</b>会 —— 由演出自己走顶层</li>
     *   <li><b>洞悉现实</b>：刻印在右侧身外、不越出手牌区，普通层即可</li>
     *   <li>其余：挂在角色身上且不越出手牌区，普通层即可</li>
     * </ul>
     */
    private static void queueAura(String cardId, PowerAuraEffect aura) {
        if (CARD_STUDY.equals(cardId) || CARD_NIRVANA.equals(cardId)) {
            queueTop(aura);
        } else {
            queue(aura);
        }
    }

    private static void start(AbstractCreature owner, String cardId, String powerId, Spec spec) {
        WeakHashMap<AbstractCreature, PowerAuraEffect> map = AURAS.get(cardId);
        if (map == null) {
            map = new WeakHashMap<>();
            AURAS.put(cardId, map);
        }
        PowerAuraEffect old = map.remove(owner);
        if (old != null) {
            old.isEnd = true;
        }
        PowerAuraEffect aura = make(owner, cardId, powerId, spec);
        // null = 这张牌没有常驻表现
        if (aura == null) {
            return;
        }
        map.put(owner, aura);
        queueAura(cardId, aura);
    }

    private static void stop(AbstractCreature owner, String cardId) {
        WeakHashMap<AbstractCreature, PowerAuraEffect> map = AURAS.get(cardId);
        if (map == null) {
            return;
        }
        PowerAuraEffect aura = map.remove(owner);
        if (aura != null) {
            aura.isEnd = true;
        }
    }

    /** 能力真正触发时：让该能力的常驻表现自己动一下。 */
    private static void pulse(AbstractCreature owner, String cardId) {
        WeakHashMap<AbstractCreature, PowerAuraEffect> map = AURAS.get(cardId);
        if (map == null) {
            return;
        }
        PowerAuraEffect aura = map.get(owner);
        if (aura != null) {
            aura.pulse();
        }
    }

    /** 取当前活着的某个常驻表现实例；没有则返回 null。 */
    private static PowerAuraEffect auraOf(AbstractCreature owner, String cardId) {
        WeakHashMap<AbstractCreature, PowerAuraEffect> map = AURAS.get(cardId);
        if (map == null) {
            return null;
        }
        return map.get(owner);
    }

    /** 放一次该牌的一次性爆发。 */
    private static void burst(AbstractCreature owner, String cardId, float power) {
        if (owner == null) {
            return;
        }
        if (CARD_LIKE_WATER.equals(cardId)) {
            queue(new WaterBurstEffect(owner, power));
            return;
        }
        if (CARD_RUSHDOWN.equals(cardId)) {
            // 猛虎下山的触发只给反馈，不画抓痕（抓痕只在打到敌人时才出现）
            RushdownFx.onEnterWrath(AbstractDungeon.player, RushdownFx.stacks(owner));
            return;
        }
        if (CARD_ESTABLISHMENT.equals(cardId)) {
            // 确立基础：回合末压实 + 逐牌金光
            FoundationFx.onTurnEnd(owner);
            return;
        }
        if (CARD_FASTING.equals(cardId)) {
            FastingFx.onTurnStart(owner);
            return;
        }
        if (CARD_DEVOTION.equals(cardId)) {
            DevotionFx.onTurnStart(owner);
            return;
        }
        if (CARD_FORESIGHT.equals(cardId)) {
            // 先见之明：回合开始占卜 —— 一列光框被依次扫描显影
            ForesightFx.onScry(owner);
            return;
        }
        if (CARD_STUDY.equals(cardId)) {
            // 研习：回合末把洞见塞进抽牌堆 —— 页一张张落进左下角的牌堆
            StudyFx.onTurnEnd(owner);
            return;
        }
        if (CARD_NIRVANA.equals(cardId)) {
            // 涅槃：占卜得到格挡 —— 新的一层护壁从体内顶出
            NirvanaFx.onScry(owner);
            return;
        }
        if (CARD_MENTAL_FORTRESS.equals(cardId)) {
            queue(new FortressBurstEffect(owner, FortressPulse.lastOld(owner),
                    FortressPulse.lastNew(owner), FortressPulse.lastNewStance(owner)));
            return;
        }
        // 战歌：音符数量由手牌里的惩击数决定，锻出时不需要额外特效
        if (CARD_BATTLE_HYMN.equals(cardId)) {
            pulse(owner, CARD_BATTLE_HYMN);
            return;
        }
        Spec spec = SPECS.get(cardId);
        if (spec == null || spec.motif == null) {
            return;
        }
        queue(new MotifBurstEffect(owner, spec.motif, spec.color, spec.accent, power));
    }

    /** 能力刚被施加时启动常驻表现。 */
    public static void onPowerApplied(AbstractPower power) {
        if (power == null || power.owner == null || power.ID == null) {
            return;
        }
        String cardId = POWER_TO_CARD.get(power.ID);
        if (cardId == null || !isOn(cardId)) {
            return;
        }
        Spec spec = SPECS.get(cardId);
        if (spec == null) {
            return;
        }
        start(power.owner, cardId, power.ID, spec);
    }

    /** 能力被移除/被关闭/生物死亡时停掉常驻表现。 */
    public static void onPowerRemoved(AbstractPower power) {
        if (power == null || power.owner == null || power.ID == null) {
            return;
        }
        String cardId = POWER_TO_CARD.get(power.ID);
        if (cardId == null) {
            return;
        }
        stop(power.owner, cardId);
    }

    // ---------------- 施放 ----------------

    /**
     * 卡牌被打出时，以它自己的表现爆发一次。
     * 统一挂在 {@code AbstractPlayer.useCard} 上按 cardID 派发。
     * <p>
     * <b>施放段有三种不同处理</b>（规范 §2.12）：
     * <ul>
     *   <li><b>先见之明</b>：原版已有 BorderFlash + GiantEye，只补<b>弱强度</b>成形（0.75）</li>
     *   <li><b>研习 / 涅槃 / 确立基础 / 洞悉现实</b>：原版<b>完全没有 VFX</b>，必须<b>完整补</b></li>
     * </ul>
     * <p>
     * <b>关于确立基础</b>：旧实现这里直接 {@code return}，理由是
     * 「降费在回合末，施放时不演压实」。那个理由只对了一半 ——
     * 不该演「压实」，但<b>必须演「碑立起来了」</b>。因为：
     * <ol>
     *   <li>原版 {@code Establishment.use()} 一个特效都没有，不补就完全无反馈</li>
     *   <li>常驻的碑在<b>角色左侧</b>，位置本身就是信息；
     *       不演「从地里升起」，玩家不会注意到那里多了一个东西</li>
     *   <li>碑面上的<b>刻线</b>才是「降几费」的读数，
     *       不演刻线点亮，读者就不知道那些线是干什么的</li>
     * </ol>
     * 洞悉现实同理：刻印在<b>右侧身外</b>、六道刻痕才是读数方式，所以施放段
     * 演的是「刻印成形 → 滑到栖位 → 空槽亮出」（{@link MasterRealityFx#onCast}）。
     */
    public static void cast(AbstractPlayer player, AbstractCard card, AbstractMonster target) {
        if (player == null || card == null || card.cardID == null) {
            return;
        }
        rushdownOnAttack(player, card, target);
        hymnSmiteStrike(player, card, target);

        if (!isOn(card.cardID)) {
            return;
        }
        if (CARD_LIKE_WATER.equals(card.cardID)) {
            queue(new WaterBurstEffect(player, 1.15f));
            return;
        }
        if (CARD_RUSHDOWN.equals(card.cardID)) {
            RushdownFx.onCast(player, card);
            return;
        }
        if (CARD_ESTABLISHMENT.equals(card.cardID)) {
            // 施放：不演压实（降费在回合末），但要演「碑升起 + 刻线点亮」。
            // 原版一个特效都没有，这一段是补上的缺口。
            FoundationFx.onCast(player, card);
            playOnce(SFX_ESTABLISHMENT, 0.06f);
            if (CardCrawlGame.screenShake != null) {
                CardCrawlGame.screenShake.shake(
                        ScreenShake.ShakeIntensity.LOW, ScreenShake.ShakeDur.SHORT, false);
            }
            return;
        }
        if (CARD_FASTING.equals(card.cardID)) {
            FastingFx.onCast(player, card);
            return;
        }
        if (CARD_DEVOTION.equals(card.cardID)) {
            // 施放：真言还没到账，所以不演「加真言」；
            // 但要演「十根刻度被刻下来、整圈锁死」。
            DevotionFx.onCast(player);
            return;
        }
        if (CARD_FORESIGHT.equals(card.cardID)) {
            // 原版已有 BorderFlash + GiantEye，这里只补光框成形的弹动。
            ForesightFx.onCast(player, card);
            return;
        }
        if (CARD_STUDY.equals(card.cardID)) {
            // 原版一个特效都没有，所以这一段要完整地演。
            StudyFx.onCast(player, card);
            playOnce(SFX_STUDY, 0.08f);
            if (CardCrawlGame.screenShake != null) {
                CardCrawlGame.screenShake.shake(
                        ScreenShake.ShakeIntensity.LOW, ScreenShake.ShakeDur.SHORT, false);
            }
            return;
        }
        if (CARD_NIRVANA.equals(card.cardID)) {
            // 原版同样一个特效都没有，完整补：护壁一圈圈从体内撑开。
            NirvanaFx.onCast(player, card);
            playOnce(SFX_NIRVANA, 0.10f);
            if (CardCrawlGame.screenShake != null) {
                CardCrawlGame.screenShake.shake(
                        ScreenShake.ShakeIntensity.LOW, ScreenShake.ShakeDur.SHORT, false);
            }
            return;
        }
        if (CARD_MENTAL_FORTRESS.equals(card.cardID)) {
            queue(new FortressBurstEffect(player,
                    new Color(0.72f, 0.80f, 1.0f, 1.0f),
                    new Color(0.55f, 0.88f, 1.0f, 1.0f), null));
            return;
        }
        if (CARD_MASTER_REALITY.equals(card.cardID)) {
            // 施放：原版一个特效都没有（只有 ApplyPowerAction）。
            // 不演「升级」（此刻还没有任何牌被造出来），演「刻印挂上了 + 读数怎么读」。
            MasterRealityFx.onCast(player, card);
            playOnce(SFX_MASTER_REALITY, 0.06f);
            if (CardCrawlGame.screenShake != null) {
                CardCrawlGame.screenShake.shake(
                        ScreenShake.ShakeIntensity.LOW, ScreenShake.ShakeDur.SHORT, false);
            }
            return;
        }
        if (CARD_BATTLE_HYMN.equals(card.cardID)) {
            // 施放这张牌本身：谱面立刻出现，音符数由手牌决定，无需额外爆发
            return;
        }
        Spec spec = SPECS.get(card.cardID);
        if (spec == null || spec.motif == null) {
            return;
        }
        queue(new MotifBurstEffect(player, spec.motif, spec.color, spec.accent, 1.0f));
    }

    /**
     * 打出「惩击」时，<b>谱上属于这张牌的那一组音符</b>脱谱飞向敌人。
     */
    private static void hymnSmiteStrike(AbstractPlayer player, AbstractCard card,
                                        AbstractMonster target) {
        if (!isOn(CARD_BATTLE_HYMN)) {
            return;
        }
        if (!CARD_SMITE.equals(card.cardID)) {
            return;
        }
        if (player.powers == null || player.getPower(POWER_BATTLE_HYMN) == null) {
            return;
        }
        if (target == null) {
            return;
        }

        HymnAuraEffect.ChordSnapshot snap = null;
        PowerAuraEffect aura = auraOf(player, CARD_BATTLE_HYMN);
        if (aura instanceof HymnAuraEffect) {
            snap = ((HymnAuraEffect) aura).takeChordForStrike(card);
        }
        if (snap == null) {
            // 兜底：按这张牌在手牌里的排位推算一格（此时它可能已被移出手牌）
            int count = Math.max(1, HymnAuraEffect.smitesInHand());
            int index = smiteOrderInHand(player, card, count);
            snap = HymnAuraEffect.layoutSnapshot(player, index, count);
        }

        queueTop(new HymnNoteStrikeEffect(snap, target.hb.cX, target.hb.cY));
        pulse(player, CARD_BATTLE_HYMN);
    }

    /** 这张惩击在手牌惩击里排第几；已离手时退回最后一格。 */
    private static int smiteOrderInHand(AbstractPlayer player, AbstractCard card, int count) {
        if (player != null && player.hand != null && player.hand.group != null) {
            int idx = 0;
            for (int i = 0; i < player.hand.group.size(); ++i) {
                AbstractCard c = player.hand.group.get(i);
                if (c == null || !CARD_SMITE.equals(c.cardID)) {
                    continue;
                }
                if (c == card) {
                    return Math.min(idx, count - 1);
                }
                ++idx;
            }
        }
        return Math.max(0, count - 1);
    }

    /**
     * 「猛虎下山」+ 愤怒 + 打出攻击牌 → <b>把抓痕刻到这一刀真正打中的敌人身上</b>。
     */
    private static void rushdownOnAttack(AbstractPlayer player, AbstractCard card,
                                         AbstractMonster target) {
        if (!isOn(CARD_RUSHDOWN)) {
            return;
        }
        if (card.type != AbstractCard.CardType.ATTACK) {
            return;
        }
        if (CARD_RUSHDOWN.equals(card.cardID)) {
            return;
        }
        if (player.powers == null || player.getPower(POWER_RUSHDOWN) == null) {
            return;
        }
        if (!RushdownFx.inWrath(player)) {
            return;
        }
        boolean heavy = card.cost >= 2 || card.baseDamage >= 15;
        RushdownFx.onWrathAttack(player, card, target, heavy);
    }

    // ---------------- 触发 ----------------

    /** 战歌：回合开始锻出 N 张惩击。 */
    public static void hymnTurn(AbstractCreature owner) {
        if (owner == null || !isOn(CARD_BATTLE_HYMN)) {
            return;
        }
        pulse(owner, CARD_BATTLE_HYMN);
    }

    /**
     * 虔信：回合开始累加真言。
     * 每回合都会发生，所以<b>不震屏、不音效</b>。
     */
    public static void devotionTurn(AbstractCreature owner) {
        if (owner == null || !isOn(CARD_DEVOTION)) {
            return;
        }
        pulse(owner, CARD_DEVOTION);
        DevotionFx.onTurnStart(owner);
    }

    /**
     * 虔信：真言满 10，升入神性。
     */
    public static void devotionAscend(AbstractCreature owner) {
        if (owner == null || !isOn(CARD_DEVOTION)) {
            return;
        }
        pulse(owner, CARD_DEVOTION);
        DevotionFx.onAscend(owner);
    }

    /**
     * 确立基础：回合末把保留牌的费用压下去。
     * <p>
     * 演出强度取<b>手牌里真正会被降费的牌数</b>（{@code selfRetain || retain}）：
     * 没有保留牌时原版什么都不做，所以演出也只剩空敲一下，
     * <b>刻线不亮、金光完全不发</b>。
     * <p>
     * 每回合都会发生 —— <b>不震屏、不音效</b>。
     */
    public static void establishmentTurn(AbstractCreature owner) {
        if (owner == null || !isOn(CARD_ESTABLISHMENT)) {
            return;
        }
        pulse(owner, CARD_ESTABLISHMENT);
        FoundationFx.onTurnEnd(owner);
    }

    /**
     * 斋戒：回合开始少一点能量。
     */
    public static void fastingTurn(AbstractCreature owner) {
        if (owner == null || !isOn(CARD_FASTING)) {
            return;
        }
        FastingFx.onTurnStart(owner);
    }

    /**
     * 先见之明：回合开始占卜 N 张。
     * <p>
     * 走普通队列 —— {@code ScryAction} 会在同一帧打开选牌界面，
     * 顶层特效会压在选牌界面上。
     */
    public static void foresightTurn(AbstractCreature owner) {
        if (owner == null || !isOn(CARD_FORESIGHT)) {
            return;
        }
        pulse(owner, CARD_FORESIGHT);
        ForesightFx.onScry(owner);
    }

    /**
     * 研习：回合末把 N 张洞见塞进抽牌堆。
     * 走顶层队列 —— 整段动画都在左下角 UI 区域。
     */
    public static void studyEnd(AbstractCreature owner) {
        if (owner == null || !isOn(CARD_STUDY)) {
            return;
        }
        pulse(owner, CARD_STUDY);
        StudyFx.onTurnEnd(owner);
    }

    /**
     * 涅槃：占了卜，于是获得格挡（{@code NirvanaPower.onScry()}）。
     * <p>
     * <b>与先见之明同一帧触发</b>（{@code ScryAction} 会遍历所有 power 调 onScry）。
     * 所以这里<b>只演「护壁多了一层」，绝不画占卜/预视的语义</b>。
     */
    public static void nirvanaScry(AbstractCreature owner) {
        if (owner == null || !isOn(CARD_NIRVANA)) {
            return;
        }
        pulse(owner, CARD_NIRVANA);
        NirvanaFx.onScry(owner);
    }

    /** 如水：平静结束回合获得格挡，水面被搅动、冲起一道水柱。 */
    public static void likeWaterEnd(AbstractCreature owner) {
        if (owner == null || !isOn(CARD_LIKE_WATER)) {
            return;
        }
        pulse(owner, CARD_LIKE_WATER);
        burst(owner, CARD_LIKE_WATER, 0.70f);
    }

    /** 心之堡垒：切换姿态获得格挡 —— 旧壳碎开、新壳成形。 */
    public static void mentalFortressStance(AbstractCreature owner, String oldStance, String newStance) {
        if (owner == null || !isOn(CARD_MENTAL_FORTRESS)) {
            return;
        }
        pulse(owner, CARD_MENTAL_FORTRESS);
        FortressPulse.remember(owner, oldStance, newStance);
        queue(new FortressBurstEffect(owner,
                FortressPulse.colorOf(oldStance),
                FortressPulse.colorOf(newStance),
                newStance));
    }

    /** 猛虎下山：进入愤怒 → 只给反馈，不画抓痕。 */
    public static void rushdownStance(AbstractCreature owner) {
        if (owner == null || !isOn(CARD_RUSHDOWN)) {
            return;
        }
        RushdownFx.onEnterWrath(AbstractDungeon.player, RushdownFx.stacks(owner));
    }

    /** 欧米伽：回合末对全体造成伤害。 */
    public static void omegaEnd(AbstractCreature owner) {
        if (owner == null || !isOn(CARD_OMEGA)) {
            return;
        }
        pulse(owner, CARD_OMEGA);
        burst(owner, CARD_OMEGA, 0.85f);
    }

    /**
     * 洞悉现实：原版真的把<b>一张战斗中新造出来的牌</b>升级了。
     * <p>
     * 每个造牌 Action 各自调它一次（见 {@code PurpleTriggerPatches} 里的四个钩子），
     * {@code index} 用于多张同时出场时的错拍。
     * <p>
     * 一张牌一次、每回合可能 1~3 次 —— <b>不震屏、不音效</b>（规范 §2.11）。
     * 落点在牌真正去的地方（手牌区 / 抽牌堆 / 弃牌堆），所以走<b>顶层队列</b>。
     */
    public static void masterRealityCreated(AbstractCard card, MasterRealityFx.Dest dest, int index) {
        AbstractPlayer p = AbstractDungeon.player;
        if (p == null || !isOn(CARD_MASTER_REALITY)) {
            return;
        }
        MasterRealityFx.noteUpgraded();
        pulse(p, CARD_MASTER_REALITY);
        queueTop(new MasterRealityStampEffect(
                MasterRealityFx.markX(p), MasterRealityFx.markY(p),
                MasterRealityFx.destX(dest), MasterRealityFx.destY(dest),
                index * 0.075f));
    }
}
