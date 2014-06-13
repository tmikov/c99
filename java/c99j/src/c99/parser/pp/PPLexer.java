package c99.parser.pp;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;

import c99.IErrorReporter;
import c99.SourceRange;
import c99.Utils;
import c99.parser.SymTable;
import c99.parser.Symbol;

/**
 * Preprocessor lexer.
 *
 * <p>Return preprocessor tokens and whitespace (which is important for the preprocessor).
 * We go into some lengths to avoid allocating an object per token. Only character/string
 * constants longer than 32 bytes cause an allocation.
 *
 * <p>While this is reasonably efficient, it could be optimized by using a tool to generate a FSM,
 * which would examine every input character only once. For now it is not worth the trouble though.
 */
public class PPLexer
{
public static enum Code
{
  EOF,
  WHITESPACE(" "),
  NEWLINE,
  COMMENT(" "),
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

  Code ()
  {
    printable = "";
  }

  Code ( String printable )
  {
    this.printable = printable;
  }
}

public abstract static class AbstractToken extends SourceRange implements Cloneable
{
  protected Code m_code;

  @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
  public abstract AbstractToken clone ();

  public abstract boolean same ( AbstractToken tok );

  public abstract int length ();

  public final Code code ()
  {
    return m_code;
  }

  @Override
  public String toString ()
  {
    return "Token{" +
           "code=" + m_code +
           "} " + super.toString();
  }

  public abstract void output ( PrintStream out ) throws IOException;
}

public static class Token extends AbstractToken
{
  private static final int DEFAULT_LEN = 32;

  private final byte[] m_defaultBuf;
  private byte[] m_text;
  private int m_length;

  private Symbol m_symbol;

  public Token ()
  {
    m_defaultBuf = new byte[DEFAULT_LEN];
  }

  public Token ( Code code )
  {
    this();
    m_code = code;
  }

  public Token ( Token tok )
  {
    m_defaultBuf = null;

    m_code = tok.m_code;
    m_length = tok.m_length;

    if (tok.m_symbol != null)
      m_symbol = tok.m_symbol;
    else if (tok.m_text != null)
      m_text = Arrays.copyOfRange( tok.m_text, 0, tok.m_length );

    setRange( tok );
  }

  public void copyFrom ( Token tok )
  {
    reset();
    if (tok.m_symbol != null)
      setSymbol( tok.m_code, tok.m_symbol );
    else if (tok.m_text != null)
      setText( tok.m_code, tok.m_text, 0, tok.m_length );
    else
      setCode( tok.m_code );
  }

  public final void reset ()
  {
    m_code = null;
    m_symbol = null;
    m_text = null;
  }

  public final Symbol symbol ()
  {
    return m_symbol;
  }

  public final void setSymbol ( Code code, Symbol symbol )
  {
    this.m_code = code;
    m_symbol = symbol;
    m_length = symbol.length();
  }

  public final void setText ( Code code, byte[] buf, int from, int count )
  {
    this.m_code = code;
    if (count <= DEFAULT_LEN)
      System.arraycopy( buf, from, m_text = m_defaultBuf, 0, count );
    else
      m_text = Arrays.copyOfRange( buf, from, from + count );
    m_length = count;
  }

  public final void setCode ( Code code )
  {
    m_code = code;
    m_length = code.printable.length();
  }

  @SuppressWarnings("CloneDoesntCallSuperClone")
  @Override
  public Token clone ()
  {
    return new Token(this);
  }

  @Override
  public boolean same ( final AbstractToken _tok )
  {
    if (_tok instanceof Token)
    {
      final Token tok = (Token)_tok;
      return
        m_code == tok.m_code &&
        m_symbol == tok.m_symbol &&
        m_length == tok.m_length &&
        (m_text == null || Utils.equals( m_text, 0, tok.m_text, 0, m_length ));
    }
    else
      return false;
  }

  @Override
  public int length ()
  {
    return m_length;
  }

