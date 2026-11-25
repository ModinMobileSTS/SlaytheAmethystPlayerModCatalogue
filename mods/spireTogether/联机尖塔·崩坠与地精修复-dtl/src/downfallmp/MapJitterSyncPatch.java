package downfallmp;

import com.evacipated.cardcrawl.modthespire.lib.SpireInstrumentPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.megacrit.cardcrawl.map.MapRoomNode;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;

@SpirePatch2(cls = "spireTogether.map.MapRoomNodeJitterSync$Sync", method = "Postfix",
    paramtypez = {MapRoomNode.class}, requiredModId = "downfall", optional = true)
public final class MapJitterSyncPatch {
    private MapJitterSyncPatch() {}

    @SpireInstrumentPatch
    public static ExprEditor Instrument() {
        return new ExprEditor() {
            @Override
            public void edit(FieldAccess field) throws CannotCompileException {
                if (field.isWriter() && MapRoomNode.class.getName().equals(field.getClassName())
                    && "offsetY".equals(field.getFieldName())) {
                    field.replace("{ $proceed(" + MapJitterSyncPatch.class.getName() + ".merge($0, $1)); }");
                }
            }
        };
    }

    public static float merge(MapRoomNode node, float syncedJitterY) {
        Float base = MapBaseJitterCapture.Fields.baseJitterY.get(node);
        if (base == null) return syncedJitterY;
        MapBaseJitterCapture.Fields.baseJitterY.set(node, null);
        // 替换的只是原版随机偏移；若崩坠的布局偏移已经写入，就保留它。
        return syncedJitterY + (node.offsetY - base);
    }
}
