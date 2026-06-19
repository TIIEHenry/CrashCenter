package tiiehenry.code.praser.prog;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

import java.util.List;

import tiiehenry.code.doc.Pair;
import tiiehenry.code.language.Language;
import tiiehenry.code.praser.Span;
import tiiehenry.code.praser.SymbolPair;
import tiiehenry.code.view.ColorScheme;
import tiiehenry.code.view.EditorField;
import tiiehenry.code.view.EditorSetting;

public class EditorDrawer {


    private final Paint _brush;
    private final Paint _brushPair;
    private final Paint _brushPairText;
    private final LineDrawer lineDrawer;
    private int _alphaWidth;
    private int _spaceWidth;
    protected static float EMPTY_CARET_WIDTH_SCALE = 0.75f;

    protected static int BASE_TEXT_SIZE_PIXELS = 16;
    private int textSize;
    private int rowHeight;

    public int getSpaceWidth() {
        return _spaceWidth;
    }

    public EditorDrawer() {
        _brush = new Paint();
        _brush.setAntiAlias(true);
        lineDrawer = new LineDrawer(this);
        _brushPair = new Paint();
        _brushPair.setAntiAlias(true);
        _brushPairText = new Paint();
        _brushPairText.setAntiAlias(true);
        setTextSize(BASE_TEXT_SIZE_PIXELS);

        ColorScheme.addOnColorChangedListener(new ColorScheme.OnColorChangedListener() {
            @Override
            public void onColorChanged() {
                _brushPair.setColor(ColorScheme.Colorable.SELECTION_BACKGROUND.getColor());
                _brushPairText.setColor(ColorScheme.Colorable.SELECTION_FOREGROUND.getColor());
            }
        });
    }

    public Paint.FontMetricsInt getFontMetricsInt() {
        return _brush.getFontMetricsInt();
    }

    private char _emoji;

    protected int getSpaceAdvance(EditorField editor) {
        if (editor.setting.isShowNonPrinting) {
            return (int) _brush.measureText(Language.GLYPH_SPACE, 0, Language.GLYPH_SPACE.length());
        } else {
            return _spaceWidth;
        }
    }


    protected int getEOLAdvance(EditorField editor) {
        if (editor.setting.isShowNonPrinting) {
            return (int) _brush.measureText(Language.GLYPH_NEWLINE, 0, Language.GLYPH_NEWLINE.length());
        } else {
            return (int) (EMPTY_CARET_WIDTH_SCALE * _brush.measureText(" ", 0, 1));
        }
    }

    public int getAdvance(EditorField editor, char c, int x) {
        int advance;
        switch (c) {
            case 0xd83c:
            case 0xd83d:
                advance = 0;
                break;
            case ' ':
                advance = getSpaceAdvance(editor);
                break;
            case Language.NEWLINE: // fall-through
            case Language.EOF:
                advance = getEOLAdvance(editor);
                break;
            case Language.TAB:
                advance = getTabAdvance(editor, x);
                break;
            default:
                if (_emoji != 0) {
                    char[] ca = {_emoji, c};
                    advance = (int) _brush.measureText(ca, 0, 2);
                } else {
                    char[] ca = {c};
                    advance = (int) _brush.measureText(ca, 0, 1);
                }
                break;
        }

        return advance;
    }


    protected int getTabAdvance(EditorField editor, int x) {
        if (editor.setting.isShowNonPrinting) {
            return editor.setting.tabLength * (int) _brush.measureText(Language.GLYPH_SPACE, 0, Language.GLYPH_SPACE.length());
        } else {
            int i = 0;
            if (x != 0) {
                i = (x - editor.getLeftOffset()) % _spaceWidth;
            }
            if (i < 0)
                i = 0;
            return editor.setting.tabLength * _spaceWidth - i;
//空格tab开头会导致偏移
//            if (x != 0) {
//                i = (x - _leftOffset) / _spaceWidth % setting.tabLength;
//            }
//            return setting.tabLength * _spaceWidth - i * _spaceWidth;
        }
    }

