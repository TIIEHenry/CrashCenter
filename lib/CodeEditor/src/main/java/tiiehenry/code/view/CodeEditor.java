package tiiehenry.code.view;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.widget.Magnifier;


import java.io.File;

import tiiehenry.code.EditorException;
import tiiehenry.code.doc.Document;
import tiiehenry.code.doc.DocumentProvider;
import tiiehenry.code.format.FormatCallback;
import tiiehenry.code.format.FormatListener;
import tiiehenry.code.listener.OnKeyShortcutListener;

public class CodeEditor extends CodeTextField {

    private Context mContext;

    private OnKeyShortcutListener _onKeyShortcutListener = new OnKeyShortcutListener() {
        @Override
        public boolean onKeyShortcut(int keyCode, KeyEvent event) {
            final int filteredMetaState = event.getMetaState() & ~KeyEvent.META_CTRL_MASK;
            if (KeyEvent.metaStateHasNoModifiers(filteredMetaState)) {
                if (event.isShiftPressed()) {//todo
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_Z:
                            redo();
                            return true;
                        case KeyEvent.KEYCODE_DPAD_UP://上移行
                            boolean isSelected = isSelectText();
                            int oriSelStart = getSelectionStart();
                            int lineStart = setting.doc.findLineNumber(oriSelStart);
                            int selPriorStart = setting.doc.getLineOffset(lineStart - 1);
                            int selCurrentStart = setting.doc.getLineOffset(lineStart);
                            int lineSelCount = selCurrentStart - selPriorStart;
                            String s = cutLines();
                            insert(selPriorStart, s);
                            if (isSelected) {
                                setSelection(selPriorStart - lineSelCount, selCurrentStart - lineSelCount);
                            } else {
                                setSelection(selPriorStart - lineSelCount);
                            }
                            return true;
                        case KeyEvent.KEYCODE_DPAD_DOWN://下移行
                            boolean isSelected2 = isSelectText();
                            int oriSelStart2 = getSelectionStart();
                            int lineStart2 = setting.doc.findLineNumber(oriSelStart2);
                            int selPriorStart2 = setting.doc.getLineOffset(lineStart2 + 1);
                            int selCurrentStart2 = setting.doc.getLineOffset(lineStart2);
                            int lineSelCount2 = selCurrentStart2 - selPriorStart2;
                            String s2 = cutLines();
                            insert(selPriorStart2, s2);
                            if (isSelected2) {
                                setSelection(selPriorStart2 - lineSelCount2, selCurrentStart2 - lineSelCount2);
                            } else {
                                setSelection(selPriorStart2 - lineSelCount2);
                            }
                            return true;
                    }
                } else {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_A:
                            selectAll(false);
                            return true;
                        case KeyEvent.KEYCODE_X:
                            if (isSelectText())
                                cut();
                            else
                                cutLine();
                            return true;
                        case KeyEvent.KEYCODE_C:
                            copy();
                            return true;
                        case KeyEvent.KEYCODE_D:
                            boolean isSelected = isSelectText();
                            int newLineToAdd = getSelectionStart() - setting.doc.getLineOffset(getCurrentLine() - 1);
                            if (!isSelected) {
                                selectLine();
                            }
                            String s = getSelectedText();
                            int lastSelectionEnd = getSelectionEnd();
                            insert(lastSelectionEnd, s);
                            if (isSelected) {
                                setSelectionRange(lastSelectionEnd, s.length());
                            } else {
                                setSelection(setting.doc.getLineOffset(getCurrentLine() - 2) + newLineToAdd);
                            }
                            return true;
                        case KeyEvent.KEYCODE_V:
                            paste();
                            return true;
                        case KeyEvent.KEYCODE_Z:
                            undo();
                            return true;
                    }
                }
            }
            return false;
        }
    };

    private final File fontDir = new File("/sdcard/Android/fonts/");

    private int _index;

    public CodeEditor(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public CodeEditor(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }

    public CodeEditor(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        initFont(fontDir);
        setTextSizeSP(Default.textSizeSP);
        //自动换行
        setWordWrap(Default.isWordWrap);
        setShowMagnifier(Default.isShowMagnifier);
    }

    public void setShowMagnifier(boolean isShow) {
        if (isShow) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setMagnifier(new Magnifier.Builder(this).build());
            } else if (setting.isShowMagnifier && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                setMagnifier(new Magnifier(this));
            }
        } else {
            setMagnifier(null);
        }
    }

    public void initFont(File fontDir) {
        if (fontDir.isFile())
            return;
        File df = new File(fontDir, "default.ttf");
        if (df.isFile()) {
            initFontFile(df);
        } else {
            setTypeface(setting.defTypeface);
        }
        File bf = new File(fontDir, "bold.ttf");
        if (bf.isFile()) {
            setBoldTypeface(Typeface.createFromFile(bf));
        } else {
            setBoldTypeface(setting.boldTypeface);
        }
        File tf = new File(fontDir, "italic.ttf");
        if (tf.isFile()) {
            setItalicTypeface(Typeface.createFromFile(tf));
        } else {
            setItalicTypeface(setting.italicTypeface);
        }
    }

    public void initFontFile(File df) {
        Typeface type = Typeface.createFromFile(df);
        setTypeface(type);
        setItalicTypeface(Typeface.create(type, Typeface.ITALIC));
        setBoldTypeface(Typeface.create(type, Typeface.BOLD));
    }

    public void defaultFont() {
        setTypeface(setting.defTypeface);
        setBoldTypeface(setting.boldTypeface);
        setItalicTypeface(setting.italicTypeface);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (_index != 0 && right > 0) {
            moveCaret(_index);
            _index = 0;
        }
    }

    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        return _onKeyShortcutListener.onKeyShortcut(keyCode, event) || super.onKeyShortcut(keyCode, event);
    }

    public void setOnKeyShortcutListener(OnKeyShortcutListener l) {
        _onKeyShortcutListener = l;
    }

    public void insert(int idx, String text) {
        if (!isEditable())
            return;
        int selStart = getSelectionStart();
        int selEnd = getSelectionEnd();
        boolean isSelected = isSelected();
        selectText(false);
        moveCaret(idx);
        paste(text);
        int len = text.length();
        if (isSelected) {
            if (idx >= selEnd) {
                setSelection(selStart, selEnd);
            } else if (idx <= selStart) {
                setSelection(selStart + len, selEnd + len);
            } else {
                setSelection(selStart, selEnd + len);
            }
        } else {
            if (idx >= selStart)
                len = 0;
            moveCaret(selStart + len);
        }
    }

    public DocumentProvider getText() {
        return createDocumentProvider();
    }

    public String getString() {
        return getText().toString();
    }

    public void setText(CharSequence c) {
        //TextBuffer text=new TextBuffer();
        Document doc = new Document(this);
        doc.setWordWrap(setting.isWordWrap);
        doc.setText(c);
        setDocumentProvider(new DocumentProvider(doc));
//        setWordWrap();
        //doc.analyzeWordWrap();
    }


    public void setText(CharSequence c, boolean isRep) {
        if (isRep) {
            if (getLength() == 0) {
                fieldController.selectionDelete();
                replaceText(0, 0, c.toString());
            } else {
                replaceText(0, getLength() - 1, c.toString());
            }

        } else {
            setText(c);
        }
    }

    public String getSelectedText() {
        return getTextForSelectionRange(getSelectionStart(), getSelectionEnd());
    }

    public String getTextForSelectionRange(int selStart, int selEnd) {
        return getDocumentProvider().subSequence(selStart, selEnd - selStart).toString();
    }

    public void setSelection(int index) {
        if (index >= 0 && index <= setting.doc.docLength()) {
            selectText(false);
            if (!hasLayout()) {
                moveCaret(index);
            } else {
                _index = index;
            }
        }
    }

    public void selectRange(int selStart, int selEnd) {
        setSelectionRange(selStart, selEnd - selStart);
    }

    public int getRowCount() {
        return setting.doc.getRowCount();
    }

    //0
    public int getLineNumberForIndex(int index) {
        return getDocumentProvider().findLineNumber(index);
    }

    //0
    public int getLineCount() {
        return getDocumentProvider().findLineNumber(setting.doc.docLength() - 1);
    }

    /**
     * @return 行号，从1开始
     */
    public int getCurrentLine() {
        return getCaretRow() + 1;
    }

    public void gotoLine(int line) {
        if (line > getRowCount()) {
            line = getRowCount();
        }
        int i = getText().getLineOffset(line - 1);
        setSelection(i);
    }


    public void selectLine() {
        selectLine(getCurrentLine());
    }

    public void selectLine(int line) {
        selectLineRange(line, line);
    }

    public void selectLines() {
        int lineStart = setting.doc.findLineNumber(getSelectionStart());
        int lineEnd = setting.doc.findLineNumber(getSelectionEnd());
        selectLineRange(lineStart + 1, lineEnd + 1);
    }

    public void selectLineRange(int startLine, int endLine) {
        int selStart = getSelectionStartForLine(startLine);
        int selEnd = getSelectionEndForLine(endLine);
        selectRange(selStart, selEnd);
    }

    //有阴影的只有一行
    public String deleteLine() {
        return deleteLine(getCurrentLine());
    }

    public String deleteLine(int line) {
        selectLine(line);
        return deleteSelection();
    }

    public String deleteLines() {
        int lineStart = setting.doc.findLineNumber(getSelectionStart());
        int lineEnd = setting.doc.findLineNumber(getSelectionEnd());
        return deleteLineRange(lineStart + 1, lineEnd + 1);
    }

    public String deleteLineRange(int startLine, int endLine) {
        selectLineRange(startLine, endLine);
        return deleteSelection();
    }

    public String deleteSelection() {
        String s = getSelectedText();
        fieldController.selectionDelete();
        return s;
    }

    public String cutLine() {
        return cutLine(getCurrentLine());
    }

    public String cutLine(int line) {
        selectLine(line);
        return cutSelection();
    }

    public String cutLines() {
        int lineStart = setting.doc.findLineNumber(getSelectionStart());
        int lineEnd = setting.doc.findLineNumber(getSelectionEnd());
        return cutLineRange(lineStart + 1, lineEnd + 1);
    }

    public String cutLineRange(int startLine, int endLine) {
        selectLineRange(startLine, endLine);
        return cutSelection();
    }

    public String cutSelection() {
        String s = getSelectedText();
        cut();
        return s;
    }

    public void copyLine() {
        copyLine(getCurrentLine());
    }

    public void copyLine(int line) {
        int selStart = getSelectionStartForLine(line);
        int selEnd = getSelectionEndForLine(line);
        copy(getTextForSelectionRange(selStart, selEnd));
    }

    public void copyLines() {
        int lineStart = setting.doc.findLineNumber(getSelectionStart());
        int lineEnd = setting.doc.findLineNumber(getSelectionEnd());
        copyLineRange(lineStart + 1, lineEnd + 1);
    }

    public void copyLineRange(int startLine, int endLine) {
        int selStart = getSelectionStartForLine(startLine);
        int selEnd = getSelectionEndForLine(endLine);
        copy(getTextForSelectionRange(selStart, selEnd));
    }


    public void copy(String s) {
        ClipData clip = ClipData.newPlainText("CodeEditor", s);
        _clipboardManager.setPrimaryClip(clip);
    }

    public String copySelection() {
        String t = getSelectedText();
        copy(t);
        return t;
    }

    public void indentLines() {
        int selStart = getSelectionStart();
        int selEnd = getSelectionEnd();
        int lineStart = setting.doc.findLineNumber(selStart);
        int lineEnd = setting.doc.findLineNumber(selEnd);
        indentLineRange(lineStart + 1, lineEnd + 1);
        if (lineStart == lineEnd) {
            if (selStart != selEnd)
                setSelection(selStart + setting.autoIndentWidth, selEnd + setting.autoIndentWidth);
            else
                setSelection(selStart + setting.autoIndentWidth);
        }
    }

    public void indentLineRange(int startLine, int endLine) {
        int selStart = getSelectionStartForLine(startLine);
        int selEnd = getSelectionEndForLine(endLine);
        String s = indentLines(getTextForSelectionRange(selStart, selEnd));
//        Log.e("ce", "indentLineRange: " + selStart + ":" + selEnd);
        replaceText(selStart, selEnd - selStart, s);
    }

    private String trimStart(String str) {
        int start = 0;
        for (char s : str.toCharArray()) {
            if (String.valueOf(s).trim().length() == 0)
                start++;
            else
                break;
        }
        return str.substring(start);
    }

    private String indentLines(String str) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < setting.autoIndentWidth; i++)
            stringBuilder.append(getLanguage().indentChar);
        String indentString = stringBuilder.toString();
        String[] strArray = str.split("\n");
        StringBuilder newStr = new StringBuilder();
        for (int i = 0; i < strArray.length; i++) {
            String lineText = strArray[i];
            StringBuilder spaceCount = new StringBuilder();
            for (char s : lineText.toCharArray()) {
                if (String.valueOf(s).trim().length() == 0)
                    spaceCount.append(s);
                else
                    break;
            }
            newStr.append(
                    spaceCount.append(indentString)
                            .append(trimStart(lineText))
            );
            if (i != strArray.length - 1)
                newStr.append("\n");
        }
        if (str.endsWith("\n")) {
            newStr.append("\n");
        }
        return newStr.toString();
    }


    public void noteLine() {
        noteLine(getCurrentLine());
    }

    public void noteLine(int line) {
        int selStart = getSelectionStartForLine(line - 1);
        int selEnd = getSelectionEndForLine(line);
        String s = noteSingleLine(getTextForSelectionRange(selStart, selEnd));
        int oldSelectionStart = getSelectionStart();
        int oldSelectionEnd = getSelectionEnd();
        replaceText(selStart, selEnd - selStart, s);
        selectRange(oldSelectionStart + getLanguage().getLineNoteStringStart().length(), oldSelectionEnd + getLanguage().getLineNoteStringStart().length() + getLanguage().getLineNoteStringEnd().length());
    }

    private String noteSingleLine(String lineText) {
        StringBuilder spaceCount = new StringBuilder();
        for (char s : lineText.toCharArray()) {
            if (String.valueOf(s).trim().length() == 0)
                spaceCount.append(s);
            else
                break;
        }
        return spaceCount.append(getLanguage().getLineNoteStringStart())
                .append(lineText.trim())
                .append(getLanguage().getLineNoteStringStart()).toString();
    }


    public void noteLines() {
        int lineStart = setting.doc.findLineNumber(getSelectionStart());
        int lineEnd = setting.doc.findLineNumber(getSelectionEnd());
        noteLineRange(lineStart + 1, lineEnd + 1);
    }

    public void noteLineRange(int startLine, int endLine) {
        int selStart = getSelectionStartForLine(startLine);
        int selEnd = getSelectionEndForLine(endLine);
        String s = noteLines(getTextForSelectionRange(selStart, selEnd));
//        Log.e("ce", "noteLineRange: " + selStart + ":" + selEnd);
        replaceText(selStart, selEnd - selStart, s);
    }

    private String noteLines(String str) {
        String[] strArray = str.split("\n");
        boolean isUnNote = true;
        for (String line : strArray) {
            String s = line.trim();
            if (s.startsWith(getLanguage().getLineNoteStringStart()) && s.endsWith(getLanguage().getLineNoteStringEnd())) {

            } else {
                isUnNote = false;
            }
        }
        StringBuilder newStr = new StringBuilder();
        for (int i = 0; i < strArray.length; i++) {
            String lineText = strArray[i];
            StringBuilder spaceCount = new StringBuilder();
            for (char s : lineText.toCharArray()) {
                if (String.valueOf(s).trim().length() == 0)
                    spaceCount.append(s);
                else
                    break;
            }
            if (isUnNote) {
                String s = lineText.trim();
                s = s.substring(getLanguage().getLineNoteStringStart().length(), s.length() - getLanguage().getLineNoteStringEnd().length());
                newStr.append(
                        spaceCount.append(s));
            } else {
                newStr.append(
                        spaceCount.append(getLanguage().getLineNoteStringStart())
                                .append(lineText.trim())
                                .append(getLanguage().getLineNoteStringEnd())
                );
            }
            if (i != strArray.length - 1)
                newStr.append("\n");
        }
        if (str.endsWith("\n")) {
            newStr.append("\n");
        }
        return newStr.toString();
    }

    public void noteBlock() {
        noteBlock(getSelectionStart(), getSelectionEnd());
    }

    public void noteBlock(int selStart, int selEnd) {
        String str = getTextForSelectionRange(selStart, selEnd);
        String[] strArray = str.split("\n");
        StringBuilder newStr = new StringBuilder();
        for (int i = 0; i < strArray.length; i++) {
            String lineText = strArray[i];
            StringBuilder spaceCount = new StringBuilder();
            for (char s : lineText.toCharArray()) {
                if (String.valueOf(s).trim().length() == 0)
                    spaceCount.append(s);
                else
                    break;
            }
            if (i == 0) {
                spaceCount.append(getLanguage().getRegionNoteStringStart());
                if (strArray.length > 1) {
                    spaceCount.append(lineText.trim());
                    spaceCount.append("\n");
                }
            } else {
                if (i < strArray.length - 1) {
                    spaceCount.append(getLanguage().getRegionNoteStringMiddle())
                            .append(lineText.trim())
                            .append("\n");
                }
            }
            if (i == strArray.length - 1) {
                spaceCount.append(lineText.trim());
                spaceCount.append(getLanguage().getRegionNoteStringEnd());
            }
            newStr.append(spaceCount);
        }
        if (str.endsWith("\n")) {
            newStr.append("\n");
        }
        replaceText(selStart, selEnd - selStart, newStr.toString());
    }

    public void noteDocBlock() {
        noteDocBlock(getSelectionStart(), getSelectionEnd());
    }

    public void noteDocBlock(int selStart, int selEnd) {
        String str = getTextForSelectionRange(selStart, selEnd);
        String[] strArray = str.split("\n");
        StringBuilder newStr = new StringBuilder();
        for (int i = 0; i < strArray.length; i++) {
            String lineText = strArray[i];
            StringBuilder spaceCount = new StringBuilder();
            for (char s : lineText.toCharArray()) {
                if (String.valueOf(s).trim().length() == 0)
                    spaceCount.append(s);
                else
                    break;
            }
            if (i == 0) {
                spaceCount.append(getLanguage().getRegionDocNoteStringStart());
                if (strArray.length > 1) {
                    spaceCount.append(lineText.trim());
                    spaceCount.append("\n");
                }
            } else {
                if (i < strArray.length - 1) {
                    spaceCount.append(getLanguage().getRegionDocNoteStringMiddle())
                            .append(lineText.trim())
                            .append("\n");
                }
            }
            if (i == strArray.length - 1) {
                spaceCount.append(lineText.trim());
                spaceCount.append(getLanguage().getRegionDocNoteStringEnd());
            }
            newStr.append(spaceCount);
        }
        if (str.endsWith("\n")) {
            newStr.append("\n");
        }
        replaceText(selStart, selEnd - selStart, newStr.toString());
    }

    private int getSelectionStartForLine(int startLine) {
        return setting.doc.getLineOffset(startLine - 1);
    }

    private int getSelectionEndForLine(int endLine) {
        return endLine == getRowCount() ?
                setting.doc.docLength() - 1 :
                setting.doc.getLineOffset(endLine) - 1;
    }

    public String replaceIndentChar(String str) {
        String[] strArray = str.split("\n");
        StringBuilder newStr = new StringBuilder();
        for (String lineText : strArray) {
            boolean isStart = true;
            for (char s : lineText.toCharArray()) {
                if (isStart) {
                    if (s == ' ' || s == '\t') {
                        newStr.append(getLanguage().indentChar);
                    } else {
                        isStart = false;
                        newStr.append(s);
                    }
                } else {
                    newStr.append(s);
                }
            }
            newStr.append('\n');
        }
        return newStr.toString();
    }


    public void format() {
        if (isSelectText()) {
            formatSelection();
        } else formatAll();
    }

    public void formatAll() {
        String input = getDocumentProvider().toString();
        if (input.length() > 0)
            formatText(input, 0, getDocumentProvider().docLength() - 1, false);
    }

    public void formatSelection() {
        String input = getSelectedText();
        if (input.length() > 0)
            formatText(input, getSelectionStart(), getSelectionEnd(), true);
    }

    private void formatText(final String text, final int selStart, final int selEnd, final boolean selectText) {
        if (canFormat()) {
            formatListener.onPrepare();
            selectText(false);
            getLexTask().formatWithCallback(text, setting.autoIndentWidth, caretPosition, new FormatCallback() {
                @Override
                public void onDone(final String s, final int newPos) {
                    if (selectText) {
                        int line = setting.doc.findLineNumber(selStart) + 1;
                        int start = getSelectionStartForLine(line);
                        int end = getSelectionEndForLine(line);
                        String selText = getTextForSelectionRange(start, end);
                        StringBuilder indentStrBuilder = new StringBuilder();
                        for (char st : selText.toCharArray()) {
                            if (st == getLanguage().indentChar || st == ' ' || st == '\t') {
                                indentStrBuilder.append(st);
                            } else {
                                break;
                            }
                        }
                        String indentStr = indentStrBuilder.toString();
                        String out = s.replaceAll("\n", "\n" + indentStr);
                        if (text.startsWith(indentStr) && !out.startsWith(indentStr))
                            out = indentStrBuilder + out;
                        final String finalOut = out;
                        post(new Runnable() {
                            @Override
                            public void run() {
                                setFormattedText(finalOut, selStart, selEnd);
                                setSelectionRange(selStart, finalOut.length());
                                formatListener.onDone();
                            }
                        });
                    } else {
                        post(new Runnable() {
                            @Override
                            public void run() {
                                setFormattedText(s, selStart, selEnd);
                                moveCaret(newPos);
                                formatListener.onDone();
                            }
                        });
                    }
                }

                @Override
                public void onError(String s, final String errorMsg) {
                    EditorException.fail("format failed" + errorMsg);
                    post(new Runnable() {
                        @Override
                        public void run() {
                            formatListener.onError(errorMsg);
                        }
                    });
                }
            });
        }
    }

    public FormatListener formatListener = new FormatListener() {
        final ProgressDialog progressDialog = new ProgressDialog(getContext());

        @Override
        public void onPrepare() {
            progressDialog.setTitle("Formatting");
            progressDialog.setMessage("Please wait...");
//            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        public void onDone() {
            progressDialog.dismiss();
        }

        @Override
        public void onError(String errMsg) {
            progressDialog.setTitle("Format Error");
            progressDialog.setMessage(errMsg);
//            progressDialog.setCancelable(true);
            progressDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    onDone();
                }
            });
