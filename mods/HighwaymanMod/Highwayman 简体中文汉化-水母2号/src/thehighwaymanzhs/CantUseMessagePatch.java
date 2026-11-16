package thehighwaymanzhs;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

/**
 * 所有“开火”牌在弹药不足时显示的提示是硬编码在代码里的英文，这里在简体中文下替换为中文。
 *
 * <p>{@link AbstractDungeon#update()} 本身没有任何参数，所以这个补丁不需要任何注入参数：
 * 每帧结尾把手牌上仍然是英文原文的提示改成中文。提示在玩家按住手牌时一直显示，
 * 而本补丁在该帧渲染之前执行，因此玩家看到的始终是中文。</p>
 */
@SpirePatch2(clz = AbstractDungeon.class, method = "update", paramtypez = {})
public final class CantUseMessagePatch {
    private static final String ORIGINAL_MESSAGE = "I don't have any ammo to fire with.";
    private static final String CHINESE_MESSAGE = "你没有弹药，无法开火。";

    private CantUseMessagePatch() {
    }

    @SpirePostfixPatch
    public static void Postfix() {
        if (!ZhsLocalization.isChinese()) {
            return;
        }
        if (AbstractDungeon.player == null || AbstractDungeon.player.hand == null) {
            return;
        }
        // 只替换本 Mod 自己写入的那条英文提示，不影响游戏原有的其它提示。
        for (Object entry : AbstractDungeon.player.hand.group) {
            if (!(entry instanceof AbstractCard)) {
                continue;
            }
            AbstractCard card = (AbstractCard) entry;
            if (ORIGINAL_MESSAGE.equals(card.cantUseMessage)) {
                card.cantUseMessage = CHINESE_MESSAGE;
            }
        }
    }
}
