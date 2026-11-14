package cardforge.zh;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.input.InputHelper;

/**
 * 铸卡界面常驻的输入处理器。
 *
 * <p>为什么不用 {@link InputHelper} 的鼠标语义：这个安卓运行时里触摸只会到达 InputProcessor，
 * 鼠标按键状态（{@code InputHelper.isMouseDown}）在触摸时一直是 false。所以按下/抬起直接取触摸事件。</p>
 *
 * <p>指针坐标同时参考触摸事件与 {@code InputHelper.mX/mY}：运行时在拖动时只有其中一个会更新，
 * 由界面层决定采用哪个（见 ForgeCraftScreen#updatePointer）。</p>
 *
 * <p>类名刻意不含 text/input/type/keyboard，避免运行时把它当成文本输入而唤起键盘。</p>
 */
final class CraftPointer implements InputProcessor {
    private final InputProcessor previous;

    /** 指针位置（世界坐标，与界面绘制同一坐标系）。 */
    float x;
    float y;
    boolean down;
    boolean pressed;
    boolean released;
    int scrollAmount;

    private CraftPointer(InputProcessor previous) {
        this.previous = previous;
        this.x = InputHelper.mX;
        this.y = InputHelper.mY;
    }

    static CraftPointer install() {
        InputProcessor current = Gdx.input.getInputProcessor();
        if (current instanceof CraftPointer) {
            CraftPointer existing = (CraftPointer) current;
            existing.pressed = false;
            existing.released = false;
            existing.scrollAmount = 0;
            return existing;
        }
        CraftPointer created = new CraftPointer(current);
        Gdx.input.setInputProcessor(created);
        return created;
    }

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
        if (isPrimary(pointer, button)) {
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
            float worldX = toWorldX(screenX);
            float worldY = toWorldY(screenY);
            if (Math.abs(worldX - x) > 0.5f || Math.abs(worldY - y) > 0.5f) {
                x = worldX;
                y = worldY;
            }
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
