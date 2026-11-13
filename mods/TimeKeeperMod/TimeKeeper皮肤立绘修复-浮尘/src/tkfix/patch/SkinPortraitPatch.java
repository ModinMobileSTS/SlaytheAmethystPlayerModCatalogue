package tkfix.patch;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MenuCancelButton;
import timekeepermod.patch.SkinSelectPatch_mic;
import timekeepermod.patch.SkinSelectScreen_green;
import timekeepermod.patch.SkinSelectScreen_mic;
import timekeepermod.patch.SkinSelectScreen_red;
import tkfix.Diag;
import tkfix.SkinFixConfig;

/**
 * 母模组把自己皮肤的整屏立绘插在 CharacterSelectScreen.render 的立绘分支里，
 * 结果只在「非常规比例」分支执行，而且执行位置在背景立绘之前，被完全盖住；
 * 手机常见的 letterbox 比例下更是根本不执行，所以皮肤立绘一直看不到。
 * <p>
 * 这里改为在选人界面「返回」按钮绘制前一刻（背景立绘已画完，按钮与角色选项还没画）
 * 绘制皮肤立绘，图层顺序与官方立绘一致。
 */
@SpirePatch2(clz = MenuCancelButton.class, method = "render", paramtypez = {SpriteBatch.class})
public class SkinPortraitPatch {

    @SpirePrefixPatch
    public static void Prefix(MenuCancelButton __instance, Object[] __args) {
        try {
            MainMenuScreen menu = CardCrawlGame.mainMenuScreen;
            if (menu == null) {
                Diag.log("css", "no main menu");
                return;
            }
            CharacterSelectScreen screen = menu.charSelectScreen;
            if (screen == null) {
                Diag.log("css", "no char select screen");
                return;
            }
            boolean charSelectScreen = menu.screen == MainMenuScreen.CurScreen.CHAR_SELECT;
            boolean ourButton = screen.cancelButton == __instance;
            AbstractPlayer.PlayerClass chosen = CardCrawlGame.chosenCharacter;
            boolean anySelected = anySelected(screen);
            if (!charSelectScreen && !ourButton) {
                Diag.log("css", "skip screen=" + menu.screen + " sameButton=" + ourButton);
                return;
            }
            Diag.log("css", "screen=" + menu.screen + " chosen=" + chosen + " any=" + anySelected
                    + " sameButton=" + ourButton
                    + " red=" + SkinFixConfig.indexRed() + " green=" + SkinFixConfig.indexGreen());
            if (!anySelected || chosen == null) {
                return;
            }
            SpriteBatch sb = (SpriteBatch) __args[0];
            sb.setColor(Color.WHITE);
            if (chosen == AbstractPlayer.PlayerClass.IRONCLAD) {
                if (SkinSelectScreen_red.Inst != null) {
                    SkinSelectScreen_red.Inst.renderPortrait_red(sb);
                    Diag.log("cssDraw", "ironclad idx=" + SkinFixConfig.indexRed()
                            + " tex=" + (texNullRed() ? "null" : "ok"));
                } else {
                    Diag.log("cssDraw", "ironclad Inst=null");
                }
            } else if (chosen == AbstractPlayer.PlayerClass.THE_SILENT) {
                if (SkinSelectScreen_green.Inst != null) {
                    SkinSelectScreen_green.Inst.renderPortrait_Green(sb);
                    Diag.log("cssDraw", "silent idx=" + SkinFixConfig.indexGreen());
                } else {
                    Diag.log("cssDraw", "silent Inst=null");
                }
            } else if (isTimeKeeper()) {
                if (SkinSelectScreen_mic.Inst != null) {
                    SkinSelectScreen_mic.Inst.renderPortrait(sb);
                    Diag.log("cssDraw", "timekeeper");
                }
            } else {
                Diag.log("cssDraw", "other chosen=" + chosen);
            }
        } catch (Throwable t) {
            Diag.error("cssDraw", t);
        }
    }

    private static boolean texNullRed() {
        try {
            return SkinSelectScreen_red.getSkin_red().portrait_IMG == null;
        } catch (Throwable t) {
            Diag.error("texNullRed", t);
            return true;
        }
    }

    private static boolean anySelected(CharacterSelectScreen screen) {
        try {
            Object v = ReflectionHacks.getPrivate(screen, CharacterSelectScreen.class, "anySelected");
            return v instanceof Boolean && (Boolean) v;
        } catch (Throwable t) {
            Diag.error("anySelected", t);
            return false;
        }
    }

    private static boolean isTimeKeeper() {
        try {
            return SkinSelectPatch_mic.isTKSelected();
        } catch (Throwable t) {
            Diag.error("isTK", t);
            return false;
        }
    }
}
