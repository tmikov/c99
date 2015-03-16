package c99.parser;

import c99.*;
import c99.Types.*;

public class BaseActions
{
protected CompEnv m_compEnv;
protected SymTable m_symTab;
protected Platform m_plat;

protected CompilerOptions m_opts;
protected IErrorReporter m_reporter;

private Types.SimpleSpec m_specs[];
private Qual m_stdQuals[];

protected Spec stdSpec ( TypeSpec ts )
{
  return m_specs[ts.ordinal() - TypeSpec.VOID.ordinal()];
}
protected Qual stdQual ( TypeSpec ts )
{
  return m_stdQuals[ts.ordinal() - TypeSpec.VOID.ordinal()];
}

private static final SimpleSpec s_errorSpec = new SimpleSpec( TypeSpec.ERROR, -1, 0 );
protected static final Qual s_errorQual = new Qual(s_errorSpec);

protected void init ( CompEnv compEnv, SymTable symTab )
{
  m_compEnv = compEnv;
  m_symTab = symTab;
  m_plat = new Platform( m_compEnv );

  m_opts = compEnv.opts;
  m_reporter = compEnv.reporter;

  // Initialize the basic type specs
  m_specs = new SimpleSpec[TypeSpec.LDOUBLE.ordinal() - TypeSpec.VOID.ordinal() + 1];
  m_stdQuals = new Qual[m_specs.length];
  for ( int i = TypeSpec.VOID.ordinal(); i <= TypeSpec.LDOUBLE.ordinal(); ++i )
  {
    final TypeSpec type = TypeSpec.values()[i];
    int size = -1, align = 0;
    if (type.sizeOf > 0)
    {
      size = type.sizeOf;
      align = m_plat.alignment( size );
    }
    int ind = i - TypeSpec.VOID.ordinal();
    m_specs[ind] = new SimpleSpec( type, size, align );
    m_stdQuals[ind] = new Qual( m_specs[ind] );
  }
}

public final void error ( ISourceRange rng, String msg, Object... args )
{
  m_reporter.error( rng, msg, args );
}

public final void warning ( ISourceRange rng, String msg, Object... args )
{
  m_reporter.warning( rng, msg, args );
}

public final void pedWarning ( ISourceRange loc, String msg, Object... args )
{
  warning( loc, msg, args );
}

public final void extWarning ( ISourceRange loc, String msg, Object... args )
{
  warning( loc, msg, args );
}

protected final PointerSpec newPointerSpec ( Qual to )
{
  int size = m_plat.pointerSize( to );
  int align = m_plat.alignment( size );
  return new PointerSpec( to, size, align );
}

/**
 *
 * @param loc
 * @param to
 * @param nelem < 0 means not specified
 * @return
 */
protected final ArraySpec newArraySpec ( ISourceRange loc, Qual to, long nelem )
{
  assert to.spec.isComplete() && to.spec.sizeOf() >= 0;

  if (nelem >= 0)
  {
    // Check for int64 overflow
    if (nelem != 0 && to.spec.sizeOf() > (Long.MAX_VALUE / nelem))
    {
      error( loc, "array size integer overflow" );
      return null;
    }
    long size = (nelem * to.spec.sizeOf()) & Long.MAX_VALUE; // note: convert to unsigned

    if (size > TypeSpec.SIZE_T.maxValue)
    {
      error( loc, "array size doesn't fit in size_t" );
      return null;
    }

    return new ArraySpec( to, nelem, size );
  }
  else
    return new ArraySpec( to );
}

public static String optName ( Ident ident )
{
  return ident != null ? ident.name : "<anonymous>";
}
} // class

