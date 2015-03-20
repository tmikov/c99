package c99;

import java.util.Arrays;

public final class WideStringConst extends AnyStringConst
{
public final int[] wvalue;

public WideStringConst ( TypeSpec spec, int[] wvalue )
{
  super(spec);
  this.wvalue = wvalue;
}

@Override
public final int length ()
{
  return this.wvalue.length;
}

@Override
public final int[] wideValue ()
{
  return this.wvalue;
}

@Override
public final WideStringConst resize ( int toLength )
{
  return toLength != this.wvalue.length ? new WideStringConst( this.spec, Arrays.copyOf( this.wvalue, toLength ) ) : this;
}

@Override
public final String toJavaString ()
{
  StringBuilder buf = new StringBuilder( this.wvalue.length );
  for (final int cp : this.wvalue)
    buf.appendCodePoint( cp );
  return buf.toString();
}
}
