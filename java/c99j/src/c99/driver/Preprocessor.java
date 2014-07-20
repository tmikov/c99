package c99.driver;

import java.io.FileInputStream;

import c99.CompilerOptions;
import c99.DummyErrorReporter;
import c99.parser.SymTable;
import c99.parser.pp.PPDefs;
import c99.parser.pp.PPLexer;
import c99.parser.pp.Prepr;

public class Preprocessor
{
public static void main ( String[] args )
{
  try
  {
    boolean cpp = true;
    boolean toks = false;
    String fileName = null;

    for ( String arg : args )
    {
      if ("--toks".equals( arg ))
      {
        toks = true;
        cpp = false;
      }
      else if ("--no-toks".equals( arg ))
        toks = false;
      else if ("--cpp".equals( arg ))
        cpp = true;
      else if ("--no-cpp".equals( arg ))
        cpp = false;
      else if (arg.startsWith( "-"))
      {
        System.err.println( "**fatal: unknown command line option '"+arg +"'" );
        System.exit(1);
      }
      else
      {
        if (fileName != null)
        {
          System.err.println( "**fatal: More than one input filename specified" );
          System.exit(1);
        }
        fileName = arg;
      }
    }

    if (fileName == null)
    {
      System.err.println( "**fatal: No input filename specified" );
      System.exit(1);
    }

    DummyErrorReporter reporter = new DummyErrorReporter();
    SymTable symTable = new SymTable();
    CompilerOptions opts = new CompilerOptions();
    Prepr pp = new Prepr( opts, reporter, fileName, new FileInputStream( fileName ), symTable );

    String lastFile = "";
    int lastLine = -1;
    PPLexer.Token tok;
    boolean nl = true;
    do
    {
      tok = pp.nextToken();
      if (!tok.getFileName().equals( lastFile ))
      {
        if (cpp)
        {
          if (!nl)
            System.out.println();
          System.out.format(  "# %d %s\n", tok.getLine1(), Prepr.simpleEscapeString(tok.getFileName()));
          nl = true;
        }
        lastLine = tok.getLine1();
      }
      else if (tok.getLine1() != lastLine)
      {
        if (tok.getLine1() - lastLine <= 10)
        {
          do
          {
            if (cpp) System.out.println();
            nl = true;
          }
          while (++lastLine < tok.getLine1());
        }
        else
        {
          if (cpp)
          {
            if (!nl)
              System.out.println();
            System.out.format( "# %d %s\n", tok.getLine1(), Prepr.simpleEscapeString(tok.getFileName()));
            nl = true;
          }
        }
        lastLine = tok.getLine1();
      }

      lastFile = tok.getFileName();

      if (cpp)
      {
        if (tok.code() != PPDefs.Code.NEWLINE)
        {
          if (nl)
          {
            for ( int i = 1, col = tok.getCol1(); i < col; ++i )
              System.out.print( ' ' );
          }
          tok.output( System.out );
          nl = false;
        }
      }
      if (toks) System.out.println( tok );
    }
    while (tok.code() != PPDefs.Code.EOF);
  }
  catch (Exception e)
  {
    e.printStackTrace();
  }
}
} // class
