package c99.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;

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
  CHAR,
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
    System.arraycopy( buf, from, this.text, 0, count );
    this.count = count;
  }

  @Override
  public void output ( final PrintStream out )
  {
    out.write( text, 0, count );
  }
}

public static class CharToken extends Token
{
  public int value;
  public CharToken ()
  {
    super( TokenCode.CHAR );
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
private final NewLineToken m_tokNewLine = new NewLineToken();
private final WhiteSpaceToken m_tokSpace = new WhiteSpaceToken();
private final IdentToken m_tokIdent = new IdentToken();
private final PPNumberToken m_tokNumber = new PPNumberToken();
private final CharToken m_tokChar = new CharToken();

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
    m_start = m_end = m_cur = 0;
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
        m_reader.calcRange( end, end + 1, m_tempRng );
        m_tempRng.setFileName( m_fileName );

        m_reporter.warning( m_tempRng, "backslash and newline separated by space" );
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

public final Token nextToken ()
{
  m_tempRng.setFileName( m_fileName );

  while (m_cur == m_end)
  {
    if (!nextLine())
    {
      m_tokEOF.setLocation( m_fileName, m_reader.getCurLineNumber(), 1 );
      return m_tokEOF;
    }
    else
    {
      m_reader.calcRange( m_cur, m_cur, m_tempRng );
      m_tokNewLine.setRange( m_tempRng );
      return m_tokNewLine;
    }
  }

  int cur = m_cur;
  int end = m_end;

  assert cur < end;

  byte[] buf = m_reader.getLineBuf();

  Token res;
  if (isAnySpace( buf[cur] ))
  {
    do
      ++cur;
    while (isAnySpace( buf[cur] ));

    res = m_tokSpace;
    res.code = TokenCode.WHITESPACE;
  }
  else if (buf[cur] == '/' && cur + 1 < end && buf[cur+1] == '/')
  {
    cur = end;
    res = m_tokSpace;
    res.code = TokenCode.COMMENT;
  }
  else if (isIdentStart( buf[cur] ))
  {
    do
      ++cur;
    while (isIdentBody( buf[cur] ));

    m_tokIdent.symbol = m_symTable.symbol( buf, m_cur, cur - m_cur );
    res = m_tokIdent;
  }
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
  else
  {
    ++cur;
    m_tokChar.value = buf[m_cur] & 0xFF;
    res = m_tokChar;
  }

  m_reader.calcRange( m_cur, cur, m_tempRng );
  res.setRange( m_tempRng );
  m_cur = cur;

  return res;
}

} // class

