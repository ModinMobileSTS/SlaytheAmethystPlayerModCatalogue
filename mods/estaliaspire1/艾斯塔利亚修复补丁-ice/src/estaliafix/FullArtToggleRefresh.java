package estaliafix;

import basemod.ReflectionHacks;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import java.util.ArrayList;

/**
 * 在百科里按 F6 切换异画后，卡面要重启游戏才变化的问题。
 *
 * 百科里的卡走 AbstractCard.renderInLibrary，它不调用 card.render()，
 * 因此父模组 Card.refreshArtwork() 永远不会因为切换而执行；卡上缓存的
 * fullArtwork / portraitImg / textureBackgroundSmallImg 一直是启动时的旧值。
 * 父模组切换成功后会调用 Mod.save()，这里在其后统一刷新所有已注册的卡，
 * 使异画开启与切回普通卡面都能当帧生效，不必重启。
 */
public class FullArtToggleRefresh {

    @SpirePatch2(clz = estalia.Mod.class, method = "save", paramtypez = {})
    public static class AfterSave {
        @SpirePostfixPatch
        public static void postfix() {
            try {
                ArrayList<AbstractCard> cards = new ArrayList<AbstractCard>(CardLibrary.cards.values());
                for (AbstractCard card : cards) {
                    refresh(card);
                }
                if (AbstractDungeon.player != null) {
                    refreshCards(AbstractDungeon.player.masterDeck.group);
                    refreshCards(AbstractDungeon.player.hand.group);
                    refreshCards(AbstractDungeon.player.drawPile.group);
                    refreshCards(AbstractDungeon.player.discardPile.group);
                    refreshCards(AbstractDungeon.player.exhaustPile.group);
                }
            } catch (Throwable t) {
                log("切换异画后刷新卡面失败: " + t);
            }
        }
    }

    private static void refreshCards(ArrayList<AbstractCard> group) {
        if (group == null) {
            return;
        }
        for (AbstractCard card : new ArrayList<AbstractCard>(group)) {
            refresh(card);
        }
    }

    private static void refresh(AbstractCard card) {
        try {
            if (card instanceof estalia.Card) {
                ReflectionHacks.privateMethod(estalia.Card.class, "refreshArtwork").invoke(card);
            } else if (card instanceof estalia.TechnologyCard) {
                ReflectionHacks.privateMethod(estalia.TechnologyCard.class, "refreshArtwork").invoke(card);
            }
        } catch (Throwable ignored) {
            // 单张卡刷新失败不应影响其它卡
        }
    }

    private static void log(String msg) {
        System.out.println("[EstaliaFix] " + msg);
    }
}
