package dnkmodl10n;

import DNKmod.Familiar;
import DNKmod.FamiliarTank;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Translates the companion (friendly minion) names and their move buttons.
 * The friendly minion classes come from the optional "Friendly Minions" mod, which is
 * not part of this patch's compile classpath, so the displayed strings are edited
 * through reflection on the created instances.
 */
public class MinionPatch {

    @SpirePatch2(clz = Familiar.class, method = "<ctor>", paramtypez = {int.class, int.class})
    public static class FamiliarCtorPatch {
        @SpirePostfixPatch
        public static void Postfix(Object __instance) {
            translateMinionName(__instance);
        }
    }

    @SpirePatch2(clz = Familiar.class, method = "addMoves", paramtypez = {})
    public static class FamiliarMovesPatch {
        @SpirePostfixPatch
        public static void Postfix(Object __instance) {
            translateMoves(__instance);
        }
    }

    @SpirePatch2(clz = FamiliarTank.class, method = "<ctor>", paramtypez = {int.class, int.class})
    public static class FamiliarTankCtorPatch {
        @SpirePostfixPatch
        public static void Postfix(Object __instance) {
            translateMinionName(__instance);
        }
    }

    @SpirePatch2(clz = FamiliarTank.class, method = "addMoves", paramtypez = {})
    public static class FamiliarTankMovesPatch {
        @SpirePostfixPatch
        public static void Postfix(Object __instance) {
            translateMoves(__instance);
        }
    }

    private static void translateMinionName(Object minion) {
        if (minion == null) {
            return;
        }
        try {
            String id = fieldText(minion, "id");
            if (id == null) {
                return;
            }
            String translated = Translations.monsterName(id);
            if (translated == null) {
                return;
            }
            setField(minion, "name", translated);
        } catch (Throwable ignored) {
            // Cosmetic only.
        }
    }

    private static void translateMoves(Object minion) {
        if (minion == null) {
            return;
        }
        try {
            Field movesField = findField(minion.getClass(), "moves");
            if (movesField == null) {
                return;
            }
            movesField.setAccessible(true);
            Object moveGroup = movesField.get(minion);
            if (moveGroup == null) {
                return;
            }
            Method getMoves = moveGroup.getClass().getMethod("getMoves");
            Object moves = getMoves.invoke(moveGroup);
            if (!(moves instanceof List)) {
                return;
            }
            for (Object move : (List<?>) moves) {
                if (move == null) {
                    continue;
                }
                String name = Translations.moveName(fieldText(move, "ID"));
                if (name != null) {
                    setField(move, "ID", name);
                }
                String description = Translations.moveDescription(fieldText(move, "moveDescription"));
                if (description != null) {
                    setField(move, "moveDescription", description);
                }
            }
        } catch (Throwable ignored) {
            // Cosmetic only.
        }
    }

    private static String fieldText(Object target, String fieldName) {
        try {
            Field field = findField(target.getClass(), fieldName);
            if (field == null) {
                return null;
            }
            field.setAccessible(true);
            Object value = field.get(target);
            return value instanceof String ? (String) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void setField(Object target, String fieldName, String value) {
        if (value == null) {
            return;
        }
        try {
            Field field = findField(target.getClass(), fieldName);
            if (field == null) {
                return;
            }
            field.setAccessible(true);
            field.set(target, value);
        } catch (Throwable ignored) {
            // Cosmetic only.
        }
    }

    private static Field findField(Class<?> type, String fieldName) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                // Keep walking up the hierarchy.
            }
        }
        return null;
    }
}
