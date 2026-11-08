package simplemapdownfall;

import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.map.MapEdge;
import com.megacrit.cardcrawl.map.MapRoomNode;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import simplemap.SimpleMap;
import simplemap.patch.ShrinkMap;

/**
 * Simple Map 的收缩地图把"爬塔进度"画在屏幕的 X 轴上：y 越大越靠右，Boss 图标在最右侧。
 * 崩坠的反派模式会把整张图上下翻转，进度变成从 y 大的行往 y=0 走，
 * 于是起始行被画在紧贴 Boss 的右端、玩家要从右往左走，整张图读起来是反的。
 *
 * 这里在崩坠翻图之后把节点重新摆到收缩网格上并镜像进度轴
 * （y 大的行在左、Boss 侧的 y=0 在右），同时：
 * 1. 直接写入绝对偏移，顺带抹掉崩坠给每个节点叠加的 400*scale 竖直偏移；
 * 2. 按新偏移重建全部连线，因为 Simple Map 的连线几何在构造 MapEdge 时就固定下来了；
 *    崩坠翻图时给 Boss 连线的 dstOffsetY 是 2*MAP_DST_Y（为未收缩的布局准备的），
 *    在收缩布局下会指向屏幕外，重建时归零，让 Boss 连线落在 Simple Map 的 Boss 图标上。
 */
public class DownfallMapAdapter {

    private static Field evilModeField;
    private static boolean evilModeLookupDone = false;

    /** 崩坠反派模式的开关（未安装崩坠时恒为 false）。 */
    public static boolean isEvilMode() {
        if (!evilModeLookupDone) {
            evilModeLookupDone = true;
            try {
                evilModeField = Class.forName("downfall.patches.EvilModeCharacterSelect")
                        .getField("evilMode");
            } catch (Throwable ignored) {
                evilModeField = null;
            }
        }
        if (evilModeField == null) {
            return false;
        }
        try {
            return evilModeField.getBoolean(null);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** 复刻崩坠自身的翻图条件，只有地图确实会被翻转时才接管布局。 */
    public static boolean willFlip() {
        if (!isEvilMode()) {
            return false;
        }
        String id = AbstractDungeon.id;
        if (id == null) {
            return false;
        }
        // 崩坠 invalidActs 中排除的幕不翻图
        if (id.equals("paleoftheancients:PaleOfTheAncients") || id.equals("infinite-spire:TheVoid")) {
            return false;
        }
        // 崩坠的 moddedActFourCheck：非终幕的第 4 幕及以后不翻图
        if (AbstractDungeon.actNum >= 4 && !Settings.isEndless && !id.equals("TheEnding")) {
            return false;
        }
        return true;
    }

    /** 只有"开启地图收缩"且"崩坠会翻图"时才接管布局。 */
    public static boolean active() {
        return SimpleMap.shrinkMap && willFlip();
    }

    /** 重新排布当前地图（幂等，重复调用结果一致）。 */
    public static void redress() {
        if (!active()) {
            return;
        }
        ArrayList<ArrayList<MapRoomNode>> map = AbstractDungeon.map;
        if (map == null || map.isEmpty()) {
            return;
        }
        int rows = map.size();
        int cols = 0;
        for (ArrayList<MapRoomNode> row : map) {
            if (row != null && row.size() > cols) {
                cols = row.size();
            }
        }
        if (rows <= 0 || cols <= 0) {
            return;
        }

        positionNodes(map, rows, cols);
        rebuildEdges(map, rows);
    }

    private static void positionNodes(ArrayList<ArrayList<MapRoomNode>> map, int rows, int cols) {
        float rowStep = ShrinkMap.SHRUNK_MAP_WIDTH / (float) rows;
        float colStep = ShrinkMap.SHRUNK_MAP_HEIGHT / (float) cols;
        for (ArrayList<MapRoomNode> row : map) {
            if (row == null) {
                continue;
            }
            for (MapRoomNode node : row) {
                if (node == null) {
                    continue;
                }
                // 进度轴镜像：翻转后的起始行（y 大）落在左端，Boss 侧（y=0）落在右端
                int progress = (rows - 1) - node.y;
                if (progress < 0) {
                    progress = 0;
                } else if (progress > rows - 1) {
                    progress = rows - 1;
                }
                float targetX = ShrinkMap.SHRUNK_MAP_MIN_X + (float) progress * rowStep;
                float targetY = ShrinkMap.SHRUNK_MAP_MIN_Y + (float) node.x * colStep;
                // 写成绝对偏移：同时覆盖父模组的网格换算和崩坠的 400*scale 平移
                node.offsetX = targetX - SimpleMap.getNodeX((float) node.x, 0.0f);
                node.offsetY = targetY - SimpleMap.getNodeY((float) node.y, 0.0f);
            }
        }
    }

    private static void rebuildEdges(ArrayList<ArrayList<MapRoomNode>> map, int rows) {
        for (ArrayList<MapRoomNode> row : map) {
            if (row == null) {
                continue;
            }
            for (MapRoomNode node : row) {
                if (node == null) {
                    continue;
                }
                ArrayList<MapEdge> old = node.getEdges();
                if (old.isEmpty()) {
                    continue;
                }
                List<int[]> targets = new ArrayList<>(old.size());
                for (MapEdge edge : old) {
                    targets.add(new int[]{edge.dstX, edge.dstY});
                }
                old.clear();
                for (int[] dst : targets) {
                    MapRoomNode dstNode = nodeAt(map, dst[0], dst[1], rows);
                    boolean isBoss = dstNode == null;
                    float dstOffsetX = isBoss ? 0.0f : dstNode.offsetX;
                    float dstOffsetY = isBoss ? 0.0f : dstNode.offsetY;
                    node.addEdge(new MapEdge(node.x, node.y, node.offsetX, node.offsetY,
                            dst[0], dst[1], dstOffsetX, dstOffsetY, isBoss));
                }
            }
        }
    }

    private static MapRoomNode nodeAt(ArrayList<ArrayList<MapRoomNode>> map, int x, int y, int rows) {
        if (y < 0 || y >= rows) {
            return null;
        }
        ArrayList<MapRoomNode> row = map.get(y);
        if (row == null || x < 0 || x >= row.size()) {
            return null;
        }
        return row.get(x);
    }
}
