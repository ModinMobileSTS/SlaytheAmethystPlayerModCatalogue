package spiceoverhaul;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.RestRoom;
import com.megacrit.cardcrawl.ui.campfire.AbstractCampfireOption;

/** Campfire option: trade one carried potion for a stack of Spice. */
public class SpiceTradeOption extends AbstractCampfireOption {
    private final AbstractPotion potion;
    private final int slot;

    public SpiceTradeOption(AbstractPotion potion, int slot) {
        this.potion = potion;
        this.slot = slot;
        this.label = potion.name;
        this.description = SpiceData.tradeDescription(potion.rarity);
        this.img = ImageMaster.CAMPFIRE_TOKE_BUTTON;
        this.usable = true;
    }

    private boolean stillValid() {
        AbstractPlayer p = AbstractDungeon.player;
        return p != null
                && SpiceData.owned() != null
                && this.slot >= 0
                && this.slot < p.potions.size()
                && p.potions.get(this.slot) == this.potion;
    }

    @Override
    public void update() {
        this.usable = stillValid();
        super.update();
    }

    @Override
    public void useOption() {
        if (!stillValid()) {
            return;
        }
        AbstractPlayer p = AbstractDungeon.player;
        int chance = SpiceData.extraRelicChance(this.potion.rarity);
        p.removePotion(this.potion);
        p.adjustPotionPositions();
        CardCrawlGame.sound.play("POTION_1");
        SpiceData.gainLayer();
        if (AbstractDungeon.miscRng.random(0, 99) < chance) {
            grantRandomRelic();
        }
        finishCampfire();
    }

    private static void grantRandomRelic() {
        AbstractRelic.RelicTier tier = AbstractDungeon.returnRandomRelicTier();
        AbstractRelic relic = AbstractDungeon.returnRandomRelic(tier);
        if (relic == null) {
            return;
        }
        AbstractRoom room = AbstractDungeon.getCurrRoom();
        if (room == null) {
            relic.instantObtain();
        } else {
            room.spawnRelicAndObtain(Settings.WIDTH * 0.5f, Settings.HEIGHT * 0.5f, relic);
        }
    }

    private static void finishCampfire() {
        AbstractRoom room = AbstractDungeon.getCurrRoom();
        if (room == null) {
            return;
        }
        AbstractDungeon.overlayMenu.proceedButton.hide();
        if (room instanceof RestRoom) {
            RestRoom rest = (RestRoom) room;
            try {
                rest.cutFireSound();
            } catch (Throwable ignored) {
            }
            rest.fadeIn();
        }
        AbstractRoom.waitTimer = 0.0f;
        room.phase = AbstractRoom.RoomPhase.COMPLETE;
    }
}
