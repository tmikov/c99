package c99.parser;

import c99.IErrorReporter;
import c99.SourceRange;
import c99.parser.pp.LineReader;

public class Lexer
{
private final IErrorReporter m_reporter;
private LineReader m_reader; // FIXME: only so it will compile
private final SourceRange m_tempRng = new SourceRange(); // FIXME: only so it will compile
private long m_longVal;

public Lexer ( final IErrorReporter reporter )
{
  m_reporter = reporter;
}

private boolean isOctal ( int c )
{
  return c >= '0' && c <= '7';
}

private boolean isHex ( int c )
{
  c |= 32;
  return c >= '0' && c <= '9' || c >= 'a' && c <= 'f';
}

private int fromHexChar ( int c )
{
  c |= 32;
  return c >= '0' && c <= '9' ? c - '0' : c - ('a' - 10);
}

private int parseEscape ( byte buf[], int cur, int end )
{
  int start = cur;
  long val = 0;
  int maxLen;

  ++cur;
  if (cur == end)
  {
    m_reporter.error( m_reader.calcRange( start, cur, m_tempRng ),
                      "Invalid escape sequence" );
    m_longVal = 0;
    return cur;
  }

  switch (buf[cur])
  {
  case '\'':
  case '"':
  case '?':
  case '\\':
    val = buf[cur];
    ++cur;
    break;

  case 'a': val = 7; ++cur; break;
  case 'b': val = 8; ++cur; break;
  case 'f': val = 12; ++cur; break;
  case 'n': val = 10; ++cur; break;
  case 'r': val = 13; ++cur; break;
  case 't': val = 9; ++cur; break;
  case 'v': val = 11; ++cur; break;

  case '0': // octal digit
    maxLen = 3;
    do
      val = (val << 3) + (buf[cur++] - '0');
    while (--maxLen > 0 && cur < end && isOctal( buf[cur] ));
    break;

  case 'x': // hex digit
    ++cur;
    if (cur == end || !isHex( buf[cur] ))
    {
      m_reporter.error( m_reader.calcRange( start, cur, m_tempRng ),
                        "Invalid hex escape sequence" );
    }
    else
    {
      do
        val = (val << 4) + fromHexChar( buf[cur++] );
      while (cur < end && isHex( buf[cur] ));
    }
    break;

  default:
    ++cur;
    m_reporter.error( m_reader.calcRange( start, cur, m_tempRng ),
                      "Invalid escape sequence" );
    break;
  }

  m_longVal = val;
  return cur;
}

} // class

