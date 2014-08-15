package c99;

import java.util.Iterator;

public final class ExtAttributes implements Iterable<ExtAttr>
{
private ExtAttr m_head, m_tail;
private int     m_flags;

public final void add ( ExtAttr attr )
{
  assert attr.next == null;
  if (!attr.def.isConst)
  {
    if (m_tail != null)
    {
      assert m_tail.next == null;
      m_tail.next = attr;
    }
    else
      m_head = attr;
    m_tail = attr;
  }
  m_flags |= attr.def.flag;
}

private final void clear ()
{
  m_head = m_tail = null;
  m_flags = 0;
}

public final void transferFrom ( /*nullable*/ ExtAttributes eas )
{
  if (eas == null)
    return;

  if (m_tail != null)
  {
    assert m_tail.next == null;
    m_tail.next = eas.m_head;
  }
  else
    m_head = eas.m_head;
  m_tail = eas.m_tail;
  m_flags |= eas.m_flags;

  eas.clear();
}

public final void combine ( ExtAttributes eas )
{
//  for ( ExtAttr ea : eas )
  for ( ExtAttr ea = eas.m_head; ea != null; ea = ea.next )
    add( ea.clone() );
  m_flags |= eas.m_flags;
}

public final int flags ()
{
  return m_flags;
}

private static final class _Iterator implements Iterator<ExtAttr>
{
  ExtAttr m_next;

  private _Iterator ( final ExtAttr next )
  {
    m_next = next;
  }

  @Override public boolean hasNext ()
  {
    return m_next != null;
  }

  @Override public ExtAttr next ()
  {
    ExtAttr res = m_next;
    m_next = res.next;
    return res;
  }

  @Override public void remove ()
  {}
}

@Override public final _Iterator iterator ()
{
  return new _Iterator(m_head);
}
}
