package c99.parser;

import c99.parser.pp.PPSymbol;

public class Symbol extends PPSymbol
{
public Code   keyword;

public Decl topDecl;
public Decl topTag;
public Object label;

public Symbol ( byte bytes[], int hash )
{
  super(bytes, hash);
}
} // class

