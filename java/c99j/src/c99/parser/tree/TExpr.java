package c99.parser.tree;

import c99.ISourceRange;
import c99.SourceRange;
import c99.Types.*;
import c99.Utils;
import c99.parser.Decl;
import c99.parser.pp.Misc;

/**
 * A namespace for expression tree classes
 */
public final class TExpr
{
private TExpr() {}

public static abstract class Expr extends SourceRange
{
  private final TreeCode m_code;
  private final Qual m_qual;

  public Expr ( ISourceRange rng, TreeCode code, Qual qual )
  {
    super( rng );
    m_code = code;
    m_qual = qual;
  }

  public abstract boolean visit ( ExprVisitor v );
  public abstract int getNumChildren ();
  public abstract Expr getChild ( int i );
  public abstract String formatDetails ();

  public final TreeCode getCode () { return m_code; }

  public final boolean isError () { return m_qual.spec.isError(); }

  public final Qual getQual () { return m_qual; }

  protected final Expr invalidChild ( int index )
  {
    throw new ArrayIndexOutOfBoundsException( index );
  }
}

public static class Error extends Expr
{
  public Error ( ISourceRange rng, Qual qual )
  {
    super( rng, TreeCode.ERROR, qual );
  }

  @Override public final boolean visit ( ExprVisitor v ) { return v.visitError( this ); }
  @Override public final int getNumChildren () { return 0; }
  @Override public final Expr getChild ( int i ) { return invalidChild( i ); }
  @Override public final String formatDetails () { return ""; }
}

public static class VarRef extends Expr
{
  private final Decl m_decl;

  public VarRef ( ISourceRange rng, Qual qual, Decl decl )
  {
    super( rng, TreeCode.VARREF, qual );
    m_decl = decl;
  }

  @Override public final boolean visit ( ExprVisitor v ) { return v.visitVarRef( this ); }
  @Override public final int getNumChildren () { return 0; }
  @Override public final Expr getChild ( int i ) { return invalidChild( i ); }

  @Override
  public final String formatDetails ()
  {
    return "'" + m_decl.symbol.name + "'";
  }

  public final Decl getDecl ()
  {
    return m_decl;
  }
}

public static abstract class Constant extends Expr
{
  private final c99.Constant.ArithC m_value;

  public Constant ( ISourceRange rng, TreeCode code, Qual qual, c99.Constant.ArithC value )
  {
    super( rng, code, qual );
    m_value = value;
  }

  public final c99.Constant.ArithC getValue ()
  {
    return m_value;
  }

  public String formatDetails ()
  {
    return "=" + m_value.toString();
  }
}

public static class ArithConstant extends Constant
{
  public ArithConstant ( ISourceRange rng, Qual qual, c99.Constant.ArithC value )
  {
    super( rng, TreeCode.CONSTANT, qual, value );
  }

  @Override public final boolean visit ( ExprVisitor v ) { return v.visitArithConstant( this ); }
  @Override public final int getNumChildren () { return 0; }
  @Override public final Expr getChild ( int i ) { return invalidChild( i ); }
}

public static final class EnumConst extends Constant
{
  private final Decl m_decl;

  public EnumConst ( ISourceRange rng, Qual qual, c99.Constant.ArithC value, Decl decl )
  {
    super( rng, TreeCode.ENUM_CONST, qual, value );
    m_decl = decl;
  }

  @Override public final boolean visit ( ExprVisitor v ) { return v.visitEnumConst( this ); }
  @Override public final int getNumChildren () { return 0; }
  @Override public final Expr getChild ( int i ) { return invalidChild( i ); }

  @Override
  public final String formatDetails ()
  {
    return "'"+m_decl.symbol.name+"'" + super.formatDetails();
  }

  public Decl getDecl ()
  {
    return m_decl;
  }
}

public static final class AttrOfType extends Constant
{
  private final Qual m_ofType;
  private final TDeclaration m_decl;

  public AttrOfType ( ISourceRange rng, TreeCode code, Qual qual, c99.Constant.ArithC value, Qual ofType, TDeclaration decl )
  {
    super( rng, code, qual, value );
    assert code == TreeCode.SIZEOF_TYPE || code == TreeCode.ALIGNOF_TYPE;
    m_ofType = ofType;
    m_decl = decl;
  }

  @Override public final boolean visit ( ExprVisitor v ) { return v.visitAttrOfType( this ); }
  @Override public final int getNumChildren () { return 0; }
  @Override public final Expr getChild ( int i ) { return invalidChild( i ); }

  @Override
  public final String formatDetails ()
  {
    return "'"+m_ofType.readableType()+"'" + super.formatDetails();
  }
}

public static class AttrOfExpr extends Constant
{
  private final TExpr.Expr m_expr;

  public AttrOfExpr ( ISourceRange rng, TreeCode code, Qual qual, c99.Constant.ArithC value, Expr expr )
  {
    super( rng, code, qual, value );
    assert code == TreeCode.SIZEOF_EXPR || code == TreeCode.ALIGNOF_EXPR;
    m_expr = expr;
  }

  @Override public final boolean visit ( ExprVisitor v ) { return v.visitAttrOfExpr( this ); }
  @Override public final int getNumChildren () { return 1; }
  @Override public final Expr getChild ( int i ) { return i == 0 ? m_expr : invalidChild( i ); }

  public final Expr getExpr ()
  {
    return m_expr;
  }
}

public static final class StringLiteral extends Expr
{
  private final byte[] m_value;

