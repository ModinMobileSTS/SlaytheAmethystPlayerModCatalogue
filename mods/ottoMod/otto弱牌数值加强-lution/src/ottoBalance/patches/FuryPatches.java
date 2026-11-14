package ottoBalance.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.unique.PoisonLoseHpAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import ottoMod.cards.NotAngry;
import ottoMod.cards.Refining;
import ottoMod.cards.TryMakeMeAngry;
import ottoMod.cards.Wow;
import ottoMod.powers.Fury;

/**
 * 怒气系列加强。
 *
 * 怒气只有在打出攻击牌时才兑现：每层让该张攻击牌的伤害 +层数，多段攻击按每次命中分别加成，
 * 但每打出一张攻击牌只消耗 1 层；没兑现完的层数会在下回合开始时按层数自伤。
 * 因此怒气牌必须给出足够大的产量，玩家才可能用一两次多段攻击兑现出与风险相称的收益。
 * 原版这几张牌只给 2-6 层，兑现窗口极小；这里把产量写死在字节码里的牌补齐额外怒气，
 * 并同步卡面文字（怒气层数原本是硬编码数字，不是 !M! 占位符）。
 */
public final class FuryPatches {

    private FuryPatches() {
    }

    /** 与原卡 addToBot 等价的入队方式（AbstractCard.addToBot 为 protected）。 */
    private static void push(AbstractGameAction action) {
        AbstractDungeon.actionManager.addToBottom(action);
    }

    // ===================== 不是急眼了：6 层 -> 9 层 =====================

    @SpirePatch2(clz = NotAngry.class, method = "<ctor>", paramtypez = {})
    public static final class NotAngryText {
        @SpirePostfixPatch
        public static void Postfix(NotAngry __instance) {
            __instance.rawDescription = "获得 !M! 层 脆弱 和9层 ottomod:怒气 。 消耗 。";
            __instance.initializeDescription();
        }
    }

    @SpirePatch2(clz = NotAngry.class, method = "use", paramtypez = {AbstractPlayer.class, AbstractMonster.class})
    public static final class NotAngryExtraFury {
        @SpirePostfixPatch
        public static void Postfix(NotAngry __instance) {
            AbstractPlayer p = AbstractDungeon.player;
            if (p == null) {
                return;
            }
            push(new ApplyPowerAction(p, p, new Fury(p, 3), 3));
        }
    }

    // ===================== 炼化：2 层 -> 4 层 =====================

    @SpirePatch2(clz = Refining.class, method = "<ctor>", paramtypez = {})
    public static final class RefiningText {
        @SpirePostfixPatch
        public static void Postfix(Refining __instance) {
            __instance.rawDescription = "造成 !D! 点伤害，获得4点 ottomod:怒气 。 ottomod:营养 ：至多 消耗 2张牌。";
            __instance.initializeDescription();
        }
    }

    @SpirePatch2(clz = Refining.class, method = "use", paramtypez = {AbstractPlayer.class, AbstractMonster.class})
    public static final class RefiningExtraFury {
        @SpirePostfixPatch
        public static void Postfix(Refining __instance) {
            AbstractPlayer p = AbstractDungeon.player;
            if (p == null) {
                return;
            }
            push(new ApplyPowerAction(p, p, new Fury(p, 2), 2));
        }
    }

    // ===================== 试图把我激怒：4 层 -> 6 层 =====================

    @SpirePatch2(clz = TryMakeMeAngry.class, method = "<ctor>", paramtypez = {})
    public static final class TryMakeMeAngryText {
        @SpirePostfixPatch
        public static void Postfix(TryMakeMeAngry __instance) {
            __instance.rawDescription = "获得 !B! 点 临时生命 。下回合获得 !B! 点 格挡 。获得6点 ottomod:怒气 。 消耗 。";
            __instance.initializeDescription();
        }
    }

    @SpirePatch2(clz = TryMakeMeAngry.class, method = "use", paramtypez = {AbstractPlayer.class, AbstractMonster.class})
    public static final class TryMakeMeAngryExtraFury {
        @SpirePostfixPatch
        public static void Postfix(TryMakeMeAngry __instance) {
            AbstractPlayer p = AbstractDungeon.player;
            if (p == null) {
                return;
            }
            push(new ApplyPowerAction(p, p, new Fury(p, 2), 2));
        }
    }

    // ===================== 哇袄！：怒气 2 倍 -> 3 倍 =====================

    @SpirePatch2(clz = Wow.class, method = "<ctor>", paramtypez = {})
    public static final class WowText {
        @SpirePostfixPatch
        public static void Postfix(Wow __instance) {
            __instance.rawDescription = "所有敌人失去等同于你 ottomod:怒气 层数3倍的生命。";
            __instance.initializeDescription();
        }
    }

    /** 原效果是“怒气层数 * 2”的无视格挡伤害且不消耗怒气，这里再追加 1 倍，合计 3 倍。 */
    @SpirePatch2(clz = Wow.class, method = "use", paramtypez = {AbstractPlayer.class, AbstractMonster.class})
    public static final class WowExtraHpLoss {
        @SpirePostfixPatch
        public static void Postfix(Wow __instance) {
            AbstractPlayer p = AbstractDungeon.player;
            if (p == null || !p.hasPower("ottoMod:Fury")) {
                return;
            }
            int fury = p.getPower("ottoMod:Fury").amount;
            if (fury <= 0) {
                return;
            }
            for (AbstractMonster monster : AbstractDungeon.getMonsters().monsters) {
                if (monster.isDead || monster.isDying) {
                    continue;
                }
                push(new PoisonLoseHpAction(monster, p, fury, AbstractGameAction.AttackEffect.FIRE));
            }
        }
    }
}
