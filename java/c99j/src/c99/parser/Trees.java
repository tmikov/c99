package c99.parser;

import c99.Constant;
import c99.ExtAttr;
import c99.ISourceRange;
import c99.SourceRange;

import java.util.ArrayList;

@Deprecated
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

public static class TIntNumber extends Tree
{
  public final Constant.IntC value;

  public TIntNumber ( final ISourceRange rng, final Constant.IntC value )
  {
    super(rng);
    this.value = value;
  }
}

}
