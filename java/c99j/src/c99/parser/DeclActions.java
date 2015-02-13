package c99.parser;

import java.util.Collection;

import c99.*;
import c99.Types.*;
import c99.Types;
import c99.parser.ast.Ast;
import c99.parser.tree.*;

import static c99.parser.Trees.*;

public class DeclActions extends AstActions
{
private Scope m_topScope;

private SimpleSpec m_specs[];

private Spec stdSpec ( TypeSpec ts )
{
  return m_specs[ts.ordinal() - TypeSpec.VOID.ordinal()];
}

private static final SimpleSpec s_errorSpec = new SimpleSpec( TypeSpec.ERROR, -1, 0 );
private static final Qual s_errorQual = new Qual(s_errorSpec);

protected void init ( CompilerOptions opts, IErrorReporter reporter, SymTable symTab )
{
  super.init( opts, reporter, symTab );

  // Initialize the basic type specs
  m_specs = new SimpleSpec[TypeSpec.LDOUBLE.ordinal() - TypeSpec.VOID.ordinal() + 1];
  for ( int i = TypeSpec.VOID.ordinal(); i <= TypeSpec.LDOUBLE.ordinal(); ++i )
  {
    final TypeSpec type = TypeSpec.values()[i];
    int size = -1, align = 0;
    if (type.sizeOf > 0)
    {
      size = type.sizeOf;
      align = Platform.alignment( m_opts, size );
    }
    m_specs[i - TypeSpec.VOID.ordinal()] = new SimpleSpec( type, size, align );
  }
}

public final Object FIXME ( String msg )
{
  return null;
}

public final Object FIXME ()
{
  return null;
}

public Scope topScope ()
{
  return m_topScope;
}

public Scope popScope ( Scope scope )
{
  assert m_topScope == scope;
  m_topScope.pop();
  m_topScope = m_topScope.getParent();
  return scope;
}

public Scope pushScope ( Scope.Kind kind )
{
  return m_topScope = new Scope( kind, m_topScope );
}

public final String stringLiteralString ( CParser.Location loc, TStringLiteral lit )
{
  return Utils.asciiString(lit.value);
}

public final TExtAttr extAttr (
  CParser.Location locAll, CParser.Location locName, String name, TreeList params
)
{
  ExtAttrDef def;
  if ((def = Platform.findExtAttr(name)) == null)
  {
    error( locName, "unknown attribute '%s'", name );
    return null;
  }
  SourceRange rngAll = BisonLexer.fromLocation(locAll);
  ExtAttr extAttr = Platform.parseExtAttr(
    m_compEnv, rngAll, BisonLexer.fromLocation(locName), def, params
  );
  if (extAttr == null)
    return null;
  return new TExtAttr(rngAll, extAttr);
}

public final TExtAttrList extAttrList ( CParser.Location loc, TExtAttrList list, TExtAttr attr )
{
  if (attr != null)
  {
    if (list == null)
      list = new TExtAttrList();
    list.add( attr );
  }
  return list;
}

public final TSpecNode specExtAttr ( CParser.Location loc, TExtAttrList attrList )
{
  if (attrList != null && attrList.size() > 0)
    return BisonLexer.setLocation(new TSpecAttrNode(null, attrList), loc);
  else
    return null;
}

public final TSpecNode spec ( CParser.Location loc, Code code )
{
  return BisonLexer.setLocation(new TSpecNode( null, code ), loc);
}

public final TSpecNode specTypename ( CParser.Location loc, Decl decl )
{
  return BisonLexer.setLocation( new TSpecDeclNode( null, Code.TYPENAME, decl ), loc );
}

private final Spec referenceAgg ( TSpecAggNode node )
{
  final Decl tagDecl;
  final TypeSpec tagSpec = node.code == Code.STRUCT ? TypeSpec.STRUCT : TypeSpec.UNION;

  assert node.identTree != null;
  final Symbol ident = node.identTree.ident;
  if (ident.topTag != null)
  {
    if (ident.topTag.type.spec.type == tagSpec)
    {
      tagDecl = ident.topTag; // Return the existing tag
    }
    else
    {
      error( node.identTree, "'%s %s' previously defined as a different kind of tag here: %s",
             node.code.str, ident.name, SourceRange.formatRange( ident.topTag ) );

      // Error recovery: return an anonymous tag
      Spec spec = new StructUnionSpec( tagSpec, null );
      tagDecl = new Decl( node, Decl.Kind.TAG, m_topScope, SClass.NONE, Linkage.NONE,
                          null, new Qual( spec ), false, true );
    }
  }
  else
  {
    // Forward declaration of tag
    Spec spec = new StructUnionSpec( tagSpec, ident );
    tagDecl = new Decl( node.identTree, Decl.Kind.TAG, m_topScope, SClass.NONE, Linkage.NONE,
                        ident, new Qual( spec ), false, false );
    m_topScope.pushTag( tagDecl );
    if (m_topScope.kind == Scope.Kind.PARAM)
      warning( tagDecl, "declaration of '%s' will not be visible outside of the function", spec.readableType() );
  }

  return tagDecl.type.spec;
}

private final Spec declareAgg ( TSpecAggNode node )
{
  if (node.memberScope == null)
    return referenceAgg( node );

  final TypeSpec tagSpec = node.code == Code.STRUCT ? TypeSpec.STRUCT : TypeSpec.UNION;

  Decl tagDecl = null;
  Symbol ident = node.identTree.ident;
  boolean haveErr = node.memberScope.error;

  // Check for redefinition: it must have been defined in the current scope
  if (ident != null && ident.topTag != null && ident.topTag.scope == m_topScope)
  {
    if (ident.topTag.type.spec.type == tagSpec)
    {
      final StructUnionSpec prevSpec = (StructUnionSpec)ident.topTag.type.spec;

      if (prevSpec.isComplete()) // Already defined?
      {
        error( node.identTree, "redefinition of '%s %s'. originally defined here: %s",
               node.code.str, ident.name, SourceRange.formatRange( ident.topTag ) );

        // Error recovery: make the aggregate anonymous
        ident = null;
        haveErr = true;
      }
      else
        tagDecl = ident.topTag; // We will complete the existing forward declaration
    }
    else
    {
      error( node.identTree, "'%s %s' previously defined as a different kind of tag here: %s",
             node.code.str, ident.name, SourceRange.formatRange( ident.topTag ) );

      // Error recovery: make the aggregate anonymous
      ident = null;
      haveErr = true;
    }
  }

  final SourceRange rng = node.identTree != null ? node.identTree : node;

  if (tagDecl == null) // If not completing a previous forward declaration
  {
    Spec spec = new StructUnionSpec( tagSpec, ident );
    tagDecl = new Decl( node, Decl.Kind.TAG, m_topScope, SClass.NONE, Linkage.NONE, ident,
                        new Qual( spec ), true, haveErr );
    m_topScope.pushTag( tagDecl );
    if (m_topScope.kind == Scope.Kind.PARAM)
      warning( rng, "declaration of '%s' will not be visible outside of the function", spec.readableType() );
  }

  tagDecl.defined = true;

  // Update the location to this one in all cases
  tagDecl.setRange( rng );

  final StructUnionSpec spec = (StructUnionSpec)tagDecl.type.spec;
  final Collection<Decl> decls = node.memberScope.decls();

  assert !spec.isComplete();
  Member[] fields = new Member[decls.size()];

  int i = 0;
  for ( Decl d : decls )
  {
    fields[i++] = new Member( d, d.symbol, d.type );
    haveErr |= d.error;
  }

  spec.orError( haveErr );
  spec.setFields( fields );
  calcAggSize( spec );

  return spec;
}

/**
 * Calculathe struct/union size and alignment and assign field offsets
 * @param spec
 */
private final void calcAggSize ( StructUnionSpec spec )
{
  assert spec.isComplete();

  if (spec.isError())
    return;

  int size = 0;
  int align = 1;

  if (spec.type == TypeSpec.STRUCT)
  {
    for ( Member field : spec.getFields() )
    {
      int falign = field.type.alignOf();
      align = Math.max( falign, align );
      size = (size + falign-1) & ~(falign-1);
      field.offset = size;
      size += field.type.spec.sizeOf();
    }
  }
  else
  {
    assert spec.type == TypeSpec.UNION;
    for ( Member field : spec.getFields() )
    {
      field.offset = 0;
      align = Math.max( field.type.alignOf(), align );
      size = Math.max( field.type.spec.sizeOf(), size );
    }
  }

  size = (size + align-1) & ~(align-1);
  spec.setSizeAlign( size, align );
}

public final TSpecNode specAgg (
  CParser.Location loc, Code tagCode,
  CParser.Location identLoc, Symbol ident, Scope memberScope
)
{
  return BisonLexer.setLocation(
    new TSpecAggNode(
      null, tagCode,
      ident != null ? BisonLexer.setLocation(new TIdent(null, ident), identLoc) : null,
      memberScope
    ),
    loc
  );
}


public final TSpecNode appendSpecNode ( TSpecNode a, TSpecNode b )
{
  if (a == null)
    return b;
  if (b == null)
    return a;

  TSpecNode t = a;
  while (t.next != null)
    t = t.next;
  t.next = b;
  return a;
}


private final class TypeHelper
{
  final SourceRange loc = new SourceRange();
  boolean haveErr = false;

