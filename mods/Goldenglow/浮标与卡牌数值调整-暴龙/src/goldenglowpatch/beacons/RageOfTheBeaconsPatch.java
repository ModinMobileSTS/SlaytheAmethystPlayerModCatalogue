package goldenglowpatch.beacons;

import Goldenglow.cards.Attack.RageOfTheBeacons;
import Goldenglow.helper.Hpr;
import Goldenglow.orbs.Buoy;
import Goldenglow.patches.DronePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.defect.ChannelAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.orbs.EmptyOrbSlot;
import java.util.ArrayList;

/**
 * 信标的愤怒（RageOfTheBeacons）重做：
 * 费用 1 -> 2（升级后 1），效果改为"激发所有浮标，然后用浮标填满你的空充能球栏位"，
 * 并同步重写卡面说明文本。
 */
public final class RageOfTheBeaconsPatch {

    private RageOfTheBeaconsPatch() {
    }

    @SpirePatch2(
        cls = "Goldenglow.cards.Attack.RageOfTheBeacons",
        method = "<ctor>",
        paramtypez = {}
    )
    public static final class CtorPatch {
        private CtorPatch() {
        }

        @SpirePostfixPatch
        public static void Postfix(RageOfTheBeacons __instance) {
            __instance.cost = 2;
            __instance.costForTurn = 2;
            // 新效果不需要选择敌人
            __instance.target = AbstractCard.CardTarget.SELF;
            __instance.rawDescription = description();
            __instance.initializeDescription();
        }
    }

    @SpirePatch2(
        cls = "Goldenglow.cards.Attack.RageOfTheBeacons",
        method = "limitedUpgrade",
        paramtypez = {}
    )
    public static final class UpgradePatch {
        private UpgradePatch() {
        }

        @SpirePostfixPatch
        public static void Postfix(RageOfTheBeacons __instance) {
            // 升级后费用 1，保留本回合手牌上的费用增减
            int diff = __instance.costForTurn - __instance.cost;
            __instance.cost = 1;
            if (__instance.costForTurn > 0) {
                __instance.costForTurn = Math.max(0, 1 + diff);
            }
            __instance.upgradedCost = true;
        }
    }

    /** 完全替换原版效果（原版为造成 !D! 伤害 !M! 次）。 */
    @SpirePatch2(
        cls = "Goldenglow.cards.Attack.RageOfTheBeacons",
        method = "use",
        paramtypez = {com.megacrit.cardcrawl.characters.AbstractPlayer.class, AbstractMonster.class}
    )
    public static final class UsePatch {
        private UsePatch() {
        }

        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix() {
            // 1. 激发玩家身上的所有浮标
            for (AbstractOrb orb : new ArrayList<>(AbstractDungeon.player.orbs)) {
                if (orb instanceof Buoy) {
                    Hpr.addToBot(new BuoyEvokeActions.PlayerBuoy(orb));
                }
            }
            // 2. 激发所有怪物身上的浮标
            for (AbstractMonster mo : Hpr.monsters()) {
                if (!Hpr.isAlive(mo)) {
                    continue;
                }
                ArrayList<AbstractOrb> orbs = DronePatch.DroneFieldsPatch.orbs.get(mo);
                int buoyCount = 0;
                for (AbstractOrb orb : orbs) {
                    if (orb instanceof Buoy) {
                        ++buoyCount;
                    }
                }
                for (int i = 0; i < buoyCount; ++i) {
                    Hpr.addToBot(new BuoyEvokeActions.MonsterBuoy(mo));
                }
            }
            // 3. 之前的激发都结算完后，用浮标填满玩家的空充能球栏位
            Hpr.addToBotAbstract(() -> {
                int empty = 0;
                for (AbstractOrb orb : AbstractDungeon.player.orbs) {
                    if (orb instanceof EmptyOrbSlot) {
                        ++empty;
                    }
                }
                for (int i = 0; i < empty; ++i) {
                    Hpr.addToBot(new ChannelAction(new Buoy()));
                }
            });
            return SpireReturn.Return(null);
        }
    }

    private static String description() {
        if (Settings.language == Settings.GameLanguage.ZHS || Settings.language == Settings.GameLanguage.ZHT) {
            return "激发所有 goldenglow:浮标 。 NL 然后用 goldenglow:浮标 填满你的空充能球栏位。";
        }
        if (Settings.language == Settings.GameLanguage.KOR) {
            return "모든 goldenglow:유닛 을 발현합니다. NL 그 후 비어 있는 구체 슬롯을 goldenglow:유닛 으로 채웁니다.";
        }
        return "Evoke all goldenglow:Buoy. NL Then fill your empty orb slots with goldenglow:Buoy.";
    }
}
