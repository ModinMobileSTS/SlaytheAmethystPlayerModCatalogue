package flamingironcladzhs;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.status.Burn;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.StrengthPower;

import flaming_ironclad.powers.InfernumPower;

/**
 * FlamingIronclad 的 InfernumPower 使用 card.name.equals("Burn") 判断是否抽到灼伤，
 * 而 card.name 是本地化后的名字，中文下为“灼伤”，导致该能力永不触发。
 * 这里在英文判定未生效时（卡名不是 "Burn"）按卡牌类型补一次同样的效果，
 * 英文环境下原逻辑已触发，因此不会重复结算。
 */
@SpirePatch2(
    cls = "flaming_ironclad.powers.InfernumPower",
    method = "onCardDraw",
    paramtypez = { AbstractCard.class }
)
public final class InfernumBurnCheckPatch {
    private InfernumBurnCheckPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(InfernumPower __instance, Object[] __args) {
        if (__instance == null || __args == null || __args.length == 0) {
            return;
        }
        if (!(__args[0] instanceof AbstractCard)) {
            return;
        }
        AbstractCard card = (AbstractCard) __args[0];
        if (card instanceof Burn && !"Burn".equals(card.name)) {
            __instance.flash();
            if (AbstractDungeon.actionManager != null) {
                AbstractDungeon.actionManager.addToTop(new ApplyPowerAction(
                    __instance.owner,
                    __instance.owner,
                    new StrengthPower(__instance.owner, 2),
                    __instance.amount
                ));
            }
        }
    }
}
