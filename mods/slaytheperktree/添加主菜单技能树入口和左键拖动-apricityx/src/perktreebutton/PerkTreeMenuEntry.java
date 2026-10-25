package perktreebutton;

import basemod.BaseMod;
import basemod.ReflectionHacks;
import basemod.interfaces.PostUpdateSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MenuButton;
import ironcladperks.IroncladPerksMod;
import ironcladperks.perks.Localization;
import ironcladperks.perks.PerkData;
import ironcladperks.perks.PerkRegistry;
import ironcladperks.screens.PerkTreeScreen;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Slay the Perk Tree main menu entry: a "skill tree" row placed directly below the vanilla
 * resume / abandon-run / start rows, plus left-button drag panning inside the skill tree screen.
 *
 * <p>Vanilla stacks the main menu rows bottom-up from {@code MenuButton.START_Y}, so the start block
 * owns the highest row indices; the extra row is inserted beneath it instead of on top of the menu.
 */
@SpireInitializer
public class PerkTreeMenuEntry implements PostUpdateSubscriber {
    private static final Logger logger = LogManager.getLogger(PerkTreeMenuEntry.class.getName());
    private static final String LABEL_FALLBACK = "Skill Tree";

    private static final float DRAG_SLOP = 6.0f;

    private static PerkTreeButton perkButton;
    private static long lastSignature = Long.MIN_VALUE;

    private static boolean dragging;
    private static int lastDragX;
    private static int lastDragY;
    private static boolean dragMoved;

    public PerkTreeMenuEntry() {
        BaseMod.subscribe(this);
    }

    public static void initialize() {
        new PerkTreeMenuEntry();
        logger.info("PerkTreeMenuEntry: initialized");
    }

    static String entryLabel() {
        try {
            return Localization.ui("SCREEN_TITLE", LABEL_FALLBACK);
        } catch (Throwable t) {
            return LABEL_FALLBACK;
        }
    }

    // ---------------------------------------------------------------- main menu row

    @SuppressWarnings("unchecked")
    private static ArrayList<MenuButton> buttons(MainMenuScreen menu) {
        Object value = menu.buttons;
        return value instanceof ArrayList ? (ArrayList<MenuButton>) value : null;
    }

    private static int indexOf(MenuButton button) {
        Object value = ReflectionHacks.getPrivate(button, MenuButton.class, "index");
        return value instanceof Integer ? ((Integer) value).intValue() : 0;
    }

    private static void setIndex(MenuButton button, int index) {
        ReflectionHacks.setPrivate(button, MenuButton.class, "index", index);
    }

    private static boolean isStartRow(MenuButton button) {
        if (button == null || button.result == null) {
            return false;
        }
        return button.result == MenuButton.ClickResult.PLAY
                || button.result == MenuButton.ClickResult.RESUME_GAME
                || button.result == MenuButton.ClickResult.ABANDON_RUN;
    }

    private static boolean isPresent(ArrayList<MenuButton> buttons) {
        if (perkButton == null) {
            return false;
        }
        for (MenuButton existing : buttons) {
            if (existing == perkButton) {
                return true;
            }
        }
        return false;
    }

    private static void refreshLabel() {
        if (perkButton == null) {
            return;
        }
        Object current = ReflectionHacks.getPrivate(perkButton, MenuButton.class, "label");
        String expected = entryLabel();
        if (current instanceof String && expected.equals(current)) {
            return;
        }
        ReflectionHacks.setPrivate(perkButton, MenuButton.class, "label", expected);
    }

    private static long signature(ArrayList<MenuButton> buttons) {
        long hash = 1125899906842597L;
        for (MenuButton button : buttons) {
            if (button == null) {
                continue;
            }
            hash = hash * 31L + (long) System.identityHashCode(button);
            hash = hash * 31L + (long) indexOf(button);
        }
        return hash;
    }

