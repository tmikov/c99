package c99.parser.pp;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import c99.IErrorReporter;
import c99.ISourceRange;
import c99.SourceRange;
import c99.parser.SymTable;
import c99.parser.Symbol;

public class Prepr extends PPLexer
{
private final Symbol m_sym_VA_ARGS;

public Prepr ( final IErrorReporter reporter, final String fileName, final InputStream input,
               final SymTable symTable )
{
  super( reporter, fileName, input, symTable );

  for ( PPSymCode ppCode : PPSymCode.values() )
  {
    Symbol sym = m_symTable.symbol( ppCode.name );
    assert sym.ppCode == null;
    sym.ppCode = ppCode;
  }

  m_sym_VA_ARGS = m_symTable.symbol( PPSymCode.VA_ARGS.name );
  assert m_sym_VA_ARGS.ppCode == PPSymCode.VA_ARGS;

}

private static final class Macro
{
  public final SourceRange nameLoc = new SourceRange();
  public final SourceRange bodyLoc = new SourceRange();
  public final Symbol name;
  public boolean funcLike;
  public boolean variadic;

  public final ArrayList<ParamDecl> params = new ArrayList<ParamDecl>();
  public final LinkedList<AbstractToken> body = new LinkedList<AbstractToken>();

  Macro ( final Symbol name, ISourceRange nameLoc )
  {
    this.name = name;
    this.nameLoc.setRange( nameLoc );
  }

  void cleanUpParamScope ()
  {
    for ( ParamDecl param : params )
      param.cleanUp();
  }

  boolean same ( Macro m )
  {
    if (this.name != m.name ||
        this.funcLike != m.funcLike ||
        this.params.size() != m.params.size() ||
        this.body.size() != m.body.size())
    {
      return false;
    }

    Iterator<ParamDecl> p1 = this.params.iterator();
    Iterator<ParamDecl> p2 = m.params.iterator();
    while (p1.hasNext())
      if (!p1.next().same( p2.next() ))
        return false;

    Iterator<AbstractToken> t1 = this.body.iterator();
    Iterator<AbstractToken> t2 = m.body.iterator();
    while (t1.hasNext())
      if (!t1.next().same( t2.next() ))
        return false;

    return true;
  }
}

private static final class ParamDecl
{
  private final Object prevPPDecl;
  public final Symbol symbol;
  public final int index;

  ParamDecl ( final Symbol symbol, int index )
  {
    this.prevPPDecl = symbol.ppDecl;
    this.symbol = symbol;
    this.index = index;

    assert !(symbol.ppDecl instanceof ParamDecl);
    symbol.ppDecl = this;
  }

  public final boolean same ( ParamDecl p )
  {
    return this.symbol == p.symbol && this.index == p.index;
  }

  void cleanUp ()
  {
    assert symbol.ppDecl == this;
    symbol.ppDecl = prevPPDecl;
  }
}

private static final class ParamToken extends AbstractToken
{
  public final ParamDecl param;
  public boolean stringify;

  private ParamToken ( final ParamDecl param )
  {
    m_code = Code.MACRO_PARAM;
    this.param = param;
  }

  @SuppressWarnings("CloneDoesntCallSuperClone")
  @Override
  public AbstractToken clone ()
  {
    return this;
  }

  @Override
  public boolean same ( final AbstractToken tok )
  {
    return this.m_code == tok.m_code && this.param.same( ((ParamToken)tok).param );
  }

  @Override
  public int length ()
  {
    return this.param.symbol.length();
  }

  @Override
  public void output ( final PrintStream out ) throws IOException
  {
    out.write( this.param.symbol.bytes );
  }
}

private static final class ConcatToken extends AbstractToken
{
  private AbstractToken left, right;

  public ConcatToken ( AbstractToken left, AbstractToken right )
  {
    m_code = Code.CONCAT;
    this.left = left;
    this.right = right;
  }

  @SuppressWarnings("CloneDoesntCallSuperClone")
  @Override
  public ConcatToken clone ()
  {
    return new ConcatToken( this.left.clone(), this.right.clone() );
  }