    /**
     * 返回开始绘制的Y
     *
     * @return The x-value of the baseline for drawing text on the given row
     */
    public int getPaintBaseline(int row) {
        Paint.FontMetricsInt metrics = _brush.getFontMetricsInt();
        return (row + 1) * rowHeight - metrics.descent;
    }

    public float getTextSize() {
        return _brush.getTextSize();
    }

    public void setTextSize(int pix) {
        if (textSize == pix) {
            return;
        }
        textSize = pix;
        _brush.setTextSize(pix);
        _brushPair.setTextSize(pix);
        _brushPairText.setTextSize(pix);
        lineDrawer.setTextSize(pix);
        _alphaWidth = (int) _brush.measureText("a");
        _spaceWidth = (int) _brush.measureText(" ");
        updateRowHeight();
    }

    //空白字符
    private void drawNonPrinting(Canvas canvas, String s, int paintX, int paintY) {
        int originalColor = _brush.getColor();
        _brush.setColor(ColorScheme.Colorable.NON_PRINTING_GLYPH.getColor());
        canvas.drawText(s, 0, 1, paintX, paintY, _brush);
        _brush.setColor(originalColor);
    }

    public void drawCharStraight(Canvas canvas, Paint _brush, char c, int paintX, int paintY, EditorField editorField) {
        if (paintX >= editorField.getScrollX() || paintX <= (editorField.getScrollX() + editorField.getContentWidth())) {
            switch (c) {
                case 0xd83c:
                case 0xd83d:
                    _emoji = c;
                    break;
                case ' ':
                    if (editorField.setting.isShowNonPrinting) {
                        drawNonPrinting(canvas, Language.GLYPH_SPACE, paintX, paintY);
                    } else {
                        canvas.drawText(" ", 0, 1, paintX, paintY, _brush);
                    }
                    break;
                case Language.EOF: //fall-through
                case Language.NEWLINE:
                    if (editorField.setting.isShowNonPrinting) {
                        drawNonPrinting(canvas, Language.GLYPH_NEWLINE, paintX, paintY);
                    }
                    break;
                case Language.TAB:
                    if (editorField.setting.isShowNonPrinting) {
                        drawNonPrinting(canvas, Language.GLYPH_TAB, paintX, paintY);
                    }
                    break;
                default:
                    if (_emoji != 0) {
                        canvas.drawText(new char[]{_emoji, c}, 0, 2, paintX, paintY, _brush);
                        _emoji = 0;
                    } else {
                        char[] ca = {c};
                        canvas.drawText(ca, 0, 1, paintX, paintY, _brush);
                    }
                    break;
            }
        }
    }

    public int drawChar(Canvas canvas, Paint _brush, char c, int paintX, int paintY, EditorField editorField) {
        int charWidth = getAdvance(editorField, c, paintX);
        drawCharStraight(canvas, _brush, c, paintX, paintY, editorField);
        return charWidth;
    }

    // paintY is the baseline for text, NOT the top extent
    public void drawTextBackground(Canvas canvas, Paint _brush, int paintX, int paintY, int advance) {
        Paint.FontMetricsInt metrics = _brush.getFontMetricsInt();
        canvas.drawRect(paintX,
                paintY + metrics.ascent,
                paintX + advance,
                paintY + metrics.descent,
                _brush);
    }

    public int drawSelectedText(Canvas canvas, char c, int paintX, int paintY, EditorField editorField) {
        int oldColor = _brush.getColor();
        int advance = getAdvance(editorField, c, paintX);

        _brush.setColor(ColorScheme.Colorable.SELECTION_BACKGROUND.getColor());
        drawTextBackground(canvas, _brush, paintX, paintY, advance);

        _brush.setColor(ColorScheme.Colorable.SELECTION_FOREGROUND.getColor());
        drawChar(canvas, _brush, c, paintX, paintY, editorField);

        _brush.setColor(oldColor);
        return advance;
    }

