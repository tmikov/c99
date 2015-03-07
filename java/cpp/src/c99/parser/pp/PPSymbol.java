package c99.parser.pp;

import c99.Ident;

public class PPSymbol extends Ident
{
public PPSymCode ppCode;
public Object ppDecl;

public PPSymbol ( byte[] bytes, int hash )
{
  super( bytes, hash );
}
}
