package c99.parser;

import c99.*;
import c99.Types.*;
import c99.parser.tree.*;

public class ExprActions extends TreeActions
{
private Qual m_constChar;

@Override
protected void init ( CompEnv compEnv, SymTable symTab )
{
  super.init( compEnv, symTab );

  m_constChar = new Qual( stdSpec( m_opts.signedChar ? TypeSpec.SCHAR : TypeSpec.UCHAR ) );
}

private final boolean isBitField ( TExpr.Expr op )
{
  return (op.getCode() == TreeCode.PTR_MEMBER || op.getCode() == TreeCode.DOT_MEMBER) &&
          ((TExpr.SelectMember)op).getMember().isBitField();
}

private final boolean isLValue ( TExpr.Expr op )
{
  switch (op.getCode())
  {
  case VARREF:
  case STRING:
  case INDIRECT:
  case SUBSCRIPT:
  case PTR_MEMBER:
    return true;

  case DOT_MEMBER:
    return isLValue( ((TExpr.SelectMember)op).getBase() );
  }
  return false;
}

private final boolean needLValue ( CParser.Location loc, TExpr.Expr op, TreeCode code )
{
  if (!isLValue( op ))
  {
    error( loc, "operand of '%s' must be an l-value", code.str );
    return false;
  }
  return true;
}

private final boolean needModifiableLValue ( CParser.Location loc, TExpr.Expr op, TreeCode code )
{
  if (!needLValue( loc, op, code ))
    return false;

  switch (op.getQual().spec.type)
  {
  case ARRAY:
  case FUNCTION:
    error( loc, "Operand of '%s' can't be an array or function", code.str );
    return false;
  }
  if (!op.getQual().spec.isComplete())
  {
    error( loc, "Operand of '%s' has incomplete type '%s'", code.str, op.getQual().readableType() );
    return false;
  }
  if (op.getQual().isConst)
  {
    error( loc, "Operand of '%s' is const", code.str );
    return false;
  }
  if (op.getQual().isConstMember())
  {
    error( loc, "Operand of '%s' (type '%s') has a const member", code.str, op.getQual().readableType() );
    return false;
  }
  return true;
}

public final TExpr.Expr implicitLoad ( TExpr.Expr op )
{
  if (op.isError())
    return op;
  switch (op.getCode())
  {
  case VARREF:
  case STRING:
  {
    Qual qual = ((TExpr.VarRef)op).getDecl().type;
    Spec spec = qual.spec;
    switch (spec.type)
    {
    case ARRAY:
      return new TExpr.Unary( op, TreeCode.IMPLICIT_CAST, new Qual(newPointerSpec( ((ArraySpec)spec).of )), op );
    case FUNCTION:
      return new TExpr.Unary( op, TreeCode.IMPLICIT_CAST, new Qual(newPointerSpec(qual)), op );
    default:
      if (!spec.isComplete())
      {
        error( op, "Incomplete type '%s'", qual.readableType() );
        return new TExpr.Unary( op, TreeCode.IMPLICIT_CAST, s_errorQual, op );
      }
      return new TExpr.Unary( op, TreeCode.IMPLICIT_LOAD, new Qual(spec), op );
    }
  }

  case INDIRECT:
  case SUBSCRIPT:
  case DOT_MEMBER:
  case PTR_MEMBER:
    if (!op.getQual().spec.isComplete())
    {
      error( op, "Incomplete type '%s'", op.getQual().readableType() );
      return new TExpr.Unary( op, TreeCode.IMPLICIT_CAST, s_errorQual, op );
    }
    return new TExpr.Unary( op, TreeCode.IMPLICIT_LOAD, new Qual(op.getQual().spec), op );

  default:
    if (!op.getQual().spec.isComplete())
    {
      error( op, "Incomplete type '%s'", op.getQual().readableType() );
      return new TExpr.Unary( op, TreeCode.IMPLICIT_CAST, s_errorQual, op );
    }
    return op;
  }
}

private TExpr.Expr implicitCast ( TExpr.Expr op, Qual type )
{
  return !op.getQual().compatible( type ) ? new TExpr.Unary( op, TreeCode.IMPLICIT_CAST, type, op ) : op;
}

private TExpr.Expr integerPromotion ( TExpr.Expr op )
{
  if (op.getQual().spec.type.integer)
  {
    Qual promoted = stdQual( Types.integerPromotion( op.getQual().spec.type ) );
    if (promoted.spec.type != op.getQual().spec.type)
      return new TExpr.Unary( op, TreeCode.IMPLICIT_CAST, promoted, op );
  }
  return op;
}

private final boolean isVoidPtr ( Qual qual )
{
  return qual.spec.type == TypeSpec.POINTER && ((PointerSpec)qual.spec).of.spec.type == TypeSpec.VOID;
}

private final boolean isNullPointerConst ( TExpr.Expr expr )
{
  if (expr.getCode() == TreeCode.CONSTANT && expr.getQual().spec.type.integer &&
          ((TExpr.Constant)expr).getValue().isZero())
  {
    return true;
  }
  else if (expr.getCode() == TreeCode.TYPECAST && isVoidPtr( expr.getQual() ))
    return isNullPointerConst( ((TExpr.Typecast)expr).getOperand() );
  else
    return false;
}

public abstract class UnaryExpr
{
  protected final TreeCode code;
  protected final String m_errorSuffix;

  public UnaryExpr ( TreeCode code, String errorSuffix )
  {
    this.code = code;
    m_errorSuffix = errorSuffix != null ? ". "+ errorSuffix : "";
  }

  public TExpr.Expr expr ( CParser.Location loc, TExpr.Expr operand )
  {
    TExpr.Expr res = null;
    if (!operand.isError())
      res = make( loc, operand );
    if (res == null) // Error?
      res = new TExpr.Unary( null, code, s_errorQual, operand );
    return BisonLexer.setLocation( res, loc );
  }

