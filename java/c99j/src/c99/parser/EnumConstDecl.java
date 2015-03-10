package c99.parser;

import c99.Constant;
import c99.ISourceRange;
import c99.Types;

public class EnumConstDecl extends Decl
{
public final Constant.IntC enumValue;

public EnumConstDecl (
  ISourceRange rng, Scope visibilityScope, Symbol symbol, Types.Qual type, boolean error, Constant.IntC enumValue
)
{
  super( rng, Kind.ENUM_CONST, visibilityScope, visibilityScope, Types.SClass.NONE, Linkage.NONE, symbol, type, true, error );
  this.enumValue = enumValue;
}

@Override
public String toString ()
{
  return "EnumConstDecl{" +
          "enumValue=" + enumValue + " " +
          super.toString() +
          '}';
}
}
