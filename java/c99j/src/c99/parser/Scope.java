package c99.parser;

import java.util.LinkedList;
import java.util.List;

public class Scope
{
public static enum Kind { FILE, BLOCK, PARAM, AGGREGATE, ENUM }

public final Kind kind;
private final Scope m_parent;
private final LinkedList<Decl> m_decls = new LinkedList<Decl>();
private final LinkedList<Decl> m_tags = new LinkedList<Decl>();

private boolean m_error;

public Scope ( Kind kind, final Scope parent )
{
  this.kind = kind;
  m_parent = parent;
}

public final Scope getParent ()
{
  return m_parent;
}

public final void orError ( boolean err )
{
  m_error |= err;
}

public boolean isError ()
{
  return m_error;
}

public final void pushDecl ( Decl decl )
{
  orError( decl.error );
  m_decls.add( decl );

  assert decl.prev == null;
  if (decl.symbol != null)
  {
    assert decl.symbol.topDecl == null || decl.symbol.topDecl.scope != decl.scope;

    decl.prev = decl.symbol.topDecl;
    decl.symbol.topDecl = decl;
  }
}

public final void pushTag ( Decl decl )
{
  orError( decl.error );
  m_tags.add( decl );

  assert decl.prev == null;
  if (decl.symbol != null)
  {
    assert decl.symbol.topTag == null || decl.symbol.topTag.scope != decl.scope;

    decl.prev = decl.symbol.topTag;
    decl.symbol.topTag = decl;
  }
}

public final void pop ()
{
  for ( Decl d : m_decls )
    if (d.symbol != null)
    {
      assert d.symbol.topDecl == d;
      d.symbol.topDecl = d.prev;
      d.prev = null;
    }
  for ( Decl d : m_tags )
    if (d.symbol != null)
    {
      assert d.symbol.topTag == d;
      d.symbol.topTag = d.prev;
      d.prev = null;
    }
}

public final List<Decl> decls ()
{
  return m_decls;
}

}
