package c99;

public class ExtAttr
{
ExtAttr next;

public final ExtAttrDef def;

public ExtAttr ( final ExtAttrDef def )
{
  this.def = def;
}

public ExtAttr clone ()
{
  return new ExtAttr(this.def);
}
}