  @Override
  public boolean same ( final AbstractToken tok )
  {
    return this.m_code == tok.m_code &&
           left.same( ((ConcatToken)tok).left ) &&
           right.same( ((ConcatToken)tok).right );
  }

  @Override
  public int length ()
  {
    return left.length() + right.length();
  }

  @Override
  public String toString ()
  {
    return "ConcatToken{" +
           "left=" + left +
           ", right=" + right +
           '}';
  }

  @Override
  public void output ( final PrintStream out ) throws IOException
  {
    left.output( out );
    out.print( " ## " );
    right.output( out );
  }
}

/**
 * The whitespace skipped by some routines.
 *
 * If {@link #nextNoBlanks()} or {@link #nextNoNewLineOrBlanks()} or {@link #skipBlanks()}
 * skips over any whitespace, a single whitespace token is set here.
 */
private Token m_skippedWs;
/**
 * The token used to initialize {@link #m_skippedWs}
 */
private Token m__defaultWs = new Token( Code.WHITESPACE );

private final SourceRange m_tmpRange = new SourceRange();

private final void nextWithBlanks ()
{
  m_skippedWs = null;
  innerNextToken();
}

private final void nextNoBlanks ()
{
  m_skippedWs = null;
  innerNextToken();
  while (m_tok.code() == Code.WHITESPACE || m_tok.code() == Code.COMMENT)
  {
    m_skippedWs = m__defaultWs;
    innerNextToken();
  }
}

private final void nextNoNewLineOrBlanks ()
{
  m_skippedWs = null;
  innerNextToken();
  while (m_tok.code() == Code.WHITESPACE || m_tok.code() == Code.COMMENT || m_tok.code() == Code.NEWLINE)
  {
    m_skippedWs = m__defaultWs;
    innerNextToken();
  }
}

private final void skipBlanks ()
{
  m_skippedWs = null;
  while (m_tok.code() == Code.WHITESPACE || m_tok.code() == Code.COMMENT)
  {
    m_skippedWs = m__defaultWs;
    innerNextToken();
  }
}

private final void skipUntilEOL ()
{
  m_skippedWs = null;
  while (m_tok.code() != Code.NEWLINE && m_tok.code() != Code.EOF)
    innerNextToken();
}

private final boolean parseMacroParamList ( Macro macro )
{
  nextNoBlanks(); // consume the '('

  if (m_tok.code() != Code.R_PAREN)
  {
    for(;;)
    {
      if (m_tok.code() == Code.IDENT)
      {
        Symbol sym = m_tok.symbol();
        if (sym.ppDecl instanceof ParamDecl)
        {
          m_reporter.error( m_tokRange, "Duplicated macro parameter '%s'", sym.name );
          skipUntilEOL();
          return false;
        }

        macro.params.add( new ParamDecl( sym, macro.params.size() ) );

        nextNoBlanks();
        if (m_tok.code() == Code.R_PAREN)
          break;
        else if (m_tok.code() == Code.COMMA)
          nextNoBlanks();
/* TODO: GCC extension for variadic macros "macro(args...)"
        else if (m_tok.code() == Code.ELLIPSIS)
          {}
*/
        else
        {
          m_reporter.error(  m_tokRange, "Expected ',', ')', '...' or an identifier in macro parameter list" );
          skipUntilEOL();
          return false;
        }
      }
      else if (m_tok.code() == Code.ELLIPSIS)
      {
        macro.variadic = true;
        macro.params.add( new ParamDecl( m_sym_VA_ARGS, macro.params.size() ) );
        nextNoBlanks();

        if (m_tok.code() == Code.R_PAREN)
          break;
        else
        {
          m_reporter.error(  m_tokRange, "Expected ')' after '...' in macro parameter list" );
          skipUntilEOL();
          return false;
        }
      }
      else
      {
        if (m_tok.code() == Code.EOF || m_tok.code() == Code.NEWLINE)
          m_reporter.error( m_tokRange, "Missing closing ')' in macro parameter list" );
        else
          m_reporter.error( m_tokRange, "Macro parameter name expected" );
        skipUntilEOL();
        return false;
      }
    }
  }

  nextNoBlanks(); // Consume the ')'
  return true;
}

private static ParamDecl isParam ( Token tok )
{
  if (tok.code() == Code.IDENT)
  {
    Symbol sym = tok.symbol();
    if (sym.ppDecl instanceof ParamDecl)
      return (ParamDecl)sym.ppDecl;
  }

  return null;
}

private final AbstractToken parseMacroReplacementListToken ( Macro macro )
{
  AbstractToken tok;
  ParamDecl param;

  if (m_tok.code() == Code.HASH)
  {
    /* 6.10.3.2 (1) Each # preprocessing token in the replacement list for a function-like macro shall be
       followed by a parameter as the next preprocessing token in the replacement list. */
    nextNoBlanks();

    if ( (param = isParam( m_tok )) != null)
    {
      ParamToken paramToken = new ParamToken( param );
      paramToken.stringify = true;
      tok = paramToken;
    }
    else
    {
      m_reporter.error( m_tokRange, "'#' must be followed by a macro parameter" );
      skipUntilEOL();
      return null;
    }
  }
  else if ((param = isParam( m_tok )) != null)
    tok = new ParamToken( param );
  else if (m_tok.code() == Code.IDENT && m_tok.symbol() == m_sym_VA_ARGS)
  {
    assert !macro.variadic;
    m_reporter.error( m_tokRange, "'__VA_ARGS__' must only appear in a variadic macro" );
    skipUntilEOL();
    return null;
  }
  else
    tok = m_tok.clone();

  return tok;
}

private final boolean parseMacroReplacementList ( Macro macro )
{
  macro.bodyLoc.setRange( m_tokRange );
  m_skippedWs = null;

  for ( ; m_tok.code() != Code.EOF && m_tok.code() != Code.NEWLINE; nextNoBlanks() )
  {
    AbstractToken tok;

    if (m_tok.code() == Code.HASH_HASH)
    {
      /* 6.10.3.3 (1) A ## preprocessing token shall not occur at the beginning or at the end of
         a replacement list for either form of macro definition. */
      if (macro.body.size() == 0)
      {
        m_reporter.error( m_tokRange, "'##' can only occur between two tokens" );
        skipUntilEOL();
        return false;
      }

      do // skip consecutive '##'
      {
        m_tmpRange.setRange( m_tokRange ); // Save the location of the token
        nextNoBlanks();
        if (m_tok.code() == Code.EOF || m_tok.code() == Code.NEWLINE)
        {
          m_reporter.error( m_tmpRange, "'##' can only occur between two tokens" );
          return false;
        }
      }
      while (m_tok.code() == Code.HASH_HASH);

      if ( (tok = parseMacroReplacementListToken( macro )) == null)
        return false;

      tok = new ConcatToken( macro.body.removeLast(), tok );
    }
    else
    {
      if ((tok = parseMacroReplacementListToken( macro )) == null)
        return false;
    }

    if (m_skippedWs != null)
    {
      macro.body.add( m_skippedWs.clone() );
      m_skippedWs = null;
    }

    macro.body.addLast( tok );
    macro.bodyLoc.extend( m_tokRange );
  }

  return true;
}

private final void parseDefine ()
{
  nextNoBlanks(); // consume the 'define'

  if (m_tok.code() != Code.IDENT)
  {
    m_reporter.error( m_tokRange, "An identifier macro name expected" );
    skipUntilEOL();
    return;
  }

  final Symbol macroSym = m_tok.symbol();
  final Macro macro = new Macro( macroSym, m_tokRange );
  try
  {
    nextWithBlanks();
    if (m_tok.code() == Code.L_PAREN)
    {
      macro.funcLike = true;
      if (!parseMacroParamList( macro ))
        return;
    }
    else
    {
      skipBlanks();
      macro.funcLike = false;
    }

    if (!parseMacroReplacementList( macro ))
      return;
  }
  finally
  {
    macro.cleanUpParamScope();
  }

  if (macroSym.ppDecl instanceof Macro)
  {
    Macro prevMacro = (Macro)macroSym.ppDecl;
    if (!macro.same( prevMacro ))
    {
      m_reporter.warning(
        macro.nameLoc, "redefinition of macro '%s' differs from previous definition at %s",
        macro.name.name, m_reporter.formatRange( prevMacro.nameLoc )
      );
    }
  }

  macroSym.ppDecl = macro;
}

private final void parseDirective ()
{
  nextNoBlanks(); // consume the '#'

  switch (m_tok.code())
  {
  case NEWLINE:
    return;

  case IDENT:
    {
      Symbol sym = m_tok.symbol();
      if (sym.ppCode != null)
      {
        switch (sym.ppCode)
        {
        case DEFINE:
          parseDefine();
          return;
        }
      }
    }
    break;

  case PP_NUMBER:
    break;
  }
}

private Iterator<AbstractToken> m_expIt;
private final Token m_savedIdent = new Token( Code.IDENT );
private final SourceRange m_savedIdentRange = new SourceRange();
private final SourceRange m_savedRange = new SourceRange();
private Token m_savedTok;

private final void expand ( Macro macro, ArrayList<List<Token>> params )
{
  m_state = sEXPANDING;
  m_expIt = macro.body.iterator();
}

private final boolean possiblyExpandFuncMacro ( Macro macro )
{
  m_savedIdent.copyFrom( m_tok );
  m_savedIdentRange.setRange( m_tokRange );

  nextNoNewLineOrBlanks();

  // False alarm? It wasn't a macro, so we must return the original token and the optional
  // white-space.
  if (m_tok.code() != Code.L_PAREN)
  {
    m_savedRange.setRange( m_tokRange );
    m_savedTok = m_tok;

    m_tok = m_savedIdent;
    m_tokRange.setRange( m_savedIdentRange );

    m_state = sUNDO_IDENT;
    return false;
  }

  expand( macro, null );
  return true;
}

private final void expandObjectMacro ( Macro macro )
{
  expand( macro, null );
}

/**
 *
 * @param macro
 * @return true if macro expansion will proceed, so we must loop instead of returning a token
 */
private final boolean possiblyExpandMacro ( Macro macro )
{
  if (macro.funcLike)
    return possiblyExpandFuncMacro( macro );
  else
  {
    expandObjectMacro( macro );
    return true;
  }
}

private final int sNORMAL_NEXT = 0;
private final int sNORMAL_USE = 1;
private final int sLINEBEG = 2;
private final int sUNDO_IDENT = 3;
private final int sEXPANDING = 4;

private int m_state = sLINEBEG;

public final Token nextToken ()
{
  for(;;)
    switch (m_state)
    {
    case sNORMAL_NEXT:
      nextWithBlanks();
    case sNORMAL_USE:
      if (m_tok.code() == Code.IDENT && m_tok.symbol().ppDecl instanceof Macro)
      {
        if (possiblyExpandMacro( (Macro)(m_tok.symbol().ppDecl)))
          continue;
      }
      else if (m_tok.code() == Code.NEWLINE)
        m_state = sLINEBEG;
      else
        m_state = sNORMAL_NEXT;
      return m_tok;

    case sLINEBEG:
      nextNoBlanks();
      switch (m_tok.code())
      {
      case HASH:
        parseDirective();
        return m_tok;

      default:
        m_state = sNORMAL_USE;
        continue;

      case NEWLINE:
        return m_tok;
      }

    case sUNDO_IDENT:
      if (m_skippedWs != null)
      {
        m_tokRange.translate( m_skippedWs.length() );
        m_tok = m_skippedWs;
        m_skippedWs = null;
      }
      else
      {
        m_tokRange.setRange( m_savedRange );
        m_tok = m_savedTok;
        m_savedTok= null;
      }
      return m_tok;

    case sEXPANDING:
      if (m_expIt.hasNext())
      {
        m_tok = (Token)m_expIt.next();
        m_tokRange.setLength( m_tok.length() );
        return m_tok;
      }
      else
        m_state = sNORMAL_NEXT;
      break;
    }
}

} // class
