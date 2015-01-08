package c99.parser.tree;

import c99.ISourceRange;
import c99.parser.Code;
import c99.parser.Scope;
import c99.parser.Symbol;
import c99.parser.Trees;

/**
 * Created by tmikov on 1/8/15.
 */
public class TSpecAggNode extends TSpecNode
{
public final TIdent identTree;
public final Scope memberScope;

public TSpecAggNode ( ISourceRange rgn, Code code, TIdent identTree, Scope memberScope )
{
  super( rgn, code );
  this.identTree = identTree;
  this.memberScope = memberScope;
}
}
