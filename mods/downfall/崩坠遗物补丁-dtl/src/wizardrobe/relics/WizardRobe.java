package wizardrobe.relics;

import basemod.abstracts.CustomRelic;
import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import gremlin.powers.WizPower;
import wizardrobe.WizardRobeMod;

/**
 * 巫师长袍 / Wizard's Robe
 *
 * At the start of each turn, gain 3 stacks of Downfall's Mana power (Gremlin:Wiz).
 * This is the exact same power the Gremlin Wizard uses, so all existing interactions
 * (Wizard Staff, Wizard Hat, the "Wiz" keyword) apply: with 3+ stacks your next Attack
 * consumes all stacks for +7 damage (+14 with Wizard Staff).
 */
public class WizardRobe extends CustomRelic {

    public static final String ID = WizardRobeMod.makeID("WizardRobe");

    /** Stacks of Gremlin:Wiz granted at the start of every turn. */
    public static final int MANA_PER_TURN = 3;

    // Art is taken from Downfall itself (it is a required dependency), so this mod ships
    // no binary assets and cannot break on a missing texture in our own jar.
    private static final String IMG = "downfallResources/images/relics/CloakOfManyFaces.png";
    private static final String OUTLINE = "downfallResources/images/relics/Outline/CloakOfManyFaces.png";

    public WizardRobe() {
        super(ID, new Texture(IMG), new Texture(OUTLINE), RelicTier.RARE, LandingSound.MAGICAL);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    /**
     * Relic turn-start pass, which the game runs at the beginning of every player turn,
     * first turn included. Same ApplyPowerAction pattern Downfall itself uses for mana
     * (see gremlin.relics.MagicalMallet and gremlin.powers.WizardryPower).
     */
    @Override
    public void atTurnStart() {
        AbstractPlayer p = AbstractDungeon.player;
        if (p == null) {
            return;
        }
        this.flash();
        this.addToBot(new ApplyPowerAction(p, p, new WizPower(p, MANA_PER_TURN), MANA_PER_TURN));
    }

    @Override
    public boolean canSpawn() {
        return false;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new WizardRobe();
    }
}