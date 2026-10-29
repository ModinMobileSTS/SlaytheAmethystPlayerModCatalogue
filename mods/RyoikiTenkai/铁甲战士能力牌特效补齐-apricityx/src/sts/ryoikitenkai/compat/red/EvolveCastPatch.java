package sts.ryoikitenkai.compat.red;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.cards.red.Evolve;
import com.megacrit.cardcrawl.characters.AbstractPlayer;

/**
 * 「进化」施放：芽从背后抽出、逐股长出、芽尖绽出光苞抛向抽牌堆。
 * <p>
 * 原版 {@code Evolve.use()} 里只有一句 {@code ApplyPowerAction}，
 * <b>没有任何 VFX</b>，所以这一段是补上的缺口。
 * <p>
 * <b>为什么挂 prefix 也能拿到正确的层数</b>：这里不读 {@code owner.getPower(...)}
 * （此刻能力还没挂上，一定返回 null），而是把 {@code card} 传下去，
 * 由 {@link EvolveFx#onCast} 读 {@code card.magicNumber}。
 * 这与观者那边「先见之明 / 研习 / 涅槃 / 确立基础」是同一处理。
 */
@SpirePatch2(
    cls = "com.megacrit.cardcrawl.cards.red.Evolve",
    method = "use",
    paramtypez = { AbstractPlayer.class,
                   com.megacrit.cardcrawl.monsters.AbstractMonster.class }
)
public final class EvolveCastPatch {
    private EvolveCastPatch() {
    }

    @SpirePrefixPatch
    public static void Prefix(Evolve __instance, Object[] __args) {
        AbstractPlayer p = __args[0] instanceof AbstractPlayer
                ? (AbstractPlayer) __args[0] : null;
        if (p == null) {
            return;
        }
        RedEffectRegistry.castEvolve(p, __instance);
    }
}
