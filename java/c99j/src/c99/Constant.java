package c99;

public class Constant
{
private Constant () {}

/** Arithmetic constant */
public abstract static class ArithC
{
  public final Types.TypeSpec spec;

  protected ArithC ( Types.TypeSpec spec_ )
  {
    assert spec_.arithmetic;
    this.spec = spec_;
  }

  public abstract void assign ( ArithC a );
  public abstract void castFrom ( ArithC a );

  public abstract boolean isZero ();
  public final boolean isTrue ()
  {
    return !isZero();
  }

  public abstract boolean eq ( ArithC a );
  public abstract boolean ne ( ArithC a );
  public abstract boolean lt ( ArithC a );
  public abstract boolean le ( ArithC a );
  public abstract boolean gt ( ArithC a );
  public abstract boolean ge ( ArithC a );

  public abstract void add ( ArithC a, ArithC b );
  public abstract void sub ( ArithC a, ArithC b );
  public abstract void mul ( ArithC a, ArithC b );
  public abstract void div ( ArithC a, ArithC b );
}

public static final class IntC extends ArithC
{
  private long m_value;

  IntC ( Types.TypeSpec spec_ ) { super(spec_); }

  private void setValue ( long x )
  {
    if (this.spec.signed)
    {
      final int bw = 64 - this.spec.width;
      m_value = (x << bw) >> bw;
    }
    else
      m_value = x & this.spec.longMask;
  }

  @Override
  public final boolean equals ( final Object obj )
  {
    if (obj instanceof IntC)
    {
      IntC that = (IntC)obj;
      if (that.spec == this.spec && that.m_value == this.m_value)
        return true;
    }
    return false;
  }

  @Override
  public final int hashCode ()
  {
    return (int)(m_value ^ (m_value >>> 32));
  }

  @Override
  public final String toString ()
  {
    // print big unsigned values in hex
    if (this.spec.signed || m_value >= 0 && m_value <= Integer.MAX_VALUE)
      return String.format( "%s(%d)", this.spec, m_value );
    else
      return String.format( "%s(0x%X)", this.spec, m_value );
  }

  public final void setBool ( boolean x )
  {
    m_value = x ? 1 : 0;
  }

  public final void setLong ( long x )
  {
    setValue( x );
  }

  @Override
  public final void assign ( final ArithC a )
  {
    assert this.spec == a.spec;
    m_value = ((IntC)a).m_value;
  }

  @Override
  public final void castFrom ( ArithC a )
  {
    if (!a.spec.floating)
      setValue( ((IntC)a).m_value );
    else
      setValue( (long)((RealC)a).m_value );
  }

  @Override
  public final boolean isZero ()
  {
    return m_value == 0;
  }

  @Override
  public final boolean lt ( final ArithC a )
  {
    assert this.spec == a.spec;
    return this.spec.signed ?
      m_value < ((IntC)a).m_value : unsignedLessThan( m_value, ((IntC)a).m_value );
  }

  @Override
  public final boolean eq ( final ArithC a )
  {
    assert this.spec == a.spec;
    return m_value == ((IntC)a).m_value;
  }

  @Override
  public final boolean ne ( ArithC a )
  {
    return !eq( a );
  }
  @Override
  public final boolean le ( ArithC a )
  {
    // this <= a  <=>  !(this > a)  <=>  !(a < this)
    return !a.lt( this );
  }
  @Override
  public final boolean gt ( ArithC a )
  {
    // this > a  <=>  a < this
    return a.lt( this );
  }
  @Override
  public final boolean ge ( ArithC a )
  {
    // this >= a  <=>  !(this < a)
    return !lt( a );
  }

  @Override
  public void add ( final ArithC a, final ArithC b )
  {
    assert this.spec == a.spec && this.spec == b.spec;
    setValue( ((IntC)a).m_value + ((IntC)b).m_value );
  }