//            onDone();
        }
    };

    public void setFormattedText(String text) {
        if (!isEditable())
            return;
        stopScrolling();
        selectText(false);
        getDocumentProvider().beginBatchEdit();
        getDocumentProvider().deleteAt(0, getDocumentProvider().docLength() - 1, System.nanoTime());
        getDocumentProvider().insertBefore(text.toCharArray(), 0, System.nanoTime());
        getDocumentProvider().endBatchEdit();
        getDocumentProvider().clearSpans();
        respan();
        invalidate();
        setEdited(true);
    }

    public void setFormattedText(String text, int selStart, int selEnd) {
        if (!isEditable())
            return;
        stopScrolling();
        replaceText(selStart, selEnd - selStart, text);
        setEdited(true);
    }


    public void undo(int count) {
        for (int i = 1; i <= count; i++)
            undo();
    }

    public void undo() {
        if (!isEditable())
            return;
        DocumentProvider doc = createDocumentProvider();
        if (doc.canUndo()) {
            int newPosition = doc.undo();
            if (newPosition >= 0) {
                setEdited(true);
                respan();
                selectText(false);
                moveCaret(newPosition);
                invalidate();
            }
        }
    }

    public void redo(int count) {
        for (int i = 1; i <= count; i++)
            redo();
    }

    public void redo() {
        if (!isEditable())
            return;
        DocumentProvider doc = createDocumentProvider();
        if (doc.canRedo()) {
            int newPosition = doc.redo();
            if (newPosition >= 0) {
                setEdited(true);
                respan();
                selectText(false);
                moveCaret(newPosition);
                invalidate();
            }
        }
    }

    public void insert(String s) {
        paste(s);
    }


    public void setTextSizeSP(int spsize) {
        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
        int size = (int) TypedValue.applyDimension(2, spsize, dm);
        setTextSize(size);
    }


    public void selectLess() {
//        if (isInSelectionMode())

    }

    public boolean isInSelectionMode() {
        return isSelectText();
    }
}
