package c99.parser;

import c99.Constant;
import c99.TypeSpec;

public final class EnumScope extends Scope
{
public TypeSpec baseSpec;
public Constant.IntC lastValue;

public EnumScope ( Scope parent )
{
  super( Kind.ENUM, parent );
  this.baseSpec = TypeSpec.SINT;
}
}
