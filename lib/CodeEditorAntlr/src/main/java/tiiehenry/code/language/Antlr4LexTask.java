package tiiehenry.code.language;


import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tiiehenry.code.format.FormatCallback;
import tiiehenry.code.IndentStringBuilder;
import tiiehenry.code.LexTask;
import tiiehenry.code.praser.BlockLine;
import tiiehenry.code.praser.SymbolPair;
import tiiehenry.code.praser.Span;
import tiiehenry.code.EditorException;
import tiiehenry.code.praser.Variable;
import tiiehenry.code.praser.prog.EditorDrawer;


public abstract class Antlr4LexTask<L extends Lexer> extends LexTask<Token, Integer> {
    private final L lexer;
    private static final ExecutorService analysis;

    static {
        analysis = Executors.newSingleThreadExecutor();
    }

    protected final EditorDrawer strDrawer;

    protected abstract L generateLexer();

    protected boolean canAnalysis() {
        return false;
    }

    protected Antlr4LexTask(Language language) {
        super(language);
        lexer = generateLexer();
        strDrawer=new EditorDrawer();
    }

    @Override
    protected boolean tokenizePrepared(List<Span> tokens, List<BlockLine> lines, List<SymbolPair> pairs, List<Variable> variables, String code, int fromIndex, int fromLineOffset, int fromLine, boolean analyze) {
//        final ANTLRInputStream in = CharStreams.fromString(code);
        final CodePointCharStream in=CharStreams.fromString(code);
        lexer.setInputStream(in);
        try {
            tokenizeIn(tokens, lines, pairs, variables, lexer, fromIndex,fromLineOffset,fromLine,code);
            if (analyze && canAnalysis()) {
//                in.reset();
                in.seek(0);
                stopLast();
                analysis.execute(new Runnable() {
                    @Override
                    public void run() {
                        parse(in);
//                    EditorPagerAdapter.INSTANCE.resetError();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            EditorException.fail("antlr4 tokenize");
            return false;
        }
        return true;
    }

    protected void parse(CodePointCharStream i) {
    }

    protected void stopLast() {
        // TODO: Implement this method
    }

    public abstract void tokenizeIn(List<Span> tokens, List<BlockLine> lines, List<SymbolPair> pairs, List<Variable> variables, L lexer, int fromIndex, int fromLineOffset, int fromLine, String code);



    public void addErrorListener(ANTLRErrorListener listener) {
        generateLexer().addErrorListener(listener);
    }

    public void setKeywords(Vocabulary tokens) {
        String keywordPattern = "'[a-z_]+'";
        int len = tokens.getMaxTokenType();
        List<String> keywords = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            String name = tokens.getLiteralName(i);
            if (name != null && name.matches(keywordPattern)) {
                keywords.add(name.substring(1, name.length() - 1));
            }
        }
        String[] keywordsArr = new String[keywords.size()];
        language.setKeywords(keywords.toArray(keywordsArr));
    }

    @Override
    public synchronized void format(final String input, final int width, final int curPos, final FormatCallback callback) throws Exception {
        final StringBuilder sb = new StringBuilder();
        final IndentStringBuilder isb = new IndentStringBuilder(input.length());
        isb.indentChar = getLanguage().indentChar;
        lexer.setInputStream(CharStreams.fromString(input));
        int newPos = formatInThread(sb, isb, lexer, input, width, curPos);
        callback.onDone(sb.toString(), newPos);
    }

    protected synchronized int formatInThread(StringBuilder sb, IndentStringBuilder isb, L lexer, CharSequence input, int width, int curPos) throws Exception {
        sb.append(input.toString());
        return curPos;
    }

    @Override
    public LexTask.Selection expandSelection(String text, int oldStart, int oldEnd) {
        ParseTree tree = getTree();
        if (tree == null) {
            L lexer = this.lexer;
            lexer.reset();
            parse(lexer);
            tree = getTree();
        }
        Selection s = null;
        if (tree != null)
            s = expandSelection(tree, oldStart, oldEnd - 1);
        if (s == null)
            return super.expandSelection(text, oldStart, oldEnd);
        return s;
    }

    protected abstract void parse(L lexer);

    protected ParseTree getTree() {
        return null;
    }

    private Selection expandSelection(ParseTree tree, int oldStart, int oldEnd) {
        if (tree instanceof TerminalNode) {//只包含一个Token
            TerminalNode node = (TerminalNode) tree;
            Token tk = node.getSymbol();
            int start = tk.getStartIndex();
            int end = tk.getStopIndex();
            return expandSelection(oldStart, oldEnd, start, end);
        } else {
            ParserRuleContext ctx = (ParserRuleContext) tree;
            int start = ctx.start.getStartIndex();

            int end = (ctx.stop == null) ? ctx.start.getStopIndex() : ctx.stop.getStopIndex();
            if (oldStart >= start && oldEnd <= end) {
                //当前选择位置在这个节点内
                if (oldStart == start && oldEnd == end)
                    return null;
                int count = ctx.getChildCount();
                //遍历子节点
                for (int i = 0; i < count; i++) {
                    ParseTree tree1 = ctx.getChild(i);
                    Selection selection = expandSelection(tree1, oldStart, oldEnd);
                    //当前子节点不符合
                    if (selection == null)
                        continue;
                    //符合
                    return selection;
                }
                return expandSelection(oldStart, oldEnd, start, end);
            }
        }
        return null;
    }

    private LexTask.Selection expandSelection(int oldStart, int oldEnd, int start, int end) {
        if (oldStart >= start && oldEnd <= end) {
            if (oldStart == start && oldEnd == end)
                return null;
            return new LexTask.Selection(start, end + 1);
        }
        return null;
    }

    public static int compute(int max, int s, int e, int c, int n) {
        if (e < c)
            n = max + 1;
        else if (c >= s && c <= e) {
            n = max - e + c;
            int s1 = max - e + s;
            if (n < s1)
                n = s1;
            n++;
        }
        return n;
    }
}

