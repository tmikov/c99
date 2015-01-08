package c99.parser.tree;

import c99.ExtAttributes;
import c99.Types;

/**
 * Created by tmikov on 1/5/15.
 */
public final class TDeclSpec
{
  public Types.SClass sc;
  public final ExtAttributes scAttr;
  public final Types.Qual qual;
  public TSpecNode scNode;
  public TSpecNode thread;
  public TSpecNode inline;
  public TSpecNode noreturn;
  public boolean error;

  public TDeclSpec ( Types.SClass sc, ExtAttributes scAttr, final Types.Qual qual )
  {
    this.sc = sc;
    this.scAttr = scAttr;
    this.qual = qual;
  }
}
