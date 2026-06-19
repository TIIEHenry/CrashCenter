package tiiehenry.code.language.lua;

import java.util.ArrayList;
import java.util.List;

import tiiehenry.code.format.FormatCallback;
import tiiehenry.code.LexTask;
import tiiehenry.code.praser.BlockLine;
import tiiehenry.code.praser.SymbolPair;
import tiiehenry.code.praser.Span;
import tiiehenry.code.EditorException;
import tiiehenry.code.praser.Variable;
import tiiehenry.code.language.Language;
import tiiehenry.code.view.ColorScheme;

import static tiiehenry.code.language.lua.LuaType.*;

public class LuaLexTask extends LexTask<LuaLexer, LuaType> {
    public LuaLexTask(Language language) {
        super(language);
    }

    @Override
    protected boolean tokenizePrepared(List<Span> tokens, List<BlockLine> lines, List<SymbolPair> pairs, List<Variable> variables, String code, int fromIndex, int fromLineOffset, int fromLine, boolean analyze) {
        ArrayList<BlockLine> lineStacks = new ArrayList<>(8196);
        lastType = null;
        LuaType lastType2 = null;
        lastName = "";
        lexer = new LuaLexer(code);
        Language language = getLanguage();
        try {

            StringBuilder bul = new StringBuilder();
            boolean isModule = false;
            boolean hasDo = true;
            int lastNameIdx = -1;

            while (!abort) {
                LuaType tokenType = lexer.advance();
                if (tokenType == null)
                    break;
                int startIndex = fromIndex+lexer.yychar();
                int len = lexer.yylength();
                int line = fromLine+lexer.yyline() + 1;
                int lineOffset = fromLineOffset+lexer.yycolumn();
                int stopIndex = startIndex+len;

                if (isModule && lastType == LuaType.STRING && tokenType != LuaType.STRING) {
                    String mod = bul.toString();
                    if (bul.length() > 2)
                        addFuncName(mod.substring(1, mod.length() - 1));
                    bul = new StringBuilder();
                    isModule = false;
                }

                switch (tokenType) {
                    case DO:
                        if (hasDo) {
                            openVariableStack(variableStacks, startIndex);
                            openLineStack(lineStacks, line, startIndex);
                            hasDo = false;
                        }
                        break;
                    case WHILE:
                    case FOR:
                        hasDo = true;
                        break;
                    case FUNCTION:
                    case IF:
                    case SWITCH:
                    case REPEAT:
                        openVariableStack(variableStacks, startIndex);
                        openLineStack(lineStacks, line, startIndex);
                        break;
                    case END:
                    case UNTIL:
                        closeVariableStack(variableStacks, startIndex,variables);
                        closeLineStack(lineStacks, line, startIndex,lines);
                        break;
                    case STRING:
                        if (lastName.equals("require"))
                            isModule = true;
                        if (isModule)
                            bul.append(lexer.yytext());
                        break;
                    case NAME:
                        if (lastType2 == LuaType.NUMBER) {
                            Span p = tokens.get(tokens.size() - 1);
                            p.colorable = ColorScheme.Colorable.TEXT;
                            p.len += len;
                        }
                        String name = lexer.yytext();
                        if (lastType == LuaType.FUNCTION) {
                            //函数名
                            addVariableStackToSuper(variableStacks, Variable.Type.Func_User, name);
                            addFuncName(name);
                        } else if (isFuncName(name)) {
                        } else if (lastType == LuaType.GOTO || lastType == LuaType.AT) {
                        } else if (language.isBasePackage(name)) {
                        } else if (lastType == LuaType.DOT && language.isBasePackage(lastName) && language.isBaseWord(lastName, name)) {
                        } else if (language.isInternalFunc(name)) {
                        } else{
                            addVariableStack(variableStacks, Variable.Type.Var, name);
                        }

                        if (lastType == LuaType.ASSIGN && name.equals("require")) {
                            addFuncName(lastName);
                            addVariableStack(variableStacks, Variable.Type.Func_User, lastName);
                            if (lastNameIdx >= 0) {
                                Span p = tokens.get(lastNameIdx - 1);
                                p.colorable = ColorScheme.Colorable.LITERAL_FUNC;
                            }
                        }
                        lastNameIdx = tokens.size();
                        lastName = name;
                        break;
                }

                addTokenPair(tokenType,LBRACK, RBRACK, pairs, pairBrackStacks, startIndex, line);
                addTokenPair(tokenType,LCURLY, RCURLY, pairs, pairCurlyStacks, startIndex, line);
                addTokenPair(tokenType, LPAREN, RPAREN, pairs, pairParenStacks, startIndex, line);
                switch (tokenType) {
                    case STRING:
                        addPair(pairs,startIndex, line, stopIndex - 1, line);
                        break;
                }
                addTokenLine(tokenType, LCURLY, RCURLY, lines, lineCurlyStacks, startIndex, line);
                addTokenLine(tokenType, LPAREN, RPAREN, lines, lineParenStacks, startIndex, line);
                addTokenSpan(tokens,lexer, tokenType, startIndex, line, lineOffset, len);
                if (tokenType != LuaType.WHITE_SPACE) {
                    lastType = tokenType;
                }
                lastType2 = tokenType;
            }
        } catch (Exception e) {
            e.printStackTrace();
            EditorException.fail(e.getMessage());
            return false;
        }
        updateFuncName();
        closeVariableStack(variableStacks, 99999,variables);
        return true;
    }


