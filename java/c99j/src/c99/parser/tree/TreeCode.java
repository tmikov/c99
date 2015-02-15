package c99.parser.tree;

public enum TreeCode
{
ERROR("<error>"),
VARREF("<identifier>"),
ENUM_CONST("<enum constant>"),
CONSTANT("<constant>"),
STRING("<string>"),
SUBSCRIPT("[]"),
CALL("<function call>"),
DOT_MEMBER("."),
PTR_MEMBER("->"),
POST_INC("()++"),
POST_DEC("()--"),
ADDRESS("&"),
INDIRECT("*"),
U_PLUS("+"),
U_MINUS("-"),
BITWISE_NOT("~"),
LOG_NEG("!"),
PRE_INC("++()"),
PRE_DEC("--()"),
SIZEOF_EXPR("sizeof"),
ALIGNOF_EXPR("_Alignof"),
SIZEOF_TYPE("sizeof"),
ALIGNOF_TYPE("_Alignof"),
LABEL_ADDRESS("&&label"),
TYPECAST("<typecast>"),
IMPLICIT_CAST("<implicit cast>"),
IMPLICIT_LOAD("<implicit load>"),
MUL("*"),
DIV("/"),
REMAINDER("%"),
ADD("+"),
SUB("-"),
LSHIFT("<<"),
RSHIFT(">>"),
LT("<"),
GT(">"),
LE("<="),
GE(">="),
EQ("=="),
NE("!="),
BITWISE_AND("&"),
BITWISE_XOR("^"),
BITWISE_OR("|"),
LOG_AND("&&"),
LOG_OR("||"),
ASSIGN("="),
ASSIGN_MUL("*="),
ASSIGN_DIV("/="),
ASSIGN_REM("%="),
ASSIGN_ADD("+="),
ASSIGN_SUB("-="),
ASSIGN_LSHIFT("<<="),
ASSIGN_RSHIFT(">>="),
ASSIGN_BITWISE_AND("&="),
ASSIGN_BITWISE_XOR("^="),
ASSIGN_BITWISE_OR("|="),
COMMA(","),
TERNARY("?:"),
;

public final String str;

private TreeCode ( String str )
{
  this.str = str;
}
}
