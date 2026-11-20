package mpdownfallmap;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;

/**
 * 访问 Together in Spire 运行期状态的反射桥。
 *
 * 联机模组不是本补丁的编译期依赖，所以只能靠反射：
 * 1) SpireTogetherMod.isConnected —— 判断当前是否在联机局里；
 * 2) P2PManager.GetSelf() / GetAllPlayers(boolean) —— 取自己和其他玩家的同步快照；
 * 3) RoomEntryPatch.roomActionCounter —— 当前房间的“阶段编号”。
 *
 * 这些只是联机模组自己缓存的展示层数据，改它们不会影响真实的对局状态。
 * 反射一律走 findField / findMethod，找不到 public 成员时会向上找并 setAccessible，
 * 避免联机模组把某个字段改成非 public 后修复静默失效。
 */
final class MpSupport {
    private static final String MOD_CLASS = "spireTogether.SpireTogetherMod";
    private static final String MANAGER_CLASS = "spireTogether.network.P2P.P2PManager";
    private static final String ROOM_ENTRY_CLASS = "spireTogether.patches.network.RoomEntryPatch";
    private static final String PLAYER_CLASS = "spireTogether.network.P2P.P2PPlayer";
    private static final String LOCATION_CLASS = "spireTogether.network.objects.rooms.NetworkLocation";

    private static boolean resolved;
    private static boolean available;

    private static Field isConnectedField;
    private static Method getSelfMethod;
    private static Method getAllPlayersMethod;
    private static Field roomActionCounterField;

    private static Field idField;
    private static Field locationField;
    private static Field locXField;
    private static Field locYField;
    private static Field locActField;
    private static Field locInfinityField;
    private static Field locActionField;

    private static long lastLogMillis;

    private MpSupport() {
    }

