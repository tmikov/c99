package c99.parser.pp;

import c99.Constant;
import c99.SourceRange;
import c99.Types;
import c99.Utils;
import c99.parser.Symbol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public interface PPDefs
{
public static enum Code
{
  EOF,
  WHITESPACE(" "),
  NEWLINE,
  IDENT,
  PP_INT_NUMBER,
  PP_REAL_NUMBER,
  CHAR_CONST,
  STRING_CONST,
  ANGLED_INCLUDE,

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

  public final byte[] printable;
  public final String str;

  Code ()
  {
    str = "";
    printable = new byte[0];
  }

  Code ( String str )
  {
    this.str = str;
    this.printable = str.getBytes();
  }
}

public abstract static class AbstractToken extends SourceRange implements Cloneable
{
  protected Code m_code;

  AbstractToken m_next, m_prev;

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

  public abstract void output ( OutputStream out ) throws IOException;

  public final String outputString ()
  {
    ByteArrayOutputStream out = new ByteArrayOutputStream( 16 );
    try {
      output(  out );
    } catch (IOException e) { throw new Error( "Unexpected", e ); }
    return out.toString();
  }
}

public static class Token extends AbstractToken
{
  private static final int DEFAULT_LEN = 32;

  private int m_length;
  private int m_flags;
  private Object m_object;

  private final byte[] m_defaultBuf;
  private byte[] m_text;

  public Token ()
  {
    m_defaultBuf = new byte[DEFAULT_LEN];
  }

  public Token ( Code code )
  {
    this();
    setCode( code );
  }

  public Token ( Token tok )
  {
    m_defaultBuf = null;
    copyFrom( tok );
  }

  public void copyFrom ( Token tok )
  {
    m_code = tok.m_code;
    m_length = tok.m_length;
    m_flags = tok.m_flags;
    m_object = tok.m_object;
    if (tok.m_text != null)
      setText( tok.m_text, 0, tok.m_length );
    setRange( tok );
  }

  public final void reset ()
  {
    m_code = null;
    m_length = 0;
    m_flags = 0;
    m_object = null;
    m_text = null;
    this.fileName = null;
    this.line1 = this.col1 = this.line2 = this.col2 = 0;
  }

  public final boolean isNoExpand ()
  {
    return (m_flags & 1) != 0;
  }

  public final void setNoExpand ( boolean f )
  {
    if (f)
      m_flags |= 1;
    else
      m_flags &= ~1;
  }

  public final Symbol symbol ()
  {
    return (Symbol)m_object;
  }

  public final void setIdent ( Symbol symbol )
  {
    this.m_code = Code.IDENT;
    m_object = symbol;
    m_length = symbol.length();
  }

  public final void setText ( byte[] buf, int from, int count )
  {
    if (m_defaultBuf != null && count <= DEFAULT_LEN)
      System.arraycopy( buf, from, m_text = m_defaultBuf, 0, count );
    else
      m_text = Arrays.copyOfRange( buf, from, from + count );
    m_length = count;
  }

  /**
   * Like {@code #setText} but transfers the ownership of {@param buf}
   * instead of copying it.
   */
  public final void setTextWithOnwership ( byte[] buf )
  {
    m_text = buf;
    m_length = buf.length;
  }

  public final void setOther ( Code code, byte[] buf, int from, int count )
  {
    m_code = code;
    setText(  buf, from, count );
  }

  public final void setStringConst ( byte[] origText, int from, int count,
                                     byte[] value )
  {
    m_code = Code.STRING_CONST;
    setText( origText, from, count );
    m_object = value;
  }

  public final void setStringConst ( String value )
  {
    m_code = Code.STRING_CONST;
    m_text = Utils.asciiBytes( Misc.simpleEscapeString( value ) );
    m_length = m_text.length;
    m_object = Utils.asciiBytes( value );
  }

  public final byte[] getStringConstValue ()
  {
    return (byte[])m_object;
  }

  public final void setCharConst ( byte[] origText, int from, int count, Constant.IntC value )
  {
    m_code = Code.CHAR_CONST;
    setText( origText, from, count );
    m_object = value;
  }

  public final Constant.IntC getCharConstValue ()
  {
    return (Constant.IntC)m_object;
  }

  /** {@link #setText(byte[], int, int)} must have already been called! */
  public final void setIntConst ( Constant.IntC value )
  {
    m_code = Code.PP_INT_NUMBER;
    m_object = value;
  }

  public final void setIntConst ( int value )
  {
    setTextWithOnwership( (""+value).getBytes() );
    setIntConst( Constant.makeLong( Types.TypeSpec.SINT, value ) );
  }

  public final Constant.IntC getIntConstValue ()
  {
    return (Constant.IntC)m_object;
  }

  public final void setRealConst ( Constant.RealC value )
  {
    m_code = Code.PP_REAL_NUMBER;
    m_object = value;
  }

  public final Constant.RealC getRealConst ()
  {
    return (Constant.RealC)m_object;
  }

  public final void setCode ( Code code )
  {
    m_code = code;
    m_length = code.printable.length;
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
      if (!(
        m_code == tok.m_code &&
        m_length == tok.m_length &&
        (m_text == null || Utils.equals(m_text, 0, tok.m_text, 0, m_length))
      ))
        return false;
      switch(m_code)
      {
      case IDENT:
        assert m_object instanceof Symbol;
        return m_object == tok.m_object;
      case STRING_CONST:
        return Arrays.equals( (byte[])m_object, (byte[])tok.m_object );
      case CHAR_CONST:
      case PP_INT_NUMBER:
        assert m_object instanceof Constant.IntC;
        return ((Constant.IntC)m_object).equals( (Constant.IntC)tok.m_object );
      case PP_REAL_NUMBER:
        assert m_object instanceof Constant.RealC;
        return ((Constant.RealC)m_object).equals( (Constant.RealC)tok.m_object );
      default:
        assert m_object == null;
        return true;
      }
    }
    else
      return false;
  }

  @Override
  public int length ()
  {
    return m_length;
  }

  public void output ( OutputStream out ) throws IOException
  {
    if (m_object instanceof Symbol)
      out.write( ((Symbol)m_object).bytes );
    else if (m_text != null)
      out.write( m_text, 0, m_length );
    else
      out.write( m_code.printable );
  }

  @Override
  public String toString ()
  {
    final StringBuilder sb = new StringBuilder( "Token{" );
    sb.append( m_code );
    if (isNoExpand())
      sb.append( ", NO_EXPAND" );
    if (m_object instanceof Symbol)
      sb.append( ", " ).append( (Symbol)m_object );
    else if (m_text != null)
      sb.append( ", '" ).append( Utils.asciiString( m_text, 0, Math.min( m_length, DEFAULT_LEN ) ) )
        .append( '\'' );
    sb.append(  ", " ).append( SourceRange.formatRange( this ) );
    sb.append( '}' );
    return sb.toString();
  }

  public final byte[] text ()
  {
    return m_text;
  }

  public final int textLen ()
  {
    return m_length;
  }

  public final String textString ()
  {
    return Utils.asciiString( m_text, 0, m_length );
  }
}
}
