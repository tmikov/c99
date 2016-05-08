package c99.driver;

import c99.CompilerOptions;
import c99.SimpleErrorReporter;
import c99.parser.pp.SearchPathFactory;

import java.io.PrintWriter;
import java.util.Arrays;

public class ParserMain
{
public static void main ( String[] args )
{
  if ("--cpp".equals( args[0] ))
  {
    PreprMain.main(Arrays.copyOfRange(args, 1, args.length));
    return;
  }

  try
  {
    String fileName = null;

    CompilerOptions opts = new CompilerOptions();
    SearchPathFactory incSearch = new SearchPathFactory();
    int debugLevel = 0;

    for ( int i = 0; i < args.length; ++i )
    {
      final String arg = args[i];

      if ("--nostdinc".equals( arg ))
        opts.getPreprOptions().setNoStdInc( true );
      if ("--debug".equals( arg ))
        debugLevel = 1;
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
      else if (arg.startsWith("-"))
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

    Parser p = new Parser(opts, incSearch, new SimpleErrorReporter(), System.out, debugLevel);
    p.run(fileName);
  }
  catch (Exception e)
  {
    e.printStackTrace();
  }
}
}
