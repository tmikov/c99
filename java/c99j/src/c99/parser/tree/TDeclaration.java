package c99.parser.tree;

import c99.ISourceRange;
import c99.SourceRange;
import c99.Types;
import c99.parser.BisonLexer;
import c99.parser.CParser;
import c99.parser.Symbol;

/**
 * We need to accumulate parameter declarations because of reduce/reduce conflicts
 * in the grammar otherwise
 */
public final class TDeclaration extends SourceRange
{
  public final TDeclSpec ds;
  public final TDeclarator declarator;
  public final Types.Qual type;


  private TDeclaration ( ISourceRange rng, TDeclSpec ds, TDeclarator declarator, Types.Qual type )
  {
    super(rng);
    this.ds = ds;
    this.declarator = declarator;
    this.type = type;
  }

  public TDeclaration ( ISourceRange rng, TDeclSpec ds, TDeclarator declarator )
  {
    this( rng, ds, declarator, declarator.attachDeclSpecs( ds.qual ));
  }

  public TDeclaration ( CParser.Location loc, TDeclSpec ds, TDeclarator declarator )
  {
    this((ISourceRange)null, ds, declarator );
    BisonLexer.setLocation( this, loc );
  }

  public final boolean hasIdent ()
  {
    return this.declarator.ident != null;
  }

  public final Symbol getIdent ()
  {
    return this.declarator.ident;
  }
}
