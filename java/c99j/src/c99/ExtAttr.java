package c99;

public class ExtAttr implements Comparable<ExtAttr>
{
ExtAttr next;

public final ExtAttrDef def;

public ExtAttr ( final ExtAttrDef def )
{
  assert def != null;
  this.def = def;
}

public ExtAttr clone ()
{
  return new ExtAttr(this.def);
}

@Override public int hashCode ()
{
  return def.flag;
}

@Override public boolean equals ( final Object obj )
{
  return this == obj || obj instanceof ExtAttr && equals((ExtAttr) obj);
}

public boolean equals ( ExtAttr o )
{
  return o != null && this.def == o.def;
}

@Override
public int compareTo ( final ExtAttr o )
{
  final int ah = this.hashCode();
  final int bh = o.hashCode();
  if (ah < bh)
    return -1;
  else if (ah > bh)
    return 1;
  else
    return this.def.compareTo( o.def );
}
}