  protected TExpr.Expr make ( CParser.Location loc, TExpr.Expr operand )
  {
    error( loc, "operand of '%s' has invalid type ('%s')%s", code.str, operand.getQual().readableType(), m_errorSuffix );
    return null;
  }
}

public final class IncExpr extends UnaryExpr
{
  public IncExpr ( TreeCode code )
  {
    super( code, "Must be arithmetic or pointer" );
  }

  @Override
  protected TExpr.Expr make ( CParser.Location loc, TExpr.Expr operand )
  {
    if (!(operand.getQual().spec.type.arithmetic || operand.getQual().spec.type == TypeSpec.POINTER))
      return super.make( loc, operand );

    if (!needModifiableLValue( loc, operand, code ))
      return null;

    return new TExpr.Unary( null, code, new Qual(operand.getQual().spec), operand );
  }
}

public class AddressExpr extends UnaryExpr
{
  public AddressExpr ()
  {
    super( TreeCode.ADDRESS, null );
  }

  @Override
  protected TExpr.Expr make ( CParser.Location loc, TExpr.Expr operand )
  {
    if (!needLValue( loc, operand, code ))
      return null;
    if (operand.getCode() == TreeCode.VARREF && ((TExpr.VarRef)operand).getDecl().sclass == SClass.REGISTER)
    {
      error( loc, "operand of %s can't be a register variable", code.str );
      return null;
    }
    if (isBitField(operand))
    {
      error( loc, "operand of %s can't be a bit-field", code.str );
      return null;
    }
    return new TExpr.Unary( null, code, new Qual(newPointerSpec(operand.getQual())), operand );
  }
}

public class IndirectExpr extends UnaryExpr
{
  public IndirectExpr ()
  {
    super( TreeCode.INDIRECT, "Pointer expected" );
  }

  @Override
  protected TExpr.Expr make ( CParser.Location loc, TExpr.Expr operand )
  {
    operand = implicitLoad( operand );
    if (operand.getQual().spec.type == TypeSpec.POINTER)
      return new TExpr.Unary( null, code, ((PointerSpec)operand.getQual().spec).of, operand );
    else
      return super.make( loc, operand );
  }
}

public abstract class LoadingUnaryExpr extends UnaryExpr
{
  public LoadingUnaryExpr ( TreeCode code, String errorSuffix )
  {
    super( code, errorSuffix );
  }

  public final TExpr.Expr expr ( CParser.Location loc, TExpr.Expr operand )
  {
    if (!operand.isError())
      operand = implicitLoad( operand );
    return super.expr( loc, operand );
  }
}

public class AdditiveUnaryExpr extends LoadingUnaryExpr
{
  public AdditiveUnaryExpr ( TreeCode code )
  {
    super( code, "Must be arithmetic" );
  }

  @Override
  protected TExpr.Expr make ( CParser.Location loc, TExpr.Expr operand )
  {
    if (operand.getQual().spec.type.arithmetic)
    {
      operand = integerPromotion( operand );
      return new TExpr.Unary( null, code, operand.getQual(), operand );
    }
    else
      return super.make( loc, operand );
  }
}

public final class TypecastExpr
{
  TypecastExpr () {}

  public final TExpr.Expr expr ( CParser.Location loc, TDeclaration typeName, TExpr.Expr operand )
  {
    TExpr.Expr res = null;

    if (!operand.isError())
      operand = implicitLoad( operand );
    if (!operand.isError())
      res = make( loc, typeName, operand );
    if (res == null) // Error?
      res = new TExpr.Typecast( null, s_errorQual, typeName, operand );

    return BisonLexer.setLocation( res, loc );
  }

  protected final TExpr.Expr make ( CParser.Location loc, TDeclaration typeName, TExpr.Expr operand )
  {
    assert typeName.type != null;
    Qual type = typeName.type;

    if (type.spec.type.isScalar())
    {
      if (!operand.getQual().spec.type.isScalar())
      {
        error( operand, "invalid typecast operand ('%s'). Must be arithmetic or pointer", operand.getQual().readableType() );
        return null;
      }
      if (type.spec.type == TypeSpec.POINTER && operand.getQual().spec.type.floating ||
          type.spec.type.floating && operand.getQual().spec.type == TypeSpec.POINTER)
      {
        error( loc, "'%s' cannot be cast to '%s'", operand.getQual().readableType(), type.readableType() );
        return null;
      }
    }
    else if (type.spec.type == TypeSpec.VOID)
    {}
    else
    {
      error( loc, "casting to invalid type '%s'. Must be void, arithmetic or pointer", type.readableType() );
      return null;
    }

    return new TExpr.Typecast( null, new Qual(type.spec), typeName, operand );
  }
}

public class BitwiseNotExpr extends LoadingUnaryExpr
{
  public BitwiseNotExpr ()
  {
    super( TreeCode.BITWISE_NOT, "Must be integer" );
  }

  @Override
  protected TExpr.Expr make ( CParser.Location loc, TExpr.Expr operand )
  {
    if (operand.getQual().spec.type.integer)
    {
      operand = integerPromotion( operand );
      return new TExpr.Unary( null, code, operand.getQual(), operand );
    }
    else
      return super.make( loc, operand );
  }
}

public class LogNegExpr extends LoadingUnaryExpr
{
  public LogNegExpr ()
  {
    super( TreeCode.LOG_NEG, "Must be arithmetic or pointer" );
  }

