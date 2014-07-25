package c99.parser.pp;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import c99.Constant;
import c99.IErrorReporter;
import c99.ISourceRange;
import c99.SourceRange;
import c99.Types;
import c99.Utils;
import c99.parser.SymTable;

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
public class PPLexer implements PPDefs
{

protected final SymTable m_symTable;
protected final IErrorReporter m_reporter;
/** Set to false in discarded conditionals */
private boolean m_reportErrors = true;
private String m_fileName;
private final String m_actualFileName;
private LineReader m_reader;
private int m_end;
private int m_cur;

private Token m_fifo[];
private int m_fifoHead, m_fifoTail, m_fifoCount, m_fifoCapacity;

private Token m_workTok;
protected Token m_lastTok;

private final SourceRange m_tmpRange = new SourceRange();

/** Special handling after #include */
private boolean m_parseInclude;

public PPLexer ( final IErrorReporter reporter, String fileName, InputStream input,
                 final SymTable symTable, int bufSize )
{
  m_reporter = reporter;
  m_symTable = symTable;
  m_fileName = fileName;
  m_actualFileName = fileName;
  m_reader = new LineReader( input, bufSize );
  m_end = m_cur = 0;

  m_fifoHead = m_fifoTail = m_fifoCount = 0;
  m_fifoCapacity = 4;
  m_fifo = new Token[m_fifoCapacity];
  for ( int i = 0; i < m_fifoCapacity; ++i )
    m_fifo[i] = new Token();

  m_lastTok = newFifoToken();

  nextLine();
}

public PPLexer ( final IErrorReporter reporter, String fileName, InputStream input,
                 final SymTable symTable )
{
  this( reporter, fileName, input, symTable, 16384 );
}

public final void close ()
{
  m_reader.close();
}

public final String getActualFileName ()
{
  return m_actualFileName;
}

private final void reportWarning ( ISourceRange pos, String msg, Object... args )
{
  m_reporter.warning( pos, msg, args );
}

private final void reportError ( ISourceRange pos, String msg, Object... args )
{
  if (m_reportErrors)
    m_reporter.error( pos, msg, args );
  else
    m_reporter.warning( pos, msg, args );
}

private final void calcEndPosAndReportError ( int cur, String msg, Object... args )
{
  m_reader.calcRangeEnd( cur, m_workTok );
  reportError( m_workTok, msg, args );
}

private final void calcPosAndReportError ( int from, int to, String msg, Object... args )
{
  final SourceRange pos = new SourceRange();
  pos.setFileName( m_fileName );
  m_reader.calcRange( from, to, pos );
  reportError( pos, msg, args );
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
  return c == 32 || c == 9 || c == 11 || c == 12 || c == 13;
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

private static int fromXDigit ( int ch )
{
  ch |= 32;
  if (ch >= '0' && ch <= '9')
    return ch - '0';
  else if (ch >= 'a' && ch <= 'f')
    return ch - ('a' - 10);
  else
    return -1;
}

private byte[] parseCharSequence ( int cur, char terminator, String what )
{
  int end = m_end;
  byte[] buf = m_reader.getLineBuf();
  ByteArrayOutputStream str = new ByteArrayOutputStream();

loop:
  for(;;++cur)
  {
    if (cur == end)
    {
      calcEndPosAndReportError( cur, "Unterminated %s", what );
      break;
    }

    if (buf[cur] == terminator)
    {
      ++cur;
      break;
    }
    if (buf[cur] == '\\')
    {
      if (++cur == end)
      {
        calcEndPosAndReportError( cur, "Unterminated %s", what );
        break;
      }

      byte ch;
      switch (buf[cur])
      {
      case '\'':case '"':case '?':case '\\': ch = buf[cur]; break;
      case 'a': ch =  7; break;
      case 'b': ch =  8; break;
      case 'f': ch = 12; break;
      case 'n': ch = 10; break;
      case 'r': ch = 13; break;
      case 't': ch =  9; break;
      case 'v': ch = 11; break;

      case 'x':
        {
          if (++cur == end)
          {
            calcEndPosAndReportError( cur, "Unterminated %s", what );
            break loop;
          }
          boolean err = false;
          int start = cur;
          int value = 0;
          int dig;
          if ( (dig = fromXDigit( buf[cur]&255 )) < 0)
          {
            err = true;
            calcPosAndReportError( cur-2, cur+1, "Invalid hex escape sequence in %s", what );
          }
          else
          {
            do
            {
              value = (value << 4) + dig;
              ++cur;
            }
            while (cur < end && (dig = fromXDigit( buf[cur]&255 )) >= 0);

            if (cur - start > 4)
            {
              err = true;
              calcPosAndReportError( start, cur, "Integer overflow in %s", what );
            }
          }

          --cur;
          if ((value & 255) != value)
          {
            if (!err)
              calcPosAndReportError( start, cur, "Character overflow in %s", what );
          }

          ch = (byte)value;
        }
        break;

      case '0':case '1':case '2':case '3':case '4':case '5':case '6':case '7':
        {
          int start = cur;
          int value = buf[cur] - '0';
          if (cur+1 < end && buf[cur+1] >= '0' && buf[cur+1] <= '7')
          {
            value = (value << 3) + (buf[cur+1] - '0');
            ++cur;
            if (cur+1 < end && buf[cur+1] >= '0' && buf[cur+1] <= '7')
            {
              value = (value << 3) + (buf[cur+1] - '0');
              ++cur;
            }
          }

          if ((value & 255) != value)
            calcPosAndReportError( start, cur+1, "Character overflow in %s", what );

          ch = (byte)value;
        }
        break;

      default:
        calcPosAndReportError( cur-1, cur+1, "Invalid escape sequence in %s", what );
        ch = buf[cur];
        break;
      }

      str.write( ch & 255 );
    }
    else
      str.write( buf[cur] & 255 );
  }

  m_reader.calcRangeEnd( cur, m_workTok );
  m_cur = cur;
  return str.toByteArray();
}

private static final Constant.IntC s_charShift =
  Constant.makeLong( Types.TypeSpec.UINT, Types.TypeSpec.UCHAR.width );

private void parseCharConst ( int cur )
{
  int start = m_cur;
  byte[] decoded = parseCharSequence( cur, '\'', "character constant" );

  Constant.IntC value = Constant.makeLong( Types.TypeSpec.SINT, 0 );

  if (decoded.length == 0)
    reportError( m_workTok, "Empty character constant" );
  else if (decoded.length == 1)
    value.setLong( decoded[0] & 255 );
  else
  {
    Constant.IntC dig = Constant.newIntConstant( Types.TypeSpec.SINT );

    if (decoded.length > (Types.TypeSpec.SINT.width+7)/8)
      reportError( m_workTok, "Character constant is too long for its type" );
    else
      reportWarning( m_workTok, "Multi-character character constant" );

    for ( byte b : decoded )
    {
      value.shl( value, s_charShift );
      dig.setLong( b & 255 );
      value.add( value, dig );
    }
  }

  m_workTok.setCharConst( m_reader.getLineBuf(), start, m_cur - start, value );
}

private void parseStringConst ( int cur )
{
  int start = m_cur;
  byte[] value = parseCharSequence( cur, '"', "string constant" );
  m_workTok.setStringConst( m_reader.getLineBuf(), start, m_cur - start, value );
}

private static final Constant.IntC s_int0 = Constant.makeLong( Types.TypeSpec.SINT, 0 );
private static final Constant.IntC s_intMaxZero = Constant.makeLong( Types.TypeSpec.INTMAX_T, 0 );
private static final Constant.IntC s_eight = Constant.makeLong( Types.TypeSpec.UINTMAX_T, 8 );
private static final Constant.IntC s_ten = Constant.makeLong( Types.TypeSpec.UINTMAX_T, 10 );
private static final Constant.IntC s_sixteen = Constant.makeLong( Types.TypeSpec.UINTMAX_T, 16 );

private static final Constant.IntC s_intMax_Max =
   Constant.makeLong( Types.TypeSpec.UINTMAX_T, Types.TypeSpec.INTMAX_T.maxValue );

// the valid suffixes are u[l], ull, l[u], ll[u]
// state > 23 indicates end. bit 0 is unsigned, bits 1..2 long/long long
static final byte s_intSuffixTab[]  = {
               // EOF,   u,   l
   /* 0 .    */  0x40, 1*3, 4*3,
   /* 1 u.   */  0x41, 100, 2*3,
   /* 2 ul.  */  0x43, 100, 3*3,
   /* 3 ull. */  0x45, 100, 100,
   /* 4 l.   */  0x42, 7*3, 5*3,
   /* 5 ll.  */  0x44, 6*3, 100,
   /* 6 llu. */  0x45, 100, 100,
   /* 7 lu.  */  0x43, 100, 100,
                 100,
};

private final Constant.IntC parsePPInteger ( Token tok )
{
  int i = 0;
  int to = tok.textLen();
  byte[] text = tok.text();

  boolean err = false;
  Constant.IntC radixC = s_ten;
  int radix = 10;

  if (text[i] == '0')
  {
    ++i;
    if (i == to)
      return s_int0;
    if ((text[i] | 32) == 'x')
    {
      ++i;
      if (i == to)
      {
        reportError( tok, "Invalid integer prefix" );
        return s_int0;
      }
      radixC = s_sixteen;
      radix = 16;
    }
    else
    {
      radixC = s_eight;
      radix = 8;
    }
  }

  Constant.IntC res = Constant.makeLong( Types.TypeSpec.UINTMAX_T, 0 );
  Constant.IntC tmp = Constant.newIntConstant( Types.TypeSpec.UINTMAX_T );
  Constant.IntC digitC = Constant.newIntConstant( Types.TypeSpec.UINTMAX_T );

  for (; i < to; ++i )
  {
    char ch = (char)(text[i] & 255 | 32);
    if (ch == 'u' || ch == 'l')
      break;

    int digit;
    if ((digit = fromXDigit( ch )) < 0 || digit >= radix)
    {
      if (!err)
      {
        err = true;
        reportError( tok, "not a valid integer" );
        break;
      }
    }

    // *= 10
    tmp.mul( res, radixC );
    if (tmp.lt( res ))
    {
      if (!err)
      {
        err = true;
        reportError( tok, "Constant is too large" );
      }
    }

    // += digit
    digitC.setLong( digit );
    res.add( tmp, digitC );
    if (res.lt( tmp ))
    {
      if (!err)
      {
        err = true;
        reportError( tok, "Constant is too large" );
      }
    }
  }

  boolean mustBeUnsigned = false;
  int mustBeLong = 0;

  if (i < to) // Suffix detected
  {
    assert s_intSuffixTab.length == 24 + 1; // 8 states * 3 + 1 extra
    int state = 0;
    do
      if (i < to)
      {
        int ch = text[i++]&255|32;
        if (ch == 'u') state += 1; else if (ch == 'l') state += 2; else state = 24;
      }
    while ( (state = s_intSuffixTab[state]) <= 23);

    if (state == 100)
    {
      if (!err)
      {
        err = true;
        reportError( tok, "Invalid integer suffix" );
      }
      mustBeUnsigned = true;
      mustBeLong = 2;
    }
    else
    {
      mustBeUnsigned = (state & 1) != 0;
      mustBeLong = (state >>> 1) & 3;
    }
  }

  boolean mustBeSigned = !mustBeUnsigned && radix == 10;

  // Determine the type
  int typeOrd;
  int step = 1;
  if (mustBeLong == 1)
    typeOrd = Types.TypeSpec.SLONG.ordinal();
  else if (mustBeLong == 2)
    typeOrd = Types.TypeSpec.SLLONG.ordinal();
  else
    typeOrd = Types.TypeSpec.SINT.ordinal();
  if (mustBeUnsigned)
  {
    assert Types.TypeSpec.values()[typeOrd].signed && !Types.TypeSpec.values()[typeOrd+1].signed;
    ++typeOrd;
    step = 2;
  }
  else if (mustBeSigned)
    step = 2;

  Types.TypeSpec type;
  for( ;; typeOrd += step )
  {
    type = Types.TypeSpec.values()[typeOrd];
    tmp.setLong( type.maxValue );
    if (res.le( tmp ))
      break;
    if (typeOrd + step > Types.TypeSpec.ULLONG.ordinal())
    {
      if (!err)
      {
        err = true;
        reportWarning( tok, "Constant is too large" );
        break;
      }
    }
  }

  if (res.spec != type)
  {
    Constant.IntC cvtRes = Constant.newIntConstant( type );
    cvtRes.castFrom( res );
    return cvtRes;
  }
  else
    return res;
}

private final Constant.RealC parsePPReal ( Token tok )
{
  // Separate the suffix
  byte[] text = tok.text();
  int e = tok.textLen();
  int suff = 0;
  if (e > 0 && ((text[e-1] | 32) == 'f' || (text[e-1] | 32) == 'l'))
  {
    suff = text[e-1] | 32;
    --e;
  }

  double x;
  try
  {
    x = Double.parseDouble( Utils.asciiString( text, 0, e ) );
  }
  catch (NumberFormatException ex)
  {
    reportError( tok, "Invalid floating point constant" );
    x = 1;
  }

  if (suff == 'f')
  {
    if (x < Types.TypeSpec.FLOAT.minReal || x > Types.TypeSpec.FLOAT.maxReal)
    {
      reportError( tok, "Constant is outside of 'float' range" );
      x = 1;
    }
    return Constant.makeDouble( Types.TypeSpec.FLOAT, x );
  }
  else if (suff == 'l')
  {
    if (x < Types.TypeSpec.LDOUBLE.minReal || x > Types.TypeSpec.LDOUBLE.maxReal)
    {
      reportError( tok, "Constant is outside of 'long double' range" );
      x = 1;
    }
    return Constant.makeDouble( Types.TypeSpec.LDOUBLE, x );
  }
  else
  {
    if (x < Types.TypeSpec.DOUBLE.minReal || x > Types.TypeSpec.DOUBLE.maxReal)
    {
      reportError( tok, "Constant is outside of 'double' range" );
      x = 1;
    }
    return Constant.makeDouble( Types.TypeSpec.DOUBLE, x );
  }
}

private final void parsePunctuator ( int cur )
{
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
      if (buf[cur+1] == '.' && buf[cur+2] == '.')
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
      switch (buf[++cur])
      {
        case '-':  code = Code.MINUS_MINUS; ++cur; break;
        case '>':  code = Code.MINUS_GREATER; ++cur; break;
        case '=':  code = Code.MINUS_EQUALS; ++cur; break;
        default:   code = Code.MINUS; break;
      }
      break;
    case '+': // +, ++, +=
      switch (buf[++cur])
      {
        case '+': code = Code.PLUS_PLUS; ++cur; break;
        case '=': code = Code.PLUS_EQUALS; ++cur; break;
        default:  code = Code.PLUS; break;
      }
      break;
    case '&': // &, &&, &=
      switch (buf[++cur])
      {
        case '&': code = Code.AMPERSAND_AMPERSAND; ++cur; break;
        case '=': code = Code.AMPERSAND_EQUALS; ++cur; break;
        default:  code = Code.AMPERSAND; break;
      }
      break;
    case '|': // |, ||, |=
      switch (buf[++cur])
      {
        case '|': code = Code.VERTICAL_VERTICAL; ++cur; break;
        case '=': code = Code.VERTICAL_EQUALS; ++cur; break;
        default:  code = Code.VERTICAL; break;
      }
      break;

    case '%': // %, %=, %>, %:, %:%:
      switch (buf[++cur])
      {
        case '=': code = Code.PERCENT_EQUALS; ++cur; break;
        case '>': code = Code.R_CURLY; ++cur; break;
        case ':':
          if (buf[cur+1] == '%' && buf[cur+2] == ':')
          {
            code = Code.HASH_HASH;
            cur += 3;
            break;
          }
          // else fall-through
        default: code = Code.PERCENT; break;
      }
      break;

    case '*': // *, *=
      if (buf[++cur] == '=')
      {
        code = Code.ASTERISK_EQUALS;
        ++cur;
      }
      else
        code = Code.ASTERISK;
      break;
    case '!': // !, !=
      if (buf[++cur] == '=')
      {
        code = Code.BANG_EQUALS;
        ++cur;
      }
      else
        code = Code.BANG;
      break;
    case '/': // /, /=
      if (buf[++cur] == '=')
      {
        code = Code.SLASH_EQUALS;
        ++cur;
      }
      else
        code = Code.SLASH;
      break;
    case '=': // =, ==
      if (buf[++cur] == '=')
      {
        code = Code.EQUALS_EQUALS;
        ++cur;
      }
      else
        code = Code.EQUALS;
      break;
    case '^': // ^, ^=
      if (buf[++cur] == '=')
      {
        code = Code.CARET_EQUALS;
        ++cur;
      }
      else
        code = Code.CARET;
      break;
    case ':': // :, :>
      if (buf[++cur] == '>')
      {
        code = Code.R_BRACKET;
        ++cur;
      }
      else
        code = Code.COLON;
      break;
    case '#': // #, ##
      if (buf[++cur] == '#')
      {
        code = Code.HASH_HASH;
        ++cur;
      }
      else
        code = Code.HASH;
      break;

    case '<': // <, <=, <:, <%, <<, <<=
      switch (buf[++cur])
      {
        case '=': code = Code.LESS_EQUALS; ++cur; break;
        case ':': code = Code.L_BRACKET; ++cur; break;
        case '%': code = Code.L_CURLY; ++cur; break;
        case '<':
          if (buf[cur+1] == '=')
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
      break;
    case '>': // >, >=, >>, >>=
      switch (buf[++cur])
      {
        case '=': code = Code.GREATER_EQUALS; ++cur; break;
        case '>':
          if (buf[cur+1] == '=')
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
      break;

    default:
      m_workTok.setOther( Code.OTHER, buf, cur++, 1 );
      m_cur = cur;
      return;
  }

  m_workTok.setCode( code );
  m_cur = cur;
}

public final void setFileName ( String fileName )
{
  m_fileName = fileName;
}

private static int find ( byte[] buf, int begin, int end, int ch )
{
  for ( ; begin < end; ++begin )
    if ((buf[begin] & 255) == ch)
      return begin;
  return -1;
}

private final void parseNextToken ( Token tok )
{
  m_workTok = tok;
  m_workTok.setFileName( m_fileName );

  byte[] buf = m_reader.getLineBuf();
  int cur = m_cur;

  m_reader.calcRangeStart( cur, m_workTok );

  // Skip all whitespace
  int ws = 0; // bit 0 is new-line, bit 1 is whitespace
  for(;;)
  {
    // Whitespace
    if (isSpace( buf[cur] ))
    {
      do
        ++cur;
      while (isSpace( buf[cur] ));
      ws |= 2;
    }
    // Block comment
    else if (buf[cur] == '/' && buf[cur+1] == '*')
    {
      m_reader.calcRangeStart( cur, m_tmpRange );
      cur += 2;
      for(;;)
      {
        if (buf[cur] == '*' && buf[cur+1] == '/')
        {
          cur += 2;
          ws |= 2;
          break;
        }
        else if (cur < m_end)
          ++cur;
        else if (nextLine())
        {
          buf = m_reader.getLineBuf();
          cur = m_cur;
        }
        else
        {
          cur = m_end;
          m_reader.calcRangeEnd( cur, m_tmpRange );
          m_reporter.error( m_tmpRange.setFileName(m_fileName), "Unterminated block comment" );
          break; // The outer loop will re-detect and return the EOF
        }
      }
    }
    // Line comment
    //
    else if (buf[cur] == '/' && buf[cur+1] == '/')
    {
      cur = m_end; // skip to the end of line
      ws |= 2;
    }
    // New line
    else if (cur == m_end)
    {
      if (nextLine())
      {
        buf = m_reader.getLineBuf();
        cur = m_cur;
        ws |= 1;
      }
      else
      {
        m_workTok.setLocation( m_reader.getCurLineNumber(), 1 );
        m_workTok.setCode( Code.EOF );
        return;
      }
    }
    else
      break;
  }

  int tmp;
  if (ws != 0) // If we detected any whitespace at all
  {
    m_workTok.setCode( (ws & 1) != 0 ? Code.NEWLINE : Code.WHITESPACE );
  }
  // pp-number
  //
  else if (isDigit( buf[cur] ) ||
           buf[cur] == '.' && isDigit( buf[cur+1] ))
  {
    boolean real = buf[cur] == '.';
    ++cur;
    for(;;)
    {
      if (((buf[cur] | 32) == 'e' || (buf[cur] | 32) == 'p') &&
               (buf[cur+1] == '+' || buf[cur+1] == '-'))
      {
        real = true;
        cur += 2;
      }
      else if (buf[cur] == '.')
      {
        real = true;
        ++cur;
      }
      else if (isDigit( buf[cur] ) || isIdentStart( buf[cur] ))
      {
        ++cur;
      }
      else
        break;
    }

    m_workTok.setText( buf, m_cur, cur - m_cur );
    m_reader.calcRangeEnd( cur, m_workTok );

    if (!real)
      m_workTok.setIntConst( parsePPInteger( m_workTok ) );
    else
      m_workTok.setRealConst( parsePPReal( m_workTok ) );

    m_cur = cur;
    return;
  }
  // Character constant
  else if (buf[cur] == '\'')
  {
    parseCharConst( cur + 1 );
    return;
  }
  else if ((buf[cur] == 'L' || buf[cur] == 'u' || buf[cur] == 'U') && buf[cur+1] == '\'')
  {
    parseCharConst( cur + 2 );
    reportError( m_workTok, "prefixed character constants are not supported yet" );
    return;
  }
  // String constant
  else if (buf[cur] == '"')
  {
    parseStringConst( cur + 1 );
    return;
  }
  else if ((buf[cur] == 'L' || buf[cur] == 'u' || buf[cur] == 'U') && buf[cur+1] == '"')
  {
    parseStringConst( cur + 2 );
    reportError( m_workTok, "prefixed strings are not supported yet" );
    return;
  }
  else if (m_parseInclude && buf[cur] == '<' && (tmp = find( buf, cur+1, m_end, '>')) >= 0)
  {
    cur = tmp+1;
    m_workTok.setOther( Code.ANGLED_INCLUDE, buf, m_cur + 1, cur - m_cur - 2 );
  }
  // Ident
  //
  else if (isIdentStart( buf[cur] ))
  {
    do
      ++cur;
    while (isIdentBody( buf[cur] ));

    m_workTok.setIdent( m_symTable.symbol( buf, m_cur, cur - m_cur ) );
  }
  // Punctuators
  //
  else
  {
    parsePunctuator( cur );
    m_reader.calcRangeEnd( m_cur, m_workTok );
    return;
  }

  m_reader.calcRangeEnd( cur, m_workTok );
  m_cur = cur;
}

final Token nextIncludeToken ()
{
  releaseFifoToken( m_lastTok );

  assert getFifoCount() == 0;
  assert !m_parseInclude;
  m_parseInclude = true;
  try {
    parseNextToken(newFifoToken());
  } finally {
    m_parseInclude = false;
  }

  return m_lastTok = getFifoHead();
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

protected final void setReportErrors ( final boolean reportErrors )
{
  m_reportErrors = reportErrors;
}
} // class
