package c99.parser;

import c99.Constant;
import c99.Utils;

public class ParserActions
{

public final Tree tree ( Code code )
{
  return new Tree0( code.name() );
}

public final Tree tree ( Code code, Tree a )
{
  return new Tree1( code.name(), a );
}

public final Tree tree ( Code code, Tree a, Tree b )
{
  return new Tree2( code.name(), a, b );
}

public final Tree tree ( String name )
{
  return new Tree0( name );
}

public final Tree tree ( String name, Tree a )
{
  return new Tree1( name, a );
}

public final Tree tree ( String name, Tree a, Tree b )
{
  return new Tree2( name, a, b );
}

public final Tree tree ( String name, Tree a, Tree b, Tree c )
{
  return new Tree3( name, a, b, c );
}

public final Tree tree ( String name, Tree a, Tree b, Tree c, Tree d )
{
  return new Tree4( name, a, b, c, d );
}

public final Tree stringLiteral ( byte[] value )
{
  return new Tree1( "<string-literal>", new Tree0( Utils.asciiString( value ) ) );
}

public final Tree treeAppend ( Tree t, Tree newChild )
{
  final int chCount = t.childCount();
  switch (chCount)
  {
    case 0: return new Tree1( t.name, newChild );
    case 1: return new Tree2( t.name, t.child( 0 ), newChild );
    case 2: return new Tree3( t.name, t.child( 0 ), t.child( 1 ), newChild );
    case 3: return new Tree4( t.name, t.child( 0 ), t.child( 1 ), t.child( 2 ), newChild );
    default:
      Tree[] children = new Tree[chCount + 1];
      for ( int i = 0; i < chCount; ++i )
        children[i] = t.child( i );
      children[chCount] = newChild;
      return new TreeN( t.name, children );
  }
}

public final Tree leftAppend ( Tree newChild, Tree t )
{
  final int chCount = t.childCount();
  switch (chCount)
  {
    case 0: return new Tree1( t.name, newChild );
    case 1: return new Tree2( t.name, newChild, t.child( 0 ) );
    case 2: return new Tree3( t.name, newChild, t.child( 0 ), t.child( 1 ) );
    case 3: return new Tree4( t.name, newChild, t.child( 0 ), t.child( 1 ), t.child( 2 ) );
    default:
      Tree[] children = new Tree[chCount + 1];
      for ( int i = 0; i < chCount; ++i )
        children[i+1] = t.child( i );
      children[0] = newChild;
      return new TreeN( t.name, children );
  }
}

public final Tree stringLiteral ( Tree lit, byte[] value )
{
  return treeAppend( lit, stringLiteral( value ) );
}

public final Tree ident ( Symbol sym )
{
  return new Tree0( "ident:" + sym.name );
}

public final Tree constant ( Constant.ArithC v )
{
  return new Tree0( "const:"+ v.toString() );
}

public final Tree seqAppend ( Tree seq, Tree newNode )
{
  if (seq == null)
    return newNode;
  if (newNode == null)
    return seq;

  Tree cur = seq, ch;
  int ind;
  while ( (ch = cur.child(ind = cur.childCount()-1)) != null)
    cur = ch;
  cur.setChild( ind, newNode );
  return seq;
}

public final Tree specifyDecl ( Tree decl, Tree specs )
{
  return seqAppend( decl, specs );
}

public void print ( Tree t )
{
  if (t != null)
    t.print( 0, System.out, 100 );
}

} // class
