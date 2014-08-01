package c99.parser;

public class Tree2 extends Tree
{
public final Tree a, b;

public Tree2 ( final String name, final Tree a, final Tree b )
{
  super( name );
  this.a = a;
  this.b = b;
}

@Override
public int childCount ()
{
  return 2;
}

@Override
public Tree child ( final int n )
{
  switch (n)
  {
    case 0: return a;
    case 1: return b;
  }
  assert false;
  return null;
}
} // class
