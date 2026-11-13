package tkfix;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedHashSet;

/**
 * 临时诊断输出：把关键状态写进智能体工作区的 diag.txt，便于定位问题。
 * 每条不同的信息只写一次，避免刷屏；任何情况下都不会影响游戏。
 */
public final class Diag {
    private static final String PATH =
            "/storage/emulated/0/Android/data/io.stamethyst/files/sts/agent_workspace/timekeepermod/diag.txt";
    private static final int MAX_LINES = 250;

    private static final LinkedHashSet<String> SEEN = new LinkedHashSet<String>();
    private static boolean cleared;

    private Diag() {
    }

    public static void log(String tag, String msg) {
        try {
            String line = tag + " | " + msg;
            if (SEEN.contains(line) || SEEN.size() >= MAX_LINES) {
                return;
            }
            SEEN.add(line);
            File f = new File(PATH);
            File dir = f.getParentFile();
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
            }
            boolean truncate = !cleared;
            cleared = true;
            Writer w = new OutputStreamWriter(new FileOutputStream(f, !truncate), "UTF-8");
            try {
                w.write(line + "\n");
            } finally {
                w.close();
            }
        } catch (Throwable ignored) {
            // 诊断失败不能影响游戏
        }
    }

    public static void error(String tag, Throwable t) {
        if (t == null) {
            return;
        }
        StringBuilder sb = new StringBuilder("EXCEPTION ");
        sb.append(t.getClass().getName()).append(": ").append(t.getMessage());
        StackTraceElement[] st = t.getStackTrace();
        int n = st == null ? 0 : Math.min(5, st.length);
        for (int i = 0; i < n; i++) {
            sb.append(" @ ").append(st[i]);
        }
        log(tag, sb.toString());
    }
}
