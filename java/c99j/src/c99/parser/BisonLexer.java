package c99.parser;

import c99.IErrorReporter;
import c99.ISourceRange;
import c99.SourceRange;
import c99.Types;
import c99.parser.pp.PPDefs;
import c99.parser.pp.Prepr;

import java.io.IOException;

public final class BisonLexer implements CParser.Lexer
{
private final IErrorReporter m_reporter;
private final SymTable m_symTab;
private final Prepr<Symbol> m_prepr;

private Object m_yylval;
private Position m_startPos = new Position();
private Position m_endPos = new Position();

public BisonLexer ( final IErrorReporter reporter, final SymTable symTab, final Prepr<Symbol> prepr )
{
  m_reporter = reporter;
  m_symTab = symTab;
  m_prepr = prepr;

  initKeywords();
}

private final void initKeywords ()
{
  for ( int i = Code.FIRST_KW.ordinal(); i <= Code.LAST_KW.ordinal(); ++i )
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
    PPDefs.Token<Symbol> ppt;
    do
      ppt = m_prepr.nextToken();
    while (ppt.code() == Code.NEWLINE || ppt.code() == Code.WHITESPACE);

    m_startPos = new Position( ppt.fileName, ppt.line1, ppt.col1 );
    m_endPos = new Position( ppt.fileName2 != null ? ppt.fileName2 : ppt.fileName, ppt.line2, ppt.col2 );

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
            if (sym.topDecl != null && sym.topDecl.sclass == Types.SClass.TYPEDEF)
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

public static <T extends SourceRange> T setLocation ( T rng, CParser.Location loc )
{
  rng.setRange( loc.begin.fileName, loc.begin.line, loc.begin.col,
                loc.end.fileName, loc.end.line, loc.end.col );
  return rng;
}

public static SourceRange fromLocation ( CParser.Location loc )
{
  return setLocation( new SourceRange(), loc );
}

public static CParser.Location toLocation ( ISourceRange rng )
{
  return new CParser.Location(
    new Position( rng.getFileName(), rng.getLine1(), rng.getCol1() ),
    new Position( rng.getFileName2(), rng.getLine2(), rng.getCol2() )
  );
}

@Override
public void yyerror ( final CParser.Location loc, final String msg )
{
  m_reporter.error( fromLocation(loc), msg );
}

}
