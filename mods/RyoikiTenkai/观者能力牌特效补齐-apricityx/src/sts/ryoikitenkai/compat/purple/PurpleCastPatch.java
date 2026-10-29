package sts.ryoikitenkai.compat.purple;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

/**
 * 观者能力牌的施放视觉。
 * <p>
 * 不逐张去补丁十二张牌的 use，而是挂在 {@code AbstractPlayer.useCard} 上按 cardID 派发：
 * 钩子只有一处，卡牌实例与目标怪物都能直接拿到（战歌的惩击需要目标位置来画音符攻击）。
 * 非本补丁关心的牌在登记表里会立刻返回。
 */
@SpirePatch2(
    cls = "com.megacrit.cardcrawl.characters.AbstractPlayer",
    method = "useCard",
    paramtypez = {
        com.megacrit.cardcrawl.cards.AbstractCard.class,
        com.megacrit.cardcrawl.monsters.AbstractMonster.class,
        int.class
    }
)
public final class PurpleCastPatch {
    private PurpleCastPatch() {
    }

    @SpirePrefixPatch
    public static void Prefix(AbstractPlayer __instance, Object[] __args) {
        AbstractCard card = __args[0] instanceof AbstractCard ? (AbstractCard) __args[0] : null;
        AbstractMonster target = __args[1] instanceof AbstractMonster
                ? (AbstractMonster) __args[1] : null;
        PurpleEffectRegistry.cast(__instance, card, target);
    }
}