  TSpecNode thread = null;
  TSpecNode inline = null;
  TSpecNode noreturn = null;

  TSpecNode complex = null;
  TSpecNode sc = null;
  int len = 0; String lenStr = null; TSpecNode lenSpec = null;
  ExtAttributes scAttrs;
  TSpecNode base = null;
  TSpecNode signed = null;
  ExtAttributes specAttrs;

  TSpecNode _const = null;
  TSpecNode _restrict = null;
  TSpecNode _volatile = null;
  TSpecNode _atomicQual = null;
  ExtAttributes qualAttrs;

  TypeHelper ( ISourceRange loc )
  {
    this.loc.setRange( loc );
  }

  void err ( ISourceRange rng, String a, String b )
  {
    haveErr = true;
    if (a.equals( b ))
      error( rng, "More than one '%s' specified", a );
    else
      error( rng, "Both '%s' and '%s' specified", a, b );
  }

  String specStr ( TSpecNode spec )
  {
    switch (spec.code)
    {
    case TYPENAME:
      return ((TSpecDeclNode)spec).decl.symbol.name;
    case STRUCT: case UNION:
      {
        TSpecAggNode n = (TSpecAggNode)spec;
        return n.identTree != null ? spec.code.str + " " + n.identTree.ident.name : spec.code.str;
      }
    case ENUM:
      assert false; // FIXME
      return spec.code.str + " " + ((TSpecDeclNode)spec).decl.symbol.name;
    default: return spec.code.str;
    }
  }

