package tiiehenry.code.view;

import java.util.ArrayList;

public class ColorScheme {

    public static boolean isDark = false;

    public static void setDark(boolean dark) {
        isDark = dark;
        notifyAllOnColorChangedListeners();
    }

    public enum Colorable {
        KEYWORD(BLUE_DARK, 0xffcc7832),//关键字pubic
        KEYWORD_SECOND(BLUE_DARK, 0xffcc7832),//次关键字 in as
        SEPARATOR(BLUE_DARK, 0xffcc7832),//(){};

        PACKAGE(0xff606060, 0xffaaaaaa),//android.os.*

        BASIC_TYPE(0xFF2C92C8, 0xffa9b7A6),//基本类型int
        INTERNAL(0xFF2C92C8, 0xffa9b7E6),//内置函数
        LITERAL_FUNC(BLUE_DARK, 0xffdddddd),//函数名

        LINK(BLUE_DARK, 0xffcc7832),//超链接

        TEXT(OFF_BLACK, 0xffdddddd),//普通文本
        SYMBOL(OFF_BLACK, 0xffdddddd),//普通符号
        LITERAL_VAR(OFF_BLACK, 0xffdddddd),//变量名

        LITERAL_NUM(0xffbc0000, 0xff559799),//数字
//        COLOR(0xffbc0000, 0xff559799),//颜色代码
        LITERAL_STRING(0xFFAA2200, 0xff5597bb),//字符串

        ERROR(RED, RED),//错误
        WARNING(PURPLE2, PURPLE2),//警告

        COMMENT_SINGLE(GREEN_LIGHT, 0xff777777),//注释
        COMMENT_REGION(GREEN_LIGHT, 0xff777777),//多行注释
        COMMENT_DOC(GREEN_LIGHT, 0xff777777),//文档注释

        NON_PRINTING_GLYPH(0x33000000, 0x33ffffff),//空白字符
        VERTICAL_LINE(0x33000000, 0x33ffffff),//分割线
        BACKGROUND(OFF_WHITE, 0xff2b2b2b),//背景
        SELECTION_FOREGROUND(OFF_WHITE, OFF_WHITE),//选中的文字
        SELECTION_BACKGROUND(0xFF999999, 0xFF999999),//选中的背景
//        CARET_BACKGROUND(0xFF000000, 0xFF000000),//yoyo背景
        CARET_BACKGROUND(0xFF546e7a, 0xFF78909c),//yoyo背景
        CARET_DISABLED(GREEN_DARK, 0xffbbbbbb),//光标
        SLIDEBAR_HANDLED(0x66000000, 0x66FFFFFF),//
        SLIDEBAR_BACKGROUND(0x4E000000, 0x4EFFFFFF),//
        LINE_OFFSET_HIGHLIGHT(0xffcfcfcf, 0xff454545),
        LINE_OFFSET(0xffeeeeee, 0xff363636),//,行号背景
        LINE_HIGHLIGHT(0xffeeeeee, 0xff363636),//当前行背景
        LINENUMBER(0x60000000, 0x44ffffff),//行号

        AUTOCOMPLETE_TEXT(OFF_BLACK, 0xffdddddd),
        AUTOCOMPLETE_KEYWORD(BLUE_DARK, 0xffcc7832),
        AUTOCOMPLETE_FUNC(0xFF2C92C8, 0xffa9b7E6),
        AUTOCOMPLETE_BACKGROUND(OFF_WHITE, 0xff2b2b2b);

        Colorable(int l, int d) {
            lightColor = l;
            darkColor = d;
        }

        int lightColor, darkColor;

        public int getColor() {
            if (isDark)
                return darkColor;
            return lightColor;
        }

        public void setColor(int color) {
            if (isDark) {
                darkColor = color;
            } else
                lightColor = color;
        }
    }

    private static final ArrayList<OnColorChangedListener> listeners = new ArrayList<>();

    public static void addOnColorChangedListener(OnColorChangedListener l) {
        listeners.add(l);
    }

    public static void removeOnColorChangedListener(OnColorChangedListener l) {
        listeners.remove(l);
    }

    public static void notifyAllOnColorChangedListeners() {
        for (OnColorChangedListener l : listeners) {
            l.onColorChanged();
        }
    }

   public interface OnColorChangedListener {
        void onColorChanged();
    }

    private static final int GREEN_DARK = 0xFF3F7F5F;
    private static final int OFF_WHITE = 0xFFFFFFFF;
    private static final int OFF_BLACK = 0xFF222222;
    private static final int GREEN_LIGHT = 0xFF009B00;
    private static final int BLUE_DARK = 0xFF2C82C8;
    private static final int RED = 0xeeFF0000;
    private static final int PURPLE2 = 0xFFFF00FF;
}