  @Override
  public void sub ( final ArithC a, final ArithC b )
  {
    assert this.spec == a.spec && this.spec == b.spec;
    setValue( ((IntC)a).m_value - ((IntC)b).m_value );
  }

  @Override
  public void mul ( final ArithC a, final ArithC b )
  {
    assert this.spec == a.spec && this.spec == b.spec;
    setValue( ((IntC)a).m_value * ((IntC)b).m_value );
  }

  @Override
  public final void div ( ArithC a_, ArithC b_ )
  {
    assert this.spec == a_.spec && this.spec == b_.spec;
    IntC a = (IntC)a_;
    IntC b = (IntC)b_;

    if (b.m_value != 0)
    {
      if (this.spec.signed || a.m_value >= 0 && b.m_value >= 0)
        setValue( a.m_value / b.m_value );
      else
        setValue( unsignedDivide( a.m_value, b.m_value ) );
    }
    else
      setValue( spec.maxValue );
  }

  public final void rem ( IntC a, IntC b )
  {
    assert this.spec == a.spec && this.spec == b.spec;

    if (b.m_value != 0)
    {
      if (this.spec.signed || a.m_value >= 0 && b.m_value >= 0)
        setValue( a.m_value % b.m_value );
      else
        setValue( unsignedRemainder( a.m_value, b.m_value ) );
    }
    else
      setValue( 0 );
  }

  public final void shl ( IntC a, IntC b )
  {
    assert this.spec == a.spec && b.spec == Types.TypeSpec.UINT;
    setValue( a.m_value << b.m_value );
  }

  public final void shr ( IntC a, IntC b )
  {
    assert this.spec == a.spec && b.spec == Types.TypeSpec.UINT;
    if (this.spec.signed)
      setValue( a.m_value >> b.m_value );
    else
      setValue( a.m_value >>> b.m_value );
  }
}

public static final class RealC extends ArithC
{
  private double m_value;

  RealC ( Types.TypeSpec spec_ ) { super(spec_); }

  private void setValue ( double x )
  {
    if (this.spec != Types.TypeSpec.FLOAT)
      m_value = x;
    else
      m_value = (float)x;
  }

  @Override
  public final boolean equals ( final Object o )
  {
    return o instanceof RealC && Double.compare( ((RealC)o).m_value, m_value ) == 0;
  }

  @Override
  public final int hashCode ()
  {
    long temp = m_value != +0.0d ? Double.doubleToLongBits( m_value ) : 0L;
    return (int)(temp ^ (temp >>> 32));
  }

  @Override
  public final String toString ()
  {
    return String.format( "%s(%f)", this.spec, m_value );
  }

  public final void setDouble ( double x )
  {
    setValue( x );
  }

  @Override
  public final void assign ( ArithC a )
  {
    assert this.spec == a.spec;
    m_value = ((RealC)a).m_value;
  }
  @Override
  public final void castFrom ( ArithC a )
  {
    if (a.spec.floating)
      setValue( ((RealC)a).m_value );
    else
      setValue( ((IntC)a).m_value );
  }

  @Override
  public final boolean isZero ()
  {
    return m_value == 0;
  }
  @Override
  public final boolean eq ( ArithC a )
  {
    assert this.spec == a.spec;
    return m_value == ((RealC)a).m_value;
  }
  @Override
  public final boolean ne ( ArithC a )
  {
    assert this.spec == a.spec;
    return m_value != ((RealC)a).m_value;
  }
  @Override
  public final boolean lt ( ArithC a )
  {
    assert this.spec == a.spec;
    return m_value < ((RealC)a).m_value;
  }
  @Override
  public final boolean le ( ArithC a )
  {
    assert this.spec == a.spec;
    return m_value <= ((RealC)a).m_value;
  }
  @Override
  public final boolean gt ( ArithC a )
  {
    assert this.spec == a.spec;
    return m_value > ((RealC)a).m_value;
  }
  @Override
  public final boolean ge ( ArithC a )
  {
    assert this.spec == a.spec;
    return m_value >= ((RealC)a).m_value;
  }