  TSpecNode set ( TSpecNode state, TSpecNode spec )
  {
    if (state == null)
      return spec;
    else
    {
      err( spec, specStr(spec), specStr(state) );
      return state;
    }
  }

  void accumulate ( TSpecNode specNode )
  {
    for ( ; specNode != null; specNode = specNode.next )
    {
      switch (specNode.code)
      {
      case INLINE:      if (inline == null) inline = specNode; break;
      case _NORETURN:   if (noreturn == null) noreturn = specNode; break;

      case CONST:       if (_const == null) _const = specNode; break;
      case RESTRICT:    if (_restrict == null) _restrict = specNode; break;
      case VOLATILE:    if (_volatile == null) _volatile = specNode; break;
      case _ATOMIC:     if (_atomicQual == null) _atomicQual = specNode; break; // FIXME: TODO

      case _THREAD_LOCAL:             thread = set( thread, specNode ); break;
      case _COMPLEX: case _IMAGINARY: complex = set( complex, specNode ); break;
      case SIGNED: case UNSIGNED:     signed = set( signed, specNode ); break;
      case TYPEDEF: case EXTERN: case STATIC: case AUTO: case REGISTER:
        sc = set( sc, specNode ); break;
      case _BOOL: case CHAR: case INT: case VOID: case FLOAT: case DOUBLE: case TYPENAME:
      case STRUCT: case UNION: case ENUM:
        base = set( base, specNode ); break;

      case GCC_ATTRIBUTE:
        {
          TSpecAttrNode an = (TSpecAttrNode) specNode;
          for ( TExtAttr ea : an.attrList )
          {
            switch (ea.extAttr.def.disposition)
            {
            case SCLASS:
              if (scAttrs == null) scAttrs = new ExtAttributes(); scAttrs.add( ea.extAttr ); break;
            case QUAL:
              if (qualAttrs == null) qualAttrs = new ExtAttributes(); qualAttrs.add( ea.extAttr ); break;
            case SPEC:
              if (specAttrs == null) specAttrs = new ExtAttributes(); specAttrs.add( ea.extAttr ); break;
            default: assert false; break;
            }
          }
        }
        break;

      case SHORT:
        if (len == 0)
        {
          len = -1;
          lenSpec = specNode;
          lenStr = specNode.code.str;
        }
        else
          err( specNode, specNode.code.str, lenStr );
        break;
      case LONG:
        if (len == 0)
        {
          len = 1;
          lenSpec = specNode;
          lenStr = specNode.code.str;
        }
        else if (len == 1)
        {
          len = 2;
          lenSpec = specNode;
          lenStr = "long long";
        }
        else
          err( specNode, specNode.code.str, lenStr );
        break;
      }
    }
  }

