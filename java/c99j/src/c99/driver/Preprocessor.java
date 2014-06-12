package c99.driver;

import java.io.FileInputStream;

import c99.DummyErrorReporter;
import c99.ISourceRange;
import c99.parser.SymTable;
import c99.parser.pp.PPLexer;
import c99.parser.pp.Prepr;

public class Preprocessor
{
public static void main ( String[] args )
{
  try
  {
    DummyErrorReporter reporter = new DummyErrorReporter();
    SymTable symTable = new SymTable();
    Prepr pp = new Prepr( reporter, args[0], new FileInputStream( args[0] ), symTable );

    String lastFile = "";
    int lastLine = -1;
    PPLexer.Token tok;
    while ((tok = pp.nextToken()).code() != PPLexer.Code.EOF)
    {
      if (!tok.getFileName().equals( lastFile ))
      {
        System.out.format(  "\n# %d %s\n", tok.getLine1(), tok.getFileName() );
        lastLine = tok.getLine1();
      }
      else if (tok.getLine1() != lastLine)
      {
        if (tok.getLine1() - lastLine <= 10)
        {
          do
            System.out.println();
          while (++lastLine < tok.getLine1());
        }
        else
          System.out.format( "\n# %d\n", tok.getLine1() );
        lastLine = tok.getLine1();
      }

      lastFile = tok.getFileName();

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
