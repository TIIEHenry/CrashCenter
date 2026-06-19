package tiiehenry.code.praser;

public class ErrorItem<T> {
    public int line;
    public int startIndex;
    public int endIndex;
    public String message;
    public ErrorType type;
    public T data;
    public enum ErrorType{

    }
}
