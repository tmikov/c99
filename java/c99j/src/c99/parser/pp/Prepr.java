package c99.parser.pp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import c99.CompilerOptions;
import c99.IErrorReporter;
import c99.ISourceRange;
import c99.SourceRange;
import c99.Utils;
import c99.parser.SymTable;
import c99.parser.Symbol;

public class Prepr extends PPLexer
{
private final CompilerOptions m_opts;
private final Symbol m_sym_VA_ARGS;

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

}

private static final class Macro
{
  public final SourceRange nameLoc = new SourceRange();
  public final SourceRange bodyLoc = new SourceRange();
  public final Symbol symbol;
  public boolean funcLike;
  public boolean variadic;

  public final ArrayList<ParamDecl> params = new ArrayList<ParamDecl>();
  public final LinkedList<AbstractToken> body = new LinkedList<AbstractToken>();

  Macro ( final Symbol symbol, ISourceRange nameLoc )
  {
    this.symbol = symbol;
    this.nameLoc.setRange( nameLoc );
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
    ConcatToken res = new ConcatToken( this.left.clone(), this.right.clone() );
    res.setRange( this );
    return res;
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

  private static final byte[] s_text = " ## ".getBytes();
  @Override
  public void output ( final OutputStream out ) throws IOException
  {
    left.output( out );
    out.write( s_text );
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
private Token m_tok;

private final void nextWithBlanks ()
{
  m_skippedWs = null;
  m_tok = innerNextToken();
}

private final void nextNoBlanks ()
{
  m_skippedWs = null;
  m_tok = innerNextToken();
  while (m_tok.code() == Code.WHITESPACE)
  {
    m_skippedWs = m__defaultWs;
    m_tok = innerNextToken();
  }
}

private final void nextNoNewLineOrBlanks ()
{
  m_skippedWs = null;
  m_tok = innerNextToken();
  while (m_tok.code() == Code.WHITESPACE || m_tok.code() == Code.NEWLINE)
  {
    m_skippedWs = m__defaultWs;
    m_tok = innerNextToken();
  }
}

private final Token lookAheadNoNewLineOrBlanks ()
{
  int distance = 0;
  Token la;
  do
    la = lookAhead( ++distance );
  while (la.code() == Code.WHITESPACE || la.code() == Code.NEWLINE);
  return la;
}

private final void skipBlanks ()
{
  m_skippedWs = null;
  while (m_tok.code() == Code.WHITESPACE)
  {
    m_skippedWs = m__defaultWs;
    m_tok = innerNextToken();
  }
}

private final void skipUntilEOL ()
{
  m_skippedWs = null;
  while (m_tok.code() != Code.NEWLINE && m_tok.code() != Code.EOF)
    m_tok = innerNextToken();
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
          m_reporter.error(  m_tok, "Expected ',', ')', '...' or an identifier in macro parameter list" );
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
      macro.body.add( m_skippedWs.clone() );
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
  final Macro macro = new Macro( macroSym, m_tok );
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

private Iterator<Token> m_expIt;
private final SourceRange m_expRange = new SourceRange();

private final Token stringify ( List<Token> toks )
{
  try
  {
    // Estimate the size
    int size = 0;
    for ( Token tok : toks )
      size += tok.length();

    ByteArrayOutputStream bo = new ByteArrayOutputStream( size + 16 );
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

private final boolean concat (
  LinkedList<Token> expanded, ISourceRange pos, ArrayList<List<Token>> args, ConcatToken ct )
{
  // TODO: In theory we don't need to create new lists here. We can simply
  // keep track of the position in the main list where elements were
  // added. The problem is that it is impossible to have a marker into LinkedList
  // efficiently in the face of modifications, even of these modifications are "safe"
  // (e.g. additions). The list iterators fail if there is any modification at all.
  //
  int savedSize = expanded.size();
  expandATok( expanded, pos, args, ct.left );
  if (expanded.size() == savedSize) // If left was empty, we have nothing to concatenate
  {
    expandATok( expanded, pos, args, ct.right );
    return true;
  }

  LinkedList<Token> rt = new LinkedList<Token>();
  expandATok( rt, pos, args, ct.right );
  if (rt.isEmpty()) // If right is empty, we have nothing to do
    return true;

  // We need to concatenate the right-most token of 'expanded' with the left-most token of 'rt'
  Token newTok;
  if ( (newTok = concatTokens( pos, expanded.getLast(), rt.getFirst())) == null)
  {
    expanded.addAll( rt );
    return false;
  }

  expanded.removeLast();
  expanded.addLast( newTok );
  rt.removeFirst();
  expanded.addAll(rt);
  return true;
}

private final void expandATok (
   LinkedList<Token> expanded, ISourceRange pos, ArrayList<List<Token>> args, AbstractToken atok )
{
  switch (atok.code())
  {
  case CONCAT:
    concat(expanded, pos, args, (ConcatToken) atok);
    break;

  case MACRO_PARAM:
    ParamToken pt = (ParamToken)atok;
    List<Token> tokens = args.get( pt.param.index );
    if (pt.stringify)
      expanded.add( stringify( tokens ) );
    else
      expanded.addAll( tokens );
    break;

  default:
    expanded.addLast( (Token)atok );
    break;
  }
}

private final void expand ( ISourceRange pos, Macro macro, ArrayList<List<Token>> args )
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
    return;
  }

  if (macro.variadic)
  {
    assert args != null; // if macro is func-like, we can't be called with null args

    if (args.size() >= macro.paramCount()) // Are there any variadic arguments at all?
    {
      // Combine all variadic arguments into one
      List<Token> vaArgs = args.get( macro.paramCount() - 1 );
      for ( int i = macro.paramCount(); i < args.size(); ++i )
      {
        if (!vaArgs.isEmpty())
        {
          // vaArgs.add( new Token(Code.WHITESPACE) );
          vaArgs.add( new Token(Code.COMMA) );
          vaArgs.add( new Token(Code.WHITESPACE) );
        }
        vaArgs.addAll( args.get( i ) );
      }

      for ( int i = args.size() - 1; i > macro.paramCount(); --i )
        args.remove( i );
    }
    else // Create an empty argument for VA_ARGS
    {
      args.add( new LinkedList<Token>() );
      assert args.size() >= macro.paramCount();
    }
  }

  LinkedList<Token> expanded = new LinkedList<Token>();

  for ( AbstractToken atok : macro.body )
  {
    expandATok( expanded, pos, args, atok );
  }

  m_state = sEXPANDING;
  m_expRange.setLocation( pos );
  m_expIt = expanded.iterator();
}

private final LinkedList<Token> parseMacroArg ()
{
  final LinkedList<Token> res = new LinkedList<Token>();
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
      res.addLast( m_skippedWs );
    res.addLast( m_tok.clone() );
    nextNoNewLineOrBlanks();
  }
}

private final void expandFuncMacro ( ISourceRange pos, Macro macro )
{
  ArrayList<List<Token>> args = new ArrayList<List<Token>>();

  nextNoNewLineOrBlanks();
  assert m_tok.code() == Code.L_PAREN;

  nextNoNewLineOrBlanks();
  if (m_tok.code() != Code.R_PAREN || macro.paramCount() > 0)
  {
    // Accumulate the args
    for (;;)
    {
      args.add( parseMacroArg() );

      if (m_tok.code() == Code.R_PAREN)
        break;
      else if (m_tok.code() == Code.EOF)
      {
        m_reporter.error( m_tok, "Unterminated argument list for macro '%s'", macro.symbol.name );
        return;
      }
      else if (m_tok.code() == Code.COMMA)
        nextNoNewLineOrBlanks();
      else
        assert false; // parseMacroParam() couldn't have returned with a different token
    }
  }

  assert m_tok.code() == Code.R_PAREN;
  nextWithBlanks();

  expand( pos, macro, args );
}

private final void expandObjectMacro ( ISourceRange pos, Macro macro )
{
  expand( pos, macro, null );
}

/**
 *
 * @param macro
 * @return true if macro expansion will proceed, so we must loop instead of returning a token
 */
private final boolean possiblyExpandMacro ( Macro macro )
{
  if (macro.funcLike)
  {
    if (lookAheadNoNewLineOrBlanks().code() != Code.L_PAREN)
      return false;

    expandFuncMacro( new SourceRange(m_tok), macro );
  }
  else
    expandObjectMacro( new SourceRange(m_tok), macro );

  return true;
}

private static final int sNORMAL_NEXT = 0;
private static final int sNORMAL_USE = 1;
private static final int sLINEBEG = 2;
private static final int sEXPANDING = 3;

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
      if (m_tok.code() == Code.IDENT && m_tok.symbol().ppDecl instanceof Macro &&
          possiblyExpandMacro( (Macro)(m_tok.symbol().ppDecl)))
      {
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
        break;

      case NEWLINE:
        return m_tok;
      }
      break;

    case sEXPANDING:
      if (m_expIt.hasNext())
      {
        Token res = (Token)m_expIt.next();
        m_expRange.shiftExtend( res.length() );
        res.setRange( m_expRange );
        return res;
      }
      else
        m_state = sNORMAL_USE;
      break;
    }
}

} // class
