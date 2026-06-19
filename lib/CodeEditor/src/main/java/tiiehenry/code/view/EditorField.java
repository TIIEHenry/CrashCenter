/*
 *****************************************************************************
 *
 * --------------------------------- row length
 * Hello World(\n)                 | 12
 * This is a test of the caret(\n) | 28
 * func|t|ions(\n)                 | 10
 * of this program(EOF)            | 16
 * ---------------------------------
 *
 * The figure illustrates the convention for counting characters.
 * Rows 36 to 39 of a hypothetical text file are shown.
 * The 0th char of the file is off-screen.
 * Assume the first char on screen is the 257th char.
 * The caret is before the char 't' of the word "functions". The caret is drawn
 * as a filled blue rectangle enclosing the 't'.
 *
 * caretPosition == 257 + 12 + 28 + 4 == 301
 *
 * Note 1: EOF (End Of File) is a real char with a length of 1
 * Note 2: Characters enclosed in parentheses are non-printable
 *
 *****************************************************************************
 *
 * There is a difference between rows and lines in TextWarrior.
 * Rows are displayed while lines are a pure logical construct.
 * When there is no word-wrap, a line of text is displayed as a row on screen.
 * With word-wrap, a very long line of text may be split across several rows
 * on screen.
 *
 */
package tiiehenry.code.view;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.text.InputType;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.method.CharacterPickerDialog;
import android.util.AttributeSet;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Magnifier;
import android.widget.OverScroller;

import tiiehenry.code.EditorException;
import tiiehenry.code.LexTask;
import tiiehenry.code.doc.DocumentProvider;
import tiiehenry.code.doc.Pair;
import tiiehenry.code.language.Language;
import tiiehenry.code.language.text.TextLanguage;
import tiiehenry.code.listener.OnEditedChangedListener;
import tiiehenry.code.listener.OnRowChangedListener;
import tiiehenry.code.listener.OnSelectionChangedListener;
import tiiehenry.code.listener.OnTextChangeListener;
import tiiehenry.code.praser.prog.EditorDrawer;
import tiiehenry.code.view.ColorScheme.Colorable;


/**
 * A custom text view that uses a solid shaded caret (aka cursor) instead of a
 * blinking caret and allows a variety of navigation methods to be easily
 * integrated.
 * <p>
 * It also has a built-in syntax highlighting feature. The global programming
 * language syntax to use is specified with Lexer.setLanguage(Language).
 * To disable syntax highlighting, simply pass LanguageNonProg to that function.
 * <p>
 * Responsibilities
 * 1. Display text
 * 2. Display padding
 * 3. Scrolling
 * 4. Store and display caret position and selection range
 * 5. Store font type, font size, and tab length
 * 6. Interpret non-touch input events and shortcut keystrokes, triggering
 * the appropriate inner class controller actions
 * 7. Reset view, set cursor position and selection range
 * <p>
 * Inner class controller responsibilities
 * 1. Caret movement
 * 2. Activate/deactivate selection mode
 * 3. Cut, copy, paste, delete, insert
 * 4. Schedule areas to repaint and analyze for spans in response to edits
 * 5. Directs scrolling if caret movements or edits causes the caret to be off-screen
 * 6. Notify rowListeners when caret row changes
 * 7. Provide helper methods for InputConnection to setComposingText from the IME
 * <p>
 * This class is aware that the underlying text buffer uses an extra char (EOF)
 * to mark the end of the text. The text size reported by the text buffer includes
 * this extra char. Some bounds manipulation is done so that this implementation
 * detail is hidden from client classes.
 */
public abstract class EditorField extends EditorAbstract {

    public EditorSetting setting;


    //---------------------------------------------------------------------
    //--------------------------  Caret Scroll  ---------------------------
    public final static int SCROLL_UP = 0;
    public final static int SCROLL_DOWN = 1;
    public final static int SCROLL_LEFT = 2;
    public final static int SCROLL_RIGHT = 3;
    /**
     * Scale factor for the width of a caret when on a NEWLINE or EOF char.
     * A factor of 1.0 is equals to the width of a space character
     */
    protected static float EMPTY_CARET_WIDTH_SCALE = 0.75f;
    protected static int BASE_TEXT_SIZE_PIXELS = 16;
    protected static long SCROLL_PERIOD = 250; //in milliseconds
    /*
     * Hash map for determining which characters to let the user choose from when
     * a hardware key is long-pressed. For example, long-pressing "e" displays
     * choices of "é, è, ê, ë" and so on.
     * This is biased towards European locales, but is standard Android behavior
     * for TextView.
     *
     * Copied from android.text.method.QwertyKeyListener, dated 2006
     */

    public OverScroller scroller;

    protected TouchNavigationMethod _navMethod;
    /**
     * 光标位置
     */
    protected int caretPosition = 0;
    protected int _selectionAnchor = -1; // inclusive
    protected int _selectionEdge = -1; // exclusive
    private boolean editable = true;
    public int textSize;
    public EditorDrawer drawer;

    public void set_selectionEdge(int i) {
        _selectionEdge = i;
    }

    public void set_selectionAnchor(int i) {
        _selectionAnchor = i;
    }

    protected OnTextChangeListener _textLis;

    protected AutoCompletePanel _autoCompletePanel;
    protected EditorController fieldController; // the controller in MVC
    EditorInputConnection _inputConnection;

    OnRowChangedListener _rowLis;
    private OnEditedChangedListener _onEditedChangedListener;
    OnSelectionChangedListener _selModeLis = new OnSelectionChangedListener() {
        @Override
        public void onSelectionChanged(boolean active, int selStart, int selEnd) {

        }
    };



    protected ClipboardManager _clipboardManager;

    private int _topOffset;

    private boolean _isLayout;

    InputMethodManager _inputMethodManager;
    private Object magnifier;

    private final Runnable _scrollCaretDownTask = new Runnable() {
        @Override
        public void run() {
            fieldController.moveCaretDown();
            if (!caretOnLastRowOfFile()) {
                postDelayed(_scrollCaretDownTask, SCROLL_PERIOD);
            }
        }
    };
    private final Runnable _scrollCaretUpTask = new Runnable() {
        @Override
        public void run() {
            fieldController.moveCaretUp();
            if (!caretOnFirstRowOfFile()) {
                postDelayed(_scrollCaretUpTask, SCROLL_PERIOD);
            }
        }
    };
    private final Runnable _scrollCaretLeftTask = new Runnable() {
        @Override
        public void run() {
            fieldController.moveCaretLeft(false);
            if (caretPosition > 0 &&
                    getCaretRow() == getDocumentProvider().findRowNumber(caretPosition - 1)) {
                postDelayed(_scrollCaretLeftTask, SCROLL_PERIOD);
            }
        }
    };
    private final Runnable _scrollCaretRightTask = new Runnable() {
        @Override
        public void run() {
            fieldController.moveCaretRight(false);
            if (!caretOnEOF() &&
                    getCaretRow() == getDocumentProvider().findRowNumber(caretPosition + 1)) {
                postDelayed(_scrollCaretRightTask, SCROLL_PERIOD);
            }
        }
    };
    public MaterialSlideBar _scrollbar;
    private int _minFling;


