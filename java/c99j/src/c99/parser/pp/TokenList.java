package c99.parser.pp;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

public class TokenList<E extends PPLexer.AbstractToken> implements Iterable<E>
{
private int m_size;

private final PPLexer.AbstractToken m_list = new PPLexer.AbstractToken()
{
  @SuppressWarnings("CloneDoesntCallSuperClone")
  @Override public PPLexer.AbstractToken clone () { return null; }
  @Override public boolean same ( final PPLexer.AbstractToken tok ) { return false; }
  @Override public int length () { return 0; }
  @Override public void output ( final OutputStream out ) throws IOException {}
};

public TokenList ()
{
  m_list.m_prev = m_list.m_next = m_list;
}

public final int size ()
{
  return m_size;
}

public final boolean isEmpty ()
{
  // return m_list.m_next == m_list
  return m_size == 0;
}

@SuppressWarnings("unchecked")
private final E cast ( PPLexer.AbstractToken t )
{
  return (E)t;
}

public final E first ()
{
  return m_list.m_next != m_list ? cast( m_list.m_next ) : null;
}

public final E last ()
{
  return m_list.m_prev != m_list ? cast(m_list.m_prev) : null;
}

public final E next ( E elem )
{
  return elem.m_next != m_list ? cast(elem.m_next) : null;
}

public final void insertBefore ( E elem, PPLexer.AbstractToken before )
{
  assert elem.m_prev == null && elem.m_next == null;

  PPLexer.AbstractToken last = before.m_prev;
  elem.m_next = before;
  before.m_prev = elem;
  elem.m_prev = last;
  last.m_next = elem;

  ++m_size;
}

public final void addLast ( E elem )
{
  insertBefore( elem, m_list );
}

public final void remove ( E elem )
{
  elem.m_prev.m_next = elem.m_next;
  elem.m_next.m_prev = elem.m_prev;
  elem.m_prev = elem.m_next = null;

  --m_size;
}

@Override
public Iterator<E> iterator ()
{
  return new Iterator<E>()
  {
    private PPLexer.AbstractToken m_cur = m_list.m_next;

    @Override
    public boolean hasNext ()
    {
      return m_cur != m_list;
    }

    @Override
    public E next ()
    {
      E res = cast(m_cur);
      m_cur = m_cur.m_next;
      return res;
    }

    @Override
    public void remove ()
    {
      TokenList.this.remove( cast(m_cur.m_prev) );
    }
  };
}
} // class
