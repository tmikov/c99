package c99;

/** A readability enhancer used for const attributes */
public class ExtConstAttr extends ExtAttr
{
public ExtConstAttr ( ExtAttrDef def )
{
  super(def);
  assert def.isConst;
}
}
