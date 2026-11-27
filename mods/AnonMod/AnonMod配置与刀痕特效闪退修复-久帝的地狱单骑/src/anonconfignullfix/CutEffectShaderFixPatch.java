package anonconfignullfix;

import cards.others.vfx.CutEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import java.lang.reflect.Field;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * CutEffect 是 LittleStar（小星星）卡片播放的“刀痕切割屏幕”特效。
 * 它的 fragment shader 是 shaders/CiYuanZhan/cuts.fs，其中
 *
 *     if (u_progress[i] &lt; 0) { continue; }
 *
 * 用 float 和 int 常量做比较。这在 GLSL ES（安卓 GLES 驱动，尤其 Adreno）里是非法写法，
 * 编译器直接报：
 *
 *     0:60: S0001: Cannot compare 'float' with 'int'
 *
 * 而 CutEffect.render 在拿到未编译成功的 ShaderProgram 后直接
 *
 *     throw new RuntimeException(cutShader.getLog());
 *
 * 渲染线程里没人接这个异常，于是 CardCrawlGame.render 抛错 -> Game crashed。
 *
 * 这里做两件事：
 *   1. 用一份把该比较改成 `&lt; 0.0` 的兼容版着色器替换掉 CutEffect 的静态 cutShader，
 *      让特效按原设计正常渲染（原 render 只要发现 cutShader 非 null 就会跳过自己的创建逻辑）；
 *   2. 万一这台设备的编译器仍然不接受，就把该特效标记为已完成并跳过渲染，
 *      保证只丢特效、绝不闪退，同时把编译日志写进日志文件方便继续修。
 */
@SpirePatch2(clz = CutEffect.class, method = "render", paramtypez = {SpriteBatch.class})
public class CutEffectShaderFixPatch {

    private static final Logger logger = LogManager.getLogger("CutEffectFix");

    /** 与 AnonMod 里 shaders/CiYuanZhan/cut.vs 内容一致，标准 SpriteBatch 顶点着色器。 */
    private static final String VERTEX_SHADER =
            "uniform mat4 u_projTrans;\n" +
            "attribute vec4 a_position;\n" +
            "attribute vec2 a_texCoord0;\n" +
            "attribute vec4 a_color;\n" +
            "varying vec4 v_color;\n" +
            "varying vec2 v_texCoord;\n" +
            "uniform vec2 u_viewportInverse;\n" +
            "void main() {\n" +
            "    gl_Position = u_projTrans * a_position;\n" +
            "    v_texCoord = a_texCoord0;\n" +
            "    v_color = a_color;\n" +
            "}\n";

