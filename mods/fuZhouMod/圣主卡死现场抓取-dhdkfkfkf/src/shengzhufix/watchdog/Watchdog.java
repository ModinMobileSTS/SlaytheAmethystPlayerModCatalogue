package shengzhufix.watchdog;

import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 卡死现场抓取（v2）。
 *
 * 主线程每帧调用一次 tick() 刷新时间戳。独立守护线程轮询：
 *  - 启动时先在若干候选目录里实测写一个探针文件，选出真正可写的目录，并写下启动横幅（用来确认补丁真的在跑、文件到底落在哪）。
 *  - 每 10 秒写一行「存活」，这样卡死之后能看出看门狗几点停止跳动。
 *  - 主线程 ≥3 秒没有心跳：先把「横幅 + 内存 + 游戏状态」立刻落盘并 flush，
 *    再另起一个线程去收集线程栈（收集线程栈在卡死时可能永远不返回，所以绝不能放在落盘之前）。
 *  - 停顿自行恢复时记一行。
 *
 * 只读游戏状态，不修改任何字段。
 */
public final class Watchdog {

    private static final long FIRST_DUMP_MS = 3000L;
    private static final long DUMP_INTERVAL_MS = 5000L;
    private static final long POLL_MS = 250L;
    private static final long ALIVE_INTERVAL_MS = 10000L;
    private static final long MAX_FILE_BYTES = 8L * 1024L * 1024L;
    private static final String FILE_NAME = "fuzhou-watchdog.txt";
    private static final String PREV_FILE_NAME = "fuzhou-watchdog.prev.txt";
    private static final int MAX_FRAMES = 80;

    private static final String[] CANDIDATE_DIRS = {
            System.getProperty("user.dir", "."),
            System.getProperty("user.dir", ".") + "/logs",
            "/storage/emulated/0/Android/data/io.stamethyst/files/sts",
            "/storage/emulated/0/Android/data/io.stamethyst/files/sts/logs",
            "/sdcard/Android/data/io.stamethyst/files/sts",
            "/sdcard/Android/data/io.stamethyst/files/sts/logs",
            System.getProperty("java.io.tmpdir", "."),
            System.getProperty("user.home", ".")
    };

    private static volatile long lastTickMs = System.currentTimeMillis();
    private static volatile Thread gameThread;
    private static volatile boolean started;
    private static volatile File outputFile;
    private static final AtomicBoolean stackDumpInFlight = new AtomicBoolean(false);

    private Watchdog() {
    }

    /** 主循环心跳，每帧一次，开销只有一次 volatile 写。 */
    public static void tick() {
        if (gameThread == null) {
            gameThread = Thread.currentThread();
        }
        lastTickMs = System.currentTimeMillis();
        if (!started) {
            ensureStarted();
        }
    }

