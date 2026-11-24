package neowpreview;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 极简反射工具，用于读取/写入原版私有字段（NeowEvent.screenNum、rewards、
 * LargeDialogOptionButton 的预览字段、NeowReward.activated）。
 */
public final class Reflect {

    private Reflect() {
    }

    public static Object get(Object target, String name) {
        if (target == null) {
            return null;
        }
        try {
            Field f = find(target.getClass(), name);
            if (f != null) {
                f.setAccessible(true);
                return f.get(target);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static boolean set(Object target, String name, Object value) {
        if (target == null) {
            return false;
        }
        try {
            Field f = find(target.getClass(), name);
            if (f != null) {
                f.setAccessible(true);
                f.set(target, value);
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /** 调用无参私有方法（例如 NeowEvent.blessing()）。 */
    public static boolean call(Object target, String name) {
        if (target == null) {
            return false;
        }
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                Method m = c.getDeclaredMethod(name);
                m.setAccessible(true);
                m.invoke(target);
                return true;
            } catch (NoSuchMethodException e) {
                c = c.getSuperclass();
            } catch (Throwable t) {
                return false;
            }
        }
        return false;
    }

    private static Field find(Class<?> type, String name) {
        Class<?> c = type;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        return null;
    }
}
