package tiiehenry.code;

import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;

import tiiehenry.code.format.FormatCallback;
import tiiehenry.code.doc.DocumentProvider;
import tiiehenry.code.language.Language;
import tiiehenry.code.praser.BlockLine;
import tiiehenry.code.praser.ErrorItem;
import tiiehenry.code.praser.SymbolPair;
import tiiehenry.code.praser.Span;
import tiiehenry.code.praser.Variable;
import tiiehenry.code.view.ColorScheme;

public abstract class LexTask<TOKEN, TYPE> implements Runnable {
    private Lexer _lexManager;
    /**
     * can be set by another thread to stop the scan immediately
     */
    private boolean isRunning;
    protected boolean abort;
    /**
     * A collection of Pairs, where Pair.first is the start
     * position of the token, and Pair.second is the type of the token.
     */
    protected final Language language;
    private DocumentProvider doc;

    public LexTask(Language language) {
        this.language = language;
        isRunning = false;
        abort = false;
    }

    protected ArrayList<Variable> variableStacks = new ArrayList<>(8196);
    protected ArrayList<BlockLine> lineCurlyStacks = new ArrayList<>(8196);
    protected ArrayList<BlockLine> lineParenStacks = new ArrayList<>(8196);
    protected ArrayList<BlockLine> lineBracketStacks = new ArrayList<>(8196);

    protected ArrayList<SymbolPair> pairParenStacks = new ArrayList<>(8196);
    protected ArrayList<SymbolPair> pairBrackStacks = new ArrayList<>(8196);
    protected ArrayList<SymbolPair> pairCurlyStacks = new ArrayList<>(8196);
    protected ArrayList<SymbolPair> pairAngleStacks = new ArrayList<>(8196);
    protected ArrayList<SymbolPair> pairAngeleStacks = new ArrayList<>(8196);
    protected ArrayList<SymbolPair> pairQuoteStacks = new ArrayList<>(8196);


    protected void addTokenPair(TYPE tokenType, TYPE openType, TYPE closeType, List<SymbolPair> pairs, ArrayList<SymbolPair> pairStacks, int startIndex, int line) {
        if (tokenType == openType) {
            openPairStack(pairStacks, line, startIndex);
        } else if (tokenType == closeType) {
            closePairStack(pairStacks, line, startIndex, pairs);
        }
    }


    protected void addTokenLine(TYPE tokenType, TYPE openType, TYPE closeType, List<BlockLine> lines, ArrayList<BlockLine> lineStacks, int startIndex, int line) {
        if (tokenType == openType) {
            openLineStack(lineStacks, line, startIndex);
        } else if (tokenType == closeType) {
            closeLineStack(lineStacks, line, startIndex, lines);
        }
    }

    protected void addTokenSpan(List<Span> tokens, TOKEN token, TYPE tokenType, int startIndex, int line, int lineOffset, int len) {
        Span s = getTokenSpan(token, tokenType, startIndex, line, lineOffset, len);
        s.fontType = getTokenFontType(token, tokenType, startIndex, line, lineOffset, len);
        tokens.add(s);
    }

    protected void addTokenSpan(List<Span> tokens, int line, int lineOffset, int startIndex, int len, ColorScheme.Colorable colorable) {
        tokens.add(new Span(line, lineOffset, startIndex, len, colorable));
    }

    protected Span getTokenSpan(TOKEN token, TYPE tokenType, int startIndex, int line, int lineOffset, int len) {
        ColorScheme.Colorable type = getTokenColorable(tokenType);
        return new Span(line, lineOffset, startIndex, len, type);
    }

    protected Span.FONTTYPE getTokenFontType(TOKEN token, TYPE tokenType, int startIndex, int line, int lineOffset, int len) {
        return Span.FONTTYPE.DEFAULT;
    }

    protected abstract ColorScheme.Colorable getTokenColorable(TYPE tokenType);

    protected void makeTokenVariableRegion(TYPE tokenType, TYPE openType, TYPE closeType, List<Variable> variables, ArrayList<Variable> variableStacks, int startIndex) {
        if (tokenType == openType) {
            openVariableStack(variableStacks, startIndex);
        } else if (tokenType == closeType) {
            closeVariableStack(variableStacks, startIndex, variables);
        }

    }

    protected void addVariable(TOKEN token, TYPE tokenType) {
    }


//    protected int getTokenStartIndex(TOKEN token,int fromIndex)

    public void abort() {
        if (isRun())
            abort = true;
    }

    public boolean isRun() {
        return isRunning;
    }

    void setLexer(Lexer p) {
        _lexManager = p;
    }


    public synchronized void setDocumentProvider(DocumentProvider hDoc) {
        doc = hDoc;
    }