  public void output ( PrintStream out ) throws IOException
  {
    if (m_symbol != null)
      out.write( m_symbol.bytes );
    else if (m_text != null)
      out.write( m_text, 0, m_length );
    else
      out.print( m_code.printable );
  }
}

protected final SymTable m_symTable;
protected final IErrorReporter m_reporter;
private String m_fileName;
private LineReader m_reader;
private int m_end;
private int m_cur;

private Token m_fifo[];
private int m_fifoHead, m_fifoTail, m_fifoCount, m_fifoCapacity;

private Token m_workTok;
protected Token m_lastTok;

private final SourceRange m_tmpRange = new SourceRange();

public PPLexer ( final IErrorReporter reporter, String fileName, InputStream input,
                 final SymTable symTable )
{
  m_reporter = reporter;
  m_symTable = symTable;
  m_fileName = fileName;
  m_reader = new LineReader( input, 16384 );
  m_end = m_cur = 0;

  m_fifoHead = m_fifoTail = m_fifoCount = 0;
  m_fifoCapacity = 4;
  m_fifo = new Token[m_fifoCapacity];
  for ( int i = 0; i < m_fifoCapacity; ++i )
    m_fifo[i] = new Token();

  m_lastTok = newFifoToken();
}

private final Token newFifoToken ()
{
  if (m_fifoCount == m_fifoCapacity) // Need to enlarge the fifo?
  {
    Token[] newFifo = new Token[m_fifoCapacity << 1];

    final int cap = m_fifoCapacity;
    final int mask = cap - 1;
    int head = m_fifoHead;
    int i;
    for ( i = 0; i < cap; ++i )
    {
      newFifo[i] = m_fifo[head];
      head = (head + 1) & mask;
    }
    for ( ; i < newFifo.length; ++i )
      newFifo[i] = new Token();

    m_fifo = newFifo;
    m_fifoCapacity <<= 1;
    m_fifoHead = 0;
    m_fifoTail = m_fifoCount;
  }

  Token tok = m_fifo[m_fifoTail];
  m_fifoTail = (m_fifoTail + 1) & (m_fifoCapacity - 1);
  ++m_fifoCount;
  return tok;
}

private final void releaseFifoToken ( Token tok )
{
  assert tok == m_fifo[m_fifoHead];
  assert m_fifoCount > 0;
  tok.reset();
  m_fifoHead = (m_fifoHead + 1) & (m_fifoCapacity - 1);
  --m_fifoCount;
}

private final int getFifoCount ()
{
  return m_fifoCount;
}

private final Token getFifoHead ()
{
  assert m_fifoCount > 0;
  return m_fifo[m_fifoHead];
}

private final Token getFifoToken ( int headOffset )
{
  assert headOffset < m_fifoCount;
  return m_fifo[(m_fifoHead + headOffset) & (m_fifoCapacity -1)];
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
            m_reader.calcRange( end, end + 1, m_tmpRange ),
            "backslash and newline separated by space"
        );
      }

      // Erase the '\'
      m_reader.appendNextLine( end );
    }
    else
    {
      buf[end+1] = 0; // Add a sentinel at the end of the line
      break;
    }
  }

  m_cur = start;
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
      m_reader.calcRangeEnd( cur, m_workTok );
      m_reporter.error( m_workTok, "Unterminated block comment" );
      break;
    }
  }

  m_reader.calcRangeEnd( cur, m_workTok );
  m_workTok.setCode( Code.COMMENT );
  m_cur = cur;
}

private void parseCharConst ( int cur )
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

  m_reader.calcRangeEnd( cur, m_workTok );

  if (!closed)
    m_reporter.error( m_workTok, "Unterminated character constant" );

  m_workTok.setText( Code.CHAR_CONST, buf, m_cur, cur - m_cur );
  m_cur = cur;
}

private void parseStringConst ( int cur )
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

  m_reader.calcRangeEnd( cur, m_workTok );

  if (!closed)
    m_reporter.error( m_workTok, "Unterminated string constant" );

  m_workTok.setText( Code.STRING_CONST, buf, m_cur, cur - m_cur );
  m_cur = cur;
}

