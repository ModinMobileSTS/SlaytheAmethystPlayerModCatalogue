package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import sts.ryoikitenkai.modcore.ShaderManager;

/**
 * 「如水」水面的着色器。
 * <p>
 * <b>为什么用着色器</b>：环形涟漪、焦散网纹、边缘柔化、水体厚度都是逐像素的细节。
 * 用 CPU 画线去拼，焦散画不出来，涟漪的层次则需要成百上千段线条。
 * 交给 GPU 后 <b>每帧只有一次 draw call</b>，几何量是一个四边形。
 * <p>
 * <b>性能取舍</b>（改动前请先读）：
 * <ol>
 *     <li><b>不用 discard</b>。移动端多为 tile-based GPU，discard 会打断其剔除优化，
 *         代价高于多算几个像素。这里把边界外乘成黑色，配合加色混合自然不可见。</li>
 *     <li><b>只做 2 次纹理采样</b>。焦散复用父模组已加载的噪声纹理，
 *         不新建纹理、不做多倍频程序化噪声。</li>
 *     <li><b>片元里没有循环</b>。涟漪由半径的一维正弦叠加得出。</li>
 *     <li><b>uniform 位置只在编译时查一次</b>，避免每帧做字符串查找。</li>
 *     <li>四边形只覆盖脚下水面范围，像素开销随分辨率线性，不随效果复杂度爆炸。</li>
 *     <li>编译失败永久降级为 null，调用方退回贴图版水面。</li>
 * </ol>
 */
final class WaterShader {
    /** 水体主色：偏青的深蓝。 */
    static final float R = 0.30f;
    static final float G = 0.74f;
    static final float B = 1.00f;

    private static final String VERT =
            "attribute vec4 a_position;\n" +
            "attribute vec4 a_color;\n" +
            "attribute vec2 a_texCoord0;\n" +
            "uniform mat4 u_projTrans;\n" +
            "varying vec4 v_color;\n" +
            "varying vec2 v_texCoords;\n" +
            "void main() {\n" +
            "    v_color = a_color;\n" +
            "    v_texCoords = a_texCoord0;\n" +
            "    gl_Position = u_projTrans * a_position;\n" +
            "}\n";

    private static final String FRAG =
            "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "#endif\n" +
            "varying vec4 v_color;\n" +
            "varying vec2 v_texCoords;\n" +
            "uniform sampler2D u_texture;\n" +      // 焦散噪声
            "uniform float u_time;\n" +
            "uniform float u_alpha;\n" +            // 淡入淡出
            "uniform float u_strength;\n" +         // 平静 / 非平静
            "uniform float u_churn;\n" +            // 触发搅动
            "uniform vec3 u_water;\n" +
            "uniform vec3 u_foam;\n" +
            "void main() {\n" +
            // 归一化到 [-1,1] 的椭圆坐标，r=1 正好是水面边界
            "    vec2 q = v_texCoords * 2.0 - 1.0;\n" +
            "    float r = length(q);\n" +
            // 边缘柔化：不用 discard，边界外乘成黑色即可
            "    float mask = 1.0 - smoothstep(0.90, 1.0, r);\n" +
            "    if (mask <= 0.0) { gl_FragColor = vec4(0.0); return; }\n" +
            // 涟漪：沿半径的一维正弦，向外推进；pow 做成尖锐波峰
            "    float t = u_time * (1.0 + 1.4 * u_churn);\n" +
            "    float ring = sin(r * 15.0 - t * 2.3) * 0.5 + 0.5;\n" +
            "    ring = pow(ring, 3.5);\n" +
            // 第二道错频涟漪，避免单频的机械感
            "    float ring2 = sin(r * 26.0 - t * 3.4 + 1.7) * 0.5 + 0.5;\n" +
            "    ring2 = pow(ring2, 5.0) * 0.55;\n" +
            // 焦散：两层噪声反向滚动相乘得到网状光斑（仅 2 次采样）
            "    vec2 n1 = q * 0.30 + vec2(t * 0.030, t * 0.018);\n" +
            "    vec2 n2 = q * 0.52 - vec2(t * 0.024, t * 0.015);\n" +
            "    float c1 = texture2D(u_texture, n1).r;\n" +
            "    float c2 = texture2D(u_texture, n2).r;\n" +
            "    float caustic = clamp(c1 * c2 * 2.1, 0.0, 1.0);\n" +
            "    caustic = caustic * caustic;\n" +
            // 中心略亮、边缘略暗，做出水体厚度
            "    float depth = 1.0 - smoothstep(0.0, 1.0, r);\n" +
            // 合成
            "    float body = 0.30 + 0.34 * (ring + ring2) + 0.26 * depth;\n" +
            "    float foam = (ring + ring2) * (0.42 + 0.58 * caustic);\n" +
            "    vec3 col = u_water * body + u_foam * foam;\n" +
            "    vec3 rgb = col * mask * u_alpha * u_strength * (1.0 + 0.45 * u_churn);\n" +
            // 加色混合下黑色即透明，alpha 交给顶点色控制整体淡入淡出
            "    gl_FragColor = vec4(rgb, 1.0) * v_color.a;\n" +
            "}\n";

    private static ShaderProgram program;
    private static boolean failed;

    private static int uTime = -1;
    private static int uAlpha = -1;
    private static int uStrength = -1;
    private static int uChurn = -1;
    private static int uWater = -1;
    private static int uFoam = -1;
    private static int uTexture = -1;

    private WaterShader() {
    }

    /** 编译着色器并缓存 uniform 位置。编译失败则永久返回 null。 */
    static ShaderProgram program() {
        if (program != null || failed) {
            return program;
        }
        try {
            ShaderProgram sp = new ShaderProgram(VERT, FRAG);
            if (!sp.isCompiled()) {
                failed = true;
                return null;
            }
            uTime = sp.getUniformLocation("u_time");
            uAlpha = sp.getUniformLocation("u_alpha");
            uStrength = sp.getUniformLocation("u_strength");
            uChurn = sp.getUniformLocation("u_churn");
            uWater = sp.getUniformLocation("u_water");
            uFoam = sp.getUniformLocation("u_foam");
            uTexture = sp.getUniformLocation("u_texture");
            program = sp;
        } catch (Throwable t) {
            failed = true;
            program = null;
        }
        return program;
    }

    /**
     * 焦散用的噪声纹理。
     * <p>
     * 复用父模组为雾气着色器加载的那张 {@code noiseTexture}：不额外占显存，也省一次加载。
     * 它是 Repeat 包裹的，所以片元里坐标超出 [0,1] 也能正确平铺。
     * <p>
     * 注意：父模组在 receivePostInitialize 才执行 ShaderManager.init()。
     * 这里<b>不缓存 null</b>，每帧重试直到拿到为止，避免因为时序早于父模组初始化
     * 就被永久判成「不可用」；真正的失败由着色器编译结果决定。
     */
    static Texture noise() {
        try {
            return ShaderManager.noiseTexture;
        } catch (Throwable ignored) {
            return null;
        }
    }

    static int uTime() {
        return uTime;
    }

    static int uAlpha() {
        return uAlpha;
    }

    static int uStrength() {
        return uStrength;
    }

    static int uChurn() {
        return uChurn;
    }

    static int uWater() {
        return uWater;
    }

    static int uFoam() {
        return uFoam;
    }

    static int uTexture() {
        return uTexture;
    }
}
