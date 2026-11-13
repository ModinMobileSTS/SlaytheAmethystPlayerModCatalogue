package tkfix;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.Animation;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.Bone;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.Slot;
import com.esotericsoftware.spine.attachments.Attachment;
import com.esotericsoftware.spine.attachments.RegionAttachment;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.characters.Ironclad;
import com.megacrit.cardcrawl.characters.TheSilent;
import com.megacrit.cardcrawl.core.AbstractCreature;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import timekeepermod.patch.SkinSelectScreen_green;
import timekeepermod.patch.SkinSelectScreen_red;

/**
 * 给原版角色套用母模组皮肤的核心逻辑。
 * <p>
 * 母模组在 Ironclad / TheSilent 构造函数 Postfix 里反射换骨架，异常被吞掉，
 * 而且之后还会被还原成默认外观；「默认」皮肤本身又长得和原版一样，
 * 所以皮肤一旦被还原，游戏里看起来就是「皮肤没了」。
 * 这里按「皮肤 → 备用皮肤 → 母模组自带默认」顺序逐个尝试，
 * 用骨架对象是否真的被替换来判断成功，并在游戏内持续复查。
 */
public final class SkinApply {
    private static final String EK_ATLAS = "timekeepermod/images/monsters/EndlessKing/EK2.atlas";
    private static final String EK_JSON = "timekeepermod/images/monsters/EndlessKing/ek237.json";
    private static final String EK_ATLAS_BACKUP = "timekeepermod/images/monsters/EndlessKing/EK.atlas";
    private static final String EK_JSON_BACKUP = "timekeepermod/images/monsters/EndlessKing/ek37.json";
    private static final String EK_DEFAULT_ATLAS = "timekeepermod/images/monsters/EndlessKing/default.atlas";
    private static final String EK_DEFAULT_JSON = "timekeepermod/images/monsters/EndlessKing/default.json";

    private static final String FK_ATLAS = "timekeepermod/images/monsters/FlowerKing/FK2.atlas";
    private static final String FK_JSON = "timekeepermod/images/monsters/FlowerKing/fk237.json";
    private static final String FK_ATLAS_BACKUP = "timekeepermod/images/monsters/FlowerKing/FK.atlas";
    private static final String FK_JSON_BACKUP = "timekeepermod/images/monsters/FlowerKing/fk37.json";
    private static final String FK_DEFAULT_ATLAS = "timekeepermod/images/monsters/FlowerKing/default.atlas";
    private static final String FK_DEFAULT_JSON = "timekeepermod/images/monsters/FlowerKing/default.json";

    private SkinApply() {
    }

    public static boolean isVanillaSkinCharacter(AbstractPlayer p) {
        return p instanceof Ironclad || p instanceof TheSilent;
    }

    public static int liveIndex(boolean ironclad) {
        return ironclad ? SkinFixConfig.indexRed() : SkinFixConfig.indexGreen();
    }

    public static int savedIndex(boolean ironclad) {
        return SkinFixConfig.savedIndex(ironclad);
    }

    /**
     * 母模组三个界面共用同一个存档键 "TimeCutter:skin"，读档时可能把原版角色的序号覆盖成 0；
     * 独立配置里记录过非默认皮肤时以它为准。
     */
    public static int resolveIndex(boolean ironclad) {
        int live = liveIndex(ironclad);
        int saved = savedIndex(ironclad);
        if (saved > 0 && live <= 0) {
            return saved;
        }
        return live;
    }

    /** 当前应该显示的皮肤：default / EK（苦痛恶魔）/ FK（花烛君主）。 */
    public static String charPath(boolean ironclad) {
        int idx = resolveIndex(ironclad);
        String fromMod = readFromMod(ironclad, idx);
        if (fromMod != null) {
            return fromMod;
        }
        return idx > 0 ? (ironclad ? "EK" : "FK") : "default";
    }

    private static String readFromMod(boolean ironclad, int idx) {
        try {
            if (idx < 0) {
                return null;
            }
            if (ironclad) {
                SkinSelectScreen_red screen = SkinSelectScreen_red.Inst;
                if (screen == null) {
                    return null;
                }
                int old = screen.index;
                try {
                    screen.index = idx;
                    return SkinSelectScreen_red.getSkin_red().charPath;
                } finally {
                    screen.index = old;
                }
            }
            SkinSelectScreen_green screen = SkinSelectScreen_green.Inst;
            if (screen == null) {
                return null;
            }
            int old = screen.index;
            try {
                screen.index = idx;
                return SkinSelectScreen_green.getSkin_green().charPath;
            } finally {
                screen.index = old;
            }
        } catch (Throwable t) {
            Diag.error("charPath", t);
            return null;
        }
    }

    public static void ensureOnce(AbstractPlayer p, String tag) {
        ensureOnce(p, tag, false);
    }

