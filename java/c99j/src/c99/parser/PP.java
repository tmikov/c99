package c99.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

import c99.CompilerLimits;
import c99.IErrorReporter;
import c99.ISourceRange;
import c99.SourceRange;
import c99.Utils;

public class PP
{
public static enum TokenCode
{
  EOF,
  WHITESPACE,
  NEWLINE,
  COMMENT,
  IDENT,
  PP_NUMBER,
  CHAR_CONST,
  STRING_CONST,

  L_BRACKET("["), R_BRACKET("]"),
  L_PAREN("("), R_PAREN(")"),
  L_CURLY("{"), R_CURLY("}"),
  FULLSTOP("."),
  MINUS_GREATER("->"),

  PLUS_PLUS("++"),
  MINUS_MINUS("--"),
  AMPERSAND("&"),
  ASTERISK("*"),
  PLUS("+"),
  MINUS("-"),
  TILDE("~"),
  BANG("!"),

  SLASH("/"),
  PERCENT("%"),
  LESS_LESS("<<"),
  GREATER_GREATER(">>"),
  LESS("<"),
  GREATER(">"),
  LESS_EQUALS("<="),
  GREATER_EQUALS(">="),
  EQUALS_EQUALS("=="),
  BANG_EQUALS("!="),
  CARET("^"),
  VERTICAL("|"),
  AMPERSAND_AMPERSAND("&&"),
  VERTICAL_VERTICAL("||"),

  QUESTION("?"),
  COLON(":"),
  SEMICOLON(";"),
  ELLIPSIS("..."),

  EQUALS("="),
  ASTERISK_EQUALS("*="),
  SLASH_EQUALS("/="),
  PERCENT_EQUALS("%="),
  PLUS_EQUALS("+="),
  MINUS_EQUALS("-="),
  LESS_LESS_EQUALS("<<="),
  GREATER_GREATER_EQUALS(">>="),
  AMPERSAND_EQUALS("&="),
  CARET_EQUALS("^="),
  VERTICAL_EQUALS("|="),

  COMMA(","),
  HASH("#"),
  HASH_HASH("##"),

  OTHER,

  /** Macro parameter reference*/
  MACRO_PARAM,
  /** '##' token */
  CONCAT;

  public final String printable;

  private TokenCode ()
  {
    printable = null;
  }

  private TokenCode ( String printable )
  {
    this.printable = printable;
  }
}

public static abstract class Token implements Cloneable
{
  public TokenCode code;

  public Token ( final TokenCode code )
  {
    this.code = code;
  }

  @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
  public abstract Token clone ();

  public abstract boolean same ( Token tok );

  public abstract int length ();

  @Override
  public String toString ()
  {
    return "Token{" +
           "code=" + code +
           "} " + super.toString();
  }

  public void output ( PrintStream out ) throws IOException
  {}
}

/**
 *
 */
public static class PunctuatorToken extends Token
{
  public PunctuatorToken ( final TokenCode code )
  {
    super(code);
  }

  public PunctuatorToken ()
  {
    super( null );
  }

  @SuppressWarnings("CloneDoesntCallSuperClone")
  @Override
  public Token clone ()
  {
    return new PunctuatorToken(this.code);
  }

  @Override
  public final boolean same ( final Token tok )
  {
    return this.code == tok.code;
  }

  @Override
  public int length ()
  {
    return code.printable.length();
  }

  public void output ( PrintStream out ) throws IOException
  {
    out.print( code.printable );
  }
}

public static class EOFToken extends Token
{
  public EOFToken ()
  {
    super( TokenCode.EOF );
  }

  @SuppressWarnings("CloneDoesntCallSuperClone")
  @Override
  public Token clone ()
  {
    return new EOFToken();
  }

  @Override
  public final boolean same ( final Token tok )
  {
    return this.code == tok.code;
  }

  @Override
  public int length ()
  {
    return 0;
  }

/*
  @Override
  public void output ( final PrintStream out ) throws IOException
  {
    out.println();
  }
*/
}

public static class NewLineToken extends Token
{
  public NewLineToken ()
  {
    super( TokenCode.NEWLINE );
  }

