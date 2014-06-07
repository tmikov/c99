package c99.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import c99.FatalException;
import c99.Location;
import c99.SourceRange;

public final class LineReader
{
private final InputStream m_input;
private byte m_buf[];
private int m_lineStart, m_limit;
private boolean m_inputEOF;
private boolean m_linesEOF;

private int m_lineEnd;
private int m_consumePos;
private int m_startLineNumber;
private int m_curLineNumber;

/** A map of the line breaks in a concatenated line */
private int m_lineMap[] = new int[32];
private int m_lineMapCount;

public LineReader ( InputStream input, int bufSize )
{
  m_input = input;
  m_buf = new byte[bufSize];
  m_lineStart = 0;
  m_limit = 0;
  m_inputEOF = false;
  m_linesEOF = false;

  m_lineEnd = 0;
  m_consumePos = 0;
  m_startLineNumber = m_curLineNumber = 0;

  m_lineMapCount = 0;
}

private void resetMap ()
{
  m_lineMapCount = 0;
}

private void addMapEntry ( int ofs )
{
  if (m_lineMapCount == m_lineMap.length) // Resize if necessary
    m_lineMap = Arrays.copyOf( m_lineMap, m_lineMap.length * 2 );

  m_lineMap[m_lineMapCount++] = ofs;
}

/**
 *
 * @return false if no new data was filled
 */
private boolean fill ()
{
  if (m_inputEOF)
    return false;

  if (m_limit == m_buf.length)
  {
    if (m_lineStart == m_limit)
    {
      m_limit = 0;
      m_lineStart = 0;
    }
    else if (m_lineStart > 0)
    {
      int len = m_limit - m_lineStart;
      System.arraycopy( m_buf, m_lineStart, m_buf, 0, len );
      m_lineStart = 0;
      m_limit = len;
    }
    else
    {
      // Buffer is completely full. Resize the buffer.
      m_buf = Arrays.copyOf( m_buf, m_buf.length * 2 );
    }
  }

  try
  {
    int n;
    n = m_input.read( m_buf, m_limit, m_buf.length - m_limit );
    if (n >= 0)
      m_limit += n;

    if (m_limit < m_buf.length)
      m_inputEOF = true;

    return n > 0;
  }
  catch (IOException e)
  {
    throw new FatalException( e );
  }
}

public boolean readNextLine ()
{
  if (m_linesEOF)
    return false;

  m_lineStart = m_consumePos; // consume the previous line
  m_startLineNumber = ++m_curLineNumber;
  resetMap();

  int ofs = 0; // offset from mark
  for(;;)
  {
    int i;
    for ( i = m_lineStart + ofs; i < m_limit && m_buf[i] != '\n'; ++i )
      {}

    if (i < m_limit)
    {
      m_lineEnd = i + 1;
      break;
    }

    ofs = i - m_lineStart;
    if (!fill())
    {
      m_lineEnd = m_limit;
      m_linesEOF = true;
      break;
    }
  }
  m_consumePos = m_lineEnd;
  return true;
}

public boolean appendNextLine ( int at )
{
  assert at >= m_lineStart && at < m_lineEnd;

  if (m_linesEOF)
    return false;

  ++m_curLineNumber;
  at -= m_lineStart; // Convert to offset
  addMapEntry( at );

  int newStartOfs = m_consumePos - m_lineStart; // convert to offset
  int ofs = newStartOfs;
  for(;;)
  {
    int i;
    for ( i = m_lineStart + ofs; i < m_limit && m_buf[i] != '\n'; ++i )
      {}

    if (i < m_limit)
    {
      m_lineEnd = i + 1;
      break;
    }

    ofs = i - m_lineStart;
    if (!fill())
    {
      m_lineEnd = m_limit;
      m_linesEOF = true;
      break;
    }
  }

  m_consumePos = m_lineEnd;

  // The new line is between [m_lineStart+newStartOfs..m_lineEnd). Append it.
  int newStart = m_lineStart + newStartOfs; // Convert to abs
  int newLen = m_lineEnd - newStart;
  at += m_lineStart; // convert back to abs

  if (newLen > 0)
    System.arraycopy( m_buf, newStart, m_buf, at, newLen );
  m_lineEnd = at + newLen;

  return true;
}

public byte[] getLineBuf ()
{
  return m_buf;
}

public int getLineStart ()
{
  return m_lineStart;
}

public int getLineEnd ()
{
  return m_lineEnd;
}

public int getCurLineNumber ()
{
  return m_curLineNumber;
}

private final Location m_tmpLoc = new Location();

public final void calcRangeStart ( int from, SourceRange rng )
{
  calcLocation( from, m_tmpLoc );
  rng.line1 = m_tmpLoc.line;
  rng.col1 = m_tmpLoc.col;
}

public final void calcRangeEnd ( int to, SourceRange rng )
{
  calcLocation( to - 1, m_tmpLoc );
  rng.line2 = m_tmpLoc.line;
  rng.col2 = m_tmpLoc.col+1;
}

public final SourceRange calcRange ( int from, int to, SourceRange rng )
{
  calcRangeStart( from, rng );

  if (to > from)
    calcRangeEnd( to, rng );
  else
  {
    rng.line2 = rng.line1;
    rng.col2 = rng.line2;
  }
  return rng;
}

private void calcLocation ( int pos, Location loc )
{
  pos -= m_lineStart; // Convert into offset

  int line = m_startLineNumber;
  for ( int i = m_lineMapCount - 1; i >= 0; --i )
  {
    if (pos >= m_lineMap[i])
    {
      pos -= m_lineMap[i];
      line += i + 1;
      break;
    }
  }

  loc.init( line, pos + 1 );
}

} // class
