package c99;

import java.util.ArrayList;
import java.util.HashMap;

public class Types
{
private Types () {}

public static final int CHAR_BITS = Platform.CHAR_BITS;
public static final int SHORT_BITS = Platform.SHORT_BITS;
public static final int INT_BITS = Platform.INT_BITS;
public static final int LONG_BITS = Platform.LONG_BITS;
public static final int LONGLONG_BITS = Platform.LONGLONG_BITS;

public static enum TypeSpec
{
  VOID("void"),

  BOOL("bool",false,1),
  // Note: the ordering matters. First <signed>, then <unsigned>, from smaller to larger
  SCHAR("signed char",true,CHAR_BITS),
  UCHAR("unsigned char",false,CHAR_BITS),
  SSHORT("short",true,SHORT_BITS),
  USHORT("unsigned short",false,SHORT_BITS),
  SINT("int",true,INT_BITS),
  UINT("unsigned",false,INT_BITS),
  SLONG("long",true,LONG_BITS),
  ULONG("unsigned long",false,LONG_BITS),
  SLLONG("long long",true,LONGLONG_BITS),
  ULLONG("unsibned long long",false,LONGLONG_BITS),
  FLOAT("float",32, Float.MIN_VALUE, Float.MAX_VALUE),
  DOUBLE("double",64, Double.MIN_VALUE, Double.MAX_VALUE),
  LDOUBLE("long double",64, Double.MIN_VALUE, Double.MAX_VALUE),

  ATOMIC("_Atomic"),
  COMPLEX("_Complex"),
  IMAGINARY("_Imaginary"),

  ENUM("enum"),

  ARRAY("[]"),
  STRUCT("struct"),
  UNION("union"),
  FUNCTION("()"),
  POINTER("*"),

  ERROR("error");

  public static final TypeSpec INTMAX_T = SLLONG;
  public static final TypeSpec UINTMAX_T = ULLONG;

  public final String str;

  public final boolean arithmetic;
  public final boolean floating;
  public final boolean integer;
  public final boolean signed;
  public final int width;
  public final int sizeOf; //< sizeof()
  public final long longMask;
  public final long minValue;
  public final long maxValue;
  public final double minReal;
  public final double maxReal;

  TypeSpec ( String str )
  {
    this.str = str;
    this.arithmetic = false;
    this.floating = false;
    this.integer = false;
    this.signed = false;
    this.width = 0;
    this.sizeOf = 0;
    this.longMask = 0;
    this.minValue = 0;
    this.maxValue = 0;
    this.minReal = 0;
    this.maxReal = 0;
  }

  TypeSpec ( String str, int width, double minReal, double maxReal )
  {
    assert width > 0;
    this.str = str;
    this.arithmetic = true;
    this.floating = true;
    this.integer = false;
    this.signed = true;
    this.width = width;
    this.sizeOf = width / CHAR_BITS; assert width % CHAR_BITS == 0;
    this.longMask = this.width < 64 ? (1L << this.width) - 1 : ~0L;
    this.minValue = 0;
    this.maxValue = 0;
    this.minReal = minReal;
    this.maxReal = maxReal;
  }

  TypeSpec ( String str, boolean signed, int width )
  {
    assert width > 0;
    this.str = str;
    this.arithmetic = true;
    this.floating = false;
    this.integer = true;
    this.signed = signed;
    this.width = width;
    this.sizeOf = (width==1?8:width) / CHAR_BITS; assert (width==1?8:width) % CHAR_BITS == 0;
    this.longMask = this.width < 64 ? (1L << this.width) - 1 : ~0L;

    if (signed)
    {
      this.maxValue = this.longMask >>> 1;
      this.minValue = -this.maxValue - 1;
    }
    else
    {
      this.minValue = 0;
      this.maxValue = this.longMask;
    }
    this.minReal = 0;
    this.maxReal = 0;
  }