    /**
     * 确认游戏内骨架是所选皮肤。
     * force=true 时无条件重设一次：图集可能还是皮肤的、骨架却被还原成了原版，
     * 这种情况只看图集判断不出来。
     */
    public static void ensureOnce(AbstractPlayer p, String tag, boolean force) {
        if (p == null) {
            return;
        }
        boolean ironclad = p instanceof Ironclad;
        if (!ironclad && !(p instanceof TheSilent)) {
            return;
        }
        String path = charPath(ironclad);
        Diag.log(tag, "cls=" + p.getClass().getSimpleName() + " live=" + liveIndex(ironclad)
                + " saved=" + savedIndex(ironclad) + " path=" + path + " " + describe(p)
                + " force=" + force);
        if (!"EK".equals(path) && !"FK".equals(path)) {
            return;
        }
        if (force) {
            Diag.log(tag + ".before", describeDraw(p));
            applyBest(p, ironclad, tag);
            Diag.log(tag + ".after", describeDraw(p));
            return;
        }
        if (hasMarker(p, ironclad)) {
            Diag.log(tag, "already=" + path);
            return;
        }
        applyBest(p, ironclad, tag);
    }

    /** 运行中定期复查：皮肤被还原就补回来。 */
    public static void guard(AbstractPlayer p) {
        try {
            boolean ironclad = p instanceof Ironclad;
            if (!ironclad && !(p instanceof TheSilent)) {
                return;
            }
            String path = charPath(ironclad);
            if (!"EK".equals(path) && !"FK".equals(path)) {
                return;
            }
            if (hasMarker(p, ironclad)) {
                return;
            }
            Diag.log("guard", "reapply cls=" + p.getClass().getSimpleName() + " path=" + path
                    + " " + describe(p) + " " + describeDraw(p));
            applyBest(p, ironclad, "guard");
        } catch (Throwable t) {
            Diag.error("guard", t);
        }
    }

    public static void applyBest(AbstractPlayer p, boolean ironclad, String tag) {
        String[][] candidates = candidates(ironclad);
        IdentityHashMap<Object, Boolean> atlases = new IdentityHashMap<Object, Boolean>();
        atlases.put(get(p, "atlas"), Boolean.TRUE);
        boolean ok = false;
        for (int i = 0; i < candidates.length; i++) {
            String[] c = candidates[i];
            Object before = get(p, "skeleton");
            try {
                Method m = AbstractCreature.class.getDeclaredMethod(
                        "loadAnimation", String.class, String.class, float.class);
                m.setAccessible(true);
                m.invoke(p, c[0], c[1], Float.parseFloat(c[2]));
            } catch (Throwable t) {
                Diag.log(tag, "try" + i + " " + c[0] + " threw " + describe(unwrap(t)));
                atlases.put(get(p, "atlas"), Boolean.TRUE);
                continue;
            }
            atlases.put(get(p, "atlas"), Boolean.TRUE);
            Object after = get(p, "skeleton");
            if (after != null && after != before) {
                Diag.log(tag, "ok" + i + " " + c[0] + " " + describe(p) + " " + describeDraw(p));
                ok = true;
                break;
            }
            Diag.log(tag, "try" + i + " " + c[0] + " notReplaced");
        }
        if (!ok) {
            Diag.log(tag, "FAILED all candidates " + describe(p));
        }
        playIdle(p, tag);
        Object keep = get(p, "atlas");
        for (Object o : atlases.keySet()) {
            if (o == null || o == keep || !(o instanceof TextureAtlas)) {
                continue;
            }
            try {
                ((TextureAtlas) o).dispose();
            } catch (Throwable t) {
                Diag.error(tag + ".dispose", unwrap(t));
            }
        }
    }

    /** 让骨架播放待机动作；优先 Idle，没有就退回第一个动作。 */
    public static void playIdle(AbstractPlayer p, String tag) {
        try {
            Object o = get(p, "skeleton");
            if (!(o instanceof Skeleton)) {
                Diag.log(tag + ".idle", "skeleton=null");
                return;
            }
            SkeletonData data = ((Skeleton) o).getData();
            if (data == null) {
                Diag.log(tag + ".idle", "data=null");
                return;
            }
            StringBuilder names = new StringBuilder();
            for (int i = 0; i < data.getAnimations().size; i++) {
                Animation a = data.getAnimations().get(i);
                if (i > 0) {
                    names.append(',');
                }
                names.append(a.getName());
            }
            String name = data.findAnimation("Idle") != null ? "Idle" : null;
            if (name == null && data.getAnimations().size > 0) {
                name = data.getAnimations().get(0).getName();
            }
            if (name == null) {
                Diag.log(tag + ".idle", "noAnimation anims=[]");
                return;
            }
            AnimationState.TrackEntry e = p.state.setAnimation(0, name, true);
            if (e != null) {
                e.setTime(e.getEndTime() * MathUtils.random());
                e.setTimeScale(1.1f);
            }
            Diag.log(tag + ".idle", "use=" + name + " anims=[" + names + "]");
        } catch (Throwable t) {
            Diag.error(tag + ".idle", unwrap(t));
        }
    }

