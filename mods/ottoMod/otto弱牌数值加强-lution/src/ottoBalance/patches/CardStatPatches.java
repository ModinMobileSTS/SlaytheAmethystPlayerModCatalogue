package ottoBalance.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import ottoMod.cards.AttackOnMonkey;
import ottoMod.cards.BigStone;
import ottoMod.cards.CallColorByte;
import ottoMod.cards.CalmDetect;
import ottoMod.cards.Dashing;
import ottoMod.cards.DeliveryMan;
import ottoMod.cards.FourWord;
import ottoMod.cards.HeadMarkG;
import ottoMod.cards.Ice_is_Sword;
import ottoMod.cards.JustHitPig;
import ottoMod.cards.Kon;
import ottoMod.cards.OpenBig;
import ottoMod.cards.Refining;
import ottoMod.cards.SilverDog;
import ottoMod.cards.SleepMan;
import ottoMod.cards.StableGun;
import ottoMod.cards.TryMakeMeAngry;

/**
 * 数值层面的平衡补丁：在卡牌构造完成后调整费用 / 伤害 / 格挡 / 魔法数字。
 * 卡面文字中的 !D! / !B! / !M! 占位符会在渲染时读取这些字段，因此文字自动同步。
 */
public final class CardStatPatches {

    private CardStatPatches() {
    }

    /**
     * 依照 AbstractCard.upgradeBaseCost 的方式同步 cost 与 costForTurn，
     * 但不设置 upgradedCost 标记，避免费用被标成“已修改”的绿色。
     */
    private static void setCost(AbstractCard card, int newCost) {
        int diff = card.costForTurn - card.cost;
        card.cost = newCost;
        if (card.costForTurn > 0) {
            card.costForTurn = newCost + diff;
        }
        if (card.costForTurn < 0) {
            card.costForTurn = 0;
        }
    }

    /** 同时写入 baseMagicNumber 与 magicNumber，保证卡面与实战数值一致。 */
    private static void setMagicNumber(AbstractCard card, int value) {
        card.baseMagicNumber = value;
        card.magicNumber = value;
    }

    // ===================== 推轮系列 =====================

    /** 国玉的战争：5 费 12 次推轮过于沉重，降为 3 费并提高到 14 次推轮。 */
    @SpirePatch2(clz = AttackOnMonkey.class, method = "<ctor>", paramtypez = {})
    public static final class AttackOnMonkeyStat {
        @SpirePostfixPatch
        public static void Postfix(AttackOnMonkey __instance) {
            setCost(__instance, 3);
            setMagicNumber(__instance, 14);
        }
    }

    /** 召唤彩字：回合开始推轮 3 次太低，提高到 4 次。 */
    @SpirePatch2(clz = CallColorByte.class, method = "<ctor>", paramtypez = {})
    public static final class CallColorByteStat {
        @SpirePostfixPatch
        public static void Postfix(CallColorByte __instance) {
            setMagicNumber(__instance, 4);
        }
    }

    /** 冲刺（衍生物）：0 费 1 次推轮弱于同类衍生物，提高到 2 次。 */
    @SpirePatch2(clz = Dashing.class, method = "<ctor>", paramtypez = {})
    public static final class DashingStat {
        @SpirePostfixPatch
        public static void Postfix(Dashing __instance) {
            setMagicNumber(__instance, 2);
        }
    }

    // ===================== 怒气系列 =====================

    /** 四字真言：1 费只给 3 点怒气偏弱，提高到 5 点（升级 6）。 */
    @SpirePatch2(clz = FourWord.class, method = "<ctor>", paramtypez = {})
    public static final class FourWordStat {
        @SpirePostfixPatch
        public static void Postfix(FourWord __instance) {
            setMagicNumber(__instance, 5);
        }
    }

    /** 开大！：抽 4 张牌的同时怒气 2 -> 4（升级 5）。 */
    @SpirePatch2(clz = OpenBig.class, method = "<ctor>", paramtypez = {})
    public static final class OpenBigStat {
        @SpirePostfixPatch
        public static void Postfix(OpenBig __instance) {
            setMagicNumber(__instance, 4);
        }
    }

    /** 大石碎胸口：翻倍怒气后额外怒气 3 -> 5，下回合格挡 12 -> 16。 */
    @SpirePatch2(clz = BigStone.class, method = "<ctor>", paramtypez = {})
    public static final class BigStoneStat {
        @SpirePostfixPatch
        public static void Postfix(BigStone __instance) {
            setMagicNumber(__instance, 5);
            __instance.baseBlock = 16;
            __instance.block = 16;
        }
    }

    /** 理性复盘：把怒气换成格挡的效率太低，每层 5 -> 6 点、基础次数 3 -> 4。 */
    @SpirePatch2(clz = CalmDetect.class, method = "<ctor>", paramtypez = {})
    public static final class CalmDetectStat {
        @SpirePostfixPatch
        public static void Postfix(CalmDetect __instance) {
            setMagicNumber(__instance, 4);
            __instance.baseBlock = 6;
            __instance.block = 6;
        }
    }

