package c99.parser.pp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;

import c99.CompilerOptions;
import c99.IErrorReporter;
import c99.ISourceRange;
import c99.OptResult;
import c99.SourceRange;
import c99.Utils;
import c99.parser.SymTable;
import c99.parser.Symbol;

public class Prepr extends PPLexer
{
private final CompilerOptions m_opts;
private final Symbol m_sym_VA_ARGS;

private static enum Builtin
{
  __LINE__,
  __FILE__,
  __DATE__
}

public Prepr ( final CompilerOptions opts, final IErrorReporter reporter,
               final String fileName, final InputStream input,
               final SymTable symTable )
{
  super( reporter, fileName, input, symTable );
  m_opts = opts;

  for ( PPSymCode ppCode : PPSymCode.values() )
  {
    Symbol sym = m_symTable.symbol( ppCode.name );
    assert sym.ppCode == null;
    sym.ppCode = ppCode;
  }

  m_sym_VA_ARGS = m_symTable.symbol( PPSymCode.VA_ARGS.name );
  assert m_sym_VA_ARGS.ppCode == PPSymCode.VA_ARGS;

  // Define built-in macros
  for ( Builtin builtin : Builtin.values() )
  {
    Symbol sym = m_symTable.symbol( builtin.name() );
    sym.ppDecl = new Macro( sym, new SourceRange(), builtin );
  }

  // Generate the date string which doesn't change duing compilation
  Macro dateMacro = (Macro) m_symTable.symbol( Builtin.__DATE__.name() ).ppDecl;
  String dateStr = '"' + new SimpleDateFormat( "MMM dd yyyy" ).format( new Date() ) + '"';
  Token tok = new Token();
  tok.setTextWithOnwership( Code.STRING_CONST, dateStr.getBytes() );
  dateMacro.body.addLast( tok );
}

private static final class Macro
{
  public final SourceRange nameLoc = new SourceRange();
  public final SourceRange bodyLoc = new SourceRange();
  public final Symbol symbol;
  public final Builtin builtin;
  public boolean funcLike;
  public boolean variadic;
  public boolean expanding;

  public final ArrayList<ParamDecl> params = new ArrayList<ParamDecl>();
  public final TokenList<AbstractToken> body = new TokenList<AbstractToken>();

  Macro ( final Symbol symbol, ISourceRange nameLoc, Builtin builtin )
  {
    this.symbol = symbol;
    this.nameLoc.setRange( nameLoc );
    this.builtin = builtin;
  }

  final int paramCount ()
  {
    return params.size();
  }

  void cleanUpParamScope ()
  {
    for ( ParamDecl param : params )
      param.cleanUp();
  }

