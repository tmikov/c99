package c99.parser;

import c99.Utils;
import c99.parser.pp.PPSymCode;

public class Symbol
{
public final byte[] bytes;
public final String name;
private final int m_hash;

public PPSymCode ppCode;
public Object ppDecl;
public Code   keyword;

public Symbol ( byte bytes[], int hash )
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
  return "Symbol{" +
         "name='" + name + '\'' +
         ", m_hash=" + m_hash +
         '}';
}
} // class

