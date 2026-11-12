package frierenaudiofix;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.megacrit.cardcrawl.audio.MainMusic;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 公共工具：解析/修复音乐资源路径。
 *
 * 崩溃根因：原版 TempMusic.getSong(String) 对未知 key 的兜底是
 *     MainMusic.newMusic("audio/music/" + key)
 * 这个路径既没有扩展名、文件也不存在，于是 libGDX 的 OpenALAudio.newMusic
 * 无法判断解码器，抛出 GdxRuntimeException("Unknown file extension for music: ...")。
 *
 * 这里做两件事：
 * 1) 把 FrierenMod 的自定义音乐键映射回它自己 jar 里的 mp3；
 * 2) 遇到"无法识别的扩展名/找不到文件"的路径时，替换成原版一定存在的 ogg。
 */
public final class MusicPathFix {

    public static final Logger logger = LogManager.getLogger("FrierenAudioFix");

    /** 原版游戏音乐目录 */
    public static final String VANILLA_DIR = "audio/music/";
    /** FrierenMod 自己的音乐目录（在 FrierenMod.jar 内） */
    public static final String FRIEREN_DIR = "FrierenModResources/audio/sound/";

    /** 原版一定存在的兜底曲目（普通场景） */
    public static final String SAFE_FALLBACK = VANILLA_DIR + "STS_Level1_NewMix_v1.ogg";
    /** 原版一定存在的兜底曲目（死亡/结算场景，较长且适合循环） */
    public static final String DEATH_FALLBACK = VANILLA_DIR + "STS_Credits_v5.ogg";

    private MusicPathFix() {
    }

    /** libGDX 能识别的音频扩展名 */
    public static boolean hasKnownExtension(String path) {
        if (path == null) {
            return false;
        }
        String p = path.toLowerCase(Locale.ROOT);
        return p.endsWith(".ogg") || p.endsWith(".mp3") || p.endsWith(".wav");
    }

    /** 路径是否像死亡/结算用的音乐 */
    public static boolean looksLikeDeath(String path) {
        if (path == null) {
            return false;
        }
        String p = path.toLowerCase(Locale.ROOT);
        return p.contains("death") || p.contains("die") || p.contains("stinger") || p.contains("gameover");
    }

    /** 去掉最后一个扩展名（没有目录分隔混淆） */
    public static String stripExtension(String path) {
        if (path == null) {
            return null;
        }
        int slash = path.lastIndexOf('/');
        int dot = path.lastIndexOf('.');
        if (dot > slash) {
            return path.substring(0, dot);
        }
        return path;
    }

    /**
     * 尝试真正创建一个 Music。只有能被 libGDX 成功加载的路径才算成功，
     * 这样就不必依赖 FileHandle.exists() 在不同后端/Android 兼容层上的行为差异。
     */
    public static Music tryLoad(String path) {
        if (path == null || !hasKnownExtension(path)) {
            return null;
        }
        if (Gdx.audio == null) {
            // 交给原版（MainMusic.newMusic 会返回 MockMusic）处理
            return null;
        }
        try {
            return MainMusic.newMusic(path);
        } catch (Throwable t) {
            logger.info("[FrierenAudioFix] 跳过无法加载的音乐: " + path
                    + " (" + t.getClass().getSimpleName() + ": " + t.getMessage() + ")");
            return null;
        }
    }

    private static Music firstPlayable(List<String> candidates) {
        for (String c : candidates) {
            Music m = tryLoad(c);
            if (m != null) {
                logger.info("[FrierenAudioFix] 使用音乐: " + c);
                return m;
            }
        }
        return null;
    }

    /**
     * 把 FrierenMod 的自定义音乐键（如 Frieren_DeathStinger）解析成它 jar 内的 mp3。
     * 只处理以 "Frieren" 开头的键，避免抢走其它 Mod 的自定义音乐。
     *
     * @return 可播放的 Music；无法确定时返回 null，让后续逻辑（Mod 自己的注入 / 原版兜底）继续跑。
     */
    public static Music resolveFrierenKey(String key) {
        if (key == null) {
            return null;
        }
        if (!key.toLowerCase(Locale.ROOT).startsWith("frieren")) {
            return null;
        }
        List<String> candidates = new ArrayList<String>();
        if (hasKnownExtension(key)) {
            candidates.add(FRIEREN_DIR + key);
            candidates.add(VANILLA_DIR + key);
        } else {
            String[] exts = {".mp3", ".ogg", ".wav"};
            for (String ext : exts) {
                candidates.add(FRIEREN_DIR + key + ext);
            }
            for (String ext : exts) {
                candidates.add(VANILLA_DIR + key + ext);
            }
        }
        Music m = firstPlayable(candidates);
        if (m == null) {
            logger.warn("[FrierenAudioFix] 找不到自定义音乐资源: " + key + "，交给后续逻辑处理");
        }
        return m;
    }

    /**
     * 修复不可用的音乐路径：
     * 1) 尝试补全扩展名后加载；
     * 2) 仍然失败则替换成原版一定存在的 ogg，保证不会抛 "Unknown file extension for music"。
     *
     * @return 替换后的 Music；只有在连原版资源都无法创建时返回 null。
     */
    public static Music repairPath(String path) {
        if (path == null) {
            return tryLoad(SAFE_FALLBACK);
        }

        List<String> candidates = new ArrayList<String>();
        String[] exts = {".ogg", ".mp3", ".wav"};
        for (String ext : exts) {
            candidates.add(path + ext);
        }
        String base = stripExtension(path);
        if (base != null && !base.equals(path)) {
            for (String ext : exts) {
                candidates.add(base + ext);
            }
        }
        Music m = firstPlayable(candidates);
        if (m != null) {
            logger.warn("[FrierenAudioFix] 已修复音乐路径: " + path + " -> 补全扩展名");
            return m;
        }

        String fallback = looksLikeDeath(path) ? DEATH_FALLBACK : SAFE_FALLBACK;
        m = tryLoad(fallback);
        if (m != null) {
            logger.warn("[FrierenAudioFix] 音乐路径不可用，已替换为原版音乐: " + path + " -> " + fallback);
        }
        return m;
    }
}
