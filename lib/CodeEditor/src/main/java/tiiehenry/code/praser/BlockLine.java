package tiiehenry.code.praser;

public class BlockLine {
    public int startIndex;
    public int startLine;
    public int endIndex;
    public int endLine;
    public BlockLine(int startIndex, int startLine, int endIndex, int endLine) {
        this.startIndex = startIndex;
        this.startLine = startLine;
        this.endIndex = endIndex;
        this.endLine = endLine;
    }
}
