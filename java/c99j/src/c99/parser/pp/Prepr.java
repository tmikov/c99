package c99.parser.pp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;

import c99.CompilerLimits;
import c99.CompilerOptions;
import c99.Constant;
import c99.IErrorReporter;
import c99.ISourceRange;
import c99.SourceRange;
import c99.Types;
import c99.Utils;
import c99.parser.Code;
import c99.parser.SymTable;
import c99.parser.Symbol;

public class Prepr implements PPDefs
{
private final CompilerOptions m_opts;
private final IErrorReporter m_reporter;
private final ISearchPath m_searchPath;
private final SymTable m_symTable;
private final Symbol m_sym_VA_ARGS;

private static final class Source
{
  final PPLexer lex;
  final int lineAdjustment;

  Source ( final PPLexer lex, final int lineAdjustment )
  {
    this.lex = lex;
    this.lineAdjustment = lineAdjustment;
  }
}

private final ArrayList<Source> m_sourceStack = new ArrayList<Source>();

private PPLexer m_lex;
private int m_lineAdjustment;

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


public Prepr ( final CompilerOptions opts, final IErrorReporter reporter,
               final ISearchPath searchPath,
               final String fileName, final InputStream input,
               final SymTable symTable )
{
  m_opts = opts;
  m_reporter = reporter;
  m_searchPath = searchPath;
  m_symTable = symTable;

  m_lex = new PPLexer(reporter, fileName, input, symTable );

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
  Token tok = new Token();
  tok.setStringConst( new SimpleDateFormat( "MMM dd yyyy" ).format( new Date() ) );
  dateMacro.body.addLast( tok );
}

public final void close ()
{
  while (popSource())
    {}
  m_lex.close();
}

private final void pushSource ( String fileName, InputStream input )
{
  PPLexer newLexer = new PPLexer(m_reporter, fileName, input, m_symTable);
  boolean ok = false;
  try
  {
    m_sourceStack.add( new Source(m_lex, m_lineAdjustment) );
    ok = true;
  }
  finally
  {
    if (!ok)
      newLexer.close();
  }

  m_lex = newLexer;
  m_lineAdjustment = 0;
}

private final boolean popSource ()
{
  int size;
  if ( (size = m_sourceStack.size()) > 0)
  {
    m_lex.close();

    Source src = m_sourceStack.remove( size - 1 );
    m_lex = src.lex;
    m_lineAdjustment = src.lineAdjustment;
    return true;
  }
  else
    return false;
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
  m_contexts.add(ctx);
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

private final void adjustToken ()
{
  m_tok.line1 += m_lineAdjustment;
  m_tok.line2 += m_lineAdjustment;
}

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
      m_tok = m_lex.innerNextToken();
      adjustToken();
      return;
    }
  }
  while ( (m_tok = m_ctx.nextToken()) == null);
}

private final void _nextIncludeToken ()
{
  assert m_laQueue.isEmpty();
  assert m_ctx == null;

  m_tok = m_lex.nextIncludeToken();
  adjustToken();
}

private final void discardLine ()
{
  assert m_laQueue.isEmpty();

  if (m_tok.code() != Code.NEWLINE && m_tok.code() != Code.EOF)
  {
    m_skippedWs = null;
    m_tok = m_lex.discardLine();
    assert m_tok.code() == Code.NEWLINE || m_tok.code() == Code.EOF;
    adjustToken();
  }
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
    la = m_lex.lookAhead( ++distance );
  while (la.code() == Code.WHITESPACE || la.code() == Code.NEWLINE);
  return la;
}

private final void nextWithBlanks ()
{
  m_skippedWs = null;
  _next();
}

