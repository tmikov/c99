package c99;

public enum TypeSpec
{
VOID("void"),

BOOL("bool",false,1),
// Note: the ordering matters. First <signed>, then <unsigned>, from smaller to larger
SCHAR("signed char",true, PlatformBits.CHAR_BITS),
UCHAR("unsigned char",false, PlatformBits.CHAR_BITS),
SSHORT("short",true, PlatformBits.SHORT_BITS),
USHORT("unsigned short",false, PlatformBits.SHORT_BITS),
SINT("int",true, PlatformBits.INT_BITS),
UINT("unsigned",false, PlatformBits.INT_BITS),
SLONG("long",true, PlatformBits.LONG_BITS),
ULONG("unsigned long",false, PlatformBits.LONG_BITS),
SLLONG("long long",true, PlatformBits.LONGLONG_BITS),
ULLONG("unsigned long long",false, PlatformBits.LONGLONG_BITS),
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

public static final TypeSpec INT_FIRST = BOOL;
public static final TypeSpec INT_LAST = ULLONG;
public static final TypeSpec INTMAX_T = SLLONG;
public static final TypeSpec UINTMAX_T = ULLONG;
public static final TypeSpec PTRDIFF_T = SINT;
public static final TypeSpec SIZE_T = UINT;
public static final TypeSpec UINTPTR_T = ULONG;

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
  this.sizeOf = width / PlatformBits.CHAR_BITS; assert width % PlatformBits.CHAR_BITS == 0;
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
  this.sizeOf = (width==1?8:width) / PlatformBits.CHAR_BITS; assert (width==1?8:width) % PlatformBits.CHAR_BITS == 0;
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

public final TypeSpec toSigned ()
{
  assert this.integer && this != BOOL;
  return this.signed ? this : values()[this.ordinal() - 1];
}
public final TypeSpec toUnsigned ()
{
  assert this.integer;
  return this.signed ? values()[this.ordinal() + 1] : this;
}

}
