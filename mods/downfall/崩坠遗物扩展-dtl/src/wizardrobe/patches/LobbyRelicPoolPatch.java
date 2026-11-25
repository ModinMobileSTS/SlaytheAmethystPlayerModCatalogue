package wizardrobe.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import wizardrobe.relics.KnowledgeBase;
import wizardrobe.relics.PastLifeMemory;

import java.util.ArrayList;

/** 保留遗物图鉴和联机开局配置中的共享注册，只剔除随机池条目。 */
@SpirePatch2(clz = RelicLibrary.class, method = "populateRelicPool",
        paramtypez = {ArrayList.class, AbstractRelic.RelicTier.class, AbstractPlayer.PlayerClass.class})
public final class LobbyRelicPoolPatch {
    private LobbyRelicPoolPatch() {}

    @SpirePostfixPatch
    public static void Postfix(Object[] __args) {
        ArrayList<?> pool = (ArrayList<?>) __args[0];
        pool.removeIf(id -> KnowledgeBase.ID.equals(id) || PastLifeMemory.ID.equals(id));
    }
}
