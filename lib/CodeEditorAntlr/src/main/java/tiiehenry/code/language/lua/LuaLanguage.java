package tiiehenry.code.language.lua;

import java.util.Collections;

import tiiehenry.code.LexTask;
import tiiehenry.code.Lexer;
import tiiehenry.code.language.Language;

/**
 * Singleton class containing the symbols and operators of the Javascript language
 */
public class LuaLanguage extends Language {

	private static class SingletonInner {
		private static LuaLanguage language = new LuaLanguage();
	}
	public static LuaLanguage getInstance() {
		return SingletonInner.language;
	}
	@Override
	public String getName() {
		return "Lua";
	}

	private final static String KEYWORD_TARGET = "and|break|do|else|elseif|end|false|for|function|goto|if|in|local|nil|not|or|repeat|return|then|true|until|while";
	private final static String GLOBAL_TARGET = "__add|__band|__bnot|__bor|__bxor|__call|__concat|__div|__eq|__idiv|__index|__le|__len|__lt|__mod|__mul|__newindex|__pow|__shl|__shr|__sub|__unm|_ENV|_G|assert|collectgarbage|dofile|error|findtable|getmetatable|ipairs|load|loadfile|loadstring|module|next|pairs|pcall|print|rawequal|rawget|rawlen|rawset|require|select|self|setmetatable|tointeger|tonumber|tostring|type|unpack|xpcall";

	private final static String PACKAGE_TARGET = "coroutine|debug|io|luajava|math|os|package|string|table|utf8";
	private final static String PACKAGE_COROUTINE = "create|isyieldable|resume|running|status|wrap|yield";
	private final static String PACKAGE_DEBUG = "debug|gethook|getinfo|getlocal|getmetatable|getregistry|getupvalue|getuservalue|sethook|setlocal|setmetatable|setupvalue|setuservalue|traceback|upvalueid|upvaluejoin";
	private final static String PACKAGE_IO = "close|flush|input|lines|open|output|popen|read|stderr|stdin|stdout|tmpfile|type|write";
	private final static String PACKAGE_MATH = "abs|acos|asin|atan|atan2|ceil|cos|cosh|deg|exp|floor|fmod|frexp|huge|ldexp|log|log10|max|maxinteger|min|mininteger|modf|pi|pow|rad|random|randomseed|sin|sinh|sqrt|tan|tanh|tointeger|type|ult";
	private final static String PACKAGE_OS = "clock|date|difftime|execute|exit|getenv|remove|rename|setlocale|time|tmpname";
	private final static String PACKAGE_PACKAGE = "config|cpath|loaded|loaders|loadlib|path|preload|searchers|searchpath|seeall";
	private final static String PACKAGE_STRING = "byte|char|dump|find|format|gfind|gmatch|gsub|len|lower|match|pack|packsize|rep|reverse|sub|unpack|upper";
	private final static String PACKAGE_TABLE = "concat|foreach|foreachi|insert|maxn|move|pack|remove|sort|unpack";
	private final static String PACKAGE_UTF8 = "char|charpattern|codepoint|codes|len|offset";
	private final static String PACKAGE_LUAJAVA = "astable|bindClass|clear|coding|createArray|createProxy|instanceof|loadLib|loaded|luapath|new|newInstance|package|tostring";
	private final static String EXT_FUNCTION_TARGET = "this|activity|call|compile|dump|each|enum|import|loadbitmap|loadlayout|loadmenu|service|set|task|thread|timer";

	private final static String FUNCTION_TARGET = GLOBAL_TARGET + "|" + EXT_FUNCTION_TARGET + "|" + PACKAGE_TARGET;

	private final static String[] KEYWORDS = KEYWORD_TARGET.split("\\|");
	private final static String[] NAMES = FUNCTION_TARGET.split("\\|");

	private final static String[] LUA_OPERATORS = {
			"(", ")", "{", "}", ",", ";", "=", "+", "-",
			"/", "*", "&", "!", "|", ":", "[", "]", "<", ">",
			"?", "~", "%", "^"
	};

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
		symbolList.clear();
		Collections.addAll(symbolList, sym);
	}
	private LuaLanguage() {
		setOperators(LUA_OPERATORS);
		setKeywords(KEYWORDS);
		setInternalFuncs(NAMES);
		addBasePackage("io", PACKAGE_IO.split("\\|"));
		addBasePackage("string", PACKAGE_STRING.split("\\|"));
		addBasePackage("luajava", PACKAGE_LUAJAVA.split("\\|"));
		addBasePackage("os", PACKAGE_OS.split("\\|"));
		addBasePackage("table", PACKAGE_TABLE.split("\\|"));
		addBasePackage("math", PACKAGE_MATH.split("\\|"));
		addBasePackage("utf8", PACKAGE_UTF8.split("\\|"));
		addBasePackage("coroutine", PACKAGE_COROUTINE.split("\\|"));
		addBasePackage("package", PACKAGE_PACKAGE.split("\\|"));
		addBasePackage("debug", PACKAGE_DEBUG.split("\\|"));
	}


	@Override
	public String getLineNoteStringStart() {
		return "--";
	}

	@Override
	public String getLineNoteStringEnd() {
		return "";
	}

	@Override
	public String getRegionNoteStringStart() {
		return "--[[";
	}

	@Override
	public String getRegionNoteStringMiddle() {
		return "";
	}

	@Override
	public String getRegionNoteStringEnd() {
		return "]]";
	}

	@Override
	public String getRegionDocNoteStringStart() {
		return "--";
	}

	@Override
	public String getRegionDocNoteStringMiddle() {
		return "--";
	}

	@Override
	public String getRegionDocNoteStringEnd() {
		return "";
	}

	@Override
	public LexTask  newLexTask() {
		return new LuaLexTask(this);
	}

}
