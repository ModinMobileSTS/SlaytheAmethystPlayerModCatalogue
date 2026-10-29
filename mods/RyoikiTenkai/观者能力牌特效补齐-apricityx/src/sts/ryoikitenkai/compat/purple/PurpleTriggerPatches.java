package sts.ryoikitenkai.compat.purple;

import basemod.ReflectionHacks;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDrawPileAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.watcher.BattleHymnPower;
import com.megacrit.cardcrawl.powers.watcher.DevotionPower;
import com.megacrit.cardcrawl.powers.watcher.EnergyDownPower;
import com.megacrit.cardcrawl.powers.watcher.EstablishmentPower;
import com.megacrit.cardcrawl.powers.watcher.ForesightPower;
import com.megacrit.cardcrawl.powers.watcher.MantraPower;
import com.megacrit.cardcrawl.powers.watcher.MentalFortressPower;
import com.megacrit.cardcrawl.powers.watcher.NirvanaPower;
import com.megacrit.cardcrawl.powers.watcher.OmegaPower;
import com.megacrit.cardcrawl.powers.watcher.RushdownPower;
import com.megacrit.cardcrawl.powers.watcher.StudyPower;
import com.megacrit.cardcrawl.stances.AbstractStance;

/**
 * 观者各能力「真正生效」时的补丁。
 * <p>
 * 每个都挂在原版能力<b>自己的回调</b>上，方法名与签名均已对照字节码核实过
 * （这一步不能省：例如 {@code EstablishmentPower} 只有 {@code atEndOfTurn(boolean)}，
 * 并没有 {@code atEndOfTurnPreEndTurnCards}）。
 * <p>
 * 约定（规范 §1.6）：{@code paramtypez} 显式记录；
 * 钩子参数一律用 {@code Object[] __args}，<b>不用命名参数</b>（验证器无法安全匹配）。
 * 这些钩子只负责「什么时候播」，具体画什么在各自的 Effect 类里。
 * <p>
 * <b>本版变更</b>：「以手拒之」（{@code BlockReturnPower}）的特效已整体移除，
 * 所以这里不再有它的钩子；新增四个「洞悉现实」的钩子 —— 它真正生效的地方
 * <b>不在能力里</b>，而在三个造牌 Action 上（详见各嵌套类的注释）。
 */
public final class PurpleTriggerPatches {
    private PurpleTriggerPatches() {
    }

    // ---------------- 姿态类 ----------------

    /**
     * 心之堡垒：切换姿态（任意的不同姿态）获得格挡。
     * <p>
     * 原版判据是 {@code !oldStance.ID.equals(newStance.ID)}，这里保持一致；
     * 并把新旧姿态一并传给登记表，好让爆发用「旧姿态的颜色 → 新姿态的颜色」表现蜕壳。
     */
    @SpirePatch2(
        cls = "com.megacrit.cardcrawl.powers.watcher.MentalFortressPower",
        method = "onChangeStance",
        paramtypez = { AbstractStance.class, AbstractStance.class }
    )
    public static class MentalFortressStance {
        @SpirePostfixPatch
        public static void Postfix(MentalFortressPower __instance, Object[] __args) {
            AbstractStance oldStance = (AbstractStance) __args[0];
            AbstractStance newStance = (AbstractStance) __args[1];
            if (__instance.owner == null || oldStance == null || newStance == null) {
                return;
            }
            String oldId = oldStance.ID;
            String newId = newStance.ID;
            // 与原版同一判据：姿态真的变了才触发
            if (oldId != null && oldId.equals(newId)) {
                return;
            }
            PurpleEffectRegistry.mentalFortressStance(__instance.owner, oldId, newId);
        }
    }

    /**
     * 猛虎下山：进入愤怒时抽牌。
     * 原版判据是「新旧不同 且 新姿态为 Wrath」，这里保持一致。
     */
    @SpirePatch2(
        cls = "com.megacrit.cardcrawl.powers.watcher.RushdownPower",
        method = "onChangeStance",
        paramtypez = { AbstractStance.class, AbstractStance.class }
    )
    public static class RushdownStance {
        @SpirePostfixPatch
        public static void Postfix(RushdownPower __instance, Object[] __args) {
            AbstractStance oldStance = (AbstractStance) __args[0];
            AbstractStance newStance = (AbstractStance) __args[1];
            if (__instance.owner == null || oldStance == null || newStance == null) {
                return;
            }
            if ("Wrath".equals(newStance.ID)
                    && (oldStance.ID == null || !oldStance.ID.equals(newStance.ID))) {
                PurpleEffectRegistry.rushdownStance(__instance.owner);
            }
        }
    }

    // ---------------- 回合开始类 ----------------

