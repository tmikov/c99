package c99;

public class DummyErrorReporter implements IErrorReporter
{

public String formatRange ( ISourceRange rng )
{
  return SourceRange.formatRange( rng );
}

private void print ( String severity, final ISourceRange rng, final String format, final Object... args )
{
  if (rng != null)
    System.err.format( "%s: %s: ", formatRange( rng ), severity );
  else
    System.err.format( "%s: ", severity );

  System.err.format( format, args );
  System.err.println();
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
