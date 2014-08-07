package c99;

import java.util.HashMap;

public class Types
{
private Types () {}

public static final int CHAR_BITS = 8;
public static final int SHORT_BITS = 16;
public static final int INT_BITS = 32;
public static final int LONG_BITS = 32;
public static final int LONGLONG_BITS = 64;

public static enum TypeSpec
{
  VOID(),

  BOOL(false,1),
  // Note: the ordering matters. First <signed>, then <unsigned>, from smaller to larger
  SCHAR(true,CHAR_BITS),
  UCHAR(false,CHAR_BITS),
  SSHORT(true,SHORT_BITS),
  USHORT(false,SHORT_BITS),
  SINT(true,INT_BITS),
  UINT(false,INT_BITS),
  SLONG(true,LONG_BITS),
  ULONG(false,LONG_BITS),
  SLLONG(true,LONGLONG_BITS),
  ULLONG(false,LONGLONG_BITS),
  FLOAT(32, Float.MIN_VALUE, Float.MAX_VALUE),
  DOUBLE(64, Double.MIN_VALUE, Double.MAX_VALUE),
  LDOUBLE(64, Double.MIN_VALUE, Double.MAX_VALUE),

  ATOMIC(),
  COMPLEX(),
  IMAGINARY(),

  ENUM(),

  ARRAY(),
  STRUCT(),
  UNION(),
  FUNCTION(),
  POINTER(),

  ERROR();

  public static final TypeSpec WCHAR_T = UINT;
  public static final TypeSpec CHAR16_T = USHORT;
  public static final TypeSpec CHAR32_T = ULONG;
  public static final TypeSpec INTMAX_T = SLLONG;
  public static final TypeSpec UINTMAX_T = ULLONG;

  public final boolean arithmetic;
  public final boolean floating;
  public final boolean integer;
  public final boolean signed;
  public final int width;
  public final long longMask;
  public final long minValue;
  public final long maxValue;
  public final double minReal;
  public final double maxReal;

  TypeSpec ()
  {
    this.arithmetic = false;
    this.floating = false;
    this.integer = false;
    this.signed = false;
    this.width = 0;
    this.longMask = 0;
    this.minValue = 0;
    this.maxValue = 0;
    this.minReal = 0;
    this.maxReal = 0;
  }

  TypeSpec ( int width, double minReal, double maxReal )
  {
    assert width > 0;
    this.arithmetic = true;
    this.floating = true;
    this.integer = false;
    this.signed = true;
    this.width = width;
    this.longMask = this.width < 64 ? (1L << this.width) - 1 : ~0L;
    this.minValue = 0;
    this.maxValue = 0;
    this.minReal = minReal;
    this.maxReal = maxReal;
  }

  TypeSpec ( boolean signed, int width )
  {
    assert width > 0;
    this.arithmetic = true;
    this.floating = false;
    this.integer = true;
    this.signed = signed;
    this.width = width;
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

  public Spec spec;

  public Qual ( final Spec spec )
  {
    this.spec = spec;
  }

  public void combine ( Qual q )
  {
    this.isConst |= q.isConst;
    this.isVolatile |= q.isVolatile;
    this.isRestrict |= q.isRestrict;
    this.isAtomic |= q.isAtomic;
  }

  public final boolean same ( Qual qual )
  {
    return this == qual ||
           isAtomic == qual.isAtomic && isConst == qual.isConst && isRestrict == qual.isRestrict &&
           isVolatile == qual.isVolatile &&
           spec.same( qual.spec );
  }
}

public static abstract class Spec
{
  public TypeSpec type;

  public Spec ( final TypeSpec type ) { this.type = type; }

  public abstract boolean isComplete ();
  public abstract boolean same ( Spec o );
}

public static final class SimpleSpec extends Spec
{
  public SimpleSpec ( final TypeSpec type ) { super(type); }

  @Override public boolean isComplete ()
  {
    return type != TypeSpec.VOID;
  }