  void deduceBase ()
  {
    if (base == null)
    {
      if (signed != null || lenSpec != null)
        base = spec( BisonLexer.toLocation( signed != null ? signed : lenSpec ), Code.INT );
      else if (complex != null)
      {
        base = spec( BisonLexer.toLocation( complex ), Code.DOUBLE );
        warning( complex, "implicit '%s' assumed with '%s'", specStr(base), specStr(complex) );
      }
      else
      {
        base = new TSpecNode( this.loc, Code.INT );
        warning( loc, "implicit '%s' assumed", specStr(base) );
      }
    }
    assert base != null;
  }

  void checkSignAndLength ()
  {
    assert base != null;
    switch (base.code)
    {
    case _BOOL: case VOID: case FLOAT: case DOUBLE: case ENUM: case STRUCT: case UNION: case TYPENAME:
      if (signed != null)
      {
        err( signed, specStr(signed), specStr(base) );
        signed = null;
      }
      break;

    case CHAR:
      if (signed == null)
        signed = spec( BisonLexer.toLocation( base ), m_opts.signedChar ? Code.SIGNED : Code.UNSIGNED);
      break;
    case INT:
      if (signed == null)
        signed = spec( BisonLexer.toLocation( base ), Code.SIGNED );
      break;
    }

    switch (base.code)
    {
    case _BOOL: case VOID: case CHAR: case FLOAT: case DOUBLE: case TYPENAME:
    case ENUM: case STRUCT: case UNION:
      if (len != 0 &&
          (base.code != Code.DOUBLE || len != 1) /* exclude 'long double' */)
      {
        err( lenSpec, lenStr, specStr(base) );
        len = 0;
        lenSpec = null;
        lenStr = null;
      }
      break;
    }

    if (complex != null)
    {
      switch (base.code)
      {
      case VOID: case TYPENAME: case ENUM: case STRUCT: case UNION:
        err( complex, specStr(complex), specStr(base) );
        complex = null;
        break;
      }
    }
  }

  Spec mkSpec ()
  {
    final Spec spec;
    switch (base.code)
    {
    case _BOOL: spec = stdSpec(TypeSpec.BOOL); break;
    case VOID: spec = stdSpec(TypeSpec.VOID); break;

    case CHAR:
      spec = stdSpec(signed != null && signed.code == Code.SIGNED ? TypeSpec.SCHAR : TypeSpec.UCHAR);
      break;

    case INT:
      {
        final TypeSpec us[] = new TypeSpec[]{TypeSpec.USHORT, TypeSpec.UINT, TypeSpec.ULONG, TypeSpec.ULLONG};
        final TypeSpec s[] = new TypeSpec[]{TypeSpec.SSHORT, TypeSpec.SINT, TypeSpec.SLONG, TypeSpec.SLLONG};
        spec = stdSpec(signed != null && signed.code == Code.UNSIGNED ? us[len+1] : s[len+1]);
      }
      break;

    case FLOAT: spec = stdSpec(TypeSpec.FLOAT); break;
    case DOUBLE: spec = stdSpec(len != 1 ? TypeSpec.DOUBLE : TypeSpec.LDOUBLE); break;

    case TYPENAME:
      spec = ((TSpecDeclNode)base).decl.type.spec;
      break;

    case STRUCT: case UNION:
      spec = declareAgg( (TSpecAggNode)base );
      break;

    case ENUM:
      assert( false );
      spec = null;
      FIXME("implement enum");
      break;

    default: spec = null; break;
    }

    if (complex != null)
      return new BasedSpec( complex.code == Code._COMPLEX ? TypeSpec.COMPLEX : TypeSpec.IMAGINARY, spec );
    else
      return spec;
  }

