package fnhgodbuff;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import fearandhunger.cards.gods.alllmer.BloodSwordStrike;
import fearandhunger.cards.gods.alllmer.WalkOnWater;
import fearandhunger.cards.gods.godofthedepth.BeeSting;
import fearandhunger.cards.gods.godofthedepth.FlockOfCrows;
import fearandhunger.cards.gods.godofthedepth.LocustSwarm;
import fearandhunger.cards.gods.godofthedepth.NeedleWorm;
import fearandhunger.cards.gods.grogoroth.BlackOrb;
import fearandhunger.cards.gods.grogoroth.BlackSmog;
import fearandhunger.cards.gods.grogoroth.Hurting;
import fearandhunger.cards.gods.logic.MothSwarm;
import fearandhunger.cards.gods.logic.RedArc;
import fearandhunger.cards.gods.rher.FeatherRain;
import fearandhunger.cards.gods.rher.LunarMeteorite;
import fearandhunger.cards.gods.sulfurgod.LonginusStrike;
import fearandhunger.cards.gods.sylvian.HealingWhisper;
import fearandhunger.cards.gods.sylvian.LovingWhisper;
import fearandhunger.cards.gods.sylvian.Pheromones;
import fearandhunger.cards.gods.vinushka.Combustion;
import fearandhunger.cards.gods.vinushka.Photosynthesis;
import fearandhunger.cards.gods.vinushka.PyromancyTrick;
import fearandhunger.cards.gods.vinushka.RootsThatReap;
import fearandhunger.cards.gods.vinushka.ScorchedEarth;

/**
 * Fear And Hunger 的神明牌需要同时支付能量与「心眼」（Rev 球），但原版数值明显低于代价。
 *
 * <p>本补丁只在每张牌的构造完成后改写数值字段（费用、伤害、格挡、能力层数），因此卡面上的
 * !D! / !M! / !B! 会自动显示新的数字，升级加成、月灼与主日等修正逻辑均保持原样；心眼需求
 * （描述里的 Rev 图标与 canUse 判定）一律不动，避免描述与实现不一致。</p>
 */
public final class GodCardBuffs {

    private GodCardBuffs() {
    }

    private static void cost(AbstractCard card, int value) {
        card.cost = value;
        card.costForTurn = value;
    }

    private static void damage(AbstractCard card, int value) {
        card.baseDamage = value;
        card.damage = value;
    }

    private static void magic(AbstractCard card, int value) {
        card.baseMagicNumber = value;
        card.magicNumber = value;
    }

    private static void block(AbstractCard card, int value) {
        card.baseBlock = value;
        card.block = value;
    }

    /* ------------------------------ 格戈洛斯 ------------------------------ */

    /** 伤害术：16→20 伤害，敌人失去力量 4→6。 */
    @SpirePatch2(
        cls = "fearandhunger.cards.gods.grogoroth.Hurting",
        method = "<ctor>",
        paramtypez = {}
    )
    public static class HurtingBuff {
        @SpirePostfixPatch
        public static void Postfix(Hurting __instance) {
            damage(__instance, 20);
            magic(__instance, 6);
        }
    }

    /** 黑雾缭绕：2 费→1 费，2→8 全体伤害，失明 1→2。 */
    @SpirePatch2(
        cls = "fearandhunger.cards.gods.grogoroth.BlackSmog",
        method = "<ctor>",
        paramtypez = {}
    )
    public static class BlackSmogBuff {
        @SpirePostfixPatch
        public static void Postfix(BlackSmog __instance) {
            cost(__instance, 1);
            damage(__instance, 8);
            magic(__instance, 2);
        }
    }

    /** 恶念魔球：每次 10→12 伤害（共 4 次）。 */
    @SpirePatch2(
        cls = "fearandhunger.cards.gods.grogoroth.BlackOrb",
        method = "<ctor>",
        paramtypez = {}
    )
    public static class BlackOrbBuff {
        @SpirePostfixPatch
        public static void Postfix(BlackOrb __instance) {
            damage(__instance, 12);
        }
    }

    /* ------------------------------- 希薇安 ------------------------------- */

