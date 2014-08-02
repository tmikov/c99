package c99.parser;

public class TreeN extends Tree
{
private final Tree ch[];

public TreeN ( final String name, final Tree... ch )
{
  super( name );
  this.ch = ch;
}

@Override
public int childCount ()
{
  return ch.length;
}

@Override
public Tree child ( final int n )
{
  return ch[n];
}

@Override
public void setChild ( final int n, final Tree v )
{
  this.ch[n] = v;
}
} // class