    private static String[][] candidates(boolean ironclad) {
        if (ironclad) {
            return new String[][]{
                    {EK_ATLAS, EK_JSON, "3.0"},
                    {EK_ATLAS_BACKUP, EK_JSON_BACKUP, "3.0"},
                    {EK_DEFAULT_ATLAS, EK_DEFAULT_JSON, "1.0"}
            };
        }
        return new String[][]{
                {FK_ATLAS, FK_JSON, "2.3"},
                {FK_ATLAS_BACKUP, FK_JSON_BACKUP, "2.3"},
                {FK_DEFAULT_ATLAS, FK_DEFAULT_JSON, "1.0"}
        };
    }

    /** 皮肤图集里的特有区域名，用来判断骨架是不是真的换成了皮肤。 */
    public static boolean hasMarker(AbstractCreature c, boolean ironclad) {
        return hasRegion(c, ironclad ? "L_wing" : "Light1");
    }

    private static boolean hasRegion(AbstractCreature c, String name) {
        try {
            Object o = get(c, "atlas");
            if (!(o instanceof TextureAtlas)) {
                return false;
            }
            com.badlogic.gdx.utils.Array regions = ((TextureAtlas) o).getRegions();
            for (int i = 0; i < regions.size; i++) {
                Object r = regions.get(i);
                if (r instanceof TextureAtlas.AtlasRegion && name.equals(((TextureAtlas.AtlasRegion) r).name)) {
                    return true;
                }
            }
            return false;
        } catch (Throwable t) {
            Diag.error("hasRegion", t);
            return false;
        }
    }

    public static String describe(AbstractCreature c) {
        try {
            Object o = get(c, "atlas");
            if (!(o instanceof TextureAtlas)) {
                return "atlas=null";
            }
            com.badlogic.gdx.utils.Array regions = ((TextureAtlas) o).getRegions();
            return "regions=" + regions.size
                    + " L_wing=" + hasRegion(c, "L_wing")
                    + " Light1=" + hasRegion(c, "Light1");
        } catch (Throwable t) {
            Diag.error("describe", t);
            return "describeError";
        }
    }

    /** 诊断：绘制相关状态（位置、缩放、透明度、当前动作、区域附件是否有效）。 */
    public static String describeDraw(AbstractPlayer p) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("draw=").append(p.drawX).append(',').append(p.drawY);
            Object o = get(p, "skeleton");
            if (!(o instanceof Skeleton)) {
                return sb.append(" skeleton=null").toString();
            }
            Skeleton sk = (Skeleton) o;
            sb.append(" pos=").append(sk.getX()).append(',').append(sk.getY());
            Bone root = sk.getRootBone();
            sb.append(" rootScale=").append(root == null ? -1f : root.getWorldScaleX());
            Color c = sk.getColor();
            sb.append(" a=").append(c == null ? -1f : c.a);
            try {
                AnimationState.TrackEntry e = p.state.getCurrent(0);
                sb.append(" anim=").append(e == null ? "null" : e.getAnimation().getName());
                sb.append(" t=").append(e == null ? -1f : e.getTime());
            } catch (Throwable t) {
                sb.append(" animError");
            }
            int total = 0;
            int missing = 0;
            com.badlogic.gdx.utils.Array slots = sk.getSlots();
            for (int i = 0; i < slots.size; i++) {
                Object s = slots.get(i);
                if (!(s instanceof Slot)) {
                    continue;
                }
                Attachment a = ((Slot) s).getAttachment();
                if (a instanceof RegionAttachment) {
                    total++;
                    if (((RegionAttachment) a).getRegion() == null) {
                        missing++;
                    }
                }
            }
            sb.append(" attach=").append(total).append(" noRegion=").append(missing);
            sb.append(" bones=").append(sk.getData() == null ? -1 : sk.getData().getBones().size);
            return sb.toString();
        } catch (Throwable t) {
            Diag.error("describeDraw", t);
            return "describeDrawError";
        }
    }

    private static Object get(AbstractCreature c, String field) {
        try {
            return ReflectionHacks.getPrivate(c, AbstractCreature.class, field);
        } catch (Throwable t) {
            Diag.error("get." + field, t);
            return null;
        }
    }

    private static Throwable unwrap(Throwable t) {
        if (t instanceof InvocationTargetException && ((InvocationTargetException) t).getCause() != null) {
            return ((InvocationTargetException) t).getCause();
        }
        return t;
    }

    private static String describe(Throwable t) {
        StringBuilder sb = new StringBuilder();
        sb.append(t.getClass().getName()).append(": ").append(t.getMessage());
        StackTraceElement[] st = t.getStackTrace();
        int n = st == null ? 0 : Math.min(4, st.length);
        for (int i = 0; i < n; i++) {
            sb.append(" @ ").append(st[i]);
        }
        return sb.toString();
    }
}
