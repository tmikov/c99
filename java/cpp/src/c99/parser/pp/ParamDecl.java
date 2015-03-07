package c99.parser.pp;

final class ParamDecl
{
private final Object prevPPDecl;
public final PPSymbol symbol;
public final int index;
public boolean variadic;

ParamDecl ( final PPSymbol symbol, int index, boolean variadic )
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
