package kimmocazhs.patches;

import basemod.BaseMod;
import basemod.ModLabeledToggleButton;
import basemod.ModPanel;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.evacipated.cardcrawl.modthespire.lib.ByRef;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.events.GenericEventDialog;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.screens.select.GridCardSelectScreen;
import com.megacrit.cardcrawl.vfx.ThoughtBubble;
import kimmocazhs.Zh;

import java.util.function.Consumer;

/**
 * The parent mod hardcodes Korean text in a handful of Java methods instead
 * of localization files. These patches translate those strings at the point
 * where they are consumed.
 *
 * Game-jar methods have no LocalVariableTable, so @SpirePatch2 cannot match
 * named parameters there. Every patch that rewrites an argument therefore
 * uses the positional v1 @SpirePatch ($0 = __instance, $1.. = arguments).
 */
public final class ZhPatches {
    private ZhPatches() {
    }

    // ------------------------------------------------------------------
    // No-argument hooks: @SpirePatch2 with __instance is safe.
    // ------------------------------------------------------------------

    /** Card names (shop "card acquisition" cards) and appended description fragments. */
    @SpirePatch2(clz = AbstractCard.class, method = "initializeDescription")
    public static class CardText {
        @SpirePrefixPatch
        public static void Prefix(AbstractCard __instance) {
            __instance.name = Zh.fix(__instance.name);
            __instance.rawDescription = Zh.fix(__instance.rawDescription);
        }
    }

    @SpirePatch2(clz = KimMoca.powers.Cheese.class, method = "updateDescription")
    public static class CheeseText {
        @SpirePostfixPatch
        public static void Postfix(AbstractPower __instance) {
            __instance.description = Zh.fix(__instance.description);
        }
    }

    @SpirePatch2(clz = KimMoca.powers.Reflux.class, method = "updateDescription")
    public static class RefluxText {
        @SpirePostfixPatch
        public static void Postfix(AbstractPower __instance) {
            __instance.description = Zh.fix(__instance.description);
        }
    }

    @SpirePatch2(clz = KimMoca.powers.Streamer_NaongBbang_Power.class, method = "updateDescription")
    public static class NaongPowerText {
        @SpirePostfixPatch
        public static void Postfix(AbstractPower __instance) {
            __instance.description = Zh.fix(__instance.description);
        }
    }

    // ------------------------------------------------------------------
    // Argument-rewriting hooks: positional v1 @SpirePatch.
    // ------------------------------------------------------------------

    @SpirePatch(clz = GenericEventDialog.class, method = "setDialogOption",
            paramtypez = {String.class})
    public static class EventSetOption {
        @SpirePrefixPatch
        public static void Prefix(GenericEventDialog __instance, @ByRef String[] text) {
            text[0] = Zh.fix(text[0]);
        }
    }

    @SpirePatch(clz = GenericEventDialog.class, method = "setDialogOption",
            paramtypez = {String.class, boolean.class})
    public static class EventSetOptionDisabled {
        @SpirePrefixPatch
        public static void Prefix(GenericEventDialog __instance, @ByRef String[] text, boolean disabled) {
            text[0] = Zh.fix(text[0]);
        }
    }

    @SpirePatch(clz = GenericEventDialog.class, method = "updateDialogOption",
            paramtypez = {int.class, String.class})
    public static class EventUpdateOption {
        @SpirePrefixPatch
        public static void Prefix(GenericEventDialog __instance, int index, @ByRef String[] text) {
            text[0] = Zh.fix(text[0]);
        }
    }

    @SpirePatch(clz = GenericEventDialog.class, method = "updateDialogOption",
            paramtypez = {int.class, String.class, boolean.class})
    public static class EventUpdateOptionDisabled {
        @SpirePrefixPatch
        public static void Prefix(GenericEventDialog __instance, int index, @ByRef String[] text,
                                  boolean disabled) {
            text[0] = Zh.fix(text[0]);
        }
    }

    @SpirePatch(clz = GridCardSelectScreen.class, method = "open",
            paramtypez = {CardGroup.class, int.class, String.class, boolean.class})
    public static class GridPrompt {
        @SpirePrefixPatch
        public static void Prefix(GridCardSelectScreen __instance, CardGroup group, int amount,
                                  @ByRef String[] message, boolean cancelable) {
            message[0] = Zh.fix(message[0]);
        }
    }

    @SpirePatch(clz = ThoughtBubble.class, method = SpirePatch.CONSTRUCTOR,
            paramtypez = {float.class, float.class, float.class, String.class, boolean.class})
    public static class BubbleText {
        @SpirePrefixPatch
        public static void Prefix(ThoughtBubble __instance, float x, float y, float duration,
                                  @ByRef String[] text, boolean instant) {
            text[0] = Zh.fix(text[0]);
        }
    }

    @SpirePatch(clz = BaseMod.class, method = "registerModBadge",
            paramtypez = {Texture.class, String.class, String.class, String.class, ModPanel.class})
    public static class BadgeText {
        @SpirePrefixPatch
        public static void Prefix(Texture texture, @ByRef String[] name, @ByRef String[] author,
                                  @ByRef String[] description, ModPanel panel) {
            name[0] = Zh.fix(name[0]);
            author[0] = Zh.fix(author[0]);
            description[0] = Zh.fix(description[0]);
        }
    }

    // The 9-arg constructor delegates to this one via this(labelText, null, ...),
    // and a constructor prefix runs after that delegation, so patch the 10-arg
    // constructor that actually builds the ModLabel and stores the tooltip.
    @SpirePatch(clz = ModLabeledToggleButton.class, method = SpirePatch.CONSTRUCTOR,
            paramtypez = {String.class, String.class, float.class, float.class, Color.class,
                    BitmapFont.class, boolean.class, ModPanel.class, Consumer.class, Consumer.class})
    public static class ToggleLabel {
        @SpirePrefixPatch
        public static void Prefix(ModLabeledToggleButton __instance, @ByRef String[] label,
                                  @ByRef String[] tooltip, float x, float y, Color color, BitmapFont font,
                                  boolean enabled, ModPanel panel, Consumer onChangeLabel,
                                  Consumer onChangeToggle) {
            label[0] = Zh.fix(label[0]);
            tooltip[0] = Zh.fix(tooltip[0]);
        }
    }
}
