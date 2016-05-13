package c99;

import c99.parser.Trees;

import java.util.HashMap;

public class Platform
{
private final CompEnv m_env;

public static final boolean LITTLE_ENDIAN = true;

public static final int CHAR_BITS = 8;
public static final int SHORT_BITS = 16;
public static final int INT_BITS = 16;
public static final int LONG_BITS = 32;
public static final int LONGLONG_BITS = 64;

public static final int MIN_ALIGN = 1;
public static final int MAX_ALIGN = 8;

private static final HashMap<String,ExtAttrDef> s_attrDefs = new HashMap<String, ExtAttrDef>();

public Platform ( CompEnv cenv )
{
  m_env = cenv;
}

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

public ExtAttrDef findExtAttr ( String name )
{
  return s_attrDefs.get( name );
}

public ExtAttr parseExtAttr (
  ISourceRange locAll, ISourceRange locName, ExtAttrDef def, Trees.TreeList params
)
{
  // None of the supported attributes have parameters
  if (params != null && params.size() != 0)
  {
    m_env.reporter.error( locAll, "attribute '%s' takes no parameters", def.name );
    return null;
  }
  return new ExtAttr(def);
}

// NOTE: it is safe to create these attributes only once becauise they are 'constant',
// they have no associated data, so they are not actually linked into the attribute chain.
private static final ExtConstAttr s_errorAttr = new ExtConstAttr(s_def_error);
private static final ExtConstAttr s_nearAttr = new ExtConstAttr(s_def_x86near);
private static final ExtConstAttr s_dsAttr = new ExtConstAttr(s_def_x86ds);
private static final ExtConstAttr s_csAttr = new ExtConstAttr(s_def_x86cs);
private static final ExtConstAttr s_farAttr = new ExtConstAttr(s_def_x86far);
private static final ExtConstAttr s_hugeAttr = new ExtConstAttr(s_def_x86huge);

private void attrError ( ExtAttributes attrs, ISourceRange loc, String msg, Object... args )
{
  if ((attrs.flags() & QUAL_ERROR) == 0)
  {
    attrs.add( s_errorAttr );
    m_env.reporter.error( loc, msg, args );
  }
}

public void setDefaultAttrs ( Types.Qual qual )
{
  final ExtAttributes attrs = qual.extAttrs;

  assert (attrs.flags() & QUAL_X86SEGS) == 0;
  assert (attrs.flags() & (QUAL_X86NEAR | QUAL_X86FAR | QUAL_X86HUGE)) == 0;

  if (qual.spec.kind == TypeSpec.FUNCTION) // Code
  {
    if (m_env.opts.defCodePointers == 0)
    {
      qual.extAttrs.add( s_nearAttr );
      qual.extAttrs.add( s_csAttr ); // Near code pointers can only be "cs:"
    }
    else
      qual.extAttrs.add( s_farAttr );
  }
  else  // Data
  {
    if (m_env.opts.defDataPointers == 0)
    {
      qual.extAttrs.add( s_nearAttr );
      qual.extAttrs.add( s_dsAttr );
    }
    else
      qual.extAttrs.add( m_env.opts.defDataPointers == 1 ? s_farAttr : s_hugeAttr );
  }
}

public boolean checkAndCompleteAttrs ( ISourceRange loc, Types.Qual qual )
{
  final ExtAttributes attrs = qual.extAttrs;

  int segs;
  if ((segs = attrs.flags() & QUAL_X86SEGS) != 0)
  {
    attrs.add( s_nearAttr );
    if ((segs & (segs - 1)) != 0)
      attrError( attrs, loc, "more than one 8086 segment qualifier specified" );
  }

  int psize;
  if ((psize = attrs.flags() & (QUAL_X86NEAR | QUAL_X86FAR | QUAL_X86HUGE)) != 0)
  {
    if ((psize & (psize - 1)) != 0)
      attrError( attrs, loc, "more than one 8086 pointer size (near/far/huge) specified" );
  }

  if (qual.spec.kind == TypeSpec.FUNCTION) // Code
  {
    // For functions we always set the necessary attributes
    if (psize == 0)
    {
      qual.extAttrs.add( m_env.opts.defCodePointers == 0 ? s_nearAttr : s_farAttr );
      psize = attrs.flags() & (QUAL_X86NEAR | QUAL_X86FAR | QUAL_X86HUGE);
    }
    if ((psize & QUAL_X86NEAR) != 0)
    {
      if (segs == 0)
        qual.extAttrs.add( s_csAttr ); // Near code pointers can only be "cs:"
      else if ((segs & QUAL_X86CS) == 0)
      {
        qual.extAttrs.add( s_csAttr ); // Near code pointers can only be "cs:"
        attrError(attrs, loc, "invalid segment qualifier for a pointer to function");
      }
    }
  }
  else  // Data
  {
    // If we have a near pointer, set the segment specifier if not already set
    if ((psize & QUAL_X86NEAR) != 0 && segs == 0)
      qual.extAttrs.add( s_dsAttr ); // Near data pointers are "ds:" by default
    // Otherwise leave things as default
  }

  return (attrs.flags() & QUAL_ERROR) == 0;
}

public int pointerSize ( Types.Qual to )
{
  final int flags = to.extAttrs.flags();
  if ((flags & (QUAL_X86HUGE | QUAL_X86FAR)) != 0)
    return 4;
  else if ((flags & QUAL_X86NEAR) != 0)
    return 2;
  else if (to.spec.kind == TypeSpec.FUNCTION)
    return m_env.opts.defCodePointers == 0 ? 2 : 4;
  else
    return m_env.opts.defDataPointers == 0 ? 2 : 4;
}

public TypeSpec pointerOffsetType ( Types.PointerSpec ptr )
{
  return (ptr.of.extAttrs.flags() & QUAL_X86HUGE) != 0 ? TypeSpec.SLONG : TypeSpec.SINT;
}

public int alignment ( int size )
{
  return Math.min(size, m_env.opts.maxAlign);
}

// Convert the bit offset so it matches the storage layout.

/**
 * Convert a bit-offset in a bit-field storage unit to a number matching the
 * storage layout so that incrementing bit offsets are stored in incrementing
 * memory addresses.
 * @param baseType
 * @param offset
 * @return
 */
public int memoryBitOffset ( TypeSpec baseType, int bitOffset, int bitWidth )
{
  if (LITTLE_ENDIAN)
    return bitOffset;
  else // big endian
    return baseType.width - bitOffset - bitWidth;
}

public static final Constant.IntC s_minInt[] =
    new Constant.IntC[TypeSpec.INT_LAST.ordinal() - TypeSpec.INT_FIRST.ordinal() + 1];
public static final Constant.IntC s_maxInt[] =
    new Constant.IntC[TypeSpec.INT_LAST.ordinal() - TypeSpec.INT_FIRST.ordinal() + 1];

static {
  for ( int i = TypeSpec.INT_FIRST.ordinal(); i <= TypeSpec.INT_LAST.ordinal(); ++i )
  {
    TypeSpec ts = TypeSpec.values()[i];
    assert ts.integer;
    s_minInt[i - TypeSpec.INT_FIRST.ordinal()] = Constant.makeLong( ts, ts.minValue );
    s_maxInt[i - TypeSpec.INT_FIRST.ordinal()] = Constant.makeLong( ts, ts.maxValue );
  }
}

public static Constant.IntC minInt ( TypeSpec ts )
{
  assert ts.integer;
  return s_minInt[ts.ordinal() - TypeSpec.INT_FIRST.ordinal()];
}
public static Constant.IntC maxInt ( TypeSpec ts )
{
  assert ts.integer;
  return s_maxInt[ts.ordinal() - TypeSpec.INT_FIRST.ordinal()];
}

/**
 * Determine the enum base type based on platform specifics. For example, on an 8-bit CPU we might want
 * to use a byte to store an enum whose values fit in a byte. In the standard case for 32-bit CPUs we
 * just return the passed value.
 *
 * @param baseSpec
 * @param minValue
 * @param maxValue
 * @return
 */
public TypeSpec determineEnumBaseSpec ( TypeSpec baseSpec, Constant.IntC minValue, Constant.IntC maxValue )
{
  assert baseSpec == minValue.spec && baseSpec == maxValue.spec;

  if (false)
    return baseSpec;
  else
  {
    if (baseSpec.signed)
    {
      if (!minValue.fitsInLong() || !maxValue.fitsInLong())
        return baseSpec;
      long mi = minValue.asLong();
      long ma = maxValue.asLong();

      for ( int i = TypeSpec.SCHAR.ordinal(); i <= TypeSpec.SLLONG.ordinal(); i += 2 )
      {
        final TypeSpec ts = TypeSpec.values()[i];
        if (mi >= ts.minValue && ma <= ts.maxValue)
          return ts.width == TypeSpec.SINT.width ? TypeSpec.SINT : ts;
      }
    }
    else
    {
      if (!minValue.fitsInULong() || !maxValue.fitsInULong())
        return baseSpec;
      long ma = maxValue.asULong();

      for ( int i = TypeSpec.UCHAR.ordinal(); i <= TypeSpec.ULLONG.ordinal(); i += 2 )
      {
        final TypeSpec ts = TypeSpec.values()[i];
        if (!Constant.unsignedLessThan( ts.maxValue, ma ))
          return ts.width == TypeSpec.UINT.width ? TypeSpec.UINT : ts;
      }
    }

    return baseSpec;
  }
}

} // class