  @Override
  public void add ( final ArithC a, final ArithC b )
  {
    assert this.spec == a.spec && this.spec == b.spec;
    setValue( ((RealC)a).m_value + ((RealC)b).m_value );
  }

  @Override
  public void sub ( final ArithC a, final ArithC b )
  {
    assert this.spec == a.spec && this.spec == b.spec;
    setValue( ((RealC)a).m_value - ((RealC)b).m_value );
  }

  @Override
  public final void mul ( final ArithC a, final ArithC b )
  {
    assert this.spec == a.spec && this.spec == b.spec;
    setValue( ((RealC)a).m_value * ((RealC)b).m_value );
  }

  @Override
  public final void div ( final ArithC a, final ArithC b )
  {
    assert this.spec == a.spec && this.spec == b.spec;
    setValue( ((RealC)a).m_value / ((RealC)b).m_value );
  }
}

public static ArithC newConstant ( Types.TypeSpec spec )
{
  switch (spec)
  {
  case BOOL:
  case SCHAR:
  case UCHAR:
  case SSHORT:
  case USHORT:
  case SINT:
  case UINT:
  case SLONG:
  case ULONG:
  case SLLONG:
  case ULLONG:
    return new IntC( spec );

  case FLOAT:
  case DOUBLE:
  case LDOUBLE:
    return new RealC( spec );

  default:
    assert false : "Invalid constant spec " + spec;
    return null;
  }
}

public static IntC newIntConstant ( Types.TypeSpec spec )
{
  return (IntC)newConstant( spec );
}

public static IntC makeLong ( Types.TypeSpec spec, long value )
{
  IntC c = newIntConstant( spec );
  c.setLong( value );
  return c;
}

public static RealC makeDouble ( Types.TypeSpec spec, double value )
{
  RealC c = (RealC)newConstant( spec );
  c.setDouble( value );
  return c;
}

static boolean unsignedLessThan ( long a, long b )
{
  return (a ^ 0x8000000000000000L) < (b ^ 0x8000000000000000L);
}

static long unsignedDivide ( long a, long b )
{
  // First handle the case of b >= 2**63 (negative in signed representation).
  // The result of the division could only be 0 or 1 (because 'a' can't be
  // twice larger than 2**63).
  //
  if (b < 0) // b >= 2**63
  {
    if (unsignedLessThan( a, b ))
      return 0; // 'a' is smaller, so 0
    else
      return 1; // a >= b, but can't be twice larger (not enough bits), so 1
  }
  // Great. Now we know that 'b' is [0..2**63) (non-negative)

  if (a >= 0) // If 'a' is also [0..2**63), we can just use signed division
    return a / b; // division by zero will go through this path

  // Unfortunately 'a' is in the range [2**63..2**64).
  // We can put it into range by dividing it by two and then calculate an approximation
  // a / b ~= ((a / 2) / b) * 2

  long scaledA = a >>> 1;
  long approx = (scaledA / b) << 1;

  long rem  = a - approx * b;
  return approx + (unsignedLessThan( rem, b ) ? 0 : 1);
}

static long unsignedRemainder ( long a, long b )
{
  if (b < 0) // b >= 2**63
  {
    if (unsignedLessThan( a, b ))
      return a;
    else
      return a - b;
  }
  // Great. Now we know that 'b' is [0..2**63) (non-negative)

  if (a >= 0)
    return a % b; // division by zero will go through this path

  // Unfortunately 'a' is in the range [2**63..2**64).
  // We can put it into range by dividing it by two and then calculate an approximation
  // a / b ~= ((a / 2) / b) * 2

  long scaledA = a >>> 1;
  long approx = (scaledA / b) << 1;

  long rem  = a - approx * b;
  return rem - (unsignedLessThan( rem, b ) ? 0 : b);
}


}