  Qual mkQual ( Spec spec )
  {
    assert spec != null;

    final Qual q = new Qual(spec);
    q.isConst = _const != null;
    q.isVolatile = _volatile != null;
    q.isRestrict = _restrict != null;
    q.isAtomic = _atomicQual != null;
    q.extAttrs.transferFrom( qualAttrs );

    // Combine the qualifiers of the typedef
    if (base != null && base.code == Code.TYPENAME)
      q.combine( ((TSpecDeclNode)base).decl.type );

    Platform.setDefaultAttrs( m_compEnv, loc, q );

    return q;
  }

  SClass mkSClass ()
  {
    switch (sc != null ? sc.code : Code.ELLIPSIS/*anything*/)
    {
    case TYPEDEF: return SClass.TYPEDEF;
    case EXTERN:  return SClass.EXTERN;
    case STATIC:  return SClass.STATIC;
    case AUTO:    return SClass.AUTO;
    case REGISTER: return SClass.REGISTER;
    case ELLIPSIS: return SClass.NONE;
    default: assert false; return null;
    }
  }

  TDeclSpec mkDeclSpec ( SClass sclass, Qual qual )
  {
    TDeclSpec ds = new TDeclSpec( sclass, scAttrs, qual );
    ds.scNode = sc;
    ds.thread = thread;
    ds.inline = inline;
    ds.noreturn = noreturn;
    ds.error = haveErr;

    return ds;
  }
}

public final TDeclarator declarator ( CParser.Location loc, Symbol ident )
{
  return BisonLexer.setLocation( new TDeclarator( null, ident ), loc );
}

public final TDeclarator abstractDeclarator ( CParser.Location loc )
{
  // create a position instead of a location
  return declarator( new CParser.Location( loc.begin ), null );
}

public final TDeclarator.Elem pointerDecl ( CParser.Location loc, TSpecNode qualList, TDeclarator.Elem to )
{
  return new TDeclarator.PointerElem( loc, qualList ).append( to );
}

public final TDeclarator.Elem arrayDecl (
  CParser.Location loc,
  TSpecNode qualList, CParser.Location _static, CParser.Location asterisk, Ast size
)
{
  // FIXME: size
  return new TDeclarator.ArrayElem( loc, qualList, _static, asterisk );
}

public final TIdentList identList ()
{
  return new TIdentList();
}

public final TIdentList identListAdd (
  CParser.Location loc, TIdentList list, Symbol sym
)
{
  Member m;
  if ( (m = list.get( sym )) == null)
  {
    m = new Member( null, sym, null );
    BisonLexer.setLocation( m, loc );
    list.put( sym, m );
  }
  else
    error( loc, "parameter '%s' already declared here: %s", sym.name, SourceRange.formatRange(m) );
  return list;
}

public final TDeclarator.Elem funcDecl ( CParser.Location loc, TDeclList paramList )
{
  return new TDeclarator.FuncElem( loc, paramList, null );
}

public final TDeclarator.Elem oldFuncDecl ( CParser.Location loc, TIdentList identList )
{
  return new TDeclarator.FuncElem( loc, null, identList );
}

public final TDeclList declList ( TDeclList list, TDeclaration di )
{
  if (list == null)
    list = new TDeclList();
  list.add( di );
  return list;
}

public final TInitDeclaratorList initDeclaratorList ( TInitDeclaratorList list, TInitDeclarator idecl )
{
  if (list == null)
    list = new TInitDeclaratorList();
  list.add( idecl );
  return list;
}

private final PointerSpec newPointerSpec ( Qual to )
{
  int size = Platform.pointerSize( to );
  int align = Platform.alignment( m_opts, size );
  return new PointerSpec( to, size, align );
}

private Qual adjustParamType ( Qual qual )
{
  if (qual.spec.type == TypeSpec.FUNCTION)
  {
    // function => pointer to function
    return new Qual(newPointerSpec(qual) );
  }
  else if (qual.spec.type == TypeSpec.ARRAY)
  {
    // array => pointer to element

    ArraySpec arraySpec = (ArraySpec)qual.spec;
    PointerSpec ptrSpec = newPointerSpec( arraySpec.of );
    if (arraySpec._static)
      ptrSpec.staticSize = arraySpec.nelem;
    Qual q = new Qual( ptrSpec );
    q.combine( qual ); // Keep the C99 array qualifiers

    return q;
  }

  return qual;
}

private static boolean compareDeclTypes ( Qual a, Qual b )
{
    // With array types, one of them or both may have an empty first dimension
  if (a.spec.type == TypeSpec.ARRAY)
  {
    if (b.spec.type != TypeSpec.ARRAY)
      return false;

    ArraySpec sa = (ArraySpec)a.spec;
    ArraySpec sb = (ArraySpec)b.spec;
    if (sa.nelem >= 0 && sb.nelem >= 0 && sa.nelem != sb.nelem)
      return false;

    return sa.of.same( sb.of );
  }

  return a.same( b );
}

/** Is the type an array which is complete other than the dimension */
private static boolean isArrayMostlyComplete ( Qual q )
{
  if (q.spec.type == TypeSpec.ARRAY)
  {
    ArraySpec s = (ArraySpec)q.spec;
    if (s.nelem < 0 && s.of.spec.isComplete())
      return true;
  }
  return false;
}

private static boolean isFunc ( Qual q )
{
  return q.spec.type == TypeSpec.FUNCTION;
}
private static boolean isArray ( Qual q )
{
  return q.spec.type == TypeSpec.ARRAY;
}

public final TDeclaration declaration ( TDeclarator dr, TSpecNode dsNode )
{
  return new TDeclaration( dr, dsNode, dr );
}

public final TDeclaration mkTypeName ( TDeclarator dr, TSpecNode dsNode )
{
  return declaration( dr, dsNode );
}

private final class TypeChecker implements TDeclarator.Visitor
{
  boolean haveError;
  Qual qual;