    private LuaType lastType = null;
    private LuaLexer lexer = null;
    private String lastName = "";
    @Override
    protected ColorScheme.Colorable getTokenColorable(LuaType tokenType) {
        ColorScheme.Colorable type;
        switch (tokenType) {
            case DO:
            case WHILE:
            case FOR:
            case FUNCTION:
            case IF:
            case SWITCH:
            case REPEAT:
            case END:
            case UNTIL:
            case THEN:
            case ELSEIF:
            case ELSE:
            case RETURN:
            case BREAK:
            case LOCAL:
            case CASE:
            case DEFAULT:
            case CONTINUE:
            case GOTO:
                //关键字
                type = ColorScheme.Colorable.KEYWORD;
                break;
            case NOT:
            case AND:
            case OR:
            case IN:
                type = ColorScheme.Colorable.KEYWORD_SECOND;
                break;
            case TRUE:
            case FALSE:
            case NIL:
                type = ColorScheme.Colorable.BASIC_TYPE;
                break;
            case LCURLY:
            case RCURLY:
            case LPAREN:
            case RPAREN:
            case LBRACK:
            case RBRACK:
                type = ColorScheme.Colorable.SEPARATOR;
                break;
            case COMMA:
            case DOT:
                type = ColorScheme.Colorable.SYMBOL;
                break;
            case STRING:
            case LONG_STRING:
                type = ColorScheme.Colorable.LITERAL_STRING;
                break;
            case NAME:

                String name = lexer.yytext();
                if (lastType == LuaType.FUNCTION) {
                    //函数名
                    type = ColorScheme.Colorable.LITERAL_FUNC;
                } else if (isFuncName(name)) {
                    type = ColorScheme.Colorable.LITERAL_FUNC;
                } else if (lastType == LuaType.GOTO || lastType == LuaType.AT) {
                    type = ColorScheme.Colorable.LITERAL_FUNC;
                } else if (language.isBasePackage(name)) {
                    type = ColorScheme.Colorable.INTERNAL;
                } else if (lastType == LuaType.DOT && language.isBasePackage(lastName) && language.isBaseWord(lastName, name)) {
                    type = ColorScheme.Colorable.INTERNAL;
                } else if (language.isInternalFunc(name)) {
                    type = ColorScheme.Colorable.INTERNAL;
                } else{
                    type = ColorScheme.Colorable.TEXT;
                }
                break;
            case SHORT_COMMENT:
                type = ColorScheme.Colorable.COMMENT_SINGLE;
                break;
            case BLOCK_COMMENT:
                type = ColorScheme.Colorable.COMMENT_REGION;
                break;
            case DOC_COMMENT:
                type = ColorScheme.Colorable.COMMENT_DOC;
                break;
            case NUMBER:
                //数字
                type = ColorScheme.Colorable.LITERAL_NUM;
                break;
            default:
                type = ColorScheme.Colorable.TEXT;
        }
        return type;
    }

    @Override
    public boolean canFormat() {
        return true;
    }


    @Override
    public synchronized void format(String input, int width, int curPos, FormatCallback callback) throws Exception {
        LuaLexer lexer = new LuaLexer(input);
        CharSequence newStr = LuaFormatter.getInstance().format(lexer, getLanguage().indentChar, width);
        callback.onDone(newStr.toString(), curPos);
    }

    @Override
    public int createAutoIndent(CharSequence subSequence) {
        LuaLexer lexer = new LuaLexer(subSequence);
        return LuaFormatter.getInstance().createAutoIndent(lexer);
    }
}