  @Override
  protected TExpr.Expr make ( CParser.Location loc, TExpr.Expr operand )
  {
    if (operand.getQual().spec.type.isScalar())
      return new TExpr.Unary( null, code, stdQual(TypeSpec.SINT), integerPromotion(operand) );
    else
      return super.make( loc, operand );
  }
}

private static final Constant.IntC s_zero = Constant.makeLong(TypeSpec.SIZE_T, 0);

public abstract class AttrOfExpr
{
  protected final TreeCode code;

  public AttrOfExpr ( TreeCode code )
  {
    this.code = code;
  }

  public final TExpr.Expr expr ( CParser.Location loc, TExpr.Expr expr )
  {
    TExpr.Expr res = null;
    if (!expr.isError())
    {
      if (!expr.getQual().spec.isComplete())
        error( loc, "'%s' of incomplete type '%s'", code.str, expr.getQual().readableType() );
      else if (isBitField(expr))
        error( loc, "'%s' of a bit-field", code.str );
      else
        res = make( loc, expr );
    }
    if (res == null) // Error?
      res = new TExpr.AttrOfExpr( null, code, s_errorQual, s_zero, expr );
    return BisonLexer.setLocation( res, loc );
  }

  protected final TExpr.Expr make ( long value, TExpr.Expr expr )
  {
    return new TExpr.AttrOfExpr(
            null, code, stdQual(TypeSpec.SIZE_T), Constant.makeLong(TypeSpec.SIZE_T, value), expr
    );
  }

  protected abstract TExpr.Expr make ( CParser.Location loc, TExpr.Expr expr );
}

public abstract class AttrOfType
{
  protected final TreeCode code;

  public AttrOfType ( TreeCode code )
  {
    this.code = code;
  }

  public final TExpr.Expr expr ( CParser.Location loc, TDeclaration typeName )
  {
    TExpr.Expr res = null;
    if (!typeName.error && typeName.type.spec.type != TypeSpec.ERROR)
      if (!typeName.type.spec.isComplete())
        error( loc, "'%s' of incomplete type '%s'", code.str, typeName.type.readableType() );
      else
        res = make( loc, typeName );
    if (res == null) // Error?
      res = new TExpr.AttrOfType( null, code, s_errorQual, s_zero, s_errorQual, typeName );
    return BisonLexer.setLocation( res, loc );
  }

  protected final TExpr.Expr make ( long value, TDeclaration typeName )
  {
    return new TExpr.AttrOfType(
            null, code, stdQual(TypeSpec.SIZE_T), Constant.makeLong(TypeSpec.SIZE_T, value), typeName.type, typeName
    );
  }
  protected abstract TExpr.Expr make ( CParser.Location loc, TDeclaration typeName );
}

public abstract class BinaryExpr
{
  protected final TreeCode code;
  protected final String m_errorSuffix;

  public BinaryExpr ( TreeCode code, String errorSuffix )
  {
    this.code = code;
    m_errorSuffix = errorSuffix != null ? ". "+ errorSuffix : "";
  }

  public final TExpr.Expr expr ( CParser.Location loc, TExpr.Expr left, TExpr.Expr right )
  {
    TExpr.Expr res = null;

    left = implicitLoad( left );
    right = implicitLoad( right );

    if (!left.isError() && !right.isError())
      res = make( loc, left, right );

    if (res == null) // Error?
      res = new TExpr.Binary( null, code, s_errorQual, left, right );

    return BisonLexer.setLocation( res, loc );
  }

  protected TExpr.Expr make ( CParser.Location loc, TExpr.Expr left, TExpr.Expr right )
  {
    error( loc, "invalid operands to '%s' ('%s' and '%s')%s", code.str,
            left.getQual().readableType(), right.getQual().readableType(), m_errorSuffix );
    return null;
  }

  protected final TExpr.Binary usualArithmeticConversions ( TExpr.Expr left, TExpr.Expr right )
  {
    Qual commonType = stdQual( Types.usualArithmeticConversions( left.getQual().spec.type, right.getQual().spec.type ) );
    if (commonType.spec.type != left.getQual().spec.type)
      left = new TExpr.Unary( left, TreeCode.IMPLICIT_CAST, commonType, left );
    if (commonType.spec.type != right.getQual().spec.type)
      right = new TExpr.Unary( right, TreeCode.IMPLICIT_CAST, commonType, right );
    return new TExpr.Binary( null, code, commonType, left, right );
  }
}

public class IntegerBinaryExpr extends BinaryExpr
{
  public IntegerBinaryExpr ( TreeCode code )
  {
    super( code, "Must be integer" );
  }

  @Override
  protected TExpr.Expr make ( CParser.Location loc, TExpr.Expr left, TExpr.Expr right )
  {
    if (left.getQual().spec.type.integer && right.getQual().spec.type.integer)
      return usualArithmeticConversions( left, right );
    else
      return super.make( loc, left, right );
  }
}

public class MultiplicativeExpr extends BinaryExpr
{
  public MultiplicativeExpr ( TreeCode code, String errorSuffix )
  {
    super( code, errorSuffix );
  }

  protected TExpr.Expr make ( CParser.Location loc, TExpr.Expr left, TExpr.Expr right )
  {
    if (left.getQual().spec.type.arithmetic && right.getQual().spec.type.arithmetic)
      return usualArithmeticConversions( left, right );
    else
      return super.make( loc, left, right );
  }
}

public class AdditiveExpr extends MultiplicativeExpr
{
  public AdditiveExpr ( TreeCode code, String errorSuffix )
  {
    super( code, errorSuffix );
  }

  @Override
  protected TExpr.Expr make ( CParser.Location loc, TExpr.Expr left, TExpr.Expr right )
  {
    if (left.getQual().spec.type == TypeSpec.POINTER && right.getQual().spec.type.integer)
    {
      PointerSpec ptr = (PointerSpec) left.getQual().spec;
      if (!ptr.of.spec.isComplete())
      {
        error( loc, "Arithmetic on a pointer to incomplete type '%s'", ptr.of.readableType() );
        return null;
      }
      right = integerPromotion( right );
      return new TExpr.Binary( null, code, left.getQual(), left, right );
    }
    else
      return super.make( loc, left, right );
  }
}

public class ShiftExpression extends BinaryExpr
{
  public ShiftExpression ( TreeCode code )
  {
    super( code, "Must be integer" );
  }

