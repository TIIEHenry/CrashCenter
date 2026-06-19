package tiiehenry.code.view;

import android.content.ClipData;
import android.content.ClipboardManager;

import java.util.List;

import tiiehenry.code.EditorException;
import tiiehenry.code.Lexer;
import tiiehenry.code.language.Language;
import tiiehenry.code.language.text.TextLanguage;
import tiiehenry.code.praser.Span;

public class EditorController implements Lexer.LexCallback {
    private final EditorField editorField;

    final Lexer lexer = new Lexer(this, TextLanguage.defaultLexTask);

    public EditorController(EditorField editorField) {
        this.editorField = editorField;

    }

    boolean _isInSelectionMode = false;
    private boolean _isInSelectionMode2;

    /**
     * Analyze the text for programming language keywords and redraws the
     * text view when done. The global programming language used is set with
     * the static method Lexer.setLanguage(Language)
     * <p>
     * Does nothing if the Lexer language is not a programming language
     */
    public void determineSpans() {
//            setting.isShowBlockRegionLines = false;
        lexer.tokenize(editorField.getDocumentProvider());
    }

    public void cancelSpanning() {
//            setting.isShowBlockRegionLines = true;
        lexer.cancelTokenize();
    }

    @Override
    //This is usually called from a non-UI thread
    public void lexDone(final List<Span> results) {
        editorField.post(new Runnable() {
            @Override
            public void run() {
//                    setting.isShowBlockRegionLines = true;
                editorField.getDocumentProvider().setSpans(results);
                editorField.drawer.resetBlockPair(editorField);
                editorField.invalidate();
            }
        });
    }

    //- TextFieldController -----------------------------------------------
    //---------------------------- Key presses ----------------------------

    //TODO minimise invalidate calls from moveCaret(), insertion/deletion and word wrap
    public void onPrintableChar(char c) {
//            setting.isShowBlockRegionLines = false;
        // delete currently selected text, if any

        boolean selectionDeleted = false;
        if (_isInSelectionMode) {
            selectionDelete();
            selectionDeleted = true;
        }

        int originalRow = editorField.getCaretRow();
        int originalOffset = editorField.getDocumentProvider().getRowOffset(originalRow);

        switch (c) {
            case Language.BACKSPACE:
                if (selectionDeleted) {
                    break;
                }

                if (editorField.getCaretPosition() > 0) {
                    editorField.getDocumentProvider().deleteAt(editorField.getCaretPosition() - 1, System.nanoTime());
                    if (editorField.getDocumentProvider().charAt(editorField.getCaretPosition() - 2) == 0xd83d || editorField.getDocumentProvider().charAt(editorField.getCaretPosition() - 2) == 0xd83c) {
                        editorField.getDocumentProvider().deleteAt(editorField.getCaretPosition() - 2, System.nanoTime());
                        editorField.moveCaretLeft(true);
                    }

                    editorField._textLis.onDel(c + "", editorField.getCaretPosition(), 1);
                    editorField.moveCaretLeft(true);

                    if (editorField.getCaretRow() < originalRow) {
                        // either a newline was deleted or the caret was on the
                        // first word and it became short enough to fit the prev
                        // row
                        editorField.invalidateFromRow(editorField.getCaretRow());
                    } else if (editorField.getDocumentProvider().isWordWrap()) {
                        if (originalOffset != editorField.getDocumentProvider().getRowOffset(originalRow)) {
                            //invalidate previous row too if its wrapping changed
                            --originalRow;
                        }
                        //TODO invalidate damaged rows only

                        editorField.invalidateFromRow(originalRow);
                    }
                }
                break;

            case Language.NEWLINE:
                if (editorField.getSetting().isAutoIndent) {
                    char[] indent = createAutoIndent();
                    editorField.getDocumentProvider().insertBefore(indent, editorField.getCaretPosition(), System.nanoTime());
                    editorField.moveCaret(editorField.getCaretPosition() + indent.length);
                } else {
                    editorField.getDocumentProvider().insertBefore(c, editorField.getCaretPosition(), System.nanoTime());
                    editorField.moveCaretRight(true);
                }

                if (editorField.getDocumentProvider().isWordWrap() && originalOffset != editorField.getDocumentProvider().getRowOffset(originalRow)) {
                    //invalidate previous row too if its wrapping changed
                    --originalRow;
                }

                editorField._textLis.onNewLine(c + "", editorField.getCaretPosition(), 1);
                editorField.invalidateFromRow(originalRow);
                break;

            default:
                editorField.getDocumentProvider().insertBefore(c, editorField.getCaretPosition(), System.nanoTime());
                editorField.moveCaretRight(true);
                editorField._textLis.onAdd(c + "", editorField.getCaretPosition(), 1);

                if (editorField.getDocumentProvider().isWordWrap()) {
                    if (originalOffset != editorField.getDocumentProvider().getRowOffset(originalRow)) {
                        //invalidate previous row too if its wrapping changed
                        --originalRow;
                    }
                    //TODO invalidate damaged rows only
                    editorField.invalidateFromRow(originalRow);
                }
                break;
        }

        editorField.setEdited(true);
        determineSpans();
    }

