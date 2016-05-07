package c99;

import c99.parser.pp.Misc;

public abstract class AnyStringConst
{
public final TypeSpec spec;

public AnyStringConst ( TypeSpec spec )
{
  this.spec = spec;
}

public abstract int length();
/** Return the character at that index or zero if beyond the length (for ASCIIZ) */
public abstract long at ( int index );
public abstract int[] wideValue ();
public abstract AnyStringConst resize ( int toLength );

public abstract String toJavaString ();

public final String toString ()
{
  return "{" + spec + ":" + Misc.simpleEscapeString(toJavaString()) + "}";
}
}

