package wizardrobe.relics;

import basemod.abstracts.CustomRelic;
import downfall.util.TextureLoader;

/** 联机房主可直接配置；不作为随机掉落。 */
abstract class LobbyRareRelic extends CustomRelic {
    private static final String ART = "wizardrobe/images/relics/";

    protected LobbyRareRelic(String id, String artName) {
        this(id, ART + artName + ".png", ART + artName + "Outline.png");
    }

    protected LobbyRareRelic(String id, String imagePath, String outlinePath) {
        super(id, TextureLoader.getTexture(imagePath), TextureLoader.getTexture(outlinePath),
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