  TypeChecker ( Qual qual )
  {
    this.qual = qual;
  }

  private boolean checkDepth ( int depth, TDeclarator.Elem elem )
  {
    if (depth <= CompilerLimits.MAX_TYPE_DEPTH)
      return true;

    error( elem, "Type is too complex" );
    haveError = true;
    return false;
  }

  @Override public boolean pointer ( int depth, TDeclarator.PointerElem elem )
  {
    if (!checkDepth( depth, elem ))
      return false;

    final TypeHelper th = new TypeHelper( elem );
    th.accumulate( elem.qualList );
    this.qual = th.mkQual( newPointerSpec( this.qual ) );

    return true;
  }

  @Override public boolean array ( int depth, TDeclarator.ArrayElem elem )
  {
    if (!checkDepth( depth, elem ))
      return false;

    if (elem.qualList != null && m_topScope.kind != Scope.Kind.PARAM)
    {
      error( elem.qualList, "type qualifiers in non-parameter array declarator" );
      haveError = true;
      elem.qualList = null;
    }

    if (elem._static != null && m_topScope.kind != Scope.Kind.PARAM)
    {
      error( elem._static, "'static' in non-parameter array declarator" );
      haveError = true;
      elem._static = null;
    }

    if (elem.asterisk != null && m_topScope.kind != Scope.Kind.PARAM)
    {
      error( elem.asterisk, "'[*]' in non-parameter array declarator" );
      haveError = true;
      elem.asterisk = null;
    }

    if (this.qual.spec.type == TypeSpec.FUNCTION)
    {
      error( elem, "array of functions" );
      haveError = true;
      this.qual = s_errorQual;
    }
    else if (!qual.spec.isComplete())
    {
      error( elem, "array has incomplete type '%s'", qual.readableType() );
      haveError = true;
      return false;
    }

    ArraySpec spec = new ArraySpec( this.qual );
    spec._static = elem._static != null;
    spec.asterisk = elem.asterisk != null;
    TypeHelper th = new TypeHelper( elem );
    th.accumulate( elem.qualList );
    this.qual = th.mkQual( spec );

    return true;
  }