  @Override
  protected TExpr.Expr make ( CParser.Location loc, TExpr.Expr left, TExpr.Expr right )
  {
    if (left.getQual().spec.type.integer && right.getQual().spec.type.integer)
    {
      left = integerPromotion(left);
      right = integerPromotion(right);
      return new TExpr.Binary( null, code, left.getQual(), left, right );
    }
    else
      return super.make( loc, left, right );
  }
}

public class RelationalExpression extends BinaryExpr
{
  public RelationalExpression ( TreeCode code )
  {
    super( code, null );
  }

  @Override
  protected TExpr.Expr make ( CParser.Location loc, TExpr.Expr left, TExpr.Expr right )
  {
    if (left.getQual().spec.type.arithmetic && right.getQual().spec.type.arithmetic)
    {
      Qual commonType = stdQual( Types.usualArithmeticConversions( left.getQual().spec.type, right.getQual().spec.type ) );
      if (commonType.spec.type != left.getQual().spec.type)
        left = new TExpr.Unary( left, TreeCode.IMPLICIT_CAST, commonType, left );
      if (commonType.spec.type != right.getQual().spec.type)
        right = new TExpr.Unary( right, TreeCode.IMPLICIT_CAST, commonType, right );
      return new TExpr.Binary( null, code, stdQual( TypeSpec.SINT ), left, right );
    }
    else if (left.getQual().spec.type == TypeSpec.POINTER && right.getQual().spec.type == TypeSpec.POINTER)
    {
      PointerSpec lptr = (PointerSpec)left.getQual().spec;
      PointerSpec rptr = (PointerSpec)right.getQual().spec;

      // FIXME: pointer sizes
      if (!lptr.of.spec.compatible( rptr.of.spec ))
      {
        error( loc, "%s: '%s' and '%s' are not pointers to compatible types", code.str,
                lptr.of.spec.readableType(), rptr.of.spec.readableType() );
      }
      return new TExpr.Binary( null, code, stdQual(TypeSpec.SINT), left, right );
    }
    else
      return super.make( loc, left, right );
  }
}

public final class EqualityExpression extends RelationalExpression
{
  public EqualityExpression ( TreeCode code )
  {
    super( code );
  }

  @Override
  protected TExpr.Expr make ( CParser.Location loc, TExpr.Expr left, TExpr.Expr right )
  {
    // FIXME: far/near pointers
    if (left.getQual().spec.type == TypeSpec.POINTER && isNullPointerConst( right ))
    {
      return new TExpr.Binary( null, code, stdQual(TypeSpec.SINT), left, implicitCast( right, left.getQual() ) );
    }
    else if (isNullPointerConst( left ) && right.getQual().spec.type == TypeSpec.POINTER)
    {
      return new TExpr.Binary( null, code, stdQual(TypeSpec.SINT), implicitCast( left, right.getQual() ), right );
    }
    else if (isVoidPtr(left.getQual()) && right.getQual().spec.type == TypeSpec.POINTER)
    {
      return new TExpr.Binary( null, code, stdQual(TypeSpec.SINT), left, implicitCast( right, left.getQual() ) );
    }
    else if (left.getQual().spec.type == TypeSpec.POINTER && isVoidPtr(right.getQual()))
    {
      return new TExpr.Binary( null, code, stdQual(TypeSpec.SINT), implicitCast( left, right.getQual() ), right );
    }
    else
      return super.make( loc, left, right );
  }
}

public final class LogicalExpression extends BinaryExpr
{
  public LogicalExpression ( TreeCode code )
  {
    super( code, "Must be arithmetic or pointer" );
  }

  @Override
  protected TExpr.Expr make ( CParser.Location loc, TExpr.Expr left, TExpr.Expr right )
  {
    if (left.getQual().spec.type.isScalar() && right.getQual().spec.type.isScalar())
      return new TExpr.Binary( null, code, stdQual(TypeSpec.SINT), integerPromotion(left), integerPromotion(right) );
    else
      return super.make( loc, left, right );
  }
}

private final TExpr.Expr exprError ( ISourceRange loc )
{
  return new TExpr.Error( loc, s_errorQual );
}
public final TExpr.Expr exprError ( CParser.Location loc )
{
  return BisonLexer.setLocation( exprError( (ISourceRange) null ), loc );
}

public TExpr.Expr exprIdent ( CParser.Location loc, Symbol sym )
{
  final Decl decl = sym.topDecl;
  if (decl  == null || decl.kind != Decl.Kind.VAR && decl.kind != Decl.Kind.ENUM_CONST)
  {
    error( loc, "Undefined identifier '%s'", sym.name );
    return exprError( loc );
  }

  if (decl.kind == Decl.Kind.VAR)
    return BisonLexer.setLocation( new TExpr.VarRef( null, decl.type, decl  ), loc );
  else
    return BisonLexer.setLocation( new TExpr.EnumConst( null, decl.type, decl.enumValue, decl ), loc );
}

public TExpr.Expr exprConstant ( CParser.Location loc, Constant.ArithC value )
{
  return BisonLexer.setLocation( new TExpr.ArithConstant( null, stdQual( value.spec ), value ), loc );
}

public TExpr.Expr exprStringLiteral ( TStringLiteral lit )
{
  ArraySpec s = newArraySpec( lit, m_constChar, lit.value.length+1 );
  if (s == null)
    return exprError( lit );
  return new TExpr.StringLiteral(lit, new Qual(s), lit.value);
}

public final BinaryExpr m_subscript = new BinaryExpr( TreeCode.SUBSCRIPT, "Must be pointer and integer" )
{
  @Override
  protected TExpr.Expr make ( CParser.Location loc, TExpr.Expr left, TExpr.Expr right )
  {
    TExpr.Expr ptre, inte;
    boolean leftPtr;

    if (left.getQual().spec.type == TypeSpec.POINTER && right.getQual().spec.type.integer)
    {
      ptre = left;
      inte = right;
      leftPtr = true;
    }
    else if (left.getQual().spec.type.integer && right.getQual().spec.type == TypeSpec.POINTER)
    {
      ptre = right;
      inte = left;
      leftPtr = false;
    }
    else
      return super.make( loc, left, right );

    PointerSpec ptrSpec = (PointerSpec) ptre.getQual().spec;
    if (!ptrSpec.of.spec.isComplete())
    {
      error( loc, "%s on a pointer (%s) to incomplete type '%s'", code.str,
              SourceRange.formatRange( ptre ), ptrSpec.of.readableType() );
      return null;
    }
    inte = integerPromotion( inte );

    return new TExpr.Binary( null, code, new Qual(ptrSpec.of.spec), leftPtr?ptre:inte, leftPtr?inte:ptre );
  }
};

public abstract class SelectMemberExpr
{
  protected final TreeCode code;