    /**
     * Return a char[] with a newline as the 0th element followed by the
     * leading spaces and tabs of the line that the caret is on
     */
    private char[] createAutoIndent() {
        int lineNum = editorField.getDocumentProvider().findLineNumber(editorField.getCaretPosition());
        int startOfLine = editorField.getDocumentProvider().getLineOffset(lineNum);
        int whitespaceCount = 0;
        editorField.getDocumentProvider().seekChar(startOfLine);
        while (editorField.getDocumentProvider().hasNext()) {
            char c = editorField.getDocumentProvider().next();
            if ((c != ' ' && c != Language.TAB) || startOfLine + whitespaceCount >= editorField.getCaretPosition()) {
                break;
            }
            ++whitespaceCount;
        }

        whitespaceCount += editorField.getSetting().autoIndentWidth * editorField.getLexTask().createAutoIndent(editorField.getDocumentProvider().subSequence(startOfLine, editorField.getCaretPosition() - startOfLine));
        if (whitespaceCount < 0) {
            return new char[]{Language.NEWLINE};
        }

        char[] indent = new char[1 + whitespaceCount];
        indent[0] = Language.NEWLINE;

        editorField.getDocumentProvider().seekChar(startOfLine);
        for (int i = 0; i < whitespaceCount; ++i) {
            indent[1 + i] = editorField.getLanguage().indentChar;
        }
        return indent;
    }

    public void moveCaretDown() {
        editorField.moveCaretDown(false);
    }

    public void moveCaretDown(boolean isTyping) {
        if (!editorField.caretOnLastRowOfFile()) {
            int currCaret = editorField.getCaretPosition();
            int currRow = editorField.getCaretRow();
            int newRow = currRow + 1;
            int currColumn = editorField.getColumn(currCaret);
            int currRowLength = editorField.getDocumentProvider().getRowSize(currRow);
            int newRowLength = editorField.getDocumentProvider().getRowSize(newRow);

            if (currColumn < newRowLength) {
                // Position at the same column as old row.
                editorField.setCaretPosition(editorField.getCaretPosition() + currRowLength);
            } else {
                // Column does not exist in the new row (new row is too short).
                // Position at end of new row instead.
                editorField.setCaretPosition(editorField.getCaretPosition() +
                        currRowLength - currColumn + newRowLength - 1);
            }
            editorField.setCaretRow(editorField.getCaretRow() + 1);

            updateSelectionRange(currCaret, editorField.getCaretPosition());
            if (!editorField.makeCharVisible(editorField.getCaretPosition())) {
                editorField.invalidateRows(currRow, newRow + 1);
            }
            if (editorField._rowLis != null)
                editorField._rowLis.onRowChanged(newRow);
            if (!isTyping)
                stopTextComposing();
        }
    }

    public void moveCaretUp() {
        editorField.moveCaretUp(false);
    }

    public void moveCaretUp(boolean isTyping) {
        if (!editorField.caretOnFirstRowOfFile()) {
            int currCaret = editorField.getCaretPosition();
            int currRow = editorField.getCaretRow();
            int newRow = currRow - 1;
            int currColumn = editorField.getColumn(currCaret);
            int newRowLength = editorField.getDocumentProvider().getRowSize(newRow);

            if (currColumn < newRowLength) {
                // Position at the same column as old row.
                editorField.setCaretPosition(editorField.getCaretPosition() - newRowLength);
            } else {
                // Column does not exist in the new row (new row is too short).
                // Position at end of new row instead.
                editorField.setCaretPosition(editorField.getCaretPosition() - currColumn - 1);
            }
            editorField.setCaretRow(editorField.getCaretRow() - 1);

            updateSelectionRange(currCaret, editorField.getCaretPosition());
            if (!editorField.makeCharVisible(editorField.getCaretPosition())) {
                editorField.invalidateRows(newRow, currRow + 1);
            }
            if (editorField._rowLis != null)
                editorField._rowLis.onRowChanged(newRow);
            if (!isTyping)
                stopTextComposing();
        }
    }

