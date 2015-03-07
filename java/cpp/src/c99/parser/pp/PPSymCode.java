package c99.parser.pp;

public enum PPSymCode
{
DEFINE("define"),
INCLUDE("include"),
LINE("line"),
IF("if"),
IFDEF("ifdef"),
IFNDEF("ifndef"),
ELSE("else"),
ELIF("elif"),
ENDIF("endif"),
DEFINED("defined"),
UNDEF("undef"),
ERROR("error"),
PRAGMA("pragma"),
VA_ARGS("__VA_ARGS__");

public final String name;

PPSymCode ( String name )
{
  this.name = name;
}
}