    @Override
    public synchronized void run() {
        isRunning = true;
        abort = false;

        ArrayList<BlockLine> lines = new ArrayList<>(8196);
        ArrayList<SymbolPair> pairs = new ArrayList<>(8196);
        ArrayList<Variable> variables = new ArrayList<>(8196);
        List<Span> tokens = new ArrayList<>();
        boolean noException = tokenize(tokens, lines, pairs, variables, doc.toString(), 0, 0, 0, true);
        setBlockRegionLinesList(lines);
        setBlockPairsList(pairs);
        setBlockVariableList(variables);
        _lexManager.tokenizeDone(tokens);
        isRunning = false;
    }


    //lextask调用 第一入口
    public synchronized boolean tokenize(List<Span> tokens, List<BlockLine> lines, List<SymbolPair> pairs, List<Variable> variables, String code, int fromIndex, int fromLineOffset, int fromLine, boolean analyze) {
        lineCurlyStacks.clear();
        lineParenStacks.clear();
        lineBracketStacks.clear();
        pairParenStacks.clear();
        pairBrackStacks.clear();
        pairCurlyStacks.clear();
        pairAngleStacks.clear();
        pairAngeleStacks.clear();
        pairQuoteStacks.clear();
        variableStacks.clear();
        openVariableStack(variableStacks, fromIndex);
        boolean noException = tokenizePrepared(tokens, lines, pairs, variables, code, fromIndex, fromLineOffset, fromLine, analyze);
        if (tokens.size() == 0) {
            addTokenSpan(tokens,  fromLine, fromLineOffset, fromIndex, code.length(), ColorScheme.Colorable.TEXT);
        } else {
            Span t = tokens.get(tokens.size() - 1);
            int len = fromIndex + code.length() - t.endIndex();
            if (len > 0)
                addTokenSpan(tokens, t.line, t.lineOffset, t.endIndex(), len, ColorScheme.Colorable.TEXT);
        }
        closeVariableStack(variableStacks, fromIndex +code.length() + 1, variables);
        return noException;
    }

    //lextask调用 第一入口
    protected abstract boolean tokenizePrepared(List<Span> tokens, List<BlockLine> lines, List<SymbolPair> pairs, List<Variable> variables, String code, int fromIndex, int fromLineOffset, int fromLine, boolean analyze);


    public synchronized void formatWithCallback(final String input, final int width, final int curPos, final FormatCallback callback) {
        new Thread() {
            @Override
            public void run() {
                formatWithCallbackInThread(input, width, curPos, callback);
            }
        }.start();
    }

    public synchronized void formatWithCallbackInThread(final String input, final int width, final int curPos, final FormatCallback callback) {
        try {
            format(input, width, curPos, callback);
        } catch (Exception e) {
            e.printStackTrace();
            callback.onError(input, e.getMessage());
        }
    }

    public synchronized void format(String input, int width, int curPos, FormatCallback callback) throws Exception {
        throw new RuntimeException("unsupport format");
    }

    public boolean canCheck() {
        return false;
    }

    public ArrayList<ErrorItem> checkError(String input) {
        return new ArrayList<>();
    }

    public boolean canFormat() {
        return false;
    }

    public Selection expandSelection(String text, int oldStart, int oldEnd) {
        return new Selection(0, text.length());
    }


    public final Language getLanguage() {
        return language;
    }

    public int createAutoIndent(CharSequence subSequence) {
        return 0;
    }

    public static class Selection {
        public final int start, len;

        public Selection(int start, int end) {
            this.start = start;
            this.len = end - start;
        }
    }

    public boolean isError(int index) {
        return false;
    }


    private List<BlockLine> blockRegionLinesList = new ArrayList<>(8196);

    public List<BlockLine> getBlockRegionLinesList() {
        return blockRegionLinesList;
    }

    private void setBlockRegionLinesList(List<BlockLine> list) {
        blockRegionLinesList = list;
    }

    private List<SymbolPair> blockPairsList = new ArrayList<>(8196);

    public List<SymbolPair> getBlockPairsList() {
        return blockPairsList;
    }

    private void setBlockPairsList(List<SymbolPair> pairs) {
        blockPairsList = pairs;
    }

    public static void openPairStack(ArrayList<SymbolPair> stacks, int line, int startIndex) {
        SymbolPair parenPairdrawable = new SymbolPair(startIndex, line, 0, line);
        stacks.add(parenPairdrawable);
    }

    public void closePairStack(ArrayList<SymbolPair> stacks, int line, int startIndex, List<SymbolPair> pairs) {
        int size = stacks.size();
        if (size > 0) {
            SymbolPair rect = stacks.remove(size - 1);
            rect.bottom = line;
            rect.right = startIndex;
            pairs.add(rect);
        }
    }

