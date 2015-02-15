package c99.parser;

import c99.Constant;
import c99.parser.tree.TIdent;
import c99.parser.tree.TStringLiteral;


public class TreeActions extends DeclActions
{

public TStringLiteral stringLiteral ( CParser.Location loc, byte[] value )
{
  return BisonLexer.setLocation(new TStringLiteral(null,value), loc);
}

public TStringLiteral stringLiteral ( CParser.Location loc, TStringLiteral lit, byte[] value )
{
  // Combine the two strings
  final byte[] comb = new byte[lit.value.length + value.length];
  System.arraycopy( lit.value, 0, comb, 0, lit.value.length );
  System.arraycopy( value, 0, comb, lit.value.length, value.length );

  return BisonLexer.setLocation(new TStringLiteral(null,value), loc);
}

public TIdent symbolTree ( CParser.Location loc, Symbol sym )
{
  return BisonLexer.setLocation( new TIdent(null,sym), loc );
}

public Trees.TIntNumber intNumber ( CParser.Location loc, Constant.IntC value )
{
  return BisonLexer.setLocation( new Trees.TIntNumber(null,value), loc );
}

public Trees.TreeList treeList ( Trees.TreeList list, Trees.Tree elem )
{
  if (list == null)
    list = new Trees.TreeList();
  list.add( elem );
  return list;
}

}
