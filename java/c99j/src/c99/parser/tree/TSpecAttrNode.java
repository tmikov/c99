package c99.parser.tree;

import c99.ISourceRange;
import c99.parser.Code;
import c99.parser.Trees;

/**
 * Created by tmikov on 1/5/15.
 */
public final class TSpecAttrNode extends TSpecNode
{
  public final Trees.TExtAttrList attrList;

  public TSpecAttrNode ( final ISourceRange rgn, final Trees.TExtAttrList attrList )
  {
    super(rgn, Code.GCC_ATTRIBUTE);
    this.attrList = attrList;
  }
}
