package wizardrobe.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.screens.CardRewardScreen;
import wizardrobe.relics.KnowledgeBase;

@SpirePatch2(clz = CardRewardScreen.class, method = "takeReward", paramtypez = {})
public final class BronzeRewardTakenPatch {
    private BronzeRewardTakenPatch() {}

    @SpirePostfixPatch
    public static void Postfix(CardRewardScreen __instance) {
        AbstractPlayer player = AbstractDungeon.player;
        if (player == null || __instance.rItem == null) {
            return;
        }
        AbstractRelic relic = player.getRelic(KnowledgeBase.ID);
        if (relic instanceof KnowledgeBase) {
            ((KnowledgeBase) relic).finishOffer(__instance.rItem);
        }
    }
}
