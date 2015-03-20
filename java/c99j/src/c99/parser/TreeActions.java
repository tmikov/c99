package c99.parser;

import c99.*;
import c99.parser.tree.TIdent;
import c99.parser.tree.TStringLiteral;


public class TreeActions extends AstActions
{

public TStringLiteral stringLiteral ( ISourceRange loc, TStringLiteral lit, AnyStringConst value )
{
  final AnyStringConst res;

  if (lit == null)
    res = value;
  else if (lit.value instanceof CharStringConst && value instanceof CharStringConst)
  {
    CharStringConst l = (CharStringConst)lit.value;
    CharStringConst r = (CharStringConst)value;
    final byte[] comb = new byte[l.value.length + r.value.length];
    System.arraycopy( l.value, 0, comb, 0, l.value.length );
    System.arraycopy( r.value, 0, comb, l.value.length, r.value.length );
    res = new CharStringConst( l.spec, comb );
  }
  else
  {
    int[] l = lit.value.wideValue();
    int[] r = value.wideValue();
    final int[] comb = new int[l.length + r.length];
    System.arraycopy( l, 0, comb, 0, l.length );
    System.arraycopy( r, 0, comb, l.length, r.length );
    res = new WideStringConst(
        TypeSpec.values()[Math.max(lit.value.spec.ordinal(), value.spec.ordinal())], // Select the larger type
        comb
    );
  }

  // Combine the two strings
  return new TStringLiteral( loc, res );
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
