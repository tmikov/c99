package c99.parser;

public class Tree3 extends Tree
{
private Tree a, b, c;

public Tree3 ( final String name, final Tree a, final Tree b, final Tree c )
{
  super( name );
  this.a = a;
  this.b = b;
  this.c = c;
}

@Override
public int childCount ()
{
  return 3;
}

@Override
public Tree child ( final int n )
{
  switch (n)
  {
    case 0: return a;
    case 1: return b;
    case 2: return c;
  }
  assert false;
  return null;
}

@Override
public void setChild ( final int n, final Tree ch )
{
  switch (n)
  {
    case 0: this.a = ch; break;
    case 1: this.b = ch; break;
    case 2: this.c = ch; break;
    default: assert false;
  }
}
} // class