  @SuppressWarnings("CloneDoesntCallSuperClone")
  @Override
  public Token clone ()
  {
    return new NewLineToken();
  }

  @Override
  public final boolean same ( final Token tok )
  {
    return this.code == tok.code;
  }

  @Override
  public int length ()
  {
    return 0;
  }

/*
  @Override
  public void output ( final PrintStream out ) throws IOException
  {
    out.println();
  }
*/
}

public static class WhiteSpaceToken extends Token
{
  private WhiteSpaceToken ( final TokenCode code )
  {
    super(code);
  }

  @Override
  public int length ()
  {
    return 1;
  }

  public WhiteSpaceToken ()
  {
    super( TokenCode.WHITESPACE );
  }

  @Override
  public final boolean same ( final Token tok )
  {
    return this.code == tok.code;
  }

  @SuppressWarnings("CloneDoesntCallSuperClone")
  @Override
  public Token clone ()
  {
    return new WhiteSpaceToken(this.code);
  }

  public void output ( PrintStream out )
  {
    out.write( ' ' );
  }
}

public static class IdentToken extends Token
{
  public Symbol symbol;

  public IdentToken ()
  {
    super( TokenCode.IDENT );
  }

  public IdentToken ( TokenCode code, Symbol symbol )
  {
    super(code);
    this.symbol = symbol;
  }

  @SuppressWarnings("CloneDoesntCallSuperClone")
  @Override
  public Token clone ()
  {
    return new IdentToken(this.code, this.symbol);
  }

  @Override
  public final boolean same ( final Token tok )
  {
    return this.code == tok.code;
  }

  @Override
  public int length ()
  {
    return symbol.length();
  }

  @Override
  public void output ( final PrintStream out ) throws IOException
  {
    out.write( symbol.bytes );
  }
}

public static class PPNumberToken extends Token
{
  public final byte[] text;
  public int count;

  public PPNumberToken ()
  {
    super( TokenCode.PP_NUMBER );
    this.text = new byte[32];
  }

  public PPNumberToken ( byte[] buf, int from, int count )
  {
    super( TokenCode.PP_NUMBER );
    this.text = Arrays.copyOfRange( buf, from, from + count );
    this.count = count;
  }

  @Override
  public boolean same ( Token tok )
  {
    if (tok.code != this.code)
      return false;
    PPNumberToken t = (PPNumberToken)tok;
    return t.count == this.count && Utils.equals( this.text, 0, t.text, 0, this.count );
  }

  @Override
  public int length ()
  {
    return this.count;
  }

  private void setText ( byte[] buf, int from, int count )
  {
    assert count < text.length;
    System.arraycopy(buf, from, this.text, 0, count );
    this.count = count;
  }

  @SuppressWarnings("CloneDoesntCallSuperClone")
  @Override
  public Token clone ()
  {
    return new PPNumberToken( this.text, 0, this.count );
  }

  @Override
  public void output ( final PrintStream out )
  {
    out.write( text, 0, count );
  }
}

public static class CharConstToken extends Token
{
  public static final int MAX_LEN = 32;

  public final byte[] text;
  public int count;

  public CharConstToken ()
  {
    super(TokenCode.CHAR_CONST);
    this.text = new byte[MAX_LEN];
  }

  public CharConstToken ( byte[] buf, int from, int count )
  {
    super(TokenCode.CHAR_CONST);
    this.text = Arrays.copyOfRange( buf, from, from + count );
  }

  @Override
  public boolean same ( Token tok )
  {
    if (tok.code != this.code)
      return false;
    PPNumberToken t = (PPNumberToken)tok;
    return t.count == this.count && Utils.equals( this.text, 0, t.text, 0, this.count );
  }

  @Override
  public int length ()
  {
    return this.count;
  }

  private void setText ( byte[] buf, int from, int count )
  {
    assert count < MAX_LEN;
    System.arraycopy( buf, from, this.text, 0, this.count = count );
  }

  @SuppressWarnings("CloneDoesntCallSuperClone")
  @Override
  public Token clone ()
  {
    return new CharConstToken( this.text, 0, this.count );
  }

