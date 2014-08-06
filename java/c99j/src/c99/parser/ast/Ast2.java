package c99.parser.ast;

public class Ast2 extends Ast
{
private Ast a, b;

public Ast2 ( final String name, final Ast a, final Ast b )
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
public Ast child ( final int n )
{
  switch (n)
  {
    case 0: return a;
    case 1: return b;
  }
  assert false;
  return null;
}

@Override
public void setChild ( final int n, final Ast ch )
{
  switch (n)
  {
    case 0: this.a = ch; break;
    case 1: this.b = ch; break;
    default: assert false;
  }
}
} // class
