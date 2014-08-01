package c99.parser;

public enum Code
{
EOF,
IDENT,
TYPENAME,
INT_NUMBER,
REAL_NUMBER,
CHAR_CONST,
WIDE_CHAR_CONST, // u,U,L
STRING_CONST,
WIDE_STRING_CONST, // u8, u, U, L

L_BRACKET("["), R_BRACKET("]"),
L_PAREN("("), R_PAREN(")"),
L_CURLY("{"), R_CURLY("}"),
FULLSTOP("."),
MINUS_GREATER("->"),

PLUS_PLUS("++"),
MINUS_MINUS("--"),
AMPERSAND("&"),
ASTERISK("*"),
PLUS("+"),
MINUS("-"),
TILDE("~"),
BANG("!"),

SLASH("/"),
PERCENT("%"),
LESS_LESS("<<"),
GREATER_GREATER(">>"),
LESS("<"),
GREATER(">"),
LESS_EQUALS("<="),
GREATER_EQUALS(">="),
EQUALS_EQUALS("=="),
BANG_EQUALS("!="),
CARET("^"),
VERTICAL("|"),
AMPERSAND_AMPERSAND("&&"),
VERTICAL_VERTICAL("||"),

QUESTION("?"),
COLON(":"),
SEMICOLON(";"),
ELLIPSIS("..."),

EQUALS("="),
ASTERISK_EQUALS("*="),
SLASH_EQUALS("/="),
PERCENT_EQUALS("%="),
PLUS_EQUALS("+="),
MINUS_EQUALS("-="),
LESS_LESS_EQUALS("<<="),
GREATER_GREATER_EQUALS(">>="),
AMPERSAND_EQUALS("&="),
CARET_EQUALS("^="),
VERTICAL_EQUALS("|="),

COMMA(","),

// Parser-only
AUTO("auto"),
BREAK("break"),
CASE("case"),
CHAR("char"),
CONST("const"),
CONTINUE("continue"),
DEFAULT("default"),
DO("do"),
DOUBLE("double"),
ELSE("else"),
ENUM("enum"),
EXTERN("extern"),
FLOAT("float"),
FOR("for"),
GOTO("goto"),
IF("if"),
INLINE("inline"),
INT("int"),
LONG("long"),
REGISTER("register"),
RESTRICT("restrict"),
RETURN("return"),
SHORT("short"),
SIGNED("signed"),
SIZEOF("sizeof"),
STATIC("static"),
STRUCT("struct"),
SWITCH("switch"),
TYPEDEF("typedef"),
UNION("union"),
UNSIGNED("unsigned"),
VOID("void"),
VOLATILE("volatile"),
WHILE("while"),
_ALIGNAS("_Alignas"),
_ALIGNOF("_Alignof"),
_ATOMIC("_Atomic"),
_BOOL("_Bool"),
_COMPLEX("_Complex"),
_GENERIC("_Generic"),
_IMAGINARY("_Imaginary"),
_NORETURN("_Noreturn"),
_STATIC_ASSERT("_Static_assert"),
_THREAD_LOCAL("_Thread_local"),

// Preprocessor-only
HASH("#"),
HASH_HASH("##"),

OTHER,

WHITESPACE(" "),
NEWLINE,
ANGLED_INCLUDE,

/** Macro parameter reference*/
MACRO_PARAM,
/** '##' token */
CONCAT;

public final byte[] printable;
public final String str;

Code ()
{
  str = "";
  printable = new byte[0];
}

Code ( String str )
{
  this.str = str;
  this.printable = str.getBytes();
}
}