    /**
     * @param isTyping Whether caret is moved to a consecutive position as
     *                 a result of entering text
     */
    public void moveCaretRight(boolean isTyping) {
        if (!editorField.caretOnEOF()) {
            int originalRow = editorField.getCaretRow();
            editorField.setCaretPosition(editorField.getCaretPosition() + 1);
            updateCaretRow();
            updateSelectionRange(editorField.getCaretPosition() - 1, editorField.getCaretPosition());
            if (!editorField.makeCharVisible(editorField.getCaretPosition())) {
                editorField.invalidateRows(originalRow, editorField.getCaretRow() + 1);
            }

            if (!isTyping) {
                stopTextComposing();
            }
        }
    }

    /**
     * @param isTyping Whether caret is moved to a consecutive position as
     *                 a result of deleting text
     */
    public void moveCaretLeft(boolean isTyping) {
        if (editorField.getCaretPosition() > 0) {
            int originalRow = editorField.getCaretRow();
            editorField.setCaretPosition(editorField.getCaretPosition() - 1);
            updateCaretRow();
            updateSelectionRange(editorField.getCaretPosition() + 1, editorField.getCaretPosition());
            if (!editorField.makeCharVisible(editorField.getCaretPosition())) {
                editorField.invalidateRows(editorField.getCaretRow(), originalRow + 1);
            }

            if (!isTyping) {
                stopTextComposing();
            }
        }
    }

    public void moveCaret(int i) {
        editorField.moveCaret(i, false);
    }

    public void moveCaret(int i, boolean isYoyoTouch) {
        if (i < 0 || i >= editorField.getDocumentProvider().docLength()) {
            EditorException.fail("Invalid caret position");
            return;
        }
        updateSelectionRange(editorField.getCaretPosition(), i);

        editorField.setCaretPosition(i);
        updateAfterCaretJump(isYoyoTouch);
    }

    private void updateAfterCaretJump() {
        updateAfterCaretJump(false);
    }

    private void updateAfterCaretJump(boolean isYoyoTouch) {
        int oldRow = editorField.getCaretRow();
        updateCaretRow();
        if (!editorField.makeCharVisible(editorField.getCaretPosition())) {
            editorField.invalidateRows(oldRow, oldRow + 1); //old caret row
            editorField.invalidateCaretRow(); //new caret row
        }
        if (!isYoyoTouch)
            stopTextComposing();
    }


    /**
     * This helper method should only be used by internal methods after setting
     * caretPosition, in order to to recalculate the new row the caret is on.
     */
    void updateCaretRow() {
        int newRow = editorField.getDocumentProvider().findRowNumber(editorField.getCaretPosition());
        if (editorField.getCaretRow() != newRow) {
            editorField.setCaretRow(newRow);
            if (editorField._rowLis != null)
                editorField._rowLis.onRowChanged(newRow);
        }
    }

    /**
     * 联想状态会导致删除已输入的文本
     */
    public void stopTextComposing() {
        // This is an overkill way to inform the InputMethod that the caret
        // might have changed position and it should re-evaluate the
        // caps mode to use.
        editorField._inputMethodManager.restartInput(editorField);

        if (editorField._inputConnection.isComposingStarted()) {
            editorField._inputConnection.resetComposingState();
        }
    }

    //- TextFieldController -----------------------------------------------
    //-------------------------- Selection mode ---------------------------
    public final boolean isSelectText() {
        return _isInSelectionMode;
    }

