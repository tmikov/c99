package c99.parser;

import c99.IErrorReporter;
import c99.ISourceRange;
import c99.SourceRange;
import c99.parser.pp.PPDefs;
import c99.parser.pp.Prepr;

import java.io.IOException;

public final class BisonLexer implements CParser.Lexer
{
private final IErrorReporter m_reporter;
private final SymTable m_symTab;
private final Prepr m_prepr;

private Object m_yylval;
private final Position m_startPos = new Position();
private final Position m_endPos = new Position();

public BisonLexer ( final IErrorReporter reporter, final SymTable symTab, final Prepr prepr )
{
  m_reporter = reporter;
  m_symTab = symTab;
  m_prepr = prepr;

  initKeywords();
}

private final void initKeywords ()
{
  for ( int i = Code.AUTO.ordinal(); i <= Code._THREAD_LOCAL.ordinal(); ++i )
  {
    final Code c = Code.values()[i];
    m_symTab.symbol( c.str ).keyword = c;
  }
}

public final void close ()
{
  m_prepr.close();
}

@Override
public Position getStartPos ()
{
  return m_startPos;
}

@Override
public Position getEndPos ()
{
  return m_endPos;
}

@Override
public Object getLVal ()
{
  return m_yylval;
}

@Override
public int yylex () throws IOException
{
  for(;;)
  {
    PPDefs.Token ppt;
    do
      ppt = m_prepr.nextToken();
    while (ppt.code() == Code.NEWLINE || ppt.code() == Code.WHITESPACE);

    m_startPos.fileName = ppt.fileName;
    m_startPos.line = ppt.line1;
    m_startPos.col = ppt.col1;
    m_endPos.fileName = ppt.fileName2 != null ? ppt.fileName2 : ppt.fileName;
    m_endPos.line = ppt.line2;
    m_endPos.col = ppt.col2;

    Code code = ppt.code();
    //m_yylval = null;
    m_yylval = code;

    switch (code)
    {
      case IDENT:
        {
          Symbol sym = ppt.symbol();
          Code kw = sym.keyword;
          if (kw != null)
          {
            code = kw;
            m_yylval = code;
          }
          else
          {
            if (sym.topDecl != null && sym.topDecl.sclass == Code.TYPEDEF)
            {
              code = Code.TYPENAME;
              m_yylval = sym.topDecl;
            }
            else
              m_yylval = sym;
          }
        }
        break;

      case EOF:
        return CParser.Lexer.EOF;

      case CHAR_CONST:
        code = Code.INT_NUMBER;
        m_yylval = ppt.getCharConstValue();
        break;

      case INT_NUMBER:
        m_yylval = ppt.getIntConstValue();
        break;

      case REAL_NUMBER:
        m_yylval = ppt.getRealConst();
        break;

      case STRING_CONST:
        m_yylval = ppt.getStringConstValue();
        break;

      case WIDE_CHAR_CONST:
      case WIDE_STRING_CONST:
        m_reporter.error( ppt, "Wide characters not supported" );
        continue;

      default:
        int ord = ppt.code().ordinal();
        if (ord >= Code.HASH.ordinal())
        {
          m_reporter.error( ppt, "Unrecognized symbol '%s'", ppt.outputString() );
          continue;
        }
        break;
    }

    return code.ordinal() + 257;
  }
}

public static SourceRange setLocation ( SourceRange rng, CParser.Location loc )
{
  return rng.setRange( loc.begin.fileName, loc.begin.line, loc.begin.col,
                       loc.end.fileName, loc.end.line, loc.end.col );
}

public static SourceRange fromLocation ( CParser.Location loc )
{
  return setLocation(new SourceRange(), loc );
}

@Override
public void yyerror ( final CParser.Location loc, final String msg )
{
  m_reporter.error( fromLocation(loc), msg );
}

}