    /** 战歌：回合开始锻出惩击（BattleHymnPower.atStartOfTurn）。 */
    @SpirePatch2(
        cls = "com.megacrit.cardcrawl.powers.watcher.BattleHymnPower",
        method = "atStartOfTurn",
        paramtypez = {}
    )
    public static class BattleHymnTurn {
        @SpirePostfixPatch
        public static void Postfix(BattleHymnPower __instance) {
            if (__instance.owner != null) {
                PurpleEffectRegistry.hymnTurn(__instance.owner);
            }
        }
    }

    /** 先见：回合开始占卜（ForesightPower.atStartOfTurn）。 */
    @SpirePatch2(
        cls = "com.megacrit.cardcrawl.powers.watcher.ForesightPower",
        method = "atStartOfTurn",
        paramtypez = {}
    )
    public static class ForesightTurn {
        @SpirePostfixPatch
        public static void Postfix(ForesightPower __instance) {
            if (__instance.owner != null) {
                PurpleEffectRegistry.foresightTurn(__instance.owner);
            }
        }
    }

    /** 斋戒：回合开始少一点能量（EnergyDownPower.atStartOfTurn）。 */
    @SpirePatch2(
        cls = "com.megacrit.cardcrawl.powers.watcher.EnergyDownPower",
        method = "atStartOfTurn",
        paramtypez = {}
    )
    public static class FastingTurn {
        @SpirePostfixPatch
        public static void Postfix(EnergyDownPower __instance) {
            if (__instance.owner != null) {
                PurpleEffectRegistry.fastingTurn(__instance.owner);
            }
        }
    }

    /**
     * 虔信：<b>回合开始、抽牌之后</b>累加真言（{@code DevotionPower.atStartOfTurnPostDraw}）。
     * <p>
     * 这个时机不能挂 {@code atStartOfTurn} —— 原版虔信明确是在抽牌之后才加真言，
     * 挂错时机的话演出会比真言真的到账早一拍。
     * <p>
     * 原版这个方法有两个分支（已对照字节码）：
     * <pre>
     * if (!player.hasPower("Mantra") &amp;&amp; amount &gt;= 10) → ChangeStanceAction("Divinity")
     * else                                            → ApplyPowerAction(MantraPower)
     * </pre>
     * 这里用<b>完全相同的判据</b>复现一次，好让登记表知道该播「升神性」还是「加真言」。
     */
    @SpirePatch2(
        cls = "com.megacrit.cardcrawl.powers.watcher.DevotionPower",
        method = "atStartOfTurnPostDraw",
        paramtypez = {}
    )
    public static class DevotionTurn {
        @SpirePostfixPatch
        public static void Postfix(DevotionPower __instance) {
            if (__instance.owner == null) {
                return;
            }
            AbstractPlayer p = AbstractDungeon.player;
            // 与原版同一判据：没有真言 且 虔信层数已够 10 → 直接升神性
            boolean ascendDirect = p != null
                    && !p.hasPower("Mantra")
                    && __instance.amount >= 10;
            if (ascendDirect) {
                PurpleEffectRegistry.devotionAscend(__instance.owner);
            } else {
                PurpleEffectRegistry.devotionTurn(__instance.owner);
            }
        }
    }

    /**
     * 虔信：<b>真言累加到 10 → 升入神性</b>（{@code MantraPower.stackPower}）。
     * <p>
     * 关键：{@code ChangeStanceAction} 是<b>入队</b>而不是立即执行，
     * 所以 postfix 时 {@code player.stance} 还是旧姿态，不能用它判断。
     * <p>
     * 可靠判据是<b>层数的变化量</b>：
     * <pre>
     *   原版：super.stackPower(stackAmount)  → amount += stackAmount
     *         if (amount &gt;= 10) { 入队升神性; amount -= 10; }
     *
     *   所以：after = before + stackAmount        （未触发）
     *        after = before + stackAmount - 10     （触发）
     * </pre>
     * 用 Prefix 记下 {@code before}，Postfix 比较实测 {@code after} 就能精确判断。
     * <p>
     * 静态字段在游戏主循环里是同步调用的，且 {@code MantraPower} 只挂在玩家身上，
     * 所以不会串数据。
     */
    @SpirePatch2(
        cls = "com.megacrit.cardcrawl.powers.watcher.MantraPower",
        method = "stackPower",
        paramtypez = { int.class }
    )
    public static class MantraAscend {
        /** 进 Prefix 时的真言层数。 */
        private static int before;

        @SpirePrefixPatch
        public static void Prefix(MantraPower __instance) {
            before = __instance.amount;
        }

        @SpirePostfixPatch
        public static void Postfix(MantraPower __instance, Object[] __args) {
            AbstractPlayer p = AbstractDungeon.player;
            if (p == null || __instance.owner != p) {
                return;
            }
            int stackAmount = __args[0] instanceof Integer ? (Integer) __args[0] : 0;
            // 没触发时 after = before + stackAmount；
            // 触发时原版多减了 10 点，所以 after 会少 10。
            int unflagged = before + stackAmount;
            if (__instance.amount <= unflagged - 10) {
                PurpleEffectRegistry.devotionAscend(p);
            }
        }
    }

