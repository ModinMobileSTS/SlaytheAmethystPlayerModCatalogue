package neowpreview;

import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.megacrit.cardcrawl.core.Settings;

/**
 * 撤回点的磁盘副本。
 *
 * <p>内存里的 {@link Snapshot} 一旦关掉游戏就没了，所以选祝福前顺手把同样的内容写进
 * ModTheSpire 的配置文件；读档回到涅奥的「已经领过祝福」界面时，看这里是否还有同一种子的撤回点，
 * 有就把「撤回祝福并重新选择」那一行加回去。</p>
 */
public final class RestorePoint {

    private static final String MOD_ID = "neowpreview";
    private static final String FILE = "neowpreview_restorepoint";
    private static final String KEY_DATA = "data";

    private static SpireConfig config;

    private RestorePoint() {
    }

    private static SpireConfig config() {
        if (config == null) {
            try {
                config = new SpireConfig(MOD_ID, FILE);
            } catch (Throwable t) {
                return null;
            }
        }
        return config;
    }

    /** 记下“选祝福前”的状态（选祝福时调用，会覆盖上一次的撤回点）。 */
    public static void save(Snapshot snapshot) {
        SpireConfig cfg = config();
        if (snapshot == null || cfg == null) {
            return;
        }
        try {
            cfg.setString(KEY_DATA, snapshot.encode());
            cfg.save();
        } catch (Throwable ignored) {
        }
    }

    /** 磁盘上是否还有属于当前这一局的撤回点。 */
    public static boolean exists() {
        return load() != null;
    }

    private static Snapshot load() {
        try {
            String data = config().getString(KEY_DATA);
            if (data == null || data.isEmpty()) {
                return null;
            }
            Snapshot snapshot = Snapshot.decode(data);
            if (snapshot == null) {
                return null;
            }
            // 种子对不上说明是上一局残留的，忽略
            if (snapshot.seed != null && Settings.seed != null
                    && !snapshot.seed.equals(Settings.seed)) {
                return null;
            }
            return snapshot;
        } catch (Throwable t) {
            return null;
        }
    }

    /** 把玩家状态恢复到磁盘上那份撤回点；成功时返回那份快照，失败返回 null。 */
    public static Snapshot restore() {
        Snapshot snapshot = load();
        if (snapshot == null) {
            return null;
        }
        try {
            snapshot.restore();
            return snapshot;
        } catch (Throwable t) {
            return null;
        }
    }

    /** 主动丢弃撤回点（例如已经离开涅奥房间）。 */
    public static void clear() {
        try {
            config().setString(KEY_DATA, "");
            config().save();
        } catch (Throwable ignored) {
        }
    }
}
