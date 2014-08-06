package c99.parser.ast;

public class Ast1 extends Ast
{
private Ast a;

public Ast1 ( final String name, final Ast a )
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
public Ast child ( final int n )
{
  assert n == 0;
  return a;
}

@Override
public void setChild ( final int n, final Ast ch )
{
  assert n == 0;
  this.a = ch;
}
} // class
