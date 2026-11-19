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
 */
final class MpSupport {
    private static final String MOD_CLASS = "spireTogether.SpireTogetherMod";
    private static final String MANAGER_CLASS = "spireTogether.network.P2P.P2PManager";
    private static final String ROOM_ENTRY_CLASS = "spireTogether.patches.network.RoomEntryPatch";

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
     * 修正“同一个地图节点上，其他玩家的房间阶段编号和本地不一致”的情况。
     *
     * 联机模组用 (地图节点 + 阶段编号) 判断“是不是和我呆在同一个房间”：
     *   P2PPlayer.IsPlayerInSameRoomAndAction() / NetworkLocation.IsSameAsCurrentRoomAndAction()
     * 角色渲染（ShouldRenderCharacter）和交易界面（PreTradingScreen.GetValidPlayers）
     * 都用这个判断。崩坠的心脏商店是特殊房间，两边的阶段编号可能差一格，
     * 结果就是：对方的信息栏还在，但人物不画出来，交易列表里也找不到人。
     *
     * 这里只把“确实在同一个节点、只是阶段编号不同”的玩家的展示用阶段编号
     * 对齐到本地，不改动任何一方真实的对局数据，也不在战斗里动。
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
            Object selfId = readField(idField, self);
            Object selfLocation = readField(locationField, self);
            if (selfLocation == null) {
                return;
            }
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
                    continue;
                }
                Object otherAction = readField(locActionField, otherLocation);
                if (!(otherAction instanceof Integer) || otherAction.equals(counter)) {
                    continue;
                }
                // 只修“差一格”的阶段漂移；差更多的说明确实是不同阶段，保持原样。
                int diff = ((Integer) counter) - ((Integer) otherAction);
                if (diff != 1 && diff != -1) {
                    continue;
                }
                locActionField.set(otherLocation, counter);
            }
        } catch (Throwable ignored) {
            // 联机模组内部结构变了也不能拖垮游戏，直接放弃这次修正。
        }
    }

    /** 同一个幕、同一个地图节点（不看阶段编号）。 */
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

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            Class<?> modClass = Class.forName(MOD_CLASS);
            Class<?> managerClass = Class.forName(MANAGER_CLASS);
            Class<?> roomEntryClass = Class.forName(ROOM_ENTRY_CLASS);
            Class<?> playerClass = Class.forName("spireTogether.network.P2P.P2PPlayer");
            Class<?> locationClass = Class.forName("spireTogether.network.objects.rooms.NetworkLocation");

            isConnectedField = modClass.getField("isConnected");
            getSelfMethod = managerClass.getMethod("GetSelf");
            getAllPlayersMethod = managerClass.getMethod("GetAllPlayers", boolean.class);
            roomActionCounterField = roomEntryClass.getField("roomActionCounter");

            idField = playerClass.getField("id");
            locationField = playerClass.getField("location");
            locXField = locationClass.getField("x");
            locYField = locationClass.getField("y");
            locActField = locationClass.getField("act");
            locInfinityField = locationClass.getField("infinityCounter");
            locActionField = locationClass.getField("action");

            available = isConnectedField != null && getSelfMethod != null
                    && getAllPlayersMethod != null && roomActionCounterField != null
                    && idField != null && locationField != null && locXField != null
                    && locYField != null && locActField != null && locInfinityField != null
                    && locActionField != null;
        } catch (Throwable t) {
            available = false;
        }
    }
}