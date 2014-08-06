package c99.parser.ast;

public class AstN extends Ast
{
private final Ast ch[];

public AstN ( final String name, final Ast... ch )
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
public Ast child ( final int n )
{
  return ch[n];
}

@Override
public void setChild ( final int n, final Ast v )
{
  this.ch[n] = v;
}
} // class
