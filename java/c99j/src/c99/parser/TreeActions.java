package c99.parser;

import c99.Constant;
import c99.parser.tree.TIdent;

import static c99.parser.Trees.*;

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

public TIntNumber intNumber ( CParser.Location loc, Constant.IntC value )
{
  return BisonLexer.setLocation( new TIntNumber(null,value), loc );
}

public TreeList treeList ( TreeList list, Tree elem )
{
  if (list == null)
    list = new TreeList();
  list.add( elem );
  return list;
}

}
