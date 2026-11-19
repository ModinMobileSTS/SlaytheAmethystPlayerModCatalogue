package mpdownfallmap;

import downfall.patches.EvilModeCharacterSelect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Together in Spire 大厅里的“崩坠地图”复选框只把布尔值写进房间设置
 * （键名 downfall_downfallMap）。真正把它转成 Downfall 崩坠模式开关
 * EvilModeCharacterSelect.evilMode 的唯一位置，是联机模组自带的
 * DownfallMapSelectionPatches$MapSet —— 一个挂在 MPLobbyScreen.Embark 上的 Prefix。
 *
 * 联机中文补丁（TogetherInSpireChinesePatch）为了汉化按钮，整个重建了大厅的开始按钮，
 * 并且在 MPLobbyScreenCP$RegisterStartButtonPostfix 里复制了一份 Embark
 * （因为 MPLobbyScreen.Embark 是 private，别的 mod 调不到）。点击出发时执行的是这份复制品，
 * 于是那个 Prefix 永远不会跑，evilMode 一直是 false：金币不换灵魂、Boss 与事件不替换、
 * 地图也不会翻转。
 *
 * 本类不再依赖大厅按钮的实现细节，而是在“运行真正开始”的两个关键时刻
 * （Downfall 的 receiveStartGame、以及地图生成之前）直接读同一份房间设置，
 * 把 evilMode 同步成正确值。不在联机房间（读不到房间设置）时完全不碰 evilMode，
 * 单人模式行为保持不变。
 *
 * 这里必须用反射：Together in Spire 不是本补丁的编译期依赖。
 */
public final class LobbyDownfallMapSync {
    /** 联机模组使用的设置键名，见 DownfallMapSelectionPatches.downfallMapSetting。 */
    public static final String MAP_SETTING = "downfall_downfallMap";
    /** Downfall 的“崩坠”运行模组，即大厅自定义模组列表里那一项。 */
    public static final String EVIL_RUN_MOD_ID = "downfall:EvilRun";

    private static final String P2P_MANAGER_CLASS = "spireTogether.network.P2P.P2PManager";

    private LobbyDownfallMapSync() {
    }

    /**
     * 把 evilMode 同步成联机房间当前的意图。
     * 读不到房间设置（单人游戏，或没装/没进联机）时直接返回，不改动任何状态。
     */
    public static void sync() {
        Object settings = readLobbySettings();
        if (settings == null) {
            return;
        }
        EvilModeCharacterSelect.evilMode = isDownfallMapRequested(settings);
    }

    private static boolean isDownfallMapRequested(Object settings) {
        // 复选框“崩坠地图”，或者大厅自定义模组里勾了“崩坠”(downfall:EvilRun)。
        return isMapSettingEnabled(settings) || hasEvilRunMod(settings);
    }

    private static boolean isMapSettingEnabled(Object settings) {
        try {
            Method getter = settings.getClass().getMethod("getBooleanSetting", String.class);
            Object value = getter.invoke(settings, MAP_SETTING);
            return value instanceof Boolean && ((Boolean) value).booleanValue();
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean hasEvilRunMod(Object settings) {
        Object mods = readField(settings, "customMods");
        if (!(mods instanceof Iterable)) {
            return false;
        }
        for (Object mod : (Iterable<?>) mods) {
            if (EVIL_RUN_MOD_ID.equals(readField(mod, "id"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * P2PManager.data.settings，即当前联机房间的设置对象。
     * P2PManager.data 只在联机房间期间存在（Reset 时会置回 null），
     * 所以返回 null 就等于“当前不是联机房间”。
     */
    private static Object readLobbySettings() {
        try {
            Object data = readStaticField(Class.forName(P2P_MANAGER_CLASS), "data");
            if (data == null) {
                return null;
            }
            return readField(data, "settings");
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object readStaticField(Class<?> type, String name) throws Exception {
        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(null);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static Object readField(Object target, String name) {
        if (target == null) {
            return null;
        }
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }
}
