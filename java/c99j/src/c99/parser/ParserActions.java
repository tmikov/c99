package c99.parser;

import c99.IErrorReporter;

import java.util.Arrays;
import java.util.Comparator;

public class ParserActions extends AstActions
{
private IErrorReporter m_reporter;
private SymTable m_symTab;

protected void init ( IErrorReporter reporter, SymTable symTab )
{
  m_reporter = reporter;
  m_symTab = symTab;
}

private static boolean isStorageClass ( Code code )
{
  switch (code)
  {
    case TYPEDEF:
    case EXTERN:
    case STATIC:
    case _THREAD_LOCAL:
    case AUTO:
    case REGISTER:
      return true;
    default:
      return false;
  }
}
private static boolean isStorageClass ( Tree t )
{
  return (t.value instanceof Code) && isStorageClass((Code) t.value);
}

private static final Comparator<Tree> s_specComp = new Comparator<Tree>()
{
  @Override
  public int compare ( final Tree o1, final Tree o2 )
  {
    int r1 = isStorageClass(o1) ? 0 : 1;
    int r2 = isStorageClass(o2) ? 0 : 1;
    return r1 - r2;
  }
};
private Tree sort ( Tree declspecs )
{
  final int chCount = declspecs.childCount();
  Tree[] children = new Tree[chCount];
  for ( int i = 0; i < chCount; ++i )
    children[i] = declspecs.child( i );

  Arrays.sort( children, s_specComp );
  return new TreeN(declspecs.name, children).value(declspecs.value);
}

public final Tree specifyDecl ( Tree decl, Tree specs )
{
  specs = sort( specs );
  return seqAppend( decl, specs );
}

public final Tree declare ( Tree decl, Tree specs )
{
  specs = sort( specs );
  Tree c0 = specs.child(0);
  Code sc;
  if (isStorageClass(c0))
    sc = (Code)c0.value;
  else
    sc = null;

  if ("direct-declarator".equals(decl.name))
  {
    Symbol sym = (Symbol)decl.child(0).value;
    // FIXME: Redeclarations are valid at global scope, etc
    if (sym.topDecl != null && sym.topDecl.scope == m_topScope)
    {
      m_reporter.error( decl.child(0), "redeclaration of symbol '%s'", sym.name );
    }
    else
      m_topScope.pushDecl( new Decl(sc, sym, m_topScope) );
  }

/*
  System.out.println( sc );
  print( decl );
  print( specs );
*/
  return seqAppend( decl, specs );
}

private Scope m_topScope;

public Scope topScope ()
{
  return m_topScope;
}

public void popScope ( Scope scope )
{
  assert m_topScope == scope;
  m_topScope.popDecls();
  m_topScope = m_topScope.getParent();
}

public Scope pushScope ()
{
  return m_topScope = new Scope( m_topScope );
}

} // class
