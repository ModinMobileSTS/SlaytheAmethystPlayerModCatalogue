package wizardrobe.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.screens.CombatRewardScreen;
import wizardrobe.relics.KnowledgeBase;

import java.util.List;

@SpirePatch2(clz = CombatRewardScreen.class, method = "setupItemReward", paramtypez = {})
public final class BronzeRewardPatch {
    private BronzeRewardPatch() {}

    @SpirePostfixPatch
    public static void Postfix(CombatRewardScreen __instance) {
        AbstractPlayer player = AbstractDungeon.player;
        if (player == null || player.currentHealth <= 0 || player.isDead || player.isEscaping
                || AbstractDungeon.getCurrMapNode() == null) {
            return;
        }
        AbstractRoom room = AbstractDungeon.getCurrRoom();
        if (room == null || !room.isBattleOver || !room.rewardAllowed || room.smoked
                || room.phase != AbstractRoom.RoomPhase.COMPLETE
                || (room.event != null && room.event.noCardsInRewards)) {
            return;
        }
        AbstractRelic relic = player.getRelic(KnowledgeBase.ID);
        if (!(relic instanceof KnowledgeBase)) {
            return;
        }
        // 普通奖励生成完毕后再加一条，不改原来的选牌奖励和其他奖励。
        List<AbstractCard> normalChoices = null;
        for (RewardItem item : __instance.rewards) {
            if (item.type == RewardItem.RewardType.CARD && item.cards != null && !item.cards.isEmpty()
                    && !item.isDone && !item.ignoreReward) {
                normalChoices = item.cards;
                break;
            }
        }
        RewardItem bonus = ((KnowledgeBase) relic).getBattleReward(player, room, normalChoices);
        if (bonus != null && !__instance.rewards.contains(bonus)) {
            __instance.rewards.add(bonus);
            __instance.hasTakenAll = false;
            __instance.positionRewards();
        }
    }
}
