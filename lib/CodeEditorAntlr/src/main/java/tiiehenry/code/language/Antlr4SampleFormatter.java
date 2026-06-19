package tiiehenry.code.language;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;

import static org.antlr.v4.runtime.Recognizer.EOF;

public class Antlr4SampleFormatter extends SampleFormatter<Lexer,Integer> {

    @Override
    public TokenInfo<Integer> nextTokenInfo(Lexer lexer) {
        Token token = lexer.nextToken();
        int type = token.getType();
        TokenInfo<Integer> info = new TokenInfo<>();
        info.type = type;
        info.text = token.getText();
        if (type == EOF)
            return null;
        return info;
    }

    @Override
    public CharSequence format(Lexer lexer, char indentChar, int width) throws Exception {
        return super.format(lexer, indentChar, width);
    }
}