  TypeSpec toUnsigned ()
  {
    assert this.integer;
    return signed ? values()[this.ordinal() + 1] : this;
  }
}

public static interface TypeVisitor
{
  public boolean visitSimple ( Qual q, SimpleSpec s );
  public boolean visitBased ( Qual q, BasedSpec s );
  public boolean visitAtomic ( Qual q, BasedSpec s );
  public boolean visitPointer ( Qual q, PointerSpec s );
  public boolean visitArray ( Qual q, ArraySpec s );
  public boolean visitStructUnion ( Qual q, StructUnionSpec s );
  public boolean visitEnum ( Qual q, EnumSpec s );
  public boolean visitFunction ( Qual q, FunctionSpec s );
}

public static boolean visitAllPre ( Qual q, TypeVisitor v )
{
  for (;;)
  {
    if (!q.spec.visit( q, v ))
      return false;
    if (q.spec instanceof DerivedSpec)
      q = ((DerivedSpec) q.spec).of;
    else
      break;
  }
  return true;
}

public static boolean visitAllPost ( Qual q, TypeVisitor v )
{
  final ArrayList<Qual> stack = new ArrayList<Qual>( 16 );
  for (;;)
  {
    stack.add( q );
    if (q.spec instanceof DerivedSpec)
      q = ((DerivedSpec) q.spec).of;
    else
      break;
  }
  for ( int i = stack.size(); i > 0; )
  {
    final Qual qq = stack.get(--i);
    if (!qq.spec.visit( qq, v ))
      return false;
  }
  return true;
}

public static enum SClass
{
  NONE,
  TYPEDEF,
  EXTERN,
  STATIC,
  AUTO,
  REGISTER,
}

public static final class Qual
{
  public boolean isConst;
  public boolean isVolatile;
  public boolean isRestrict;
  public boolean isAtomic;
  private int    m_align;
  public final ExtAttributes extAttrs = new ExtAttributes();

  public Spec spec;

  public Qual ( final Spec spec )
  {
    assert spec != null;
    this.spec = spec;
  }

  public void combine ( Qual q )
  {
    this.isConst |= q.isConst;
    this.isVolatile |= q.isVolatile;
    this.isRestrict |= q.isRestrict;
    this.isAtomic |= q.isAtomic;
    this.extAttrs.combine( q.extAttrs );
  }

  public final int alignOf ()
  {
    return Math.max( m_align, this.spec.align );
  }

  /**
   * Is a type valid for a re-declaration of the same symbol.
   * @param qual
   * @return
   */
  public final boolean same ( Qual qual )
  {
    return this == qual ||
           isAtomic == qual.isAtomic && isConst == qual.isConst && isRestrict == qual.isRestrict &&
           isVolatile == qual.isVolatile &&
           m_align == qual.m_align &&
           extAttrs.same( qual.extAttrs ) &&
           spec.same( qual.spec );
  }

  @Override public final String toString ()
  {
    StringBuilder buf = new StringBuilder();
    if (isConst) buf.append( "const " );
    if (isVolatile) buf.append( "volatile " );
    if (isRestrict) buf.append( "restrict " );
    if (isAtomic) buf.append(  "atomic " );
    if (spec != null)
      buf.append( spec.toString() );
    return buf.toString();
  }

  public final String readableType ()
  {
    return toString();
  }
}

public static abstract class Spec
{
  public TypeSpec type;
  public final ExtAttributes extAttrs = new ExtAttributes();
  protected boolean complete;
  private int size;
  int align;

  public Spec ( final TypeSpec type, boolean complete )
  {
    this.type = type;
    this.complete = complete;
    this.size = -1;
  }

  public abstract boolean visit ( Qual q, TypeVisitor v );

  public abstract boolean isError ();

  public final boolean isComplete ()
  {
    return this.complete;
  }

  public final void setSizeAlign ( int size, int align )
  {
    this.size = size;
    this.align = align;
  }

  public final int sizeOf ()
  {
    assert isComplete();
    return this.size;
  }

  public boolean same ( Spec o )
  {
    return o == this || this.type == o.type && extAttrs.same( o.extAttrs );
  }

  @Override public String toString ()
  {
    return type.str;
  }

  public final String readableType ()
  {
    return toString();
  }
}

public static final class SimpleSpec extends Spec
{
  public SimpleSpec ( final TypeSpec type, int size, int align )
  {
    super(type, size > 0);
    if (size > 0)
    {
      assert type.sizeOf == size && align > 0;
      setSizeAlign( size, align );
    }
    else
    {
      assert type.sizeOf == 0 && size == -1 && align == 0;
    }
  }

  @Override
  public boolean visit ( Qual q, TypeVisitor v )
  {
    return v.visitSimple( q, this );
  }

  @Override
  public boolean isError ()
  {
    return this.type == TypeSpec.ERROR;
  }
}

/** Complex, Imaginary, Atomic */
public static final class BasedSpec extends Spec
{
  public final Spec on;

