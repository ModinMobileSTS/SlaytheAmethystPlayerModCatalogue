package sts.ryoikitenkai.compat.red;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.cards.red.Metallicize;
import com.megacrit.cardcrawl.characters.AbstractPlayer;

/**
 * 打出「金属化」时：钢蓝色的卡片轮廓闪光 + 身上护甲合拢的反馈。
 * <p>
 * 用 Prefix（不替换原方法）：卡牌本身的结算照常，这里只插一段视觉。
 * 卡牌实例传下去是为了让 {@code CardFlashVfx} 能在卡牌自己的位置画那张背景剪影。
 */
@SpirePatch2(
    cls = "com.megacrit.cardcrawl.cards.red.Metallicize",
    method = "use",
    paramtypez = { com.megacrit.cardcrawl.characters.AbstractPlayer.class, com.megacrit.cardcrawl.monsters.AbstractMonster.class }
)
public final class MetallicizeCastPatch {
    private MetallicizeCastPatch() {
    }

    @SpirePrefixPatch
    public static void Prefix(Metallicize __instance, Object[] __args) {
        RedEffectRegistry.castMetallicize((AbstractPlayer) __args[0], __instance);
    }
}
