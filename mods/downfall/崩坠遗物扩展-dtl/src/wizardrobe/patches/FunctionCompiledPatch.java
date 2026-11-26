package wizardrobe.patches;

import automaton.FunctionHelper;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import wizardrobe.relics.CompileDirective;

@SpirePatch2(clz = FunctionHelper.class, method = "output", paramtypez = {})
public final class FunctionCompiledPatch {
    private FunctionCompiledPatch() {}

    @SpirePostfixPatch
    public static void Postfix() {
        // output() 对应一次编译；makeFunction() 也用于预览和同一次编译的额外副本。
        AbstractPlayer player = AbstractDungeon.player;
        if (player != null) {
            AbstractRelic relic = player.getRelic(CompileDirective.ID);
            if (relic instanceof CompileDirective) {
                ((CompileDirective) relic).onFunctionCompiled(player);
            }
        }
    }
}
