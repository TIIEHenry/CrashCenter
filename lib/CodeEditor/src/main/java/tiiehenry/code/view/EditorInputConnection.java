package tiiehenry.code.view;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputContentInfo;
import android.view.inputmethod.InputMethodManager;

import tiiehenry.code.language.Language;

public class EditorInputConnection extends BaseInputConnection {
    private final EditorField editorField;
    private final EditorController fieldController;
    private boolean _isComposing = false;
    private int _composingCharCount = 0;
    protected final InputMethodManager mIMM;

    public EditorInputConnection(EditorField editorField) {
        super(editorField, true);
        this.editorField = editorField;
        this.fieldController = editorField.fieldController;
        mIMM = (InputMethodManager) editorField.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    public void resetComposingState() {
        _composingCharCount = 0;
        _isComposing = false;
        editorField.getDocumentProvider().endBatchEdit();
    }


    boolean isShiftPressed = false;

    @Override
    public boolean sendKeyEvent(KeyEvent event) {
//                         　 成功返回true，当输入连接无效返回false。
//            Log.e("sendKeyEvent", "sendKeyEvent: " + event);

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_SHIFT_LEFT) {
                isShiftPressed = true;
                return true;
            }
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    return true;
                case KeyEvent.KEYCODE_DPAD_UP:
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    return true;
                case KeyEvent.KEYCODE_MOVE_HOME:
                    return true;
                case KeyEvent.KEYCODE_MOVE_END:
                    return true;
                default:
                    return super.sendKeyEvent(event);
            }
        }
        if (event.getAction() == KeyEvent.ACTION_UP) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_SHIFT_LEFT) {
                isShiftPressed = false;
                return true;
            }
            if (!isShiftPressed && editorField.isSelectText() && event.getKeyCode() != KeyEvent.KEYCODE_DEL) {
                editorField.selectText(false);
            } else if (isShiftPressed && !editorField.isSelectText()) {
                editorField.selectText(true);
            }
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    editorField.moveCaretLeft(true);
                    return true;
                case KeyEvent.KEYCODE_DPAD_UP:
                    editorField.moveCaretUp(true);
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    editorField.moveCaretRight(true);
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    editorField.moveCaretDown(true);
                    return true;
                case KeyEvent.KEYCODE_MOVE_HOME:
                    editorField.moveCaret(0);
                    return true;
                case KeyEvent.KEYCODE_MOVE_END:
                    editorField.moveCaret(editorField.getDocumentProvider().length());
                    return true;
                default:
                    return super.sendKeyEvent(event);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mIMM.dispatchKeyEventFromInputMethod(editorField, event);
        }
        return false;
    }

    @Override
    public boolean clearMetaKeyStates(int states) {
        return false;
    }

    /**
     * Only true when the InputConnection has not been used by the IME yet.
     * Can be programatically cleared by resetComposingState()
     */
    public boolean isComposingStarted() {
        return _isComposing;
    }

    @Override
    public boolean setComposingText(CharSequence text, int newCursorPosition) {
        _isComposing = true;
        if (!editorField.getDocumentProvider().isBatchEdit()) {
            editorField.getDocumentProvider().beginBatchEdit();
        }

        editorField.fieldController.replaceComposingText(
                editorField.getCaretPosition() - _composingCharCount,
                _composingCharCount,
                text.toString());
        _composingCharCount = text.length();

        //TODO reduce invalidate calls
        if (newCursorPosition > 1) {
            fieldController.moveCaret(editorField.caretPosition + newCursorPosition - 1);
        } else if (newCursorPosition <= 0) {
            fieldController.moveCaret(editorField.caretPosition - text.length() - newCursorPosition);
        }
        return true;
    }

    @Override
    public boolean setComposingRegion(int start, int end) {
//        new Throwable("setComposingRegion"+start+":"+end).printStackTrace();
        return false;
    }

    /**
     * 解决AutoComplete选择补全后不清楚联想的问题
     */
    public boolean commitTextDisableOnce=false;
    public boolean commitTextDisableOnceCommitted=false;

    /**
     * 向文本框提交文本并设置新的光标位置。之前设置的正编辑文字将自动删除。
     * @param text
     * @param newCursorPosition 相对位置
     * @return
     */
    @Override
    public boolean commitText(CharSequence text, int newCursorPosition) {
        if (commitTextDisableOnce){
            commitTextDisableOnceCommitted=true;
            commitTextDisableOnce=false;
            return true;
        }
//        new Throwable("commitText:"+text+":"+newCursorPosition).printStackTrace();
        fieldController.replaceComposingText(
                editorField.getCaretPosition() - _composingCharCount,
                _composingCharCount,
                text.toString());
        _composingCharCount = 0;
        editorField.getDocumentProvider().endBatchEdit();

        //TODO reduce invalidate calls
        if (newCursorPosition > 1) {
            fieldController.moveCaret(editorField.caretPosition + newCursorPosition - 1);
        } else if (newCursorPosition <= 0) {
            fieldController.moveCaret(editorField.caretPosition - text.length() - newCursorPosition);
        }
        _isComposing = false;
        return true;
    }

    @Override
    public boolean commitCompletion(CompletionInfo text) {
        return false;
    }

    @Override
    public boolean commitCorrection(CorrectionInfo correctionInfo) {
        return false;
    }

    /**
     * 删除当前光标前的leftLength个字符，并删除当前光标后的rightLength个字符，不包联想输入(composing)的文字。
     * @param leftLength
     * @param rightLength
     * @return
     */
    @Override
    public boolean deleteSurroundingText(int leftLength, int rightLength) {
        if (commitTextDisableOnceCommitted){
            commitTextDisableOnceCommitted=false;
            return true;
        }
        if (_composingCharCount != 0) {
            Log.i("editor",
                    "Warning: Implmentation of InputConnection.deleteSurroundingText" +
                            " will not skip composing text");
        }else {
        }
        fieldController.deleteAroundComposingText(leftLength, rightLength);
        return true;
    }

    @Override
    public boolean deleteSurroundingTextInCodePoints(int beforeLength, int afterLength) {
        return false;
    }

    @Override
    public boolean finishComposingText() {
        resetComposingState();
        return true;
    }


    @Override
    public int getCursorCapsMode(int reqModes) {
        int capsMode = 0;

        // Ignore InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS; not used in TextWarrior

        if ((reqModes & InputType.TYPE_TEXT_FLAG_CAP_WORDS)
                == InputType.TYPE_TEXT_FLAG_CAP_WORDS) {
            int prevChar = editorField.caretPosition - 1;
            if (prevChar < 0 || editorField.getSetting().language.isWhitespace(editorField.getDocumentProvider().charAt(prevChar))) {
                capsMode |= InputType.TYPE_TEXT_FLAG_CAP_WORDS;

                //set CAP_SENTENCES if client is interested in it
                if ((reqModes & InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
                        == InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) {
                    capsMode |= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
                }
            }
        }

        // Strangely, Android soft keyboard does not set TYPE_TEXT_FLAG_CAP_SENTENCES
        // in reqModes even if it is interested in doing auto-capitalization.
        // Android bug? Therefore, we assume TYPE_TEXT_FLAG_CAP_SENTENCES
        // is always set to be on the safe side.
        else {
            Language lang = editorField.getLanguage();

            int prevChar = editorField.caretPosition - 1;
            int whitespaceCount = 0;
            boolean capsOn = true;

            // Turn on caps mode only for the first char of a sentence.
            // A fresh line is also considered to start a new sentence.
            // The position immediately after a period is considered lower-case.
            // Examples: "abc.com" but "abc. Com"
            while (prevChar >= 0) {
                char c = editorField.getDocumentProvider().charAt(prevChar);
                if (c == Language.NEWLINE) {
                    break;
                }

                if (!lang.isWhitespace(c)) {
                    if (whitespaceCount == 0 || !lang.isSentenceTerminator(c)) {
                        capsOn = false;
                    }
                    break;
                }

                ++whitespaceCount;
                --prevChar;
            }

            if (capsOn) {
                capsMode |= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
            }
        }

        return capsMode;
    }

    @Override
    public ExtractedText getExtractedText(ExtractedTextRequest request, int flags) {
        ExtractedText text = new ExtractedText();
        text.selectionStart = editorField.getSelectionStart();
        text.selectionEnd = editorField.getSelectionEnd();
        text.flags = 1;
        text.text = editorField.getDocumentProvider().toString();
        text.startOffset = 2;
        return text;
    }

    @Override
    public CharSequence getTextAfterCursor(int maxLen, int flags) {
        return fieldController.getTextAfterCursor(maxLen); //ignore flags
    }

    @Override
    public CharSequence getTextBeforeCursor(int maxLen, int flags) {
        return fieldController.getTextBeforeCursor(maxLen); //ignore flags
    }

    @Override
    public boolean setSelection(int start, int end) {
        if (start == end) {
            if (start == 0) {
                //适配搜狗输入法
                if (editorField.getCaretPosition() > 0) {
                    fieldController.moveCaret(0);
                }
            } else {
                fieldController.moveCaret(start);
            }
        } else {
            fieldController.setSelectionRange(start, end - start, false, true);
        }
        return true;
    }


    @Override
    public boolean performEditorAction(int editorAction) {
        return false;
    }

    @Override
    public boolean performContextMenuAction(int id) {
        switch (id) {
            case android.R.id.copy:
                editorField.copy();
                return true;
            case android.R.id.paste:
                editorField.paste();
                return true;
            case android.R.id.cut:
                editorField.cut();
                return true;
            case android.R.id.stopSelectingText:
                editorField.setSelected(false);
            case android.R.id.startSelectingText:
                editorField.setSelected(true);
            case android.R.id.selectAll:
                editorField.selectAll(false);
                return false;
            case android.R.id.home:
                editorField.moveCaret(0);
                return true;
        }
        return false;
    }

    @Override
    public boolean beginBatchEdit() {
        return super.beginBatchEdit();
    }

    @Override
    public boolean endBatchEdit() {
        return super.endBatchEdit();
    }

    @Override
    public Editable getEditable() {
        return Editable.Factory.getInstance().newEditable(editorField.getDocumentProvider().toString());
    }

    @Override
    public boolean reportFullscreenMode(boolean enabled) {
        return false;
    }

    @Override
    public boolean performPrivateCommand(String action, Bundle data) {
        return false;
    }

    @Override
    public boolean requestCursorUpdates(int cursorUpdateMode) {
        return super.requestCursorUpdates(cursorUpdateMode);
    }

    @Override
    public Handler getHandler() {
        return super.getHandler();
    }

    @Override
    public void closeConnection() {
        super.closeConnection();
    }

    @Override
    public boolean commitContent(InputContentInfo inputContentInfo, int flags, Bundle opts) {
        return super.commitContent(inputContentInfo, flags, opts);
    }

    @Override
    public CharSequence getSelectedText(int flags) {
        return editorField.getDocumentProvider().subSequence(editorField.getSelectionStart(), editorField.getSelectionEnd() - editorField.getSelectionStart()).toString();
    }
}// end inner class}