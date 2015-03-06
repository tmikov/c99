package c99.parser.tree;

import c99.ISourceRange;
import c99.Types;
import c99.parser.Code;

public class TSpecTagNode extends TSpecNode
{
public final Types.TagSpec spec;

public TSpecTagNode ( ISourceRange rgn, Code code, Types.TagSpec spec )
{
  super( rgn, code );
  this.spec = spec;
}
}