    public EditorField(Context context) {
        super(context);
        initTextField(context);
    }

    public EditorField(Context context, AttributeSet attrs) {
        super(context, attrs);
        initTextField(context);
    }

    public EditorField(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initTextField(context);
    }

    protected void initTextField(Context context) {
        setting = new EditorSetting();
        setting.doc = new DocumentProvider(this);
        setNavigationMethod(new YoyoNavigationMethod(this));
        scroller = new OverScroller(context);
        ViewConfiguration config = ViewConfiguration.get(getContext());
        _minFling = config.getScaledMinimumFlingVelocity();

        _clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        _inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        initView();
    }

    private CharSequence getSetVariableBeforePosition(int caretPosition) {
        int currPos = caretPosition;
        for (; currPos > 0; currPos--) {
            char c = getDocumentProvider().charAt(currPos - 1);
            if (getLanguage().isSplitChar(c)) {
                break;
            }
        }
        if (caretPosition > currPos) {
            return getDocumentProvider().subSequence(currPos, caretPosition - currPos);
        } else {

        }
        return null;
    }

    private void activeAutoComplete(int caretPosition) {
        CharSequence s = getSetVariableBeforePosition(caretPosition);
        if (s != null && s.length() > 0) {
            _autoCompletePanel.update(s);
        } else {
            _autoCompletePanel.dismiss();
        }
    }

    private void initView() {
        _scrollbar = new MaterialSlideBar(this);
        fieldController = new EditorController(this);
        _inputConnection = new EditorInputConnection(this);
        drawer = new EditorDrawer();
/*
        _brush = new Paint();
        _brush.setAntiAlias(true);
        _brush.setTextSize(BASE_TEXT_SIZE_PIXELS);
        _brushLine = new Paint();
        _brushLine.setAntiAlias(true);
        _brushLine.setTextSize(BASE_TEXT_SIZE_PIXELS);
        _brushLineHighLignt = new Paint();
        _brushLineHighLignt.setAntiAlias(true);
        _brushLineErr = new Paint();
        _brushLineErr.setAntiAlias(true);
        _brushLineErr.setTextSize(BASE_TEXT_SIZE_PIXELS);
        _brushPair = new Paint();
        _brushPair.setAntiAlias(true);
        _brushPair.setTextSize(BASE_TEXT_SIZE_PIXELS);
        _brushPairText = new Paint();
        _brushPairText.setAntiAlias(true);
        _brushPairText.setTextSize(BASE_TEXT_SIZE_PIXELS);*/
        setLongClickable(true);
        setFocusableInTouchMode(true);
        setHapticFeedbackEnabled(true);//默认长按可以振动
        setWillNotDraw(false);

        _textLis = new OnTextChangeListener() {

            @Override
            public void onNewLine(String c, int _caretPosition, int p2) {
//                _caretSpan.len += 1;
                _autoCompletePanel.dismiss();
            }


            @Override
            public void onDel(CharSequence text, int _caretPosition, int delCount) {
//                if (delCount <= _caretSpan.len){
//                    _caretSpan.len -= 1;
//                }
                activeAutoComplete(_caretPosition - 1);
            }

            /**
             * before  deternSpan
             * @param text
             * @param caretPosition
             * @param addCount
             */
            @Override
            public void onAdd(CharSequence text, int caretPosition, int addCount) {
                if (text.length() == 0)
                    return;
//                _caretSpan.len += addCount;
//                _caretSpan.startIndex,_caretSpan.len+1
                activeAutoComplete(caretPosition);
            }
        };

        resetView();
        _autoCompletePanel = new AutoCompletePanel(this);

        ColorScheme.addOnColorChangedListener(new ColorScheme.OnColorChangedListener() {
            @Override
            public void onColorChanged() {
                setBackgroundColor(Colorable.BACKGROUND.getColor());
                /*_brushPair.setColor(Colorable.SELECTION_BACKGROUND.getColor());
                _brushPairText.setColor(Colorable.SELECTION_FOREGROUND.getColor());
                _brushLineErr.setColor(Colorable.ERROR.getColor());
                _brushLineHighLignt.setColor(Colorable.LINE_HIGHLIGHT.getColor());*/
            }
        });
        invalidate();
    }

    public void setLexTask(LexTask<?, ?> mLexTask) {
        setting.lexTask = mLexTask;
        setting.language = mLexTask.getLanguage();
        fieldController.lexer.setWorkerTask(mLexTask);
        fieldController.determineSpans();
    }

    public LexTask<?, ?> getLexTask() {
        return setting.lexTask;
    }

    public EditorSetting getSetting() {
        return setting;
    }

    public void setSetting(EditorSetting s) {
        setting = s;
        setDocumentProvider(s.doc);
        setWordWrap(s.isWordWrap);
        setLexTask(s.lexTask);
        _scrollbar.updateHeight();
    }

    public void setDefaultLexTask() {
        setLexTask(TextLanguage.defaultLexTask);
    }

    public void setLanguage(Language lan) {
        setting.language = lan;
    }

    public Language getLanguage() {
        return setting.language;
    }

    public void setMagnifier(Object m) {
        magnifier = m;
    }

//    public Magnifier getMagnifier() {
//        return magnifier;
//    }

