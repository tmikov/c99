package c99.parser;

public class Tree1 extends Tree
{
private Tree a;

public Tree1 ( final String name, final Tree a )
{
  super( name );
  this.a = a;
}

@Override
public int childCount ()
{
  return 1;
}

@Override
public Tree child ( final int n )
{
  assert n == 0;
  return a;
}

@Override
public void setChild ( final int n, final Tree ch )
{
  assert n == 0;
  this.a = ch;
}
} // class