    private static ArrayList<MenuButton> sortedOthers(ArrayList<MenuButton> buttons) {
        ArrayList<MenuButton> ordered = new ArrayList<MenuButton>();
        for (MenuButton button : buttons) {
            if (button == null || button == perkButton) {
                continue;
            }
            ordered.add(button);
        }
        Collections.sort(ordered, new Comparator<MenuButton>() {
            @Override
            public int compare(MenuButton left, MenuButton right) {
                return Integer.compare(indexOf(left), indexOf(right));
            }
        });
        return ordered;
    }

    /** Row slot of the skill tree entry: right below the start / resume / abandon-run block. */
    private static int desiredSlot(ArrayList<MenuButton> others) {
        int startIndex = -1;
        for (MenuButton button : others) {
            if (!isStartRow(button)) {
                continue;
            }
            int index = indexOf(button);
            if (startIndex < 0 || index < startIndex) {
                startIndex = index;
            }
        }
        if (startIndex < 0) {
            return others.size();
        }
        int below = 0;
        for (MenuButton button : others) {
            if (indexOf(button) < startIndex) {
                below++;
            }
        }
        return below;
    }

    private static float rowStep() {
        return Settings.isTouchScreen || Settings.isMobile ? MenuButton.SPACE_Y * 2.0f : MenuButton.SPACE_Y;
    }

    /** Re-flows the rows so they stay contiguous and the skill tree row sits below the start block. */
    private static void relayout(ArrayList<MenuButton> buttons) {
        if (perkButton == null) {
            return;
        }
        ArrayList<MenuButton> others = sortedOthers(buttons);
        if (others.isEmpty()) {
            return;
        }
        for (MenuButton button : others) {
            if (button.hb == null || button.hb.cY < 0.0f) {
                // another mod parked the rows (menu pagination); leave that layout alone
                return;
            }
        }
        MenuButton reference = others.get(0);
        int referenceIndex = indexOf(reference);
        float referenceY = reference.hb.cY;
        float step = rowStep();

        ArrayList<MenuButton> ordered = new ArrayList<MenuButton>(others);
        int slot = Math.max(0, Math.min(desiredSlot(others), ordered.size()));
        ordered.add(slot, perkButton);

        for (int i = 0; i < ordered.size(); i++) {
            MenuButton button = ordered.get(i);
            setIndex(button, i);
            if (button.hb != null) {
                button.hb.moveY(referenceY + (float) (i - referenceIndex) * step);
            }
        }
    }

    private static int positionBeforeStartBlock(ArrayList<MenuButton> buttons) {
        int anchorIndex = -1;
        int position = -1;
        for (int i = 0; i < buttons.size(); i++) {
            MenuButton button = buttons.get(i);
            if (!isStartRow(button)) {
                continue;
            }
            int index = indexOf(button);
            if (anchorIndex < 0 || index < anchorIndex) {
                anchorIndex = index;
                position = i;
            }
        }
        return position < 0 ? buttons.size() : position;
    }

    private static void ensureButton(MainMenuScreen menu) {
        ArrayList<MenuButton> buttons = buttons(menu);
        if (buttons == null || buttons.isEmpty()) {
            return;
        }
        if (!isPresent(buttons)) {
            try {
                // Insert before the start block instead of appending: vanilla drops the last two list
                // entries when a run is abandoned, which would otherwise delete this row.
                int position = positionBeforeStartBlock(buttons);
                int index = desiredSlot(sortedOthers(buttons));
                PerkTreeButton created = new PerkTreeButton(index);
                buttons.add(position, created);
                perkButton = created;
                relayout(buttons);
                lastSignature = signature(buttons);
                logger.info("PerkTreeMenuEntry: main menu entry added below the start / abandon rows");
            } catch (Throwable t) {
                logger.error("PerkTreeMenuEntry: failed to add the main menu entry", t);
            }
            return;
        }
        refreshLabel();
        long current = signature(buttons);
        if (current == lastSignature) {
            return;
        }
        lastSignature = current;
        if (indexOf(perkButton) == desiredSlot(sortedOthers(buttons))) {
            return;
        }
        try {
            relayout(buttons);
        } catch (Throwable t) {
            logger.error("PerkTreeMenuEntry: failed to move the main menu entry", t);
        }
        lastSignature = signature(buttons);
    }

