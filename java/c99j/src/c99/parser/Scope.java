package c99.parser;

import java.util.LinkedList;

public class Scope
{
private final Scope m_parent;
private final LinkedList<Decl> m_decls = new LinkedList<Decl>();

public Scope ( final Scope parent )
{
  m_parent = parent;
}

public Scope getParent ()
{
  return m_parent;
}

public void pushDecl ( Decl decl )
{
  m_decls.add( decl );

  assert decl.prevDecl == null;
  assert decl.symbol.topDecl == null || decl.symbol.topDecl.scope != decl.scope;

  decl.prevDecl = decl.symbol.topDecl;
  decl.symbol.topDecl = decl;
}

public void popDecls ()
{
  for ( Decl d : m_decls )
  {
    assert d.symbol.topDecl == d;
    d.symbol.topDecl = d.prevDecl;
    d.prevDecl = null;
  }
}

}
