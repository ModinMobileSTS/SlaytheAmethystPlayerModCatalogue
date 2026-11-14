package cardforge.zh;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.input.InputHelper;

/**
 * 铸卡界面自己的输入处理器。
 *
 * <p>为什么不用 {@link InputHelper}：这个安卓运行时里触摸只会到达 InputProcessor 的
 * touchDown / touchDragged / touchUp，鼠标按键状态（{@code InputHelper.isMouseDown}）在触摸时一直是 false，
 * 而且实现“点击命中”补丁会把 {@code justClickedLeft} 延迟一帧、甚至在拖动过程中不再更新
 * {@code InputHelper.mY}。所以这里直接接管触摸事件，按下/移动/抬起与坐标都来自原始事件。</p>
 *
 * <p>所有事件都会转发给原来的处理器，保证游戏自身的输入逻辑不受影响。</p>
 */
final class ForgeInput implements InputProcessor {
    private final InputProcessor previous;

    /** 指针位置（世界坐标，与界面绘制同一坐标系）。 */
    float x;
    float y;
    /** 当前是否按住。 */
    boolean down;
    /** 本帧是否刚按下 / 刚抬起。 */
    boolean pressed;
    boolean released;
    /** 本帧滚轮累计（正数向下）。 */
    int scrollAmount;

    private ForgeInput(InputProcessor previous) {
        this.previous = previous;
        this.x = InputHelper.mX;
        this.y = InputHelper.mY;
    }

    /** 安装到游戏输入链上；如果已经装过就复用。 */
    static ForgeInput install() {
        InputProcessor current = Gdx.input.getInputProcessor();
        if (current instanceof ForgeInput) {
            ForgeInput existing = (ForgeInput) current;
            existing.pressed = false;
            existing.released = false;
            existing.scrollAmount = 0;
            return existing;
        }
        ForgeInput created = new ForgeInput(current);
        Gdx.input.setInputProcessor(created);
        return created;
    }

    /** 卸载，恢复原来的处理器。 */
    void uninstall() {
        if (Gdx.input.getInputProcessor() == this) {
            try {
                Gdx.input.setInputProcessor(previous);
            } catch (Throwable ignored) {
            }
        }
    }

    void consume() {
        pressed = false;
        released = false;
        scrollAmount = 0;
    }

    private static float toWorldX(int screenX) {
        if (Settings.isTouchScreen) {
            return screenX + Settings.VERT_LETTERBOX_AMT;
        }
        if (screenX < 0) {
            return 0.0f;
        }
        return Math.min(screenX, Settings.WIDTH);
    }

    private static float toWorldY(int screenY) {
        if (Settings.isTouchScreen) {
            float value = Settings.HEIGHT - screenY + Settings.HORIZ_LETTERBOX_AMT;
            return value < 1.0f ? 1.0f : value;
        }
        float value = Settings.HEIGHT - screenY;
        if (value < 1.0f) {
            return 1.0f;
        }
        return Math.min(value, Settings.HEIGHT);
    }

    private static boolean isPrimary(int pointer, int button) {
        return pointer == 0 && (button == 0 || button == -1);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (previous != null) {
            previous.touchDown(screenX, screenY, pointer, button);
        }
        if (isPrimary(pointer, button)) {
            x = toWorldX(screenX);
            y = toWorldY(screenY);
            down = true;
            pressed = true;
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (previous != null) {
            previous.touchUp(screenX, screenY, pointer, button);
        }
        if (isPrimary(pointer, button) && down) {
            x = toWorldX(screenX);
            y = toWorldY(screenY);
            down = false;
            released = true;
        }
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (previous != null) {
            previous.touchDragged(screenX, screenY, pointer);
        }
        if (pointer == 0) {
            x = toWorldX(screenX);
            y = toWorldY(screenY);
        }
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        if (previous != null) {
            previous.mouseMoved(screenX, screenY);
        }
        if (!down) {
            x = toWorldX(screenX);
            y = toWorldY(screenY);
        }
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        if (previous != null) {
            previous.scrolled(amount);
        }
        scrollAmount += amount;
        return false;
    }

    @Override
    public boolean keyDown(int keycode) {
        return previous != null && previous.keyDown(keycode);
    }

    @Override
    public boolean keyUp(int keycode) {
        return previous != null && previous.keyUp(keycode);
    }

    @Override
    public boolean keyTyped(char character) {
        return previous != null && previous.keyTyped(character);
    }
}