  public BasedSpec ( TypeSpec type, Spec on )
  {
    super( type, on.isComplete() );
    assert type == TypeSpec.COMPLEX || type == TypeSpec.IMAGINARY || type == TypeSpec.ATOMIC;
    this.on = on;
  }

  @Override
  public boolean visit ( Qual q, TypeVisitor v )
  {
    return this.type == TypeSpec.ATOMIC ? v.visitAtomic( q, this ) : v.visitBased( q, this );
  }

  @Override
  public boolean isError ()
  {
    return this.on.isError();
  }

  @Override public boolean same ( Spec o )
  {
    return super.same( o ) && this.on.same( ((BasedSpec)o).on );
  }

  @Override public final String toString ()
  {
    return type + " of " + on;
  }
}

public static abstract class DerivedSpec extends Spec
{
  public Qual of;

  public DerivedSpec ( final TypeSpec type, boolean complete, Qual of )
  {
    super( type, complete );
    this.of = of;
  }

  @Override
  public boolean same ( Spec o )
  {
    return super.same(o) && this.of.same( ((DerivedSpec)o).of );
  }

  @Override public String toString ()
  {
    return type + " of " + of;
  }
}

public static final class PointerSpec extends DerivedSpec
{
  public int staticSize; // from ArraySpec.size and _static

  public PointerSpec ( Qual of, int size, int align )
  {
    super(TypeSpec.POINTER, true, of);
    setSizeAlign( size, align );
  }

  @Override
  public boolean visit ( Qual q, TypeVisitor v )
  {
    return v.visitPointer( q, this );
  }

  @Override
  public boolean isError ()
  {
    return false;
  }

  @Override public final boolean same ( Spec o )
  {
    return
      super.same(o) && this.staticSize == ((PointerSpec)o).staticSize;
  }

  @Override public final String toString ()
  {
    return staticSize > 0 ? "ptr to /static "+ staticSize+"/ "+ of : "ptr to "+ of;
  }
}

public static final class ArraySpec extends DerivedSpec
{
  public int nelem;
  public boolean _static;
  public boolean asterisk;

  public ArraySpec ( Qual of )
  {
    super( TypeSpec.ARRAY, false, of );
    this.nelem = -1;
  }

  @Override
  public boolean visit ( Qual q, TypeVisitor v )
  {
    return v.visitArray( q, this );
  }

  @Override
  public boolean isError ()
  {
    return false;
  }

  @Override public boolean same ( Spec o )
  {
    if (!super.same(o))
      return false;
    ArraySpec x = (ArraySpec) o;
    return this._static == x._static && this.asterisk == x.asterisk && this.nelem == x.nelem;
  }

  @Override public final String toString ()
  {
    StringBuilder buf = new StringBuilder();
    buf.append( "array[ " );
    if (_static)
      buf.append( "static" );
    if (nelem >= 0)
      buf.append( nelem );
    if (asterisk)
      buf.append( '*' );
    buf.append( ']' );
    buf.append( of.toString() );
    return buf.toString();
  }
}

public static abstract class TagSpec extends Spec
{
  public final Ident name;

  public TagSpec ( final TypeSpec type, final Ident name )
  {
    super( type, false );
    this.name = name;
  }

  @Override public boolean same ( Spec o )
  {
    return this == o;
  }

  public String toString ()
  {
    return this.name != null ? this.type.str + " " + this.name.name : this.type.str;
  }
}

public static final class StructUnionSpec extends TagSpec
{
  private boolean m_error;
  private Member[] m_fields;
  public HashMap<Ident,Member> lookup;

  public StructUnionSpec ( final TypeSpec type, final Ident name )
  {
    super( type, name );
  }

  @Override
  public boolean visit ( Qual q, TypeVisitor v )
  {
    return v.visitStructUnion( q, this );
  }

  @Override
  public boolean isError ()
  {
    return this.m_error;
  }

  public void orError ( boolean err )
  {
    this.m_error |= err;
  }

  public void setFields ( Member[] fields )
  {
    assert m_fields == null;
    this.complete = fields != null;
    m_fields = fields;
  }

  public Member[] getFields ()
  {
    return m_fields;
  }
}

public static final class EnumSpec extends TagSpec
{
  public SimpleSpec spec;

  public EnumSpec ( final Ident name )
  {
    super( TypeSpec.ENUM, name );
  }