  public SelectMemberExpr ( TreeCode code )
  {
    this.code = code;
  }

  public final TExpr.Expr expr ( CParser.Location loc, TExpr.Expr agg, Symbol memberName )
  {
    TExpr.Expr res = null;
    if (!agg.isError() && memberName != null)
      res = make( loc, agg, memberName );
    if (res == null)
      res = new TExpr.SelectMember( null, code, s_errorQual, agg, null, null );
    return BisonLexer.setLocation( res, loc );
  }

  protected abstract TExpr.Expr make ( CParser.Location loc, TExpr.Expr agg, Symbol memberName );
}

public final SelectMemberExpr m_dotMember = new SelectMemberExpr(TreeCode.DOT_MEMBER)
{
  @Override
  protected TExpr.Expr make ( CParser.Location loc, TExpr.Expr agg, Symbol memberName )
  {
    Spec spec = agg.getQual().spec;
    if (spec.type != TypeSpec.STRUCT && spec.type != TypeSpec.UNION)
    {
      error( loc, "%s%s of type '%s' which is not struct/union", code.str, memberName.name, spec.readableType() );
      return null;
    }
    if (!spec.isComplete())
    {
      error( loc, "%s%s of incomplete type '%s'", code.str, memberName.name, spec.readableType() );
      return null;
    }
    StructUnionSpec aggSpec = (StructUnionSpec)spec;
    Member m;
    if ( (m = aggSpec.lookupMember( memberName )) == null)
    {
      error( loc, "invalid member '%s'%s%s", spec.readableType(), code.str, memberName.name );
      return null;
    }

    // 6.5.2.3#3
    // If the first expression has qualified type, the result has the so-qualified version of the
    // type of the designated member.
    // TZM: I extend this a bit by combining the member's qualifier with the base one
    Qual comb = m.type.copy();
    comb.combine( agg.getQual() );
    return new TExpr.SelectMember( null, code, comb, agg, aggSpec, m );
  }
};

public final SelectMemberExpr m_ptrMember = new SelectMemberExpr(TreeCode.PTR_MEMBER)
{
  @Override
  protected TExpr.Expr make ( CParser.Location loc, TExpr.Expr agg, Symbol memberName )
  {
    agg = implicitLoad( agg );

    Spec bspec = agg.getQual().spec;
    if (bspec.type != TypeSpec.POINTER)
    {
      error( loc, "invalid operand to '%s%s' (%s). Must be a pointer", code.str, memberName.name, bspec.readableType() );
      return null;
    }

    PointerSpec ptrSpec = (PointerSpec)bspec;
    Spec toSpec = ptrSpec.of.spec;

    if (toSpec.type != TypeSpec.STRUCT && toSpec.type != TypeSpec.UNION)
    {
      error( loc, "%s%s of type '%s' which is not a struct/union",
              code.str, memberName.name, toSpec.readableType() );
      return null;
    }
    if (!toSpec.isComplete())
    {
      error( loc, "%s%s of incomplete type '%s'", code.str, memberName.name, toSpec.readableType() );
      return null;
    }
    StructUnionSpec aggSpec = (StructUnionSpec)toSpec;
    Member m;
    if ( (m = aggSpec.lookupMember( memberName )) == null)
    {
      error( loc, "invalid member '%s'%s%s", toSpec.readableType(), code.str, memberName.name );
      return null;
    }

    // 6.5.2.3#4
    // If the first expression is a pointer to a qualified type, the result has the so-qualified
    // version of the type of the designated member.
    // TZM: I extend this a bit by combining the member's qualifier with the base one
    Qual comb = m.type.copy();
    comb.combine( ptrSpec.of );
    return new TExpr.SelectMember( null, code, comb, agg, aggSpec, m );
  }
};

public final IncExpr m_postInc = new IncExpr( TreeCode.POST_INC );
public final IncExpr m_postDec = new IncExpr( TreeCode.POST_DEC );

public final AddressExpr m_addr = new AddressExpr();

public final IndirectExpr m_indirect = new IndirectExpr();

public final IncExpr m_preInc = new IncExpr( TreeCode.PRE_INC );
public final IncExpr m_preDec = new IncExpr( TreeCode.PRE_DEC );

public final AdditiveUnaryExpr m_uplus = new AdditiveUnaryExpr( TreeCode.U_PLUS );
public final AdditiveUnaryExpr m_uminus = new AdditiveUnaryExpr( TreeCode.U_MINUS );

public final BitwiseNotExpr m_bitwiseNot = new BitwiseNotExpr();

public final LogNegExpr m_logNeg = new LogNegExpr();

public final AttrOfExpr m_sizeOfExpr = new AttrOfExpr(TreeCode.SIZEOF_EXPR)
{
  @Override
  protected TExpr.Expr make ( CParser.Location loc, TExpr.Expr expr )
  {
    return super.make( expr.getQual().spec.sizeOf(), expr );
  }
};

public final AttrOfExpr m_alignOfExpr = new AttrOfExpr(TreeCode.ALIGNOF_EXPR)
{
  @Override
  protected TExpr.Expr make ( CParser.Location loc, TExpr.Expr expr )
  {
    return super.make( expr.getQual().spec.alignOf(), expr );
  }
};

public final AttrOfType m_sizeOfType = new AttrOfType(TreeCode.SIZEOF_TYPE)
{
  @Override
  protected TExpr.Expr make ( CParser.Location loc, TDeclaration typeName )
  {
    return super.make( typeName.type.spec.sizeOf(), typeName );
  }
};

public final AttrOfType m_alignOfType = new AttrOfType(TreeCode.ALIGNOF_TYPE)
{
  @Override
  protected TExpr.Expr make ( CParser.Location loc, TDeclaration typeName )
  {
    return super.make( typeName.type.spec.alignOf(), typeName );
  }
};

public final TypecastExpr m_typecast = new TypecastExpr();

public final BinaryExpr m_mul = new MultiplicativeExpr( TreeCode.MUL, "Must be arithmetic" );
public final BinaryExpr m_div = new MultiplicativeExpr( TreeCode.DIV, "Must be arithmetic" );

public final BinaryExpr m_remainder = new IntegerBinaryExpr( TreeCode.REMAINDER );

public final BinaryExpr m_add = new AdditiveExpr( TreeCode.ADD, null ) {
  @Override
  public TExpr.Expr make ( CParser.Location loc, TExpr.Expr left, TExpr.Expr right )
  {
    if (left.getQual().spec.type.integer && right.getQual().spec.type == TypeSpec.POINTER)
    {
      PointerSpec ptr = (PointerSpec) right.getQual().spec;
      if (!ptr.of.spec.isComplete())
      {
        error( loc, "Arithmetic on a pointer to incomplete type '%s'", ptr.of.readableType() );
        return null;
      }
      left = integerPromotion( left );
      return new TExpr.Binary( null, code, right.getQual(), left, right );
    }
    else
      return super.make( loc, left, right );
  }
};

public final BinaryExpr m_sub = new BinaryExpr( TreeCode.SUB, null ) {
  @Override
  public TExpr.Expr make ( CParser.Location loc, TExpr.Expr left, TExpr.Expr right )
  {
    if (left.getQual().spec.type == TypeSpec.POINTER && right.getQual().spec.type == TypeSpec.POINTER)
    {
      PointerSpec lptr = (PointerSpec)left.getQual().spec;
      PointerSpec rptr = (PointerSpec)right.getQual().spec;

      if (!lptr.of.spec.compatible( rptr.of.spec ))
      {
        error( loc, "%s: '%s' and '%s' are not pointers to compatible types", code.str,
                lptr.of.spec.readableType(), rptr.of.spec.readableType() );
      }

      return new TExpr.Binary( null, code, stdQual(TypeSpec.PTRDIFF_T), left, right );
    }
    else
      return super.make( loc, left, right );
  }
};

public final ShiftExpression m_lshift = new ShiftExpression( TreeCode.LSHIFT );
public final ShiftExpression m_rshift = new ShiftExpression( TreeCode.RSHIFT );

public final RelationalExpression m_lt = new RelationalExpression( TreeCode.LT );
public final RelationalExpression m_gt = new RelationalExpression( TreeCode.GT );
public final RelationalExpression m_le = new RelationalExpression( TreeCode.LE );
public final RelationalExpression m_ge = new RelationalExpression( TreeCode.GE );

public final EqualityExpression m_eq = new EqualityExpression( TreeCode.EQ );
public final EqualityExpression m_ne = new EqualityExpression( TreeCode.NE );

public final IntegerBinaryExpr m_bitwiseAnd = new IntegerBinaryExpr( TreeCode.BITWISE_AND );
public final IntegerBinaryExpr m_bitwiseXor = new IntegerBinaryExpr( TreeCode.BITWISE_XOR );
public final IntegerBinaryExpr m_bitwiseOr = new IntegerBinaryExpr( TreeCode.BITWISE_OR );

public final LogicalExpression m_logAnd = new LogicalExpression( TreeCode.LOG_AND );
public final LogicalExpression m_logOr = new LogicalExpression( TreeCode.LOG_OR );

private final class IntConstEvaluator implements TExpr.ExprVisitor
{
  private TExpr.Expr m_errorNode;
  private Constant.ArithC m_res;
  private Qual m_resType;

