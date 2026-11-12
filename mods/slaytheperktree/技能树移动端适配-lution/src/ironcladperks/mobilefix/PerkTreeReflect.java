package ironcladperks.mobilefix;

import com.megacrit.cardcrawl.helpers.Hitbox;
import ironcladperks.screens.PerkTreeScreen;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Reflective access to the private state of {@link PerkTreeScreen}.
 *
 * <p>The parent mod keeps the camera offset, the node positions and the hovered node in private
 * fields. The replacement touch controls need to read and write them so the untouched
 * {@code render} method keeps drawing the same tree.</p>
 */
public final class PerkTreeReflect {
    private static final Logger logger = LogManager.getLogger(PerkTreeReflect.class.getName());

    private static Field cameraX;
    private static Field cameraY;
    private static Field hoveredPerk;
    private static Field nodePositions;
    private static Field nodeHitboxes;

    private static boolean initialized;
    private static boolean broken;

    private PerkTreeReflect() {
    }

    private static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            cameraX = find("cameraX");
            cameraY = find("cameraY");
            hoveredPerk = find("hoveredPerk");
            nodePositions = find("nodePositions");
            nodeHitboxes = find("nodeHitboxes");
        } catch (Throwable t) {
            broken = true;
            logger.error("PerkTreeMobile: PerkTreeScreen internals are not accessible, "
                    + "keeping the original keyboard/mouse controls", t);
        }
    }

    private static Field find(String name) throws NoSuchFieldException {
        Field field = PerkTreeScreen.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    public static boolean available() {
        init();
        return !broken;
    }

    public static float getCameraX(Object screen) {
        init();
        try {
            return cameraX == null ? 0.0f : cameraX.getFloat(screen);
        } catch (Throwable t) {
            return 0.0f;
        }
    }

    public static void setCameraX(Object screen, float value) {
        init();
        try {
            if (cameraX != null) {
                cameraX.setFloat(screen, value);
            }
        } catch (Throwable ignored) {
        }
    }

    public static float getCameraY(Object screen) {
        init();
        try {
            return cameraY == null ? 0.0f : cameraY.getFloat(screen);
        } catch (Throwable t) {
            return 0.0f;
        }
    }

    public static void setCameraY(Object screen, float value) {
        init();
        try {
            if (cameraY != null) {
                cameraY.setFloat(screen, value);
            }
        } catch (Throwable ignored) {
        }
    }

    public static void setHovered(Object screen, String perkId) {
        init();
        try {
            if (hoveredPerk != null) {
                hoveredPerk.set(screen, perkId);
            }
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, float[]> positions(Object screen) {
        init();
        try {
            Object value = nodePositions == null ? null : nodePositions.get(screen);
            if (value instanceof Map) {
                return (Map<String, float[]>) value;
            }
        } catch (Throwable ignored) {
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Hitbox> hitboxes(Object screen) {
        init();
        try {
            Object value = nodeHitboxes == null ? null : nodeHitboxes.get(screen);
            if (value instanceof Map) {
                return (Map<String, Hitbox>) value;
            }
        } catch (Throwable ignored) {
        }
        return Collections.emptyMap();
    }
}