    public int drawErrorText(Canvas canvas, char c, int paintX, int paintY, EditorField editorField) {
        int advance = drawChar(canvas, _brush, c, paintX, paintY, editorField);
//        int rowHeight = rowHeight;
        if (paintX > editorField.getScrollX() || paintX < (editorField.getScrollX() + editorField.getContentWidth())) {
            Paint.FontMetricsInt metrics = _brush.getFontMetricsInt();

            int margin = advance / 4;
            lineDrawer.drawErrorLine(canvas, paintX + margin,
                    paintY + metrics.descent - 3,
                    paintX + advance - margin,
                    paintY + metrics.descent);

        }
        return advance;
    }

    private int _caretX;
    private int _caretY;

    //光标
    public void drawCaret(Canvas canvas, int paintX, int paintY, EditorField editorField) {
        int originalColor = _brush.getColor();
        _caretX = paintX - editorField.setting.caretWidth / 2;
        _caretY = paintY;
        int caretColor = ColorScheme.Colorable.CARET_DISABLED.getColor();
        _brush.setColor(caretColor);
        // draw full caret
        drawTextBackground(canvas, _brush, _caretX, paintY, editorField.setting.caretWidth);
        _brush.setColor(originalColor);
    }

    private int _caretRow = 0; //当前行号 can be calculated, but stored for efficiency purposes

    //获取当前行，有阴影的
    public int getCaretRow() {
        return _caretRow;
    }

    public void setCaretRow(int _caretRow) {
        this._caretRow = _caretRow;
    }

    public void drawBlockPairs(Canvas canvas, EditorField editorField) {
        if (currPair != null && !editorField.isSelectText() && !editorField.isWordWrap()) {
            Pair open = getCharExtent(currPair.left, editorField);
            Pair close = getCharExtent(currPair.right, editorField);
            int openY = (currPair.top - 1) * rowHeight;
            int closeY = (currPair.bottom - 1) * rowHeight;
//            drawBlockPairCharHighLight(canvas,left,_brush,top);
            Paint.FontMetricsInt metrics = _brushPair.getFontMetricsInt();

//            Log.e("EditorDrawer", "drawBlockPairs openY: " + openY);
//            Log.e("EditorDrawer", "drawBlockPairs metrics.ascent: " + metrics.ascent);
//            Log.e("EditorDrawer", "drawBlockPairs rowHeight: " + rowHeight);
//            Log.e("EditorDrawer", "drawBlockPairs currPair.top: " + currPair.top);
//            int advance = getAdvance(editorField, editorField.getDocumentProvider().charAt(currPair.left), 0);
//            canvas.drawRect( open.first,
//                    openY ,
//                    open.first + advance,
//                    openY+rowHeight,
//                    _brushPair);
            drawPairBackWithground(canvas, editorField.getDocumentProvider().charAt(currPair.left), open.first, openY - metrics.ascent, editorField);
            drawPairBackWithground(canvas, editorField.getDocumentProvider().charAt(currPair.right), close.first, closeY - metrics.ascent, editorField);
//            drawBlockPairCharHighLight(canvas,right,_brush,bottom);
//            lineDrawer.drawLineNumberBackgroundHighlight(canvas, _leftOffset, currPair.bottom-1, editorField);
        }
    }

    public void drawPairBackWithground(Canvas canvas, char c, int paintX, int paintY, EditorField editorField) {
        int advance = getAdvance(editorField, c, 0);
//        canvas.drawRect(paintX,
//                paintY ,
//                paintX + advance,
//                paintY,
//                _brushPair);
        drawTextBackground(canvas, _brushPair, paintX, paintY, advance);
        drawCharStraight(canvas, _brushPairText, c, paintX, paintY, editorField);
    }

    /**
     * maxWidth一行的最大宽度
     * Max amount that can be scrolled horizontally based on the longest line
     * displayed on screen so far
     */
    private int _xExtent = 0;

    public int getAlphaWidth() {
        return _alphaWidth;
    }

    public int getXExtent() {
        return _xExtent;
    }

