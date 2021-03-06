package c99;

public class Ident implements Comparable<Ident>
{
public final byte[] bytes;
public final String name;
private final int m_hash;

public Ident ( byte bytes[], int hash )
{
  this.bytes = bytes;
  this.name = Utils.asciiString( bytes, 0, bytes.length );
  m_hash = hash;
}

public final int length ()
{
  return this.bytes.length;
}

@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
@Override
public final boolean equals ( final Object o )
{
  return o == this;
}

@Override
public final int hashCode ()
{
  return m_hash;
}

@Override
public String toString ()
{
  return this.name;
}

public static int calcHashCode( byte val[], int off, int len )
{
  int h = 0;
  for (int i = 0; i < len; i++) {
      h = 31*h + (val[off++] & 0xFF);
  }
  return h;
}

@Override
public final int compareTo ( Ident o )
{
  if (this == o)
    return 0;
  return m_hash < o.m_hash ? -1 :
          (m_hash > o.m_hash ? +1 : Utils.compare(this.bytes, 0, this.bytes.length, o.bytes, 0, o.bytes.length));
}
} // class
