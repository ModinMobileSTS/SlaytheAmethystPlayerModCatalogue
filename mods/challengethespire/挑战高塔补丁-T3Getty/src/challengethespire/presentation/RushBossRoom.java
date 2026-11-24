package challengethespire.presentation;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.helpers.MonsterHelper;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoomBoss;

/**
 * Boss Rush encounter room that is a real {@link MonsterRoomBoss}.
 *
 * <p>The parent mod's MonsterRoomBossRush extends the ordinary MonsterRoom, so the vanilla
 * {@code instanceof MonsterRoomBoss} checks never fire. That suppresses the boss battle music,
 * the boss background variations, the ambiance fade and the boss presentation cues. This class
 * keeps the parent mod's encounter and relic rewards while restoring the boss room type, and pays
 * out a flat {@value #RUSH_GOLD} gold per room instead of the boss gold plus the mod's bonus.</p>
 */
public class RushBossRoom extends MonsterRoomBoss {
    /** Gold a rush room pays out; the vanilla boss amount plus the parent mod's bonus was ~200. */
    private static final int RUSH_GOLD = 100;

    private final String encounterID;

    public RushBossRoom(String encounterID) {
        this.encounterID = encounterID;
    }

    @Override
    public void onPlayerEntry() {
        // Vanilla boss rooms silence the exploration track; the boss's pre-battle action then
        // starts the act-specific boss music.
        CardCrawlGame.music.silenceBGM();
        if (this.monsters != null && this.monsters.monsters != null && !this.monsters.monsters.isEmpty()) {
            AbstractRoom.waitTimer = 0.1f;
            return;
        }
        this.monsters = MonsterHelper.getEncounter(this.encounterID);
        AbstractDungeon.lastCombatMetricKey = this.encounterID;
        if (this.monsters != null) {
            this.monsters.init();
        }
        AbstractRoom.waitTimer = 0.1f;
    }

    @Override
    public void dropReward() {
        super.dropReward();
        for (RewardItem r : this.rewards) {
            if (r.type != RewardItem.RewardType.GOLD) {
                continue;
            }
            // The game pays 100 +-5 gold for a boss room (75 on ascension 13+) and the parent mod
            // added another 100 on top of it, so a rush room paid roughly 200 gold. Flatten the
            // reward to RUSH_GOLD instead; the gold bonus modifiers are recalculated from it.
            r.incrementGold(RUSH_GOLD - r.goldAmt);
        }
        // The parent mod's MonsterRoomBossRush is not a MonsterRoomBoss, so with Vintage
        // enabled it also receives the extra reward relic from MonsterRoom.dropReward().
        // Mirror that here despite the boss room type used for the presentation.
        if (ModHelper.isModEnabled("Vintage")) {
            this.addRelicToRewards(this.returnRandomRelicTier());
        }
        this.addRelicToRewards(this.returnRandomRelicTier());
    }

    private AbstractRelic.RelicTier returnRandomRelicTier() {
        int roll = AbstractDungeon.relicRng.random(0, 99);
        if (roll < 50) {
            return AbstractRelic.RelicTier.COMMON;
        }
        if (roll > 82) {
            return AbstractRelic.RelicTier.RARE;
        }
        return AbstractRelic.RelicTier.UNCOMMON;
    }

    // Keep the parent mod's normal card rarity instead of MonsterRoomBoss's guaranteed rare.
    @Override
    public AbstractCard.CardRarity getCardRarity(int roll) {
        return this.getCardRarity(roll, true);
    }
}