    /**
     * Enter or exit select mode.
     * Does not invalidate view.
     *
     * @param mode If true, enter select mode; else exit select mode
     */
    public void setSelectText(boolean mode) {
        if (mode != _isInSelectionMode) {
            if (mode) {
                editorField.set_selectionAnchor(editorField.getCaretPosition());
                editorField.set_selectionEdge(editorField.getCaretPosition());
            } else {
                    /*try {
                        throw new Exception("err");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Log.e(TAG, "setSelectText: " + mode);*/
                editorField.set_selectionAnchor(-1);
                editorField.set_selectionEdge(-1);
            }
            _isInSelectionMode = mode;
            _isInSelectionMode2 = mode;
            editorField._selModeLis.onSelectionChanged(mode, editorField.getSelectionStart(), editorField.getSelectionEnd());
        }
    }

    public final boolean isSelectText2() {
        return _isInSelectionMode2;
    }

    public boolean inSelectionRange(int charOffset) {
        if (editorField._selectionAnchor < 0) {
            return false;
        }

        return (editorField._selectionAnchor <= charOffset &&
                charOffset < editorField._selectionEdge);
    }

    /**
     * Selects numChars count of characters starting from beginPosition.
     * Invalidates necessary areas.
     *
     * @param beginPosition
     * @param numChars
     * @param scrollToStart If true, the start of the selection will be scrolled
     *                      into view. Otherwise, the end of the selection will be scrolled.
     */
    public void setSelectionRange(int beginPosition, int numChars, boolean scrollToStart, boolean mode) {
        setSelectionRange(beginPosition, numChars, scrollToStart, mode, false);
    }

    public void setSelectionRange(int beginPosition, int numChars, boolean scrollToStart, boolean mode, boolean stopInput) {
        EditorException.assertVerbose(
                (beginPosition >= 0) && numChars <= (editorField.getDocumentProvider().docLength() - 1) && numChars >= 0,
                "Invalid range to select");

        if (_isInSelectionMode) {
            // unhighlight previous selection
            editorField.invalidateSelectionRows();
        } else {
            // unhighlight caret
            editorField.invalidateCaretRow();
            if (mode) {
                setSelectText(true);
            } else {
                _isInSelectionMode = true;
            }
        }

        if (stopInput)
            stopTextComposing();

        editorField.set_selectionAnchor(beginPosition);
        editorField.set_selectionEdge(editorField._selectionAnchor + numChars);

        editorField.setCaretPosition(editorField._selectionEdge);

        updateCaretRow();
        if (mode) {
            editorField._selModeLis.onSelectionChanged(editorField.isSelectText(), editorField._selectionAnchor, editorField._selectionEdge);
        }
        boolean scrolled = editorField.makeCharVisible(editorField._selectionEdge);

        if (scrollToStart) {
            //TODO reduce unnecessary scrolling and write a method to scroll
            // the beginning of multi-line selections as far left as possible
            scrolled = editorField.makeCharVisible(editorField._selectionAnchor);
        }

        if (!scrolled) {
            editorField.invalidateSelectionRows();
        }
    }

    /**
     * Moves the caret to an edge of selected text and scrolls it to view.
     *
     * @param start If true, moves the caret to the beginning of
     *              the selection. Otherwise, moves the caret to the end of the selection.
     *              In all cases, the caret is scrolled to view if it is not visible.
     */
    public void focusSelection(boolean start) {
        if (_isInSelectionMode) {
            if (start && editorField.getCaretPosition() != editorField._selectionAnchor) {
                editorField.setCaretPosition(editorField._selectionAnchor);
                updateAfterCaretJump();
            } else if (!start && editorField.getCaretPosition() != editorField._selectionEdge) {
                editorField.setCaretPosition(editorField._selectionEdge);
                updateAfterCaretJump();
            }
        }
    }


    /**
     * Used by internal methods to update selection boundaries when a new
     * caret position is set.
     * Does nothing if not in selection mode.
     */
    private void updateSelectionRange(int oldCaretPosition, int newCaretPosition) {
        if (!_isInSelectionMode) {
            return;
        }

        if (oldCaretPosition < editorField._selectionEdge) {
            if (newCaretPosition > editorField._selectionEdge) {
                editorField.set_selectionAnchor(editorField._selectionEdge);
                editorField.set_selectionEdge(newCaretPosition);
            } else {
                editorField.set_selectionAnchor(newCaretPosition);
            }

        } else {
            if (newCaretPosition < editorField._selectionAnchor) {
                editorField.set_selectionEdge(editorField._selectionAnchor);
                editorField.set_selectionAnchor(newCaretPosition);
            } else {
                editorField.set_selectionEdge(newCaretPosition);
            }
        }
        editorField._selModeLis.onSelectionChanged(_isInSelectionMode, editorField._selectionAnchor, editorField._selectionEdge);
    }


