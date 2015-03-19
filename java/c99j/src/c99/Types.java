package c99;

import java.util.ArrayList;
import java.util.HashMap;

public class Types
{
private Types () {}

public static interface TypeVisitor
{
  public boolean visitSimple ( Qual q, SimpleSpec s );
  public boolean visitBased ( Qual q, BasedSpec s );
  public boolean visitAtomic ( Qual q, BasedSpec s );
  public boolean visitPointer ( Qual q, PointerSpec s );
  public boolean visitArray ( Qual q, ArraySpec s );
  public boolean visitStructUnion ( Qual q, StructUnionSpec s );
  public boolean visitEnum ( Qual q, EnumSpec s );
  public boolean visitFunction ( Qual q, FunctionSpec s );
}

public static boolean visitAllPre ( Qual q, TypeVisitor v )
{
  for (;;)
  {
    if (!q.spec.visit( q, v ))
      return false;
    if (q.spec instanceof DerivedSpec)
      q = ((DerivedSpec) q.spec).of;
    else
      break;
  }
  return true;
}

public static boolean visitAllPost ( Qual q, TypeVisitor v )
{
  final ArrayList<Qual> stack = new ArrayList<Qual>( 16 );
  for (;;)
  {
    stack.add( q );
    if (q.spec instanceof DerivedSpec)
      q = ((DerivedSpec) q.spec).of;
    else
      break;
  }
  for ( int i = stack.size(); i > 0; )
  {
    final Qual qq = stack.get(--i);
    if (!qq.spec.visit( qq, v ))
      return false;
  }
  return true;
}

public static enum SClass
{
  NONE,
  TYPEDEF,
  EXTERN,
  STATIC,
  AUTO,
  REGISTER,
}

public static final class Qual
{
  public boolean isConst;
  public boolean isVolatile;
  public boolean isRestrict;
  public boolean isAtomic;
  public final ExtAttributes extAttrs = new ExtAttributes();

  public final Spec spec;

  public Qual ( final Spec spec )
  {
    assert spec != null;
    this.spec = spec;
  }

  public final boolean isVoid ()
  {
    return this.spec.kind == TypeSpec.VOID;
  }

  public final boolean isUnqualified ()
  {
    return
      !isConst &&
      !isVolatile &&
      !isRestrict &&
      !isAtomic &&
      extAttrs.isEmpty();
  }

  public final Qual newUnqualified ()
  {
    return this.isUnqualified() ? this : new Qual(spec);
  }

  public final Qual copy ( Spec newSpec )
  {
    Qual q = new Qual(newSpec);
    q.combine( this ); // save us some typing
    return q;
  }

  public final Qual copy ()
  {
    return copy( this.spec );
  }

  public final void combine ( Qual q )
  {
    this.isConst |= q.isConst;
    this.isVolatile |= q.isVolatile;
    this.isRestrict |= q.isRestrict;
    this.isAtomic |= q.isAtomic;
    this.extAttrs.combine( q.extAttrs );
  }

  /**
   * Is a type valid for a re-declaration of the same symbol.
   * @param qual
   * @return
   */
  public final boolean compatible ( Qual qual )
  {
    return this == qual ||
           isConst == qual.isConst &&
           isVolatile == qual.isVolatile &&
           isRestrict == qual.isRestrict &&
           isAtomic == qual.isAtomic &&
           extAttrs.same( qual.extAttrs ) &&
           spec.compatible( qual.spec );
  }

  private static boolean ge ( boolean a, boolean b )
  {
    return a == b || a;
  }

  /**
   * Checks if our qualifiers are more restrictive than the parameter; in other words
   * the parameter can be assigned to us, but we can't be assigned to it.
   * @param q
   * @return true, if this is more restrictive
   */
  public final boolean moreRestrictiveOrEqual ( Qual q )
  {
    return
      ge(isConst, q.isConst) &&
      ge(isVolatile, q.isVolatile) &&
      ge(isRestrict, q.isRestrict) &&
      ge(isAtomic, q.isAtomic) &&
      extAttrs.same( q.extAttrs );
  }

  @Override public final String toString ()
  {
    StringBuilder buf = new StringBuilder();
    if (isConst) buf.append( "const " );
    if (isVolatile) buf.append( "volatile " );
    if (isRestrict) buf.append( "restrict " );
    if (isAtomic) buf.append(  "atomic " );
    // FIXME: append the attributes
    buf.append( spec.toString() );
    return buf.toString();
  }

  public final String readableType ()
  {
    return toString();
  }

  public final boolean isConstMember ()
  {
    return (spec.kind == TypeSpec.STRUCT || spec.kind == TypeSpec.UNION) &&
            ((StructUnionSpec)spec).isConstMember();
  }
}

public static abstract class Spec
{
  public final TypeSpec kind;
  public final ExtAttributes extAttrs = new ExtAttributes();
  protected boolean m_complete;
  private long m_size;
  private int m_align;

