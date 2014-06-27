package c99.parser.pp;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

public class TokenList<E extends PPLexer.AbstractToken> implements Iterable<E>
{
private int m_size;

private final PPLexer.AbstractToken m_head = new PPLexer.AbstractToken()
{
  @SuppressWarnings("CloneDoesntCallSuperClone")
  @Override public PPLexer.AbstractToken clone () { return null; }
  @Override public boolean same ( final PPLexer.AbstractToken tok ) { return false; }
  @Override public int length () { return 0; }
  @Override public void output ( final OutputStream out ) throws IOException {}
};

public TokenList ()
{
  m_head.m_prev = m_head.m_next = m_head;
}

public final int size ()
{
  return m_size;
}

public final boolean isEmpty ()
{
  // return m_head.m_next == m_head
  return m_size == 0;
}

@SuppressWarnings("unchecked")
private final E cast ( PPLexer.AbstractToken t )
{
  return (E)t;
}

public final E first ()
{
  return cast( m_head.m_next );
}

public final E last ()
{
  return cast(m_head.m_prev);
}

public final E next ( E elem )
{
  return elem.m_next != m_head ? cast(elem.m_next) : null;
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
  insertBefore( elem, m_head );
}

public final void addLastClone ( E elem )
{
  insertBefore( cast(elem.clone()), m_head );
}

public final void remove ( E elem )
{
  elem.m_prev.m_next = elem.m_next;
  elem.m_next.m_prev = elem.m_prev;
  elem.m_prev = elem.m_next = null;

  --m_size;
}

public final E removeFirst ()
{
  E elem = first();
  remove( elem );
  return elem;
}

public final E removeLast ()
{
  E elem = last();
  remove( elem );
  return elem;
}

public final void transferAllBefore ( TokenList<E> fromList, PPLexer.AbstractToken before )
{
  if (fromList.isEmpty())
    return;

  PPLexer.AbstractToken fhead = fromList.m_head;
  PPLexer.AbstractToken last = before.m_prev;
  fhead.m_prev.m_next = before;
  before.m_prev = fhead.m_prev;
  fhead.m_next.m_prev = last;
  last.m_next = fhead.m_next;

  fhead.m_prev = fhead.m_next = fhead;
  m_size += fromList.m_size;
  fromList.m_size = 0;
}

public final void transferAllBeforeFirst ( TokenList<E> fromList )
{
  transferAllBefore( fromList, m_head.m_next );
}

public final void transferAll ( TokenList<E> fromList )
{
  transferAllBefore( fromList, m_head );
}

public final void addAllClone ( TokenList<E> fromList )
{
  final PPLexer.AbstractToken fhead = fromList.m_head;
  for ( PPLexer.AbstractToken cur = fhead.m_next; cur != fhead; cur = cur.m_next )
    addLastClone( cast(cur) );
}

public final String toString ()
{
  StringBuilder res = new StringBuilder();
  res.append( "TokenList{" );
  for ( PPLexer.AbstractToken cur = m_head.m_next; cur !=  m_head; cur = cur.m_next )
  {
    if (cur != m_head.m_next)
      res.append( ", " );
    res.append( cur.toString() );
  }
  res.append( "}" );
  return res.toString();
}

@Override
public Iterator<E> iterator ()
{
  return new Iterator<E>()
  {
    private PPLexer.AbstractToken m_cur = m_head.m_next;

    @Override
    public boolean hasNext ()
    {
      return m_cur != m_head;
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
