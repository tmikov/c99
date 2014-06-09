package c99.parser;

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
PRAGMA("pragma"),
VA_ARGS("__VA_ARGS__");

public final String name;

private PPSymCode ( String name )
{
  this.name = name;
}

}
