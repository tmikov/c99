package c99.parser;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.StringTokenizer;

import c99.CompilerOptions;
import c99.IErrorReporter;
import c99.ISourceRange;
import c99.SourceRange;
import c99.Types.*;
import c99.Utils;
import c99.parser.ast.Ast;

public class DeclActions extends AstActions
{
private Scope m_topScope;

public static class SpecNode extends SourceRange
{
  public final Code code;
  public SpecNode next;

  public SpecNode ( CParser.Location loc, Code code )
  {
    this.code = code;
    BisonLexer.setLocation( this, loc );
  }
  public SpecNode ( ISourceRange rgn, Code code )
  {
    super(rgn);
    this.code = code;
  }
}

public static final class SpecDeclNode extends SpecNode
{
  public final Decl decl;

  public SpecDeclNode ( CParser.Location loc, Code code, Decl decl )
  {
    super( loc, code );
    this.decl = decl;
  }
  public SpecDeclNode ( ISourceRange rng, Code code, Decl decl )
  {
    super( rng, code );
    this.decl = decl;
  }
}

public static final class DeclSpec
{
  public SClass sc;
  public final Qual qual;
  public SpecNode scNode;
  public SpecNode thread;
  public SpecNode inline;
  public SpecNode noreturn;
  public boolean error;

  public DeclSpec ( final SClass sc, final Qual qual )
  {
    this.sc = sc;
    this.qual = qual;
  }
}


public static final class DeclElem extends SourceRange
{
  public Qual qual;
  public DerivedSpec spec;
  public DeclElem to;

  public DeclElem ( CParser.Location loc, final Qual qual )
  {
    this.qual = qual;
    this.spec = (DerivedSpec)qual.spec;
    assert this.spec.of == null;

    BisonLexer.setLocation( this, loc );
  }

  DeclElem append ( DeclElem next )
  {
    if (next != null)
    {
      assert this.to == null && this.spec.of == null;
      this.to = next;
      this.spec.of = next.qual;
    }
    return this;
  }
}

public static final class Declarator extends SourceRange
{
  public final Symbol ident;
  DeclElem top;
  DeclElem bottom;

  public Declarator ( CParser.Location loc, final Symbol ident )
  {
    this.ident = ident;
    BisonLexer.setLocation( this, loc );
  }

  Declarator append ( DeclElem next )
  {
    if (next != null)
    {
      if (bottom != null)
        bottom.append( next );
      else
        top = next;
      bottom = next;
    }
    return this;
  }

  Qual complete ( Qual declSpec )
  {
    assert declSpec != null;
    if (bottom != null)
    {
      bottom.spec.of = declSpec;
      declSpec = top.qual;

      bottom = top = null; // Mark it as invalid
    }
    return declSpec;
  }
}

/**
 * We need to accumulate parameter declarations because of reduce/reduce conflicts
 * in the grammar otherwise
 */
public static final class DeclInfo extends SourceRange
{
  public final Symbol ident;
  public final Qual   type;
  public final DeclSpec ds;

  public DeclInfo ( ISourceRange rng, Symbol ident, Qual type, DeclSpec ds )
  {
    super(rng);
    this.ident = ident;
    this.type = type;
    this.ds = ds;
  }

  public DeclInfo ( CParser.Location loc, Symbol ident, Qual type, DeclSpec ds )
  {
    this((ISourceRange)null, ident, type, ds );
    BisonLexer.setLocation( this, loc );
  }
}

public static final class DeclList extends LinkedList<DeclInfo>
{
  public boolean ellipsis;

