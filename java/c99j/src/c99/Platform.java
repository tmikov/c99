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
  // Per GCC, attributes are also valid with a "__" prefix and suffix
  s_attrDefs.put( "__" + def.name + "__", def );
}

public static final int QUAL_ERROR = (1 << 0);

public static final int QUAL_CDECL    = (1 << 1);
public static final int QUAL_STDCALL  = (1 << 2);
public static final int QUAL_FASTCALL = (1 << 3);

public static final int QUAL_X86NEAR = (1 <<  4);
public static final int QUAL_X86DS   = (1 <<  5);
public static final int QUAL_X86ES   = (1 <<  6);
public static final int QUAL_X86CS   = (1 <<  7);
public static final int QUAL_X86SS   = (1 <<  8);
public static final int QUAL_X86FAR  = (1 <<  9);
public static final int QUAL_X86HUGE = (1 << 10);

public static final int QUAL_X86SEGS = QUAL_X86DS | QUAL_X86ES | QUAL_X86CS | QUAL_X86SS;

private static final ExtAttrDef s_def_error = new ExtAttrDef("error", ExtAttrDef.D.QUAL, true, QUAL_ERROR);
private static final ExtAttrDef s_def_cdecl = new ExtAttrDef("cdecl", ExtAttrDef.D.QUAL, true, QUAL_CDECL);
private static final ExtAttrDef s_def_stdcall = new ExtAttrDef("stdcall", ExtAttrDef.D.QUAL, true, QUAL_STDCALL);
private static final ExtAttrDef s_def_fastcall = new ExtAttrDef("fastcall", ExtAttrDef.D.QUAL, true, QUAL_FASTCALL);
private static final ExtAttrDef s_def_x86near = new ExtAttrDef("x86near", ExtAttrDef.D.QUAL, true, QUAL_X86NEAR);
private static final ExtAttrDef s_def_x86ds  = new ExtAttrDef("x86ds", ExtAttrDef.D.QUAL, true, QUAL_X86DS);
private static final ExtAttrDef s_def_x86es  = new ExtAttrDef("x86es", ExtAttrDef.D.QUAL, true, QUAL_X86ES);
private static final ExtAttrDef s_def_x86cs  = new ExtAttrDef("x86cs", ExtAttrDef.D.QUAL, true, QUAL_X86CS);
private static final ExtAttrDef s_def_x86ss  = new ExtAttrDef("x86ss", ExtAttrDef.D.QUAL, true, QUAL_X86SS);
private static final ExtAttrDef s_def_x86far = new ExtAttrDef("x86far", ExtAttrDef.D.QUAL, true, QUAL_X86FAR);
private static final ExtAttrDef s_def_x86huge = new ExtAttrDef("x86huge", ExtAttrDef.D.QUAL, true, QUAL_X86HUGE);

static {
  defAttr(s_def_cdecl);
  defAttr(s_def_stdcall);
  defAttr(s_def_fastcall);

  defAttr(s_def_x86near);
  defAttr(s_def_x86ds);
  defAttr(s_def_x86es);
  defAttr(s_def_x86cs);
  defAttr(s_def_x86ss);
  defAttr(s_def_x86far);
  defAttr(s_def_x86huge);

}

public static ExtAttrDef findExtAttr ( String name )
{
  return s_attrDefs.get( name );
}

public static ExtAttr parseExtAttr (
  CompEnv env, ISourceRange locAll, ISourceRange locName, ExtAttrDef def, Trees.TreeList params
)
{
  // None of the supported attributes have parameters
  if (params != null && params.size() != 0)
  {
    env.reporter.error( locAll, "attribute '%s' takes no parameters", def.name );
    return null;
  }
  return new ExtAttr(def);
}

private static final ExtAttr s_errorAttr = new ExtAttr(s_def_error);
private static final ExtAttr s_nearAttr = new ExtAttr(s_def_x86near);
private static final ExtAttr s_dsAttr = new ExtAttr(s_def_x86ds);
private static final ExtAttr s_csAttr = new ExtAttr(s_def_x86cs);
private static final ExtAttr s_farAttr = new ExtAttr(s_def_x86far);
private static final ExtAttr s_hugeAttr = new ExtAttr(s_def_x86huge);

private static void attrError ( CompEnv env, ExtAttributes attrs, ISourceRange loc, String msg, Object... args )
{
  if ((attrs.flags() & QUAL_ERROR) == 0)
  {
    attrs.add( s_errorAttr );
    env.reporter.error( loc, msg, args );
  }
}

public static void setDefaultAttrs ( CompEnv env, ISourceRange loc, Types.Qual qual )
{
  final ExtAttributes attrs = qual.extAttrs;

  int segs;
  if ((segs = attrs.flags() & QUAL_X86SEGS) != 0)
  {
    attrs.add( s_nearAttr );
    if ((segs & (segs - 1)) != 0)
      attrError( env, attrs, loc, "more than one 8086 segment qualifier specified" );
  }

  int psize;
  if ((psize = attrs.flags() & (QUAL_X86NEAR | QUAL_X86FAR | QUAL_X86HUGE)) != 0)
  {
    if ((psize & (psize - 1)) != 0)
      attrError( env, attrs, loc, "more than one 8086 pointer size (near/far/huge) specified" );
  }

  if (qual.spec.type == Types.TypeSpec.FUNCTION) // Code
  {
    if (psize == 0)
      qual.extAttrs.add( env.opts.defCodePointers == 0 ? s_nearAttr : s_farAttr );
    if ((psize & QUAL_X86NEAR) != 0)
    {
      if (segs == 0)
        qual.extAttrs.add( s_csAttr ); // Near code pointers can only be "cs:"
      else if ((segs & QUAL_X86CS) == 0)
      {
        qual.extAttrs.add( s_csAttr ); // Near code pointers can only be "cs:"
        attrError(env, attrs, loc, "invalid segment qualifier for a pointer to function");
      }
    }
  }
  else  // Data
  {
    if (psize == 0)
      qual.extAttrs.add( env.opts.defDataPointers == 0 ? s_nearAttr :
                                (env.opts.defDataPointers == 1 ? s_farAttr : s_hugeAttr) );

    if ((psize & QUAL_X86NEAR) != 0 && segs == 0)
      qual.extAttrs.add( s_dsAttr ); // Near data pointers are "ds:" by default
  }
}

public static int pointerSize ( Types.Qual qual )
{
  final int flags = qual.extAttrs.flags();
  if ((flags & (QUAL_X86HUGE | QUAL_X86FAR)) != 0)
    return 4;
  else if ((flags & QUAL_X86NEAR) != 0)
    return 2;
  else
  {
    assert false : "Default memory space must have been set";
    return 0;
  }
}

} // class
