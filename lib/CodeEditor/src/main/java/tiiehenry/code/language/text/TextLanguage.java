package tiiehenry.code.language.text;

import tiiehenry.code.LexTask;
import tiiehenry.code.language.Language;

public class TextLanguage extends Language {

    private TextLanguage() {
        isWordWrap = true;
    }

    private static class SingletonInner {
        private static final TextLanguage language = new TextLanguage();
    }

    public static TextLanguage getInstance() {
        return SingletonInner.language;
    }

    @Override
    public String getName() {
        return "Text";
    }

    //是否为高级语言
    public boolean isProgLang() {
        return false;
    }

    @Override
    public String getLineNoteStringStart() {
        return "";
    }

    @Override
    public String getLineNoteStringEnd() {
        return "";
    }

    @Override
    public String getRegionNoteStringStart() {
        return "";
    }

    @Override
    public String getRegionNoteStringMiddle() {
        return "";
    }

    @Override
    public String getRegionNoteStringEnd() {
        return "";
    }

    @Override
    public String getRegionDocNoteStringStart() {
        return "";
    }

    @Override
    public String getRegionDocNoteStringMiddle() {
        return "";
    }

    @Override
    public String getRegionDocNoteStringEnd() {
        return "";
    }

    @Override
    public LexTask newLexTask() {
        return new TextLexTask(this);
    }

    public static TextLexTask defaultLexTask = new TextLexTask(TextLanguage.getInstance());

}

