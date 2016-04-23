package c99;

import java.io.PrintWriter;

public class SimpleErrorReporter implements IErrorReporter
{
private final PrintWriter m_err;

public SimpleErrorReporter (PrintWriter err)
{
  m_err = err;
}

public SimpleErrorReporter ()
{
  this(new PrintWriter(System.err, true));
}

public String formatRange ( ISourceRange rng )
{
  return SourceRange.formatRange( rng );
}

private void print ( String severity, final ISourceRange rng, final String format, final Object... args )
{
  if (rng != null)
    m_err.format( "%s: %s: ", formatRange( rng ), severity );
  else
    m_err.format( "%s: ", severity );

  m_err.format( format, args );
  m_err.println();
}

@Override
public void warning ( final ISourceRange rng, final String format, final Object... args )
{
  print( "warning", rng,  format, args );
}

@Override
public void error ( final ISourceRange rng, final String format, final Object... args )
{
  print( "error", rng,  format, args );
}
} // class
