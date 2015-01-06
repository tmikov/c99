package c99.parser.tree;

import c99.SourceRange;
import c99.Types;
import c99.parser.BisonLexer;
import c99.parser.CParser;

/**
 * Created by tmikov on 1/5/15.
 */
public final class TDeclElem extends SourceRange
{
  public Types.Qual qual;
  public Types.Spec spec;
  public TDeclElem to;

  private static boolean specIsLast ( Types.Spec spec )
  {
    return !(spec instanceof Types.DerivedSpec) || ((Types.DerivedSpec)spec).of == null;
  }

  public TDeclElem ( CParser.Location loc, final Types.Qual qual )
  {
    this.qual = qual;
    this.spec = qual.spec;
    assert specIsLast( this.spec );

    BisonLexer.setLocation( this, loc );
  }

  public TDeclElem append ( TDeclElem next )
  {
    if (next != null)
    {
      assert this.to == null && specIsLast( this.spec );
      assert this.spec instanceof Types.DerivedSpec;
      this.to = next;
      ((Types.DerivedSpec)this.spec).of = next.qual;
    }
    return this;
  }
}
