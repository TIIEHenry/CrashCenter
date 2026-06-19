package tiiehenry.code.view;

import android.graphics.Typeface;

import tiiehenry.code.doc.DocumentProvider;
import tiiehenry.code.LexTask;
import tiiehenry.code.language.Language;

/**
 * @author TIIEHenry
 */
public class EditorSetting {

	public boolean isShowLineNumbers = CodeEditor.Default.isShowLineNumbers;
	//是否显示行号旁边的竖线
	public boolean isShowLineOffset = CodeEditor.Default.isShowLineOffset;
	//显示空白字符
	public boolean isShowNonPrinting = CodeEditor.Default.isShowNonPrinting;
	//是否显示代码块的竖线
	public boolean isShowBlockRegionLines = CodeEditor.Default.isShowBlockRegionLines;
	public boolean isShowBlockRegionHighlightLine = CodeEditor.Default.isShowBlockRegionHighlightLine;

	public boolean isWordWrap = CodeEditor.Default.isWordWrap;
	public boolean isCompatibilityMode = CodeEditor.Default.isCompatibilityMode;

	public boolean isAutoIndent = CodeEditor.Default.isAutoIndent;
	public int autoIndentWidth = CodeEditor.Default.autoIndentWidth;
	public boolean isAutoComplete = CodeEditor.Default.isAutoComplete;
	public int autoCompleteItemCount = CodeEditor.Default.autoCompleteItemCount;

	public Typeface defTypeface = CodeEditor.Default.defTypeface;
	public Typeface boldTypeface = CodeEditor.Default.boldTypeface;
	public Typeface italicTypeface = CodeEditor.Default.italicTypeface;
	// 是否被编辑过
	public boolean isEdited = CodeEditor.Default.isEdited;
	public boolean isShowIME = CodeEditor.Default.isShowIME;


	public boolean isShowMagnifier = CodeEditor.Default.isShowMagnifier;

	public boolean isLongPressCaps = CodeEditor.Default.isLongPressCaps;

	public boolean isHighlightCurrentRow = CodeEditor.Default.isHighlightCurrentRow;

	public int tabLength = CodeEditor.Default.tabLength;//need set
	public int caretWidth = CodeEditor.Default.caretWidth;

	public int yoyoSize = CodeEditor.Default.yoyoSize;


	public boolean isShowScrollbar = CodeEditor.Default.isShowScrollbar;
	public float slidingSpeedVertical = CodeEditor.Default.slidingSpeedVertical;
	public float slidingSpeedHorizontal = CodeEditor.Default.slidingSpeedHorizontal;

	public Language language= CodeEditor.Default.language;

	public LexTask<?,?> lexTask = CodeEditor.Default.lexTask;

	public DocumentProvider doc;
	public EditorSetting() {

	}

	@Override
	public EditorSetting clone() {
		EditorSetting s = new EditorSetting();
		s.isShowLineNumbers = this.isShowLineNumbers;
		s.isShowLineOffset = this.isShowLineOffset;
		s.isShowNonPrinting = this.isShowNonPrinting;
		s.isShowBlockRegionLines = this.isShowBlockRegionLines;
		s.isShowBlockRegionHighlightLine = this.isShowBlockRegionHighlightLine;
		s.isWordWrap = this.isWordWrap;
		s.isCompatibilityMode = this.isCompatibilityMode;
		s.isAutoIndent = this.isAutoIndent;
		s.autoIndentWidth = this.autoIndentWidth;
		s.isAutoComplete = this.isAutoComplete;
		s.autoCompleteItemCount = this.autoCompleteItemCount;

		s.defTypeface = this.defTypeface;
		s.boldTypeface = this.boldTypeface;
		s.italicTypeface = this.italicTypeface;
		s.isEdited = this.isEdited;
		s.isShowIME = this.isShowIME;
		s.isShowMagnifier = this.isShowMagnifier;
		s.isLongPressCaps = this.isLongPressCaps;
		s.isHighlightCurrentRow = this.isHighlightCurrentRow;
		s.doc = new DocumentProvider(this.doc);
		s.tabLength = this.tabLength;
		s.caretWidth = this.caretWidth;
		s.isShowScrollbar = this.isShowScrollbar;
		s.slidingSpeedVertical = this.slidingSpeedVertical;
		s.slidingSpeedHorizontal = this.slidingSpeedHorizontal;
		s.language=this.language;
		s.lexTask=this.lexTask;
		return s;
	}
}
