package sts.ryoikitenkai.compat.red;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.red.Metallicize;
import com.megacrit.cardcrawl.characters.AbstractPlayer;

/**
 * 「金属化」施放：甲片逐片飞来、逐片锁上、接缝扫亮。
 * <p>
 * 原版 {@code Metallicize.use()} 里只有一句 {@code ApplyPowerAction}，
 * <b>没有任何 VFX</b>，所以这一段是补上的缺口。
 * <p>
 * <b>为什么必须把 card 传下去</b>：prefix 阶段能力还没挂上，
 * {@code owner.getPower("Metallicize")} 一定返回 null，
 * 甲片数只能从 {@code card.magicNumber} 读（原版 3 / 升级 4）。
 * 这与观者那几张、以及本补丁的「进化」是同一处理。
 * <p>
 * 音效不在这里播：{@code MetallicizePower.playApplyPowerSfx()} 已经播
 * {@code POWER_METALLICIZE}，再补一个会双响。
 */
@SpirePatch2(
    cls = "com.megacrit.cardcrawl.cards.red.Metallicize",
    method = "use",
    paramtypez = { AbstractPlayer.class,
                   com.megacrit.cardcrawl.monsters.AbstractMonster.class }
)
public final class MetallicizeCastPatch {
    private MetallicizeCastPatch() {
    }

    @SpirePrefixPatch
    public static void Prefix(Metallicize __instance, Object[] __args) {
        AbstractPlayer p = __args[0] instanceof AbstractPlayer
                ? (AbstractPlayer) __args[0] : null;
        if (p == null) {
            return;
        }
        AbstractCard card = __instance;
        RedEffectRegistry.castMetallicize(p, card);
    }
}
