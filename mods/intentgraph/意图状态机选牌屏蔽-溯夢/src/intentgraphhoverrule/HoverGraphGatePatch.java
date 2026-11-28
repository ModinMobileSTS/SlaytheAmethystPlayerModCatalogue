package intentgraphhoverrule;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import io.chaofan.sts.intentgraph.IntentGraphMod;

/**
 * Intent Graph 在 PostRender 里只会为「当前被悬停的那一个敌人」调用本方法，
 * 是意图状态机唯一的绘制出口。在入口处按规则决定这一帧画不画：
 * 跳过本方法即同时跳过状态机绘制、未解锁占位图以及解锁记录。
 */
@SpirePatch2(clz = IntentGraphMod.class, method = "renderIntentGraphForMonster",
        paramtypez = {AbstractMonster.class, SpriteBatch.class})
public class HoverGraphGatePatch {

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(Object[] __args) {
        if (__args.length < 1 || !(__args[0] instanceof AbstractMonster)) {
            return SpireReturn.Continue();
        }
        AbstractMonster hoveredMonster = (AbstractMonster) __args[0];
        if (hoveredMonster == null || !hoveredMonster.hb.hovered) {
            return SpireReturn.Continue();
        }
        if (!IntentGraphHoverRule.isGraphAllowed(hoveredMonster)) {
            return SpireReturn.Return();
        }
        return SpireReturn.Continue();
    }
}