  private final boolean retError ( TExpr.Expr e )
  {
    m_res = null;
    m_resType = null;
    m_errorNode = e;
    return false;
  }

  private final boolean retResult ( Constant.ArithC c, Qual type )
  {
    m_res = c;
    m_resType = type;
    return true;
  }

  public TExpr.Expr getErrorNode ()
  {
    return m_errorNode;
  }

  public Constant.ArithC getRes ()
  {
    return m_res;
  }

  public Qual getResType ()
  {
    return m_resType;
  }

  @Override public boolean visitError ( TExpr.Error e ) {
    return retError( e );
  }

  @Override public boolean visitVarRef ( TExpr.VarRef e ) {
    return retError( e );
  }

  @Override public boolean visitArithConstant ( TExpr.ArithConstant e ) {
    return retResult( e.getValue(), e.getQual() );
  }

  @Override public boolean visitEnumConst ( TExpr.EnumConst e ) {
    return retResult( e.getValue(), e.getQual() );
  }

  @Override public boolean visitAttrOfType ( TExpr.AttrOfType e ) {
    return retResult( e.getValue(), e.getQual() );
  }

  @Override public boolean visitAttrOfExpr ( TExpr.AttrOfExpr e ) {
    return retResult( e.getValue(), e.getQual() );
  }

