package heartfx;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

/**
 * 在「我夺取心脏」真正会发动斩杀时，把演出动作插到动作队列最前面。
 * 原方法不改写：牌的费用、忍术判定、蕾克拉消耗、语音与死亡动画全部沿用蕾忍mod 原逻辑，
 * 演出结束后自然衔接原流程。
 */
@SpirePatch2(
        cls = "cards.TakeYourHeart",
        method = "use",
        paramtypez = {AbstractPlayer.class, AbstractMonster.class}
)
public final class TakeHeartUsePatch {

    private TakeHeartUsePatch() {
    }

    @SpirePrefixPatch
    public static void Prefix(Object[] __args) {
        if (__args == null || __args.length < 2) {
            return;
        }
        if (!(__args[0] instanceof AbstractPlayer)) {
            return;
        }
        AbstractPlayer p = (AbstractPlayer) __args[0];
        if (!(__args[1] instanceof AbstractCreature)) {
            return;
        }
        AbstractCreature target = (AbstractCreature) __args[1];
        if (target.hasPower(HeartFx.POWER_ARTIFACT) || target.isDeadOrEscaped()) {
            return;
        }
        if (!HeartFx.willTrigger(p, HeartFx.NINJUTSU_COST)) {
            return;
        }
        AbstractDungeon.actionManager.addToTop(new TakeHeartCinematicAction(target));
    }
}
