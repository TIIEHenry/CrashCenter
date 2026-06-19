package tiiehenry.code.view;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.util.List;

import tiiehenry.code.doc.Selection;
import tiiehenry.code.doc.DocumentProvider;
import tiiehenry.code.LexTask;
import tiiehenry.code.praser.Span;

//TODO minimise unnecessary invalidate calls

/**
 * TouchNavigationMethod classes implementing their own carets have to override
 * getCaretBloat() to return the size of the drawing area it needs, in excess of
 * the bounding box of the character the caret is on, and use
 * onTextDrawComplete(Canvas) to draw the caret. Currently, only a fixed size
 * caret is allowed, but scalable carets may be implemented in future.
 */
public class TouchNavigationMethod extends GestureDetector.SimpleOnGestureListener {
    private final static Rect _caretBloat = new Rect(0, 0, 0, 0);
    // When the caret is dragged to the edges of the text field, 编辑器将
    // 自动滚动. SCROLL_EDGE_SLOP is the width of these edges in pixels
    // and extends inside the content area, not outside to the padding area
    protected static int SCROLL_EDGE_SLOP = 10;
    /**
     * The radius, in density-independent pixels, around a point of interest
     * where any touch event within that radius is considered to have touched
     * the point of interest itself
     */
    protected static int TOUCH_SLOP = 12;
    protected EditorField _textField;
    protected boolean _isCaretTouched = false;
    private final GestureDetector _gestureDetector;
    private float lastDist;
    private float lastSize;
    private int flingMode;
    private static final int FLING_NONE = 0;
    private static final int FLING_VERTICAL = -1;
    private static final int FLING_HORIZONTAL = 1;

    public TouchNavigationMethod(EditorField textField) {
        _textField = textField;

        _gestureDetector = new GestureDetector(textField.getContext(), this);
        _gestureDetector.setIsLongpressEnabled(true);
    }

    public boolean isDragging = false;

    @Override
    public boolean onDown(MotionEvent e) {
        isDragging = true;
        int x = screenToViewX((int) e.getX());
        int y = screenToViewY((int) e.getY());
        _isCaretTouched = isNearChar(x, y, _textField.getCaretPosition());

        if (_textField.isFlingScrolling()) {
            _textField.stopFlingScrolling();
        } else if (_textField.isSelectText()) {
            if (isNearChar(x, y, _textField.getSelectionStart())) {
                _textField.focusSelectionStart();
                _textField.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                _isCaretTouched = true;
            } else if (isNearChar(x, y, _textField.getSelectionEnd())) {
                _textField.focusSelectionEnd();
                _textField.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                _isCaretTouched = true;
            }
        }

        if (_isCaretTouched) {
            _textField.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }

        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        int x = screenToViewX((int) e.getX());
        int y = screenToViewY((int) e.getY());
        int charOffset = _textField.coordToCharIndex(x, y);

        if (_textField.isSelectText()) {
            int strictCharOffset = _textField.coordToCharIndexStrict(x, y);
            if (_textField.inSelectionRange(strictCharOffset) ||
                    isNearChar(x, y, _textField.getSelectionStart()) ||
                    isNearChar(x, y, _textField.getSelectionEnd())) {
                // do nothing
            } else {
                _textField.selectText(false);
                if (strictCharOffset >= 0) {
                    _textField.moveCaret(charOffset);
                }
            }
        } else {
            if (charOffset >= 0) {
                _textField.moveCaret(charOffset);
            }
        }
        _textField.showIME(true);
        return true;
    }

    /**
     * Note that up events from a flingMode are NOT captured here.
     * Subclasses have to call super.onUp(MotionEvent) in their implementations
     * of onFling().
     * <p>
     * Also, up events from non-primary pointers in a multi-touch situation are
     * not captured here.
     */
    public boolean onUp(MotionEvent e) {
        _textField.stopAutoScrollCaret();
        _isCaretTouched = false;
        isDragging = false;
        lastDist = 0;
        flingMode = FLING_NONE;

        return true;
    }