    // ---------------------------------------------------------------- left-button panning

    private static PerkTreeScreen openScreen() {
        try {
            Object value = ReflectionHacks.getPrivateStatic(IroncladPerksMod.class, "skillTreeScreen");
            return value instanceof PerkTreeScreen && ((PerkTreeScreen) value).isOpen ? (PerkTreeScreen) value : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static float camera(Object screen, String field) {
        Object value = ReflectionHacks.getPrivate(screen, PerkTreeScreen.class, field);
        return value instanceof Float ? ((Float) value).floatValue() : 0.0f;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Hitbox> nodeHitboxes(PerkTreeScreen screen) {
        Object value = ReflectionHacks.getPrivate(screen, PerkTreeScreen.class, "nodeHitboxes");
        return value instanceof Map ? (Map<String, Hitbox>) value : null;
    }

    private static boolean overNode(PerkTreeScreen screen, int x, int y) {
        Map<String, Hitbox> hitboxes = nodeHitboxes(screen);
        if (hitboxes == null) {
            return false;
        }
        for (Hitbox hb : hitboxes.values()) {
            if (hb == null) {
                continue;
            }
            if ((float) x > hb.x && (float) x < hb.x + hb.width
                    && (float) y > hb.y && (float) y < hb.y + hb.height) {
                return true;
            }
        }
        return false;
    }

    private static void resetDrag() {
        dragging = false;
        dragMoved = false;
    }

    /**
     * The parent mod only pans the tree while the right button is held. Mirror that for the left
     * button, but never start a drag on a perk node so clicking a node keeps buying / removing ranks.
     */
    private static void updatePanning() {
        PerkTreeScreen screen = openScreen();
        if (screen == null) {
            resetDrag();
            return;
        }
        if (!InputHelper.isMouseDown) {
            resetDrag();
            return;
        }
        int x = InputHelper.mX;
        int y = InputHelper.mY;
        if (!dragging) {
            if (overNode(screen, x, y)) {
                return;
            }
            dragging = true;
            dragMoved = false;
            lastDragX = x;
            lastDragY = y;
            return;
        }
        int dx = x - lastDragX;
        int dy = y - lastDragY;
        if (dx == 0 && dy == 0) {
            return;
        }
        if (!dragMoved) {
            float slop = DRAG_SLOP * Settings.scale;
            if (Math.abs(dx) <= slop && Math.abs(dy) <= slop) {
                lastDragX = x;
                lastDragY = y;
                return;
            }
            dragMoved = true;
        }
        lastDragX = x;
        lastDragY = y;
        ReflectionHacks.setPrivate(screen, PerkTreeScreen.class, "cameraX", camera(screen, "cameraX") + (float) dx);
        ReflectionHacks.setPrivate(screen, PerkTreeScreen.class, "cameraY", camera(screen, "cameraY") + (float) dy);
    }

    @Override
    public void receivePostUpdate() {
        MainMenuScreen menu = CardCrawlGame.mainMenuScreen;
        if (menu != null) {
            ensureButton(menu);
        }
        try {
            updatePanning();
        } catch (Throwable t) {
            resetDrag();
            logger.error("PerkTreeMenuEntry: left-button panning failed", t);
        }
    }

    /** An extra main menu row that opens the perk tree screen when clicked. */
    public static class PerkTreeButton extends MenuButton {
        PerkTreeButton(int index) {
            super(MenuButton.ClickResult.INFO, index);
            ReflectionHacks.setPrivate(this, MenuButton.class, "label", entryLabel());
        }

        @Override
        public void buttonEffect() {
            MainMenuScreen menu = CardCrawlGame.mainMenuScreen;
            if (menu == null || menu.screen != MainMenuScreen.CurScreen.MAIN_MENU) {
                return;
            }
            try {
                IroncladPerksMod.openSkillTree();
            } catch (Throwable t) {
                logger.error("PerkTreeMenuEntry: failed to open the skill tree", t);
            }
        }
    }
}
