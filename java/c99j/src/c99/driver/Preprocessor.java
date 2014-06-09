package c99.driver;

import java.io.FileInputStream;

import c99.DummyErrorReporter;
import c99.ISourceRange;
import c99.parser.PP;

public class Preprocessor
{
public static void main ( String[] args )
{
  try
  {
    DummyErrorReporter reporter = new DummyErrorReporter();
    PP pp = new PP( reporter, args[0], new FileInputStream( args[0] ) );

    String lastFile = "";
    int lastLine = -1;
    PP.Token tok;
    while ((tok = pp.nextToken()).code != PP.TokenCode.EOF)
    {
      ISourceRange rng = pp.lastSourceRange();

      if (!rng.getFileName().equals( lastFile ))
      {
        System.out.format(  "\n# %d %s\n", rng.getLine1(), rng.getFileName() );
        lastLine = rng.getLine1();
      }
      else if (rng.getLine1() != lastLine)
      {
        if (rng.getLine1() - lastLine <= 10)
        {
          do
            System.out.println();
          while (++lastLine < rng.getLine1());
        }
        else
          System.out.format( "\n# %d\n", rng.getLine1() );
        lastLine = rng.getLine1();
      }

      lastFile = rng.getFileName();

      tok.output( System.out );
      if (false)
        System.err.println( tok );
    }
  }
  catch (Exception e)
  {
    e.printStackTrace();
  }
}
} // class
