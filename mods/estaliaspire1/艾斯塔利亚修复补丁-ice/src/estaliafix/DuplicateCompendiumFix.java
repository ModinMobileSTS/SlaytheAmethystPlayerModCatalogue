package estaliafix;

import basemod.ReflectionHacks;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MenuButton;
import java.util.ArrayList;

/**
 * 主菜单出现两个一模一样的“百科全书/百科大全”按钮的修复。
 *
 * 原因：父模组 TechnologyScreen.MenuEntry 用 MenuButton.ClickResult.INFO 又造了一个
 * 菜单按钮（原本打算显示成“艾丝科技”），于是主菜单里出现了第二个百科按钮；
 * 它的 label 反射改名在某些环境下没有生效，就变成了两个完全相同的条目。
 *
 * 处理：把父模组附加的 Entry 按钮名字固定为“艾丝科技”，保证只剩一个百科按钮；
 * 如果仍出现多余的 INFO 按钮，也一并去重。通向艾丝科技面板的功能保持不变。
 */
public class DuplicateCompendiumFix {

    private static final String ENTRY_CLASS = "estalia.TechnologyScreen$Entry";
    private static final String TECH_LABEL = "艾丝科技";

    @SpirePatch2(clz = MainMenuScreen.class, method = "setMainMenuButtons", paramtypez = {})
    public static class AfterButtons {
        @SpirePostfixPatch
        public static void postfix(MainMenuScreen __instance) {
            fix(__instance);
        }
    }

    @SpirePatch2(clz = MainMenuScreen.class, method = "update", paramtypez = {})
    public static class AfterUpdate {
        @SpirePostfixPatch
        public static void postfix(MainMenuScreen __instance) {
            fix(__instance);
        }
    }

    private static void fix(MainMenuScreen menu) {
        try {
            ArrayList<MenuButton> buttons = menu.buttons;
            if (buttons == null) {
                return;
            }
            MenuButton firstInfo = null;
            ArrayList<MenuButton> extras = new ArrayList<MenuButton>();
            for (MenuButton button : buttons) {
                boolean entry = ENTRY_CLASS.equals(button.getClass().getName());
                if (entry && !TECH_LABEL.equals(label(button))) {
                    ReflectionHacks.setPrivate(button, MenuButton.class, "label", TECH_LABEL);
                }
                if (button.result != MenuButton.ClickResult.INFO) {
                    continue;
                }
                if (firstInfo == null) {
                    firstInfo = button;
                } else if (!entry && !TECH_LABEL.equals(label(button))) {
                    extras.add(button);
                }
            }
            if (!extras.isEmpty()) {
                buttons.removeAll(extras);
            }
        } catch (Throwable t) {
            log("主菜单按钮去重失败: " + t);
        }
    }

    private static String label(MenuButton button) {
        try {
            return ReflectionHacks.getPrivate(button, MenuButton.class, "label");
        } catch (Throwable t) {
            return null;
        }
    }

    private static void log(String msg) {
        System.out.println("[EstaliaFix] " + msg);
    }
}