    public void showMagnifier(float x, float y) {
        if (magnifier != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ((Magnifier) magnifier).show(x, y);
            }
        }
    }

    public void hideMagnifier() {
        if (magnifier != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ((Magnifier) magnifier).dismiss();
            }
        }
    }

    public AutoCompletePanel getAutoCompletePanel() {
        return _autoCompletePanel;
    }

    public boolean canFormat() {
        return isEditable() && getLexTask().canFormat();
    }

    public int getTopOffset() {
        return _topOffset;
    }

    public int getCaretX() {
        return drawer.getCaretX();
    }

    public int getCaretY() {
        return drawer.getCaretY();
    }

    public int getLeftOffset() {
        return drawer.getLeftOffset();
    }

    public float getTextSize() {
        return drawer.getTextSize();
    }

    public void setTextSize(int pix) {
        if (pix <= 9 || pix >= 90 || pix == drawer.getTextSize()) {
            return;
        }
        textSize = pix;
        double oldHeight = drawer.getRowHeight();
        double oldWidth = getAdvance('a');
        drawer.setTextSize(pix);

        analyzeIfWordWrap(getDocumentProvider());
        fieldController.updateCaretRow();
        _scrollbar.updateHeight();
        TouchNavigationMethod.touchSlopVertical = drawer.getRowHeight() * 3;
        if (TouchNavigationMethod.touchSlopHorizontal != 0)
            TouchNavigationMethod.touchSlopHorizontal = drawer.getRowHeight();
        double x = getScrollX() * ((double) getAdvance('a') / oldWidth);
        double y = getScrollY() * ((double) drawer.getRowHeight() / oldHeight);
        scrollTo((int) x, (int) y);
        int idx = coordToCharIndex(getScrollX(), getScrollY());
        if (!makeCharVisible(idx)) {
            invalidate();
        }
    }

    public void replaceText(int from, int charCount, String text) {
        if (!isEditable())
            return;
        getDocumentProvider().beginBatchEdit();
        fieldController.replaceText(from, charCount, text);
        fieldController.stopTextComposing();
        getDocumentProvider().endBatchEdit();
    }


    public void stopScrolling() {
        stopFlingScrolling();
        stopNestedScroll();
    }

    private void resetView() {
        setCaretPosition(0);
        setCaretRow(0);
        drawer.setXExtent(0);
        fieldController.setSelectText(false);
        fieldController.stopTextComposing();
        getDocumentProvider().clearSpans();
        if (getContentWidth() > 0 || !getDocumentProvider().isWordWrap()) {
            getDocumentProvider().analyzeWordWrap();
        }
        if (_rowLis != null)
            _rowLis.onRowChanged(0);
        scrollTo(0, 0);
    }

    /**
     * Sets the text displayed to the document referenced by hDoc. The view
     * state is reset and the view is invalidated as a side-effect.
     */
    public void setDocumentProvider(DocumentProvider hDoc) {
        setting.doc = hDoc;
        resetView();
        fieldController.cancelSpanning(); //stop existing lex threads
        fieldController.determineSpans();
        invalidate();
    }

    /**
     * Returns a DocumentProvider that references the same Document used by the
     * FreeScrollingTextField.
     */
    public DocumentProvider createDocumentProvider() {
        return new DocumentProvider(setting.doc);
    }

    public DocumentProvider getDocumentProvider() {
        return setting.doc;
    }

    //to do
    //点击行会触发
    public void setRowListener(OnRowChangedListener rLis) {
        _rowLis = rLis;
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener sLis) {
        _selModeLis = sLis;
    }

    /**
     * Sets the caret navigation method used by this text field
     */
    public void setNavigationMethod(TouchNavigationMethod navMethod) {
        _navMethod = navMethod;
    }

    public void setOnEditStateChangedListener(OnEditedChangedListener listener) {
        _onEditedChangedListener = listener;
    }

    // this used to be isDirty(), but was renamed to avoid conflicts with Android API 11
    public boolean isEdited() {
        return setting.isEdited;
    }

    public void setEdited(boolean set) {
        setting.isEdited = set;
        if (_onEditedChangedListener != null)
            _onEditedChangedListener.onEditedChanged(set);
    }

    public void setEditable(boolean editable) {
        this.editable = editable;

        showIME(isEditable());
//        ((View) getParent()).setFocusable(!editable);
//        ((View) getParent()).setFocusableInTouchMode(!editable);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            setFocusedByDefault(isEditable());
//        }
    }


    public boolean isEditable() {
        return editable;
    }


    //---------------------------------------------------------------------
    //-------------------------- Paint methods ----------------------------


    //---------------------------------------------------------------------
    //------------------------- Layout methods ----------------------------
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(useAllDimensions(widthMeasureSpec), useAllDimensions(heightMeasureSpec));
    }


    Rect onLayoutRect = new Rect();

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed) {
            onLayoutRect.bottom = 0;
            onLayoutRect.top = 0;
            onLayoutRect.left = 0;
            onLayoutRect.right = 0;
            getWindowVisibleDisplayFrame(onLayoutRect);
            _topOffset = onLayoutRect.top + onLayoutRect.height() - getHeight();
            if (!_isLayout) {
                respan();
            }
            _isLayout = right > 0;
            invalidate();
            _autoCompletePanel.setWidth((int) (getWidth() * 0.62));

        }
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (oldw != w) {
            analyzeIfWordWrap(getDocumentProvider());
        }
        fieldController.updateCaretRow();
        _scrollbar.updateHeight();
        if (h < oldh) {
            makeCharVisible(caretPosition);
        }
    }

    private int useAllDimensions(int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int result = MeasureSpec.getSize(measureSpec);

        if (specMode != MeasureSpec.EXACTLY && specMode != MeasureSpec.AT_MOST) {
            result = Integer.MAX_VALUE;
            EditorException.fail("MeasureSpec cannot be UNSPECIFIED. Setting dimensions to max.");
        }

        return result;
    }

    protected int getNumVisibleRows() {
        return (int) Math.ceil((double) getContentHeight() / drawer.getRowHeight());
    }


    /*
     The only methods that have to worry about padding are invalidate, draw
     and computeVerticalScrollRange() methods. Other methods can assume that
     the text completely fills a rectangular viewport given by getContentWidth()
     and getContentHeight()
     */
    protected int getContentHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    public int getContentWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    /**
     * Determines if the View has been layout or is still being constructed
     */
    public boolean hasLayout() {
        return (getWidth() == 0); // simplistic implementation, but should work for most cases
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        //translate clipping region to create padding around edges
        canvas.clipRect(getScrollX() + getPaddingLeft(),
                getScrollY() + getPaddingTop(),
                getScrollX() + getWidth() - getPaddingRight(),
                getScrollY() + getHeight() - getPaddingBottom());
        canvas.translate(getPaddingLeft(), getPaddingTop());

        drawer.realDraw(canvas, this);

//        canvas.restore();

        _navMethod.onTextDrawComplete(canvas);

        _scrollbar.draw(canvas);
//        canvas.restore();
    }


    @Override
    final public int getRowWidth() {
        drawer.resetLeftOffset(this);
        return getContentWidth() - getLeftOffset() - _scrollbar.width;
    }

    public int getBound() {
        return -getLeftOffset();
    }


    /**
     * Returns printed width of c.
     * <p>
     * Takes into account user-specified tab width and also handles
     * application-defined widths for NEWLINE and EOF
     *
     * @param c Character to measure
     * @return Advance of character, in pixels
     */
    @Override
    public int getAdvance(char c) {
        return drawer.getAdvance(this, c, 0);
    }


    //---------------------------------------------------------------------
    //------------------- Scrolling and touch -----------------------------

    /**
     * Invalidate rows from startRow (inclusive) to endRow (exclusive)
     */
    void invalidateRows(int startRow, int endRow) {
        EditorException.assertVerbose(startRow <= endRow && startRow >= 0,
                "Invalid startRow and/or endRow");

        Rect caretSpill = _navMethod.getCaretBloat();
        //TODO The ascent of (startRow+1) may jut inside startRow, so part of
        // that rows have to be invalidated as well.
        // This is a problem for Thai, Vietnamese and Indic scripts
        Paint.FontMetricsInt metrics = drawer.getFontMetricsInt();
        int top = startRow * drawer.getRowHeight() + getPaddingTop();
        top -= Math.max(caretSpill.top, metrics.descent);
        top = Math.max(0, top);

        super.invalidate(0,
                top,
                getScrollX() + getWidth(),
                endRow * drawer.getRowHeight() + getPaddingTop() + caretSpill.bottom);
    }

    /**
     * Invalidate rows from startRow (inclusive) to the end of the field
     */
    void invalidateFromRow(int startRow) {
        EditorException.assertVerbose(startRow >= 0,
                "Invalid startRow");

        Rect caretSpill = _navMethod.getCaretBloat();
        //TODO The ascent of (startRow+1) may jut inside startRow, so part of
        // that rows have to be invalidated as well.
        // This is a problem for Thai, Vietnamese and Indic scripts
        Paint.FontMetricsInt metrics = drawer.getFontMetricsInt();
        int top = startRow * drawer.getRowHeight() + getPaddingTop();
        top -= Math.max(caretSpill.top, metrics.descent);
        top = Math.max(0, top);

        super.invalidate(0,
                top,
                getScrollX() + getWidth(),
                getScrollY() + getHeight());
    }

    void invalidateCaretRow() {
        invalidateRows(getCaretRow(), getCaretRow() + 1);
    }

    void invalidateSelectionRows() {
        int startRow = getDocumentProvider().findRowNumber(_selectionAnchor);
        int endRow = getDocumentProvider().findRowNumber(_selectionEdge);

        invalidateRows(startRow, endRow + 1);
    }

    /**
     * Scrolls the text horizontally and/or vertically if the character
     * specified by charOffset is not in the visible text region.
     * The view is invalidated if it is scrolled.
     *
     * @param charOffset The index of the character to make visible
     * @return True if the drawing area was scrolled horizontally
     * and/or vertically
     */
    boolean makeCharVisible(int charOffset) {
        EditorException.assertVerbose(
                charOffset >= 0 && charOffset < getDocumentProvider().docLength(),
                "Invalid charOffset given");
        int scrollVerticalBy = makeCharRowVisible(charOffset);
        int scrollHorizontalBy = makeCharColumnVisible(charOffset);

        if (scrollVerticalBy == 0 && scrollHorizontalBy == 0) {
            return false;
        } else {
            scrollBy(scrollHorizontalBy, scrollVerticalBy);
            return true;
        }
    }

    /**
     * Calculates the amount to scroll vertically if the char is not
     * in the visible region.
     *
     * @param charOffset The index of the character to make visible
     * @return The amount to scroll vertically
     */
    private int makeCharRowVisible(int charOffset) {
        int scrollBy = 0;
        int charTop = getDocumentProvider().findRowNumber(charOffset) * drawer.getRowHeight();
        int charBottom = charTop + drawer.getRowHeight();

        if (charTop < getScrollY()) {
            scrollBy = charTop - getScrollY();
        } else if (charBottom > (getScrollY() + getContentHeight())) {
            scrollBy = charBottom - getScrollY() - getContentHeight();
        }

        return scrollBy;
    }

    /**
     * Calculates the amount to scroll horizontally if the char is not
     * in the visible region.
     *
     * @param charOffset The index of the character to make visible
     * @return The amount to scroll horizontally
     */
    private int makeCharColumnVisible(int charOffset) {
        int scrollBy = 0;
        Pair visibleRange = drawer.getCharExtent(charOffset, this);

        int charLeft = visibleRange.first;
        int charRight = visibleRange.second;

        if (charRight > (getScrollX() + getContentWidth())) {
            scrollBy = charRight - getScrollX() - getContentWidth();
        }

        if (charLeft < getScrollX() + drawer.getAlphaWidth()) {
            scrollBy = charLeft - getScrollX() -  drawer.getAlphaWidth();
        }

        return scrollBy;
    }


    /**
     * Returns the bounding box of a character in the text field.
     * The coordinate system used is one where (0, 0) is the top left corner
     * of the text, before padding is added.
     *
     * @param charOffset The character offset of the character of interest
     * @return Rect(left, top, right, bottom) of the character bounds,
     * or Rect(-1, -1, -1, -1) if there is no character at that coordinate.
     */
    Rect getBoundingBox(int charOffset) {
        if (charOffset < 0 || charOffset >= getDocumentProvider().docLength()) {
            return new Rect(-1, -1, -1, -1);
        }

        int row = getDocumentProvider().findRowNumber(charOffset);
        int top = row * drawer.getRowHeight();
        int bottom = top + drawer.getRowHeight();

        Pair xExtent = drawer.getCharExtent(charOffset, this);
        int left = xExtent.first;
        int right = xExtent.second;

        return new Rect(left, top, right, bottom);
    }

    /**
     * Maps a coordinate to the character that it is on. If the coordinate is
     * on empty space, the nearest character on the corresponding row is returned.
     * If there is no character on the row, -1 is returned.
     * <p>
     * The coordinates passed in should not have padding applied to them.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @return The index of the closest character, or -1 if there is
     * no character or nearest character at that coordinate
     */
    int coordToCharIndex(int x, int y) {
        int row = y / drawer.getRowHeight();
        if (row > getDocumentProvider().getRowCount()) {
            return getDocumentProvider().docLength() - 1;
        }

        int charIndex = getDocumentProvider().getRowOffset(row);
        if (charIndex < 0 || x < 0) {
            return -1;//non-existent row
        }

        String rowText = getDocumentProvider().getRow(row);
        int i = drawer.coordToCharIndexGetI(rowText, drawer.getLeftOffset(), x, this);

        if (i < rowText.length()) {
            return charIndex + i;
        }
        //nearest char is last char of line
        return charIndex + i - 1;
    }

    /**
     * Maps a coordinate to the character that it is on.
     * Returns -1 if there is no character on the coordinate.
     * <p>
     * The coordinates passed in should not have padding applied to them.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @return The index of the character that is on the coordinate,
     * or -1 if there is no character at that coordinate.
     */
    int coordToCharIndexStrict(int x, int y) {
        int row = y / drawer.getRowHeight();
        int charIndex = getDocumentProvider().getRowOffset(row);

        if (charIndex < 0 || x < 0) {
            return -1;//non-existent row
        }

        String rowText = getDocumentProvider().getRow(row);
        int i = drawer.coordToCharIndexGetI(rowText, 0, x, this);

        if (i < rowText.length()) {
            return charIndex + i;
        }
        //no char enclosing x
        return -1;
    }

    /**
     * Not private to allow access by TouchNavigationMethod
     *
     * @return The maximum x-value that can be scrolled to for the current rows
     * of text in the viewport.
     */
    int getMaxScrollX() {
        if (isWordWrap()) {
            return drawer.getLeftOffset();
        } else {
            return Math.max(0, getMaxWidth() - getContentWidth() + _navMethod.getCaretBloat().right + drawer.getAlphaWidth());
        }
    }

    public int getMaxWidth() {
        return drawer.getXExtent();
    }

    public int getMaxHeight() {
        return getDocumentProvider().getRowCount() * drawer.getRowHeight();
    }

    public int getMaxHeightForScroll() {
        return getDocumentProvider().getRowCount() * drawer.getRowHeight() + getContentHeight() / 2;
    }

    /**
     * Not private to allow access by TouchNavigationMethod
     *
     * @return The maximum y-value that can be scrolled to.
     */
    int getMaxScrollY() {
        return Math.max(0, getMaxHeightForScroll() - getContentHeight() + _navMethod.getCaretBloat().bottom);
    }

    String TAG = "CodeEditor";

    @Override
    protected int computeVerticalScrollOffset() {
        return Math.max(0, super.computeVerticalScrollOffset());
    }


    @Override
    protected int computeVerticalScrollExtent() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }


    @Override
    protected int computeVerticalScrollRange() {
        int rowCount = getDocumentProvider().getRowCount();
        final int contentHeight = getHeight();
        if (rowCount == 0) {
            return contentHeight;
        }

        int scrollRange = rowCount * drawer.getRowHeight();
        final int scrollY = getScrollY();
        final int overscrollBottom = Math.max(0, scrollRange - contentHeight);
        if (scrollY < 0) {
            scrollRange -= scrollY;
        } else if (scrollY > overscrollBottom) {
            scrollRange += scrollY - overscrollBottom;
        }
        return scrollRange;
    }

    public void scrollTo(int x, int y) {
        int left = -TouchNavigationMethod.touchSlopHorizontal;
        int right = getMaxScrollX() + TouchNavigationMethod.touchSlopHorizontal;
        int top = -TouchNavigationMethod.touchSlopVertical;
        int bottom = getDocumentProvider().getRowCount() * drawer.getRowHeight();
        if (x < left) {
            x = left;
        } else if (x > right) {
            x = right;
        }
        if (y < top) {
            y = top;
        } else if (y > bottom) {
            y = bottom;
        }
        super.scrollTo(x, y);
    }

    /*
     *//**
     * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param dx the number of pixels to scroll by on the X axis
     * @param dy the number of pixels to scroll by on the Y axis
     *//*
    public final void smoothScrollBy(int dx, int dy) {
        if (getHeight() == 0) {
            // Nothing to do.
            return;
        }
        long duration = AnimationUtils.currentAnimationTimeMillis() - mLastScroll;
        if (duration > 250) {
            //final int maxY = getMaxScrollX();
            final int scrollY = getScrollY();
            final int scrollX = getScrollX();

            //dy = Math.max(0, Math.min(scrollY + dy, maxY)) - scrollY;

            scroller.startScroll(scrollX, scrollY, dx, dy);
            postInvalidate();
        } else {
            if (!scroller.isFinished()) {
                scroller.abortAnimation();
            }
            scrollBy(dx, dy);
        }
        mLastScroll = AnimationUtils.currentAnimationTimeMillis();
    }

    *//**
     * Like {@link #scrollTo}, but scroll smoothly instead of immediately.
     *
     * @param x the position where to scroll on the X axis
     * @param y the position where to scroll on the Y axis
     *//*
    public final void smoothScrollTo(int x, int y) {
        smoothScrollBy(x - getScrollX(), y - getScrollY());
    }*/


    /**
     * Start fling scrolling
     */
    void flingScroll(int velocityX, int velocityY) {
      /*  if (velocityX == 0 && velocityY == 0)
            _navMethod.springBack();
        else*/
        scroller.fling(getScrollX(), getScrollY(), velocityX, velocityY,
                -TouchNavigationMethod.touchSlopHorizontal, getMaxScrollX() + TouchNavigationMethod.touchSlopHorizontal, -TouchNavigationMethod.touchSlopVertical, getMaxScrollY() + TouchNavigationMethod.touchSlopVertical);
        // Keep on drawing until the animation has finished.
        postInvalidate();
//        postInvalidateOnAnimation();
    }

    public boolean isFlingScrolling() {
        return !scroller.isFinished();
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), scroller.getCurrY());
            postInvalidate();
        } else {
            if (!_navMethod.isDragging)
                if (_navMethod.canSpringBack()) { // TODO X边界还要改我
                    _navMethod.springBack();
                    postInvalidate();
                }
        }
    }
