package mpdownfallmap;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.map.MapEdge;
import com.megacrit.cardcrawl.map.MapRoomNode;
import downfall.patches.EvilModeCharacterSelect;

import java.util.ArrayList;

/**
 * 修复联机时崩坠（Evil Mode）翻转地图的图标错位（火堆钻进 Boss 图标 / 掉出地图底边）。
 *
 * 原理：
 * Downfall 的 FlipMap.PositionAdjustment 挂在 MapRoomNode 的构造器上（Postfix），
 * 崩坠模式下把每个节点的 offsetY 抬高 400 * Settings.scale（EVIL_ADJUST）。
 * 翻转后的地图要靠这 400 把整张图往上顶，才不会和底部的 Boss 图标、地图下边框打架。
 *
 * 联机模组（Together in Spire）的 MapRoomNodeJitterSync.Sync 挂在同一个构造器上，
 * 为了让所有玩家看到一样的抖动，它会把 offsetX / offsetY 直接重算成 ±37 的小随机值。
 * 它比 Downfall 后加载，于是构造器跑完之后 400 就被整段覆盖掉了：
 * 地图数据是翻转的，但整张图比单机崩坠模式低了约 2.7 行，
 * 最底下那排火堆（翻图时整行改成 RestRoom 的那排）就压在 Boss 图标与地图底边上。
 *
 * 处理方式：在 generateMap 结束时（此时节点和连线都已生成完毕）检查一次，
 * 只要确认“这张图确实被崩坠翻转过”，就把被抹掉的 400 补回去；
 * 节点偏移变了以后，已经按旧偏移烘死在地图连线里的点阵坐标也必须重建，否则路点会错位。
 *
 * 单机（联机模组没有覆盖偏移）时 offsetY 已经带着 400，这里检测后什么都不做。
 */
public final class EvilMapLayoutFix {
    /** Downfall FlipMap.PositionAdjustment 里的 EVIL_ADJUST，乘 Settings.scale 使用。 */
    private static final float EVIL_ADJUST = 400.0f;
    /** 重建连线时给“假 Boss 节点”用的目标 Y，与 Downfall FlipMap.flip 一致。 */
    private static final int FAKE_BOSS_Y = -1;

    private EvilMapLayoutFix() {
    }

    /**
     * generateMap 的最后一步是 distributeRoomsAcrossMap，而崩坠的翻转插在它前面，
     * 所以这里的 Postfix 拿到的是“已经翻转、房间也已分配完”的最终地图。
     */
    @SpirePatch2(clz = AbstractDungeon.class, method = "generateMap", paramtypez = {})
    public static class GenerateMapPostfix {
        @SpirePostfixPatch
        public static void Postfix() {
            restoreEvilNodeOffset();
        }
    }

    private static void restoreEvilNodeOffset() {
        if (!EvilModeCharacterSelect.evilMode) {
            return;
        }
        ArrayList<ArrayList<MapRoomNode>> map = AbstractDungeon.map;
        if (map == null || map.isEmpty() || !isFlippedByDownfall(map)) {
            // 没有翻转（例如 Downfall 跳过的其它 mod 幕），保持原样，绝不能乱动偏移。
            return;
        }

        float evilAdjust = EVIL_ADJUST * Settings.scale;
        // 联机模组写回去的抖动最大约 37 * scale，和 400 * scale 差了一个数量级，
        // 用一半（200 * scale）当阈值可以干净地区分“被抹掉”和“还在”。
        float missingThreshold = evilAdjust * 0.5f;

        boolean changed = false;
        for (ArrayList<MapRoomNode> row : map) {
            if (row == null) {
                continue;
            }
            for (MapRoomNode node : row) {
                if (node == null) {
                    continue;
                }
                if (Math.abs(node.offsetY) < missingThreshold) {
                    node.offsetY += evilAdjust;
                    changed = true;
                }
            }
        }
        if (changed) {
            rebuildEdges(map);
        }
    }

    /**
     * 崩坠翻转地图时会给最下面一行补一条通往 y = -1 的假 Boss 连线（FlipMap.flip），
     * 这条连线只可能在翻转成功的地图上出现，用它判断这张图是否真的被翻转过。
     */
    private static boolean isFlippedByDownfall(ArrayList<ArrayList<MapRoomNode>> map) {
        for (ArrayList<MapRoomNode> row : map) {
            if (row == null) {
                continue;
            }
            for (MapRoomNode node : row) {
                if (node == null) {
                    continue;
                }
                for (MapEdge edge : node.getEdges()) {
                    if (edge != null && edge.dstY == FAKE_BOSS_Y) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * MapEdge 的点阵是在构造时按当时的节点偏移算好并保存下来的（绝对坐标），
     * 节点偏移改了以后必须用新偏移重建，否则路线会整体比图标低 400。
     */
    private static void rebuildEdges(ArrayList<ArrayList<MapRoomNode>> map) {
        for (ArrayList<MapRoomNode> row : map) {
            if (row == null) {
                continue;
            }
            for (MapRoomNode node : row) {
                if (node == null) {
                    continue;
                }
                ArrayList<MapEdge> edges = node.getEdges();
                if (edges.isEmpty()) {
                    continue;
                }
                ArrayList<int[]> targets = new ArrayList<int[]>(edges.size());
                for (MapEdge edge : edges) {
                    if (edge != null) {
                        targets.add(new int[]{edge.dstX, edge.dstY});
                    }
                }
                edges.clear();
                for (int[] target : targets) {
                    addRebuiltEdge(map, node, target[0], target[1]);
                }
            }
        }
    }

    private static void addRebuiltEdge(ArrayList<ArrayList<MapRoomNode>> map, MapRoomNode node, int dstX, int dstY) {
        if (dstY == FAKE_BOSS_Y) {
            // 与 Downfall FlipMap.flip 里的假 Boss 连线保持一致（1280 半径 + 偏移）。
            node.addEdge(new MapEdge(node.x, node.y, node.offsetX, node.offsetY,
                    3, FAKE_BOSS_Y, 0.0f, Settings.MAP_DST_Y * 2.0f, true));
            return;
        }
        if (dstY < 0 || dstY >= map.size()) {
            return;
        }
        ArrayList<MapRoomNode> dstRow = map.get(dstY);
        if (dstRow == null || dstX < 0 || dstX >= dstRow.size()) {
            return;
        }
        MapRoomNode dst = dstRow.get(dstX);
        if (dst == null) {
            return;
        }
        node.addEdge(new MapEdge(node.x, node.y, node.offsetX, node.offsetY,
                dst.x, dst.y, dst.offsetX, dst.offsetY, false));
    }
}
