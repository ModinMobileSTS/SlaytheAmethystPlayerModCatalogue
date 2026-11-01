package chronopatch;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.helpers.PowerTip;
import java.util.HashMap;
import java.util.Map;

public class TipZhPatch {
    private static final Map<String, String> TITLES = new HashMap<String, String>();
    private static final Map<String, String> BODIES = new HashMap<String, String>();

    static {
        TITLES.put("Urn", "\u9e1f\u9762\u74ee");
        TITLES.put("Urn+", "\u9e1f\u9762\u74ee+");
        TITLES.put("Mummified Hand", "\u6728\u4e43\u4f0a\u4e4b\u624b");
        TITLES.put("Mummified Hand+", "\u6728\u4e43\u4f0a\u4e4b\u624b+");
        TITLES.put("Ice Cream", "\u51b0\u6fc0\u51cc");
        TITLES.put("Ice Cream+", "\u51b0\u6fc0\u51cc+");
        TITLES.put("Nitrogen", "\u6db2\u6c2e");
        TITLES.put("Nitrogen+", "\u6db2\u6c2e+");
        TITLES.put("Delayed Effects", "\u5ef6\u8fdf\u6548\u679c");
        BODIES.put("Whenever you play a Power, heal #g2 HP. NL #pShatters #pin #b4 #pturns.", "\u6bcf\u5f53\u4f60\u6253\u51fa\u4e00\u5f20 \u80fd\u529b\u724c \uff0c\u6062\u590d #g2 \u70b9\u751f\u547d\u3002 NL #y\u788e\u88c2 \u4e8e #b4 \u56de\u5408\u540e\u3002");
        BODIES.put("Whenever you play a Power, heal #g3 HP. NL #pShatters #pin #b4 #pturns.", "\u6bcf\u5f53\u4f60\u6253\u51fa\u4e00\u5f20 \u80fd\u529b\u724c \uff0c\u6062\u590d #g3 \u70b9\u751f\u547d\u3002 NL #y\u788e\u88c2 \u4e8e #b4 \u56de\u5408\u540e\u3002");
        BODIES.put("Reduces the cost of a card in your hand by #g1 when you play a Power. NL #pShatters #pin #b8 #pturns.", "\u6bcf\u5f53\u4f60\u6253\u51fa\u4e00\u5f20 \u80fd\u529b\u724c \uff0c\u4f60\u624b\u724c\u4e2d\u7684\u4e00\u5f20\u724c\u8d39\u7528\u51cf\u5c11 #g1 \u3002 NL #y\u788e\u88c2 \u4e8e #b8 \u56de\u5408\u540e\u3002");
        BODIES.put("Reduces the cost of a card in your hand by #g2 when you play a Power. NL #pShatters #pin #b8 #pturns.", "\u6bcf\u5f53\u4f60\u6253\u51fa\u4e00\u5f20 \u80fd\u529b\u724c \uff0c\u4f60\u624b\u724c\u4e2d\u7684\u4e00\u5f20\u724c\u8d39\u7528\u51cf\u5c11 #g2 \u3002 NL #y\u788e\u88c2 \u4e8e #b8 \u56de\u5408\u540e\u3002");
        BODIES.put("#yEnergy retains between turns. NL #pShatters #pin #b5 #pturns.", " #y\u80fd\u91cf \u5728\u56de\u5408\u95f4\u4fdd\u7559\u3002 NL #y\u788e\u88c2 \u4e8e #b5 \u56de\u5408\u540e\u3002");
        BODIES.put("At the start of your turn, gain #g1 #yenergy, and #yenergy retains between turns. NL #pShatters #pin #b5 #pturns.", "\u6bcf\u56de\u5408\u5f00\u59cb\u65f6\uff0c\u83b7\u5f97 #g1 \u70b9 #y\u80fd\u91cf \uff0c\u4e14 #y\u80fd\u91cf \u5728\u56de\u5408\u95f4\u4fdd\u7559\u3002 NL #y\u788e\u88c2 \u4e8e #b5 \u56de\u5408\u540e\u3002");
        BODIES.put("All #yreplicas last #g1 turns longer. NL #pShatters #pin #b6 #pturns.", "\u6240\u6709 #y\u590d\u523b\u54c1 \u7684\u6301\u7eed\u65f6\u95f4\u5ef6\u957f #g1 \u56de\u5408\u3002 NL #y\u788e\u88c2 \u4e8e #b6 \u56de\u5408\u540e\u3002");
        BODIES.put("All #yreplicas last #g2 turns longer. NL #pShatters #pin #b6 #pturns.", "\u6240\u6709 #y\u590d\u523b\u54c1 \u7684\u6301\u7eed\u65f6\u95f4\u5ef6\u957f #g2 \u56de\u5408\u3002 NL #y\u788e\u88c2 \u4e8e #b6 \u56de\u5408\u540e\u3002");
        BODIES.put("Delayed Effects include Bite Command, Rage Command, Hands Up, Wake Up Call, and The Bomb.", "\u5ef6\u8fdf\u6548\u679c\u5305\u62ec\uff1a \u566c\u65f6\u6307\u4ee4 \u3001 \u6012\u6f6e\u6307\u4ee4 \u3001 \u7f34\u68b0 \u3001 \u5524\u9192 \u4e0e \u70b8\u5f39 \u3002");
    }

    private static void fix(PowerTip powerTip) {
        try {
            String string;
            if (powerTip.header != null && (string = TITLES.get(powerTip.header)) != null) {
                powerTip.header = string;
            }
            if (powerTip.body != null && (string = BODIES.get(powerTip.body)) != null) {
                powerTip.body = string;
            }
        } catch (Throwable throwable) {}
    }

    @SpirePatch(clz = PowerTip.class, method = "<ctor>", paramtypez = {String.class, String.class})
    public static class Ctor2 {
        @SpirePostfixPatch
        public static void Postfix(PowerTip powerTip, String string, String string2) {
            TipZhPatch.fix(powerTip);
        }
    }

    @SpirePatch(clz = PowerTip.class, method = "<ctor>", paramtypez = {String.class, String.class, Texture.class})
    public static class Ctor3 {
        @SpirePostfixPatch
        public static void Postfix(PowerTip powerTip, String string, String string2, Texture texture) {
            TipZhPatch.fix(powerTip);
        }
    }

    @SpirePatch(clz = PowerTip.class, method = "<ctor>", paramtypez = {String.class, String.class, TextureAtlas.AtlasRegion.class})
    public static class Ctor4 {
        @SpirePostfixPatch
        public static void Postfix(PowerTip powerTip, String string, String string2, TextureAtlas.AtlasRegion atlasRegion) {
            TipZhPatch.fix(powerTip);
        }
    }
}
