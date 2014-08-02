package c99.parser;

public class Tree4 extends Tree
{
private Tree a, b, c, d;

public Tree4 ( final String name, final Tree a, final Tree b, final Tree c, final Tree d )
{
  super( name );
  this.a = a;
  this.b = b;
  this.c = c;
  this.d = d;
}

@Override
public int childCount ()
{
  return 4;
}

@Override
public Tree child ( final int n )
{
  switch (n)
  {
    case 0: return a;
    case 1: return b;
    case 2: return c;
    case 3: return d;
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
    case 3: this.d = ch; break;
    default: assert false;
  }
}
} // class
