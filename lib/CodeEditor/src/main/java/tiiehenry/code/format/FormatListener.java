package tiiehenry.code.format;

public interface FormatListener {
    void onPrepare();
    void onDone();
    void onError(String errMsg);
}