    // ---------------- 回合末类 ----------------

    /** 树立：回合末把保留牌的费用压低（EstablishmentPower.atEndOfTurn(boolean)）。 */
    @SpirePatch2(
        cls = "com.megacrit.cardcrawl.powers.watcher.EstablishmentPower",
        method = "atEndOfTurn",
        paramtypez = { boolean.class }
    )
    public static class EstablishmentTurn {
        @SpirePostfixPatch
        public static void Postfix(EstablishmentPower __instance, Object[] __args) {
            if (__instance.owner != null) {
                PurpleEffectRegistry.establishmentTurn(__instance.owner);
            }
        }
    }

    /** 研习：回合末把洞见洗进抽牌堆（StudyPower.atEndOfTurn(boolean)）。 */
    @SpirePatch2(
        cls = "com.megacrit.cardcrawl.powers.watcher.StudyPower",
        method = "atEndOfTurn",
        paramtypez = { boolean.class }
    )
    public static class StudyEnd {
        @SpirePostfixPatch
        public static void Postfix(StudyPower __instance, Object[] __args) {
            if (__instance.owner != null) {
                PurpleEffectRegistry.studyEnd(__instance.owner);
            }
        }
    }

    /** 欧米伽：回合末对全体造成伤害（OmegaPower.atEndOfTurn(boolean)）。 */
    @SpirePatch2(
        cls = "com.megacrit.cardcrawl.powers.watcher.OmegaPower",
        method = "atEndOfTurn",
        paramtypez = { boolean.class }
    )
    public static class OmegaEnd {
        @SpirePostfixPatch
        public static void Postfix(OmegaPower __instance, Object[] __args) {
            if (__instance.owner != null) {
                PurpleEffectRegistry.omegaEnd(__instance.owner);
            }
        }
    }

    /**
     * 涅槃：占卜时获得格挡（NirvanaPower.onScry）。
     * <p>
     * 这正是原版自己触发格挡的位置，比挂在 GainBlockAction 上精确 ——
     * 后者会被任意格挡来源误触发。
     */
    @SpirePatch2(
        cls = "com.megacrit.cardcrawl.powers.watcher.NirvanaPower",
        method = "onScry",
        paramtypez = {}
    )
    public static class NirvanaScry {
        @SpirePostfixPatch
        public static void Postfix(NirvanaPower __instance) {
            if (__instance.owner != null) {
                PurpleEffectRegistry.nirvanaScry(__instance.owner);
            }
        }
    }

    // ---------------- 洞悉现实：真正被升级的「新牌」 ----------------

    /**
     * 手牌方向 ①：{@code MakeTempCardInHandAction(card, isOtherCardInCenter)}。
     * <p>
     * <b>为什么挂造牌 Action 而不是 AbstractCard.upgrade()</b>：原版
     * {@code MasterRealityPower} <b>一个回调都没有</b>（无 amount、无 onXxx），
     * 升级动作是写在这三个造牌 Action 自己的构造器/update 里的（已对照字节码）。
     * 挂 {@code upgrade()} 的话，Armaments / Apotheosis 之类「升级已有牌」的效果
     * 也会被算进来 —— 那些<b>不是</b>这张牌管的事（规范 §2.9）。
     * <p>
     * 原版判据（逐字一致）：{@code type != CURSE && type != STATUS && hasPower(MasterRealityPower)}。
     */
    @SpirePatch2(
        cls = "com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction",
        method = "<ctor>",
        paramtypez = { AbstractCard.class, boolean.class }
    )
    public static class MasterRealityHandCtorOne {
        @SpirePostfixPatch
        public static void Postfix(Object[] __args) {
            emitHand(__args, 1);
        }
    }

    /** 手牌方向 ②：{@code MakeTempCardInHandAction(card, amount)}。其余四个构造器都委派到它。 */
    @SpirePatch2(
        cls = "com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction",
        method = "<ctor>",
        paramtypez = { AbstractCard.class, int.class }
    )
    public static class MasterRealityHandCtorMany {
        @SpirePostfixPatch
        public static void Postfix(Object[] __args) {
            int amount = __args.length > 1 && __args[1] instanceof Integer
                    ? Math.max(1, (Integer) __args[1]) : 1;
            emitHand(__args, amount);
        }
    }

