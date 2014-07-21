package c99.driver;

import java.io.FileInputStream;

import c99.CompilerOptions;
import c99.DummyErrorReporter;
import c99.parser.SymTable;
import c99.parser.pp.PPDefs;
import c99.parser.pp.PPLexer;
import c99.parser.pp.Prepr;
import c99.parser.pp.SearchPathFactory;

public class Preprocessor
{
public static void main ( String[] args )
{
  try
  {
    boolean cpp = true;
    boolean toks = false;
    String fileName = null;

    CompilerOptions opts = new CompilerOptions();
    SearchPathFactory incSearch = new SearchPathFactory();

    for ( int i = 0; i < args.length; ++i )
    {
      final String arg = args[i];

      if ("--toks".equals(arg))
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
      else if ("--nostdinc".equals( arg ))
        opts.noStdInc = true;
      else if (arg.startsWith("-I") || arg.startsWith("-i"))
      {
        String tmp = arg.substring( 2 );
        if (tmp.length() == 0)
        {
          System.err.println( "**fatal: missing argument for " + arg );
          System.exit(1);
        }
        if (arg.startsWith("-I"))
          incSearch.addInclude( tmp );
        else
          incSearch.addQuotedInclude(tmp);
      }
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
    Prepr pp = new Prepr( opts, reporter, incSearch.finish( opts ),
                          fileName, new FileInputStream( fileName ), symTable );

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
