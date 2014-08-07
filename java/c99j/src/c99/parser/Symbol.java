package c99.parser;

import c99.Ident;
import c99.parser.pp.PPSymCode;

public class Symbol extends Ident
{
public PPSymCode ppCode;
public Object ppDecl;
public Code   keyword;

public Decl topDecl;
public Decl topTag;
public Object label;

public Symbol ( byte bytes[], int hash )
{
  super(bytes, hash);
}
} // class