  boolean same ( Macro m )
  {
    if (this.symbol != m.symbol ||
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
  public boolean variadic;

  ParamDecl ( final Symbol symbol, int index, boolean variadic )
  {
    this.prevPPDecl = symbol.ppDecl;
    this.symbol = symbol;
    this.index = index;
    this.variadic = variadic;

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
  public ParamToken clone ()
  {
    ParamToken res = new ParamToken( this.param );
    res.setRange( this );
    return res;
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
  public void output ( final OutputStream out ) throws IOException
  {
    out.write(this.param.symbol.bytes);
  }
}

/**
 * A '##' between two tokens.
 *
 * It is left associative, meaning that multiple '##'-s aways generate a left tree.
 * 'a ## b ## c' always produces
 * <p>{@code concat( concat( a, b ), c )}
 */
private static final class ConcatToken extends AbstractToken
{
  private final AbstractToken tokens[];

  public ConcatToken ( AbstractToken left, AbstractToken right )
  {
    assert !(right instanceof ConcatToken);

    m_code = Code.CONCAT;
    if (!(left instanceof ConcatToken))
      this.tokens = new AbstractToken[]{ left, right };
    else
    {
      final ConcatToken lt = (ConcatToken)left;
      final int len = lt.tokens.length;
      this.tokens = new AbstractToken[len+1];
      System.arraycopy( lt.tokens, 0, this.tokens, 0, len );
      this.tokens[len] = right;
    }
  }

  private ConcatToken ( AbstractToken[] tokens )
  {
    this.tokens = tokens;
  }

  @SuppressWarnings("CloneDoesntCallSuperClone")
  @Override
  public ConcatToken clone ()
  {
    AbstractToken[] t = new AbstractToken[this.tokens.length];
    for ( int i = 0; i < this.tokens.length; ++i )
      t[i] = this.tokens[i].clone();
    ConcatToken res = new ConcatToken( t );
    res.setRange( this );
    return res;
  }

  @Override
  public boolean same ( final AbstractToken tok )
  {
    if (tok.m_code != m_code)
      return false;
    ConcatToken t = (ConcatToken)tok;
    if (this.tokens.length != t.tokens.length)
      return false;
    for ( int i = 0; i < tokens.length; ++i )
      if (!this.tokens[i].same( t.tokens[i] ))
        return false;
    return true;
  }

  @Override
  public int length ()
  {
    int len = 0;
    for ( AbstractToken tok : this.tokens )
      len += tok.length();
    return len;
  }

  @Override
  public String toString ()
  {
    StringBuilder b = new StringBuilder();
    b.append(  "ConcatToken{" );
    for ( int i = 0; i < this.tokens.length; ++i )
    {
      if (i > 0)
        b.append( ", " );
      b.append( i ).append( '=' ).append( this.tokens[i].toString() );
    }
    b.append( '}' );
    return b.toString();
  }

  private static final byte[] s_text = " ## ".getBytes();
  @Override
  public void output ( final OutputStream out ) throws IOException
  {
    for ( int i = 0; i < this.tokens.length; ++i )
    {
      if (i > 0)
        out.write( s_text );
      this.tokens[i].output( out );
    }
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
private Token m_tok;

private static final class Arg
{
  public final TokenList<Token> original;
  public final TokenList<Token> expanded;

  private Arg ( final TokenList<Token> original, final TokenList<Token> expanded )
  {
    this.original = original;
    this.expanded = expanded;
  }
}

private static enum ContextState
{
  MACRO, PARAM, CONCAT, CONCAT_PARAM, SPACE_BEFORE_PARAM
}

private final class Context
{
  private final SourceRange m_pos = new SourceRange();
  final Macro macro;
  private final ArrayList<Arg> m_args;
  private final TokenList<AbstractToken> m_tokens;
  private ContextState m_state;
  private AbstractToken m_next;

  private TokenList<Token> m_argTokens;
  private Token m_argNext;

  private AbstractToken[] m_concatChildren;
  private int m_concatIndex;
  private Token m_concatA, m_concatB;

  Context ( ISourceRange pos, final Macro macro, final ArrayList<Arg> args )
  {
    this.macro = macro;
    m_pos.setRange( pos );
    m_args = args;
    m_tokens = macro.body;
    m_state = ContextState.MACRO;
    m_next = m_tokens.first();
  }

  Context ( TokenList<Token> tokens )
  {
    this.macro = null;
    m_args = null;
    m_tokens = null;
    m_state = ContextState.PARAM;
    m_next = null;

    m_argTokens = tokens;
    m_argNext = m_argTokens.first();
  }

  private Token nextToken ()
  {
    Token res = _nextToken();
    if (res == null)
      return null;

    if (m_pos.line1 > 0)
      res.setRange( m_pos );

    if (!res.isNoExpand() && res.code() == Code.IDENT)
      if (res.symbol().ppDecl instanceof Macro)
      {
        Macro macro = (Macro) res.symbol().ppDecl;
        if (macro.expanding)
        {
          res = res.clone();
          res.setNoExpand( true );
        }
      }

    return res;
  }

  private Token _nextToken ()
  {
    for(;;)
    {
      AbstractToken tok;

      switch (m_state)
      {
      case MACRO:
        {
          if ((tok = m_next) == null)
          {
            popContext();
            return null;
          }
          m_next = m_tokens.next(m_next);

          switch (tok.code())
          {
          case MACRO_PARAM:
            {
              ParamToken pt = (ParamToken)tok;
              // Note: we must check args.size() because in a variadic macro the last argument may be missing
              Arg arg = pt.param.index < m_args.size() ? m_args.get( pt.param.index ) : null;
              if (pt.stringify)
                return stringify(arg != null ? arg.original : null);
              else if (arg != null)
              {
                m_argTokens = arg.expanded;
                m_argNext = m_argTokens.first();
                m_state = ContextState.PARAM;
              }
            }
            break;

          case CONCAT:
            m_concatChildren = ((ConcatToken)tok).tokens;
            m_concatIndex = 0;
            m_concatA = m_concatB = null;

            // GCC extension. ', ## __VA_ARGS__' eliminates the comma if __VA_ARGS__ is null
            //
            if (m_opts.gccExtensions &&
                m_concatChildren.length == 2 &&
                m_concatChildren[0].code() == Code.COMMA &&
                m_concatChildren[1].code() == Code.MACRO_PARAM &&
                ((ParamToken)m_concatChildren[1]).param.variadic &&
                !((ParamToken)m_concatChildren[1]).stringify)
            {
              ParamToken pt = (ParamToken)m_concatChildren[1];
              // Note: we must check args.size() because in a variadic macro the last argument may be missing
              Arg arg = pt.param.index < m_args.size() ? m_args.get( pt.param.index ) : null;
              if (arg != null)
              {
                m_argTokens = arg.expanded;
                m_argNext = m_argTokens.first();
                m_state = ContextState.SPACE_BEFORE_PARAM;

                Token res = (Token)m_concatChildren[0];
                m_concatChildren = null;
                return res;
              }
              else
                break;
            }
            m_state = ContextState.CONCAT;
            break;

          default:
            return (Token)tok;
          }
        }
        break;

      case SPACE_BEFORE_PARAM:
        m_state = ContextState.PARAM;
        return m__defaultWs;

      case PARAM:
        {
          Token res;
          if ((res = m_argNext) == null)
            m_state = ContextState.MACRO;
          else
          {
            m_argNext = m_argTokens.next( m_argNext );
            return res;
          }
        }
        break;

      case CONCAT:
        {
          if (m_concatA == null)
            m_concatA = m_concatB;
          else if (m_concatB != null)
          {
            Token tmp = concatTokens( m_pos, m_concatA, m_concatB );
            if (tmp != null) // successful concatenation?
            {
              m_concatA = tmp;
              m_concatB = null;
            }
            else // Handle the error case by returning the tokens separately
            {
              Token res = m_concatA;
              m_concatA = null;
              return res;
            }
          }

          if (m_concatIndex == m_concatChildren.length)
          {
            m_concatChildren = null;
            m_state = ContextState.MACRO;
            if (m_concatA != null)
            {
              Token res = m_concatA;
              m_concatA = null;
              return res;
            }
            else
              break;
          }

          AbstractToken child = m_concatChildren[ m_concatIndex++ ];
          if (child instanceof ParamToken)
          {
            ParamToken pt = (ParamToken)child;
            // Note: we must check args.size() because in a variadic macro the last argument may be missing
            Arg arg = pt.param.index < m_args.size() ? m_args.get( pt.param.index ) : null;
            if (pt.stringify)
            {
              m_concatB = stringify(arg != null ? arg.original : null);
              m_state = ContextState.CONCAT;
              break;
            }
            else if (arg != null)
            {
              m_argTokens = arg.original;
              m_argNext = m_argTokens.first();
              m_state = ContextState.CONCAT_PARAM;
              break;
            }
            else
            {
              m_concatB = null;
              m_state = ContextState.CONCAT;
              break;
            }
          }
          else
          {
            m_concatB = (Token)child;
            m_state = ContextState.CONCAT;
            break;
          }
        }

      // Return all parameter tokens except the last one which we must concat
      case CONCAT_PARAM:
        {
          Token res;
          if ( (res = m_argNext) == null ||
               (m_argNext = m_argTokens.next( m_argNext )) == null)
          {
            m_concatB = res;
            m_state = ContextState.CONCAT;
            break;
          }
          else
            return res;
        }
      }
    }
  }
}

private Context m_ctx;
private ArrayList<Context> m_contexts = new ArrayList<Context>();

private final void pushContext ( Context ctx )
{
  if (ctx.macro != null)
  {
    assert !ctx.macro.expanding;
    ctx.macro.expanding = true;
  }
  m_contexts.add( ctx );
  m_ctx = ctx;
}

private final void popContext ()
{
  int last = m_contexts.size() - 1;

  Context ctx = m_contexts.remove( last );
  if (ctx.macro != null)
  {
    assert ctx.macro.expanding;
    ctx.macro.expanding = false;
  }

  if (last > 0)
    m_ctx = m_contexts.get( last-1 );
  else
    m_ctx = null;
}

/** Holds lookahead tokens from a context */
private final ArrayDeque<Token> m_laQueue = new ArrayDeque<Token>();

private final void _next ()
{
  if (!m_laQueue.isEmpty())
  {
    m_tok = m_laQueue.poll();
    return;
  }

  do
  {
    if (m_ctx == null)
    {
      m_tok = innerNextToken();
      return;
    }
  }
  while ( (m_tok = m_ctx.nextToken()) == null);
}

private final Token lookAheadForLParen ()
{
  int size;
  if ( (size = m_laQueue.size()) > 0)
  {
    assert false : "Unoptimized path!";
    Token toks[] = m_laQueue.toArray( new Token[size] );
    for ( Token la : toks )
    {
      if (la.code() != Code.WHITESPACE && la.code() != Code.NEWLINE)
        return la;
    }
  }

  while (m_ctx != null)
  {
    Token la = m_ctx.nextToken();
    if (la != null)
    {
      m_laQueue.offer( la );
      if (la.code() != Code.WHITESPACE && la.code() != Code.NEWLINE)
        return la;
    }
  }

  int distance = 0;
  Token la;
  do
    la = lookAhead( ++distance );
  while (la.code() == Code.WHITESPACE || la.code() == Code.NEWLINE);
  return la;
}

private final void nextWithBlanks ()
{
  m_skippedWs = null;
  _next();
}

private final void nextNoBlanks ()
{
  m_skippedWs = null;
  _next();
  while (m_tok.code() == Code.WHITESPACE)
  {
    m_skippedWs = m__defaultWs;
    _next();
  }
}

private final void nextNoNewLineOrBlanks ()
{
  m_skippedWs = null;
  _next();
  while (m_tok.code() == Code.WHITESPACE || m_tok.code() == Code.NEWLINE)
  {
    m_skippedWs = m__defaultWs;
    _next();
  }
}

private final void skipBlanks ()
{
  m_skippedWs = null;
  while (m_tok.code() == Code.WHITESPACE)
  {
    m_skippedWs = m__defaultWs;
    _next();
  }
}

private final void skipUntilEOL ()
{
  m_skippedWs = null;
  while (m_tok.code() != Code.NEWLINE && m_tok.code() != Code.EOF)
    _next();
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
          m_reporter.error( m_tok, "Duplicated macro parameter '%s'", sym.name );
          skipUntilEOL();
          return false;
        }

        macro.params.add( new ParamDecl( sym, macro.params.size(), false ) );

        nextNoBlanks();
        if (m_tok.code() == Code.R_PAREN)
          break;
        else if (m_tok.code() == Code.COMMA)
          nextNoBlanks();
/* GCC extension for variadic macros "macro(args...)" */
        else if (m_opts.gccExtensions && m_tok.code() == Code.ELLIPSIS)
        {
          macro.variadic = true;
          macro.params.get( macro.params.size()-1 ).variadic = true;

          nextNoBlanks();

          if (m_tok.code() == Code.R_PAREN)
            break;
          else
          {
            m_reporter.error( m_tok, "Expected ')' after '...' in macro parameter list" );
            skipUntilEOL();
            return false;
          }
        }
        else
        {
          m_reporter.error(  m_tok, "Expected ',', ')', '...' or an identifier in macro parameter list" );
          skipUntilEOL();
          return false;
        }
      }
      else if (m_tok.code() == Code.ELLIPSIS)
      {
        macro.variadic = true;
        macro.params.add( new ParamDecl( m_sym_VA_ARGS, macro.params.size(), true ) );
        nextNoBlanks();

        if (m_tok.code() == Code.R_PAREN)
          break;
        else
        {
          m_reporter.error( m_tok, "Expected ')' after '...' in macro parameter list" );
          skipUntilEOL();
          return false;
        }
      }
      else
      {
        if (m_tok.code() == Code.EOF || m_tok.code() == Code.NEWLINE)
          m_reporter.error( m_tok, "Missing closing ')' in macro parameter list" );
        else
          m_reporter.error( m_tok, "Macro parameter name expected" );
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
    Token savedWs = m_skippedWs;

    /* 6.10.3.2 (1) Each # preprocessing token in the replacement list for a function-like macro shall be
       followed by a parameter as the next preprocessing token in the replacement list. */
    nextNoBlanks();

    m_skippedWs = savedWs; // Keep the space before the '# something'

    if ( (param = isParam( m_tok )) != null)
    {
      ParamToken paramToken = new ParamToken( param );
      paramToken.stringify = true;
      tok = paramToken;
    }
    else
    {
      m_reporter.error( m_tok, "'#' must be followed by a macro parameter" );
      skipUntilEOL();
      return null;
    }
  }
  else if ((param = isParam( m_tok )) != null)
    tok = new ParamToken( param );
  else if (m_tok.code() == Code.IDENT && m_tok.symbol() == m_sym_VA_ARGS)
  {
    assert !macro.variadic;
    m_reporter.error( m_tok, "'__VA_ARGS__' must only appear in a variadic macro" );
    skipUntilEOL();
    return null;
  }
  else
    tok = m_tok.clone();

  return tok;
}

private final boolean parseMacroReplacementList ( Macro macro )
{
  macro.bodyLoc.setRange( m_tok );
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
        m_reporter.error( m_tok, "'##' can only occur between two tokens" );
        skipUntilEOL();
        return false;
      }

      do // skip consecutive '##'
      {
        m_tmpRange.setRange( m_tok ); // Save the location of the token
        nextNoBlanks();
        if (m_tok.code() == Code.EOF || m_tok.code() == Code.NEWLINE)
        {
          m_reporter.error( m_tmpRange, "'##' can only occur between two tokens" );
          return false;
        }
      }
      while (m_tok.code() == Code.HASH_HASH);

      m_skippedWs = null;

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
      macro.body.addLastClone( m_skippedWs );
      m_skippedWs = null;
    }

    macro.body.addLast( tok );
    macro.bodyLoc.extend( m_tok );
  }

  return true;
}

private final void parseDefine ()
{
  nextNoBlanks(); // consume the 'define'

  if (m_tok.code() != Code.IDENT)
  {
    m_reporter.error( m_tok, "An identifier macro name expected" );
    skipUntilEOL();
    return;
  }

  final Symbol macroSym = m_tok.symbol();
  final Macro macro = new Macro( macroSym, m_tok, null );
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
    if (prevMacro.builtin != null)
    {
      m_reporter.warning( macro.nameLoc, "redefinition of builtin macro '%s'", macro.symbol.name );
    }
    else if (!macro.same( prevMacro ))
    {
      m_reporter.warning(
        macro.nameLoc, "redefinition of macro '%s' differs from previous definition at %s",
        macro.symbol.name, m_reporter.formatRange( prevMacro.nameLoc )
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

private final Token stringify ( TokenList<Token> toks )
{
  try
  {
    // Estimate the size
    int size = 0;
    if (toks != null)
      for ( Token tok : toks )
        size += tok.length();

    ByteArrayOutputStream bo = new ByteArrayOutputStream( size + 16 );
    if (toks != null)
      for ( Token tok : toks )
        tok.output( bo );

    // Perform escaping
    byte[] buf = bo.toByteArray();
    bo.reset();

    bo.write( '"' );
    for ( int ch : buf )
    {
      if (ch == '"' || ch == '\\')
      {
        bo.write( '\\' );
        bo.write( ch );
      }
      else if (ch == '\n')
      {
        bo.write( '\\' );
        bo.write( 'n' );
      }
      else if (ch < 32)
      {
        bo.write( '\\' );
        bo.write( ((ch >>> 6)&3) + '0' );
        bo.write( ((ch >>> 3)&7) + '0' );
        bo.write( (ch&7) + '0' );
      }
      else
        bo.write( ch );
    }
    bo.write( '"' );

    Token res = new Token();
    res.setTextWithOnwership( Code.STRING_CONST, bo.toByteArray() );
    return res;
  }
  catch (IOException e)
  {
    throw new RuntimeException( "Unexpected", e );
  }
}

private final Token concatTokens ( ISourceRange pos, Token a, Token b )
{
  ByteArrayOutputStream os = new ByteArrayOutputStream(a.length() + b.length());
  try
  {
    a.output(os);
    b.output(os);
  }
  catch (IOException e)
  {
    throw new RuntimeException( "Unexpected", e );
  }
  byte[] bytes = os.toByteArray();
  ByteArrayInputStream is = new ByteArrayInputStream(bytes);

  final int[] errorCount = new int[1];

  final IErrorReporter reporter = new IErrorReporter()
  {
    @Override
    public void warning ( final ISourceRange rng, final String format, final Object... args ) {}

    @Override
    public void error ( final ISourceRange rng, final String format, final Object... args )
    {
      ++errorCount[0];
    }

    @Override
    public String formatRange ( final ISourceRange rng ) {return "";}
  };

  PPLexer lexer = new PPLexer( reporter, "", is, m_symTable, bytes.length+1 );
  Token res = lexer.innerNextToken();
  if (res.code() == Code.EOF || res.code() == Code.WHITESPACE || errorCount[0] != 0 ||
      lexer.lookAhead(1).code() != Code.EOF)
  {
    m_reporter.error( pos, "Combining \"%s\" and \"%s\" does not produce a valid token",
                      Utils.asciiString( bytes, 0, a.length() ),
                      Utils.asciiString( bytes, a.length(), b.length() ) );
    return null;
  }
  return res.clone();
}


private final boolean expand ( ISourceRange pos, Macro macro, ArrayList<Arg> args )
{
  int expectedParams = macro.variadic ? macro.paramCount() - 1 : macro.paramCount();
  int actualArguments = args != null ? args.size() : 0;

  // Check if the number of parameters match
  if (macro.variadic && actualArguments < expectedParams ||
      !macro.variadic && actualArguments != expectedParams)
  {
    m_reporter.error( new SourceRange(pos).extendBefore( m_tok ),
       "macro '%s' requires %d arguments but %d supplied",
       macro.symbol.name, expectedParams, actualArguments );
    return false;
  }

  if (macro.variadic)
  {
    assert args != null; // if macro is func-like, we can't be called with null args

    if (args.size() >= macro.paramCount()) // Are there any variadic arguments at all?
    {
      // Combine all variadic arguments into one
      Arg arg = args.get( macro.paramCount() - 1 );
      //TokenList<Token> vaArgs = ;
      for ( int i = macro.paramCount(); i < args.size(); ++i )
      {
        if (!arg.original.isEmpty())
        {
          // vaArgs.add( new Token(Code.WHITESPACE) );
          arg.original.addLast(new Token(Code.COMMA));
          arg.original.addLast(new Token(Code.WHITESPACE));
        }
        arg.original.transferAll(args.get(i).original);
        if (!arg.expanded.isEmpty())
        {
          // vaArgs.add( new Token(Code.WHITESPACE) );
          arg.expanded.addLast(new Token(Code.COMMA));
          arg.expanded.addLast(new Token(Code.WHITESPACE));
        }
        arg.expanded.transferAll(args.get(i).expanded);
      }

      for ( int i = args.size() - 1; i > macro.paramCount(); --i )
        args.remove( i );
    }
  }

  pushContext( new Context( pos, macro, args ) );
  return true;
}

private final TokenList<Token> parseMacroArg ()
{
  final TokenList<Token> res = new TokenList<Token>();
  int parenCount = 0;

  m_skippedWs = null;

  for(;;)
  {
    switch (m_tok.code())
    {
    case L_PAREN:
      ++parenCount;
      break;

    case R_PAREN:
      if (parenCount == 0)
        return res;
      --parenCount;
      break;

    case COMMA:
      if (parenCount == 0)
        return res;
      break;

    case EOF:
      return res; // The caller will report the error
    }

    if (m_skippedWs != null)
      res.addLastClone(m_skippedWs);
    res.addLastClone(m_tok);
    nextNoNewLineOrBlanks();
  }
}

private final boolean expandFuncMacro ( SourceRange pos, Macro macro )
{
  ArrayList<Arg> args = new ArrayList<Arg>();

  nextNoNewLineOrBlanks();
  assert m_tok.code() == Code.L_PAREN;

  nextNoNewLineOrBlanks();
  if (m_tok.code() != Code.R_PAREN || macro.paramCount() > 0)
  {
    // Accumulate the args
    for (;;)
    {
      TokenList<Token> original = parseMacroArg();
      args.add( new Arg( original, expandTokens( original ) ) );

      if (m_tok.code() == Code.R_PAREN)
        break;
      else if (m_tok.code() == Code.EOF)
      {
        m_reporter.error( m_tok, "Unterminated argument list for macro '%s'", macro.symbol.name );
        return false;
      }
      else if (m_tok.code() == Code.COMMA)
        nextNoNewLineOrBlanks();
      else
        assert false; // parseMacroParam() couldn't have returned with a different token
    }
  }

  assert m_tok.code() == Code.R_PAREN;
  //nextWithBlanks();

  pos.extend( m_tok );

  return expand( pos, macro, args );
}

public static String genString ( String str )
{
  final StringBuilder buf = new StringBuilder( str.length() + 8 );
  buf.append( '"' );
  for ( int len = str.length(), i = 0; i < len; ++i )
  {
    final char ch = str.charAt( i );
    if (ch == '"' || ch == '\\')
      buf.append( '\\' );
    buf.append( ch );
  }
  buf.append( '"' );
  return buf.toString();
}

private final boolean expandBuiltin ( ISourceRange pos, Macro macro )
{
  Token tok = new Token();

  switch (macro.builtin)
  {
  case __LINE__:
    tok.setTextWithOnwership( Code.PP_NUMBER, (pos.getLine1()+"").getBytes() );
    break;
  case __FILE__:
    tok.setTextWithOnwership( Code.STRING_CONST, genString( pos.getFileName() ).getBytes() );
    break;
  case __DATE__:
    tok.copyFrom( (Token) macro.body.first());
    break;
  }

  TokenList<Token> tokens = new TokenList<Token>();
  tokens.addLast( tok );
  for ( Token t : tokens )
    t.setRange( pos );

  pushContext( new Context( tokens ) );

  return true;
}

private final boolean expandObjectMacro ( ISourceRange pos, Macro macro )
{
  if (macro.builtin == null)
    return expand( pos, macro, null );
  else
    return expandBuiltin( pos, macro );
}

/**
 *
 * @param tok
 * @return true if macro expansion will proceed, so we must loop instead of returning a token
 */
private final boolean possiblyExpandMacro ( Token tok )
{
  if (!(tok.symbol().ppDecl instanceof Macro))
    return false;
  Macro macro = (Macro) tok.symbol().ppDecl;

  if (macro.funcLike && lookAheadForLParen().code() != Code.L_PAREN)
    return false;

  if (tok.isNoExpand())
    return false;

  SourceRange pos = new SourceRange(tok);
  return
    macro.funcLike ? expandFuncMacro( pos, macro ) : expandObjectMacro( pos, macro );
}

private final TokenList<Token> expandTokens ( TokenList<Token> tokens )
{
  final TokenList<Token> expanded = new TokenList<Token>();

  tokens.addLast( new Token( Code.EOF ) );
  Token saveTok = m_tok.clone(); // to be on the safe side
  try
  {
    pushContext( new Context( tokens ) );

    for ( _next(); m_tok.code() != Code.EOF; _next() )
    {
      if (m_tok.code() != Code.IDENT || !possiblyExpandMacro( m_tok ))
        expanded.addLastClone( m_tok );
    }
  }
  finally
  {
    m_tok = saveTok;
    tokens.removeLast(); // Remove the EOF we added
  }

  return expanded;
}

private static final int sNORMAL_NEXT = 0;
private static final int sNORMAL_USE = 1;
private static final int sLINEBEG = 2;

private int m_state = sLINEBEG;

public final Token nextToken ()
{
  for(;;)
    switch (m_state)
    {
    case sNORMAL_NEXT:
      nextWithBlanks();
      m_state = sNORMAL_USE;
      // fall
    case sNORMAL_USE:
      OptResult<TokenList<Token>> res;
      if (m_tok.code() == Code.IDENT && possiblyExpandMacro( m_tok ))
      {
        m_state = sNORMAL_NEXT;
        continue;
      }
      else if (m_tok.code() == Code.NEWLINE)
        m_state = sLINEBEG;
      else
        m_state = sNORMAL_NEXT;
      return m_tok;

    case sLINEBEG:
      assert m_ctx == null;
      nextNoBlanks();
      switch (m_tok.code())
      {
      case HASH:
        parseDirective();
        return m_tok;

      default:
        m_state = sNORMAL_USE;
        break;

      case NEWLINE:
        return m_tok;
      }
      break;
    }
}

} // class
