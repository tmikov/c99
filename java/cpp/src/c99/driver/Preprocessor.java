package c99.driver;

import java.io.*;

import c99.SimpleErrorReporter;
import c99.parser.Code;
import c99.parser.pp.*;

public class Preprocessor
{
private final PreprOptions m_opts;
private final SearchPathFactory m_incSearch;
private final SimpleErrorReporter m_reporter;
private final PrintStream m_out;

public Preprocessor ( PreprOptions opts, SearchPathFactory incSearch, SimpleErrorReporter reporter, PrintStream out )
{
  m_opts = opts;
  m_incSearch = incSearch;
  m_reporter = reporter;
  m_out = out;
}

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

public void run (String fileName, boolean cpp, boolean toks) throws IOException
{
  PPSymTable symTable = new PPSymTable();
  // NOTE: use File.getAbsoluteFile() to make it possible to change the current directory
  // by setting the system property "user.dir". File.getAbsoluteFile() obeys that property.
  Prepr<PPSymbol> pp = new Prepr<PPSymbol>(m_opts, m_reporter, m_incSearch.finish(m_opts),
                        fileName, new FileInputStream( new File(fileName).getAbsoluteFile() ), symTable );

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
          m_out.println();
        m_out.format(  "# %d %s\n", tok.getLine1(), Misc.simpleEscapeString(
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
          if (cpp) m_out.println();
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
          m_out.format( "# %d %s\n", tok.getLine1(), Misc.simpleEscapeString(tok.getFileName()));
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
            m_out.print( ' ' );
            lastTok = Code.WHITESPACE;
          }
        }
        if (needWS( lastTok, tok.code() ))
          m_out.print( ' ' );
        lastTok = tok.code();
        tok.output( m_out );
        nl = false;
      }
    }
    if (toks) m_out.println( tok );
  }
  while (tok.code() != Code.EOF);
}
} // class
