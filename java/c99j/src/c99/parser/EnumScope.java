package c99.parser;

import c99.CompilerOptions;
import c99.Constant;
import c99.TypeSpec;

public final class EnumScope extends Scope
{
public TypeSpec baseSpec;
public Constant.IntC lastValue;

public EnumScope ( CompilerOptions opts, Scope parent )
{
  super( opts, Kind.ENUM, parent );
  this.baseSpec = TypeSpec.SINT;
}
}
