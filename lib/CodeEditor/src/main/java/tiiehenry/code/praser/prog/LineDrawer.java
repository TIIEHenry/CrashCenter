package tiiehenry.code.praser.prog;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

import java.util.List;

import tiiehenry.code.praser.BlockLine;
import tiiehenry.code.view.ColorScheme;
import tiiehenry.code.view.EditorField;
import tiiehenry.code.view.TouchNavigationMethod;

public class LineDrawer implements ColorScheme.OnColorChangedListener {

    private final Paint _brushLine;
    private final Paint _brushLineNumber;
    private final Paint _brushLineBackground;
    private final Paint _brushLineBackgroundHighlight;
    private final Paint _brushLineErr;
    private final EditorDrawer drawer;

    public LineDrawer(EditorDrawer editorDrawer) {
        this.drawer = editorDrawer;
        _brushLine = new Paint();
        _brushLine.setAntiAlias(true);
        _brushLineNumber = new Paint();
        _brushLineNumber.setAntiAlias(true);
        _brushLineBackground = new Paint();
        _brushLineBackground.setAntiAlias(true);
        _brushLineBackgroundHighlight = new Paint();
        _brushLineBackgroundHighlight.setAntiAlias(true);
        _brushLineErr = new Paint();
        _brushLineErr.setAntiAlias(true);
        onColorChanged();
        ColorScheme.addOnColorChangedListener(this);
    }

    @Override
    public void onColorChanged() {
        _brushLineNumber.setColor(ColorScheme.Colorable.LINENUMBER.getColor());
        _brushLineErr.setColor(ColorScheme.Colorable.ERROR.getColor());
        _brushLineBackground.setColor(ColorScheme.Colorable.LINE_OFFSET.getColor());
        _brushLineBackgroundHighlight.setColor(ColorScheme.Colorable.LINE_OFFSET_HIGHLIGHT.getColor());
    }

    public void setTextSize(int pix) {
//        _brushLine.setTextSize(pix);
        _brushLineNumber.setTextSize(pix);
        _brushLineErr.setTextSize(pix);
        _brushLineBackground.setTextSize(pix);
        _brushLineBackgroundHighlight.setTextSize(pix);
    }

    public void drawLineBackground(Canvas canvas, int paintX, int paintY, int advance) {
        Paint.FontMetricsInt metrics = getFontMetricsInt();
        canvas.drawRect(paintX /*- TouchNavigationMethod.touchSlopHorizontal*/,
                paintY + metrics.ascent,
                paintX/* + TouchNavigationMethod.touchSlopHorizontal */+ advance,
                paintY + metrics.descent,
                _brushLineBackground);
    }

    public void drawErrorLine(Canvas canvas, float left, float top, float right, float bottom) {
        canvas.drawRect(left,
                top,
                right,
                bottom,
                _brushLineErr);
    }

    public void drawBlockLine(Canvas canvas, EditorField editorField) {
        if (editorField.setting.isShowBlockRegionLines && !editorField.isWordWrap()) {
            List<BlockLine> lines = editorField.getLexTask().getBlockRegionLinesList();
            if (!lines.isEmpty()) {
//                int w = getAdvance('}') / 3;
                /*int o = editorDrawer._brush.getColor();
                editorDrawer._brush.setColor(ColorScheme.Colorable.VERTICAL_LINE.getColor());
*/
                Rect clipBounds = canvas.getClipBounds();
                int canvasTop = clipBounds.top;
                int canvasBottom = clipBounds.bottom;

                BlockLine rect3 = null;
                for (BlockLine rect : lines) {
                    int top = (rect.startLine) * drawer.getRowHeight();
                    int bottom = (rect.endLine - 1) * drawer.getRowHeight();
                    if (editorField.setting.isShowBlockRegionHighlightLine &&
                            rect.startLine - 1 <= editorField.getCaretRow() && rect.endLine - 1 >= editorField.getCaretRow()
                            && (rect3 == null || rect3.startIndex < rect.startIndex)) {
                        rect3 = rect;
                    }
                    if (bottom > canvasTop && top < canvasBottom) {
                        int left = Math.min(drawer.getCharExtent(rect.startIndex, editorField).first, drawer.getCharExtent(rect.endIndex, editorField).first);
                        canvas.drawLine(left, top, left, bottom, _brushLine);
                    }
                }
                if (rect3 != null) {
                    int top = (rect3.startLine) * drawer.getRowHeight();
                    int bottom = (rect3.endLine - 1) * drawer.getRowHeight();
                    int left = Math.min(drawer.getCharExtent(rect3.startIndex, editorField).first, drawer.getCharExtent(rect3.endIndex, editorField).first);
                    int color = _brushLine.getColor();
                    _brushLine.setColor(ColorScheme.Colorable.SEPARATOR.getColor());
                    canvas.drawRect(left - 2, top, left + 2, bottom, _brushLine);
                    _brushLine.setColor(color);
                }
/*
                editorDrawer._brush.setColor(o);*/
            }
        }
    }

    public void setTypeface(Typeface typeface) {
        _brushLineNumber.setTypeface(typeface);
    }

    public int measureTextWidth(String s) {
        return (int) _brushLineNumber.measureText(s);
    }


    public void drawLineNumber(Canvas canvas, int leftOffset, int num, int paintY, EditorField editorField) {
        if (editorField.setting.isShowLineNumbers) {
            String s = String.valueOf(num);
            float x = (leftOffset - _brushLineNumber.measureText(s) - drawer.getSpaceWidth() / 2f) / 2;
            canvas.drawText(s, x, paintY, _brushLineNumber);
        }
    }

    public void drawLineNumberBackground(Canvas canvas, int leftOffset, EditorField editorField) {
        if (editorField.setting.isShowLineOffset) {
            //所有行号背景
            int width = leftOffset - drawer.getSpaceWidth() / 4;
            canvas.drawRect(0,
                    editorField.getScrollY(),
                    width,
                    editorField.getScrollY() + editorField.getHeight(),
                    _brushLineBackground);
            //当前行号的的背景高亮
            drawLineNumberBackgroundHighlight(canvas,leftOffset,drawer.getCaretRow(),editorField);
        }
    }

    public void drawLineNumberBackgroundHighlight(Canvas canvas, int leftOffset, int row,EditorField editorField) {
        if (editorField.setting.isShowLineOffset) {
            //所有行号背景
            int width = leftOffset - drawer.getSpaceWidth() / 4;
            //当前行号的的背景高亮
            int y = drawer.getPaintBaseline(row);
            Paint.FontMetricsInt metrics = getFontMetricsInt();
            canvas.drawRect(0,
                    y + metrics.ascent,
                    width,
                    y + metrics.descent,
                    _brushLineBackgroundHighlight);
        }
    }

    public Paint.FontMetricsInt getFontMetricsInt() {
        return _brushLineNumber.getFontMetricsInt();
    }

    public void drawOptionHighlightRow(Canvas canvas, EditorField editorField) {
        if (editorField.setting.isHighlightCurrentRow) {
            int y = drawer.getPaintBaseline(drawer.getCaretRow());
//            int y = (drawer.getCaretRow() + 1) * drawer.getRowHeight();
            int width = editorField.getContentWidth();
//            int lineLength = Math.max(drawer.getXExtent(), width);
            drawLineBackground(canvas, 0, y, editorField.getScrollX()+width);
        }
    }

}
