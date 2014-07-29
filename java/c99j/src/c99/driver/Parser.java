package c99.driver;

import c99.CompilerOptions;
import c99.DummyErrorReporter;
import c99.parser.BisonLexer;
import c99.parser.CParser;
import c99.parser.SymTable;
import c99.parser.pp.Prepr;
import c99.parser.pp.SearchPathFactory;

import java.io.FileInputStream;
import java.util.Arrays;

public class Parser
{
public static void main ( String[] args )
{
  if ("--cpp".equals( args[0] ))
  {
    Preprocessor.main(Arrays.copyOfRange(args, 1, args.length));
    return;
  }
  
  try
  {
    String fileName = null;

    CompilerOptions opts = new CompilerOptions();
    SearchPathFactory incSearch = new SearchPathFactory();

    for ( int i = 0; i < args.length; ++i )
    {
      final String arg = args[i];

      if ("--nostdinc".equals( arg ))
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

    DummyErrorReporter reporter = new DummyErrorReporter();
    SymTable symTable = new SymTable();
    Prepr pp = new Prepr( opts, reporter, incSearch.finish( opts ),
                          fileName, new FileInputStream( fileName ), symTable );
    BisonLexer lex = new BisonLexer(reporter, symTable, pp);
    CParser parser = new CParser(lex);
    parser.parse();
  }
  catch (Exception e)
  {
    e.printStackTrace();
  }
}
}
