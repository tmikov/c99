package c99;

import java.util.HashMap;

public interface Types
{

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
}

public static class Qual
{
  public boolean isConst;
  public boolean isVolatile;
  public boolean isRestrict;
  public boolean isAtomic;

  public Spec spec;
}

public static class Spec
{
  public TypeSpec type;

  boolean isComplete () { return true; }
}

public static class DerivedSpec extends Spec
{
  public Qual of;
  public boolean complete;

  boolean isComplete () { return this.complete; }
}

public static class StructUnionSpec extends DerivedSpec
{
  Member[] fields;
  HashMap<String,Member> lookup;
}

public static class ArraySpec extends DerivedSpec
{
  Constant.IntC size;
}

public static class EnumSpec extends DerivedSpec
{

}

public static class FunctionSpec extends DerivedSpec
{
  Member[] params;
}

public static class Member
{
  public String name;
  public Qual type;
}

} // class
