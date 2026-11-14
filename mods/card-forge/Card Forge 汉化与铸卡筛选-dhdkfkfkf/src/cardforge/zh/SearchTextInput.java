package cardforge.zh;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;

/**
 * 搜索框用的键盘处理器。
 *
 * <p>Amethyst 运行时会在 {@code Gdx.input.setInputProcessor} 收到“看起来像文本输入”的处理器时请求系统软键盘
 * （类名包含 text/input/type/keyboard，且不包含 scroll/mouse/touch），所以这里用这个类名并自己攒字符。
 * 触摸事件全部转发给原处理器，保证界面按钮在输入时依然可点。</p>
 */
final class SearchTextInput implements InputProcessor {
    /** 单次搜索的最大字符数。 */
    private static final int MAX_LEN = 12;

    interface Listener {
        /** 文本变化。 */
        void changed(String text);

        /** 用户确认输入。 */
        void done();
    }

    private final StringBuilder buffer = new StringBuilder();
    private final Listener listener;
    private final InputProcessor previous;

    SearchTextInput(String initial, Listener listener, InputProcessor previous) {
        if (initial != null) {
            buffer.append(initial);
        }
        this.listener = listener;
        this.previous = previous;
    }

    private void backspace() {
        if (buffer.length() > 0) {
            buffer.deleteCharAt(buffer.length() - 1);
            listener.changed(buffer.toString());
        }
    }

    private void append(char character) {
        if (buffer.length() < MAX_LEN) {
            buffer.append(character);
            listener.changed(buffer.toString());
        }
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ENTER || keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
            listener.done();
            return true;
        }
        if (keycode == Input.Keys.BACKSPACE || keycode == Input.Keys.FORWARD_DEL) {
            backspace();
            return true;
        }
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        return true;
    }

    @Override
    public boolean keyTyped(char character) {
        if (character == 8 || character == 127) {
            backspace();
            return true;
        }
        if (character == '\r' || character == '\n') {
            listener.done();
            return true;
        }
        if (character < 32) {
            return true;
        }
        append(character);
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return previous != null && previous.touchDown(screenX, screenY, pointer, button);
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return previous != null && previous.touchUp(screenX, screenY, pointer, button);
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return previous != null && previous.touchDragged(screenX, screenY, pointer);
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return previous != null && previous.mouseMoved(screenX, screenY);
    }

    @Override
    public boolean scrolled(int amount) {
        return previous != null && previous.scrolled(amount);
    }
}
