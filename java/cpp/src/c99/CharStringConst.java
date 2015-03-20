package c99;

import java.util.Arrays;

public final class CharStringConst extends AnyStringConst
{
public final byte[] value;

public CharStringConst ( TypeSpec spec, byte[] value )
{
  super(spec);
  this.value = value;
}

@Override
public final int length ()
{
  return this.value.length;
}

@Override
public final int[] wideValue ()
{
  int[] res = new int[this.value.length];
  for ( int i = 0, e = this.value.length; i < e; ++i )
    res[i] = this.value[i] & 255;
  return res;
}

@Override
public final CharStringConst resize ( int toLength )
{
  return toLength != this.value.length ? new CharStringConst( this.spec, Arrays.copyOf(this.value, toLength) ) : this;
}

@Override
public final String toJavaString ()
{
  return Utils.asciiString( this.value );
}
}