    //- TextFieldController -----------------------------------------------
    //------------------------ Cut, copy, paste ---------------------------

    /**
     * Convenience method for consecutive copy and paste calls
     */
    public void cut(ClipboardManager cb) {
        editorField.copy(cb);
        selectionDelete();
    }

    /**
     * Copies the selected text to the clipboard.
     * <p>
     * Does nothing if not in select mode.
     */
    public void copy(ClipboardManager cb) {
        if (_isInSelectionMode && editorField._selectionAnchor < editorField._selectionEdge) {
            CharSequence contents = editorField.getDocumentProvider().subSequence(editorField._selectionAnchor, editorField._selectionEdge - editorField._selectionAnchor);
            ClipData clip = ClipData.newPlainText("CodeEditor", contents);
            cb.setPrimaryClip(clip);
        }
    }

    /**
     * Inserts text at the caret position.
     * Existing selected text will be deleted and select mode will end.
     * The deleted area will be invalidated.
     * <p>
     * After insertion, the inserted area will be invalidated.
     */
    public void paste(String text) {
        if (!editorField.isEditable())
            return;

        if (text == null) {
            return;
        }
        editorField.getDocumentProvider().beginBatchEdit();
        selectionDelete();

        int originalRow = editorField.getCaretRow();
        int originalOffset = editorField.getDocumentProvider().getRowOffset(originalRow);
        editorField.getDocumentProvider().insertBefore(text.toCharArray(), editorField.getCaretPosition(), System.nanoTime());
        editorField._textLis.onAdd(text, editorField.getCaretPosition(), text.length());
        editorField.getDocumentProvider().endBatchEdit();

        editorField.setCaretPosition(editorField.getCaretPosition() + text.length());
        updateCaretRow();

        editorField.setEdited(true);
        determineSpans();
        stopTextComposing();

        if (!editorField.makeCharVisible(editorField.getCaretPosition())) {
            int invalidateStartRow = originalRow;
            //invalidate previous row too if its wrapping changed
            if (editorField.getDocumentProvider().isWordWrap() && originalOffset != editorField.getDocumentProvider().getRowOffset(originalRow)) {
                --invalidateStartRow;
            }

            if (originalRow == editorField.getCaretRow() && !editorField.getDocumentProvider().isWordWrap()) {
                //pasted text only affects caret row
                editorField.invalidateRows(invalidateStartRow, invalidateStartRow + 1);
            } else {
                //TODO invalidate damaged rows only
                editorField.invalidateFromRow(invalidateStartRow);
            }
        }
    }

    /**
     * Deletes selected text, exits select mode and invalidates deleted area.
     * If the selected range is empty, this method exits select mode and
     * invalidates the caret.
     * <p>
     * Does nothing if not in select mode.
     */
    public void selectionDelete() {
        if (!editorField.isEditable())
            return;

        if (!_isInSelectionMode) {
            return;
        }

        int totalChars = editorField._selectionEdge - editorField._selectionAnchor;

        if (totalChars > 0) {
            int originalRow = editorField.getDocumentProvider().findRowNumber(editorField._selectionAnchor);
            int originalOffset = editorField.getDocumentProvider().getRowOffset(originalRow);
            boolean isSingleRowSel = editorField.getDocumentProvider().findRowNumber(editorField._selectionEdge) == originalRow;
            editorField.getDocumentProvider().deleteAt(editorField._selectionAnchor, totalChars, System.nanoTime());
            editorField._textLis.onDel("", editorField.getCaretPosition(), totalChars);
            editorField.setCaretPosition(editorField._selectionAnchor);
            updateCaretRow();
            editorField.setEdited(true);
            determineSpans();
            setSelectText(false);
            stopTextComposing();

            if (!editorField.makeCharVisible(editorField.getCaretPosition())) {
                int invalidateStartRow = originalRow;
                //invalidate previous row too if its wrapping changed
                if (editorField.getDocumentProvider().isWordWrap() &&
                        originalOffset != editorField.getDocumentProvider().getRowOffset(originalRow)) {
                    --invalidateStartRow;
                }

                if (isSingleRowSel && !editorField.getDocumentProvider().isWordWrap()) {
                    //pasted text only affects current row
                    editorField.invalidateRows(invalidateStartRow, invalidateStartRow + 1);
                } else {
                    //TODO invalidate damaged rows only
                    editorField.invalidateFromRow(invalidateStartRow);
                }
            }
        } else {
            setSelectText(false);
            editorField.invalidateCaretRow();
        }
    }

