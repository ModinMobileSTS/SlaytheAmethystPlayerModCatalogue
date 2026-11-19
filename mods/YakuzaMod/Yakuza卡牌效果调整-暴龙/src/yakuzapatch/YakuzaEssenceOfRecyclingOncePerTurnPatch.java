package yakuzapatch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import theYakuza.powers.EssenceOfRecyclingPower;

/**
 * 「废品回收」能力：道具损坏时的触发改为每回合仅第一次生效（一回合一次），并同步能力描述。
 */
public final class YakuzaEssenceOfRecyclingOncePerTurnPatch {
    private YakuzaEssenceOfRecyclingOncePerTurnPatch() {
    }

    private static boolean usedThisTurn = false;

    @SpirePatch2(cls = "com.megacrit.cardcrawl.actions.GameActionManager", method = "clear", paramtypez = {})
    public static final class CombatResetPatch {
        private CombatResetPatch() {
        }

        @SpirePostfixPatch
        public static void Postfix() {
            usedThisTurn = false;
        }
    }

    @SpirePatch2(cls = "com.megacrit.cardcrawl.powers.AbstractPower", method = "atStartOfTurn", paramtypez = {})
    public static final class TurnStartResetPatch {
        private TurnStartResetPatch() {
        }

        @SpirePostfixPatch
        public static void Postfix(AbstractPower __instance) {
            if (__instance instanceof EssenceOfRecyclingPower) {
                usedThisTurn = false;
            }
        }
    }

    @SpirePatch2(cls = "theYakuza.powers.EssenceOfRecyclingPower", method = "onItemBreak", paramtypez = {})
    public static final class OnItemBreakPatch {
        private OnItemBreakPatch() {
        }

        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(EssenceOfRecyclingPower __instance) {
            if (usedThisTurn) {
                return SpireReturn.Return();
            }
            usedThisTurn = true;
            return SpireReturn.Continue();
        }
    }

    @SpirePatch2(cls = "theYakuza.powers.EssenceOfRecyclingPower", method = "updateDescription", paramtypez = {})
    public static final class DescriptionPatch {
        private DescriptionPatch() {
        }

        @SpirePostfixPatch
        public static void Postfix(EssenceOfRecyclingPower __instance) {
            String prefix = Settings.language == Settings.GameLanguage.ZHS ? "一回合一次。 NL " : "Once per turn. NL ";
            if (__instance.description != null && !__instance.description.startsWith(prefix)) {
                __instance.description = prefix + __instance.description;
            }
        }
    }
}
