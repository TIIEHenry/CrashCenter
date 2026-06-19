package tiiehenry.code.language;

import tiiehenry.code.LexTask;

public interface ILanguage {
    String getName();

    //是否为高级语言
    boolean isProgLang();

    String getLineNoteStringStart();

    String getLineNoteStringEnd();

    String getRegionNoteStringStart();

    String getRegionNoteStringMiddle();

    String getRegionNoteStringEnd();

    String getRegionDocNoteStringStart();

    String getRegionDocNoteStringMiddle();

    String getRegionDocNoteStringEnd();

    LexTask newLexTask();
}
