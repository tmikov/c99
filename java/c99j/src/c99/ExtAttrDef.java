package c99;

public class ExtAttrDef
{
public static enum D { SCLASS, QUAL, SPEC }

public final String name;
public final D disposition;
public final boolean isConst;
public final int flag;

public ExtAttrDef ( final String name, final D disposition, final boolean isConst, final int flag )
{
  this.name = name;
  this.disposition = disposition;
  this.isConst = isConst;
  this.flag = flag;
}
}