  public DeclList setEllipsis () { this.ellipsis = true; return this; }
}

/** This class needed only as a workaround for a Bison BUG - generics in %type */
public static final class IdentList extends LinkedHashMap<Symbol,Member>
{
}

protected void init ( CompilerOptions opts, IErrorReporter reporter, SymTable symTab )
{
  super.init( opts, reporter, symTab );
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

public final SpecNode spec ( CParser.Location loc, Code code )
{
  return new SpecNode( loc, code );
}

public final SpecNode specTypename ( CParser.Location loc, Decl decl )
{
  return new SpecDeclNode( loc, Code.TYPENAME, decl );
}

public final SpecNode declareAgg (
  CParser.Location loc, Code tagCode,
  CParser.Location identLoc, Symbol ident,
  Scope memberScope
)
{
  assert memberScope != null;
  final TypeSpec tagSpec = tagCode == Code.STRUCT ? TypeSpec.STRUCT : TypeSpec.UNION;

  Decl tagDecl = null;
  boolean haveErr = memberScope.error;

  // Check for redefinition: it must have been defined in the current scope
  if (ident != null && ident.topTag != null && ident.topTag.scope == m_topScope)
  {
    if (ident.topTag.type.spec.type == tagSpec)
    {
      final StructUnionSpec prevSpec = (StructUnionSpec)ident.topTag.type.spec;

      if (prevSpec.fields != null) // Already defined?
      {
        error( identLoc, "redefinition of '%s %s'. originally defined here: %s",
               tagCode.str, ident.name, SourceRange.formatRange( ident.topTag ) );

        // Error recovery: make the aggregate anonymous
        ident = null;
        haveErr = true;
      }
      else
        tagDecl = ident.topTag; // We will complete the existing forward declaration
    }
    else
    {
      error( identLoc, "'%s %s' previously defined as a different kind of tag here: %s",
             tagCode.str, ident.name, SourceRange.formatRange( ident.topTag ) );

      // Error recovery: make the aggregate anonymous
      ident = null;
      haveErr = true;
    }
  }

  if (tagDecl == null) // If not completing a previous forward declaration
  {
    Spec spec = new StructUnionSpec( tagSpec, ident );
    tagDecl = new Decl( null, Decl.Kind.TAG, m_topScope, SClass.NONE, Linkage.NONE, ident,
                        new Qual( spec ), true, haveErr );
    m_topScope.pushTag( tagDecl );
  }

  tagDecl.defined = true;

  // Update the location to this one in all cases
  BisonLexer.setLocation( tagDecl, identLoc != null ? identLoc : loc );

  final StructUnionSpec spec = (StructUnionSpec)tagDecl.type.spec;
  final Collection<Decl> decls = memberScope.decls();

  assert spec.fields == null;
  spec.error |= haveErr;
  spec.fields = new Member[decls.size()];

  int i = 0;
  for ( Decl d : decls )
    spec.fields[i++] = new Member( d, d.symbol, d.type );

  return new SpecDeclNode( tagDecl, tagCode, tagDecl );
}

public final SpecNode specAgg (
  CParser.Location loc, Code tagCode,
  CParser.Location identLoc, Symbol ident
)
{
  assert ident != null;
  final TypeSpec tagSpec = tagCode == Code.STRUCT ? TypeSpec.STRUCT : TypeSpec.UNION;

  final Decl tagDecl;
  if (ident.topTag != null)
  {
    if (ident.topTag.type.spec.type == tagSpec)
    {
      tagDecl = ident.topTag; // Return the existing tag
    }
    else
    {
      error( loc, "'%s %s' previously defined as a different kind of tag here: %s",
             tagCode.str, ident.name, SourceRange.formatRange( ident.topTag ) );

      // Error recovery: return an anonymous tag
      Spec spec = new StructUnionSpec( tagSpec, null );
      tagDecl = new Decl( null, Decl.Kind.TAG, m_topScope, SClass.NONE, Linkage.NONE,
                          null, new Qual( spec ), false, true );
      BisonLexer.setLocation( tagDecl, loc );
    }
  }
  else
  {
    // Forward declaration of tag
    Spec spec = new StructUnionSpec( tagSpec, ident );
    tagDecl = new Decl( null, Decl.Kind.TAG, m_topScope, SClass.NONE, Linkage.NONE,
                        ident, new Qual( spec ), false, false );
    BisonLexer.setLocation( tagDecl, identLoc );
    m_topScope.pushTag( tagDecl );
  }

  return new SpecDeclNode( tagDecl, tagCode, tagDecl );
}


public final SpecNode spec ( Ast ast )
{
  assert false; // FIXME
  return null;
}

public final SpecNode append ( SpecNode a, SpecNode b )
{
  SpecNode t = a;
  while (t.next != null)
    t = t.next;
  t.next = b;
  return a;
}

private static final SimpleSpec s_specs[];
static {
  s_specs = new SimpleSpec[TypeSpec.LDOUBLE.ordinal() - TypeSpec.VOID.ordinal() + 1];
  for ( int i = TypeSpec.VOID.ordinal(); i <= TypeSpec.LDOUBLE.ordinal(); ++i )
    s_specs[i - TypeSpec.VOID.ordinal()] = new SimpleSpec(TypeSpec.values()[i]);
}

private static Spec stdSpec ( TypeSpec ts )
{
  return s_specs[ts.ordinal() - TypeSpec.VOID.ordinal()];
}

private final class TypeHelper
{
  boolean haveErr = false;

  SpecNode thread = null;
  SpecNode inline = null;
  SpecNode noreturn = null;

  SpecNode complex = null;
  SpecNode sc = null;
  int len = 0; String lenStr = null; SpecNode lenSpec = null;
  SpecNode base = null;
  SpecNode signed = null;

  SpecNode _const = null;
  SpecNode _restrict = null;
  SpecNode _volatile = null;
  SpecNode _atomicQual = null;

  void err ( ISourceRange rng, String a, String b )
  {
    haveErr = true;
    if (a.equals( b ))
      error( rng, "More than one '%s' specified", a );
    else
      error( rng, "Both '%s' and '%s' specified", a, b );
  }

  String specStr ( SpecNode spec )
  {
    switch (spec.code)
    {
    case TYPENAME:
      return ((SpecDeclNode)spec).decl.symbol.name;
    case STRUCT: case UNION:
      return spec.code.str + " " + ((SpecDeclNode)spec).decl.symbol.name;
    case ENUM:
      assert false; // FIXME
      return spec.code.str + " " + ((SpecDeclNode)spec).decl.symbol.name;
    default: return spec.code.str;
    }
  }

  SpecNode set ( SpecNode state, SpecNode spec )
  {
    if (state == null)
      return spec;
    else
    {
      err( spec, specStr(spec), specStr(state) );
      return state;
    }
  }

  void accumulate ( SpecNode specNode )
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

  void deduceBase ( CParser.Location loc )
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
        base = spec( loc, Code.INT );
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

    case TYPENAME: case STRUCT: case UNION: case ENUM:
      spec = ((SpecDeclNode)base).decl.type.spec;
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
    final Qual q = new Qual(spec);
    q.isConst = _const != null;
    q.isVolatile = _volatile != null;
    q.isRestrict = _restrict != null;
    q.isAtomic = _atomicQual != null;

    // Combine the qualifiers of the typedef
    if (base != null && base.code == Code.TYPENAME)
      q.combine( ((SpecDeclNode)base).decl.type );

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

  DeclSpec mkDeclSpec ( SClass sclass, Qual qual )
  {
    DeclSpec ds = new DeclSpec( sclass, qual );
    ds.scNode = sc;
    ds.thread = thread;
    ds.inline = inline;
    ds.noreturn = noreturn;
    ds.error = haveErr;

    return ds;
  }
}

public final DeclSpec declSpec ( CParser.Location loc, SpecNode specNode )
{
  final TypeHelper th = new TypeHelper();

  th.accumulate( specNode );
  th.deduceBase( loc );
  th.checkSignAndLength();

  final Spec spec = th.mkSpec();
  final Qual qual = th.mkQual( spec );
  final SClass sclass = th.mkSClass();
  return th.mkDeclSpec( sclass, qual );
}

public final Declarator declarator ( CParser.Location loc, Symbol ident )
{
  return new Declarator( loc, ident );
}

public final Declarator abstractDeclarator ( CParser.Location loc )
{
  // create a position instead of a location
  return declarator( new CParser.Location( loc.begin ), null );
}

public final DeclElem pointerDecl ( CParser.Location loc, SpecNode qualList, DeclElem to )
{
  final TypeHelper th = new TypeHelper();
  th.accumulate( qualList );
  return new DeclElem( loc, th.mkQual(new PointerSpec()) ).append( to );
}

public final DeclElem arrayDecl (
  CParser.Location loc,
  SpecNode qualList, CParser.Location _static, CParser.Location asterisk, Ast size
)
{
  final TypeHelper th = new TypeHelper();
  if (qualList != null && m_topScope.kind != Scope.Kind.PARAM)
  {
    error( qualList, "type qualifiers in non-parameter array declarator" );
    qualList = null;
  }
  th.accumulate( qualList );

  if (_static != null && m_topScope.kind != Scope.Kind.PARAM)
  {
    error( _static, "'static' in non-parameter array declarator" );
    _static = null;
  }

  if (asterisk != null && m_topScope.kind != Scope.Kind.PARAM)
  {
    error( asterisk, "'[*]' in non-parameter array declarator" );
    asterisk = null;
  }

  ArraySpec spec = new ArraySpec();
  spec._static = _static != null;
  spec.asterisk = asterisk != null;
  // FIXME: size
  return new DeclElem( loc, th.mkQual(spec) );
}

public final IdentList identList ()
{
  return new IdentList();
}

public final IdentList identListAdd (
  CParser.Location loc, IdentList list, Symbol sym
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

public final DeclElem funcDecl ( CParser.Location loc, DeclList paramList )
{
  Scope paramScope = pushScope( Scope.Kind.PARAM );
  try
  {
    for ( DeclInfo di : paramList )
      declare( di, false );
    if (paramList.ellipsis)
      FIXME("implement ellipsis");
  }
  finally
  {
    popScope( paramScope );
  }
  return funcDecl( loc, paramScope );
}

private final DeclElem funcDecl ( CParser.Location loc, Scope paramScope )
{
  final FunctionSpec spec = new FunctionSpec();
  final Collection<Decl> decls = paramScope.decls();

  spec.params = new Member[decls.size()];
  int i = 0;
  for ( Decl d : decls )
    spec.params[i++] = new Member(d, d.symbol, d.type);

  return new DeclElem( loc, new Qual(spec) );
}

public final DeclElem oldFuncDecl ( CParser.Location loc, IdentList identList )
{
  final FunctionSpec spec = new FunctionSpec(true);

  if (identList == null)
    spec.params = new Member[0];
  else
  {
    spec.params = new Member[identList.size()];
    int i = 0;
    for ( Member m : identList.values() )
      spec.params[i++] = m;
  }

  return new DeclElem( loc, new Qual(spec) );
}

public final DeclList declList ( DeclList list, DeclInfo di )
{
  if (list == null)
    list = new DeclList();
  list.add( di );
  return list;
}

public final Qual mkTypeName ( Declarator dr, DeclSpec ds )
{
  return dr.complete( ds.qual );
}

private Qual adjustParamType ( Qual qual )
{
  if (qual.spec.type == TypeSpec.FUNCTION)
  {
    // function => pointer to function
    return new Qual(new PointerSpec(qual));
  }
  else if (qual.spec.type == TypeSpec.ARRAY)
  {
    // array => pointer to element

    ArraySpec arraySpec = (ArraySpec)qual.spec;
    PointerSpec ptrSpec = new PointerSpec( arraySpec.of );
    if (arraySpec._static)
      ptrSpec.staticSize = arraySpec.size;
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
    if (sa.size != null && sb.size != null && !sa.size.equals( sb.size ))
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
    if (s.size == null && s.of.spec.isComplete())
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

public final DeclInfo declInfo ( Declarator dr, DeclSpec ds )
{
  return new DeclInfo( dr, dr.ident, dr.complete( ds.qual ), ds );
}

public final Decl declare ( Declarator dr, DeclSpec ds )
{
  return declare(  dr, ds, false );
}

public final Decl declare ( Declarator dr, DeclSpec ds, boolean hasInit )
{
  return declare( declInfo( dr, ds ), hasInit );
}

public final Decl declare ( DeclInfo di, boolean hasInit )
{
  final DeclSpec ds = di.ds;
  SClass sc = ds.sc;
  boolean haveError = ds.error;
  Qual type = di.type;

  Linkage linkage;
  boolean defined;
  switch (m_topScope.kind)
  {
  case FILE:
    if (sc == SClass.NONE && isFunc(type))
      sc = SClass.EXTERN;
    else if (sc == SClass.REGISTER || sc == SClass.AUTO)
    {
      error( ds.scNode, "'%s' storage class at file scope", ds.scNode.code.str );
      haveError = true;
      ds.error = true;
      sc = ds.sc = SClass.NONE;
    }

    if (hasInit && sc == SClass.EXTERN && !isFunc(type))
    {
      warning( di, "'%s': ignoring 'extern' in initialization", di.ident );
      sc = SClass.NONE;
    }

    linkage = sc == SClass.STATIC ? Linkage.INTERNAL : Linkage.EXTERNAL;
    switch (sc)
    {
    case EXTERN: // only in case of isFunc()
    case NONE:
      linkage = Linkage.EXTERNAL;
      defined = hasInit;
      break;
    case STATIC:
      linkage = Linkage.INTERNAL;
      defined = hasInit;
      break;
    case TYPEDEF:
      linkage = Linkage.NONE;
      defined = true;
      break;
    default: assert false; defined = false; break;
    }
    break;

  case BLOCK:
    if (sc == SClass.NONE && isFunc(type))
      sc = SClass.EXTERN;

    if (hasInit && sc == SClass.EXTERN && !isFunc(type))
    {
      error( di, "'%s': 'extern' and initialization", di.ident );
      sc = SClass.NONE; // Just pretend it is a new declaration for error recovery
      haveError = true;
    }

    linkage = sc == SClass.EXTERN ? Linkage.EXTERNAL : Linkage.NONE;
    defined = sc != SClass.EXTERN;
    break;

  case PARAM:
    assert !hasInit;
    type = adjustParamType( type );
    if (sc == SClass.REGISTER)
    {
      warning( ds.scNode, "'%s' storage class is ignored", ds.scNode.code.str );
      sc = SClass.NONE;
    }
    else if (sc != SClass.NONE)
    {
      error( ds.scNode, "'%s' storage class in function declaration", ds.scNode.code.str );
      haveError = true;
      ds.error = true;
      sc = ds.sc = SClass.NONE;
    }
    linkage = Linkage.NONE;
    defined = true;
    break;

  case AGGREGATE:
    assert !hasInit;
    if (isFunc(type))
    {
      error( di, "field declared as a function in struct/union" );
      type = adjustParamType( type ); // Least painful way of error recovery is to convert to a pointer
    }
    if (sc != SClass.NONE)
    {
      error( ds.scNode, "storage class in struct/union scope" );
      haveError = true;
      ds.error = true;
      sc = ds.sc = SClass.NONE;
    }
    if (!type.spec.isComplete())
    {
      error( di, "'%s' has an incomplete type", Utils.defaultIfEmpty(di.ident.name, "<unnamed>") );
      haveError = true;
    }
    linkage = Linkage.NONE;
    defined = true;
    break;

  default:
    assert false;
    linkage = null;
    defined = false;
    break;
  }

  /*
    Check for re-declaration.
    The only allowed cases of re-declaration:
      - [EXTERNAL] ... [EXTERNAL]
      - [INTERNAL] ... extern [EXTERNAL]
      - [INTERNAL] ... [INTERNAL]
   */
  Decl prevDecl = null;

  // Check for a previous declaration in this scope
  if (di.ident != null && di.ident.topDecl != null && di.ident.topDecl.scope == m_topScope)
    prevDecl = di.ident.topDecl;

  // Locate a previous declaration with linkage in any parent scope
  if (prevDecl == null && linkage != Linkage.NONE)
  {
    assert di.ident != null;
    prevDecl = di.ident != null ? di.ident.topDecl : null;
    while (prevDecl != null && prevDecl.linkage == Linkage.NONE)
      prevDecl = prevDecl.prev;
  }

redeclaration:
  if (prevDecl != null)
  {
    if (!compareDeclTypes( prevDecl.type, type ))
    {
      error( di, "'%s' redeclared differently; previous declaration here: %s",
             di.ident.name, SourceRange.formatRange(prevDecl) );
      haveError = true;
      break redeclaration;
    }

    if (defined && prevDecl.defined)
    {
      error( di, "'%s': invalid redefinition; already defined here: %s",
             di.ident.name, SourceRange.formatRange(prevDecl) );
      haveError = true;
      break redeclaration;
    }

    if (prevDecl.linkage == Linkage.EXTERNAL && linkage == Linkage.EXTERNAL)
      {}
    else if (prevDecl.linkage == Linkage.INTERNAL && linkage == Linkage.EXTERNAL && sc == SClass.EXTERN)
      {}
    else if (prevDecl.linkage == Linkage.INTERNAL && linkage == Linkage.INTERNAL)
      {}
    else
    {
      error( di, "'%s': invalid redeclaration; previously declared here: %s",
             di.ident.name, SourceRange.formatRange(prevDecl) );
      haveError = true;
      break redeclaration;
    }

    if (defined)
    {
      if (prevDecl.sclass == SClass.EXTERN)
        prevDecl.sclass = SClass.NONE;
      if (!prevDecl.defined)
        prevDecl.setRange( di );
      prevDecl.defined = true;
    }
    // Complete the array size, if it wasn't provided before
    if (isArray( prevDecl.type ) && ((ArraySpec)prevDecl.type.spec).size == null)
      ((ArraySpec)prevDecl.type.spec).size = ((ArraySpec)type.spec).size;

    return prevDecl;
  }

  if (defined && sc == SClass.EXTERN)
    sc = SClass.NONE;

  Decl decl = new Decl(
    di, Decl.Kind.VAR, m_topScope, sc, linkage, di.ident, type, defined, haveError
  );
  if (prevDecl == null) // We could arrive here in case of an incorrect redeclaration
    m_topScope.pushDecl( decl );
  return decl;
}

} // class