private final Token nextNoBlanks ()
{
  m_skippedWs = null;
  _next();
  while (m_tok.code() == Code.WHITESPACE)
  {
    m_skippedWs = m__defaultWs;
    _next();
  }
  return m_tok;
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

private final Token nextExpandWithBlanks ()
{
  m_skippedWs = null;
  do
    _next();
  while (m_tok.code() == Code.IDENT && possiblyExpandMacro( m_tok ));
  return m_tok;
}

private final Token curExpandWithBlanks ()
{
  m_skippedWs = null;
  while (m_tok.code() == Code.IDENT && possiblyExpandMacro( m_tok ))
    _next();
  return m_tok;
}

private final Token curExpandNoBlanks ()
{
  m_skippedWs = null;
  while (m_tok.code() == Code.WHITESPACE || m_tok.code() == Code.IDENT && possiblyExpandMacro( m_tok ))
    _next();
  return m_tok;
}

private final Token nextExpandNoBlanks ()
{
  m_skippedWs = null;
  do
    _next();
  while (m_tok.code() == Code.WHITESPACE || m_tok.code() == Code.IDENT && possiblyExpandMacro( m_tok ));
  return m_tok;
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
    m_reporter.error( m_tok, "An identifier macro name expected after #define" );
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

private final void parseUndef ()
{
  nextNoBlanks(); // consume the 'undef'

  if (m_tok.code() != Code.IDENT)
  {
    m_reporter.error( m_tok, "An identifier macro name expected after #undef" );
    skipUntilEOL();
    return;
  }

  final Symbol macroSym = m_tok.symbol();
  if (macroSym.ppDecl instanceof Macro)
  {
    if (((Macro)macroSym.ppDecl).builtin != null)
      m_reporter.warning( m_tok, "undefining '%s'", macroSym.name );

    macroSym.ppDecl = null;
  }
  else
    if (m_opts.warnUndef)
      m_reporter.warning( m_tok, "Macro '%s' not defined in #undef", macroSym.name );

  nextNoBlanks();
  if (m_tok.code() != Code.EOF && m_tok.code() != Code.NEWLINE)
  {
    m_reporter.error( m_tok, "Extra tokens after end of #undef" );
    skipUntilEOL();
  }
}

private final void handleLineDirective ( int line, String fileName )
{
  m_lineAdjustment = line - m_tok.getLine1() + m_lineAdjustment - 1;
  if (fileName != null)
    m_lex.setFileName( fileName );

  // We have a problem if there are multiple new lines after the #line directive,
  // since they have been merged into a single token. What we want is to modify the
  // current new-line to end on the next line, and to synthesize a new one with
  // the adjusted line
  if (m_tok.line2 - m_tok.line1 > 1)
  {
    TokenList<Token> list = new TokenList<Token>();
    Token nl = new Token( Code.NEWLINE );
    nl.fileName = fileName != null ? fileName : m_tok.fileName;
    nl.line1 = line;
    nl.col1 = 1;
    nl.line2 = m_tok.line2 + m_lineAdjustment;
    nl.col2 = 1;
    list.addLast( nl );
    pushContext( new Context( list ) );
    m_tok.line2 = m_tok.line1 + 1;
  }
}

private final int parseLineNumber ( String afterWhat )
{
  if (m_tok.code() != Code.INT_NUMBER)
  {
    m_reporter.error( m_tok, "Integer line number expected after %s", afterWhat );
    skipUntilEOL();
    return -1;
  }

  // Line directive only recognizes decimal numbers
  if (m_tok.textLen() > 1 && m_tok.text()[0] == '0')
  {
    m_reporter.error( m_tok, "Only decimal integers supported after %s", afterWhat );
    skipUntilEOL();
    return -1;
  }

  long lline = m_tok.getIntConstValue()._getLong();
  int line = (int)lline;
  if (line < 0 || line != lline)
  {
    m_reporter.error( m_tok, "Line number outside of allowed range after %s", afterWhat );
    skipUntilEOL();
    return -1;
  }

  return line;
}

private final void parseLine ()
{
  nextExpandNoBlanks(); // consume the 'line'

  int line;
  if ( (line = parseLineNumber( "#line" )) < 0)
    return;

  nextExpandNoBlanks();
  String fileName;
  if (m_tok.code() != Code.EOF && m_tok.code() != Code.NEWLINE)
  {
    if (m_tok.code() != Code.STRING_CONST)
    {
      m_reporter.error( m_tok, "Filename must be a string constant after #line" );
      skipUntilEOL();
      return;
    }

    fileName = Utils.asciiString( m_tok.getStringConstValue() );
    if (fileName == null)
    {
      skipUntilEOL();
      return;
    }

    nextNoBlanks();
  }
  else
    fileName = null;

  if (m_tok.code() != Code.EOF && m_tok.code() != Code.NEWLINE)
  {
    m_reporter.error( m_tok, "Extra tokens after end of #line" );
    skipUntilEOL();
  }

  handleLineDirective( line, fileName );
}

private final void parseLineMarker ()
{
  int line;
  if ( (line = parseLineNumber( "#" )) < 0)
    return;

  nextNoBlanks();

  if (m_tok.code() != Code.STRING_CONST)
  {
    m_reporter.error( m_tok, "Missing filename after #" );
    skipUntilEOL();
    return;
  }

  String fileName = Utils.asciiString( m_tok.getStringConstValue() );
  if (fileName == null)
  {
    skipUntilEOL();
    return;
  }

  nextNoBlanks();
  boolean ignored = false;
  while (m_tok.code() != Code.NEWLINE && m_tok.code() != Code.EOF)
  {
    if (m_tok.code() != Code.INT_NUMBER)
      m_reporter.error( m_tok, "Invalid flag '%s' after #", m_tok.outputString() );
    else
    {
      if (!ignored)
      {
        ignored = true;
        m_reporter.warning( m_tok, "Ignoring unsupported GCC-style marker flags" );
      }
    }

    nextNoBlanks();
  }

  handleLineDirective(line, fileName);
}

private final void parseInclude ()
{
  SourceRange pos = new SourceRange(m_tok);

  assert m_laQueue.isEmpty();
  assert m_ctx == null;

  // Consume the include, but carefully :-)
  //
  m_skippedWs = null;
  do
    _nextIncludeToken();
  while (m_tok.code() == Code.WHITESPACE);

  boolean angled;
  String name;

  if (m_tok.code() == Code.ANGLED_INCLUDE)
  {
    angled = true;
    name = m_tok.textString();
    nextNoBlanks();
  }
  else
  {
    curExpandNoBlanks();

    if (m_tok.code() == Code.STRING_CONST)
    {
      angled = false;
      name = Utils.asciiString( m_tok.getStringConstValue() );
      if (name == null)
      {
        skipUntilEOL();
        return;
      }

      nextNoBlanks();
    }
    else if (m_tok.code() == Code.LESS)
    {
      // So, this is a "magical" case. A #include <filename> form, generated by expanding
      // a macro
      angled = true;
      StringBuilder buf = new StringBuilder();
loop:
      for(;;)
      {
        nextExpandWithBlanks();
        switch (m_tok.code())
        {
        case EOF: case NEWLINE:
          m_reporter.error( m_tok, "Missing #include terminating > character" );
          break loop;

        case MINUS_GREATER: case GREATER_GREATER:
          {
            String s = m_tok.outputString();
            buf.append( s.substring( 0, s.length()-1 ) );
            nextExpandNoBlanks();
          }
          break loop;

        case GREATER_EQUALS:
        case GREATER_GREATER_EQUALS:
          m_reporter.error( m_tok, "Extra tokens after end of #include" );
          nextExpandNoBlanks();
          break loop;

        case GREATER:
          nextExpandNoBlanks();
          break loop;
        default:
          buf.append( m_tok.outputString() );
          break;
        }
      }

      name = buf.toString();
    }
    else
    {
      m_reporter.error( m_tok, "Invalid #include filename" );
      skipUntilEOL();
      return;
    }
  }

  if (name.length() == 0)
  {
    m_reporter.error( pos, "Empty filename in #include" );
    skipUntilEOL();
    return;
  }

  checkEOL( "include" );

  ISearchPath.Result resolvedIncludePath;
  if (angled)
    resolvedIncludePath = m_searchPath.searchAngled( name );
  else
    resolvedIncludePath = m_searchPath.searchQuoted( m_lex.getActualFileName(), name );

  if (resolvedIncludePath == null)
  {
    m_reporter.error( pos, "Can't find include file '%s'", name );
    return;
  }

  if (m_sourceStack.size() == CompilerLimits.MAX_INCLUDE_DEPTH)
  {
    m_reporter.error( pos, "More than %d nested includes", CompilerLimits.MAX_INCLUDE_DEPTH );
    return;
  }

  FileInputStream input;
  try
  {
    input = new FileInputStream( resolvedIncludePath.path );
  }
  catch (FileNotFoundException e)
  {
    m_reporter.error( pos, "Can't open include file '%s': %s", name, e.getCause() );
    return;
  }

  pushSource( resolvedIncludePath.path, input );
  if (m_tok.code() == Code.EOF)
  {
    Token tok = new Token(Code.NEWLINE);
    tok.setRange( pos );
    m_tok = tok;
  }
}

private static final class IfState
{
  static final int BLOCK_NONE = 0;
  static final int BLOCK_IF = 1;
  static final int BLOCK_ELSE = 2;

  Token tok;
  int blockType;
  boolean parentExec;
  boolean cond;

  IfState ( Token tok, final int blockType, final boolean parentExec, boolean cond )
  {
    this.tok = tok;
    this.blockType = blockType;
    this.parentExec = parentExec;
    this.cond = cond;
  }
}

private final ArrayList<IfState> m_ifStack = new ArrayList<IfState>();
private IfState m_ifTop = new IfState( null, IfState.BLOCK_NONE, false, false );
private boolean m_exec = true;

private final void pushIfState ( Token tok, int blockType, boolean cond, boolean newExec )
{
  m_ifStack.add( m_ifTop );
  m_ifTop = new IfState( tok, blockType, m_exec, cond );
  m_exec = newExec;
  m_lex.setReportErrors( m_exec );
}

private final void popIfState ()
{
  m_exec = m_ifTop.parentExec;
  m_lex.setReportErrors( m_exec );
  m_ifTop = m_ifStack.remove( m_ifStack.size() - 1 );
}

private final void checkEOL ( String after )
{
  if (m_tok.code() != Code.EOF && m_tok.code() != Code.NEWLINE)
  {
    m_reporter.error( m_tok, "Extra tokens after end of #%s", after );
    skipUntilEOL();
  }
}

private final void parseIfdef ()
{
  Token tok = m_tok.clone();
  nextNoBlanks(); // consume the if[n]def

  boolean cond = false;

  if (!m_exec)
    skipUntilEOL();
  else
  {
    if (m_tok.code() != Code.IDENT)
    {
      m_reporter.error( m_tok, "Identifier expected after #%s", tok.symbol().ppCode.name );
      skipUntilEOL();
    }
    else
    {
      cond = (m_tok.symbol().ppDecl instanceof Macro) ^ (tok.symbol().ppCode == PPSymCode.IFNDEF);
      nextNoBlanks();
      checkEOL(tok.symbol().ppCode.name);
    }
  }

  assert m_tok.code() == Code.EOF || m_tok.code() == Code.NEWLINE;

  pushIfState( tok, IfState.BLOCK_IF, cond, m_exec && cond );
}

private final void parseElse ()
{
  Token tok = m_tok.clone();

  if (m_ifTop.blockType != IfState.BLOCK_IF)
  {
    m_reporter.error( m_tok, "#else without #if" );
    skipUntilEOL();
    return;
  }

  nextNoBlanks(); // consume the else
  if (!m_ifTop.parentExec)
    skipUntilEOL();
  else
    checkEOL( "else" );

  assert m_tok.code() == Code.EOF || m_tok.code() == Code.NEWLINE;
  m_ifTop.tok = tok;
  m_ifTop.blockType = IfState.BLOCK_ELSE;
  m_exec = m_ifTop.parentExec && !m_ifTop.cond;
}

private final void parseEndif ()
{
  if (m_ifTop.blockType != IfState.BLOCK_IF && m_ifTop.blockType != IfState.BLOCK_ELSE)
  {
    m_reporter.error( m_tok, "#endif without #if" );
    skipUntilEOL();
    return;
  }

  nextNoBlanks(); // consume the endif
  if (!m_ifTop.parentExec)
    skipUntilEOL();
  else
    checkEOL( "endif" );

  assert m_tok.code() == Code.EOF || m_tok.code() == Code.NEWLINE;
  popIfState();
}

private boolean m_exprErrorReported;

private final void exprError ( ISourceRange pos, String msg, Object... args )
{
  if (!m_exprErrorReported)
  {
    m_exprErrorReported = true;
    m_reporter.error( pos, msg, args );
  }
}

private final boolean need ( Code code )
{
  if (m_tok.code() == code)
  {
    nextExpandNoBlanks();
    return true;
  }
  else
  {
    exprError( m_tok, "'%s' expected", code.str );
    return false;
  }
}

private static final Constant.IntC s_zero = Constant.makeLong( Types.TypeSpec.INTMAX_T, 0 );
private static final Constant.IntC s_one = Constant.makeLong( Types.TypeSpec.INTMAX_T, 1 );

// 6.10.1 [4] For the purposes of this
// token conversion and evaluation, all signed integer types and all unsigned integer types
// act as if they hav e the same representation as, respectively, the types intmax_t and
// uintmax_t defined in the header <stdint.h>
private static Constant.IntC expandToMax ( Constant.IntC v )
{
  return (Constant.IntC)Constant.convert(
    Types.usualArithmeticConversions( v.spec, Types.TypeSpec.INTMAX_T ), v
  );
}

private static void performUsualArithmeticConversions ( Constant.ArithC e[] )
{
  Types.TypeSpec spec = Types.usualArithmeticConversions( e[0].spec, e[1].spec );
  e[0] = Constant.convert( spec, e[0] );
  e[1] = Constant.convert( spec, e[1] );
}

private static Constant.IntC performIntegerPromotion ( Constant.IntC e )
{
  return (Constant.IntC)Constant.convert( Types.integerPromotion( e.spec ), e );
}

private abstract class Expression
{
  abstract Constant.IntC parse ();
}

private class BinEx extends Expression
{
  private final Expression m_parent;
  private final Code m_c0, m_c1, m_c2, m_c3;

  protected BinEx ( final Expression parent, final Code c0, final Code c1,
                    final Code c2, final Code c3 )
  {
    m_parent = parent;
    m_c0 = c0;
    m_c1 = c1;
    m_c2 = c2;
    m_c3 = c3;
  }

  protected BinEx ( final Expression parent, final Code c0 )
  {
    this( parent, c0, c0, c0, c0 );
  }

  protected BinEx ( final Expression parent, final Code c0, final Code c1 )
  {
    this( parent, c0, c1, c1, c1 );
  }

  protected BinEx ( final Expression parent, final Code c0, final Code c1, final Code c2 )
  {
    this( parent, c0, c1, c2, c2 );
  }

  final Constant.IntC parse ()
  {
    final Constant.IntC e[] = new Constant.IntC[2];
    e[0] = m_parent.parse();

    for ( Code code = m_tok.code();
          code == m_c0 || code == m_c1 || code == m_c2 || code == m_c3;
          code = m_tok.code() )
    {
      final SourceRange pos = new SourceRange( m_tok );

      nextExpandNoBlanks();

      e[1] = m_parent.parse();
      e[0] = perform( pos, code, e );
    }
    return e[0];
  }

  Constant.IntC perform ( ISourceRange pos, Code code, Constant.IntC e[] )
  {
    performUsualArithmeticConversions( e );
    return binaryOp( pos, code, e[0], e[1] );
  }

  @Override
  public String toString ()
  {
    return m_c0.toString(); // For debugging
  }
}

private final Constant.IntC binaryOp ( ISourceRange pos, Code code, Constant.IntC e0, Constant.IntC e1 )
{
  switch (code)
  {
  case ASTERISK: e0.mul( e0, e1 ); return e0;
  case SLASH:
    if (!e1.isZero())
      e0.div( e0, e1 );
    else
      exprError( pos, "Division by zero in '/'" );
    return e0;
  case PERCENT:
    if (!e1.isZero())
      e0.rem( e0, e1 );
    else
      exprError( pos, "Division by zero in '%'" );
    return e0;
  case PLUS: e0.add( e0, e1 ); return e0;
  case MINUS: e0.sub( e0, e1 ); return e0;
  case LESS_LESS: e0.shl( e0, e1 ); return e0;
  case GREATER_GREATER: e0.shr( e0, e1 ); return e0;
  case LESS: return e0.lt( e1 ) ? s_one : s_zero;
  case GREATER: return e0.gt( e1 ) ? s_one : s_zero;
  case LESS_EQUALS: return e0.le( e1 ) ? s_one : s_zero;
  case GREATER_EQUALS: return e0.ge( e1 ) ? s_one : s_zero;
  case EQUALS_EQUALS: return e0.eq( e1 ) ? s_one : s_zero;
  case BANG_EQUALS: return e0.ne( e1 ) ? s_one : s_zero;
  case AMPERSAND: e0.and( e0, e1 ); return e0;
  case CARET: e0.xor( e0, e1 ); return e0;
  case VERTICAL: e0.or( e0, e1 ); return e0;
  case AMPERSAND_AMPERSAND: return e0.isTrue() && e1.isTrue() ? s_one : s_zero;
  case VERTICAL_VERTICAL: return e0.isTrue() || e1.isTrue() ? s_one : s_zero;
  }
  assert false;
  return null;
}

/** The type of the returned value is always either intmax_t or uintmax_t */
private final Constant.IntC primary_expression ()
{
  Constant.IntC res;

  switch (m_tok.code())
  {
  case IDENT:
    nextExpandNoBlanks();
    return s_zero;

  case INT_NUMBER:
    res = expandToMax( m_tok.getIntConstValue() );
    nextExpandNoBlanks();
    return res;

  case CHAR_CONST:
    res = expandToMax( m_tok.getCharConstValue() );
    nextExpandNoBlanks();
    return res;

  case REAL_NUMBER:
    exprError( m_tok, "floating point constants are not valid in preprocessor expressions" );
    nextExpandNoBlanks();
    return s_zero;

  case STRING_CONST:
    exprError( m_tok, "strings are not valid in preprocessor expressions" );
    nextExpandNoBlanks();
    return s_zero;

  case L_PAREN:
    nextExpandNoBlanks();
    res = constant_expression();
    need( Code.R_PAREN );
    return res;

  default:
    if (m_tok.code() == Code.NEWLINE || m_tok.code() == Code.EOF)
      exprError( m_tok, "unexpected end of line in preprocessor expression" );
    else
      exprError( m_tok, "unexpected token '%s' in preprocessor expression", m_tok.outputString() );
    return s_zero;
  }
}

private final Constant.IntC unary_expression ()
{
  Constant.IntC res;
  switch (m_tok.code())
  {
  case PLUS:
    nextExpandNoBlanks();
    return performIntegerPromotion( unary_expression() );
  case MINUS:
    nextExpandNoBlanks();
    res = performIntegerPromotion( unary_expression() );
    res.neg( res );
    return res;
  case TILDE:
    nextExpandNoBlanks();
    res = performIntegerPromotion( unary_expression() );
    res.not( res );
    return res;
  case BANG:
    nextExpandNoBlanks();
    return unary_expression().isZero() ? s_one : s_zero;

  case IDENT:
    {
      if (m_tok.symbol().ppCode != PPSymCode.DEFINED)
        break;
      nextNoBlanks();
      boolean paren = false;
      if (m_tok.code() == Code.L_PAREN)
      {
        paren = true;
        nextNoBlanks();
      }

      if (m_tok.code() != Code.IDENT)
      {
        exprError( m_tok, "Identifier expected after 'defined'" );
        res = s_zero;
      }
      else
      {
        res = (m_tok.symbol().ppDecl instanceof Macro) ? s_one : s_zero;
        if (paren)
          nextNoBlanks();
        else
          nextExpandNoBlanks();
      }

      if (paren)
      {
        if (m_tok.code() != Code.R_PAREN)
          exprError( m_tok, "Missing closing ')' after defined" );
        else
          nextExpandNoBlanks();
      }

      return res;
    }
  }

  return primary_expression();
}

private final Expression m_unary = new Expression() {
  @Override Constant.IntC parse () { return unary_expression(); }
};

private final BinEx m_multipl    = new BinEx(m_unary,      Code.ASTERISK, Code.SLASH, Code.PERCENT );
private final BinEx m_additive   = new BinEx(m_multipl,    Code.PLUS, Code.MINUS );

private final Expression m_shift = new BinEx(m_additive,   Code.LESS_LESS, Code.GREATER_GREATER) {
  @Override Constant.IntC perform (ISourceRange pos, Code code, Constant.IntC e[]) {
    return binaryOp( pos, code, performIntegerPromotion(e[0]), performIntegerPromotion(e[1]) );
  }
};

private final BinEx m_relational = new BinEx(m_shift,      Code.LESS, Code.GREATER, Code.LESS_EQUALS, Code.GREATER_EQUALS);
private final BinEx m_equality   = new BinEx(m_relational, Code.EQUALS_EQUALS, Code.BANG_EQUALS);
private final BinEx m_And        = new BinEx(m_equality,   Code.AMPERSAND);
private final BinEx m_Xor        = new BinEx(m_And,        Code.CARET);
private final BinEx m_Or         = new BinEx(m_Xor,        Code.VERTICAL);
private final BinEx m_logicalAnd = new BinEx(m_Or,         Code.AMPERSAND_AMPERSAND);
private final BinEx m_logicalOR  = new BinEx(m_logicalAnd, Code.VERTICAL_VERTICAL);

private final Constant.IntC conditional_expression ()
{
  Constant.IntC cond = m_logicalOR.parse();
  if (m_tok.code() == Code.QUESTION)
  {
    cond = (Constant.IntC)Constant.convert( Types.integerPromotion( cond.spec ), cond );

    nextExpandNoBlanks();

    Constant.IntC e[] = new Constant.IntC[2];
    e[0] = constant_expression();
    if (need( Code.COLON ))
      e[1] = conditional_expression();
    else
      e[1] = s_zero;

    performUsualArithmeticConversions( e );
    return cond.isTrue() ? e[0] : e[1];
  }
  else
    return cond;
}

private final Constant.IntC constant_expression ()
{
  return conditional_expression();
}

private final boolean parseExpression ()
{
  m_exprErrorReported = false;
  Constant.IntC intC = conditional_expression();
  if (m_exprErrorReported)
    skipUntilEOL();
  return intC.isTrue();
}

private final void _parseIf ( Token tok, String what )
{
  boolean cond = false;

  if (!m_exec)
    skipUntilEOL();
  else
  {
    cond = parseExpression();
    checkEOL( what );
  }

  assert m_tok.code() == Code.EOF || m_tok.code() == Code.NEWLINE;

  pushIfState( tok, IfState.BLOCK_IF, cond, m_exec && cond );
}


private final void parseIf ()
{
  Token tok = m_tok.clone();

  nextExpandNoBlanks(); // consume the if
  _parseIf( tok, "if" );
}

private final void parseElif ()
{
  Token tok = m_tok.clone();

  if (m_ifTop.blockType != IfState.BLOCK_IF)
  {
    m_reporter.error( m_tok, "#elif without #if" );
    if (m_ifTop.blockType == IfState.BLOCK_NONE)
    {
      // At top level treat it as a regular if for error recovery
      nextExpandNoBlanks(); // consume the elif
      _parseIf( tok, "elif" );
    }
    else
      skipUntilEOL();
    return;
  }

  nextExpandNoBlanks(); // consume the elif

  boolean cond = false;

  if (!m_ifTop.parentExec)
    skipUntilEOL();
  else
  {
    cond = parseExpression();
    checkEOL( "elif" );
  }

  assert m_tok.code() == Code.EOF || m_tok.code() == Code.NEWLINE;

  m_exec = m_ifTop.parentExec && !m_ifTop.cond && cond;
  m_ifTop.tok = tok;
  m_ifTop.cond |= cond;
}

private final void parseErrorDirective ()
{
  SourceRange pos = new SourceRange( m_tok );
  StringBuilder msg = new StringBuilder();

  nextNoBlanks();
  while (m_tok.code() != Code.NEWLINE && m_tok.code() != Code.EOF)
  {
    msg.append( m_tok.outputString() );
    nextWithBlanks();
  }

  m_reporter.error( m_tok, "#error %s", msg.toString() );
}

private final void parsePragmaDirective ()
{
  m_reporter.warning( m_tok, "Ignoring unsupported #pragma" );
  skipUntilEOL();
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
          if (m_exec)
            { parseDefine(); return; }
          break;

        case UNDEF:
          if (m_exec)
            { parseUndef(); return; }
          break;

        case LINE:
          if (m_exec)
            { parseLine(); return; }
          break;

        case INCLUDE:
          if (m_exec)
            { parseInclude(); return; }
          break;

        case ERROR:
          if (m_exec)
            { parseErrorDirective(); return; }
          break;

        case PRAGMA:
          if (m_exec)
            { parsePragmaDirective(); return; }
          break;

        case IF:
          parseIf();
          return;

        case ELIF:
          parseElif();
          return;

        case IFDEF:
        case IFNDEF:
          parseIfdef();
          return;

        case ELSE:
          parseElse();
          return;

        case ENDIF:
          parseEndif();
          return;
        }
      }
    }
    break;

  case INT_NUMBER:
    if (m_exec)
      { parseLineMarker(); return; }
    break;
  }

  if (m_exec)
    m_reporter.error( m_tok, "Invalid preprocessor directive #%s", m_tok.outputString() );
  discardLine();
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

    Token res = new Token();
    res.setStringConst( Utils.asciiString( bo.toByteArray() ) );
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

