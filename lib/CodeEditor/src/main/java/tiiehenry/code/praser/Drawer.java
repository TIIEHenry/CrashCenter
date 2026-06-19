package tiiehenry.code.praser;

import android.graphics.Canvas;

import tiiehenry.code.view.EditorField;

public interface Drawer {
    /**
     *
     * @param editor
     * @param canvas
     * @param paintX
     * @param paintY
     * @return paintX的增加量
     */
    int draw(EditorField editor, Canvas canvas, Blockable blockable, int paintX, int paintY);
}
