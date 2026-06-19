package tiiehenry.code.language;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import tiiehenry.code.view.CodeEditor;
import tiiehenry.code.view.ColorScheme;

/**
 * 高级语言类
 * 包含符号
 */
public abstract class Language implements ILanguage {
    public final static char EOF = '\uFFFF';
    public final static char NULL_CHAR = '\u0000';
    public final static char NEWLINE = '\n';
    public final static char BACKSPACE = '\b';
    public final static char TAB = '\t';
    public final static String GLYPH_NEWLINE = "\u21b5";
    public final static String GLYPH_SPACE = "\u00b7";
    public final static String GLYPH_TAB = "\u00bb";

    public char indentChar = CodeEditor.Default.indentChar;

    public String getIndentStr(int width) {
        return getIndentStr(indentChar, width);
    }

    public static String getIndentStr(String indentStr, int width) {
        StringBuilder indentBuilder = new StringBuilder();
        for (int i = 0; i < width; i++)
            indentBuilder.append(indentStr);
        return indentBuilder.toString();
    }

    public static String getIndentStr(char indentChar, int width) {
        StringBuilder indentBuilder = new StringBuilder();
        for (int i = 0; i < width; i++)
            indentBuilder.append(indentChar);
        return indentBuilder.toString();
    }

    public boolean isWordWrap = false;
    protected ArrayList<String> symbolList = new ArrayList<>();

    {
        String[] sym = new String[]{
                "-",
                "+",
                "=",
                "{", "}",
                "\"",
                "(", ")",
                "_",
                "%",
                "<", ">",
                "[", "]",
                "'",
                "~",
                "^",
                "$"
        };
        Collections.addAll(symbolList, sym);
    }

    public ArrayList<String> getSymbolList() {
        return symbolList;
    }

    public CharSequence complete(ArrayList<String> buf, CharSequence constraint) {
        String word = constraint.toString().toLowerCase();
        for (String k : _keyword) {
            if (k.toLowerCase().startsWith(word))
                buf.add(k);
        }
        return constraint;
    }

    private HashMap<String, ColorScheme.Colorable> _keywords = new HashMap<>(0);
    private String[] _keyword = new String[0];

    /**
     * @return names
     */
    public void setKeywords(String[] keywords) {
        _keyword = keywords;
        _keywords = new HashMap<>(keywords.length);
        for (int i = 0; i < keywords.length; ++i) {
            _keywords.put(keywords[i], ColorScheme.Colorable.KEYWORD);
        }
    }

    public boolean isKeyword(String s) {
        return _keywords.containsKey(s);
    }

    public String[] getKeywords() {
        return _keyword;
    }


    private String[] _name = new String[0];

    public void setInternalFuncs(String[] names) {
        _name = names;
    }


    public String[] getInternalFuncs() {
        return _name;
    }


    public final boolean isInternalFunc(String s) {
        for (String n : _name) {
            if (n.equals(s))
                return true;
        }
        return false;
    }

    public void addInternalFunc(String[] names) {
        String[] old = this.getInternalFuncs();
        String[] news = new String[old.length + names.length];
        System.arraycopy(old, 0, news, 0, old.length);
        System.arraycopy(names, 0, news, old.length, names.length);
        this.setInternalFuncs(news);

    }

    //包(os,table)，类(File)
    private final HashMap<String, String[]> _basePackages = new HashMap<>(0);


    public void addBasePackage(String name, String[] names) {
        _basePackages.put(name, names);
    }


    public String[] getBasePackage(String name) {
        return _basePackages.get(name);
    }


    public final boolean isBasePackage(String s) {
        return _basePackages.containsKey(s);
    }


    public final boolean isBaseWord(String p, String s) {
        String[] pkg = _basePackages.get(p);
        for (String n : pkg) {
            if (n.equals(s)) {
                return true;
            }
        }
        return false;
    }


    private final List<String> _operators = new ArrayList<>();


    public List<String> getOperators() {
        return _operators;
    }

    public void setOperators(String[] operators) {
        _operators.clear();
        addOperators(operators);
    }

    public void addOperators(String[] operators) {
        _operators.addAll(Arrays.asList(operators));
    }


    public boolean isWhitespace(char c) {
        return (c == ' ' || c == '\n' || c == '\t' ||
                c == '\r' || c == '\f' || c == EOF);
    }

    public boolean isSentenceTerminator(char c) {
        return (c == '.');
    }

    //是否为高级语言
    @Override
    public boolean isProgLang() {
        return true;
    }


    /**
     * 是否是分隔符，隔开变量的
     *
     * @param c
     * @return
     */
    public boolean isSplitChar(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == ':' || c == ';' || c == '#';
    }


/*
    public char getEOFChar() {
        return '\uFFFF';
    }

    public char getNullChar() {
        return '\u0000';
    }

    public char getNewlineChar() {
        return '\n';
    }

    public char getBackspaceChar() {
        return '\b';
    }

    public char getTabChar() {
        return '\t';
    }

    public String getNewlineGlyphString() {
        return "\u21b5";
    }

    public String getSpaceGlyphString() {
        return "\u00b7";
    }

    public String getTabGlyphString() {
        return "\u00bb";
    }*/

}

