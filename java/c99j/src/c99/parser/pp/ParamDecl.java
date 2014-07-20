package c99.parser.pp;

import c99.parser.Symbol;

final class ParamDecl
{
private final Object prevPPDecl;
public final Symbol symbol;
public final int index;
public boolean variadic;

ParamDecl ( final Symbol symbol, int index, boolean variadic )
{
  this.prevPPDecl = symbol.ppDecl;
  this.symbol = symbol;
  this.index = index;
  this.variadic = variadic;

  assert !(symbol.ppDecl instanceof ParamDecl);
  symbol.ppDecl = this;
}

public final boolean same ( ParamDecl p )
{
  return this.symbol == p.symbol && this.index == p.index;
}

void cleanUp ()
{
  assert symbol.ppDecl == this;
  symbol.ppDecl = prevPPDecl;
}
}
