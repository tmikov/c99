package c99.parser;

import c99.Ident;
import c99.Utils;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;

public abstract class IdentTable<T extends Ident>
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

  @Override
  public String toString ()
  {
    return "Key{" +
           "hash=" + hash +
           ", count=" + count +
           '}';
  }
}

private final HashMap<Key,T> m_map = new HashMap<Key, T>();
private final Key m_dummyKey = new Key();

public final T symbol ( byte val[], int offset, int len )
{
  m_dummyKey.setVal( Ident.calcHashCode( val, offset, len ), val, offset, len );

  T symbol = m_map.get( m_dummyKey );
  if (symbol == null)
  {
    Key key = new Key( m_dummyKey.hash, val, offset, len );
    symbol = newIdent( key.val, m_map.size() + 1 );
    m_map.put( key, symbol );
  }

  return symbol;
}

private static final Charset s_latin = Charset.forName( "ISO-8859-1" );

public final T symbol ( String name )
{
  byte[] bytes = name.getBytes( s_latin );
  return symbol( bytes, 0, bytes.length );
}

protected abstract T newIdent ( byte[] bytes, int hash );

} // class