private final void parsePunctuator ( int cur )
{
  int end = m_end;
  byte[] buf = m_reader.getLineBuf();
  Code code;

  switch (buf[cur])
  {
    case '[': code = Code.L_BRACKET; ++cur; break;
    case ']': code = Code.R_BRACKET; ++cur; break;
    case '(': code = Code.L_PAREN; ++cur; break;
    case ')': code = Code.R_PAREN; ++cur; break;
    case '{': code = Code.L_CURLY; ++cur; break;
    case '}': code = Code.R_CURLY; ++cur; break;
    case '~': code = Code.TILDE; ++cur; break;
    case '?': code = Code.QUESTION; ++cur; break;
    case ';': code = Code.SEMICOLON; ++cur; break;
    case ',': code = Code.COMMA; ++cur; break;

    case '.': // ., ...
      if (cur+2 < end && buf[cur+1] == '.' && buf[cur+2] == '.')
      {
        code = Code.ELLIPSIS;
        cur += 3;
      }
      else
      {
        code = Code.FULLSTOP;
        ++cur;
      }
      break;

    case '-': // -, --, ->, -=
      if (++cur < end)
        switch (buf[cur])
        {
          case '-':  code = Code.MINUS_MINUS; ++cur; break;
          case '>':  code = Code.MINUS_GREATER; ++cur; break;
          case '=':  code = Code.MINUS_EQUALS; ++cur; break;
          default:   code = Code.MINUS; break;
        }
      else
        code = Code.MINUS;
      break;
    case '+': // +, ++, +=
      if (++cur < end)
        switch (buf[cur])
        {
          case '+': code = Code.PLUS_PLUS; ++cur; break;
          case '=': code = Code.PLUS_EQUALS; ++cur; break;
          default:  code = Code.PLUS; break;
        }
      else
        code = Code.PLUS;
      break;
    case '&': // &, &&, &=
      if (++cur < end)
        switch (buf[cur])
        {
          case '&': code = Code.AMPERSAND_AMPERSAND; ++cur; break;
          case '=': code = Code.AMPERSAND_EQUALS; ++cur; break;
          default:  code = Code.AMPERSAND; break;
        }
      else
        code = Code.AMPERSAND;
      break;
    case '|': // |, ||, |=
      if (++cur < end)
        switch (buf[cur])
        {
          case '|': code = Code.VERTICAL_VERTICAL; ++cur; break;
          case '=': code = Code.VERTICAL_EQUALS; ++cur; break;
          default:  code = Code.VERTICAL; break;
        }
      else
        code = Code.VERTICAL;
      break;

    case '%': // %, %=, %>, %:, %:%:
      if (++cur < end)
        switch (buf[cur])
        {
          case '=': code = Code.PERCENT_EQUALS; ++cur; break;
          case '>': code = Code.R_CURLY; ++cur; break;
          case ':':
            if (cur + 2 < end && buf[cur+1] == '%' && buf[cur+2] == ':')
            {
              code = Code.HASH_HASH;
              cur += 3;
              break;
            }
            // else fall-through
          default: code = Code.PERCENT; break;
        }
      else
        code = Code.PERCENT;
      break;

    case '*': // *, *=
      if (++cur < end && buf[cur] == '=')
      {
        code = Code.ASTERISK_EQUALS;
        ++cur;
      }
      else
        code = Code.ASTERISK;
      break;
    case '!': // !, !=
      if (++cur < end && buf[cur] == '=')
      {
        code = Code.BANG_EQUALS;
        ++cur;
      }
      else
        code = Code.BANG;
      break;
    case '/': // /, /=
      if (++cur < end && buf[cur] == '=')
      {
        code = Code.SLASH_EQUALS;
        ++cur;
      }
      else
        code = Code.SLASH;
      break;
    case '=': // =, ==
      if (++cur < end && buf[cur] == '=')
      {
        code = Code.EQUALS_EQUALS;
        ++cur;
      }
      else
        code = Code.EQUALS;
      break;
    case '^': // ^, ^=
      if (++cur < end && buf[cur] == '=')
      {
        code = Code.CARET_EQUALS;
        ++cur;
      }
      else
        code = Code.CARET;
      break;
    case ':': // :, :>
      if (++cur < end && buf[cur] == '>')
      {
        code = Code.R_BRACKET;
        ++cur;
      }
      else
        code = Code.COLON;
      break;
    case '#': // #, ##
      if (++cur < end && buf[cur] == '#')
      {
        code = Code.HASH_HASH;
        ++cur;
      }
      else
        code = Code.HASH;
      break;

    case '<': // <, <=, <:, <%, <<, <<=
      if (++cur < end)
        switch (buf[cur])
        {
          case '=': code = Code.LESS_EQUALS; ++cur; break;
          case ':': code = Code.L_BRACKET; ++cur; break;
          case '%': code = Code.L_CURLY; ++cur; break;
          case '<':
            if (cur+1 < end && buf[cur+1] == '=')
            {
              code = Code.LESS_LESS_EQUALS;
              cur += 2;
            }
            else
            {
              code = Code.LESS_LESS;
              ++cur;
            }
            break;
          default: code = Code.LESS; break;
        }
      else
        code = Code.LESS;
      break;
    case '>': // >, >=, >>, >>=
      if (++cur < end)
        switch (buf[cur])
        {
          case '=': code = Code.GREATER_EQUALS; ++cur; break;
          case '>':
            if (cur+1 < end && buf[cur+1] == '=')
            {
              code = Code.GREATER_GREATER_EQUALS;
              cur += 2;
            }
            else
            {
              code = Code.GREATER_GREATER;
              ++cur;
            }
            break;
          default: code = Code.GREATER; break;
        }
      else
        code = Code.GREATER;
      break;

    default:
      m_workTok.setText( Code.OTHER, buf, cur++, 1 );
      m_cur = cur;
      return;
  }

  m_workTok.setCode( code );
  m_cur = cur;
}