    /**
     * shaders/CiYuanZhan/cuts.fs 的兼容版：内容保持原样，
     * 只把 `u_progress[i] < 0` 改成 `u_progress[i] < 0.0`（float 与 float 比较）。
     */
    private static final String FRAGMENT_SHADER =
            "//SpriteBatch will use texture unit 0\n" +
            "uniform sampler2D u_texture;\n" +
            "uniform float u_scale;\n" +
            "uniform vec2 u_screenSize;\n" +
            "#define BATCH_SIZE 10\n" +
            "uniform vec2 u_point[BATCH_SIZE];\n" +
            "uniform float u_direction[BATCH_SIZE];\n" +
            "uniform float u_progress[BATCH_SIZE];\n" +
            "varying vec4 v_color;\n" +
            "varying vec2 v_texCoord;\n" +
            "#define THRESHOLD 0.5\n" +
            "#define CUT_TIME 0.05\n" +
            "#define CUT_THRESHOLD (2.5 * u_scale)\n" +
            "#define CUT_THRESHOLD_SMALL (1.5 * u_scale)\n" +
            "#define MOVE_X_BASE 30.0\n" +
            "#define MOVE_X_VAR 18.0\n" +
            "#define MOVE_X_RAND(n) (MOVE_X_BASE + MOVE_X_VAR * (rand(n, i) - 0.5))\n" +
            "#define MOVE_Y_VAR 40.0\n" +
            "#define MOVE_Y_RAND(n) (MOVE_Y_VAR * (rand(n, i) - 0.5))\n" +
            "#define SCALE_I_RAND(n) (0.1 * (rand(n, i) - 0.6))\n" +
            "#define SCALE_D_RAND(n) (0.06 * (rand(n, i) - 0.5))\n" +
            "#define TWP_SIZE 20\n" +
            "struct TexCoordWithPower {\n" +
            "    vec2 texCoord;\n" +
            "    float power;\n" +
            "} twp[TWP_SIZE];\n" +
            "int twpSize = 0;\n" +
            "float rand(float v, int i) {\n" +
            "    vec2 co = u_point[i] + u_direction[i] + v;\n" +
            "    return fract(sin(dot(co, vec2(12.9898, 78.233))) * 43758.5453);\n" +
            "}\n" +
            "void main() {\n" +
            "    twpSize = 1;\n" +
            "    twp[0].texCoord = v_texCoord;\n" +
            "    twp[0].power = 1.0;\n" +
            "    for (int i = BATCH_SIZE - 1; i >= 0; i--) {\n" +
            "        if (u_progress[i] < 0.0) {\n" +
            "            continue;\n" +
            "        }\n" +
            "        int currentSize = twpSize;\n" +
            "        for (int j = 0; j < currentSize; j++) {\n" +
            "            if (twp[j].power == 0.0) {\n" +
            "                continue;\n" +
            "            }\n" +
            "            vec2 screen = twp[j].texCoord * u_screenSize;\n" +
            "            vec2 cutDirection = vec2(cos(u_direction[i]), sin(u_direction[i]));\n" +
            "            vec2 perpendicularDirection = vec2(-sin(u_direction[i]), cos(u_direction[i]));\n" +
            "            mat2 cutMat = mat2(cutDirection, perpendicularDirection);\n" +
            "            vec2 cut = (screen - u_point[i]) * cutMat;\n" +
            "            vec2 move1 = vec2(MOVE_X_RAND(0.0), MOVE_Y_RAND(1.0));\n" +
            "            mat2 scale1 = mat2(SCALE_I_RAND(2.0), SCALE_D_RAND(3.0), SCALE_D_RAND(4.0), SCALE_I_RAND(5.0));\n" +
            "            vec2 move2 = vec2(-MOVE_X_RAND(6.0), MOVE_Y_RAND(7.0));\n" +
            "            mat2 scale2 = mat2(SCALE_I_RAND(8.0), SCALE_D_RAND(9.0), SCALE_D_RAND(10.0), SCALE_I_RAND(11.0));\n" +
            "            float progress = u_progress[i] < 0.5 ? 2.0 * u_progress[i] * u_progress[i] : 1.0 - 2.0 * (u_progress[i] - 1.0) * (u_progress[i] - 1.0);\n" +
            "            if (abs(cut.y) < THRESHOLD && twpSize < TWP_SIZE) {\n" +
            "                vec2 screen1 = cutMat * (cut + (move1 + cut * scale1) * u_scale * progress) + u_point[i];\n" +
            "                vec2 screen2 = cutMat * (cut + (move2 + cut * scale2) * u_scale * progress) + u_point[i];\n" +
            "                float d = (cut.y + THRESHOLD) / (2.0 * THRESHOLD);\n" +
            "                twp[twpSize].texCoord = screen2 / u_screenSize;\n" +
            "                twp[twpSize].power = d * twp[j].power;\n" +
            "                twpSize++;\n" +
            "                twp[j].texCoord = screen1 / u_screenSize;\n" +
            "                twp[j].power = (1.0 - d) * twp[j].power;\n" +
            "            } else {\n" +
            "                if (cut.y < 0.0) {\n" +
            "                    screen = cutMat * (cut + (move1 + cut * scale1) * u_scale * progress) + u_point[i];\n" +
            "                } else {\n" +
            "                    screen = cutMat * (cut + (move2 + cut * scale2) * u_scale * progress) + u_point[i];\n" +
            "                }\n" +
            "                twp[j].texCoord = screen / u_screenSize;\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "    vec4 bgColor = vec4(0.0, 0.0, 0.0, 0.0);\n" +
            "    for (int i = 0; i < TWP_SIZE; i++) {\n" +
            "        if (i >= twpSize) {\n" +
            "            continue;\n" +
            "        }\n" +
            "        bgColor += texture2D(u_texture, twp[i].texCoord) * twp[i].power;\n" +
            "    }\n" +
            "    for (int i = 0; i < BATCH_SIZE; i++) {\n" +
            "        if (u_progress[i] < 0.0) {\n" +
            "            continue;\n" +
            "        }\n" +
            "        vec2 screen = v_texCoord * u_screenSize;\n" +
            "        vec2 cutDirection = vec2(cos(u_direction[i]), sin(u_direction[i]));\n" +
            "        vec2 perpendicularDirection = vec2(-sin(u_direction[i]), cos(u_direction[i]));\n" +
            "        mat2 cutMat = mat2(cutDirection, perpendicularDirection);\n" +
            "        vec2 cut = (screen - u_point[i]) * cutMat;\n" +
            "        if (u_progress[i] < CUT_TIME && abs(cut.y) < CUT_THRESHOLD) {\n" +
            "            vec4 white = vec4(1.0, 1.0, 1.0, 1.0);\n" +
            "            float time = u_progress[i] / CUT_TIME;\n" +
            "            float timeFactor = 1.0 - time * time;\n" +
            "            float spaceFactor = abs(cut.y) < CUT_THRESHOLD_SMALL ? 1.0 : (CUT_THRESHOLD - abs(cut.y)) / (CUT_THRESHOLD - CUT_THRESHOLD_SMALL);\n" +
            "            float d = timeFactor * spaceFactor;\n" +
            "            bgColor = bgColor * (1.0 - d) + white * d;\n" +
            "        }\n" +
            "    }\n" +
            "    gl_FragColor = bgColor;\n" +
            "}\n";

    private static boolean tried = false;
    private static boolean unusable = false;

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(CutEffect __instance) {
        if (!tried) {
            tried = true;
            try {
                ShaderProgram program = new ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER);
                if (program.isCompiled()) {
                    setStaticShader(program);
                    logger.info("[CutEffectFix] 刀痕特效着色器已替换为 GLES 兼容版本");
                } else {
                    unusable = true;
                    logger.warn("[CutEffectFix] 兼容版着色器仍未通过编译，本次将跳过刀痕特效（不会闪退）。编译日志: " + program.getLog());
                }
            }
            catch (Throwable t) {
                unusable = true;
                logger.warn("[CutEffectFix] 创建兼容版着色器失败，本次将跳过刀痕特效（不会闪退）: " + t);
            }
        }
        if (unusable) {
            finishEffect(__instance);
            return SpireReturn.Return();
        }
        return SpireReturn.Continue();
    }

    private static void setStaticShader(ShaderProgram program) throws Exception {
        Field field = CutEffect.class.getDeclaredField("cutShader");
        field.setAccessible(true);
        field.set(null, program);
    }

    /** 标记特效已结束，让它从 effectList 中移除；只丢特效，不闪退。 */
    private static void finishEffect(CutEffect effect) {
        try {
            Field field = AbstractGameEffect.class.getDeclaredField("isDone");
            field.setAccessible(true);
            field.setBoolean(effect, true);
        }
        catch (Throwable ignored) {
            // 标记失败也无所谓：渲染已经跳过，异常不会再从这里抛出
        }
    }
}