    /**
     * 不会传入ACTION_UP
     */
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (_isCaretTouched) {
            dragCaret(e2);
        } else if (e2.getPointerCount() == 1) {
            if (flingMode == FLING_NONE) {
                if (Math.abs(distanceX) > Math.abs(distanceY)) {
                    flingMode = FLING_HORIZONTAL;
                } else {
                    flingMode = FLING_VERTICAL;
                }
            }
            if (flingMode == FLING_HORIZONTAL) {
                distanceY = 0;
            } else if (flingMode == FLING_VERTICAL) {
                distanceX = 0;
            }

            scrollView(distanceX, distanceY);

        }
        return true;
    }

    private void dragCaret(MotionEvent e) {
        if (!_textField.isSelectText() && isDragSelect()) {
            _textField.selectText(true);
        }

        int x = (int) e.getX() - _textField.getPaddingLeft();
        int y = (int) e.getY() - _textField.getPaddingTop();
        boolean scrolled = false;

        // If the edges of the textField content area are touched, scroll in the
        // corresponding direction.
        if (x < SCROLL_EDGE_SLOP) {
            scrolled = _textField.autoScrollCaret(EditorField.SCROLL_LEFT);
        } else if (x >= (_textField.getContentWidth() - SCROLL_EDGE_SLOP)) {
            scrolled = _textField.autoScrollCaret(EditorField.SCROLL_RIGHT);
        } else if (y < SCROLL_EDGE_SLOP) {
            scrolled = _textField.autoScrollCaret(EditorField.SCROLL_UP);
        } else if (y >= (_textField.getContentHeight() - SCROLL_EDGE_SLOP)) {
            scrolled = _textField.autoScrollCaret(EditorField.SCROLL_DOWN);
        }

        if (!scrolled) {
            _textField.stopAutoScrollCaret();
            int newCaretIndex = _textField.coordToCharIndex(
                    screenToViewX((int) e.getX()),
                    screenToViewY((int) e.getY())
            );
            if (newCaretIndex >= 0) {
                _textField.moveCaret(newCaretIndex);
            }
        }
    }

    public static int touchSlopVertical = 200;
    public static int touchSlopHorizontal = 100;

    protected void scrollView(float distanceX, float distanceY) {
        int newX = (int) distanceX + _textField.getScrollX();
        int newY = (int) distanceY + _textField.getScrollY();
        _textField.scrollTo(newX, newY);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (!_isCaretTouched) {
            if (flingMode == FLING_HORIZONTAL) {
                velocityY = 0;
            } else if (flingMode == FLING_VERTICAL) {
                velocityX = 0;
            }
            //滑动过程的速度
            _textField.flingScroll((int) (-velocityX * _textField.setting.slidingSpeedHorizontal), (int) (-velocityY * _textField.setting.slidingSpeedVertical));
        }
        onUp(e2);
        return true;
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    boolean canSpringBack() {
        return _textField.getScrollX() < 0 || _textField.getScrollX() > _textField.getMaxScrollX() || _textField.getScrollY() < 0 || _textField.getScrollY() > _textField.getMaxScrollY();
    }

    void springBack() {
        _textField.scroller.springBack(_textField.getScrollX(), _textField.getScrollY(), 0, _textField.getMaxScrollX(), 0, _textField.getMaxScrollY());
    }

    private void onTouchZoom(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_MOVE && e.getPointerCount() == 2) {
            if (lastDist == 0) {
                lastDist = spacing(e);
                lastSize = _textField.getTextSize();
            }
            float dist = spacing(e);
            if (lastDist != 0) {
                _textField.setTextSize((int) (lastSize * (dist / lastDist)));
            }
            return;
        }
        lastDist = 0;
    }

    /**
     * Subclasses overriding this method have to call the superclass method
     */
    public boolean onTouchEvent(MotionEvent event) {
        onTouchZoom(event);
        boolean handled = _gestureDetector.onTouchEvent(event);
        if (!handled && (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            // 传播事件，因为GestureDetector不这样做
            handled = onUp(event);
            if (canSpringBack()) {
                springBack();
                _textField.postInvalidate();
            }
        }
        return handled;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        onDoubleTap(e);
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        _isCaretTouched = true;
        int x = screenToViewX((int) e.getX());
        int y = screenToViewY((int) e.getY());
        int charOffset = _textField.coordToCharIndex(x, y);
        if (_textField.isSelectText() && _textField.inSelectionRange(charOffset)) {
            DocumentProvider doc = _textField.createDocumentProvider();
            LexTask.Selection selection = _textField.getLexTask().expandSelection(doc.toString(), _textField.getSelectionStart(), _textField.getSelectionEnd());
            int line = doc.findLineNumber(charOffset);
            int start = doc.getLineOffset(line);
            int end = doc.getLineOffset(line + 1) - 1;
            if (_textField.getSelectionStart() == start && _textField.getSelectionEnd() == end) {
                _textField.setSelectionRange(selection.start, selection.len);
            } else if (selection.start < start || selection.start + selection.len > end) {
                selectLine(doc, charOffset);
            } else {
                _textField.setSelectionRange(selection.start, selection.len);
            }
//            if (_textField.selectMore();)
        } else {
            if (charOffset >= 0) {
                _textField.moveCaret(charOffset, true);
                DocumentProvider doc = _textField.createDocumentProvider();
                Selection s = getSelectionForIdentifier(doc, charOffset);
                List<Span> spans = doc.getSpans();
                int spanSize = spans.size();
                if (spanSize < 1) {
                    spans.add(new Span(0, 0, 0, doc.docLength(), ColorScheme.Colorable.TEXT));
                }
                int spanIndex = _textField.drawer.binarySearchSpan(spans, _textField.getSelectionStart());
                if (spanIndex != -1) {
                    Span currSpan = spans.get(spanIndex);
                    if (currSpan.startIndex < _textField.getSelectionStart() || currSpan.startIndex + currSpan.len > _textField.getSelectionEnd()) {
                        int lineStart = doc.findLineNumber(currSpan.startIndex);
                        int lineEnd = doc.findLineNumber(currSpan.startIndex + currSpan.len);
                        if (lineStart == lineEnd) {
                            if (s.start < currSpan.startIndex && s.end > currSpan.endIndex()) {
                                _textField.setSelectionRange(currSpan.startIndex, currSpan.len);
                            } else {
                                selectSelection(s);
                            }
                        } else {
                            selectLine(doc, charOffset);
                        }
                    } else {
                        selectSelection(s);
                    }
                } else {
                    selectSelection(s);
                }
            }
        }
        return true;
    }

    private void selectLine(DocumentProvider doc, int charOffset) {
        int line = doc.findLineNumber(charOffset);
        int start = doc.getLineOffset(line);
        int end = doc.getLineOffset(line + 1) - 1;
        if (end < start)
            end = start;
        _textField.setSelectionRange(start, end - start);
    }

    private Selection getSelectionForIdentifier(DocumentProvider doc, int charOffset) {
        int start;
        int end;
        for (start = charOffset; start >= 0; start--) {
            char c = doc.charAt(start);
            if (!Character.isJavaIdentifierPart(c)) {
                break;
            }
        }
        if (start != charOffset) {
            start++;
        }
        for (end = charOffset; end >= 0; end++) {
            char c = doc.charAt(end);
            if (!Character.isJavaIdentifierPart(c)) {
                break;
            }
        }
        return new Selection(start, end);
    }

    private void selectSelection(Selection selection) {
        _textField.setSelectionRange(selection.start, selection.len);
    }

    private void selectIdentifier(DocumentProvider doc, int charOffset) {
        Selection s = getSelectionForIdentifier(doc, charOffset);
//                _textField.selectText(true);
        selectSelection(s);
    }


    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    /**
     * Called by FreeScrollingTextField when it has finished drawing text.
     * Classes extending TouchNavigationMethod can use this to draw, for
     * example, a custom caret.
     * <p>
     * The canvas includes padding in it.
     *
     * @param canvas
     */
    public void onTextDrawComplete(Canvas canvas) {
        //yoyo draw
    }


    //*********************************************************************
    //**************************** Utilities ******************************
    //*********************************************************************

    /**
     * For any printed character, this method returns the amount of space
     * required in excess of the bounding box of the character to draw the
     * caret.
     * Subclasses should override this method if they are drawing their
     * own carets.
     */
    //相当于padding
    public Rect getCaretBloat() {
        return _caretBloat;
    }

    final protected int getPointerId(MotionEvent e) {
        return (e.getAction() & MotionEvent.ACTION_POINTER_ID_MASK)
                >> MotionEvent.ACTION_POINTER_ID_SHIFT;
    }

    /**
     * Converts a x-coordinate from screen coordinates to local coordinates,
     * excluding padding
     */
    final protected int screenToViewX(int x) {
        return x - _textField.getPaddingLeft() + _textField.getScrollX();
    }

    /**
     * Converts a y-coordinate from screen coordinates to local coordinates,
     * excluding padding
     */
    final protected int screenToViewY(int y) {
        return y - _textField.getPaddingTop() + _textField.getScrollY();
    }

    final public boolean isRightHanded() {
        return true;
    }

    final private boolean isDragSelect() {
        return false;
    }

    /**
     * Determine if a point(x,y) on screen is near a character of interest,
     * specified by its index charOffset. The radius of proximity is defined
     * by TOUCH_SLOP.
     *
     * @param x          X-coordinate excluding padding
     * @param y          Y-coordinate excluding padding
     * @param charOffset the character of interest
     * @return Whether (x,y) lies close to the character with index charOffset
     */
    public boolean isNearChar(int x, int y, int charOffset) {
        Rect bounds = _textField.getBoundingBox(charOffset);

        return (y >= (bounds.top - TOUCH_SLOP)
                && y < (bounds.bottom + TOUCH_SLOP)
                && x >= (bounds.left - TOUCH_SLOP)
                && x < (bounds.right + TOUCH_SLOP)
        );
    }
}
