package c99.parser.tree;

/**
 * Created by tmikov on 2/5/15.
 */
public class TInitDeclarator
{
public final TDeclarator declarator;
public final boolean init; // FIXME

public TInitDeclarator ( TDeclarator declarator, boolean init )
{
  this.declarator = declarator;
  this.init = init;
}
}
