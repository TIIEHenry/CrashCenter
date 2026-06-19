package tiiehenry.code.doc;

public class Selection {
    public final int start;
    public final int end;
    public final int len;

    public Selection(int start, int end) {
        this.start = start;
        this.end=end;
        this.len=end-start;
    }
}