package tiiehenry.code.language.lua;

import tiiehenry.code.language.SampleFormatter;

public class LuaFormatter extends SampleFormatter<LuaLexer,LuaType> {

    private static class SingletonInner {
        private static LuaFormatter formatter = new LuaFormatter();
    }

    public static LuaFormatter getInstance() {
        return SingletonInner.formatter;
    }

    private LuaFormatter() {
        formatIndentLine.add(LuaType.NEW_LINE);
        formatIndentWS.add(LuaType.WHITE_SPACE);

        autoIndentIncrease.add(LuaType.DO);
        autoIndentIncrease.add(LuaType.FUNCTION);
        autoIndentIncrease.add(LuaType.THEN);
        autoIndentIncrease.add(LuaType.REPEAT);
        autoIndentIncrease.add(LuaType.LCURLY);
        autoIndentIncrease.add(LuaType.ELSE);

        autoIndentDecrease.add(LuaType.UNTIL);
        autoIndentDecrease.add(LuaType.RETURN);


        formatIndentIncreaseDef.add(LuaType.DO);
        formatIndentIncreaseDef.add(LuaType.FUNCTION);
        formatIndentIncreaseDef.add(LuaType.REPEAT);
        formatIndentIncreaseDef.add(LuaType.LCURLY);
        formatIndentDecreaseDef.add(LuaType.END);
        formatIndentDecreaseDef.add(LuaType.RCURLY);


        formatIndentIncreaseAfter.add(LuaType.IF);
        formatIndentIncreaseAfter.add(LuaType.CASE);
        formatIndentIncreaseAfter.add(LuaType.DEFAULT);
        formatIndentIncreaseAfter.add(LuaType.FUNCTION);

        formatIndentDecreaseBefore.add(LuaType.END);
        formatIndentDecreaseBefore.add(LuaType.UNTIL);
        formatIndentDecreaseBefore.add(LuaType.RCURLY);
//        formatIndentDecreaseAfter.add(CloseBrace);

        formatIndentDecreaseIncrease.add(LuaType.ELSE);
        formatIndentDecreaseIncrease.add(LuaType.ELSEIF);
    }

    @Override
    public TokenInfo<LuaType> nextTokenInfo(LuaLexer lexer) throws Exception {
        LuaType type = lexer.advance();
        if (type == null) {
            return null;
        }
        TokenInfo<LuaType> info = new TokenInfo<>();
        info.type = type;
        info.text = lexer.yytext();
        return info;
    }

}
