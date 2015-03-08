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

  public final Qual copy ()
  {
    Qual q = new Qual(spec);
    q.combine( this ); // save us some typing
    return q;
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

  @Override public final String toString ()
  {
    StringBuilder buf = new StringBuilder();
    if (isConst) buf.append( "const " );
    if (isVolatile) buf.append( "volatile " );
    if (isRestrict) buf.append( "restrict " );
    if (isAtomic) buf.append(  "atomic " );
    if (spec != null)
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

  public final TypeSpec effectiveKind ()
  {
    return this.kind == TypeSpec.ENUM ? ((EnumSpec)this).getBaseSpec().effectiveKind() : this.kind;
  }

  public abstract boolean visit ( Qual q, TypeVisitor v );

  public abstract boolean isError ();

  public final boolean isComplete ()
  {
    return m_complete;
  }

  public final void setAlign ( int align )
  {
    m_align = align;
  }

  public final void setSizeAlign ( long size, int align )
  {
    m_size = size;
    m_align = align;
  }

  public final long sizeOf ()
  {
    assert isComplete();
    return m_size;
  }

  public final int alignOf ()
  {
    assert m_align > 0;
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

  public ArraySpec ( Qual of )
  {
    super( TypeSpec.ARRAY, false, of );
    m_nelem = -1;
    setAlign( of.spec.alignOf() );
  }

  public ArraySpec ( Qual of, long nelem, long size )
  {
    super( TypeSpec.ARRAY, true, of );
    assert of.spec.isComplete();
    m_nelem = nelem;
    setSizeAlign( size, of.spec.alignOf() );
  }

  @Override
  public boolean visit ( Qual q, TypeVisitor v )
  {
    return v.visitArray( q, this );
  }

  @Override
  public boolean isError ()
  {
    return false;
  }

  public final boolean hasNelem ()
  {
    return m_nelem >= 0;
  }

  public final long getNelem ()
  {
    assert m_nelem >= 0;
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

  public void setFields ( Member[] fields )
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
  public boolean oldStyle;
  public Param[] params;

  public FunctionSpec ( boolean oldStyle, Qual returning )
  {
    super( TypeSpec.FUNCTION, false, returning );
    this.oldStyle = oldStyle;
  }

  @Override
  public boolean isError ()
  {
    return false;
  }

  @Override
  public boolean visit ( Qual q, TypeVisitor v )
  {
    return v.visitFunction( q, this );
  }

  @Override public boolean compatible ( Spec o )
  {
    if (o == this)
      return true;
    if (!super.compatible( o ))
      return false;
    // FIXME: 6.7.6.3[15]
    FunctionSpec x = (FunctionSpec)o;
    if (this.oldStyle != x.oldStyle) return false;
    if (this.params == null) return x.params == null;
    if (this.params.length != x.params.length) return false;

    for ( int e = this.params.length, i = 0; i < e; ++i )
    {
      final Param pa = this.params[i];
      final Param pb = x.params[i];
      if (!pa.type.compatible( pb.type ) || !pa.extAttrs.same(pb.extAttrs))
        return false;
    }

    return true;
  }

  @Override public String toString ()
  {
    StringBuilder buf = new StringBuilder();
    buf.append( oldStyle ? "oldfunc(" : "func(" );
    if (params != null)
      for ( int i = 0; i < params.length; ++i )
      {
        final Param param = params[i];
        if (i > 0)
          buf.append( ", " );
        if (param.name != null)
          buf.append( param.name.name );
        buf.append( ':' );
        if (param.type != null)
          buf.append( param.type.toString() );
      }
    buf.append( ") returning " );
    buf.append( of.toString() );
    return buf.toString();
  }
}

public static class Param extends SourceRange
{
  public final Ident name;
  public final Qual type;
  public final ExtAttributes extAttrs = new ExtAttributes();

  public Param ( ISourceRange rng, final Ident name, final Qual type  )
  {
    super( rng );
    this.name = name;
    this.type = type;
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
  public Member ( final ISourceRange rng, final Ident name, final Qual type, final int bitFieldWidth )
  {
    super(rng, name, type);
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