  public StringLiteral ( ISourceRange rng, Qual qual, byte[] value )
  {
    super( rng, TreeCode.STRING, qual );
    m_value = value;
  }

  @Override public final boolean visit ( ExprVisitor v ) { return v.visitStringLiteral( this ); }
  @Override public final int getNumChildren () { return 0; }
  @Override public final Expr getChild ( int i ) { return invalidChild( i ); }

  @Override
  public String formatDetails ()
  {
    return Misc.simpleEscapeString( Utils.asciiString(m_value) );
  }

  public final byte[] getValue ()
  {
    return m_value;
  }
}

public static class Call extends Expr
{
  private final Expr m_target;
  private final Expr[] m_args;

  private static final Expr[] s_noArgs = new Expr[0];

  public Call ( ISourceRange rng, Qual qual, Expr target, Expr[] args )
  {
    super( rng, TreeCode.CALL, qual );
    m_target = target;
    m_args = args != null ? args : s_noArgs;
  }

  @Override public final boolean visit ( ExprVisitor v ) { return v.visitCall( this ); }

  @Override
  public final int getNumChildren ()
  {
    return m_args.length + 1;
  }

  @Override
  public final Expr getChild ( int i )
  {
    return i == 0 ? m_target : m_args[i-1];
  }

  @Override
  public final String formatDetails ()
  {
    return "";
  }
}

public static final class SelectMember extends Expr
{
  private final Expr m_base;
  private final StructUnionSpec m_aggSpec;
  private final Member m_member;

  public SelectMember ( ISourceRange rng, TreeCode code, Qual qual, Expr base, StructUnionSpec aggSpec, Member member )
  {
    super( rng, code, qual );
    assert code == TreeCode.PTR_MEMBER || code == TreeCode.DOT_MEMBER;
    m_base = base;
    m_aggSpec = aggSpec;
    m_member = member;
  }

  @Override public final boolean visit ( ExprVisitor v ) { return v.visitSelectMember( this ); }
  @Override public final int getNumChildren () { return 1; }
  @Override public final Expr getChild ( int i ) { return i == 0 ? m_base : invalidChild( i ); }

  @Override
  public final String formatDetails ()
  {
    return "'." + m_member.name.name + "'";
  }

  public final Expr getBase ()
  {
    return m_base;
  }

  public final Member getMember ()
  {
    return m_member;
  }
}

public static class Unary extends Expr
{
  private final Expr m_operand;

  public Unary ( ISourceRange rng, TreeCode code, Qual qual, Expr operand )
  {
    super( rng, code, qual );
    m_operand = operand;
  }

  @Override public boolean visit ( ExprVisitor v ) { return v.visitUnary( this ); }
  @Override public final int getNumChildren () { return 1; }
  @Override public final Expr getChild ( int i ) { return i == 0 ? m_operand : invalidChild( i ); }

  @Override
  public final String formatDetails ()
  {
    return "";
  }

  public final Expr getOperand ()
  {
    return m_operand;
  }
}

public static class Typecast extends Unary
{
  private final TDeclaration m_decl;

  public Typecast ( ISourceRange rng, Qual qual, TDeclaration decl, Expr operand )
  {
    super( rng, TreeCode.TYPECAST, qual, operand );
    m_decl = decl;
  }

  @Override
  public final boolean visit ( ExprVisitor v )
  {
    return v.visitTypecast( this );
  }
}

public static class Binary extends Expr
{
  private final Expr m_left, m_right;

  public Binary ( ISourceRange rng, TreeCode code, Qual qual, Expr left, Expr right )
  {
    super( rng, code, qual );
    m_left = left;
    m_right = right;
  }

  @Override
  public final boolean visit ( ExprVisitor v )
  {
    return v.visitBinary( this );
  }

  @Override
  public final int getNumChildren ()
  {
    return 2;
  }

  @Override
  public final Expr getChild ( int i )
  {
    return i == 0 ? m_left : (i == 1 ? m_right : invalidChild( i ));
  }

  @Override
  public final String formatDetails ()
  {
    return "";
  }

  public final Expr getLeft ()
  {
    return m_left;
  }

  public final Expr getRight ()
  {
    return m_right;
  }
}

public static interface ExprVisitor
{
  public boolean visitError ( Error e );
  public boolean visitVarRef ( VarRef e );
  public boolean visitArithConstant ( ArithConstant e );
  public boolean visitEnumConst ( EnumConst e );
  public boolean visitAttrOfType ( AttrOfType e );
  public boolean visitAttrOfExpr ( AttrOfExpr e );
  public boolean visitStringLiteral ( StringLiteral e );
  public boolean visitCall ( Call e );
  public boolean visitSelectMember ( SelectMember e );
  public boolean visitUnary ( Unary e );
  public boolean visitTypecast ( Typecast e );
  public boolean visitBinary ( Binary e );
}

public static boolean visitAllPre ( TExpr.Expr ex, final ExprVisitor v )
{
  if (!ex.visit( v ))
    return false;
  for ( int i = 0, c = ex.getNumChildren(); i < c; ++i )
    if (!visitAllPre( ex.getChild( i ), v ))
      return false;
  return true;
}

public static boolean visitAllPost ( TExpr.Expr ex, final ExprVisitor v )
{
  for ( int i = 0, c = ex.getNumChildren(); i < c; ++i )
    if (!visitAllPre( ex.getChild( i ), v ))
      return false;
  return ex.visit( v );
}

}