  public Spec ( final TypeSpec kind, boolean complete )
  {
    this.kind = kind;
    m_complete = complete;
    m_size = -1;
  }

  public final boolean isInteger ()
  {
    return this.kind.integer || this.kind == TypeSpec.ENUM;
  }
  public final boolean isFloating ()
  {
    return this.kind.floating;
  }
  public final boolean isScalar ()
  {
    return this.kind.arithmetic || this.kind == TypeSpec.ENUM || this.kind == TypeSpec.POINTER;
  }
  public final boolean isArithmetic ()
  {
    return this.kind.arithmetic || this.kind == TypeSpec.ENUM;
  }
  public final boolean isPointer ()
  {
    return this.kind == TypeSpec.POINTER;
  }
  public final boolean isArray ()
  {
    return this.kind == TypeSpec.ARRAY;
  }
  public final boolean isEnum ()
  {
    return this.kind == TypeSpec.ENUM;
  }
  public final boolean isStructUnion ()
  {
    return this.kind == TypeSpec.STRUCT || this.kind == TypeSpec.UNION;
  }

  public final TypeSpec effectiveKind ()
  {
    assert this.kind != TypeSpec.ENUM || this.isComplete();
    return this.kind == TypeSpec.ENUM ? ((EnumSpec)this).getBaseSpec().effectiveKind() : this.kind;
  }

  public abstract boolean visit ( Qual q, TypeVisitor v );

  public abstract boolean isError ();

  public final boolean isComplete ()
  {
    return m_complete;
  }

  protected final void setSizeAlign ( long size, int align )
  {
    m_size = size;
    m_align = align;
  }

  public final long sizeOf ()
  {
    return m_size;
  }

  public final int alignOf ()
  {
    return m_align;
  }

  public boolean compatible ( Spec o )
  {
    return o == this || this.kind == o.kind && extAttrs.same( o.extAttrs );
  }

  @Override public String toString ()
  {
    return kind.str;
  }

  public final String readableType ()
  {
    return toString();
  }
}

public static final class SimpleSpec extends Spec
{
  public SimpleSpec ( final TypeSpec type, int size, int align )
  {
    super(type, size > 0);
    if (size > 0)
    {
      assert type.sizeOf == size && align > 0;
      setSizeAlign( size, align );
    }
    else
    {
      assert type.sizeOf == 0 && size == -1 && align == 0;
    }
  }

  @Override
  public boolean visit ( Qual q, TypeVisitor v )
  {
    return v.visitSimple( q, this );
  }

  @Override
  public boolean isError ()
  {
    return this.kind == TypeSpec.ERROR;
  }
}

/** Complex, Imaginary, Atomic */
public static final class BasedSpec extends Spec
{
  public final Spec on;

  public BasedSpec ( TypeSpec type, Spec on )
  {
    super( type, on.isComplete() );
    assert type == TypeSpec.COMPLEX || type == TypeSpec.IMAGINARY || type == TypeSpec.ATOMIC;
    this.on = on;
  }

  @Override
  public boolean visit ( Qual q, TypeVisitor v )
  {
    return this.kind == TypeSpec.ATOMIC ? v.visitAtomic( q, this ) : v.visitBased( q, this );
  }

  @Override
  public boolean isError ()
  {
    return this.on.isError();
  }

  @Override public boolean compatible ( Spec o )
  {
    return super.compatible( o ) && this.on.compatible( ((BasedSpec) o).on );
  }

  @Override public final String toString ()
  {
    return kind + " of " + on;
  }
}

public static abstract class DerivedSpec extends Spec
{
  public Qual of;

  public DerivedSpec ( final TypeSpec type, boolean complete, Qual of )
  {
    super( type, complete );
    this.of = of;
  }

  @Override
  public boolean compatible ( Spec o )
  {
    return super.compatible( o ) && this.of.compatible( ((DerivedSpec) o).of );
  }

  @Override public String toString ()
  {
    return kind + " of " + of;
  }
}

public static final class PointerSpec extends DerivedSpec
{
  public long staticSize; // from ArraySpec.size and _static

  public PointerSpec ( Qual of, int size, int align )
  {
    super(TypeSpec.POINTER, true, of);
    setSizeAlign( size, align );
  }

  @Override
  public boolean visit ( Qual q, TypeVisitor v )
  {
    return v.visitPointer( q, this );
  }

  @Override
  public boolean isError ()
  {
    return false;
  }

  @Override public final boolean compatible ( Spec o )
  {
    return
      super.compatible( o ) && this.staticSize == ((PointerSpec)o).staticSize;
  }