private final void parseNextToken ( Token tok )
{
  m_workTok = tok;
  m_workTok.setFileName( m_fileName );

  if (m_cur == m_end)
  {
    if (!nextLine())
    {
      m_workTok.setLocation( m_reader.getCurLineNumber(), 1 );
      m_workTok.setCode( Code.EOF );
      return;
    }
    else
    {
      m_reader.calcRange( m_cur, m_cur, m_workTok );
      m_workTok.setCode( Code.NEWLINE );
      return;
    }
  }

  m_reader.calcRangeStart( m_cur, m_workTok );

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

    m_workTok.setCode( Code.WHITESPACE );
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
    m_workTok.setCode( Code.COMMENT );
  }
  // Ident
  //
  else if (isIdentStart( buf[cur] ))
  {
    do
      ++cur;
    while (isIdentBody( buf[cur] ));

    m_workTok.setSymbol( Code.IDENT, m_symTable.symbol( buf, m_cur, cur - m_cur ) );
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

    m_workTok.setText( Code.PP_NUMBER, buf, m_cur, cur - m_cur );
  }
  // Character constant
  else if (buf[cur] == '\'')
  {
    parseCharConst( cur + 1 );
    return;
  }
  else if ((buf[cur] == 'L' || buf[cur] == 'u' || buf[cur] == 'U') && cur+1 < end && buf[cur+1] == '\'')
  {
    parseCharConst( cur + 2 );
    return;
  }
  // String constant
  else if (buf[cur] == '"')
  {
    parseStringConst( cur + 1 );
    return;
  }
  else if ((buf[cur] == 'L' || buf[cur] == 'u' || buf[cur] == 'U') && cur+1 < end && buf[cur+1] == '"')
  {
    parseStringConst( cur + 2 );
    return;
  }
  else
  // Punctuators
  //
  {
    parsePunctuator( cur );
    return;
  }

  m_reader.calcRangeEnd( cur, m_workTok );
  m_cur = cur;
}

protected final Token innerNextToken ()
{
  releaseFifoToken( m_lastTok );

  if (getFifoCount() == 0)
    parseNextToken( newFifoToken() );

  return m_lastTok = getFifoHead();
}

protected final Token lookAhead ( int distance )
{
  assert distance >= 0;
  while (getFifoCount() <= distance)
    parseNextToken( newFifoToken() );
  return getFifoToken( distance );
}

} // class
