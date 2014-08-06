package c99.parser.ast;

public class Ast3 extends Ast
{
private Ast a, b, c;

public Ast3 ( final String name, final Ast a, final Ast b, final Ast c )
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
public Ast child ( final int n )
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
public void setChild ( final int n, final Ast ch )
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
