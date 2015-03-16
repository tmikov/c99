package c99.parser;

import c99.Constant;
import c99.ISourceRange;
import c99.parser.tree.TIdent;
import c99.parser.tree.TStringLiteral;


public class TreeActions extends AstActions
{

public TStringLiteral stringLiteral ( ISourceRange loc, byte[] value )
{
  return new TStringLiteral( loc, value );
}

public TStringLiteral stringLiteral ( ISourceRange loc, TStringLiteral lit, byte[] value )
{
  // Combine the two strings
  final byte[] comb = new byte[lit.value.length + value.length];
  System.arraycopy( lit.value, 0, comb, 0, lit.value.length );
  System.arraycopy( value, 0, comb, lit.value.length, value.length );

  return new TStringLiteral( loc, value );
}

public TIdent symbolTree ( ISourceRange loc, Symbol sym )
{
  return new TIdent( loc, sym );
}

public Trees.TIntNumber intNumber ( ISourceRange loc, Constant.IntC value )
{
  return new Trees.TIntNumber( loc, value );
}

public Trees.TreeList treeList ( Trees.TreeList list, Trees.Tree elem )
{
  if (list == null)
    list = new Trees.TreeList();
  list.add( elem );
  return list;
}

}
