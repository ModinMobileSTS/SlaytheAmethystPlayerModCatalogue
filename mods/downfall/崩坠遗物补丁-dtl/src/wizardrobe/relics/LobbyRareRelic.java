package wizardrobe.relics;

import basemod.abstracts.CustomRelic;
import downfall.util.TextureLoader;

/** 联机房主可直接配置；不作为随机掉落。 */
abstract class LobbyRareRelic extends CustomRelic {
    private static final String ART = "wizardrobe/images/relics/";

    protected LobbyRareRelic(String id, String artName) {
        super(id, TextureLoader.getTexture(ART + artName + ".png"),
                TextureLoader.getTexture(ART + artName + "Outline.png"),
                RelicTier.RARE, LandingSound.MAGICAL);
    }

    @Override
    public final boolean canSpawn() {
        return false;
    }

    @Override
    public final String getUpdatedDescription() {
        return DESCRIPTIONS[0];
    }
}