  @Override public boolean function ( int depth, TDeclarator.FuncElem elem )
  {
    if (!checkDepth( depth, elem ))
      return false;

    if (this.qual.spec.type == TypeSpec.ARRAY)
    {
      error( elem, "function returning an array" );
      this.qual = s_errorQual;
      this.haveError = true;
    }
    else if (this.qual.spec.type == TypeSpec.FUNCTION)
    {
      error( elem, "function returning a function" );
      this.qual = s_errorQual;
      this.haveError = true;
    }

    final FunctionSpec spec = new FunctionSpec( elem.declList == null, qual );
    if (elem.declList != null) // new-style function?
    {
      // FIXME: support "func (void)"
      // FIXME: implement ellipsis
      Scope paramScope = pushScope( Scope.Kind.PARAM );
      try
      {
        for (TDeclaration decl : elem.declList )
          declare( decl, false );
        if (elem.declList.ellipsis)
          FIXME( "implement ellipsis" );
      }
      finally
      {
        popScope( paramScope );
      }
      Collection<Decl> decls = paramScope.decls();
      spec.params = new Member[decls.size()];
      int i = 0;
      for ( Decl d : decls )
        spec.params[i++] = new Member( d, d.symbol, d.type ); // FIXME: coordinates
    }
    else // old-style function
    {
      if (elem.identList == null)
        spec.params = new Member[0];
      else
      {
        spec.params = new Member[elem.identList.size()];
        int i = 0;
        for ( Member m : elem.identList.values() )
          spec.params[i++] = m; // FIXME: coordinates
      }
    }
    this.qual = new Qual( spec );
    return true;
  }
}

private final void validateType ( TDeclaration decl )
{
  final TDeclSpec ds;
  {
    final TypeHelper th = new TypeHelper( decl.dsNode );
    th.accumulate( decl.dsNode );
    th.deduceBase();
    th.checkSignAndLength();
    ds = th.mkDeclSpec( th.mkSClass(), th.mkQual( th.mkSpec() ) );
  }

  decl.ds = ds;

  TypeChecker checker = new TypeChecker( ds.qual );
  if (decl.declarator != null)
  {
    if (decl.declarator.visitPost( checker ) && !checker.haveError)
      decl.type = checker.qual;
    else
    {
      decl.type = s_errorQual;
      decl.error = true;
    }
  }
  else
  {
    decl.type = ds.qual;
  }
}

/**
 * Sets {@link TDeclaration#sclass}, {@link TDeclaration#linkage}, {@link TDeclaration#defined}
 * @param di
 * @param hasInit
 */
private final void validateLinkage ( final TDeclaration di, final boolean hasInit )
{
  final TDeclSpec ds = di.ds;
  di.sclass = ds.sc;

  switch (m_topScope.kind)
  {
  case FILE:
    if (di.sclass == SClass.NONE && isFunc(di.type))
      di.sclass = SClass.EXTERN;
    else if (di.sclass == SClass.REGISTER || di.sclass == SClass.AUTO)
    {
      error( ds.scNode, "'%s' storage class at file scope", ds.scNode.code.str );
      di.error = true;
      ds.error = true;
      di.sclass = ds.sc = SClass.NONE;
    }

    if (hasInit && di.sclass == SClass.EXTERN && !isFunc(di.type))
    {
      warning( di, "'%s': ignoring 'extern' in initialization", di.getIdent() );
      di.sclass = SClass.NONE;
    }

    switch (di.sclass)
    {
    case EXTERN: // only in case of isFunc()
    case NONE:
      di.linkage = Linkage.EXTERNAL;
      di.defined = hasInit;
      break;
    case STATIC:
      di.linkage = Linkage.INTERNAL;
      di.defined = hasInit;
      break;
    case TYPEDEF:
      di.linkage = Linkage.NONE;
      di.defined = true;
      break;
    default: assert false; di.defined = false; break;
    }
    break;

  case BLOCK:
    if (di.sclass == SClass.NONE && isFunc(di.type))
      di.sclass = SClass.EXTERN;

    if (hasInit && di.sclass == SClass.EXTERN && !isFunc(di.type))
    {
      error( di, "'%s': 'extern' and initialization", di.getIdent() );
      di.sclass = SClass.NONE; // Just pretend it is a new declaration for error recovery
      di.error = true;
    }

    di.linkage = di.sclass == SClass.EXTERN ? Linkage.EXTERNAL : Linkage.NONE;
    di.defined = di.sclass != SClass.EXTERN;
    break;

  case PARAM:
    assert !hasInit;
    di.type = adjustParamType( di.type );
    if (di.sclass == SClass.REGISTER)
    {
      warning( ds.scNode, "'%s' storage class is ignored", ds.scNode.code.str );
      di.sclass = SClass.NONE;
    }
    else if (di.sclass != SClass.NONE)
    {
      error( ds.scNode, "'%s' storage class in function declaration", ds.scNode.code.str );
      di.error = true;
      ds.error = true;
      di.sclass = ds.sc = SClass.NONE;
    }
    di.linkage = Linkage.NONE;
    di.defined = true;
    break;

  case AGGREGATE:
    assert !hasInit;
    if (isFunc(di.type))
    {
      error( di, "field declared as a function in struct/union" );
      di.error = true;
      di.type = adjustParamType( di.type ); // Least painful way of error recovery is to convert to a pointer
    }
    if (di.sclass != SClass.NONE)
    {
      error( ds.scNode, "storage class in struct/union scope" );
      di.error = true;
      ds.error = true;
      di.sclass = ds.sc = SClass.NONE;
    }
    if (!di.type.spec.isComplete())
    {
      error( di, "'%s' has an incomplete type", Utils.defaultIfEmpty(di.getIdent().name, "<unnamed>") );
      di.error = true;
    }
    di.linkage = Linkage.NONE;
    di.defined = true;
    break;

  default:
    assert false;
    di.linkage = null;
    di.defined = false;
    break;
  }
}

public final Decl declare ( TDeclaration di, boolean hasInit )
{
  validateType( di );
  validateLinkage( di, hasInit );

  /*
    Check for re-declaration.
    The only allowed cases of re-declaration:
      - [EXTERNAL] ... [EXTERNAL]
      - [INTERNAL] ... extern [EXTERNAL]
      - [INTERNAL] ... [INTERNAL]
   */
  Decl prevDecl = null;

  // Check for a previous declaration in this scope
  if (di.hasIdent() && di.getIdent().topDecl != null && di.getIdent().topDecl.scope == m_topScope)
    prevDecl = di.getIdent().topDecl;

  // Locate a previous declaration with linkage in any parent scope
  if (prevDecl == null && di.linkage != Linkage.NONE)
  {
    assert di.hasIdent();
    prevDecl = di.hasIdent() ? di.getIdent().topDecl : null;
    while (prevDecl != null && prevDecl.linkage == Linkage.NONE)
      prevDecl = prevDecl.prev;
  }

redeclaration:
  if (prevDecl != null)
  {
    // Get to the top declaration
    Decl impDecl = prevDecl;
    while (impDecl.importedDecl != null)
      impDecl = impDecl.importedDecl;

    if (!compareDeclTypes( impDecl.type, di.type ))
    {
      error( di, "'%s' redeclared differently; previous declaration here: %s",
             di.getIdent().name, SourceRange.formatRange(impDecl) );
      di.error = true;
      break redeclaration;
    }

    if (di.defined && impDecl.defined)
    {
      error( di, "'%s': invalid redefinition; already defined here: %s",
             di.getIdent().name, SourceRange.formatRange(impDecl) );
      di.error = true;
      break redeclaration;
    }

    if (prevDecl.linkage == Linkage.EXTERNAL && di.linkage == Linkage.EXTERNAL)
      {}
    else if (prevDecl.linkage == Linkage.INTERNAL && di.linkage == Linkage.EXTERNAL && di.sclass == SClass.EXTERN)
      {}
    else if (prevDecl.linkage == Linkage.INTERNAL && di.linkage == Linkage.INTERNAL)
      {}
    else
    {
      error( di, "'%s': invalid redeclaration; previously declared here: %s",
             di.getIdent().name, SourceRange.formatRange(prevDecl) );
      di.error = true;
      break redeclaration;
    }

    if (di.defined)
    {
      if (impDecl.sclass == SClass.EXTERN)
        impDecl.sclass = SClass.NONE;
      if (!impDecl.defined)
        impDecl.setRange( di );
      impDecl.defined = true;
    }
    // Complete the array size, if it wasn't provided before
    if (isArray( impDecl.type ) && ((ArraySpec)impDecl.type.spec).nelem < 0)
      ((ArraySpec)impDecl.type.spec).nelem = ((ArraySpec)di.type.spec).nelem;

    if (prevDecl.scope != m_topScope)
    {
      Decl decl = new Decl( di, m_topScope, impDecl, di.error );
      m_topScope.pushDecl( decl );
      return decl;
    }

    return prevDecl;
  }

  if (di.defined && di.sclass == SClass.EXTERN)
    di.sclass = SClass.NONE;

  Decl decl = new Decl(
    di, Decl.Kind.VAR, m_topScope, di.sclass, di.linkage, di.getIdent(), di.type, di.defined, di.error
  );
  if (prevDecl == null) // We could arrive here in case of an incorrect redeclaration
    m_topScope.pushDecl( decl );
  return decl;
}

public final void declaration ( TSpecNode specNode, TInitDeclaratorList ideclList )
{
  if (ideclList != null && ideclList.size() > 0)
  {
    for ( TInitDeclarator idecl : ideclList )
      declare( declaration( idecl.declarator, specNode ), idecl.init );
  }
  else
  {
    validateType( declaration( new TDeclarator( specNode, null ), specNode ) );
  }
}

} // class
