package tiiehenry.code.praser;

import android.graphics.Canvas;

import java.util.ArrayList;


import tiiehenry.code.view.ColorScheme;
import tiiehenry.code.view.EditorField;

public class Span {

    public enum FONTTYPE {
        DEFAULT, BOLD, ITALIC, BOLDITALIC
    }

    public boolean underline = false;//下划线
    public boolean throughline = false;//中划线
    public ColorScheme.Colorable background;//span的背景
    public FONTTYPE fontType=null;//字体

//    public Drawable throughline=false;//中划线
//    public boolean


    public int startIndex = -1;
    public int line = -1;
    public int lineOffset = -1;//不准确
    public int len;
    public ColorScheme.Colorable colorable;

    public Span(int len, ColorScheme.Colorable colorable) {
        this(0, 0, 0, len, colorable);
    }

    public Span(int line, int lineOffset, int startIndex, int len, ColorScheme.Colorable colorable) {
        this.line = line;
        this.lineOffset = lineOffset;
        this.startIndex = startIndex;
        this.len = len;
        this.colorable = colorable;
    }

    public int startIndex() {
        return startIndex;
    }

    public int endIndex() {
        return startIndex + len;
    }


    @Override
    public String toString() {
        return "startIndex:" + startIndex + ";line:" + line + ";lineOffset:" + lineOffset + ";len:" + len;
    }
}
