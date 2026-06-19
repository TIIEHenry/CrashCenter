package tiiehenry.code.language;

public abstract class LanguageCFamily extends Language {
    @Override
    public String getLineNoteStringStart() {
        return "//";
    }

    @Override
    public String getLineNoteStringEnd() {
        return "";
    }

    @Override
    public String getRegionNoteStringStart() {
        return "/*";
    }

    @Override
    public String getRegionNoteStringMiddle() {
        return "";
    }

    @Override
    public String getRegionNoteStringEnd() {
        return "*/";
    }

    @Override
    public String getRegionDocNoteStringStart() {
        return "/**";
    }

    @Override
    public String getRegionDocNoteStringMiddle() {
        return "*";
    }

    @Override
    public String getRegionDocNoteStringEnd() {
        return "*/";
    }
}