    /** 费洛蒙：格挡 7→10，虚弱 1→2。 */
    @SpirePatch2(
        cls = "fearandhunger.cards.gods.sylvian.Pheromones",
        method = "<ctor>",
        paramtypez = {}
    )
    public static class PheromonesBuff {
        @SpirePostfixPatch
        public static void Postfix(Pheromones __instance) {
            block(__instance, 10);
            magic(__instance, 2);
        }
    }

    /** 爱之低语：回复上限 10→14。 */
    @SpirePatch2(
        cls = "fearandhunger.cards.gods.sylvian.LovingWhisper",
        method = "<ctor>",
        paramtypez = {}
    )
    public static class LovingWhisperBuff {
        @SpirePostfixPatch
        public static void Postfix(LovingWhisper __instance) {
            magic(__instance, 14);
        }
    }

    /** 治愈低语：再生 5→8。 */
    @SpirePatch2(
        cls = "fearandhunger.cards.gods.sylvian.HealingWhisper",
        method = "<ctor>",
        paramtypez = {}
    )
    public static class HealingWhisperBuff {
        @SpirePostfixPatch
        public static void Postfix(HealingWhisper __instance) {
            magic(__instance, 8);
        }
    }

    /* ------------------------------ 维努什卡 ------------------------------ */

    /** 引火术：每点心眼 3→4 伤害、2→3 灼伤。 */
    @SpirePatch2(
        cls = "fearandhunger.cards.gods.vinushka.PyromancyTrick",
        method = "<ctor>",
        paramtypez = {}
    )
    public static class PyromancyTrickBuff {
        @SpirePostfixPatch
        public static void Postfix(PyromancyTrick __instance) {
            damage(__instance, 4);
            magic(__instance, 3);
        }
    }

    /** 燃烧：2 费→1 费，8→10 全体伤害，6→8 灼伤。 */
    @SpirePatch2(
        cls = "fearandhunger.cards.gods.vinushka.Combustion",
        method = "<ctor>",
        paramtypez = {}
    )
    public static class CombustionBuff {
        @SpirePostfixPatch
        public static void Postfix(Combustion __instance) {
            cost(__instance, 1);
            damage(__instance, 10);
            magic(__instance, 8);
        }
    }

    /** 灼烧大地：2 费→1 费，每回合灼伤 3→5。 */
    @SpirePatch2(
        cls = "fearandhunger.cards.gods.vinushka.ScorchedEarth",
        method = "<ctor>",
        paramtypez = {}
    )
    public static class ScorchedEarthBuff {
        @SpirePostfixPatch
        public static void Postfix(ScorchedEarth __instance) {
            cost(__instance, 1);
            magic(__instance, 5);
        }
    }

    /** 深根收拔：5→7 全体伤害，缓慢 1→2。 */
    @SpirePatch2(
        cls = "fearandhunger.cards.gods.vinushka.RootsThatReap",
        method = "<ctor>",
        paramtypez = {}
    )
    public static class RootsThatReapBuff {
        @SpirePostfixPatch
        public static void Postfix(RootsThatReap __instance) {
            damage(__instance, 7);
            magic(__instance, 2);
        }
    }

    /** 光合作用：被消耗时回复 6→8。 */
    @SpirePatch2(
        cls = "fearandhunger.cards.gods.vinushka.Photosynthesis",
        method = "<ctor>",
        paramtypez = {}
    )
    public static class PhotosynthesisBuff {
        @SpirePostfixPatch
        public static void Postfix(Photosynthesis __instance) {
            magic(__instance, 8);
        }
    }

    /* ------------------------------ 奥尔弥尔 ------------------------------ */

    /** 鲜血圣剑打击：10→15 伤害。 */
    @SpirePatch2(
        cls = "fearandhunger.cards.gods.alllmer.BloodSwordStrike",
        method = "<ctor>",
        paramtypez = {}
    )
    public static class BloodSwordStrikeBuff {
        @SpirePostfixPatch
        public static void Postfix(BloodSwordStrike __instance) {
            damage(__instance, 15);
        }
    }

    /** 水上行走：每个心眼的格挡 3→5。 */
    @SpirePatch2(
        cls = "fearandhunger.cards.gods.alllmer.WalkOnWater",
        method = "<ctor>",
        paramtypez = {}
    )
    public static class WalkOnWaterBuff {
        @SpirePostfixPatch
        public static void Postfix(WalkOnWater __instance) {
            magic(__instance, 5);
        }
    }

