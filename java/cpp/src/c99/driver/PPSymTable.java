package c99.driver;

import c99.parser.IdentTable;
import c99.parser.pp.PPSymbol;

public class PPSymTable extends IdentTable<PPSymbol>
{
@Override
protected PPSymbol newIdent ( byte[] bytes, int hash )
{
  return new PPSymbol( bytes, hash );
}
}
