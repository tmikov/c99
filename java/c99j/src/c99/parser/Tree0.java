package c99.parser;

public class Tree0 extends Tree
{
protected Tree0 ( final String name )
{
  super( name );
}

@Override
public int childCount ()
{
  return 0;
}

@Override
public Tree child ( final int n )
{
  assert false;
  return null;
}
} // class
