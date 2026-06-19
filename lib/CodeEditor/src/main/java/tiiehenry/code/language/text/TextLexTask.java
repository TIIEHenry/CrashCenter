package tiiehenry.code.language.text;

import android.graphics.Rect;

import java.util.List;

import tiiehenry.code.LexTask;
import tiiehenry.code.praser.BlockLine;
import tiiehenry.code.praser.SymbolPair;
import tiiehenry.code.praser.Span;
import tiiehenry.code.praser.Variable;
import tiiehenry.code.language.Language;
import tiiehenry.code.view.ColorScheme;

public class TextLexTask extends LexTask<String, Integer> {
    public TextLexTask(Language language) {
        super(language);
    }

    @Override
    protected boolean tokenizePrepared(List<Span> tokens, List<BlockLine> lines, List<SymbolPair> pairs, List<Variable> variables, String code, int fromIndex, int fromLineOffset, int fromLine, boolean analyze) {
        return false;
    }

    @Override
    protected ColorScheme.Colorable getTokenColorable(Integer tokenType) {
        return null;
    }
}
