package c99.parser.pp;

import c99.ISourceRange;
import c99.SourceRange;

import java.util.ArrayList;
import java.util.Iterator;

final class Macro
{
public final SourceRange nameLoc = new SourceRange();
public final SourceRange bodyLoc = new SourceRange();
public final PPSymbol symbol;
public final Builtin builtin;
public boolean funcLike;
public boolean variadic;
public boolean expanding;

public final ArrayList<ParamDecl> params = new ArrayList<ParamDecl>();
public final TokenList<PPDefs.AbstractToken> body = new TokenList<PPDefs.AbstractToken>();

Macro ( final PPSymbol symbol, ISourceRange nameLoc, Builtin builtin )
{
  this.symbol = symbol;
  this.nameLoc.setRange( nameLoc );
  this.builtin = builtin;
}

final int paramCount ()
{
  return params.size();
}

void cleanUpParamScope ()
{
  for ( ParamDecl param : params )
    param.cleanUp();
}

boolean same ( Macro m )
{
  if (this.symbol != m.symbol ||
      this.funcLike != m.funcLike ||
      this.params.size() != m.params.size() ||
      this.body.size() != m.body.size())
  {
    return false;
  }

  Iterator<ParamDecl> p1 = this.params.iterator();
  Iterator<ParamDecl> p2 = m.params.iterator();
  while (p1.hasNext())
    if (!p1.next().same( p2.next() ))
      return false;

  Iterator<PPDefs.AbstractToken> t1 = this.body.iterator();
  Iterator<PPDefs.AbstractToken> t2 = m.body.iterator();
  while (t1.hasNext())
    if (!t1.next().same( t2.next() ))
      return false;

  return true;
}
}
