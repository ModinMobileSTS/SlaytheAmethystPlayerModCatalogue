package wizardrobe.relics;

import automaton.AutomatonChar;
import basemod.abstracts.CustomSavable;
import com.evacipated.cardcrawl.mod.stslib.StSLib;
import com.evacipated.cardcrawl.mod.stslib.relics.OnSkipCardRelic;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardSave;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoomBoss;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import wizardrobe.WizardRobeMod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public final class KnowledgeBase extends LobbyRareRelic
        implements CustomSavable<KnowledgeBase.SavedOffer>, OnSkipCardRelic {
    public static final String ID = WizardRobeMod.makeID("KnowledgeBase");

    // 自动存档以战斗结束时为准。保存额外奖励本身，不重抽，也不更改原版奖励的读档逻辑。
    public static final class SavedOffer {
        private int floor;
        private int act;
        private ArrayList<CardSave> cards = new ArrayList<>();
        private boolean finished;

        private SavedOffer() {}

        SavedOffer(RewardItem reward) {
            floor = AbstractDungeon.floorNum;
            act = AbstractDungeon.actNum;
            for (AbstractCard card : reward.cards) {
                cards.add(new CardSave(card.cardID, card.timesUpgraded, card.misc));
            }
        }
    }

    private SavedOffer savedOffer;
    private AbstractRoom offerRoom;
    private RewardItem offer;

    public KnowledgeBase() {
        super(ID, "KnowledgeBase");
    }

    @Override
    public AbstractRelic makeCopy() {
        return new KnowledgeBase();
    }

    private boolean hasOfferOnThisFloor() {
        return savedOffer != null && savedOffer.floor == AbstractDungeon.floorNum
                && savedOffer.act == AbstractDungeon.actNum;
    }

    @Override
    public SavedOffer onSave() {
        return hasOfferOnThisFloor() ? savedOffer : null;
    }

    @Override
    public void onLoad(SavedOffer data) {
        savedOffer = data;
        offerRoom = null;
        offer = null;
    }

    public void beginBattle(AbstractRoom room) {
        if (!room.isBattleOver && !AbstractDungeon.loading_post_combat) {
            onLoad(null);
        }
    }

    @Override
    public void onEnterRoom(AbstractRoom room) {
        // 离开上一间房才真正放弃未领取的选牌；查看地图后返回不算放弃。
        // 同层刚读出的战后记录还未绑定房间，不要在录入奖励前清掉它。
        if (offerRoom != null || !hasOfferOnThisFloor()) {
            onLoad(null);
        }
    }

    @Override
    public void onSkipCard(RewardItem reward) {
        // 原版「继续」按钮此时只是打开地图，玩家还可以返回领取。
    }

    @Override
    public void onSkipSingingBowl(RewardItem reward) {
        // 歌唱碗以生命上限换掉这组牌，原版会移除奖励项。
        finishOffer(reward);
    }

    public void finishOffer(RewardItem item) {
        if (item != null && item == offer && hasOfferOnThisFloor()
                && AbstractDungeon.getCurrMapNode() != null && AbstractDungeon.getCurrRoom() == offerRoom) {
            savedOffer.finished = true;
        }
    }

    public RewardItem getBattleReward(AbstractPlayer player, AbstractRoom room, List<AbstractCard> normalChoices) {
        if (offerRoom != room) {
            if (offerRoom != null || !hasOfferOnThisFloor()) {
                savedOffer = null;
            }
            offerRoom = room;
            offer = null;
        }
        if (savedOffer != null) {
            if (savedOffer.finished) {
                return null;
            }
            if (offer == null) {
                ArrayList<AbstractCard> choices = new ArrayList<>();
                for (CardSave save : savedOffer.cards) {
                    AbstractCard card = CardLibrary.getCard(save.id);
                    if (card != null && isRewardCard(card)) {
                        choices.add(CardLibrary.getCopy(save.id, save.upgrades, save.misc));
                    }
                }
                offer = makeReward(choices, room);
            }
            return offer;
        }
        if (normalChoices == null || normalChoices.isEmpty()) {
            return null;
        }
        offer = makeReward(rollChoices(player, normalChoices), room);
        if (offer != null) {
            savedOffer = new SavedOffer(offer);
            flash();
        }
        return offer;
    }

    private RewardItem makeReward(List<AbstractCard> choices, AbstractRoom room) {
        if (choices.isEmpty()) {
            return null;
        }
        RewardItem reward = StSLib.generateCardReward(choices, room instanceof MonsterRoomBoss);
        reward.text = DESCRIPTIONS[1];
        return reward;
    }

    private List<AbstractCard> rollChoices(AbstractPlayer player, List<AbstractCard> normalChoices) {
        List<AbstractCard> pool = new ArrayList<>();
        boolean allUnlocked = Settings.treatEverythingAsUnlocked();
        for (AbstractCard card : CardLibrary.getAllCards()) {
            if (isRewardCard(card) && (allUnlocked || !UnlockTracker.isCardLocked(card.cardID))) {
                pool.add(card);
            }
        }
        // 用本局种子和楼层独立选牌，不消耗原版奖励的随机数。
        pool.sort(Comparator.comparing(card -> card.cardID));
        long runSeed = Settings.seed == null ? 0L : Settings.seed;
        Random random = new Random(runSeed ^ (AbstractDungeon.floorNum * 0x9e3779b97f4a7c15L)
                ^ 0x43e69be5b8144029L);
        List<AbstractCard> choices = new ArrayList<>();
        for (AbstractCard slot : normalChoices) {
            if (pool.isEmpty()) {
                break;
            }
            List<AbstractCard> matching = new ArrayList<>();
            for (AbstractCard card : pool) {
                if (card.rarity == slot.rarity) {
                    matching.add(card);
                }
            }
            List<AbstractCard> candidates = matching.isEmpty() ? pool : matching;
            AbstractCard chosen = candidates.get(random.nextInt(candidates.size()));
            pool.remove(chosen);
            AbstractCard copy = chosen.makeCopy();
            if (slot.upgraded && copy.canUpgrade()) {
                copy.upgrade();
            }
            for (AbstractRelic relic : player.relics) {
                relic.onPreviewObtainCard(copy);
            }
            choices.add(copy);
        }
        return choices;
    }

    private static boolean isRewardCard(AbstractCard card) {
        return card.color == AutomatonChar.Enums.BRONZE_AUTOMATON
                && (card.type == AbstractCard.CardType.ATTACK || card.type == AbstractCard.CardType.SKILL
                        || card.type == AbstractCard.CardType.POWER)
                && (card.rarity == AbstractCard.CardRarity.COMMON || card.rarity == AbstractCard.CardRarity.UNCOMMON
                        || card.rarity == AbstractCard.CardRarity.RARE);
    }
}
