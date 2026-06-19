package tiiehenry.code.language;

import java.util.ArrayList;

public abstract class SampleFormatter<E, T> {

    public ArrayList<T> autoIndentIncrease = new ArrayList<>();
    public ArrayList<T> autoIndentDecrease = new ArrayList<>();
    public ArrayList<T> formatIndentIncreaseBefore = new ArrayList<>();
    public ArrayList<T> formatIndentIncreaseAfter = new ArrayList<>();
    public ArrayList<T> formatIndentDecreaseBefore = new ArrayList<>();
    public ArrayList<T> formatIndentIncreaseDef = new ArrayList<>();
    public ArrayList<T> formatIndentDecreaseDef = new ArrayList<>();
    public ArrayList<T> formatIndentDecreaseAfter = new ArrayList<>();
    public ArrayList<T> formatIndentDecreaseIncrease = new ArrayList<>();
    public ArrayList<T> formatIndentIncreaseDecrease = new ArrayList<>();
    public ArrayList<T> formatIndentLine = new ArrayList<>();
    public ArrayList<T> formatIndentWS = new ArrayList<>();

    public int createAutoIndent(E lexer) {
        int idt = 0;
        try {
            while (true) {
                TokenInfo<T> info = nextTokenInfo(lexer);
                if (info == null)
                    break;
                idt += indentAuto(info.type);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return idt;
    }

    private int indentAuto(T t) {
        if (autoIndentIncrease.contains(t))
            return 1;
        else if (autoIndentDecrease.contains(t))
            return -1;
        return 0;
    }

    private int indent(T type) {
        if (formatIndentIncreaseDef.contains(type)) {
            return 1;
        } else if (formatIndentDecreaseDef.contains(type)) {
            return -1;
        }
        return 0;
    }

    public static class TokenInfo<T> {
        public T type;
        public String text;
    }

    public abstract TokenInfo<T> nextTokenInfo(E lexer) throws Exception;

    /**
     *
     * @param lexer
     * @param indentChar 缩进字符，一般是空格或者\t
     * @param width 缩进宽度
     * @return
     * @throws Exception
     */
    public CharSequence format(E lexer, char indentChar, int width) throws Exception {
        StringBuilder builder = new StringBuilder();
        boolean isNewLine = true;
        int idt = 0;
        while (true) {
            TokenInfo<T> info = nextTokenInfo(lexer);
            if (info == null)
                break;
            T type = info.type;
            String text = info.text;

            if (formatIndentLine.contains(type)) {
                if (builder.length() > 0 && builder.charAt(builder.length() - 1) == ' ') {
                    builder.deleteCharAt(builder.length() - 1);
                }
                isNewLine = true;
                builder.append('\n');
                idt = Math.max(0, idt);
            } else if (isNewLine) {
                if (formatIndentWS.contains(type)) {
                } else {
                    if (formatIndentDecreaseBefore.contains(type) ||
                            formatIndentDecreaseIncrease.contains(type)) {
                        idt--;
                    } else if (formatIndentIncreaseBefore.contains(type) ||
                            formatIndentIncreaseDecrease.contains(type)) {
                        idt++;
                    }
                    builder.append(createIndentChar(idt * width, indentChar));
                    builder.append(text);
                    if (formatIndentIncreaseAfter.contains(type) ||
                            formatIndentDecreaseIncrease.contains(type)) {
                        idt++;
                    } else if (formatIndentDecreaseAfter.contains(type) ||
                            formatIndentIncreaseDecrease.contains(type)) {
                        idt--;
                    }
                    isNewLine = false;
                }
            } else if (formatIndentWS.contains(type)) {
                builder.append(' ');
            } else {
                builder.append(text);
                idt += indent(type);
            }

        }

        return builder;
    }

    /**
     *
     * @param n
     * @param indentChar
     * @return 若干个相同的char
     */
    private static char[] createIndentChar(int n, char indentChar) {
        if (n < 0) {
            return new char[0];
        }
        char[] idts = new char[n];
        for (int i = 0; i < n; i++) {
            idts[i] = indentChar;
        }
        return idts;
    }
}