  @Override public boolean same ( Spec o )
  {
    return this.type == o.type;
  }
}

/** Complex, Imaginary, Atomic */
public static final class BasedSpec extends Spec
{
  public final Spec on;

  public BasedSpec ( TypeSpec type, Spec on )
  {
    super( type );
    assert type == TypeSpec.COMPLEX || type == TypeSpec.IMAGINARY || type == TypeSpec.ATOMIC;
    this.on = on;
  }

  @Override public boolean isComplete ()
  {
    return on != null && on.isComplete();
  }

  @Override public boolean same ( Spec o )
  {
    if (this == o) return true;
    if (this.type != o.type) return false;
    BasedSpec x = (BasedSpec)o;
    return this.on.same( x.on );
  }
}

public static abstract class DerivedSpec extends Spec
{
  public Qual of;

  public DerivedSpec ( final TypeSpec type, Qual of ) { super( type ); this.of = of; }
  public DerivedSpec ( final TypeSpec type ) { this( type, null ); }

  @Override
  public boolean same ( Spec o )
  {
    if (this == o) return true;
    if (this.type != o.type) return false;
    DerivedSpec x = (DerivedSpec)o;
    return this.of.same( x.of );
  }
}

public static final class PointerSpec extends DerivedSpec
{
  public Constant.IntC staticSize; // from ArraySpec.size and _static

  public PointerSpec ( Qual of )
  {
    super( TypeSpec.POINTER, of );
  }

  public PointerSpec ()
  {
    this( null );
  }

  @Override public boolean isComplete ()
  {
    return true;
  }
}

public static final class ArraySpec extends DerivedSpec
{
  public Constant.IntC size;
  public boolean _static;
  public boolean asterisk;

  public ArraySpec () { super( TypeSpec.ARRAY ); }

  @Override public boolean isComplete ()
  {
    return size != null && this.of != null && this.of.spec.isComplete();
  }

  @Override public boolean same ( Spec o )
  {
    return
      super.same(o) &&
      (this.size != null ? this.size.equals( ((ArraySpec)o).size ) : ((ArraySpec)o).size == null);
  }
}

public static abstract class TagSpec extends DerivedSpec
{
  public final Ident name;

  public TagSpec ( final TypeSpec type, final Ident name )
  {
    super( type );
    this.name = name;
  }

  @Override public boolean same ( Spec o )
  {
    return this == o;
  }
}

public static final class StructUnionSpec extends TagSpec
{
  public boolean error;
  public Member[] fields;
  public HashMap<Ident,Member> lookup;

  public StructUnionSpec ( final TypeSpec type, final Ident name )
  {
    super( type, name );
  }

  @Override
  public boolean isComplete ()
  {
    return this.fields != null;
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
  public boolean isComplete ()
  {
    return spec != null && spec.isComplete();
  }
}

public static final class FunctionSpec extends DerivedSpec
{
  public boolean oldStyle;
  public Member[] params;

  public FunctionSpec ()
  {
    super( TypeSpec.FUNCTION );
  }
  public FunctionSpec ( boolean oldStyle )
  {
    this();
    this.oldStyle = oldStyle;
  }

  @Override
  public boolean isComplete ()
  {
    return false;
  }

  @Override public boolean same ( Spec o )
  {
    if (o == this) return true;
    if (!super.same( o )) return false;
    FunctionSpec x = (FunctionSpec)o;
    if (this.oldStyle != x.oldStyle) return false;
    if (this.params == null) return x.params == null;
    if (this.params.length != x.params.length) return false;

    for ( int e = this.params.length, i = 0; i < e; ++i )
      if (!this.params[i].type.same( x.params[i].type ))
        return false;

    return true;
  }
}

public static class Member extends SourceRange
{
  public final Ident name;
  public final Qual type;

  public Member ( ISourceRange rng, final Ident name, final Qual type  )
  {
    super( rng );
    this.name = name;
    this.type = type;
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