  @Override
  public void output ( final PrintStream out )
  {
    out.write( text, 0, count );
  }
}

public static class StringConstToken extends Token
{
  public byte[] text;
  public int count;

  public StringConstToken ()
  {
    super(TokenCode.STRING_CONST);
  }

  public StringConstToken ( byte[] buf, int from, int count )
  {
    super(TokenCode.STRING_CONST);
    setText( buf, from, count );
  }

  @Override
  public boolean same ( Token tok )
  {
    if (tok.code != this.code)
      return false;
    PPNumberToken t = (PPNumberToken)tok;
    return t.count == this.count && Utils.equals( this.text, 0, t.text, 0, this.count );
  }

  @Override
  public int length ()
  {
    return this.count;
  }

  private void setText ( byte[] buf, int from, int count )
  {
    this.count = count;
    text = Arrays.copyOfRange( buf, from, from + count );
  }

  @SuppressWarnings("CloneDoesntCallSuperClone")
  @Override
  public Token clone ()
  {
    return new StringConstToken( this.text, 0, this.count );
  }

  @Override
  public void output ( final PrintStream out )
  {
    out.write( text, 0, count );
  }
}

public static class OtherToken extends Token
{
  public int value;
  public OtherToken ()
  {
    super( TokenCode.OTHER );
  }

  public OtherToken ( int value )
  {
    super( TokenCode.OTHER );
    this.value = value;
  }

  @Override
  public boolean same ( Token tok )
  {
    return tok.code == this.code && ((OtherToken)tok).value == this.value;
  }

  @Override
  public int length ()
  {
    return 1;
  }

  @SuppressWarnings("CloneDoesntCallSuperClone")
  @Override
  public Token clone ()
  {
    return new OtherToken( this.value );
  }

  @Override
  public void output ( final PrintStream out ) throws IOException
  {
    out.write( value );
  }
}

private static final class IncludeEntry
{
  private LineReader reader;
  private String fileName;
  private int line, col;

