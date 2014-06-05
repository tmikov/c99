package c99;

import java.io.FileInputStream;

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
      if (!tok.fileName1.equals( lastFile ))
      {
        System.out.format(  "\n# %d %s\n", tok.getLine1(), tok.fileName1 );
        lastLine = tok.line1;
      }
      else if (tok.line1 != lastLine)
      {
        if (tok.line1 - lastLine <= 10)
        {
          do
            System.out.println();
          while (++lastLine < tok.line1);
        }
        else
          System.out.format( "\n# %d\n", tok.line1 );
        lastLine = tok.line1;
      }

      lastFile = tok.fileName1;

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