    /** 脑门纹急：每次获得怒气时的推轮次数 2 -> 3。 */
    @SpirePatch2(clz = HeadMarkG.class, method = "<ctor>", paramtypez = {})
    public static final class HeadMarkGStat {
        @SpirePostfixPatch
        public static void Postfix(HeadMarkG __instance) {
            setMagicNumber(__instance, 3);
        }
    }

    /** 试图把我激怒：临时生命与下回合格挡 4 -> 6。 */
    @SpirePatch2(clz = TryMakeMeAngry.class, method = "<ctor>", paramtypez = {})
    public static final class TryMakeMeAngryStat {
        @SpirePostfixPatch
        public static void Postfix(TryMakeMeAngry __instance) {
            __instance.baseBlock = 6;
        }
    }

    /** 炼化：2 费 16 点伤害配 2 点怒气偏低，基础伤害提高到 18。 */
    @SpirePatch2(clz = Refining.class, method = "<ctor>", paramtypez = {})
    public static final class RefiningStat {
        @SpirePostfixPatch
        public static void Postfix(Refining __instance) {
            __instance.baseDamage = 18;
        }
    }

    // ===================== 能力牌 =====================

    /** 白银低分狗：2 费虚无能力过重，降为 1 费。 */
    @SpirePatch2(clz = SilverDog.class, method = "<ctor>", paramtypez = {})
    public static final class SilverDogStat {
        @SpirePostfixPatch
        public static void Postfix(SilverDog __instance) {
            setCost(__instance, 1);
        }
    }

    /** 吉吉国外卖员：2 费换一张韭菜盒子+，降为 1 费。 */
    @SpirePatch2(clz = DeliveryMan.class, method = "<ctor>", paramtypez = {})
    public static final class DeliveryManStat {
        @SpirePostfixPatch
        public static void Postfix(DeliveryMan __instance) {
            setCost(__instance, 1);
        }
    }

    /** 吉吉国外卖员升级后原为 1 费，基础费用下降后同步改为 0 费。 */
    @SpirePatch2(clz = DeliveryMan.class, method = "upgrade", paramtypez = {})
    public static final class DeliveryManUpgradeStat {
        @SpirePostfixPatch
        public static void Postfix(DeliveryMan __instance) {
            if (__instance.upgraded) {
                setCost(__instance, 0);
            }
        }
    }

    // ===================== 高费牌 =====================

    /** 冰即是刀：4 费 11 格挡过于昂贵，降为 3 费 13 格挡。 */
    @SpirePatch2(clz = Ice_is_Sword.class, method = "<ctor>", paramtypez = {})
    public static final class IceIsSwordStat {
        @SpirePostfixPatch
        public static void Postfix(Ice_is_Sword __instance) {
            setCost(__instance, 3);
            __instance.baseBlock = 13;
        }
    }

    /** 就打铸币：3 费基础伤害 10 太笨重，降为 2 费。 */
    @SpirePatch2(clz = JustHitPig.class, method = "<ctor>", paramtypez = {})
    public static final class JustHitPigStat {
        @SpirePostfixPatch
        public static void Postfix(JustHitPig __instance) {
            setCost(__instance, 2);
        }
    }

    /** “Kon!”：4 费进入神格后几乎没有能量输出，降为 3 费。 */
    @SpirePatch2(clz = Kon.class, method = "<ctor>", paramtypez = {})
    public static final class KonStat {
        @SpirePostfixPatch
        public static void Postfix(Kon __instance) {
            setCost(__instance, 3);
        }
    }

    /** “Kon!”升级后原为 3 费，基础费用下降后同步改为 2 费。 */
    @SpirePatch2(clz = Kon.class, method = "upgrade", paramtypez = {})
    public static final class KonUpgradeStat {
        @SpirePostfixPatch
        public static void Postfix(Kon __instance) {
            if (__instance.upgraded) {
                setCost(__instance, 2);
            }
        }
    }

    /** 睡眠棍哥：2 费回复 9 点并结束回合收益太低，回复提高到 12 点。 */
    @SpirePatch2(clz = SleepMan.class, method = "<ctor>", paramtypez = {})
    public static final class SleepManStat {
        @SpirePostfixPatch
        public static void Postfix(SleepMan __instance) {
            setMagicNumber(__instance, 12);
        }
    }

    /** 稳健棍：非营养状态下 1 费仅 4 点伤害，基础伤害提高到 6。 */
    @SpirePatch2(clz = StableGun.class, method = "<ctor>", paramtypez = {})
    public static final class StableGunStat {
        @SpirePostfixPatch
        public static void Postfix(StableGun __instance) {
            __instance.baseDamage = 6;
            __instance.damage = 6;
        }
    }
}
