package tiiehenry.code.view;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;

public class MaterialSlideBar {
    public int touchedWidth = 30, width = 25;
    public int height = 100;

    private boolean expand;
    private final Paint mp;
    private float startY;
    private final EditorField textField;

    public MaterialSlideBar(EditorField parent) {
        textField = parent;
        expand = false;
        mp = new Paint();
        mp.setStyle(Paint.Style.FILL);
        mp.setAntiAlias(false);
    }


    public boolean handleEvent(MotionEvent event) {
        if (height<textField.getContentHeight()/2) {
            final float x = event.getX();
            final float y = event.getY();
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                expand = false;
                if ((textField.getWidth() - x) <= touchedWidth) {
                    startY = y - getBarStartY();
                    expand = startY >= 0 && startY <= height;
                    return expand;
                }
                return false;
            }
            if (!expand) return false;
            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                case MotionEvent.ACTION_UP:
                    float target = (y - startY) / (textField.getHeight() - height);
                    if (target < 0) target = 0;
                    if (target > 1) target = 1;
                    textField.scrollTo(textField.getScrollX(), (int) (target * textField.getMaxScrollY()));
                    return true;
            }
            return true;
        }
        return false;
    }

    private int getWidth() {
        return expand ? touchedWidth : width;
    }

    public float getBarStartY() {
        return (float) (textField.getHeight() - height) * textField.getScrollY() / textField.getMaxScrollY();
    }

    public void draw(Canvas canvas) {
        final float start = getBarStartY() + +textField.getScrollY();
        int right = canvas.getWidth() + textField.getScrollX();
        if (expand)
            mp.setColor(ColorScheme.Colorable.SLIDEBAR_HANDLED.getColor());
        else
            mp.setColor(ColorScheme.Colorable.SLIDEBAR_BACKGROUND.getColor());
        canvas.drawRect(right - getWidth() * 2 / 3f, start, right, start + height, mp);
    }

    public void updateHeight() {
        height =Math.max( textField.getContentHeight() * textField.getContentHeight() / textField.getMaxHeight(),getWidth() * 3);
    }
}
