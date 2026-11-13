package tkfix;

import basemod.ReflectionHacks;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.esotericsoftware.spine.Animation;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.SkeletonJson;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import java.lang.reflect.Constructor;

/**
 * 启动自检：
 * 1. 把皮肤图集 / 骨架文件逐个试着加载一遍；
 * 2. 真的创建一个铁甲战士 / 静默猎手实例，走一遍「游戏内皮肤修复」流程，
 * 确认进入游戏后骨架确实换成了所选皮肤。
 * 结果写进 diag.txt，任何异常都只记录，不影响游戏。
 */
public final class SelfTest {
    private static final String EK_DIR = "timekeepermod/images/monsters/EndlessKing/";
    private static final String FK_DIR = "timekeepermod/images/monsters/FlowerKing/";

    private SelfTest() {
    }

    public static void run() {
        try {
            Diag.log("test", "start scale=" + Settings.scale + " renderScale=" + Settings.renderScale);
            probe("EK2", EK_DIR + "EK2.atlas", EK_DIR + "ek237.json", 3.0f);
            probe("EKdef", EK_DIR + "default.atlas", EK_DIR + "default.json", 1.0f);
            probe("FK2", FK_DIR + "FK2.atlas", FK_DIR + "fk237.json", 2.3f);
            probe("FKdef", FK_DIR + "default.atlas", FK_DIR + "default.json", 1.0f);
            characterCheck("Ironclad", "The Ironclad");
            characterCheck("TheSilent", "The Silent");
            Diag.log("test", "done");
        } catch (Throwable t) {
            Diag.error("test", t);
        }
    }

    private static void probe(String tag, String atlasPath, String jsonPath, float scale) {
        TextureAtlas atlas = null;
        try {
            FileHandle f = Gdx.files.internal(atlasPath);
            Diag.log("test", tag + " exists=" + f.exists() + " len=" + (f.exists() ? f.length() : -1));
            atlas = new TextureAtlas(f);
            Diag.log("test", tag + " atlasOK regions=" + atlas.getRegions().size);
        } catch (Throwable t) {
            Diag.log("test", tag + " atlasFAIL " + desc(t));
            return;
        }
        try {
            SkeletonJson json = new SkeletonJson(atlas);
            json.setScale(Settings.renderScale / scale);
            SkeletonData data = json.readSkeletonData(Gdx.files.internal(jsonPath));
            StringBuilder names = new StringBuilder();
            for (int i = 0; i < data.getAnimations().size; i++) {
                Animation a = data.getAnimations().get(i);
                if (i > 0) {
                    names.append(',');
                }
                names.append(a.getName());
            }
            Diag.log("test", tag + " jsonOK bones=" + data.getBones().size
                    + " anims=[" + names + "]"
                    + " idle=" + (data.findAnimation("Idle") != null));
        } catch (Throwable t) {
            Diag.log("test", tag + " jsonFAIL " + desc(t));
        } finally {
            try {
                atlas.dispose();
            } catch (Throwable ignored) {
                // 忽略
            }
        }
    }

    /** 真正创建一个原版角色实例，验证游戏内的皮肤修复能生效。 */
    private static void characterCheck(String className, String name) {
        AbstractPlayer p = null;
        try {
            Class<?> cls = Class.forName("com.megacrit.cardcrawl.characters." + className);
            Constructor<?> ctor = cls.getDeclaredConstructor(String.class);
            ctor.setAccessible(true);
            p = (AbstractPlayer) ctor.newInstance(name);
            SkinApply.ensureOnce(p, "selftest." + className);
        } catch (Throwable t) {
            Diag.error("selftest." + className, t);
        } finally {
            if (p != null) {
                try {
                    Object atlas = ReflectionHacks.getPrivate(p, AbstractCreature.class, "atlas");
                    if (atlas instanceof TextureAtlas) {
                        ((TextureAtlas) atlas).dispose();
                    }
                } catch (Throwable ignored) {
                    // 忽略
                }
            }
        }
    }

    private static String desc(Throwable t) {
        StringBuilder sb = new StringBuilder();
        sb.append(t.getClass().getSimpleName()).append(": ").append(t.getMessage());
        StackTraceElement[] st = t.getStackTrace();
        int n = st == null ? 0 : Math.min(3, st.length);
        for (int i = 0; i < n; i++) {
            sb.append(" @ ").append(st[i]);
        }
        return sb.toString();
    }
}
