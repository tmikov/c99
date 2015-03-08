package c99;

public final class TypeRules
{
private TypeRules () {};

public static TypeSpec integerPromotion ( TypeSpec spec )
{
  // 6.3.1.1 [2]
  // assert spec.integer;
  if (spec.ordinal() > TypeSpec.VOID.ordinal() && spec.ordinal() < TypeSpec.SINT.ordinal())
    return (spec.width - (spec.signed?1:0) <= TypeSpec.SINT.width) ? TypeSpec.SINT : TypeSpec.UINT;
  else
    return spec;
}

public static TypeSpec defaultArgumentPromotion ( TypeSpec spec )
{
  // 6.5.2.2 [6]
  // assert spec.arithmetic;
  if (spec == TypeSpec.FLOAT)
    return TypeSpec.DOUBLE;
  else if (spec.integer)
    return integerPromotion( spec );
  else
    return spec;
}

public static TypeSpec usualArithmeticConversions ( TypeSpec s0, TypeSpec s1 )
{
  // 6.3.1.8

  TypeSpec greaterRank = s0.ordinal() > s1.ordinal() ? s0 : s1;

  if (greaterRank.floating)
    return greaterRank;

  s0 = integerPromotion( s0 );
  s1 = integerPromotion( s1 );

  // If both operands have the same type, then no further conversion is needed.
  if (s0 == s1)
    return s0;

  TypeSpec lesserRank;
  if (s0.ordinal() > s1.ordinal())
  {
    greaterRank = s0;
    lesserRank = s1;
  }
  else
  {
    greaterRank = s1;
    lesserRank = s0;
  }

  // Otherwise, if both operands have signed integer types or both have unsigned
  // integer types, the operand with the type of lesser integer conversion rank is
  // converted to the type of the operand with greater rank.
  //
  // Otherwise, if the operand that has unsigned integer type has rank greater or
  // equal to the rank of the type of the other operand, then the operand with
  // signed integer type is converted to the type of the operand with unsigned
  // integer type.
  //
  // Otherwise, if the type of the operand with signed integer type can represent
  // all of the values of the type of the operand with unsigned integer type, then
  // the operand with unsigned integer type is converted to the type of the
  // operand with signed integer type.
  if (s0.signed == s1.signed ||
      !greaterRank.signed ||
      greaterRank.width-1 >= lesserRank.width)
  {
    return greaterRank;
  }

  // Otherwise, both operands are converted to the unsigned integer type
  // corresponding to the type of the operand with signed integer type.
  assert greaterRank.signed;
  assert !lesserRank.signed;

  return greaterRank.toUnsigned();
}
}
