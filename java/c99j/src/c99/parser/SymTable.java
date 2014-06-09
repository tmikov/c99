package c99.parser;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;

import c99.Utils;

public class SymTable
{
private static final class Key
{
  int hash;
  byte val[];
  int offset;
  int count;

  Key () {}

  Key ( final int hash, final byte[] val, int offset, int count )
  {
    this.hash = hash;
    this.val = Arrays.copyOfRange( val, offset, offset + count );
    this.offset = 0;
    this.count = count;
  }

  final void setVal ( final int hash, final byte[] val, final int offset, final int count )
  {
    this.hash = hash;
    this.val = val;
    this.offset = offset;
    this.count = count;
  }

  @Override
  public final int hashCode ()
  {
    return this.hash;
  }

  @Override
  public boolean equals ( final Object o )
  {
    if (this == o)
      return true;
    if (!(o instanceof Key))
      return false;

    Key key = (Key)o;

    return
      hash == key.hash &&
      count == key.count &&
      Utils.equals( this.val, this.offset, key.val, key.offset, this.count );
  }
}

private final HashMap<Key,Symbol> m_map = new HashMap<Key, Symbol>();
private final Key m_dummyKey = new Key();

public Symbol symbol ( byte val[], int offset, int len )
{
  m_dummyKey.setVal( calcHashCode( val, offset, len ), val, offset, len );

  Symbol symbol = m_map.get( m_dummyKey );
  if (symbol == null)
  {
    Key key = new Key( m_dummyKey.hash, val, offset, len );
    symbol = new Symbol( key.val, m_map.size() + 1 );
    m_map.put( key, symbol );
  }

  return symbol;
}

private static final Charset s_latin = Charset.forName( "ISO-8859-1" );

public Symbol symbol ( String name )
{
  byte[] bytes = name.getBytes( s_latin );
  return symbol( bytes, 0, bytes.length );
}

private static int calcHashCode( byte val[], int off, int len )
{
  int h = 0;
  for (int i = 0; i < len; i++) {
      h = 31*h + (val[off++] & 0xFF);
  }
  return h;
}


} // class