//    @Override
//    public void computeScroll() {
//        super.computeScroll();
//        if (getScrollX() < 0 ||getScrollX() >getMaxScrollX()|| getScrollY() < 0 || getScrollY() > getMaxScrollY()) { // TODO X边界还要改我
//            _navMethod.springBack();
//            postInvalidate();
//        }
//    }
//---------------------------------------------------------------------
    //------------------------- Caret methods -----------------------------

    public void stopFlingScrolling() {
        scroller.forceFinished(true);
    }

    /**
     * Starting scrolling continuously in scrollDir.
     * Not private to allow access by TouchNavigationMethod.
     *
     * @return True if auto-scrolling started
     */
    boolean autoScrollCaret(int scrollDir) {
        boolean scrolled = false;
        switch (scrollDir) {
            case SCROLL_UP:
                removeCallbacks(_scrollCaretUpTask);
                if ((!caretOnFirstRowOfFile())) {
                    post(_scrollCaretUpTask);
                    scrolled = true;
                }
                break;
            case SCROLL_DOWN:
                removeCallbacks(_scrollCaretDownTask);
                if (!caretOnLastRowOfFile()) {
                    post(_scrollCaretDownTask);
                    scrolled = true;
                }
                break;
            case SCROLL_LEFT:
                removeCallbacks(_scrollCaretLeftTask);
                if (caretPosition > 0 &&
                        getCaretRow() == getDocumentProvider().findRowNumber(caretPosition - 1)) {
                    post(_scrollCaretLeftTask);
                    scrolled = true;
                }
                break;
            case SCROLL_RIGHT:
                removeCallbacks(_scrollCaretRightTask);
                if (!caretOnEOF() &&
                        getCaretRow() == getDocumentProvider().findRowNumber(caretPosition + 1)) {
                    post(_scrollCaretRightTask);
                    scrolled = true;
                }
                break;
            default:
                EditorException.fail("Invalid scroll direction");
                break;
        }
        return scrolled;
    }

    /**
     * Stops automatic scrolling initiated by autoScrollCaret(int).
     * Not private to allow access by TouchNavigationMethod
     */
    void stopAutoScrollCaret() {
        removeCallbacks(_scrollCaretDownTask);
        removeCallbacks(_scrollCaretUpTask);
        removeCallbacks(_scrollCaretLeftTask);
        removeCallbacks(_scrollCaretRightTask);
    }

    //获取当前行，有阴影的
    public int getCaretRow() {
        return drawer.getCaretRow();
    }

    public void setCaretRow(int _caretRow) {
        drawer.setCaretRow(_caretRow);
    }

    public void setCaretPosition(int n) {
        caretPosition = n;
        drawer.resetBlockPair(this);
    }

    public int getCaretPosition() {
        return caretPosition;
    }


    public void moveCaret(int i) {
        moveCaret(i, false);
    }

    /**
     * Sets the caret to position i, scrolls it to view and invalidates
     * the necessary areas for redrawing
     *
     * @param i The character index that the caret should be set to
     */
    public void moveCaret(int i, boolean isYoyoTouch) {
        fieldController.moveCaret(i, isYoyoTouch);
    }

    /**
     * Sets the caret one position back, scrolls it on screen, and invalidates
     * the necessary areas for redrawing.
     * <p>
     * If the caret is already on the first character, nothing will happen.
     */
    public void moveCaretLeft() {
        moveCaretLeft(false);
    }

    public void moveCaretLeft(boolean isTyping) {
        fieldController.moveCaretLeft(isTyping);
    }

    /**
     * Sets the caret one position forward, scrolls it on screen, and
     * invalidates the necessary areas for redrawing.
     * <p>
     * If the caret is already on the last character, nothing will happen.
     */
    public void moveCaretRight() {
        moveCaretRight(false);
    }

    public void moveCaretRight(boolean isTyping) {
        fieldController.moveCaretRight(isTyping);
    }

    /**
     * Sets the caret one row down, scrolls it on screen, and invalidates the
     * necessary areas for redrawing.
     * <p>
     * If the caret is already on the last row, nothing will happen.
     */
    public void moveCaretDown() {
        moveCaretDown(false);
    }

    public void moveCaretDown(boolean isTyping) {
        fieldController.moveCaretDown(isTyping);
    }

    /**
     * Sets the caret one row up, scrolls it on screen, and invalidates the
     * necessary areas for redrawing.
     * <p>
     * If the caret is already on the first row, nothing will happen.
     */
    public void moveCaretUp() {
        moveCaretUp(false);
    }

    public void moveCaretUp(boolean isTyping) {
        fieldController.moveCaretUp(isTyping);
    }

    /**
     * Scrolls the caret into view if it is not on screen
     */
    public void focusCaret() {
        makeCharVisible(caretPosition);
    }


    //---------------------------------------------------------------------
    //------------------------- Text Selection ----------------------------

    /**
     * @return The column number where charOffset appears on
     */
    protected int getColumn(int charOffset) {
        int row = getDocumentProvider().findRowNumber(charOffset);
        EditorException.assertVerbose(row >= 0,
                "Invalid char offset given to getColumn");
        int firstCharOfRow = getDocumentProvider().getRowOffset(row);
        return charOffset - firstCharOfRow;
    }

    protected boolean caretOnFirstRowOfFile() {
        return (getCaretRow() == 0);
    }

    protected boolean caretOnLastRowOfFile() {
        return (getCaretRow() == (getDocumentProvider().getRowCount() - 1));
    }

    protected boolean caretOnEOF() {
        return (caretPosition == (getDocumentProvider().docLength() - 1));
    }

    public final boolean isSelectText() {
        return fieldController.isSelectText();
    }

    public final boolean isSelectText2() {
        return fieldController.isSelectText2();
    }

    /**
     * Enter or exit select mode.
     * Invalidates necessary areas for repainting.
     *
     * @param mode If true, enter select mode; else exit select mode
     */
    public void selectText(boolean mode) {
        if (fieldController.isSelectText()) {
            if (!mode) {
                invalidateSelectionRows();
                fieldController.setSelectText(false);
            }
        } else if (mode) {
            invalidateCaretRow();
            fieldController.setSelectText(true);
        }
    }

    public void setIsSelectionMode(boolean mode) {
        fieldController._isInSelectionMode = mode;

    }

    public void selectAll() {
        selectAll(true);
    }

    public void selectAll(boolean stopInput) {
        fieldController.setSelectionRange(0, getDocumentProvider().docLength() - 1, false, true, stopInput);
    }

    public void selectMore() {
        LexTask.Selection selection = getLexTask().expandSelection(getDocumentProvider().toString(), getSelectionStart(), getSelectionEnd());
        fieldController.setSelectionRange(selection.start, selection.len, false, true);
    }

    public void setSelection(int beginPosition, int endPosition) {
        fieldController.setSelectionRange(beginPosition, endPosition - beginPosition, true, false);
    }

    public void setSelection(int beginPosition, int endPosition, boolean scrollToStart,
                             boolean mode) {
        fieldController.setSelectionRange(beginPosition, endPosition - beginPosition, scrollToStart, mode);
    }

    public void setSelectionRange(int beginPosition, int numChars) {
        fieldController.setSelectionRange(beginPosition, numChars, true, true);
    }

    public boolean inSelectionRange(int charOffset) {
        return fieldController.inSelectionRange(charOffset);
    }

    public int getSelectionStart() {
        if (_selectionAnchor < 0) {
            return caretPosition;
        } else {
            return _selectionAnchor;
        }
    }

    public int getSelectionEnd() {
        if (_selectionEdge < 0) {
            return caretPosition;
        } else {
            return _selectionEdge;
        }
    }

    public void focusSelectionStart() {
        fieldController.focusSelection(true);
    }

    public void focusSelectionEnd() {
        fieldController.focusSelection(false);
    }


    //---------------------------------------------------------------------
    //------------------------- Formatting methods ------------------------

    public void cut() {
        cut(_clipboardManager);
    }

    public void cut(ClipboardManager cb) {
        if (_selectionAnchor != _selectionEdge) {
            fieldController.cut(cb);
        }
    }

    public void copy() {
        copy(_clipboardManager);
    }

    public void copy(ClipboardManager cb) {
        if (_selectionAnchor != _selectionEdge) {
            fieldController.copy(cb);
        }
//        selectText(false);
    }

    public void paste() {
        ClipData clipData = _clipboardManager.getPrimaryClip();
        if (clipData != null) {
            CharSequence text = clipData.getItemAt(0).getText();
            if (text != null) {
                fieldController.paste(text.toString());
            }
        }
    }

    public void paste(String text) {
        fieldController.paste(text);
    }

    private boolean reachedNextSpan(int charIndex, Pair span) {
        return span != null && (charIndex == span.first);
    }

    public void respan() {
        fieldController.determineSpans();
    }

    public void cancelSpanning() {
        fieldController.cancelSpanning();
    }

    /**
     * Sets the text to use the new typeface, scrolls the view to display the
     * caret if needed, and invalidates the entire view
     */
    public void setTypeface(Typeface typeface) {
        setting.defTypeface = typeface;
        setting.boldTypeface = Typeface.create(typeface, Typeface.BOLD);
        setting.italicTypeface = Typeface.create(typeface, Typeface.ITALIC);
        drawer.setTypeface(typeface, setting);
        analyzeIfWordWrap(getDocumentProvider());
        fieldController.updateCaretRow();
        if (!makeCharVisible(caretPosition)) {
            invalidate();
        }
    }

    public void setItalicTypeface(Typeface typeface) {
        setting.italicTypeface = typeface;
    }

    public void setBoldTypeface(Typeface typeface) {
        setting.boldTypeface = typeface;
        drawer.setBoldTypeface(typeface);
    }

    public boolean isWordWrap() {
        return getDocumentProvider().isWordWrap();
    }

    public void setWordWrap(boolean enable) {
        setting.isWordWrap = enable;
        getDocumentProvider().setWordWrap(enable);
        if (enable) {
            drawer.setXExtent(0);
            scrollTo(0, 0);
        }
        fieldController.updateCaretRow();
        if (!makeCharVisible(caretPosition)) {
            invalidate();
        }
    }

    public void analyzeIfWordWrap(DocumentProvider dp) {
        if (dp.isWordWrap()) {
            dp.analyzeWordWrap();
        }
    }


    /**
     * Sets the length of a tab character, scrolls the view to display the
     * caret if needed, and invalidates the entire view
     *
     * @param spaceCount The number of spaces a tab represents
     */
    public void setTabSpaces(int spaceCount) {
        if (spaceCount < 0) {
            return;
        }
        setting.tabLength = spaceCount;
        analyzeIfWordWrap(getDocumentProvider());
        fieldController.updateCaretRow();
        if (!makeCharVisible(caretPosition)) {
            invalidate();
        }
    }

    /**
     * Enable/disable auto-indent
     */
    public void setAutoIndent(boolean enable) {
        setting.isAutoIndent = enable;
    }

    public void setAutoComplete(boolean enable) {
        setting.isAutoComplete = enable;
    }


    /**
     * Enable/disable long-pressing capitalization.
     * When enabled, a long-press on a hardware key capitalizes that letter.
     * When disabled, a long-press on a hardware key bring up the
     * CharacterPickerDialog, if there are alternative characters to choose from.
     */
    public void setLongPressCaps(boolean enable) {
        setting.isLongPressCaps = enable;
    }

    /**
     * Enable/disable highlighting of the current row. The current row is also
     * invalidated
     */
    public void setHighlightCurrentRow(boolean enable) {
        setting.isHighlightCurrentRow = enable;
        invalidateCaretRow();
    }

    /**
     * Enable/disable display of visible representations of non-printing
     * characters like spaces, tabs and end of lines
     * Invalidates the view if the enable state changes
     */
    public void setNonPrintingCharVisibility(boolean enable) {
        if (enable ^ setting.isShowNonPrinting) {
            setting.isShowNonPrinting = enable;
            analyzeIfWordWrap(getDocumentProvider());
            fieldController.updateCaretRow();
            if (!makeCharVisible(caretPosition)) {
                invalidate();
            }
        }
    }

    //---------------------------------------------------------------------
    //------------------------- Event handlers ----------------------------
    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        //Intercept multiple key presses of printing characters to implement
        //long-press caps, because the IME may consume them and not pass the
        //event to onKeyDown() for long-press caps logic to work.
        //TODO Technically, long-press caps should be implemented in the IME,
        //but is put here for end-user's convenience. Unfortunately this may
        //cause some IMEs to break. Remove this feature in future.
        if (setting.isLongPressCaps
                && event.getRepeatCount() == 1
                && event.getAction() == KeyEvent.ACTION_DOWN) {

            char c = KeysInterpreter.keyEventToPrintableChar(event);
            if (Character.isLowerCase(c)
                    && c == Character.toLowerCase(getDocumentProvider().charAt(caretPosition - 1))) {
                fieldController.onPrintableChar(Language.BACKSPACE);
                fieldController.onPrintableChar(Character.toUpperCase(c));
                return true;
            }
        }

        return super.onKeyPreIme(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        Log.e("field", "onKeyDown: "+keyCode );
//        删除键
        //check if direction or symbol key
        if (KeysInterpreter.isNavigationKey(event)) {
            handleNavigationKey(keyCode, event);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_SYM || keyCode == KeyCharacterMap.PICKER_DIALOG_INPUT) {
            showCharacterPicker(PICKER_SETS.get(KeyCharacterMap.PICKER_DIALOG_INPUT), false);
            return true;
        }

        //check if character is printable
        char c = KeysInterpreter.keyEventToPrintableChar(event);
        if (c == Language.NULL_CHAR) {
            return super.onKeyDown(keyCode, event);
        }

        int repeatCount = event.getRepeatCount();
        //handle multiple (held) key presses
        if (repeatCount == 1) {
            if (setting.isLongPressCaps) {
                handleLongPressCaps(c);
            } else {
                handleLongPressDialogDisplay(c);
            }
        } else if (repeatCount == 0
                || setting.isLongPressCaps && !Character.isLowerCase(c)
                || !setting.isLongPressCaps && PICKER_SETS.get(c) == null) {
            fieldController.onPrintableChar(c);
        }

        return true;
    }

    private void handleNavigationKey(int keyCode, KeyEvent event) {
        if (event.isShiftPressed() && !isSelectText()) {
            invalidateCaretRow();
            fieldController.setSelectText(true);
        } else if (!event.isShiftPressed() && isSelectText()) {
            invalidateSelectionRows();
            fieldController.setSelectText(false);
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                fieldController.moveCaretRight(false);
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                fieldController.moveCaretLeft(false);
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                fieldController.moveCaretDown();
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                fieldController.moveCaretUp();
                break;
            default:
                break;
        }
    }

    private void handleLongPressCaps(char c) {
        if (Character.isLowerCase(c)
                && c == getDocumentProvider().charAt(caretPosition - 1)) {
            fieldController.onPrintableChar(Language.BACKSPACE);
            fieldController.onPrintableChar(Character.toUpperCase(c));
        } else {
            fieldController.onPrintableChar(c);
        }
    }

    //Precondition: If c is alphabetical, the character before the caret is
    //also c, which can be lower- or upper-case
    private void handleLongPressDialogDisplay(char c) {
        //workaround to get the appropriate caps mode to use
        boolean isCaps = Character.isUpperCase(getDocumentProvider().charAt(caretPosition - 1));
        char base = (isCaps) ? Character.toUpperCase(c) : c;

        String candidates = PICKER_SETS.get(base);
        if (candidates != null) {
            fieldController.stopTextComposing();
            showCharacterPicker(candidates, true);
        } else {
            fieldController.onPrintableChar(c);
        }
    }

    /**
     * @param candidates A string of characters to for the user to choose from
     * @param replace    If true, the character before the caret will be replaced
     *                   with the user-selected char. If false, the user-selected char will
     *                   be inserted at the caret position.
     */
    private void showCharacterPicker(String candidates, boolean replace) {
        final boolean shouldReplace = replace;
        final SpannableStringBuilder dummyString = new SpannableStringBuilder();
        Selection.setSelection(dummyString, 0);

        CharacterPickerDialog dialog = new CharacterPickerDialog(getContext(),
                this, dummyString, candidates, true);

        dialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (dummyString.length() > 0) {
                    if (shouldReplace) {
                        fieldController.onPrintableChar(Language.BACKSPACE);
                    }
                    fieldController.onPrintableChar(dummyString.charAt(0));
                }
            }
        });
        dialog.show();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return _navMethod.onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        int deltaX = Math.round(event.getX());
        int deltaY = Math.round(event.getY());
        while (deltaX > 0) {
            fieldController.moveCaretRight(false);
            --deltaX;
        }
        while (deltaX < 0) {
            fieldController.moveCaretLeft(false);
            ++deltaX;
        }
        while (deltaY > 0) {
            fieldController.moveCaretDown();
            --deltaY;
        }
        while (deltaY < 0) {
            fieldController.moveCaretUp();
            ++deltaY;
        }
        return true;
    }

    //监控鼠标光标在view上的变化
    @Override
    public boolean onHoverEvent(MotionEvent event) {
        return super.onHoverEvent(event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        //The input source is a pointing device associated with a display.
        //输入源为可显示的指针设备，如：mouse pointing device(鼠标指针),stylus pointing device(尖笔设备)
        if (0 != (event.getSource() & InputDevice.SOURCE_CLASS_POINTER)) {
            switch (event.getAction()) {
                // process the scroll wheel movement...处理滚轮事件
                case MotionEvent.ACTION_SCROLL:
                    //获得垂直坐标上的滚动方向,也就是滚轮向下滚
                    /*if( event.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0.0f){
                        //Log.i("fortest::onGenericMotionEvent", "down" );
                    }
                    //获得垂直坐标上的滚动方向,也就是滚轮向上滚
                    else{
                        //Log.i("fortest::onGenericMotionEvent", "up" );
                    }*/
                    scrollView(0, -event.getAxisValue(MotionEvent.AXIS_VSCROLL) * drawer.getRowHeight());
                    return true;
            }
        }
        return super.onGenericMotionEvent(event);
    }

    private void scrollView(float distanceX, float distanceY) {
        int newX = (int) distanceX + getScrollX();
        int newY = (int) distanceY + getScrollY();

        // If scrollX and scrollY are somehow more than the recommended
        // max scroll values, use them as the new maximum
        // Also take into account the size of the caret,
        // which may extend beyond the text boundaries
        int maxWidth = Math.max(getMaxScrollX(),
                getScrollX());
        if (newX > maxWidth) {
            newX = maxWidth;
        } else if (newX < 0) {
            newX = 0;
        }

        int maxHeight = Math.max(getMaxScrollY(),
                getScrollY());
        if (newY > maxHeight) {
            newY = maxHeight;
        } else if (newY < 0) {
            newY = 0;
        }
        //_textField.scrollTo(newX, newY);
        scrollTo(newX, newY);

    }

    @Override
    public boolean onCheckIsTextEditor() {
        return isEditable();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isFocused()) {
            if (!_scrollbar.handleEvent(event))
                _navMethod.onTouchEvent(event);
        } else {
//                return true;
            if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP &&
                    isPointInView(event.getX(), event.getY())) {
                // somehow, the framework does not automatically change the focus
                // to this view when it is touched
                if (isEditable())
                    requestFocus();
            }
        }
