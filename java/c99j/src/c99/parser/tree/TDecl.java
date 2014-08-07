package c99.parser.tree;

import c99.ISourceRange;
import c99.parser.Decl;

public class TDecl extends Tree
{
public final Decl decl;
public final TExpr init;

public TDecl ( ISourceRange rng, Decl decl, TExpr init )
{
  super( rng );
  this.decl = decl;
  this.init = init;
}
} // class