    void replaceText(int from, int charCount, String text) {
        int invalidateStartRow, originalOffset;
        boolean isInvalidateSingleRow = true;
        boolean dirty = false;
        //delete selection
        if (_isInSelectionMode) {
            //_isInSelectionMode替换会有问题，所以
            editorField.selectText(false);
        }
        if (_isInSelectionMode) {
            invalidateStartRow = editorField.getDocumentProvider().findRowNumber(editorField._selectionAnchor);
            originalOffset = editorField.getDocumentProvider().getRowOffset(invalidateStartRow);

            int totalChars = editorField._selectionEdge - editorField._selectionAnchor;

            if (totalChars > 0) {
                editorField.setCaretPosition(editorField._selectionAnchor);
                editorField.getDocumentProvider().deleteAt(editorField._selectionAnchor, totalChars, System.nanoTime());

                if (invalidateStartRow != editorField.getCaretRow()) {
                    isInvalidateSingleRow = false;
                }
                dirty = true;
            }

            setSelectText(false);
        } else {
            invalidateStartRow = editorField.getCaretRow();
            originalOffset = editorField.getDocumentProvider().getRowOffset(editorField.getCaretRow());
        }

        //delete requested chars
        if (charCount > 0) {
            int delFromRow = editorField.getDocumentProvider().findRowNumber(from);
            if (delFromRow < invalidateStartRow) {
                invalidateStartRow = delFromRow;
                originalOffset = editorField.getDocumentProvider().getRowOffset(delFromRow);
            }

            if (invalidateStartRow != editorField.getCaretRow()) {
                isInvalidateSingleRow = false;
            }

            editorField.setCaretPosition(from);
            editorField.getDocumentProvider().deleteAt(from, charCount, System.nanoTime());
            dirty = true;
        }

        //insert
        if (text != null && text.length() > 0) {
            int insFromRow = editorField.getDocumentProvider().findRowNumber(from);
            if (insFromRow < invalidateStartRow) {
                invalidateStartRow = insFromRow;
                originalOffset = editorField.getDocumentProvider().getRowOffset(insFromRow);
            }

            editorField.getDocumentProvider().insertBefore(text.toCharArray(), editorField.getCaretPosition(), System.nanoTime());
            editorField.setCaretPosition(editorField.getCaretPosition() + text.length());
            dirty = true;
        }

        if (dirty) {
            editorField.setEdited(true);
            determineSpans();
        }

        int originalRow = editorField.getCaretRow();
        updateCaretRow();
        if (originalRow != editorField.getCaretRow()) {
            isInvalidateSingleRow = false;
        }

        if (!editorField.makeCharVisible(editorField.getCaretPosition())) {
            //invalidate previous row too if its wrapping changed
            if (editorField.getDocumentProvider().isWordWrap() && originalOffset != editorField.getDocumentProvider().getRowOffset(invalidateStartRow)) {
                --invalidateStartRow;
            }

            if (isInvalidateSingleRow && !editorField.getDocumentProvider().isWordWrap()) {
                //replaced text only affects current row
                editorField.invalidateRows(editorField.getCaretRow(), editorField.getCaretRow() + 1);
            } else {
                //TODO invalidate damaged rows only
                editorField.invalidateFromRow(invalidateStartRow);
            }
        }
    }

    //- TextFieldController -----------------------------------------------
    //----------------- Helper methods for InputConnection ----------------