    public void setXExtent(int width) {
        this._xExtent = width;
    }

    SymbolPair currPair = null;

    public void resetBlockPair(EditorField editorField) {
        if (editorField.getLexTask() != null) {
            List<SymbolPair> pairList = editorField.getLexTask().getBlockPairsList();
            for (SymbolPair p : pairList) {
                if (p.right + 1 == editorField.getCaretPosition()) {
                    currPair = p;
                    return;
                } else if (p.left == editorField.getCaretPosition()) {
                    currPair = p;
                    return;
                } else if (p.left + 1 == editorField.getCaretPosition()) {
                    currPair = p;
                    return;
                }
            }
        }
        currPair = null;
    }

    public int getCaretX() {
        return _caretX;
    }

    public int getCaretY() {
        return _caretY;
    }

    private int _leftOffset = 0;

    public int getLeftOffset() {
        return _leftOffset;
    }

    private Span _caretSpan = new Span(0, ColorScheme.Colorable.TEXT);

    public void realDraw(Canvas canvas, EditorField editorField) {
        lineDrawer.drawOptionHighlightRow(canvas, editorField);
        lineDrawer.drawBlockLine(canvas, editorField);
//        getCaretPosition()
        resetLeftOffset(editorField);

        if (editorField.setting.isShowLineNumbers) {
//            drawLineNumbersVerticalLine(canvas, _leftOffset);
            lineDrawer.drawLineNumberBackground(canvas, _leftOffset, editorField);
        }


        int tabWidth = editorField.getAdvance(Language.TAB);
//        int tabWidth = 0;
        int drawXStart = editorField.getScrollX() + editorField.getPaddingLeft() - tabWidth;
        int drawXEnd = editorField.getWidth() + editorField.getScrollX() - editorField.getPaddingLeft() - editorField.getPaddingRight() + tabWidth;


        //rowStartForDraw
        int currRowNum = Math.max(getBeginPaintRow(canvas), 0);
        int endRowNum = getEndPaintRow(canvas);

        int currIndex = editorField.getDocumentProvider().getRowOffset(currRowNum);
        if (currIndex < 0)
            return;

        //int len = _hDoc.length();
        int currLineNum = editorField.isWordWrap() ? editorField.getDocumentProvider().findLineNumber(currIndex) + 1 : currRowNum + 1;
        int lastLineNum = 0;

        int currPaintY = getPaintBaseline(currRowNum);

        //----------------------------------------------
        // set up initial span color
        //----------------------------------------------
//        int spanIndex = 0;
        List<Span> spans = editorField.getDocumentProvider().getSpans();
        int spanSize = spans.size();

//        Log.e(TAG, "realDraw: "+spanSize );
        if (spanSize < 1) {
            spans.add(new Span(0, 0, 0, editorField.getDocumentProvider().docLength(), ColorScheme.Colorable.TEXT));
        }

        int spanIndex = editorField.drawer.binarySearchSpan(spans, currIndex);

        if (spanIndex == -1)
            return;
        Span currSpan = spans.get(spanIndex);
        int spanEndIndex = currSpan.startIndex + currSpan.len;

        ColorScheme.Colorable currType = currSpan.colorable;
        ColorScheme.Colorable lastType = currType;
        Typeface lastTypeface = getTypeface(lastType, editorField);
        _brush.setTypeface(lastTypeface);
        _brush.setColor(lastType.getColor());
        //----------------------------------------------
        // start painting!f
        //----------------------------------------------


        boolean isError = false;

        int endRowNumForDraw = Math.min(endRowNum, editorField.getDocumentProvider().getRowCount());
        while (currRowNum <= endRowNumForDraw) {
            int paintX = _leftOffset;
            if (currLineNum != lastLineNum) {
                if (drawXStart < paintX)
                    lineDrawer.drawLineNumber(canvas, paintX, currLineNum, currPaintY, editorField);
                lastLineNum = currLineNum;
            }

            for (int i = 0, rowLen = editorField.getDocumentProvider().getRowSize(currRowNum); i < rowLen; ++i) {
                if (spanEndIndex <= currIndex && spanIndex < spanSize - 1) {
                    currSpan = spans.get(++spanIndex);
                    spanEndIndex = currSpan.startIndex + currSpan.len;
//                    Log.e("span", "span: "+currSpan );
//                    Log.e("span", "spanEndIndex: "+spanEndIndex );
//                    Log.e("span", "char: "+getDocumentProvider().subSequence(currIndex,currSpan.len) );
                    currType = currSpan.colorable;
                    if (lastType != currType) {//span changed
                        lastType = currType;
                        _brush.setColor(currType.getColor());

                        Typeface currTypeface = getTypeface(currType, editorField);
                        if (lastTypeface != currTypeface) {
                            lastTypeface = currTypeface;
                            _brush.setTypeface(currTypeface);
                        }

                        if (currType == ColorScheme.Colorable.ERROR)
                            isError = true;
                    }
                }

                int x = paintX;
                char c = editorField.getDocumentProvider().charAt(currIndex);
                if (drawXStart < paintX && paintX < drawXEnd) {//中间
                    if (editorField.inSelectionRange(currIndex)) {
                        paintX += drawSelectedText(canvas, c, paintX, currPaintY, editorField);
                    } else if (isError || editorField.getLexTask().isError(currIndex)) {
                        paintX += drawErrorText(canvas, c, paintX, currPaintY, editorField);
                    } else {
                        paintX += drawChar(canvas, _brush, c, paintX, currPaintY, editorField);
                    }
                } else {
                    paintX += editorField.getAdvance(c);
                }

                ++currIndex;
                //解决光标被Selection挡住
                if (currIndex == editorField.getCaretPosition()) {
                    _caretSpan = currSpan;
                } else if (currIndex - 1 == editorField.getCaretPosition()) {
                    drawCaret(canvas, x, currPaintY, editorField);
                }

            }// end draw line

            if (editorField.getDocumentProvider().charAt(currIndex - 1) == Language.NEWLINE) {
                ++currLineNum;
            }

            currPaintY += rowHeight;
            if (paintX > _xExtent) {
                // record widest line seen so far
                if (editorField.isWordWrap())
                    _xExtent = paintX;
                else
                    _xExtent = paintX + _spaceWidth * 10;
            }
            ++currRowNum;
        }
        drawBlockPairs(canvas, editorField);
    }

