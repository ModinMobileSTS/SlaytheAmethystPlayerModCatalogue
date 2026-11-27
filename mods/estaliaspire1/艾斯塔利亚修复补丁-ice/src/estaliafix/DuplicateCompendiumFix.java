package estaliafix;

import basemod.ReflectionHacks;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MenuButton;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * 主菜单出现两个一模一样的“百科”按钮的修复。
 *
 * 父模组 TechnologyScreen.Entry 用 MenuButton.ClickResult.INFO 造了一个额外按钮
 * （本应显示为“艾丝科技”），与游戏自带的 INFO 百科按钮重复。这里把父模组按钮
 * 的标题强制改成“艾丝科技”，并按标题去掉多余的 INFO 按钮；保留 Entry 本身，
 * 所以艾丝科技面板仍然可以正常进入。
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
            MenuButton tech = null;
            for (MenuButton button : new ArrayList<MenuButton>(buttons)) {
                if (!ENTRY_CLASS.equals(button.getClass().getName())) {
                    continue;
                }
                if (!TECH_LABEL.equals(label(button))) {
                    setLabel(button, TECH_LABEL);
                }
                if (tech == null) {
                    tech = button;
                } else {
                    buttons.remove(button); // 多个科技入口只保留一个
                }
            }

            ArrayList<MenuButton> extras = new ArrayList<MenuButton>();
            HashSet<String> seen = new HashSet<String>();
            for (MenuButton button : new ArrayList<MenuButton>(buttons)) {
                if (button.result != MenuButton.ClickResult.INFO) {
                    continue;
                }
                String text = label(button);
                if (ENTRY_CLASS.equals(button.getClass().getName())) {
                    seen.add(TECH_LABEL);
                    continue;
                }
                // 与科技入口同名的重复按钮，或标题已经出现过的 INFO 按钮，去掉。
                if (TECH_LABEL.equals(text) && tech != null || !seen.add(text)) {
                    extras.add(button);
                }
            }
            if (extras.isEmpty()) {
                return;
            }
            buttons.removeAll(extras);
            int index = 0;
            for (MenuButton button : buttons) {
                if (index != buttonIndex(button)) {
                    ReflectionHacks.setPrivate(button, MenuButton.class, "index", Integer.valueOf(index));
                }
                ++index;
            }
            log("去掉了 " + extras.size() + " 个重复的 INFO 按钮，艾丝科技入口保留");
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

    private static void setLabel(MenuButton button, String text) {
        ReflectionHacks.setPrivate(button, MenuButton.class, "label", text);
    }

    private static int buttonIndex(MenuButton button) {
        Integer index = ReflectionHacks.getPrivate(button, MenuButton.class, "index");
        return index == null ? -1 : index.intValue();
    }

    private static void log(String msg) {
        System.out.println("[EstaliaFix] " + msg);
    }
}
