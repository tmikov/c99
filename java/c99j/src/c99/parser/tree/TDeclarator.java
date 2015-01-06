package c99.parser.tree;

import c99.SourceRange;
import c99.Types;
import c99.parser.BisonLexer;
import c99.parser.CParser;
import c99.parser.Symbol;

/**
 * Created by tmikov on 1/5/15.
 */
public final class TDeclarator extends SourceRange
{
  public final Symbol ident;
  TDeclElem top;
  TDeclElem bottom;

  public TDeclarator ( CParser.Location loc, final Symbol ident )
  {
    this.ident = ident;
    BisonLexer.setLocation( this, loc );
  }

  public TDeclarator append ( TDeclElem next )
  {
    if (next != null)
    {
      if (bottom != null)
        bottom.append( next );
      else
        top = next;
      bottom = next;
    }
    return this;
  }

  public Types.Qual attachDeclSpecs ( Types.Qual declSpec )
  {
    assert declSpec != null;
    if (bottom != null)
    {
      ((Types.DerivedSpec)bottom.qual.spec).of = declSpec;
      declSpec = top.qual;

      bottom = top = null; // Mark it as invalid
    }
    return declSpec;
  }
}
