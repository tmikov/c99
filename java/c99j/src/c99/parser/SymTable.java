package c99.parser;

public class SymTable extends IdentTable<Symbol>
{
@Override
protected Symbol newIdent ( byte[] bytes, int hash )
{
  return new Symbol( bytes, hash );
}

} // class

