package c99;

import c99.parser.Trees;

import java.util.HashMap;

public class Platform
{
public static final int CHAR_BITS = 8;
public static final int SHORT_BITS = 16;
public static final int INT_BITS = 16;
public static final int LONG_BITS = 32;
public static final int LONGLONG_BITS = 64;

public static final int MIN_ALIGN = 1;
public static final int MAX_ALIGN = 8;

private static final HashMap<String,ExtAttrDef> s_attrDefs = new HashMap<String, ExtAttrDef>();
private static void defAttr ( ExtAttrDef def )
{
  s_attrDefs.put( def.name, def );
}

public static final int QUAL_X86NEAR = (1 << 0);
public static final int QUAL_X86FAR  = (1 << 1);
public static final int QUAL_X86HUGE = (1 << 2);

public static final int QUAL_CDECL    = (1 << 3);
public static final int QUAL_STDCALL  = (1 << 4);
public static final int QUAL_FASTCALL = (1 << 5);

static {
  defAttr(new ExtAttrDef("x86near", ExtAttrDef.D.QUAL, true, QUAL_X86NEAR));
  defAttr(new ExtAttrDef("x86far",  ExtAttrDef.D.QUAL, true, QUAL_X86FAR));
  defAttr(new ExtAttrDef("x86huge", ExtAttrDef.D.QUAL, true, QUAL_X86HUGE));

  defAttr(new ExtAttrDef("cdecl",   ExtAttrDef.D.QUAL, true, QUAL_CDECL));
  defAttr(new ExtAttrDef("stdcall", ExtAttrDef.D.QUAL, true, QUAL_STDCALL));
  defAttr(new ExtAttrDef("fastcall",ExtAttrDef.D.QUAL, true, QUAL_FASTCALL));

}

public static ExtAttrDef findExtAttr ( String name )
{
  ExtAttrDef res;
  if ((res = s_attrDefs.get( name )) == null)
  {
    // Per GCC, attributes are also valid with a "__" prefix and suffix
    if (name.length() > 4 && name.startsWith("__") && name.endsWith("__"))
      res = s_attrDefs.get( name.substring( 2, name.length() - 2 ) );
  }
  return res;
}

public static ExtAttr parseExtAttr (
  IErrorReporter reporter, ISourceRange locAll, ISourceRange locName, ExtAttrDef def, Trees.TreeList params
)
{
  // None of the supported attributes have parameters
  if (params != null && params.size() != 0)
  {
    reporter.error( locAll, "attribute '%s' takes no parameters", def.name );
    return null;
  }
  return new ExtAttr(def);
}

} // class