    public void addPair(List<SymbolPair> pairs, int startIndex, int startLine, int stopIndex, int stopLine) {
        SymbolPair parenPairdrawable = new SymbolPair(startIndex, startLine, stopIndex, stopLine);
        pairs.add(parenPairdrawable);
    }

    public static void openStacks(ArrayList<Rect>[] stacksArray, int line, int startIndex) {
        Rect parenRect = new Rect(startIndex, line, 0, line);
        for (ArrayList<Rect> stacks : stacksArray) {
            stacks.add(parenRect);
        }
    }

    public static void openLineStack(ArrayList<BlockLine> stacks, int line, int startIndex) {
        BlockLine parenRect = new BlockLine(startIndex, line, 0, line);
        stacks.add(parenRect);
    }

    public void closeLineStack(ArrayList<BlockLine> stacks, int line, int startIndex, List<BlockLine> lines) {
        int size = stacks.size();
        if (size > 0) {
            BlockLine rect = stacks.remove(size - 1);
            rect.endLine = line;
            rect.endIndex = startIndex;

            if (rect.endLine - rect.startLine > 1)
                lines.add(rect);
        }
    }

    private List<Variable> blockVariableList = new ArrayList<>(8196);

    public List<Variable> getBlockVariableList() {
        return blockVariableList;
    }

    public void setBlockVariableList(List<Variable> variables) {
        blockVariableList = variables;
    }

    public static void openVariableStack(ArrayList<Variable> stacks, int startIndex) {
        Variable v = new Variable(startIndex);
        stacks.add(v);
    }

    public static void setVariableToTopLevel(ArrayList<Variable> stacks, Variable.Type type, String name, ArrayList<Variable> variables) {
        if (variables.size() > 0) {
            boolean hasTop = false;
            for (Variable v : variables) {
                if (v.typeList.contains(type)) {
                    if (v.nameList.contains(name)) {
                        hasTop = true;
                    }
                }
                if (!hasTop) {//添加一个新的到顶层
                    Variable topV = variables.get(0);
                    topV.typeList.add(type);
                    topV.nameList.add(name);
                }
            }
        }
    }

    public static void addVariableStack(ArrayList<Variable> stacks, Variable.Type type, String name) {
        int variableListSize = stacks.size();
        if (variableListSize > 0) {
            Variable rect = stacks.get(variableListSize - 1);
            rect.typeList.add(type);
            rect.nameList.add(name);
        }
    }

    public static void addVariableStackToSuper(ArrayList<Variable> stacks, Variable.Type type, String name) {
        int variableListSize = stacks.size();
        if (variableListSize > 1) {
            Variable rect = stacks.get(variableListSize - 2);
            rect.typeList.add(type);
            rect.nameList.add(name);
        }
    }

    public void closeVariableStack(ArrayList<Variable> stacks, int endIndex, List<Variable> variables) {
        int variableListSize = stacks.size();
        if (variableListSize > 0) {
            Variable rect = stacks.remove(variableListSize - 1);
            rect.endIndex = endIndex;
            variables.add(rect);
        }
    }

    protected String replaceIndentChar(String str, String indentStr) {
        String[] strArray = str.split("\n");
        StringBuilder newStr = new StringBuilder();
        for (String lineText : strArray) {
            boolean isStart = true;
            int txtStartIndex = 0;
            StringBuilder lineCode = new StringBuilder();
            char[] lineArray = lineText.toCharArray();
            for (char s : lineArray) {
                if (isStart) {
                    if (s == ' ' || s == '\t') {
                        txtStartIndex++;
                    } else {
                        isStart = false;
                        lineCode.append(s);
                    }
                } else {
                    lineCode.append(s);
                }
            }
            for (int d = 0; d < txtStartIndex; d += 2) {
                newStr.append(indentStr);
            }
            newStr.append(lineCode);
            newStr.append('\n');
        }
        return newStr.toString();
    }


    //用户函数，方法
    private String[] _funcNames = new String[0];
    private final ArrayList<String> _funcNameCache = new ArrayList<>();


    public void addFuncName(String name) {
        if (!_funcNameCache.contains(name) && !getLanguage().isInternalFunc(name)) {
            _funcNameCache.add(name);
        }
    }


    public String[] getFuncNames() {
        return _funcNames;
    }


    public void clearFuncName() {
        _funcNameCache.clear();
    }


    public void updateFuncName() {
        String[] uw = new String[_funcNameCache.size()];
        _funcNames = _funcNameCache.toArray(uw);
    }


    public boolean isFuncName(String s) {
        return _funcNameCache.contains(s);
    }
}

