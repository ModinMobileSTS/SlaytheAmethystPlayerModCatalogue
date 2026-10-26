package sts.ryoikitenkai.compat.red;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * 把角色贴图渲染成「纯色剪影」的着色器。
 * <p>
 * 关键点：输出颜色完全由 uniform 决定，贴图只提供 alpha。
 * 如果沿用父模组那种「改顶点颜色」的做法（skeleton.setColor），
 * 结果是贴图颜色与颜色相乘 —— 深色部位仍然发暗，描不出均匀的金属边。
 * 这里丢弃 RGB、只取 alpha，才能得到干净的剪影。
 * <p>
 * 属性名必须用 libgdx 的标准命名（a_position / a_color / a_texCoord0），
 * uniform 里的 u_projTrans 由 SpriteBatch 自动填充，不需要手动设置投影矩阵。
 */
public final class SilhouetteShader {
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
            "uniform sampler2D u_texture;\n" +
            "uniform vec4 u_silhouetteColor;\n" +
            "void main() {\n" +
            "    vec4 tex = texture2D(u_texture, v_texCoords);\n" +
            "    float a = tex.a * v_color.a;\n" +
            "    if (a < 0.04) discard;\n" +
            "    gl_FragColor = vec4(u_silhouetteColor.rgb, a * u_silhouetteColor.a);\n" +
            "}\n";

    private static ShaderProgram program;
    private static int colorUniform = -1;
    private static boolean failed = false;

    private SilhouetteShader() {
    }

    /**
     * 惰性构建：首次绘制时才编译，此时 GL 上下文一定已就绪。
     * 编译失败就永久降级（返回 null），绝不让描边把游戏拖崩。
     */
    public static ShaderProgram program() {
        if (program != null || failed) {
            return program;
        }
        try {
            ShaderProgram sp = new ShaderProgram(VERT, FRAG);
            if (!sp.isCompiled()) {
                failed = true;
                return null;
            }
            colorUniform = sp.getUniformLocation("u_silhouetteColor");
            program = sp;
        } catch (Throwable t) {
            failed = true;
            program = null;
        }
        return program;
    }

    public static int colorUniform() {
        return colorUniform;
    }
}