  private IncludeEntry ( final LineReader reader, final String fileName, final int line,
                         final int col )
  {
    this.reader = reader;
    this.fileName = fileName;
    this.line = line;
    this.col = col;
  }
}

private final ArrayList<IncludeEntry> m_includeStack = new ArrayList<IncludeEntry>( CompilerLimits.MAX_INCLUDE_DEPTH );

private final SymTable m_symTable = new SymTable();
private final IErrorReporter m_reporter;
private String m_fileName;
private LineReader m_reader;
private int m_start, m_end;
private int m_cur;

private final Token m_tokEOF = new EOFToken();
private final PunctuatorToken m_tokPunkt = new PunctuatorToken();
private final NewLineToken m_tokNewLine = new NewLineToken();
private final WhiteSpaceToken m_tokSpace = new WhiteSpaceToken();
private final IdentToken m_tokIdent = new IdentToken();
private final PPNumberToken m_tokNumber = new PPNumberToken();
private final CharConstToken m_tokCharConst = new CharConstToken();
private final StringConstToken m_tokStringConst = new StringConstToken();
private final OtherToken m_tokOther = new OtherToken();

private Token m_tok;
private final SourceRange m_tokRange = new SourceRange();

private final Symbol m_sym_VA_ARGS;

public PP ( final IErrorReporter reporter, String fileName, InputStream input )
{
  m_reporter = reporter;
  m_fileName = fileName;
  m_reader = new LineReader( input, 16384 );
  m_start = m_end = m_cur = 0;

  m_tokRange.setFileName( m_fileName );

  for ( PPSymCode ppCode : PPSymCode.values() )
  {
    Symbol sym = m_symTable.symbol( ppCode.name );
    assert sym.ppCode == null;
    sym.ppCode = ppCode;
  }

  m_sym_VA_ARGS = m_symTable.symbol( PPSymCode.VA_ARGS.name );
  assert m_sym_VA_ARGS.ppCode == PPSymCode.VA_ARGS;
}

public ISourceRange lastSourceRange ()
{
  return m_tokRange;
}

private boolean isSpace ( int c )
{
  return c == 32 || c == 9 || c == 11 || c == 12;
}

private boolean isAnySpace ( int c )
{
  return c == 32 || c >= 9 && c <= 13;
}

private boolean isNewLine ( int c )
{
  return c == 10 || c == 13;
}

private boolean isIdentStart ( int c )
{
  return c == '_' || (c|32) >= 'a' && (c|32) <= 'z';
}

private boolean isIdentBody ( int c )
{
  return c == '_' || c >= '0' && c <= '9' || (c|32) >= 'a' && (c|32) <= 'z';
}

private boolean isDigit ( int c )
{
  return c >= '0' && c <= '9';
}

private boolean nextLine ()
{
  if (!m_reader.readNextLine())
  {
    m_cur = m_end;
    return false;
  }

  int start;
  int end;

  for(;;) // Loop concatenating lines
  {
    start = m_reader.getLineStart();
    end = m_reader.getLineEnd() - 1;
    byte[] buf = m_reader.getLineBuf();

    // Skip the CR/LF and any trailing spaces before that
    while (end >= start && isAnySpace( buf[end] ))
      --end;

    // Check for line concatenation
    if (end >= start && buf[end] == '\\')
    {
      // Check if there are spaces between the '\' and end of line
      if (end+1 < m_reader.getLineEnd() && isSpace( buf[end+1] ))
      {
        m_reporter.warning(
            m_reader.calcRange( end, end + 1, m_tokRange ),
            "backslash and newline separated by space"
        );
      }

      // Erase the '\'
      m_reader.appendNextLine( end );
    }
    else
      break;
  }

  m_cur = m_start = start;
  m_end = end + 1;

  return true;
}

private void parseBlockComment ( int cur )
{
  byte [] buf;
  int end;

outerLoop:
  for(;;)
  {
    buf = m_reader.getLineBuf();
    end = m_end;

    int state = 0;

    while (cur < end)
    {
      if (state == 0)
      {
        if (buf[cur++] == '*')
          state = 1;
      }
      else
      {
        switch (buf[cur++])
        {
        case '/':
          break outerLoop;
        case '*': break;
        default: state = 1; break;
        }
      }
    }

    // A line has been consumed. Get the next one
    //
    if (!nextLine())
    {
      cur = m_end;
      m_reader.calcRangeEnd( cur, m_tokRange );
      m_reporter.error( m_tokRange, "Unterminated block comment" );
      break;
    }
  }

  m_reader.calcRangeEnd( cur, m_tokRange );
  m_cur = cur;
  m_tokSpace.code = TokenCode.COMMENT;
  m_tok = m_tokSpace;
}

private int parseCharConst ( int cur )
{
  int end = m_end;
  byte[] buf = m_reader.getLineBuf();

  boolean closed = false;
loop:
  while (cur < end)
    switch (buf[cur++])
    {
    case '\\': if (cur < end) ++cur; break;
    case '\'': closed = true; break loop;
    }

  m_reader.calcRangeEnd( cur, m_tokRange );

  if (!closed)
    m_reporter.error( m_tokRange, "Unterminated character constant" );

  int len = cur - m_cur;
  if (len > CharConstToken.MAX_LEN)
  {
    m_reporter.warning( m_reader.calcRange( m_cur, cur, m_tokRange ),
                        "Character constant is too long" );
    len = CharConstToken.MAX_LEN;
  }
  m_tokCharConst.setText( buf, m_cur, len );
  m_tok = m_tokCharConst;

  return cur;
}

private int parseStringConst ( int cur )
{
  int end = m_end;
  byte[] buf = m_reader.getLineBuf();

  boolean closed = false;
loop:
  while (cur < end)
    switch (buf[cur++])
    {
    case '\\': if (cur < end) ++cur; break;
    case '"': closed = true; break loop;
    }

  m_reader.calcRangeEnd( cur, m_tokRange );

  if (!closed)
    m_reporter.error( m_tokRange, "Unterminated string constant" );

  m_tokStringConst.setText( buf, m_cur, cur-m_cur );
  m_tok = m_tokStringConst;

  return cur;
}

private final int parsePunctuator ( int cur )
{
  int end = m_end;
  byte[] buf = m_reader.getLineBuf();
  TokenCode code;

  switch (buf[cur])
  {
    case '[': code = TokenCode.L_BRACKET; ++cur; break;
    case ']': code = TokenCode.R_BRACKET; ++cur; break;
    case '(': code = TokenCode.L_PAREN; ++cur; break;
    case ')': code = TokenCode.R_PAREN; ++cur; break;
    case '{': code = TokenCode.L_CURLY; ++cur; break;
    case '}': code = TokenCode.R_CURLY; ++cur; break;
    case '~': code = TokenCode.TILDE; ++cur; break;
    case '?': code = TokenCode.QUESTION; ++cur; break;
    case ';': code = TokenCode.SEMICOLON; ++cur; break;
    case ',': code = TokenCode.COMMA; ++cur; break;

    case '.': // ., ...
      if (cur+2 < end && buf[cur+1] == '.' && buf[cur+2] == '.')
      {
        code = TokenCode.ELLIPSIS;
        cur += 3;
      }
      else
      {
        code = TokenCode.FULLSTOP;
        ++cur;
      }
      break;

    case '-': // -, --, ->, -=
      if (++cur < end)
        switch (buf[cur])
        {
          case '-':  code = TokenCode.MINUS_MINUS; ++cur; break;
          case '>':  code = TokenCode.MINUS_GREATER; ++cur; break;
          case '=':  code = TokenCode.MINUS_EQUALS; ++cur; break;
          default:   code = TokenCode.MINUS; break;
        }
      else
        code = TokenCode.MINUS;
      break;
    case '+': // +, ++, +=
      if (++cur < end)
        switch (buf[cur])
        {
          case '+': code = TokenCode.PLUS_PLUS; ++cur; break;
          case '=': code = TokenCode.PLUS_EQUALS; ++cur; break;
          default:  code = TokenCode.PLUS; break;
        }
      else
        code = TokenCode.PLUS;
      break;
    case '&': // &, &&, &=
      if (++cur < end)
        switch (buf[cur])
        {
          case '&': code = TokenCode.AMPERSAND_AMPERSAND; ++cur; break;
          case '=': code = TokenCode.AMPERSAND_EQUALS; ++cur; break;
          default:  code = TokenCode.AMPERSAND; break;
        }
      else
        code = TokenCode.AMPERSAND;
      break;
    case '|': // |, ||, |=
      if (++cur < end)
        switch (buf[cur])
        {
          case '|': code = TokenCode.VERTICAL_VERTICAL; ++cur; break;
          case '=': code = TokenCode.VERTICAL_EQUALS; ++cur; break;
          default:  code = TokenCode.VERTICAL; break;
        }
      else
        code = TokenCode.VERTICAL;
      break;

    case '%': // %, %=, %>, %:, %:%:
      if (++cur < end)
        switch (buf[cur])
        {
          case '=': code = TokenCode.PERCENT_EQUALS; ++cur; break;
          case '>': code = TokenCode.R_CURLY; ++cur; break;
          case ':':
            if (cur + 2 < end && buf[cur+1] == '%' && buf[cur+2] == ':')
            {
              code = TokenCode.HASH_HASH;
              cur += 3;
              break;
            }
            // else fall-through
          default: code = TokenCode.PERCENT; break;
        }
      else
        code = TokenCode.PERCENT;
      break;

    case '*': // *, *=
      if (++cur < end && buf[cur] == '=')
      {
        code = TokenCode.ASTERISK_EQUALS;
        ++cur;
      }
      else
        code = TokenCode.ASTERISK;
      break;
    case '!': // !, !=
      if (++cur < end && buf[cur] == '=')
      {
        code = TokenCode.BANG_EQUALS;
        ++cur;
      }
      else
        code = TokenCode.BANG;
      break;
    case '/': // /, /=
      if (++cur < end && buf[cur] == '=')
      {
        code = TokenCode.SLASH_EQUALS;
        ++cur;
      }
      else
        code = TokenCode.SLASH;
      break;
    case '=': // =, ==
      if (++cur < end && buf[cur] == '=')
      {
        code = TokenCode.EQUALS_EQUALS;
        ++cur;
      }
      else
        code = TokenCode.EQUALS;
      break;
    case '^': // ^, ^=
      if (++cur < end && buf[cur] == '=')
      {
        code = TokenCode.CARET_EQUALS;
        ++cur;
      }
      else
        code = TokenCode.CARET;
      break;
    case ':': // :, :>
      if (++cur < end && buf[cur] == '>')
      {
        code = TokenCode.R_BRACKET;
        ++cur;
      }
      else
        code = TokenCode.COLON;
      break;
    case '#': // #, ##
      if (++cur < end && buf[cur] == '#')
      {
        code = TokenCode.HASH_HASH;
        ++cur;
      }
      else
        code = TokenCode.HASH;
      break;

    case '<': // <, <=, <:, <%, <<, <<=
      if (++cur < end)
        switch (buf[cur])
        {
          case '=': code = TokenCode.LESS_EQUALS; ++cur; break;
          case ':': code = TokenCode.L_BRACKET; ++cur; break;
          case '%': code = TokenCode.L_CURLY; ++cur; break;
          case '<':
            if (cur+1 < end && buf[cur+1] == '=')
            {
              code = TokenCode.LESS_LESS_EQUALS;
              cur += 2;
            }
            else
            {
              code = TokenCode.LESS_LESS;
              ++cur;
            }
            break;
          default: code = TokenCode.LESS; break;
        }
      else
        code = TokenCode.LESS;
      break;
    case '>': // >, >=, >>, >>=
      if (++cur < end)
        switch (buf[cur])
        {
          case '=': code = TokenCode.GREATER_EQUALS; ++cur; break;
          case '>':
            if (cur+1 < end && buf[cur+1] == '=')
            {
              code = TokenCode.GREATER_GREATER_EQUALS;
              cur += 2;
            }
            else
            {
              code = TokenCode.GREATER_GREATER;
              ++cur;
            }
            break;
          default: code = TokenCode.GREATER; break;
        }
      else
        code = TokenCode.GREATER;
      break;

    default:
      m_tokOther.value = buf[cur++] & 0xFF;
      m_tok = m_tokOther;
      return cur;
  }

  m_tokPunkt.code = code;
  m_tok = m_tokPunkt;
  return cur;
}

private final void innerNextToken ()
{
  if (m_cur == m_end)
  {
    if (!nextLine())
    {
      m_tokRange.setLocation( m_reader.getCurLineNumber(), 1 );
      m_tok = m_tokEOF;
      return;
    }
    else
    {
      m_reader.calcRange( m_cur, m_cur, m_tokRange );
      m_tok = m_tokNewLine;
      return;
    }
  }

  m_reader.calcRangeStart( m_cur, m_tokRange );

  int cur = m_cur;
  int end = m_end;

  assert cur < end;

  byte[] buf = m_reader.getLineBuf();

  // Whitespace
  //
  if (isAnySpace( buf[cur] ))
  {
    do
      ++cur;
    while (isAnySpace( buf[cur] ));

    m_tokSpace.code = TokenCode.WHITESPACE;
    m_tok = m_tokSpace;
  }
  // Block comment
  else if (buf[cur] == '/' && cur + 1 < end && buf[cur+1] == '*')
  {
    parseBlockComment( cur + 2 );
    return;
  }
  // Line comment
  //
  else if (buf[cur] == '/' && cur + 1 < end && buf[cur+1] == '/')
  {
    cur = end;
    m_tokSpace.code = TokenCode.COMMENT;
    m_tok = m_tokSpace;
  }
  // Ident
  //
  else if (isIdentStart( buf[cur] ))
  {
    do
      ++cur;
    while (isIdentBody( buf[cur] ));

    m_tokIdent.symbol = m_symTable.symbol( buf, m_cur, cur - m_cur );
    m_tok = m_tokIdent;
  }
  // pp-number
  //
  else if (isDigit( buf[cur] ) || buf[cur] == '.' && cur+1 < end && isDigit( buf[cur+1] ))
  {
    while (++cur < end)
    {
      if (isDigit( buf[cur] ) ||
          isIdentStart( buf[cur] ) ||
          buf[cur] == '.')
      {}
      else if (((buf[cur] | 32) == 'e' || (buf[cur] | 32) == 'p') &&
               cur+1 < end &&
               (buf[cur]+1 == '+' || buf[cur]+1 == '-'))
      {
        ++cur;
      }
      else
        break;
    }

    m_tokNumber.setText( buf, m_cur, cur - m_cur );
    m_tok = m_tokNumber;
  }
  // Character constant
  else if (buf[cur] == '\'')
  {
    cur = parseCharConst( cur + 1 );
  }
  else if ((buf[cur] == 'L' || buf[cur] == 'u' || buf[cur] == 'U') && cur+1 < end && buf[cur+1] == '\'')
  {
    cur = parseCharConst( cur + 2 );
  }
  // String constant
  else if (buf[cur] == '"')
  {
    cur = parseStringConst( cur + 1 );
  }
  else if ((buf[cur] == 'L' || buf[cur] == 'u' || buf[cur] == 'U') && cur+1 < end && buf[cur+1] == '"')
  {
    cur = parseStringConst( cur + 2 );
  }
  else
  // Punctuators
  //
  {
    cur = parsePunctuator( cur );
  }

  m_reader.calcRangeEnd( cur, m_tokRange );
  m_cur = cur;
}

private static final class Macro
{
  public final SourceRange nameLoc = new SourceRange();
  public final SourceRange bodyLoc = new SourceRange();
  public final Symbol name;
  public boolean funcLike;
  public boolean variadic;

