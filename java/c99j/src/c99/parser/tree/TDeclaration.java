package c99.parser.tree;

import c99.ISourceRange;
import c99.SourceRange;
import c99.Types;
import c99.parser.*;

/**
 * We need to accumulate parameter declarations because of reduce/reduce conflicts
 * in the grammar otherwise
 */
public final class TDeclaration extends SourceRange
{
  public final TSpecNode dsNode;
  public final TDeclarator declarator;
  public TDeclSpec ds;

  public Types.SClass sclass;
  public Linkage linkage;
  public Types.Qual type;
  public boolean defined;
  public boolean error;

  public TDeclaration ( ISourceRange rng, TSpecNode dsNode, TDeclarator declarator )
  {
    super(rng);
    this.dsNode = dsNode;
    this.declarator = declarator;
  }

  public TDeclaration ( CParser.Location loc, TSpecNode dsNode, TDeclarator declarator )
  {
    this((ISourceRange)null, dsNode, declarator );
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
