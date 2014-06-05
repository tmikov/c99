package c99;

public class DummyErrorReporter implements IErrorReporter
{
private boolean isRealRange ( ISourceRange rng )
{
  return rng != null &&
         (!Utils.equals( rng.getFileName1(), rng.getFileName2() ) ||
          rng.getLine2() > rng.getLine1() ||
          rng.getLine2() == rng.getLine1() && rng.getCol2() > rng.getCol1() + 1);
}

private void print ( String severity, final ISourceRange rng, final String format, final Object... args )
{
  if (rng != null)
  {
    if (isRealRange( rng ))
    {
      if (Utils.equals( rng.getFileName1(), rng.getFileName2() ))
      {
        System.err.format( "%s(%d:%d..%d:%d): %s: ",
          Utils.defaultString( rng.getFileName1() ),
          rng.getLine1(), rng.getCol1(),
          rng.getLine2(), rng.getCol2(),
          severity
        );
      }
      else
      {
        System.err.format( "%s(%d:%d)..%s(%d:%d): %s: ",
          Utils.defaultString( rng.getFileName1() ),
          rng.getLine1(), rng.getCol1(),
          Utils.defaultString( rng.getFileName2() ),
          rng.getLine2(), rng.getCol2(),
          severity
        );
      }
    }
    else
    {
      System.err.format( "%s(%d:%d): %s: ",
        Utils.defaultString( rng.getFileName1() ),
        rng.getLine1(), rng.getCol1(),
        severity
      );
    }
  }

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
