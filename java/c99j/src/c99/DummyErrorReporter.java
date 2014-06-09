package c99;

public class DummyErrorReporter implements IErrorReporter
{
private boolean isRealRange ( ISourceRange rng )
{
  return rng != null &&
         (rng.getLine2() > rng.getLine1() ||
          rng.getLine2() == rng.getLine1() && rng.getCol2() > rng.getCol1() + 1);
}

public String formatRange ( ISourceRange rng )
{
  if (rng == null)
    return "";

  if (isRealRange( rng ))
  {
    if (rng.getLine1() != rng.getLine2())
    {
      return String.format( "%s(%d)[%d]..(%d)[%d]",
                            Utils.defaultString( rng.getFileName() ),
                            rng.getLine1(), rng.getCol1(),
                            rng.getLine2(), rng.getCol2()
      );
    }
    else
    {
      return String.format( "%s(%d)[%d..%d])",
                            Utils.defaultString( rng.getFileName() ),
                            rng.getLine1(), rng.getCol1(), rng.getCol2()
      );
    }
  }
  else
  {
    return String.format( "%s(%d)[%d]",
      Utils.defaultString( rng.getFileName() ),
      rng.getLine1(), rng.getCol1()
    );
  }
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
