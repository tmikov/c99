package c99.parser;

import c99.Constant;
import c99.ISourceRange;
import c99.Types;

public class EnumConstDecl extends Decl
{
public final Constant.IntC value;

public EnumConstDecl (
  ISourceRange rng, Scope scope, Symbol symbol, Types.Qual type, Constant.IntC value, boolean error
)
{
  super( rng, Kind.ENUM_CONST, scope, Types.SClass.NONE, Linkage.NONE, symbol, type, true, error );
  this.value = value;
}
} // class

