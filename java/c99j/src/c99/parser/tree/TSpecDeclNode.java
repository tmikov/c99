package c99.parser.tree;

import c99.ISourceRange;
import c99.parser.Code;
import c99.parser.Decl;

/**
 * Created by tmikov on 1/5/15.
 */
public final class TSpecDeclNode extends TSpecNode
{
  public final Decl decl;

  public TSpecDeclNode ( ISourceRange rng, Code code, Decl decl )
  {
    super( rng, code );
    this.decl = decl;
  }
}