  public final ArrayList<ParamDecl> params = new ArrayList<ParamDecl>();
  public final LinkedList<Token> body = new LinkedList<Token>();

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

    Iterator<Token> t1 = this.body.iterator();
    Iterator<Token> t2 = m.body.iterator();
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

private static final class ParamToken extends Token
{
  public final ParamDecl param;
  public boolean stringify;

  private ParamToken ( final ParamDecl param )
  {
    super( TokenCode.MACRO_PARAM );
    this.param = param;
  }

  @SuppressWarnings("CloneDoesntCallSuperClone")
  @Override
  public Token clone ()
  {
    return this;
  }

  @Override
  public boolean same ( final Token tok )
  {
    return this.code == tok.code && this.param.same( ((ParamToken)tok).param );
  }

  @Override
  public int length ()
  {
    return this.param.symbol.length();
  }
}

private static final class ConcatToken extends Token
{
  private Token left, right;

  public ConcatToken ( Token left, Token right )
  {
    super(TokenCode.CONCAT);
    this.left = left;
    this.right = right;
  }

  @SuppressWarnings("CloneDoesntCallSuperClone")
  @Override
  public Token clone ()
  {
    return new ConcatToken( this.left.clone(), this.right.clone() );
  }

  @Override
  public boolean same ( final Token tok )
  {
    return this.code == tok.code &&
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
}

private final void nextNoBlanks ()
{
  do
    innerNextToken();
  while (m_tok.code == TokenCode.WHITESPACE || m_tok.code == TokenCode.COMMENT);
}

private final void nextNoNewLineOrBlanks ()
{
  do
    innerNextToken();
  while (m_tok.code == TokenCode.WHITESPACE || m_tok.code == TokenCode.COMMENT || m_tok.code == TokenCode.NEWLINE);
}

private final int sNORMAL = 0;
private final int sLINEBEG = 1;

private int m_state = sLINEBEG;

private final void skipBlanks ()
{
  while (m_tok.code == TokenCode.WHITESPACE || m_tok.code == TokenCode.COMMENT)
    innerNextToken();
}

private final void skipUntilEOL ()
{
  while (m_tok.code != TokenCode.NEWLINE && m_tok.code != TokenCode.EOF)
    innerNextToken();
}

private final boolean parseMacroParamList ( Macro macro )
{
  nextNoBlanks(); // consume the '('

  if (m_tok.code != TokenCode.R_PAREN)
  {
    for(;;)
    {
      if (m_tok.code == TokenCode.IDENT)
      {
        Symbol sym = ((IdentToken)m_tok).symbol;
        if (sym.ppDecl instanceof ParamDecl)
        {
          m_reporter.error( m_tokRange, "Duplicated macro parameter '%s'", sym.name );
          skipUntilEOL();
          return false;
        }

        macro.params.add( new ParamDecl( sym, macro.params.size() ) );

        nextNoBlanks();
        if (m_tok.code == TokenCode.R_PAREN)
          break;
        else if (m_tok.code == TokenCode.COMMA)
          nextNoBlanks();
/* TODO: GCC extension for variadic macros "macro(args...)"
        else if (m_tok.code == TokenCode.ELLIPSIS)
          {}
*/
        else
        {
          m_reporter.error(  m_tokRange, "Expected ',', ')', '...' or an identifier in macro parameter list" );
          skipUntilEOL();
          return false;
        }
      }
      else if (m_tok.code == TokenCode.ELLIPSIS)
      {
        macro.variadic = true;
        macro.params.add( new ParamDecl( m_sym_VA_ARGS, macro.params.size() ) );
        nextNoBlanks();

        if (m_tok.code == TokenCode.R_PAREN)
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
        if (m_tok.code == TokenCode.EOF || m_tok.code == TokenCode.NEWLINE)
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
  if (tok.code == TokenCode.IDENT)
  {
    Symbol sym = ((IdentToken)tok).symbol;
    if (sym.ppDecl instanceof ParamDecl)
      return (ParamDecl)sym.ppDecl;
  }

  return null;
}

private final SourceRange m_tmpRange = new SourceRange();

private final Token parseMacroReplacementListToken ( Macro macro )
{
  Token tok;
  ParamDecl param;

  if (m_tok.code == TokenCode.HASH)
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
  else if (m_tok.code == TokenCode.IDENT && ((IdentToken)m_tok).symbol == m_sym_VA_ARGS)
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
  Token ws = null;

  macro.bodyLoc.setRange( m_tokRange );

  for ( ; m_tok.code != TokenCode.EOF && m_tok.code != TokenCode.NEWLINE; innerNextToken() )
  {
    if (m_tok.code == TokenCode.WHITESPACE || m_tok.code == TokenCode.COMMENT)
    {
      if (ws == null)
        ws = new WhiteSpaceToken();
    }
    else
    {
      Token tok;

      if (m_tok.code == TokenCode.HASH_HASH)
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
          if (m_tok.code == TokenCode.EOF || m_tok.code == TokenCode.NEWLINE)
          {
            m_reporter.error( m_tmpRange, "'##' can only occur between two tokens" );
            return false;
          }
        }
        while (m_tok.code == TokenCode.HASH_HASH);

        if ( (tok = parseMacroReplacementListToken( macro )) == null)
          return false;

        tok = new ConcatToken( macro.body.removeLast(), tok );
      }
      else
      {
        if ((tok = parseMacroReplacementListToken( macro )) == null)
          return false;
      }

      if (ws != null)
      {
        macro.body.add( ws );
        ws = null;
      }

      macro.body.addLast( tok );
      macro.bodyLoc.extend( m_tokRange );
    }
  }

  return true;
}

private final void parseDefine ()
{
  nextNoBlanks(); // consume the 'define'

  if (m_tok.code != TokenCode.IDENT)
  {
    m_reporter.error( m_tokRange, "An identifier macro name expected" );
    skipUntilEOL();
    return;
  }

  final Symbol macroSym = ((IdentToken)m_tok).symbol;
  final Macro macro = new Macro( macroSym, m_tokRange );
  try
  {
    innerNextToken();
    if (m_tok.code == TokenCode.L_PAREN)
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

  switch (m_tok.code)
  {
  case NEWLINE:
    return;

  case IDENT:
    {
      Symbol sym = ((IdentToken)m_tok).symbol;
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

public final Token nextToken ()
{
  switch (m_state)
  {
  //case sNORMAL:
  default:
    innerNextToken();
    if (m_tok.code == TokenCode.NEWLINE)
      m_state = sLINEBEG;
    return m_tok;

  case sLINEBEG:
    nextNoBlanks();
    switch (m_tok.code)
    {
    case HASH:
      parseDirective();
      return m_tok;

    default:
      m_state = sNORMAL;
      // Fall-through
    case NEWLINE:
      return m_tok;
    }
  }
}

} // class