  @Override public final String toString ()
  {
    return staticSize > 0 ? "pointer to /static "+ staticSize+"/ "+ of : "pointer to "+ of;
  }
}

public static final class ArraySpec extends DerivedSpec
{
  private long m_nelem;
  public boolean _static;
  public boolean asterisk;

  public ArraySpec ( Qual of, long nelem, long size )
  {
    super( TypeSpec.ARRAY, of.spec.isComplete() && nelem >= 0, of );
    m_nelem = nelem;
    setSizeAlign( size, of.spec.alignOf() );
  }

  public ArraySpec ( Qual of )
  {
    this( of, -1, -1 );
  }

  @Override
  public boolean visit ( Qual q, TypeVisitor v )
  {
    return v.visitArray( q, this );
  }

  @Override
  public boolean isError ()
  {
    return this.of.spec.isError();
  }

  public final boolean hasNelem ()
  {
    return m_nelem >= 0;
  }

  public final long getNelem ()
  {
    return m_nelem;
  }

  @Override public boolean compatible ( Spec o )
  {
    if (!super.compatible( o ))
      return false;
    ArraySpec x = (ArraySpec) o;
    return m_nelem < 0 || x.m_nelem < 0 || m_nelem == x.m_nelem;
  }

  @Override public final String toString ()
  {
    StringBuilder buf = new StringBuilder();
    buf.append( "array[" );
    if (_static)
      buf.append( "static" );
    if (m_nelem >= 0)
    {
      if (_static)
        buf.append(' ');
      buf.append( m_nelem );
    }
    if (asterisk)
      buf.append( '*' );
    buf.append( ']' );
    buf.append( of.toString() );
    return buf.toString();
  }
}

public static abstract class TagSpec extends Spec
{
  public final Ident name;
  private boolean m_error;

  public TagSpec ( final TypeSpec type, final Ident name )
  {
    super( type, false );
    this.name = name;
  }

  @Override public boolean compatible ( Spec o )
  {
    return this == o;
  }

  @Override
  public boolean isError ()
  {
    return m_error;
  }

  public final void orError ( boolean err )
  {
    m_error |= err;
  }

  public String toString ()
  {
    return this.name != null ? this.kind.str + " " + this.name.name : this.kind.str;
  }
}

public static final class StructUnionSpec extends TagSpec
{
  private Member[] m_fields;
  private HashMap<Ident,Member> m_lookup;
  private boolean m_constMember; //< at least one member is const

  public StructUnionSpec ( final TypeSpec type, final Ident name )
  {
    super( type, name );
    assert type == TypeSpec.STRUCT || type == TypeSpec.UNION;
  }

  @Override
  public boolean visit ( Qual q, TypeVisitor v )
  {
    return v.visitStructUnion( q, this );
  }

  public void setFields ( Member[] fields, long size, int align )
  {
    assert m_fields == null;
    m_complete = fields != null;
    m_fields = fields;

    // Scan the fields for const-ness
    if (fields != null)
    {
      m_lookup = new HashMap<Ident, Member>( (int)(fields.length / 0.75)+1 );
      for (Member f : fields)
      {
        m_lookup.put( f.name, f );
        if (f.type.isConst || f.type.isConstMember())
          m_constMember = true;
      }
    }

    setSizeAlign( size, align );
  }

  public Member[] getFields ()
  {
    return m_fields;
  }

  public Member lookupMember ( Ident name )
  {
    return m_lookup != null ? m_lookup.get( name ) : null;
  }

  public boolean isConstMember ()
  {
    return m_constMember;
  }
}

public static final class EnumSpec extends TagSpec
{
  private SimpleSpec m_baseSpec;

  public EnumSpec ( final Ident name )
  {
    super( TypeSpec.ENUM, name );
  }

  @Override
  public boolean visit ( Qual q, TypeVisitor v )
  {
    return v.visitEnum( q, this );
  }

  @Override
  public boolean isError ()
  {
    return false;
  }

  public SimpleSpec getBaseSpec ()
  {
    assert m_baseSpec != null;
    return m_baseSpec;
  }

  public void setBaseSpec ( SimpleSpec baseSpec )
  {
    assert m_baseSpec == null;
    m_baseSpec = baseSpec;
    setSizeAlign( baseSpec.sizeOf(), baseSpec.alignOf() );
    m_complete = baseSpec.isComplete();
  }
}

public static final class FunctionSpec extends DerivedSpec
{
  private final boolean m_oldStyle;
  private final boolean m_ellipsis;
  private final Param[] m_params;
  private boolean m_error;

  public static final Param[] NO_PARAMS = new Param[0];

  public FunctionSpec ( boolean oldStyle, Param[] params, boolean ellipsis, Qual returning )
  {
    super( TypeSpec.FUNCTION, false, returning );
    m_oldStyle = oldStyle;
    m_ellipsis = ellipsis;
    m_params = params != null ? params : NO_PARAMS;

    for ( Param p : m_params )
      if (p.isError())
      {
        m_error = true;
        break;
      }
  }