  @Override public boolean visitStringLiteral ( TExpr.StringLiteral e ) {
    return retError( e );
  }

  @Override public boolean visitCall ( TExpr.Call e ) {
    return retError( e );
  }

  @Override public boolean visitSelectMember ( TExpr.SelectMember e ) {
    if (!e.getBase().visit( this ))
      return false;

    // We simply calculate the address of the member (the base is constant)
    switch (e.getCode())
    {
    case DOT_MEMBER:
    case PTR_MEMBER:
      Constant.IntC c = Constant.newIntConstant( TypeSpec.UINTPTR_T );
      c.add( Constant.convert( c.spec, m_res ), Constant.makeLong( c.spec, e.getMember().getOffset() ) );
      return retResult( c, stdQual(c.spec) );
    default:
      assert false;
      return false;
    }
  }

  @Override public boolean visitUnary ( TExpr.Unary e )
  {
    if (!e.getOperand().visit( this ))
      return false;

    Constant.ArithC c;
    final TypeSpec ts = e.getQual().spec.type;
    switch (e.getCode())
    {
    case POST_INC:
    case POST_DEC:
    case PRE_INC:
    case PRE_DEC:
      return retError( e );

    case ADDRESS:
      // The operand, which we know is constant, must have calculated its own address. It is one of
      // INDIRECT, DOT_MEMBER or PTR_MEMBER (otherwise it wouldn't be constant). We do nothing.
    case INDIRECT:
      // The operand, which we know is constant, must have calculated its own address.
      // We do nothing (IMPLICIT_LOAD would do the actual loading)
      return retResult( m_res, m_resType );

    case U_PLUS:
      c = Constant.newConstant( ts );
      c.assign( m_res );
      break;

    case U_MINUS:
      c = Constant.newConstant( ts );
      c.neg( m_res );
      break;

    case BITWISE_NOT:
      c = Constant.newConstant( ts );
      ((Constant.IntC)c).not( (Constant.IntC)m_res );
      break;

    case LOG_NEG:
      c = Constant.makeLong( ts, m_res.isZero() ? 1 : 0 );
      break;

    case IMPLICIT_CAST:
      return performTypecast( e );

    case IMPLICIT_LOAD:
      return retError( e );

    default:
      c = null;
      assert false : "Unsupported code "+ e.getCode();
    }
    return retResult( c, e.getQual() );
  }

  // factor out common code between TYPECAST AND IMPLICIT_CAST
  private final boolean performTypecast ( TExpr.Unary e )
  {
    Qual type = e.getQual();
    TypeSpec ts;

    if (type.spec.type.arithmetic)
      ts = type.spec.type;
    else if (type.spec.type == TypeSpec.POINTER)
      ts = TypeSpec.UINTPTR_T; // FIXME: different pointer sizes
    else
      return retError( e );

    return retResult( Constant.convert(ts, m_res), type );
  }

  @Override public boolean visitTypecast ( TExpr.Typecast e )
  {
    if (!e.getOperand().visit( this ))
      return false;

    return performTypecast( e );
  }

  /** Add or subtract a constant to a pointer */
  private final void performPointerAdd (
    Constant.ArithC resc, TExpr.Expr ptre, Constant.ArithC ptrc, TExpr.Expr adde, Constant.ArithC addc,
    boolean add
  )
  {
    Spec ptrTo = ((PointerSpec)ptre.getQual().spec).of.spec;
    assert ptrTo.isComplete();
    assert ptrc.spec == TypeSpec.UINTPTR_T;
    assert addc.spec.integer;

    // Multiply the addend by sizeof(*ptr)
    Constant.IntC tmp = Constant.makeLong( addc.spec, ptrTo.sizeOf() );
    tmp.mul( tmp, addc );
    addc = tmp;

    ptrc = Constant.convert( resc.spec, ptrc );
    addc = Constant.convert( resc.spec, addc );
    if (add)
      resc.add( ptrc, addc );
    else
      resc.sub( ptrc, addc );
  }

  /** Subtract two pointers resulting in a ptrdiff_t */
  private final boolean performPointerSub (
    Constant.ArithC resc, TExpr.Expr left, Constant.ArithC lc, TExpr.Expr right, Constant.ArithC rc
  )
  {
    Spec ptrTo = ((PointerSpec)left.getQual().spec).of.spec;
    assert ptrTo.isComplete();
    // Both pointers are constants stored as uintptr_t
    assert lc.spec == TypeSpec.UINTPTR_T && rc.spec == TypeSpec.UINTPTR_T;

    if (ptrTo.sizeOf() == 0)
      return false;

    // Subtract the pointers
    Constant.IntC tmp = Constant.newIntConstant( lc.spec );
    tmp.sub( lc, rc );

    // Convert to signed (the difference could be negative)
    tmp = (Constant.IntC)Constant.convert( tmp.spec.toSigned(), tmp );

    // Divide by sizeof(*ptr)
    tmp.div( tmp, Constant.makeLong( tmp.spec, ptrTo.sizeOf() ) );

    // Convert to the final type
    resc.castFrom( tmp );
    return true;
  }

