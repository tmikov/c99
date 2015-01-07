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
  public final Symbol ident;
  public final Types.Qual type;
  public final TDeclSpec ds;

  public TDeclaration ( ISourceRange rng, Symbol ident, Types.Qual type, TDeclSpec ds )
  {
    super(rng);
    this.ident = ident;
    this.type = type;
    this.ds = ds;
  }

  public TDeclaration ( CParser.Location loc, Symbol ident, Types.Qual type, TDeclSpec ds )
  {
    this((ISourceRange)null, ident, type, ds );
    BisonLexer.setLocation( this, loc );
  }
}