    /**
     * 弃牌堆方向：{@code MakeTempCardInDiscardAction(card, sameUUID)}。
     * <p>
     * 原版<b>只在这个构造器里</b>升级，而且只在 {@code !sameUUID} 时升级
     * （{@code (card, int amount)} 那个构造器完全不升级 —— 原版就是这样，这里照抄）。
     */
    @SpirePatch2(
        cls = "com.megacrit.cardcrawl.actions.common.MakeTempCardInDiscardAction",
        method = "<ctor>",
        paramtypez = { AbstractCard.class, boolean.class }
    )
    public static class MasterRealityDiscardCtor {
        @SpirePostfixPatch
        public static void Postfix(Object[] __args) {
            boolean sameUUID = __args.length > 1 && __args[1] instanceof Boolean
                    && (Boolean) __args[1];
            AbstractCard card = cardOf(__args);
            if (sameUUID || card == null || !MasterRealityFx.shouldWitness(card)) {
                return;
            }
            PurpleEffectRegistry.masterRealityCreated(card, MasterRealityFx.Dest.DISCARD, 0);
        }
    }

    /**
     * 抽牌堆方向：{@code MakeTempCardInDrawPileAction.update()}。
     * <p>
     * 原版在第一帧（{@code duration == startDuration}）为每张副本
     * {@code makeStatEquivalentCopy()} 后调用 {@code upgrade()}。
     * 这里用 Prefix 抢在它之前读私有字段 {@code cardToMake}，于是能区分两种情形：
     * <ul>
     *   <li>源牌还没升级 → 原版这次升级<b>真的生效</b> → 逐张演（规范 §2.8）</li>
     *   <li>源牌本来就是 + → 原版 {@code upgrade()} 是空转 → <b>不演</b>（规范 §2.9）</li>
     * </ul>
     * {@code duration / startDuration} 在 {@code AbstractGameAction} 里不是 public，
     * 所以与原版一样按「两个值相等」判断第一帧，只是这里走 {@link ReflectionHacks} 读。
     */
    @SpirePatch2(
        cls = "com.megacrit.cardcrawl.actions.common.MakeTempCardInDrawPileAction",
        method = "update",
        paramtypez = {}
    )
    public static class MasterRealityDrawPile {
        @SpirePrefixPatch
        public static void Prefix(MakeTempCardInDrawPileAction __instance) {
            if (__instance == null || !firstFrame(__instance)) {
                return;
            }
            AbstractCard card = ReflectionHacks.getPrivate(
                    __instance, MakeTempCardInDrawPileAction.class, "cardToMake");
            if (card == null) {
                return;
            }
            // 原版判据：诅咒 / 状态牌不升级
            if (card.type == AbstractCard.CardType.CURSE
                    || card.type == AbstractCard.CardType.STATUS) {
                return;
            }
            if (card.upgraded || !MasterRealityFx.isOn()) {
                return;
            }
            AbstractPlayer p = AbstractDungeon.player;
            if (p == null || !p.hasPower(PurpleEffectRegistry.POWER_MASTER_REALITY)) {
                return;
            }
            int n = Math.max(1, Math.min(__instance.amount, MasterRealityFx.MAX_STAMP));
            for (int i = 0; i < n; ++i) {
                PurpleEffectRegistry.masterRealityCreated(card, MasterRealityFx.Dest.DRAW, i);
            }
        }

        /** 原版就是靠 {@code duration == startDuration} 判断第一帧，这里照抄同一判据。 */
        private static boolean firstFrame(AbstractGameAction action) {
            Object d = ReflectionHacks.getPrivate(action, AbstractGameAction.class, "duration");
            Object s = ReflectionHacks.getPrivate(action, AbstractGameAction.class, "startDuration");
            return d instanceof Float && s instanceof Float
                    && ((Float) d).floatValue() == ((Float) s).floatValue();
        }
    }

    /** 取 {@code __args[0]} 里那张牌。 */
    private static AbstractCard cardOf(Object[] args) {
        return args != null && args.length > 0 && args[0] instanceof AbstractCard
                ? (AbstractCard) args[0] : null;
    }

    /**
     * 手牌方向：一张牌一份演出；超过手牌上限的那几张实际进弃牌堆，
     * 所以落点按 {@code hand.size()} 实际余量拆开（原版 {@code update()} 就是这么分的）。
     */
    private static void emitHand(Object[] args, int amount) {
        AbstractCard card = cardOf(args);
        if (card == null || !MasterRealityFx.shouldWitness(card)) {
            return;
        }
        AbstractPlayer p = AbstractDungeon.player;
        int capacity = p != null && p.hand != null ? Math.max(0, 10 - p.hand.size()) : 0;
        int n = Math.max(1, Math.min(amount, MasterRealityFx.MAX_STAMP));
        for (int i = 0; i < n; ++i) {
            PurpleEffectRegistry.masterRealityCreated(card,
                    i < capacity ? MasterRealityFx.Dest.HAND : MasterRealityFx.Dest.DISCARD, i);
        }
    }
}
