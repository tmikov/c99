package c99.driver;

import java.io.FileInputStream;

import c99.CompilerOptions;
import c99.DummyErrorReporter;
import c99.parser.Code;
import c99.parser.SymTable;
import c99.parser.pp.Misc;
import c99.parser.pp.PPLexer;
import c99.parser.pp.Prepr;
import c99.parser.pp.SearchPathFactory;

public class Preprocessor
{

private static boolean needWS ( Code t1, Code t2 )
{
  switch (t1)
  {
  case IDENT:
  case INT_NUMBER:
  case REAL_NUMBER:
    return t2 == Code.IDENT || t2 == Code.INT_NUMBER || t2 == Code.REAL_NUMBER;

  case NEWLINE:
    return t2 == Code.HASH;

  case HASH:
    return t2 == Code.HASH;

  case PLUS:
    return t2 == Code.EQUALS || t2 == Code.PLUS;
  case MINUS:
    return t2 == Code.EQUALS || t2 == Code.MINUS || t2 == Code.GREATER;
  case ASTERISK:
    return t2 == Code.EQUALS || t2 == Code.SLASH;
  case SLASH:
    return t2 == Code.EQUALS || t2 == Code.SLASH || t2 == Code.ASTERISK;
  case PERCENT:
  case CARET:
  case BANG:
  case EQUALS:
    return t2 == Code.EQUALS;
  case AMPERSAND:
    return t2 == Code.EQUALS || t2 == Code.AMPERSAND;
  case VERTICAL:
    return t2 == Code.EQUALS || t2 == Code.VERTICAL;

  case GREATER:
    return t2 == Code.GREATER || t2 == Code.EQUALS || t2 == Code.GREATER_EQUALS;
  case LESS:
    return t2 == Code.LESS || t2 == Code.EQUALS || t2 == Code.LESS_EQUALS;
  case GREATER_GREATER:
    return t2 == Code.EQUALS;
  case LESS_LESS:
    return t2 == Code.EQUALS;
  }
  return false;
}

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
    Code lastTok = Code.NEWLINE;
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
          System.out.format(  "# %d %s\n", tok.getLine1(), Misc.simpleEscapeString(
                  tok.getFileName() ));
          nl = true;
          lastTok = Code.NEWLINE;
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
          }
          while (++lastLine < tok.getLine1());
          nl = true;
          lastTok = Code.NEWLINE;
        }
        else
        {
          if (cpp)
          {
            if (!nl)
              System.out.println();
            System.out.format( "# %d %s\n", tok.getLine1(), Misc.simpleEscapeString(tok.getFileName()));
            nl = true;
            lastTok = Code.NEWLINE;
          }
        }
        lastLine = tok.getLine1();
      }

      lastFile = tok.getFileName();

      if (cpp)
      {
        if (tok.code() != Code.NEWLINE)
        {
          if (nl)
          {
            for ( int i = 1, col = tok.getCol1(); i < col; ++i )
            {
              System.out.print( ' ' );
              lastTok = Code.WHITESPACE;
            }
          }
          if (needWS( lastTok, tok.code() ))
            System.out.print( ' ' );
          lastTok = tok.code();
          tok.output( System.out );
          nl = false;
        }
      }
      if (toks) System.out.println( tok );
    }
    while (tok.code() != Code.EOF);
  }
  catch (Exception e)
  {
    e.printStackTrace();
  }
}
} // class