  @Override public boolean visitBinary ( TExpr.Binary e )
  {
    final TypeSpec ts = e.getQual().spec.type != TypeSpec.POINTER ? e.getQual().spec.type : TypeSpec.UINTPTR_T;

    if (!e.getLeft().visit( this ))
      return false;
    Constant.ArithC lc = m_res;

    // Short-circuit "||" and "&&". We must ignore the second operand even if it is not a constant
    if (e.getCode() == TreeCode.LOG_OR && lc.isTrue())
      return retResult( Constant.makeLong( ts, 1 ), e.getQual() );
    else if (e.getCode() == TreeCode.LOG_AND && !lc.isTrue())
      return retResult( Constant.makeLong( ts, 0 ), e.getQual() );

    if (!e.getRight().visit( this ))
      return false;
    Constant.ArithC rc = m_res;

    Constant.ArithC c = Constant.newConstant( ts );

    switch (e.getCode())
    {
    case SUBSCRIPT: // left[right] corresponds to *(left + right)
      // We must calculate the address of the element. The actual loading, if requested, would be done by
      // IMPLICIT_LOAD (and would mean we are not a const expression)
      c = Constant.newConstant( TypeSpec.UINTPTR_T );
      if (e.getLeft().getQual().spec.type == TypeSpec.POINTER)
        performPointerAdd( c, e.getLeft(), lc, e.getRight(), rc, true );
      else if (e.getRight().getQual().spec.type == TypeSpec.POINTER)
        performPointerAdd( c, e.getRight(), rc, e.getLeft(), lc, true );
      else
      {
        assert false;
        return retError( e );
      }
      break;

    case MUL:
      c.mul( lc, rc );
      break;
    case DIV:
      if (rc.spec.integer && rc.isZero())
      {
        warning( e, "division by zero" );
        return retError( e );
      }
      c.div( lc, rc );
      break;
    case REMAINDER:
      if (rc.isZero())
      {
        error( e, "division by zero" );
        return retError( e );
      }
      ((Constant.IntC)c).rem( (Constant.IntC)lc, (Constant.IntC)rc );
      break;
    case ADD:
      if (e.getLeft().getQual().spec.type == TypeSpec.POINTER)
        performPointerAdd( c, e.getLeft(), lc, e.getRight(), rc, true );
      else if (e.getRight().getQual().spec.type == TypeSpec.POINTER)
        performPointerAdd( c, e.getRight(), rc, e.getLeft(), lc, true );
      else
        c.add( Constant.convert(ts, lc), Constant.convert(ts, rc) );
      break;
    case SUB:
      if (e.getRight().getQual().spec.type == TypeSpec.POINTER) // pointer - pointer?
      {
        assert e.getLeft().getQual().spec.type == TypeSpec.POINTER;
        if (!performPointerSub( c, e.getLeft(), lc, e.getRight(), rc ))
        {
          warning( e, "division by zero in pointer subtraction" );
          return retError( e );
        }
      }
      else if (e.getLeft().getQual().spec.type == TypeSpec.POINTER) // pointer - int?
        performPointerAdd( c, e.getLeft(), lc, e.getRight(), rc, false );
      else
        c.sub( Constant.convert( ts, lc ), Constant.convert( ts, rc ) );
      break;
    case LSHIFT:
      ((Constant.IntC)c).shl( (Constant.IntC)lc, (Constant.IntC)rc );
      break;
    case RSHIFT:
      ((Constant.IntC)c).shr( (Constant.IntC) lc, (Constant.IntC) rc );
      break;
    case LT:
      ((Constant.IntC)c).setBool( lc.lt( rc ) );
      break;
    case GT:
      ((Constant.IntC)c).setBool( lc.gt( rc ) );
      break;
    case LE:
      ((Constant.IntC)c).setBool( lc.le( rc ) );
      break;
    case GE:
      ((Constant.IntC)c).setBool( lc.ge( rc ) );
      break;
    case EQ:
      ((Constant.IntC)c).setBool( lc.eq( rc ) );
      break;
    case NE:
      ((Constant.IntC)c).setBool( lc.ne( rc ) );
      break;
    case BITWISE_AND:
      ((Constant.IntC)c).and( (Constant.IntC)lc, (Constant.IntC)rc );
      break;
    case BITWISE_XOR:
      ((Constant.IntC)c).xor( (Constant.IntC)lc, (Constant.IntC)rc );
      break;
    case BITWISE_OR:
      ((Constant.IntC)c).or( (Constant.IntC)lc, (Constant.IntC)rc );
      break;
    case LOG_AND:
      ((Constant.IntC)c).setBool( lc.isTrue() && rc.isTrue() );
      break;
    case LOG_OR:
      ((Constant.IntC)c).setBool( lc.isTrue() || rc.isTrue() );
      break;

    default:
      c = null;
      assert false : "Unsupported code "+ e.getCode();
      break;
    }
    return retResult( c, e.getQual() );
  }
}

private final TExpr.ArithConstant errorVal ( ISourceRange loc )
{
  return new TExpr.ArithConstant( loc, s_errorQual, Constant.makeLong( TypeSpec.SINT, 0 ) );
}

public final TExpr.ArithConstant constantExpression ( ISourceRange loc, TExpr.Expr e )
{
  e = implicitLoad( e );
  if (e.isError())
    return errorVal( loc );

  IntConstEvaluator ev = new IntConstEvaluator();
  if (!e.visit( ev ))
  {
    error( ev.getErrorNode(), "not a constant expression" );
    return errorVal( loc );
  }
  if (!ev.getResType().spec.type.integer)
  {
    error( loc, "not an integer constant expression" );
    return errorVal( loc );
  }
  return new TExpr.ArithConstant( loc, ev.getResType(), ev.getRes() );
}

public final TExpr.ArithConstant constantExpression ( CParser.Location loc, TExpr.Expr e )
{
  return constantExpression( BisonLexer.fromLocation(loc), e );
}

}