    /** 当前是否处于联机房间/联机局。没装或不兼容联机模组时永远是 false。 */
    static boolean isConnected() {
        resolve();
        if (!available || isConnectedField == null) {
            return false;
        }
        try {
            return isConnectedField.getBoolean(null);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * 修正“同一个地图格子上，其他玩家的房间阶段编号和本地不一致”的问题。
     *
     * 联机模组用 (地图格子 + 阶段编号) 判断“是不是和我待在同一个房间”：
     *   P2PPlayer.ShouldRenderCharacter() -> NetworkLocation.IsSameAsCurrentRoomAndAction()
     *   PreTradingScreen.GetValidPlayers() -> P2PPlayer.IsPlayerInSameRoomAndAction()
     * 于是编号一错，队友的人物就不画、交易名单也空。
     *
     * 崩坠的心脏商店每次 showHeartMerchant 都会让编号 +1，而“生成这间房”的机器和
     * “只加载房间数据”的机器调用次数不一样，编号就对不上了。
     *
     * 这里只把“确实在同一个地图格子（幕/坐标/幕内计数完全相同）”的队友的展示用编号
     * 对齐到本地，地图位置字段一个都不改、真实对局数据也不动，所以不存在
     * “不在同一个地方也能交易”的可能。
     */
    static void alignSameRoomAction() {
        if (!isConnected()) {
            return;
        }
        try {
            Object counter = readField(roomActionCounterField, null);
            if (!(counter instanceof Integer)) {
                return;
            }
            Object self = invoke(getSelfMethod);
            if (self == null) {
                return;
            }
            Object selfLocation = readField(locationField, self);
            if (selfLocation == null) {
                return;
            }
            Object selfId = readField(idField, self);
            for (Iterator<?> it = allPlayers(); it != null && it.hasNext(); ) {
                Object other = it.next();
                if (other == null) {
                    continue;
                }
                Object otherId = readField(idField, other);
                if (otherId != null && otherId.equals(selfId)) {
                    continue;
                }
                Object otherLocation = readField(locationField, other);
                if (otherLocation == null || !isSameNode(otherLocation, selfLocation)) {
                    // 不在同一个地图格子：完全不碰，避免出现“不同地点也能交易”。
                    continue;
                }
                Object otherAction = readField(locActionField, otherLocation);
                if (counter.equals(otherAction)) {
                    continue;
                }
                locActionField.set(otherLocation, counter);
                logFix(otherId, otherAction, counter);
            }
        } catch (Throwable ignored) {
            // 联机模组内部结构变了也不能拖垮游戏，直接放弃这次修正。
        }
    }

    /** 同一个幕、同一个地图格子（不看阶段编号）。 */
    private static boolean isSameNode(Object a, Object b) {
        Object ax = readField(locXField, a);
        Object bx = readField(locXField, b);
        Object ay = readField(locYField, a);
        Object by = readField(locYField, b);
        Object aAct = readField(locActField, a);
        Object bAct = readField(locActField, b);
        Object aInf = readField(locInfinityField, a);
        Object bInf = readField(locInfinityField, b);
        return ax != null && ax.equals(bx) && ay != null && ay.equals(by)
                && aAct != null && aAct.equals(bAct)
                && aInf != null && aInf.equals(bInf);
    }

    /** 修正时留一条日志，方便以后排查；限流避免刷屏。 */
    private static void logFix(Object otherId, Object before, Object after) {
        long now = System.currentTimeMillis();
        if (now - lastLogMillis < 3000L) {
            return;
        }
        lastLogMillis = now;
        System.out.println("[downfall-mp-fix] 对齐同房间阶段编号：玩家 " + otherId
                + " 的编号 " + before + " -> " + after);
    }

    private static Iterator<?> allPlayers() {
        Object result = invoke(getAllPlayersMethod, Boolean.FALSE);
        if (result instanceof Iterator) {
            return (Iterator<?>) result;
        }
        if (result instanceof Iterable) {
            return ((Iterable<?>) result).iterator();
        }
        return null;
    }

    private static Object invoke(Method method, Object... args) {
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(null, args);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object readField(Field field, Object target) {
        if (field == null) {
            return null;
        }
        try {
            return field.get(target);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            Class<?> modClass = Class.forName(MOD_CLASS);
            Class<?> managerClass = Class.forName(MANAGER_CLASS);
            Class<?> roomEntryClass = Class.forName(ROOM_ENTRY_CLASS);
            Class<?> playerClass = Class.forName(PLAYER_CLASS);
            Class<?> locationClass = Class.forName(LOCATION_CLASS);

            isConnectedField = findField(modClass, "isConnected");
            getSelfMethod = findMethod(managerClass, "GetSelf");
            getAllPlayersMethod = findMethod(managerClass, "GetAllPlayers", boolean.class);
            roomActionCounterField = findField(roomEntryClass, "roomActionCounter");

            idField = findField(playerClass, "id");
            locationField = findField(playerClass, "location");
            locXField = findField(locationClass, "x");
            locYField = findField(locationClass, "y");
            locActField = findField(locationClass, "act");
            locInfinityField = findField(locationClass, "infinityCounter");
            locActionField = findField(locationClass, "action");

            available = isConnectedField != null && getSelfMethod != null
                    && getAllPlayersMethod != null && roomActionCounterField != null
                    && idField != null && locationField != null && locXField != null
                    && locYField != null && locActField != null && locInfinityField != null
                    && locActionField != null;
        } catch (Throwable t) {
            available = false;
        }
    }

    /** 找字段（含父类），找到后强制可访问。 */
    private static Field findField(Class<?> type, String name) {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Field field = c.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (Throwable ignored) {
                // 继续往上找
            }
        }
        return null;
    }

    /** 找静态方法（含父类），找到后强制可访问。 */
    private static Method findMethod(Class<?> type, String name, Class<?>... params) {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Method method = c.getDeclaredMethod(name, params);
                method.setAccessible(true);
                return method;
            } catch (Throwable ignored) {
                // 继续往上找
            }
        }
        return null;
    }
}
