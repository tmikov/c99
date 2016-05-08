package c99.parser.tree;

import c99.ISourceRange;
import c99.SourceRange;
import c99.Types;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * A namespace for structured initializer values.
 */
public final class TInit
{
private TInit () {}

public static class Value extends SourceRange
{
  private static final int IS_ERROR = 1;
  private static final int IS_COMPOUND = 2;

  private Types.Qual m_qual;
  private int m_flags;
  private @Nullable Object m_data;

  public Value ( @Nullable ISourceRange loc, @NotNull Types.Qual qual )
  {
    super(loc);
    this.m_qual = qual;
  }

/*  public Value ( Types.Qual qual )
  {
    this.m_qual = qual;
  }*/

  public final Types.Qual getQual ()
  {
    return m_qual;
  }

  /** Used only for completing array types */
  public void updateQual ( Types.Qual qual )
  {
    m_qual = qual;
  }

  public boolean isCompound () { return (m_flags & IS_COMPOUND) != 0; }
  public boolean isSingle () { return (m_flags & IS_COMPOUND) == 0; }

  public boolean isEmpty () { return m_data == null; }

  public boolean isError ()
  {
    return (m_flags & IS_ERROR) != 0;
  }

  public void markError ()
  {
    m_flags |= IS_ERROR;
  }

  public ISourceRange getLoc ()
  {
    return (ISourceRange)m_data;
  }

  public TExpr.Expr getExpr ()
  {
    assert isSingle();
    return (TExpr.Expr)m_data;
  }

  public void setExpr ( TExpr.Expr expr )
  {
    m_flags &= ~IS_COMPOUND;
    m_data = expr;
    if (expr != null && expr.isError())
      markError();
  }

  public Compound getCompound ()
  {
    assert isCompound();
    return (Compound)m_data;
  }

  public Compound makeCompound ()
  {
    assert !isCompound();

    // NOTE: If it is an aggregate, we optimize the capacity by pre-allocating the known number of sub-objects
    final Compound c = new Compound(this, getSequentialInitLength());
    m_data = c;
    m_flags |= IS_COMPOUND;
    return c;
  }

  /**
   * Return the number of elements to be initialized sequentially, which for unions is 1 (only the first
   * union element is initialized). For arrays we never allow more than 2**31 elements to be initialized.
   */
  public int getSequentialInitLength ()
  {
    final Types.Spec spec = m_qual.spec;
    switch (spec.kind)
    {
    case STRUCT: return ((Types.StructUnionSpec)spec).getFields().length;
    case UNION:  return Math.min(1, ((Types.StructUnionSpec)spec).getFields().length);
    case ARRAY:
      Types.ArraySpec arraySpec = (Types.ArraySpec)spec;
      return arraySpec.hasNelem() ? (int)Math.min(Integer.MAX_VALUE, arraySpec.getNelem()) : Integer.MAX_VALUE;
    default:     return 0;
    }
  }
}

public static final class Compound
{
  private final Value m_parent;
  private @NotNull Value[] m_subObjects;
  private int m_length;

  private Compound ( Value parent, int capacity )
  {
    m_parent = parent;
    m_subObjects = new Value[Math.max(4, capacity == Integer.MAX_VALUE ? 0 : capacity)];
  }

  public int getLength ()
  {
    return m_length;
  }

  /**
   * Get an element. Elements outside of the array range are returned as {@code null}
   */
  public Value getSubObject ( int index )
  {
    return index < m_length ? m_subObjects[index] : null;
  }

  /**
   * Set an element, resizing the array if necessary
   */
  public void setSubObject ( int index, Value value )
  {
    if (value.isError())
      m_parent.markError();

    if (index < m_length)
      ;
    else if (index < m_subObjects.length)
    {
      m_length = index+1;
    }
    else
    {
      // Need to resize
      int newLen = Math.max(m_subObjects.length << 1, index+1);
      m_subObjects = Arrays.copyOf(m_subObjects, newLen);
      m_length = index + 1;
    }
    m_subObjects[index] = value;
  }
}

}
