package c99;

import java.util.HashMap;

public interface Types
{

public static enum TypeSpec
{
  VOID(),

  BOOL(false,1),
  SCHAR(true,8),
  UCHAR(false,8),
  SSHORT(true,16),
  USHORT(false,16),
  SINT(true,32),
  UINT(false,32),
  SLONG(true,32),
  ULONG(false,32),
  SLLONG(true,64),
  ULLONG(false,64),
  FLOAT(32),
  DOUBLE(64),
  LDOUBLE(64),

  ENUM(),

  ARRAY(),
  STRUCT(),
  UNION(),
  FUNCTION(),
  POINTER(),

  ERROR();

  public final boolean arithmetic;
  public final boolean floating;
  public final boolean signed;
  public final int width;
  public final long longMask;
  public final long minValue;
  public final long maxValue;

  TypeSpec ()
  {
    this.arithmetic = false;
    this.floating = false;
    this.signed = false;
    this.width = 0;
    this.longMask = 0;
    this.minValue = 0;
    this.maxValue = 0;
  }

  TypeSpec ( int width )
  {
    assert width > 0;
    this.arithmetic = true;
    this.floating = true;
    this.signed = true;
    this.width = width;
    this.longMask = this.width < 64 ? (1L << this.width) - 1 : ~0L;
    this.minValue = 0;
    this.maxValue = 0;
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
