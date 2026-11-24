package therailgunfix;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.helpers.GameDictionary;

@SpirePatch2(clz = com.megacrit.cardcrawl.cards.AbstractCard.class, method = "dedupeKeyword", paramtypez = {String.class})
public class CardKeywordTooltipFix {
    private static final String MOD_PREFIX = "therailgun:";

    @SpirePrefixPatch
    public static SpireReturn prefix(Object[] __args) {
        Object arg = __args[0];
        if (!(arg instanceof String)) {
            return SpireReturn.Continue();
        }
        String keyword = (String) arg;
        if (keyword.isEmpty()) {
            return SpireReturn.Continue();
        }
        String prefixed = MOD_PREFIX + keyword;
        String parent = GameDictionary.parentWord.get(prefixed);
        if (parent != null && GameDictionary.keywords.containsKey(parent)) {
            return SpireReturn.Return(parent);
        }
        return SpireReturn.Continue();
    }
}
