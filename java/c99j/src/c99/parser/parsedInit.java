package c99.parser;

import c99.ISourceRange;
import c99.SourceRange;
import c99.parser.tree.TExpr;

/**
 * A namespace holding the unvalidated representation of initializers as recursive
 * expression lists with optional designators.
 */
final class parsedInit
{
private parsedInit () {}

static abstract class Designator extends SourceRange
{
  private Designator m_next;
  private boolean m_error;

  public Designator ( ISourceRange rng, boolean error ) { super( rng );
    m_error = error;
  }

  public final boolean isError () { return m_error; }

  /**
   * Construct a new list from a new head element and a tail list. The chain of designators is likely
   * to be very short (1) in most cases, so instead of a separate list object, we simply chain them together.
   *
   * @param tailList
   * @return
   */
  public final Designator cons ( Designator tailList )
  {
    assert m_next == null;
    m_next = tailList;
    if (tailList != null)
      m_error |= tailList.isError();
    return this;
  }

  public final Designator getNext ()
  {
    return m_next;
  }

  public final boolean isFieldDesignator () { return this instanceof FieldDesignator; }
  public final FieldDesignator asFieldDesignator () { return (FieldDesignator)this; }
  public final boolean isIndexDesignator () { return this instanceof IndexDesignator; }
  public final IndexDesignator asIndexDesignator () { return (IndexDesignator)this; }
  public final boolean isRangeDesignator () { return this instanceof RangeDesignator; }
  public final RangeDesignator asRangeDesignator () { return (RangeDesignator)this; }
}

static final class FieldDesignator extends Designator
{
  public final Symbol ident;

  public FieldDesignator ( ISourceRange rng, boolean error, Symbol ident )
  {
    super( rng, error );
    this.ident = ident;
  }

  public String toString ()
  {
    return "." + ident.name + (getNext() != null ? getNext().toString() : "");
  }
}

static final class IndexDesignator extends Designator
{
  public final int index;
  public IndexDesignator ( ISourceRange rng, boolean error, int index )
  {
    super( rng, error );
    this.index = index;
  }

  public String toString ()
  {
    return "[" + index + "]" + (getNext() != null ? getNext().toString() : "");
  }
}

static final class RangeDesignator extends Designator
{
  public final int first;
  /** inclusive */
  public final int last;

  public RangeDesignator ( ISourceRange rng, boolean error, int first, int last )
  {
    super( rng, error );
    this.first = first;
    this.last = last;
  }

  public String toString ()
  {
    return "[" + first + "..." + last + "]" + (getNext() != null ? getNext().toString() : "");
  }
}

static abstract class Initializer extends SourceRange
{
  Initializer m_next;
  private boolean m_error;
  private Designator m_designation;

  public Initializer ( ISourceRange rng )
  {
    super( rng );
  }

  public final boolean isList ()
  {
    return this instanceof InitializerList;
  }
  public final InitializerList asList ()
  {
    return (InitializerList)this;
  }
  public final boolean isExpr ()
  {
    return this instanceof InitializerExpr;
  }
  public final InitializerExpr asExpr ()
  {
    return (InitializerExpr)this;
  }

  /**
   * Indicates that this initializer, or any of its children recursively, has an error
   */
  public final boolean isError ()
  {
    return m_error;
  }

  public final void orError ( boolean error )
  {
    m_error |= error;
  }

  public final Designator getDesignation ()
  {
    return m_designation;
  }

  public final void setDesignation ( Designator designation )
  {
    assert m_designation == null;
    m_designation = designation;
  }

  public final Initializer getNext ()
  {
    return m_next;
  }
}

static final class InitializerExpr extends Initializer
{
  private final TExpr.Expr m_expr;

  public InitializerExpr ( ISourceRange rng, TExpr.Expr expr )
  {
    super( rng );
    m_expr = expr;
    orError( expr.isError() );
  }

  public final TExpr.Expr getExpr ()
  {
    return m_expr;
  }

  public final String toString ()
  {
    return (getDesignation() != null ? getDesignation().toString() + "=" : "") + m_expr.getCode().toString();
  }
}

static final class InitializerList extends Initializer
{
  private Initializer m_first, m_last;
  private int m_length;

  public InitializerList ( ISourceRange rng )
  {
    super( rng );
    m_length = 0;
  }

  public final void add ( Initializer elem )
  {
    if (m_last != null)
      m_last.m_next = elem;
    else
      m_first = elem;
    m_last = elem;
    ++m_length;
    orError( elem.isError() );
  }

  public final Initializer getFirst ()
  {
    return m_first;
  }

  public final int getLength ()
  {
    return m_length;
  }

  @Override
  public String toString ()
  {
    StringBuilder buf = new StringBuilder();

    if (getDesignation() != null)
      buf.append( getDesignation().toString() ).append('=');

    buf.append( '{' );
    for ( Initializer cur = m_first; cur != null; cur = cur.getNext() )
    {
      if (cur != m_first)
        buf.append( ", " );
      buf.append( cur.toString() );
    }
    buf.append( '}' );
    return buf.toString();
  }
}

}
