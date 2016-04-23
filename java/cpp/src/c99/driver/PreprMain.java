package c99.driver;

import c99.SimpleErrorReporter;
import c99.parser.pp.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class PreprMain
{
public static void main ( String[] args )
{
  try
  {
    boolean cpp = true;
    boolean toks = false;
    String fileName = null;

    PreprOptions opts = new PreprOptions();
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
        opts.setNoStdInc( true );
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
      else if (arg.startsWith("--date"))
      {
        String datestr = "";
        if (arg.startsWith("--date="))
          datestr = arg.substring(7);
        else if (i + 1 < args.length)
        {
          ++i;
          datestr = args[i];
        }
        else
        {
          System.err.println( "**fatal: missing argument for " + arg );
          System.exit(1);
        }

        try
        {
          opts.setForcedDate(new SimpleDateFormat("MMM ddd yyyy HH:mm:ss").parse(datestr));
        }
        catch (ParseException e)
        {
          System.err.println( "**fatal: invalid date '"+ datestr +"'" );
          System.exit(1);
        }
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

    SimpleErrorReporter reporter = new SimpleErrorReporter();

    new Preprocessor(opts, incSearch, reporter, System.out).run(fileName, cpp, toks);
  }
  catch (Exception e)
  {
    e.printStackTrace();
  }
}
}