    /* -------------------------------- 雷尔 -------------------------------- */

    /** 月陨：3 费→2 费，44→48 伤害。 */
    @SpirePatch2(
        cls = "fearandhunger.cards.gods.rher.LunarMeteorite",
        method = "<ctor>",
        paramtypez = {}
    )
    public static class LunarMeteoriteBuff {
        @SpirePostfixPatch
        public static void Postfix(LunarMeteorite __instance) {
            cost(__instance, 2);
            damage(__instance, 48);
        }
    }

    /** 落羽：每张被消耗的灵魂 6→10 伤害。 */
    @SpirePatch2(
        cls = "fearandhunger.cards.gods.rher.FeatherRain",
        method = "<ctor>",
        paramtypez = {}
    )
    public static class FeatherRainBuff {
        @SpirePostfixPatch
        public static void Postfix(FeatherRain __instance) {
            damage(__instance, 10);
        }
    }

    /* -------------------------------- 逻辑 -------------------------------- */

    /** 红色电弧：2 费→1 费，每类灵魂 8→10 点生命流失。 */
    @SpirePatch2(
        cls = "fearandhunger.cards.gods.logic.RedArc",
        method = "<ctor>",
        paramtypez = {}
    )
    public static class RedArcBuff {
        @SpirePostfixPatch
        public static void Postfix(RedArc __instance) {
            cost(__instance, 1);
            magic(__instance, 10);
        }
    }

    /** 飞蛾群：2 费→1 费，每张灵魂 4→6 点生命流失。 */
    @SpirePatch2(
        cls = "fearandhunger.cards.gods.logic.MothSwarm",
        method = "<ctor>",
        paramtypez = {}
    )
    public static class MothSwarmBuff {
        @SpirePostfixPatch
        public static void Postfix(MothSwarm __instance) {
            cost(__instance, 1);
            magic(__instance, 6);
        }
    }

    /* ------------------------------- 硫磺神 ------------------------------- */

    /** 朗基努斯打击：13→17 伤害。 */
    @SpirePatch2(
        cls = "fearandhunger.cards.gods.sulfurgod.LonginusStrike",
        method = "<ctor>",
        paramtypez = {}
    )
    public static class LonginusStrikeBuff {
        @SpirePostfixPatch
        public static void Postfix(LonginusStrike __instance) {
            damage(__instance, 17);
        }
    }

    /* ----------------------------- 深渊之神 ----------------------------- */

    /** 水蛭虹吸：每种负面状态 4→6 伤害。 */
    @SpirePatch2(
        cls = "fearandhunger.cards.gods.godofthedepth.NeedleWorm",
        method = "<ctor>",
        paramtypez = {}
    )
    public static class NeedleWormBuff {
        @SpirePostfixPatch
        public static void Postfix(NeedleWorm __instance) {
            damage(__instance, 6);
        }
    }

    /** 乌鸦群：出血 7→9。 */
    @SpirePatch2(
        cls = "fearandhunger.cards.gods.godofthedepth.FlockOfCrows",
        method = "<ctor>",
        paramtypez = {}
    )
    public static class FlockOfCrowsBuff {
        @SpirePostfixPatch
        public static void Postfix(FlockOfCrows __instance) {
            magic(__instance, 9);
        }
    }

    /** 蝗虫群：2 费→1 费，出血 1→4。 */
    @SpirePatch2(
        cls = "fearandhunger.cards.gods.godofthedepth.LocustSwarm",
        method = "<ctor>",
        paramtypez = {}
    )
    public static class LocustSwarmBuff {
        @SpirePostfixPatch
        public static void Postfix(LocustSwarm __instance) {
            cost(__instance, 1);
            magic(__instance, 4);
        }
    }

    /** 蜂刺：2 费→1 费，1→2 伤害，3→4 中毒。 */
    @SpirePatch2(
        cls = "fearandhunger.cards.gods.godofthedepth.BeeSting",
        method = "<ctor>",
        paramtypez = {}
    )
    public static class BeeStingBuff {
        @SpirePostfixPatch
        public static void Postfix(BeeSting __instance) {
            cost(__instance, 1);
            damage(__instance, 2);
            magic(__instance, 4);
        }
    }
}
