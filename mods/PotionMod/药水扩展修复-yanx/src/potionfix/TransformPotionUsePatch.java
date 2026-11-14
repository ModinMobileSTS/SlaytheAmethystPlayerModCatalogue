package potionfix;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.common.TransformCardInHandAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import java.util.ArrayList;
import potionmod.potions.TransformPotion;

/**
 * 转化药水原本照搬了原版“转化（Transmutation）”卡：往手里塞随机无色牌，
 * 而且 TransmutationAction 在 freeToPlayOnce=false 时会调用 energy.use(全部能量)，
 * 于是喝一瓶药水就把当前能量清空，和描述“将手牌中的 2 张牌转化为随机牌”完全不符。
 * 这里跳过原方法体，改为真正随机转化手牌中的牌，并且不再消耗能量。
 */
@SpirePatch2(
    cls = "potionmod.potions.TransformPotion",
    method = "use",
    paramtypez = { com.megacrit.cardcrawl.core.AbstractCreature.class }
)
public final class TransformPotionUsePatch {
    private TransformPotionUsePatch() {
    }

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(TransformPotion __instance) {
        AbstractPlayer player = AbstractDungeon.player;
        if (player == null || AbstractDungeon.actionManager == null) {
            return SpireReturn.Return();
        }
        int handSize = player.hand.size();
        if (handSize <= 0) {
            return SpireReturn.Return();
        }
        int count = Math.min(Math.max(1, __instance.getPotency()), handSize);
        ArrayList<Integer> picked = new ArrayList<Integer>();
        while (picked.size() < count) {
            int index = AbstractDungeon.cardRandomRng.random(handSize - 1);
            if (!picked.contains(Integer.valueOf(index))) {
                picked.add(Integer.valueOf(index));
            }
        }
        java.util.Collections.sort(picked);
        for (int i = 0; i < picked.size(); ++i) {
            int index = picked.get(i).intValue();
            if (index < 0 || index >= player.hand.size()) {
                continue;
            }
            AbstractCard source = player.hand.group.get(index);
            if (source == null) {
                continue;
            }
            AbstractDungeon.transformCard(source, false, AbstractDungeon.cardRandomRng);
            AbstractCard replacement = AbstractDungeon.getTransformedCard();
            if (replacement == null) {
                continue;
            }
            AbstractDungeon.actionManager.addToBottom(new TransformCardInHandAction(index, replacement));
        }
        return SpireReturn.Return();
    }
}