    /**
     * The first row of text to paint, which may be partially visible.
     * Deduced from the clipping rectangle given to onDraw()
     */
    private int getBeginPaintRow(Canvas canvas) {
        Rect bounds = canvas.getClipBounds();
        return bounds.top / rowHeight;
    }

    /**
     * The last row of text to paint, which may be partially visible.
     * Deduced from the clipping rectangle given to onDraw()
     */
    private int getEndPaintRow(Canvas canvas) {
        //clip top and left are inclusive; bottom and right are exclusive
        Rect bounds = canvas.getClipBounds();
        return (bounds.bottom - 1) / rowHeight;
    }


    //查找startIndex小于等于currIndex的最大数
    public int binarySearchSpan(List<Span> spans, int currIndex) {
        int start = 0;
        int end = spans.size() - 1;
        while (start <= end) {
            int mid = (start + end) / 2;
            Span midSpan = spans.get(mid);
            int startIndex = midSpan.startIndex;
            if (currIndex < startIndex) {
                end = mid - 1;
            } else if (currIndex <= startIndex + midSpan.len)
                return mid;
            else
                start = mid + 1;
        }
        return -1;
    }

    public Typeface getTypeface(ColorScheme.Colorable c, EditorField editorField) {
        if (c == ColorScheme.Colorable.KEYWORD)
            return editorField.setting.boldTypeface;
        if (c == ColorScheme.Colorable.COMMENT_REGION)
            return editorField.setting.italicTypeface;
        return editorField.setting.defTypeface;
    }

    private void updateRowHeight() {
        Paint.FontMetricsInt metrics = _brush.getFontMetricsInt();
        rowHeight = metrics.descent - metrics.ascent;
    }