  @Override
  public boolean isError ()
  {
    return m_error;
  }

  @Override
  public boolean visit ( Qual q, TypeVisitor v )
  {
    return v.visitFunction( q, this );
  }

  public final boolean isOldStyle () { return m_oldStyle; }
  public final boolean isNewStyle () { return !m_oldStyle; };
  public final boolean getEllipsis () { return m_ellipsis; };

  public final boolean isVoidParams ()
  {
    return !m_oldStyle && !m_ellipsis && m_params.length == 0;
  }

  public final Param[] getParams ()
  {
    return m_params;
  }

  @Override public boolean compatible ( Spec o )
  {
    if (o == this)
      return true;

    // Check for if 'o' is a function and has a compatible return type
    if (!super.compatible( o ))
      return false;

    // 6.7.6.3[15]

    final FunctionSpec x = (FunctionSpec)o;

    if (this.isNewStyle() && x.isNewStyle()) // both new-style specifiers
    {
      if (this.getEllipsis() != x.getEllipsis())
        return false;
      if (this.m_params.length != x.m_params.length)
        return false;
      for ( int i = 0; i < this.m_params.length; ++i )
      {
        final Param pa = this.m_params[i];
        final Param pb = x.m_params[i];
        if (!pa.type.compatible( pb.type ) || !ExtAttributes.same(pa.extAttrs, pb.extAttrs))
          return false;
      }
    }
    else if (this.isOldStyle() && x.isOldStyle())
    {
      // Nothing to do here. Both functions are old-style, so they are compatible
    }
    else
    {
      // One new style and one old-style
      final FunctionSpec newS = this.isNewStyle() ? this : x;

      if (newS.getEllipsis()) // "..." can never be compatible with an old-style function
        return false;

      for ( Param p : newS.m_params)
      {
        final Spec spec = p.type.spec;

        // An incomplete enum is not compatible because we don't know its size
        if (spec.isEnum() && !spec.isComplete())
          return false;

        // The parameter type must be compatible with the promoted type
        TypeSpec ts = spec.effectiveKind();
        if (ts != TypeRules.defaultArgumentPromotion(ts))
          return false;
      }
    }

    return true;
  }

  @Override public String toString ()
  {
    StringBuilder buf = new StringBuilder();
    buf.append( "function(" );
    if (isNewStyle())
    {
      if (isVoidParams())
        buf.append( "void" );
      else
      {
        for ( int i = 0; i < m_params.length; ++i )
        {
          final Param param = m_params[i];
          if (i > 0)
            buf.append( ", " );
          if (param.name != null)
            buf.append("/*").append( param.name.name ).append("*/ ");
          if (param.type != null)
            buf.append( param.type.toString() );
        }
        if (getEllipsis())
        {
          if (m_params.length > 0)
            buf.append( ", " );
          buf.append( "..." );
        }
      }
    }
    buf.append( ") returning " );
    buf.append( of.toString() );
    return buf.toString();
  }
}

public static class Param extends SourceRange
{
  public final int index;
  public final Ident name;
  public final Qual type;
  public final ExtAttributes extAttrs;

  public Param ( ISourceRange rng, int index, final Ident name, final Qual type, ExtAttributes extAttrs  )
  {
    super( rng );
    this.index = index;
    this.name = name;
    this.type = type;
    this.extAttrs = extAttrs;
  }

  public final boolean isError ()
  {
    return type != null && type.spec.isError();
  }
}

public static class Member extends Param
{
  private long m_offset;
  /** -1 means not a bit-field */
  private final int m_bitFieldWidth;
  /** bit offset within the base-type sized unit, if a bit-field */
  private int m_bitOffset;

  /**
   *
   * @param rng
   * @param name
   * @param type
   * @param bitFieldWidth -1 means not a bit-field
   */
  public Member ( final ISourceRange rng, int index, final Ident name, final Qual type, final int bitFieldWidth )
  {
    super(rng, index, name, type, null);
    m_bitFieldWidth = bitFieldWidth;
  }

  public final long getOffset ()
  {
    return m_offset;
  }
  public final void setOffset ( long offset )
  {
    m_offset = offset;
  }

  public final boolean isBitField ()
  {
    return m_bitFieldWidth >= 0;
  }

  public final int getBitFieldWidth ()
  {
    assert isBitField();
    return m_bitFieldWidth;
  }

  public final int getBitOffset ()
  {
    assert isBitField();
    return m_bitOffset;
  }

  public final void setBitOffset ( int bitOffset )
  {
    assert isBitField();
    assert bitOffset >= 0 && bitOffset+m_bitFieldWidth <= type.spec.kind.width;
    m_bitOffset = bitOffset;
  }
}

} // class
