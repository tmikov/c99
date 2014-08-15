package c99.parser;

import c99.CompilerOptions;
import c99.IErrorReporter;
import c99.ISourceRange;

public class BaseActions
{
protected CompilerOptions m_opts;
protected IErrorReporter m_reporter;
protected SymTable m_symTab;

protected void init ( CompilerOptions opts, IErrorReporter reporter, SymTable symTab )
{
  m_opts = opts;
  m_reporter = reporter;
  m_symTab = symTab;
}

public final void error ( CParser.Location loc, String msg, Object... args )
{
  m_reporter.error( BisonLexer.fromLocation( loc ), msg, args );
}

public final void error ( ISourceRange rng, String msg, Object... args )
{
  m_reporter.error( rng, msg, args );
}

public final void warning ( CParser.Location loc, String msg, Object... args )
{
  m_reporter.warning( BisonLexer.fromLocation( loc ), msg, args );
}

public final void warning ( ISourceRange rng, String msg, Object... args )
{
  m_reporter.warning( rng, msg, args );
}

public final void pedWarning ( CParser.Location loc, String msg, Object... args )
{
  warning( loc, msg, args );
}

} // class