    public int getRowHeight() {
        return rowHeight;
    }

    public void setTypeface(Typeface typeface, EditorSetting setting) {
        _brush.setTypeface(typeface);
        _brushPair.setTypeface(typeface);
        lineDrawer.setTypeface(typeface);
        _brushPairText.setTypeface(setting.boldTypeface);
    }

    public void setBoldTypeface(Typeface typeface) {
        _brushPairText.setTypeface(typeface);

    }

    public void resetLeftOffset(EditorField editorField) {
        if (editorField.setting.isShowLineNumbers) {
            _leftOffset = lineDrawer.measureTextWidth(editorField.getDocumentProvider().getRowCount() + "  ");
        } else {
            _leftOffset = editorField._scrollbar.width;
        }
    }

    public int coordToCharIndexGetI(String rowText, int extent, int x, EditorField editorField) {
        int i = 0;
        temp.isEmoji = false;

        //x-=getAdvance('a')/2;
        for (int len = rowText.length(); i < len; i++) {
            char c = rowText.charAt(i);
            char nextc = temp.isEmoji ? rowText.charAt(i + 1) : ' ';
            extent += getCharAdvanceInRow(c, nextc, extent, temp, editorField);
            if (extent >= x) {
                break;
            }
        }
        return i;
    }

    public int getTabAdvance(int x, EditorField editorField) {
        if (editorField.setting.isShowNonPrinting) {
            return editorField.setting.tabLength * (int) _brush.measureText(Language.GLYPH_SPACE, 0, Language.GLYPH_SPACE.length());
        } else {
            int i = 0;
            if (x != 0) {
                i = (x - _leftOffset) % _spaceWidth;
            }
            if (i < 0)
                i = 0;
            return editorField.setting.tabLength * _spaceWidth - i;
//空格tab开头会导致偏移
//            if (x != 0) {
//                i = (x - _leftOffset) / _spaceWidth % setting.tabLength;
//            }
//            return setting.tabLength * _spaceWidth - i * _spaceWidth;
        }
    }


    public int getCharAdvance(char c) {
        return (int) _brush.measureText(new char[]{c}, 0, 1);
    }

    /**
     * Calculates the x-coordinate extent of charOffset.
     *
     * @return The x-values of left and right edges of charOffset. Pair.first
     * contains the left edge and Pair.second contains the right edge
     */
    public Pair getCharExtent(int charOffset, EditorField editorField) {
        int row = editorField.getDocumentProvider().findRowNumber(charOffset);
        int rowOffset = editorField.getDocumentProvider().getRowOffset(row);
        int left = _leftOffset;
        int right = _leftOffset;
        temp.isEmoji = false;
        String rowText = editorField.getDocumentProvider().getRow(row);


        for (int i = 0,
             len = rowText.length();
             rowOffset + i <= charOffset && i < len; ++i) {
            char c = rowText.charAt(i);
            left = right;
            char nextc = temp.isEmoji ? rowText.charAt(i + 1) : ' ';
            right += getCharAdvanceInRow(c, nextc, right, temp, editorField);

        }
        return new Pair(left, right);
    }


    CharTemp temp = new CharTemp();

    private static class CharTemp {
        public boolean isEmoji = false;
    }

    public int getCharAdvanceInRow(char c, char nextc, int x, CharTemp temp, EditorField editorField) {
        switch (c) {
            case 0xd83c:
            case 0xd83d:
                temp.isEmoji = true;
                return (int) _brush.measureText(new char[]{c, nextc}, 0, 2);
            case Language.NEWLINE:
            case Language.EOF:
                return getEOLAdvance(editorField);
            case ' ':
                return getSpaceAdvance(editorField);
            case Language.TAB:
                return getTabAdvance(x, editorField);
            default:
                if (temp.isEmoji) {
                    temp.isEmoji = false;
                } else {
                    return getCharAdvance(c);
                }
        }
        return 0;
    }
}
