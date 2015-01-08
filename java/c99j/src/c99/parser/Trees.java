package c99.parser;

import c99.Constant;
import c99.ExtAttr;
import c99.ISourceRange;
import c99.SourceRange;

import java.util.ArrayList;
import java.util.Iterator;

public abstract class Trees
{
public static class Tree extends SourceRange
{
  public Tree ( ISourceRange rng )
  {
    super( rng );
  }
} // class

public static class TreeList extends ArrayList<Tree>
{
}

public static class TExtAttr extends Tree
{
  public final ExtAttr extAttr;

  public TExtAttr ( final ISourceRange rng, final ExtAttr extAttr )
  {
    super(rng);
    this.extAttr = extAttr;
  }
}

public static class TExtAttrList extends ArrayList<TExtAttr>
{
}

public static class TDecl extends Tree
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

public static class TExpr extends Tree
{
  public TExpr ( ISourceRange rng )
  {
    super( rng );
  }
} // class

public static class TIntNumber extends Tree
{
  public final Constant.IntC value;

  public TIntNumber ( final ISourceRange rng, final Constant.IntC value )
  {
    super(rng);
    this.value = value;
  }
}

public static class TStringLiteral extends Tree
{
  public final byte[] value;
  public TStringLiteral ( final ISourceRange rng, byte[] value )
  {
    super(rng);
    this.value = value;
  }
}
}