//        return super.onTouchEvent(event);
        return true;
    }

    private boolean isPointInView(float x, float y) {
        return (x >= 0 && x < getWidth() &&
                y >= 0 && y < getHeight());
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        invalidateCaretRow();
    }


    /**
     * Not public to allow access by {@link TouchNavigationMethod}
     */
    public void showIME(boolean show) {
        if (show && setting.isShowIME) {
            if (!isEditable())
                return;
            requestFocus();
            _inputMethodManager.showSoftInput(this, 0);
        } else {
            _inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
        }
    }

    public void showIME() {
        showIME(true);
    }

    public void hideIME() {
        showIME(false);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        if (isEditable()) {
            outAttrs.inputType = InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS //输入法无提示
                    | InputType.TYPE_TEXT_FLAG_MULTI_LINE;
            outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_ENTER_ACTION
                    | EditorInfo.IME_ACTION_DONE
                    | EditorInfo.IME_FLAG_NO_EXTRACT_UI;
            outAttrs.initialSelStart = getSelectionStart();
            outAttrs.initialSelEnd = getSelectionEnd();
//            outAttrs.initialCapsMode = _inputConnection.getCursorCapsMode(outAttrs .getInputType());
            _inputConnection.resetComposingState();
            return _inputConnection;
        }
        return null;
    }

}