private final boolean expandBuiltin ( ISourceRange pos, Macro macro )
{
  Token tok = new Token();

  switch (macro.builtin)
  {
  case __LINE__:
    tok.setIntConst( pos.getLine1() );
    break;
  case __FILE__:
    tok.setStringConst( pos.getFileName() );
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

    while (nextExpandWithBlanks().code() != Code.EOF)
      expanded.addLastClone( m_tok );
  }
  finally
  {
    m_tok = saveTok;
    tokens.removeLast(); // Remove the EOF we added
  }

  return expanded;
}

private boolean m_lineBeg = true;

public final Token nextToken ()
{
  for(;;)
  {
    if (m_lineBeg)
    {
      // This craps out when we are compensating for merged new lines after a #line directive
      // see parseLine()
      // assert m_ctx == null;
       m_lineBeg = false;

      if (nextNoBlanks().code() == Code.HASH)
        parseDirective();
      else
      {
        if (m_exec)
          curExpandWithBlanks();
        else
          discardLine();
      }
    }
    else
      nextExpandWithBlanks();

    if (m_tok.code() == Code.NEWLINE)
      m_lineBeg = true;
    else if (m_tok.code() == Code.EOF)
    {
      m_lineBeg = true;
      m_lex.close();
      if (popSource())
        continue;
      else
        break;
    }

    if (m_exec)
      return m_tok;
  }

  if (m_ifTop.blockType != IfState.BLOCK_NONE)
  {
    assert m_ifTop.tok != null;
    m_reporter.error( m_ifTop.tok, "Unterminated #%s", m_ifTop.tok.outputString() );
  }
  return m_tok;
}

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

} // class