    /**
     * delete selection(from,from+charCount)
     * Deletes existing selected text, then deletes charCount number of
     * characters starting at from, and inserts text in its place.
     * <p>
     * Unlike paste or selectionDelete, does not signal the end of
     * text composing to the IME.
     */
    void replaceComposingText(int from, int charCount, String text) {
        int invalidateStartRow, originalOffset;
        boolean isInvalidateSingleRow = true;
        boolean dirty = false;

        //delete selection
        if (_isInSelectionMode) {
            invalidateStartRow = editorField.getDocumentProvider().findRowNumber(editorField._selectionAnchor);
            originalOffset = editorField.getDocumentProvider().getRowOffset(invalidateStartRow);

            int totalChars = editorField._selectionEdge - editorField._selectionAnchor;

            if (totalChars > 0) {
                editorField.setCaretPosition(editorField._selectionAnchor);
                editorField.getDocumentProvider().deleteAt(editorField._selectionAnchor, totalChars, System.nanoTime());

                if (invalidateStartRow != editorField.getCaretRow()) {
                    isInvalidateSingleRow = false;
                }
                dirty = true;
            }

            setSelectText(false);
        } else {
            invalidateStartRow = editorField.getCaretRow();
            originalOffset = editorField.getDocumentProvider().getRowOffset(editorField.getCaretRow());
        }

        //delete requested chars
        if (charCount > 0) {
            int delFromRow = editorField.getDocumentProvider().findRowNumber(from);
            if (delFromRow < invalidateStartRow) {
                invalidateStartRow = delFromRow;
                originalOffset = editorField.getDocumentProvider().getRowOffset(delFromRow);
            }

            if (invalidateStartRow != editorField.getCaretRow()) {
                isInvalidateSingleRow = false;
            }

            editorField.setCaretPosition(from);
            editorField.getDocumentProvider().deleteAt(from, charCount, System.nanoTime());
            dirty = true;
        }

        //insert
        if (text != null && text.length() > 0) {
            int insFromRow = editorField.getDocumentProvider().findRowNumber(from);
            if (insFromRow < invalidateStartRow) {
                invalidateStartRow = insFromRow;
                originalOffset = editorField.getDocumentProvider().getRowOffset(insFromRow);
            }

            editorField.getDocumentProvider().insertBefore(text.toCharArray(), editorField.getCaretPosition(), System.nanoTime());
            editorField.setCaretPosition(editorField.getCaretPosition() + text.length());
            dirty = true;
        }

        editorField._textLis.onAdd(text, editorField.getCaretPosition(), text.length() - charCount);
        if (dirty) {
            editorField.setEdited(true);
            determineSpans();
        }

        int originalRow = editorField.getCaretRow();
        updateCaretRow();
        if (originalRow != editorField.getCaretRow()) {
            isInvalidateSingleRow = false;
        }

        if (!editorField.makeCharVisible(editorField.getCaretPosition())) {
            //invalidate previous row too if its wrapping changed
            if (editorField.getDocumentProvider().isWordWrap() && originalOffset != editorField.getDocumentProvider().getRowOffset(invalidateStartRow)) {
                --invalidateStartRow;
            }

            if (isInvalidateSingleRow && !editorField.getDocumentProvider().isWordWrap()) {
                //replaced text only affects current row
                editorField.invalidateRows(editorField.getCaretRow(), editorField.getCaretRow() + 1);
            } else {
                //TODO invalidate damaged rows only
                editorField.invalidateFromRow(invalidateStartRow);
            }
        }
    }

    /**
     * Delete leftLength characters of text before the current caret
     * position, and delete rightLength characters of text after the current
     * cursor position.
     * <p>
     * Unlike paste or selectionDelete, does not signal the end of
     * text composing to the IME.
     */
    void deleteAroundComposingText(int left, int right) {
        int start = editorField.getCaretPosition() - left;
        if (start < 0) {
            start = 0;
        }
        int end = editorField.getCaretPosition() + right;
        int docLength = editorField.getDocumentProvider().docLength();
        if (end > (docLength - 1)) { //exclude the terminal EOF
            end = docLength - 1;
        }
        replaceComposingText(start, end - start, "");
    }

    String getTextAfterCursor(int maxLen) {
        int docLength = editorField.getDocumentProvider().docLength();
        if ((editorField.getCaretPosition() + maxLen) > (docLength - 1)) {
            //exclude the terminal EOF
            return editorField.getDocumentProvider().subSequence(editorField.getCaretPosition(), docLength - editorField.getCaretPosition() - 1).toString();
        }

        return editorField.getDocumentProvider().subSequence(editorField.getCaretPosition(), maxLen).toString();
    }

    String getTextBeforeCursor(int maxLen) {
        int caretPos = editorField.getCaretPosition();
        int start = caretPos - maxLen;
        if (start < 0) {
            start = 0;
        }
        return editorField.getDocumentProvider().subSequence(start, caretPos - start).toString();
    }
}//end inner controller class}