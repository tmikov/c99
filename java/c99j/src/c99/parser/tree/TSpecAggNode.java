package c99.parser.tree;

import c99.ISourceRange;
import c99.Types;
import c99.parser.Code;

/**
 * Created by tmikov on 1/8/15.
 */
public class TSpecAggNode extends TSpecNode
{
public final Types.StructUnionSpec spec;

public TSpecAggNode ( ISourceRange rgn, Code code, Types.StructUnionSpec spec )
{
  super( rgn, code );
  this.spec = spec;
}
}
