package c99.parser;

import c99.Constant;
import c99.Utils;
import c99.parser.ast.*;

public class AstActions extends BaseActions
{

public final Ast ast ( Code code )
{
  return new Ast0( code.name() ).value(code);
}

public final Ast ast ( Code code, CParser.Location loc )
{
  return new Ast0( code.name() ).value(code).location(loc);
}

public final Ast ast ( Code code, Ast a )
{
  return new Ast1( code.name(), a ).value(code);
}

public final Ast ast ( Code code, Ast a, Ast b )
{
  return new Ast2( code.name(), a, b ).value(code);
}

public final Ast ast ( String name )
{
  return new Ast0( name );
}

public final Ast ast ( String name, Ast a )
{
  return new Ast1( name, a );
}

public final Ast ast ( String name, Ast a, Ast b )
{
  return new Ast2( name, a, b );
}

public final Ast ast ( String name, Ast a, Ast b, Ast c )
{
  return new Ast3( name, a, b, c );
}

public final Ast ast ( String name, Ast a, Ast b, Ast c, Ast d )
{
  return new Ast4( name, a, b, c, d );
}

public final Ast stringLiteral ( byte[] value )
{
  return new Ast1( "<string-literal>", new Ast0( Utils.asciiString( value ) ).value(value) );
}

public final Ast astAppend ( Ast t, Ast newChild )
{
  final int chCount = t.childCount();
  switch (chCount)
  {
    case 0: return new Ast1( t.name, newChild );
    case 1: return new Ast2( t.name, t.child( 0 ), newChild );
    case 2: return new Ast3( t.name, t.child( 0 ), t.child( 1 ), newChild );
    case 3: return new Ast4( t.name, t.child( 0 ), t.child( 1 ), t.child( 2 ), newChild );
    default:
      Ast[] children = new Ast[chCount + 1];
      for ( int i = 0; i < chCount; ++i )
        children[i] = t.child( i );
      children[chCount] = newChild;
      return new AstN( t.name, children );
  }
}

public final Ast leftAppend ( Ast newChild, Ast t )
{
  final int chCount = t.childCount();
  switch (chCount)
  {
    case 0: return new Ast1( t.name, newChild ).value(t.value);
    case 1: return new Ast2( t.name, newChild, t.child( 0 ) ).value(t.value);
    case 2: return new Ast3( t.name, newChild, t.child( 0 ), t.child( 1 ) ).value(t.value);
    case 3: return new Ast4( t.name, newChild, t.child( 0 ), t.child( 1 ), t.child( 2 ) ).value(t.value);
    default:
      Ast[] children = new Ast[chCount + 1];
      for ( int i = 0; i < chCount; ++i )
        children[i+1] = t.child( i );
      children[0] = newChild;
      return new AstN( t.name, children ).value(t.value);
  }
}

public final Ast stringLiteral ( Ast lit, byte[] value )
{
  return astAppend( lit, stringLiteral( value ) ).value(value);
}

public final Ast ident ( Symbol sym, CParser.Location loc )
{
  return new Ast0( "ident:" + sym.name ).value(sym).location(loc);
}

public final Ast constant ( Constant.ArithC v, CParser.Location loc )
{
  return new Ast0( "const:"+ v.toString() ).value(v).location( loc );
}

public final Ast seqAppend ( Ast seq, Ast newNode )
{
  if (seq == null)
    return newNode;
  if (newNode == null)
    return seq;

  Ast cur = seq, ch;
  int ind;
  while ( (ch = cur.child(ind = cur.childCount()-1)) != null)
    cur = ch;
  cur.setChild( ind, newNode );
  return seq;
}

public void print ( Ast t )
{
  if (t != null)
    t.print( 0, System.out, 100 );
}

} // class
