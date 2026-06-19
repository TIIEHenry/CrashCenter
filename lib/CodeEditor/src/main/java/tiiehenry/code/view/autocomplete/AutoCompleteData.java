package tiiehenry.code.view.autocomplete;

import tiiehenry.code.praser.Variable;

public class AutoCompleteData {
    public Variable.Type type;
    public String text;

    public AutoCompleteData(Variable.Type t, String s) {
        type = t;
        text = s;
    }
}
