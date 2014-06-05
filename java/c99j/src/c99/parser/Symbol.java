package c99.parser;

import c99.Utils;

public class Symbol
{
public final byte[] bytes;
public final String name;
private final int m_hash;

public Symbol ( byte bytes[], int hash )
{
  this.bytes = bytes;
  this.name = Utils.asciiString( bytes, 0, bytes.length );
  m_hash = hash;
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

} // class

