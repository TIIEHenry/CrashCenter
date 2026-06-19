package tiiehenry.code.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

import java.io.File;

import tiiehenry.code.LexTask;
import tiiehenry.code.language.Language;
import tiiehenry.code.language.text.TextLanguage;

public abstract class CodeTextField extends EditorField {
	public static class Default {
		public static boolean isShowLineNumbers = true;
		//是否显示行号旁边的竖线
		public static boolean isShowLineOffset = true;
		//显示空白字符
		public static boolean isShowNonPrinting = false;
		//是否显示代码块的竖线
		public static boolean isShowBlockRegionLines = true;
		public static boolean isShowBlockRegionHighlightLine = true;

		public static boolean isAutoIndent = true;
		public static char indentChar = '\t';
		public static int autoIndentWidth = 1;
		public static boolean isAutoComplete = true;
		public static int autoCompleteItemCount = 4;

		public static Typeface defTypeface = Typeface.MONOSPACE;
		public static Typeface boldTypeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
		public static Typeface italicTypeface = Typeface.create(Typeface.MONOSPACE, Typeface.ITALIC);
		// 是否被编辑过
		public static boolean isEdited = false;
		public static boolean isShowIME = true;


		public static boolean isShowMagnifier = true;

		public static boolean isLongPressCaps = false;

		public static boolean isHighlightCurrentRow = true;

		public static int tabLength = 3;
		public static int caretWidth = 6;

		public static int yoyoSize = 16;



		public static boolean isShowScrollbar = true;
		public static float slidingSpeedVertical = 1;
		public static float slidingSpeedHorizontal = 1;

		public static Language language = TextLanguage.getInstance();

		public static LexTask lexTask = TextLanguage.defaultLexTask;


		public static boolean isWordWrap = false;
		public static boolean isCompatibilityMode = false;
		public static int textSizeSP=15;
		public static void initFont(File fontDir) {
			File df = new File(fontDir, "default.ttf");
			if (df.isFile()) {
				defTypeface=Typeface.createFromFile(df);
			}
			File bf = new File(fontDir, "bold.ttf");
			if (bf.isFile()) {
				boldTypeface=Typeface.createFromFile(bf);
				boldTypeface=Typeface.create(boldTypeface, Typeface.BOLD);
			}
			File tf = new File(fontDir, "italic.ttf");
			if (tf.isFile()) {
				italicTypeface=Typeface.createFromFile(tf);
				italicTypeface=Typeface.create(italicTypeface, Typeface.ITALIC);
			}
		}
	}



	public CodeTextField(Context context) {
		super(context);
	}

	public CodeTextField(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CodeTextField(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setDark(boolean isDark) {
		ColorScheme.setDark(isDark);
	}


	public int getLength() {
		return getDocumentProvider().docLength();
	}



	public TextFieldUiState getUiState() {
		stopScrolling();
		return new TextFieldUiState(this);
	}

	public void restoreUiState(final TextFieldUiState uiState) {
		stopScrolling();
		setSetting(uiState.setting);

		setTextSize((int) uiState.textSize);

		final int caretPosition = uiState.caretPosition;
		// If the text field is in the process of being created, it may not
		// have its width and height set yet.
		// Therefore, post UI restoration tasks to run later.
		if (uiState.selectMode) {
			final int selStart = uiState.selectBegin;
			final int selEnd = uiState.selectEnd;

			post(new Runnable() {
				@Override
				public void run() {
					setSelectionRange(selStart, selEnd - selStart);
					if (caretPosition < selEnd) {
						focusSelectionStart(); //caret at the end by default
					}
					setScrollX(uiState.scrollX);
					setScrollY(uiState.scrollY);
				}
			});
		} else {
			post(new Runnable() {
				@Override
				public void run() {
					moveCaret(caretPosition);
					setScrollX(uiState.scrollX);
					setScrollY(uiState.scrollY);
				}
			});
		}
	}

	//*********************************************************************
	//**************** UI State for saving and restoring ******************
	//*********************************************************************
	public static class TextFieldUiState {
		final int caretPosition;
		final int scrollX;
		final int scrollY;
		final boolean selectMode;
		final int selectBegin;
		final int selectEnd;
		final float textSize;
		public EditorSetting setting;

		public TextFieldUiState(CodeTextField textField) {
			textSize = textField.getTextSize();
			caretPosition = textField.getCaretPosition();
			scrollX = textField.getScrollX();
			scrollY = textField.getScrollY();
			selectMode = textField.isSelectText();
			selectBegin = textField.getSelectionStart();
			selectEnd = textField.getSelectionEnd();
			setting = textField.getSetting().clone();
		}

	}


}
