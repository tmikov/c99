package c99;

import java.util.Iterator;

public final class ExtAttributes implements Iterable<ExtAttr>
{
private ExtAttr m_head; // Sorted
private int     m_flags;

public final boolean isEmpty ()
{
  return m_flags == 0 && m_head == null;
}

/**
 * @return true if the collection changed as a result of this
 */
public final boolean add ( ExtAttr attr )
{
  assert attr.next == null;

  final int flag = attr.def.flag;
  if (attr.def.isConst)
  {
    if ((m_flags & flag) != 0)
      return false;
    m_flags |= flag;
    return true;
  }

  m_flags |= flag;

  // Insertion sort
  ExtAttr prev = null;
  ExtAttr cur = m_head;
  int c = -1;
  while (cur != null && (c = cur.compareTo(attr)) < 0)
  {
    prev = cur;
    cur = cur.next;
  }

  if (c == 0)
    return false; // Duplicate!

  attr.next = cur;
  if (prev != null)
    prev.next = attr;
  else
    m_head = attr;

  return true;
}

private final void clear ()
{
  m_head = null;
  m_flags = 0;
}

private final ExtAttr removeFirst ()
{
  if (m_head != null)
  {
    ExtAttr res = m_head;
    m_head = res.next;
    res.next = null;
    return res;
  }
  else
    return null;
}

public final void transferFrom ( /*nullable*/ ExtAttributes eas )
{
  if (eas == null)
    return;

  m_flags |= eas.m_flags;

  if (m_head == null)  // Fast case: just take ownership of all elements
    m_head = eas.m_head;
  else
  {
    ExtAttr ea;
    while ( (ea = eas.removeFirst()) != null)
      add( ea );
  }
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

public final boolean same ( ExtAttributes eas )
{
  if (m_flags != eas.m_flags)
    return false;
  // Compare the two sorted lists
  ExtAttr a = m_head, b = eas.m_head;
  for(;;)
  {
    if (a == null)
      return b == null;
    if (!a.equals( b ))
      return false;
    a = a.next;
    b = b.next;
  }
}

public static boolean same ( ExtAttributes a, ExtAttributes b )
{
  return a != null ? b != null && a.same( b ) : b == null;
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
