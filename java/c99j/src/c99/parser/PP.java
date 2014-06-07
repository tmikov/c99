package c99.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

import c99.CompilerLimits;
import c99.IErrorReporter;
import c99.SourceRange;

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
  SHARP("#"),
  SHARP_SHARP("##"),

  OTHER;

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

public static class Token extends SourceRange
{
  public TokenCode code;

  public Token ( final TokenCode code )
  {
    this.code = code;
  }

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

public static class PunctuatorToken extends Token
{
  public PunctuatorToken ()
  {
    super( null );
  }

  public void output ( PrintStream out ) throws IOException
  {
    out.print( code.printable );
  }
}

public static class NewLineToken extends Token
{
  public NewLineToken ()
  {
    super( TokenCode.NEWLINE );
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
  public WhiteSpaceToken ()
  {
    super( TokenCode.WHITESPACE );
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

  @Override
  public void output ( final PrintStream out ) throws IOException
  {
    out.write( symbol.bytes );
  }
}

public static class PPNumberToken extends Token
{
  public final byte[] text = new byte[32];
  public int count;

  public PPNumberToken ()
  {
    super( TokenCode.PP_NUMBER );
  }

  private void setText ( byte[] buf, int from, int count )
  {
    assert count < text.length;
    System.arraycopy( buf, from, this.text, 0, this.count = count );
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

  public final byte[] text = new byte[MAX_LEN];
  public int count;

  public CharConstToken ()
  {
    super(TokenCode.CHAR_CONST);
  }

  private void setText ( byte[] buf, int from, int count )
  {
    assert count < MAX_LEN;
    System.arraycopy( buf, from, this.text, 0, this.count = count );
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

  private void setText ( byte[] buf, int from, int count )
  {
    this.count = count;
    text = Arrays.copyOfRange( buf, from, from + count );
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

private final SourceRange m_tempRng = new SourceRange();

private final Token m_tokEOF = new Token( TokenCode.EOF );
private final PunctuatorToken m_tokPunkt = new PunctuatorToken();
private final NewLineToken m_tokNewLine = new NewLineToken();
private final WhiteSpaceToken m_tokSpace = new WhiteSpaceToken();
private final IdentToken m_tokIdent = new IdentToken();
private final PPNumberToken m_tokNumber = new PPNumberToken();
private final CharConstToken m_tokCharConst = new CharConstToken();
private final StringConstToken m_tokStringConst = new StringConstToken();
private final OtherToken m_tokOther = new OtherToken();

/** Most internal parsing functions store their result here */
private Token m_tok;

public PP ( final IErrorReporter reporter, String fileName, InputStream input )
{
  m_reporter = reporter;
  m_fileName = fileName;
  m_reader = new LineReader( input, 16384 );
  m_start = m_end = m_cur = 0;
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
            m_reader.calcRange( end, end + 1, m_tempRng.setFileName( m_fileName ) ),
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

private Token parseBlockComment ( int cur )
{
  byte [] buf;
  int end;

  m_reader.calcRangeStart( m_cur, m_tempRng );
  m_tokSpace.code = TokenCode.COMMENT;

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
          m_reader.calcRangeEnd( cur, m_tempRng );
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
      m_reader.calcRangeEnd( cur, m_tempRng );
      m_reporter.error( m_tempRng, "Unterminated block comment" );
      break;
    }
  }

  m_tokSpace.setRange( m_tempRng );
  m_cur = cur;
  return m_tokSpace;
}

private int parseCharConst ( int cur )
{
  int end = m_end;
  byte[] buf = m_reader.getLineBuf();

  while (cur < end && buf[cur] != '\'')
    if (buf[cur++] == '\\')
      if (cur < end && buf[cur] == '\'')
        ++cur;

  if (cur < end)
  {
    assert buf[cur] == '\'';
    ++cur;
  }
  else
    m_reporter.error( m_reader.calcRange( m_cur, cur, m_tempRng ), "Unterminated character constant" );

  int len = cur - m_cur;
  if (len > CharConstToken.MAX_LEN)
  {
    m_reporter.error( m_reader.calcRange( m_cur, cur, m_tempRng ),
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

  while (cur < end && buf[cur] != '"')
    if (buf[cur++] == '\\')
      if (cur < end && buf[cur] == '"')
        ++cur;

  if (cur < end)
  {
    assert buf[cur] == '"';
    ++cur;
  }
  else
    m_reporter.error( m_reader.calcRange( m_cur, cur, m_tempRng ), "Unterminated string constant" );

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
              code = TokenCode.SHARP_SHARP;
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
        code = TokenCode.SHARP_SHARP;
        ++cur;
      }
      else
        code = TokenCode.SHARP;
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

public final Token nextToken ()
{
  m_tempRng.setFileName( m_fileName );

  if (m_cur == m_end)
  {
    if (!nextLine())
    {
      m_tokEOF.setLocation( m_fileName, m_reader.getCurLineNumber(), 1 );
      return m_tokEOF;
    }
    else
    {
      m_tokNewLine.setRange( m_reader.calcRange( m_cur, m_cur, m_tempRng ) );
      return m_tokNewLine;
    }
  }

  int cur = m_cur;
  int end = m_end;

  assert cur < end;

  byte[] buf = m_reader.getLineBuf();

  Token res;
  // Whitespace
  //
  if (isAnySpace( buf[cur] ))
  {
    do
      ++cur;
    while (isAnySpace( buf[cur] ));

    res = m_tokSpace;
    res.code = TokenCode.WHITESPACE;
  }
  // Block comment
  else if (buf[cur] == '/' && cur + 1 < end && buf[cur+1] == '*')
  {
    return parseBlockComment( cur + 2 );
  }
  // Line comment
  //
  else if (buf[cur] == '/' && cur + 1 < end && buf[cur+1] == '/')
  {
    cur = end;
    res = m_tokSpace;
    res.code = TokenCode.COMMENT;
  }
  // Ident
  //
  else if (isIdentStart( buf[cur] ))
  {
    do
      ++cur;
    while (isIdentBody( buf[cur] ));

    m_tokIdent.symbol = m_symTable.symbol( buf, m_cur, cur - m_cur );
    res = m_tokIdent;
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
    res = m_tokNumber;
  }
  // Character constant
  else if (buf[cur] == '\'')
  {
    cur = parseCharConst( cur + 1 );
    res = m_tok;
  }
  else if ((buf[cur] == 'L' || buf[cur] == 'u' || buf[cur] == 'U') && cur+1 < end && buf[cur+1] == '\'')
  {
    cur = parseCharConst( cur + 2 );
    res = m_tok;
  }
  // String constant
  else if (buf[cur] == '"')
  {
    cur = parseStringConst( cur + 1 );
    res = m_tok;
  }
  else if ((buf[cur] == 'L' || buf[cur] == 'u' || buf[cur] == 'U') && cur+1 < end && buf[cur+1] == '"')
  {
    cur = parseStringConst( cur + 2 );
    res = m_tok;
  }
  else
  // Punctuators
  //
  {
    cur = parsePunctuator( cur );
    res = m_tok;
  }

  res.setRange( m_reader.calcRange( m_cur, cur, m_tempRng ) );
  m_cur = cur;

  return res;
}

} // class