  @Override
  public boolean visit ( Qual q, TypeVisitor v )
  {
    return v.visitEnum( q, this );
  }

  @Override
  public boolean isError ()
  {
    return false;
  }
}

public static final class FunctionSpec extends DerivedSpec
{
  public boolean oldStyle;
  public Param[] params;

  public FunctionSpec ( boolean oldStyle, Qual returning )
  {
    super( TypeSpec.FUNCTION, false, returning );
    this.oldStyle = oldStyle;
  }

  @Override
  public boolean isError ()
  {
    return false;
  }

  @Override
  public boolean visit ( Qual q, TypeVisitor v )
  {
    return v.visitFunction( q, this );
  }

  @Override public boolean same ( Spec o )
  {
    if (o == this)
      return true;
    if (!super.same( o ))
      return false;
    FunctionSpec x = (FunctionSpec)o;
    if (this.oldStyle != x.oldStyle) return false;
    if (this.params == null) return x.params == null;
    if (this.params.length != x.params.length) return false;

    for ( int e = this.params.length, i = 0; i < e; ++i )
    {
      final Param pa = this.params[i];
      final Param pb = x.params[i];
      if (!pa.type.same( pb.type ) || !pa.extAttrs.same(pb.extAttrs))
        return false;
    }

    return true;
  }

  @Override public String toString ()
  {
    StringBuilder buf = new StringBuilder();
    buf.append( oldStyle ? "oldfunc(" : "func(" );
    if (params != null)
      for ( int i = 0; i < params.length; ++i )
      {
        final Param param = params[i];
        if (i > 0)
          buf.append( ", " );
        if (param.name != null)
          buf.append( param.name.name );
        buf.append( ':' );
        if (param.type != null)
          buf.append( param.type.toString() );
      }
    buf.append( ") returning " );
    buf.append( of.toString() );
    return buf.toString();
  }
}

public static class Param extends SourceRange
{
  public final Ident name;
  public final Qual type;
  public final ExtAttributes extAttrs = new ExtAttributes();

  public Param ( ISourceRange rng, final Ident name, final Qual type  )
  {
    super( rng );
    this.name = name;
    this.type = type;
  }
}

public static class Member extends Param
{
  public int offset;

  public Member ( final ISourceRange rng, final Ident name, final Qual type )
  {
    super(rng, name, type);
  }
}

public static TypeSpec integerPromotion ( TypeSpec spec )
{
  // 6.3.1.1 [2]
  assert spec.integer;
  if (spec.ordinal() < TypeSpec.SINT.ordinal())
    return (spec.width - (spec.signed?1:0) <= TypeSpec.SINT.width) ? TypeSpec.SINT : TypeSpec.UINT;
  else
    return spec;
}

public static TypeSpec usualArithmeticConversions ( TypeSpec s0, TypeSpec s1 )
{
  // 6.3.1.8

  Types.TypeSpec greaterRank = s0.ordinal() > s1.ordinal() ? s0 : s1;

  if (greaterRank.floating)
    return greaterRank;

  s0 = integerPromotion( s0 );
  s1 = integerPromotion( s1 );

  // If both operands have the same type, then no further conversion is needed.
  if (s0 == s1)
    return s0;

  TypeSpec lesserRank;
  if (s0.ordinal() > s1.ordinal())
  {
    greaterRank = s0;
    lesserRank = s1;
  }
  else
  {
    greaterRank = s1;
    lesserRank = s0;
  }

  // Otherwise, if both operands have signed integer types or both have unsigned
  // integer types, the operand with the type of lesser integer conversion rank is
  // converted to the type of the operand with greater rank.
  //
  // Otherwise, if the operand that has unsigned integer type has rank greater or
  // equal to the rank of the type of the other operand, then the operand with
  // signed integer type is converted to the type of the operand with unsigned
  // integer type.
  //
  // Otherwise, if the type of the operand with signed integer type can represent
  // all of the values of the type of the operand with unsigned integer type, then
  // the operand with unsigned integer type is converted to the type of the
  // operand with signed integer type.
  if (s0.signed == s1.signed ||
      !greaterRank.signed ||
      greaterRank.width-1 >= lesserRank.width)
  {
    return greaterRank;
  }

  // Otherwise, both operands are converted to the unsigned integer type
  // corresponding to the type of the operand with signed integer type.
  assert greaterRank.signed;
  assert !lesserRank.signed;

  return greaterRank.toUnsigned();
}

} // class
