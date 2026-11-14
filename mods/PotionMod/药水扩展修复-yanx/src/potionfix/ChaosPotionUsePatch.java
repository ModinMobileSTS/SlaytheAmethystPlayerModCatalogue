package potionfix;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import potionmod.potions.ChaosPotion;

/**
 * 喝下混乱药水的当下就让所有敌人重新决策，玩家能立刻看到意图被打乱，
 * 而不是要到本回合结束才第一次生效。
 */
@SpirePatch2(
    cls = "potionmod.potions.ChaosPotion",
    method = "use",
    paramtypez = { com.megacrit.cardcrawl.core.AbstractCreature.class }
)
public final class ChaosPotionUsePatch {
    private ChaosPotionUsePatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(ChaosPotion __instance) {
        if (AbstractDungeon.getCurrRoom() == null || AbstractDungeon.getCurrRoom().monsters == null) {
            return;
        }
        for (AbstractMonster monster : AbstractDungeon.getCurrRoom().monsters.monsters) {
            ChaosReroll.reroll(monster);
        }
    }
}