    private static synchronized void ensureStarted() {
        if (started) {
            return;
        }
        started = true;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                loop();
            }
        }, "ShengZhu-Watchdog");
        t.setDaemon(true);
        t.start();
    }

    private static void loop() {
        StringBuilder probeLog = new StringBuilder();
        try {
            outputFile = resolveOutput(probeLog);
        } catch (Throwable ignored) {
        }
        try {
            StringBuilder banner = new StringBuilder();
            banner.append("########## 卡死抓取看门狗启动 ").append(now()).append(" ##########\n");
            banner.append("user.dir = ").append(System.getProperty("user.dir", "?")).append('\n');
            banner.append("输出文件 = ").append(outputFile == null ? "!! 没有找到任何可写目录 !!" : outputFile.getAbsolutePath()).append('\n');
            banner.append(probeLog);
            write(banner.toString());
            logToGame("看门狗启动，输出文件=" + (outputFile == null ? "无" : outputFile.getAbsolutePath()));
        } catch (Throwable ignored) {
        }

        boolean inStall = false;
        long stallStartMs = 0L;
        long lastDumpMs = 0L;
        long lastAliveMs = 0L;
        while (true) {
            try {
                Thread.sleep(POLL_MS);
            } catch (InterruptedException e) {
                return;
            }
            try {
                long now = System.currentTimeMillis();
                long since = now - lastTickMs;
                if (since >= FIRST_DUMP_MS) {
                    if (!inStall) {
                        inStall = true;
                        stallStartMs = lastTickMs;
                        lastDumpMs = 0L;
                    }
                    if (lastDumpMs == 0L || now - lastDumpMs >= DUMP_INTERVAL_MS) {
                        lastDumpMs = now;
                        writeStallState(now, since);
                        spawnStackDump(now, since);
                    }
                } else {
                    if (inStall) {
                        inStall = false;
                        write("【恢复】" + now() + " 停顿持续 " + (now - stallStartMs)
                                + "ms 后自行恢复，当前动作=" + currentActionName());
                        logToGame("主线程停顿 " + (now - stallStartMs) + "ms 后恢复，已记录。");
                    }
                    if (now - lastAliveMs >= ALIVE_INTERVAL_MS) {
                        lastAliveMs = now;
                        write("【存活】" + now() + " 房间=" + roomName() + " 楼层=" + floorNum()
                                + " 主线程滞后=" + since + "ms 动作=" + currentActionName());
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }

    /** 依次实测候选目录，返回第一个真正能写文件的位置。 */
    private static File resolveOutput(StringBuilder probeLog) {
        for (int i = 0; i < CANDIDATE_DIRS.length; i++) {
            String path = CANDIDATE_DIRS[i];
            if (path == null || path.isEmpty()) {
                continue;
            }
            File dir = new File(path);
            boolean ok = false;
            try {
                if (dir.isDirectory()) {
                    File probe = new File(dir, "fuzhou-watchdog-probe.tmp");
                    FileWriter w = new FileWriter(probe, false);
                    w.write("probe");
                    w.flush();
                    w.close();
                    ok = probe.exists() && probe.length() > 0;
                    probe.delete();
                }
            } catch (Throwable ignored) {
                ok = false;
            }
            probeLog.append("  候选 ").append(path).append(" -> ").append(ok ? "可写" : "不可写").append('\n');
            if (ok) {
                return new File(dir, FILE_NAME);
            }
        }
        return null;
    }

    private static void writeStallState(long nowMs, long sinceMs) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("\n==================== 主线程卡住中（尚未恢复） ====================\n");
        sb.append("时间: ").append(format(nowMs)).append('\n');
        sb.append("主线程已连续 ").append(sinceMs).append(" ms 没有完成一次更新/渲染\n");
        appendMemory(sb);
        appendGameState(sb);
        sb.append("（线程栈稍后追加；若始终没有追加，说明卡死时连收集栈都拿不到）\n");
        write(sb.toString());
        logToGame("检测到主线程停顿 " + sinceMs + "ms，已写入现场。");
    }

    private static void spawnStackDump(final long nowMs, final long sinceMs) {
        if (!stackDumpInFlight.compareAndSet(false, true)) {
            return;
        }
        try {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        StringBuilder sb = new StringBuilder(8192);
                        sb.append("---- 线程栈（").append(format(nowMs)).append("，停顿 ")
                                .append(sinceMs).append("ms）----\n");
                        appendStacks(sb);
                        write(sb.toString());
                    } catch (Throwable ignored) {
                    } finally {
                        stackDumpInFlight.set(false);
                    }
                }
            }, "ShengZhu-Watchdog-Stacks");
            t.setDaemon(true);
            t.start();
        } catch (Throwable t) {
            stackDumpInFlight.set(false);
        }
    }

    private static String format(long ms) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT).format(new Date(ms));
        } catch (Throwable t) {
            return String.valueOf(ms);
        }
    }

    private static String now() {
        return format(System.currentTimeMillis());
    }

    private static void appendMemory(StringBuilder sb) {
        try {
            Runtime rt = Runtime.getRuntime();
            sb.append("堆内存: 已用 ").append((rt.totalMemory() - rt.freeMemory()) / 1048576L)
                    .append("MB / 上限 ").append(rt.maxMemory() / 1048576L).append("MB\n");
        } catch (Throwable ignored) {
        }
    }

    private static void appendGameState(StringBuilder sb) {
        try {
            AbstractRoom room = AbstractDungeon.getCurrRoom();
            sb.append("房间: ").append(room == null ? "null" : room.getClass().getName());
            if (room != null) {
                sb.append("  阶段: ").append(String.valueOf(room.phase));
            }
            sb.append("  楼层: ").append(AbstractDungeon.floorNum);
            sb.append("  幕: ").append(AbstractDungeon.actNum).append('\n');
        } catch (Throwable t) {
            sb.append("房间: <读取失败 ").append(t.getClass().getSimpleName()).append(">\n");
        }

        try {
            AbstractPlayer p = AbstractDungeon.player;
            if (p == null) {
                sb.append("玩家: null\n");
            } else {
                sb.append("玩家: ").append(p.getClass().getSimpleName()).append(" [").append(p.name).append(']');
                sb.append("  生命 ").append(p.currentHealth).append('/').append(p.maxHealth);
                sb.append("  格挡 ").append(p.currentBlock);
                sb.append("  贴图 ").append(p.img == null ? "null"
                        : (p.img.getWidth() + "x" + p.img.getHeight()));
                sb.append('\n');
            }
        } catch (Throwable t) {
            sb.append("玩家: <读取失败 ").append(t.getClass().getSimpleName()).append(">\n");
        }

        try {
            AbstractPlayer p = AbstractDungeon.player;
            if (p != null && p.powers != null) {
                sb.append("玩家能力:");
                for (AbstractPower power : p.powers) {
                    if (power == null) {
                        continue;
                    }
                    sb.append(' ').append(power.name).append('x').append(power.amount);
                }
                sb.append('\n');
            }
        } catch (Throwable t) {
            sb.append("玩家能力: <读取失败 ").append(t.getClass().getSimpleName()).append(">\n");
        }

        try {
            AbstractPlayer p = AbstractDungeon.player;
            if (p != null && p.hand != null && p.hand.group != null) {
                sb.append("手牌:");
                for (AbstractCard card : p.hand.group) {
                    if (card == null) {
                        continue;
                    }
                    sb.append(' ').append(card.name).append('[').append(card.cardID).append(']');
                }
                sb.append('\n');
            }
        } catch (Throwable t) {
            sb.append("手牌: <读取失败 ").append(t.getClass().getSimpleName()).append(">\n");
        }

        try {
            GameActionManager am = AbstractDungeon.actionManager;
            if (am == null) {
                sb.append("动作管理器: null\n");
            } else {
                sb.append("当前动作: ").append(nameOf(am.currentAction)).append('\n');
                sb.append("上一个动作: ").append(nameOf(am.previousAction)).append('\n');
                sb.append("动作队列长度: ").append(am.actions == null ? -1 : am.actions.size())
                        .append("  回合已结束: ").append(am.turnHasEnded).append('\n');
                if (am.actions != null) {
                    Object[] pending = am.actions.toArray();
                    int n = Math.min(8, pending.length);
                    for (int i = 0; i < n; i++) {
                        sb.append("  队列[").append(i).append("]: ").append(nameOf(pending[i])).append('\n');
                    }
                }
            }
        } catch (Throwable t) {
            sb.append("动作管理器: <读取失败 ").append(t.getClass().getSimpleName()).append(">\n");
        }

        try {
            MonsterGroup group = AbstractDungeon.getMonsters();
            if (group != null && group.monsters != null) {
                for (AbstractMonster m : group.monsters) {
                    if (m == null) {
                        continue;
                    }
                    sb.append("怪物: ").append(m.getClass().getSimpleName())
                            .append("  生命 ").append(m.currentHealth).append('/').append(m.maxHealth)
                            .append("  意图 ").append(m.intent == null ? "-" : String.valueOf(m.intent))
                            .append('\n');
                }
            }
        } catch (Throwable t) {
            sb.append("怪物: <读取失败 ").append(t.getClass().getSimpleName()).append(">\n");
        }
    }

    private static String nameOf(Object o) {
        return o == null ? "null" : o.getClass().getName();
    }

    private static String roomName() {
        try {
            AbstractRoom room = AbstractDungeon.getCurrRoom();
            return room == null ? "null" : room.getClass().getSimpleName();
        } catch (Throwable t) {
            return "?";
        }
    }

    private static int floorNum() {
        try {
            return AbstractDungeon.floorNum;
        } catch (Throwable t) {
            return -1;
        }
    }

    private static String currentActionName() {
        try {
            GameActionManager am = AbstractDungeon.actionManager;
            if (am == null || am.currentAction == null) {
                return "null";
            }
            return am.currentAction.getClass().getSimpleName();
        } catch (Throwable t) {
            return "?";
        }
    }

    private static void appendStacks(StringBuilder sb) {
        Map<Thread, StackTraceElement[]> all = Thread.getAllStackTraces();
        Thread main = gameThread;
        if (main != null) {
            sb.append("--- 游戏主线程: ").append(main.getName())
                    .append("  状态 ").append(String.valueOf(main.getState())).append(" ---\n");
            appendFrames(sb, all.get(main));
            sb.append('\n');
        } else {
            sb.append("--- 未记录到游戏主线程引用 ---\n");
        }

        sb.append("--- 其他停留在 basicmod / shengzhufix 的线程 ---\n");
        int found = 0;
        for (Map.Entry<Thread, StackTraceElement[]> entry : all.entrySet()) {
            Thread t = entry.getKey();
            if (t == null || t == main) {
                continue;
            }
            StackTraceElement[] frames = entry.getValue();
            if (!hasModFrame(frames)) {
                continue;
            }
            found++;
            sb.append("线程 ").append(t.getName()).append("  状态 ")
                    .append(String.valueOf(t.getState())).append('\n');
            appendFrames(sb, frames);
            sb.append('\n');
        }
        if (found == 0) {
            sb.append("（无）\n");
        }
    }

    private static boolean hasModFrame(StackTraceElement[] frames) {
        if (frames == null) {
            return false;
        }
        for (StackTraceElement frame : frames) {
            String cn = frame.getClassName();
            if (cn != null && (cn.startsWith("basicmod.") || cn.startsWith("shengzhufix."))) {
                return true;
            }
        }
        return false;
    }

    private static void appendFrames(StringBuilder sb, StackTraceElement[] frames) {
        if (frames == null) {
            sb.append("    <无栈信息>\n");
            return;
        }
        int n = Math.min(MAX_FRAMES, frames.length);
        for (int i = 0; i < n; i++) {
            sb.append("    at ").append(frames[i].toString()).append('\n');
        }
        if (frames.length > n) {
            sb.append("    ... 其余 ").append(frames.length - n).append(" 层已省略\n");
        }
    }

    private static synchronized void write(String text) {
        File file = outputFile;
        if (file == null) {
            return;
        }
        BufferedWriter writer = null;
        try {
            if (file.exists() && file.length() > MAX_FILE_BYTES) {
                File prev = new File(file.getParentFile(), PREV_FILE_NAME);
                if (prev.exists()) {
                    prev.delete();
                }
                file.renameTo(prev);
            }
            writer = new BufferedWriter(new FileWriter(file, true), 16384);
            writer.write(text);
            writer.newLine();
            writer.flush();
        } catch (Throwable ignored) {
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /** 顺带写一行到游戏日志，方便从普通日志里交叉验证补丁是否在跑。单独起线程，避免被卡住的主线程拖住。 */
    private static void logToGame(final String message) {
        try {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        basicmod.BasicMod.logger.info("[卡死抓取] " + message);
                    } catch (Throwable ignored) {
                    }
                }
            }, "ShengZhu-Watchdog-Log");
            t.setDaemon(true);
            t.start();
        } catch (Throwable ignored) {
        }
    }
}
