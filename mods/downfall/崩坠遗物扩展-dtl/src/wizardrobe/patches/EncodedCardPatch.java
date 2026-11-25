package wizardrobe.patches;

import automaton.FunctionHelper;
import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import wizardrobe.relics.PastLifeMemory;

/** 只在成功写入编码队列时触发；方法预览、生成、或等待空位均不触发。 */
@SpirePatch2(clz = FunctionHelper.class, method = "addToSequence", paramtypez = {AbstractCard.class})
public final class EncodedCardPatch {
    private EncodedCardPatch() {}

    @SpireInstrumentPatch
    public static ExprEditor instrument() {
        return new ExprEditor() {
            @Override
            public void edit(MethodCall call) throws CannotCompileException {
                if (CardGroup.class.getName().equals(call.getClassName()) && "addToTop".equals(call.getMethodName())) {
                    call.replace("{ int sizeBefore = $0.size(); $proceed($$); "
                            + "if ($0.size() > sizeBefore) " + EncodedCardPatch.class.getName()
                            + ".onInserted(); }");
                }
            }
        };
    }

    public static void onInserted() {
        AbstractPlayer player = AbstractDungeon.player;
        if (player != null) {
            AbstractRelic relic = player.getRelic(PastLifeMemory.ID);
            if (relic instanceof PastLifeMemory) {
                ((PastLifeMemory) relic).onCardEncoded();
            }
        }
    }
}
