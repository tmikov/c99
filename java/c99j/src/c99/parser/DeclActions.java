package c99.parser;

import java.io.PrintWriter;
import java.util.*;

import c99.*;
import c99.Types.*;
import c99.parser.tree.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static c99.parser.Code.TYPENAME;
import static c99.parser.Trees.*;

public class DeclActions extends ExprActions
{
private Scope m_topScope;
private Scope m_translationUnitScope;

protected void init ( CompEnv compEnv, SymTable symTab )
{
  super.init( compEnv, symTab );
}

/**
 * A final commit must not contain references to this
 */
@Deprecated
public final Object FIXMENOW ( String msg )
{
  assert false;
  return null;
}

@Deprecated
public final Object FIXME ( String msg )
{
  assert false;
  return null;
}

@Deprecated
public final Object FIXME ()
{
  assert false;
  return null;
}

@Deprecated
public final Object TODO ( String msg )
{
  return null;
}

public final Scope topScope ()
{
  return m_topScope;
}

public final <T extends Scope> T popScope ( T scope )
{
  assert m_topScope == scope;
  m_topScope.pop();
  m_topScope = m_topScope.getParent();
  return scope;
}

private final <T extends Scope> T pushScope ( T scope )
{
  m_topScope = scope;
  return scope;
}

public final Scope pushFileScope ()
{
  assert m_topScope == null && m_translationUnitScope == null;
  return m_translationUnitScope = m_topScope = new Scope( m_opts, Scope.Kind.FILE, m_topScope );
}
public final Scope pushBlockScope ()
{
  return m_topScope = new Scope( m_opts, Scope.Kind.BLOCK, m_topScope );
}
public final ParamScope pushParamScope ()
{
  return pushScope( new ParamScope( m_opts, m_topScope ) );
}
public final Scope pushAggScope ()
{
  return pushScope( new Scope( m_opts, Scope.Kind.AGGREGATE, m_topScope ) );
}
public final EnumScope pushEnumScope ()
{
  return pushScope( new EnumScope( m_opts, m_topScope ) );
}

private final Scope topNonStructScope ()
{
  Scope res = m_topScope;
  while (res.kind == Scope.Kind.AGGREGATE || res.kind == Scope.Kind.ENUM)
    res = res.getParent();
  return res;
}

public final String stringLiteralString ( ISourceRange loc, TStringLiteral lit )
{
  return lit.value.toJavaString();
}

public final TExtAttr extAttr (
  ISourceRange locAll, ISourceRange locName, String name, TreeList params
)
{
  ExtAttrDef def;
  if ((def = m_plat.findExtAttr(name)) == null)
  {
    error( locName, "unknown attribute '%s'", name );
    return null;
  }
  ISourceRange rngAll = locAll;
  ExtAttr extAttr = m_plat.parseExtAttr(
    rngAll, locName, def, params
  );
  if (extAttr == null)
    return null;
  return new TExtAttr(rngAll, extAttr);
}

public final TExtAttrList extAttrList ( ISourceRange loc, TExtAttrList list, TExtAttr attr )
{
  if (attr != null)
  {
    if (list == null)
      list = new TExtAttrList();
    list.add( attr );
  }
  return list;
}

public final TSpecNode specExtAttr ( ISourceRange loc, TExtAttrList attrList )
{
  if (attrList != null && attrList.size() > 0)
    return new TSpecAttrNode( loc, attrList );
  else
    return null;
}

public final TSpecNode spec ( ISourceRange loc, Code code )
{
  return new TSpecNode( loc, code );
}

public final TSpecNode specTypename ( ISourceRange loc, Decl decl )
{
  return new TSpecDeclNode( loc, TYPENAME, decl );
}

public final TSpecNode referenceAgg (
  final ISourceRange loc, final Code code, final ISourceRange identLoc, final Symbol ident
)
{
  final Decl tagDecl;
  final TypeSpec tagSpec = code == Code.ENUM ? TypeSpec.ENUM : (code == Code.STRUCT ? TypeSpec.STRUCT : TypeSpec.UNION);
  boolean fwdDecl = false;
  final Scope declScope = topNonStructScope(); // a forward decl would go in this scope

  assert ident != null;
  if (ident.topTag != null)
  {
    if (ident.topTag.type.spec.kind == tagSpec)
    {
      tagDecl = ident.topTag; // Return the existing tag
    }
    else
    {
      error( identLoc, "'%s %s' previously defined as a different kind of tag here: %s",
             code.str, ident.name, SourceRange.formatRange( ident.topTag ) );

      // Error recovery: return an anonymous tag
      Spec spec = tagSpec == TypeSpec.ENUM ? new EnumSpec( null ) : new StructUnionSpec( tagSpec, null );
      tagDecl = new Decl(loc,Decl.Kind.TAG,declScope,declScope,SClass.NONE,Linkage.NONE,null,new Qual( spec ),false,true);
      fwdDecl = true;
    }
  }
  else
  {
    // Forward declaration of tag
    fwdDecl = true;
    Spec spec = tagSpec == TypeSpec.ENUM ? new EnumSpec( ident ) : new StructUnionSpec( tagSpec, ident );
    tagDecl = new Decl(identLoc,Decl.Kind.TAG,declScope,declScope,SClass.NONE,Linkage.NONE,ident,new Qual( spec ),false,false);
    declScope.pushTag( tagDecl );
    if (declScope.kind == Scope.Kind.PARAM)
      warning( tagDecl, "declaration of '%s' will not be visible outside of the function", spec.readableType() );
  }

  return new TSpecTagNode( tagDecl, code, (TagSpec) tagDecl.type.spec );
}

public final Decl beginDeclareAgg (
  final ISourceRange loc, final Code code, final ISourceRange identLoc, Symbol ident
)
{
  final TypeSpec tagSpec = code == Code.ENUM ? TypeSpec.ENUM : (code == Code.STRUCT ? TypeSpec.STRUCT : TypeSpec.UNION);
  final Scope declScope = topNonStructScope(); // a forward decl would go in this scope

  Decl tagDecl = null;
  boolean haveErr = false;

  // Check for redefinition: it must have been defined in the current scope
  if (ident != null && ident.topTag != null && ident.topTag.visibilityScope == declScope)
  {
    if (ident.topTag.type.spec.kind == tagSpec)
    {
      final TagSpec prevSpec = (TagSpec)ident.topTag.type.spec;

      if (prevSpec.isComplete()) // Already defined?
      {
        error( identLoc, "redefinition of '%s %s'. originally defined here: %s",
               code.str, ident.name, SourceRange.formatRange( ident.topTag ) );

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
             code.str, ident.name, SourceRange.formatRange( ident.topTag ) );

      // Error recovery: make the aggregate anonymous
      ident = null;
      haveErr = true;
    }
  }

  final ISourceRange aggLoc = identLoc != null ? identLoc : loc;

  if (tagDecl == null) // If not completing a previous forward declaration
  {
    Spec spec = tagSpec == TypeSpec.ENUM ? new EnumSpec( ident ) : new StructUnionSpec( tagSpec, ident );
    tagDecl = new Decl( null, Decl.Kind.TAG, declScope, declScope, SClass.NONE, Linkage.NONE, ident,
                        new Qual( spec ), true, haveErr );
    declScope.pushTag( tagDecl );
    if (declScope.kind == Scope.Kind.PARAM)
      warning( aggLoc, "declaration of '%s' will not be visible outside of the function", spec.readableType() );
  }

  // Update the location to this one in all cases
  tagDecl.setRange( aggLoc );

  tagDecl.orError( haveErr );

  return tagDecl;
}

public final TSpecNode declareAgg ( Code tagCode, Decl tagDecl, Scope memberScope )
{
  assert memberScope != null;

  final TagSpec tagSpec = (TagSpec)tagDecl.type.spec;
  final Collection<Decl> decls = memberScope.decls();

  assert !tagSpec.isComplete();

  if (tagSpec.kind == TypeSpec.ENUM)
  {
    EnumScope enumScope = (EnumScope)memberScope;
    Scope targetScope = topNonStructScope();

    // Find the min and max values so that platform code can optionally select a narrower type for the enum
    Constant.IntC minValue = null, maxValue = null;
    for ( Decl _d : decls )
    {
      if (_d.kind != Decl.Kind.ENUM_CONST)
        continue;
      final EnumConstDecl d = (EnumConstDecl)_d;

      Constant.IntC tmp = (Constant.IntC)Constant.convert( enumScope.baseSpec, d.enumValue );
      if (minValue == null)
        minValue = maxValue = tmp;
      else if (tmp.lt( minValue ))
        minValue = tmp;
      else if (tmp.gt( maxValue ))
        maxValue = tmp;
    }

    if (minValue == null) // Perhaps it could happen during error recovery
      minValue = maxValue = Constant.makeLong( enumScope.baseSpec, 0 );

    SimpleSpec baseSpec = (SimpleSpec)stdSpec( m_plat.determineEnumBaseSpec( enumScope.baseSpec, minValue, maxValue ) );
    EnumSpec enumSpec = (EnumSpec)tagSpec;
    enumSpec.setBaseSpec( baseSpec );
    Qual type = new Qual(enumSpec);

    if (m_opts.debugEnum)
      System.out.format( "defined '%s' based on '%s'\n", type.readableType(), enumSpec.getBaseSpec().readableType() );

    // Declare all enum constants in the parent scope
    for ( Decl _d : decls )
    {
      if (_d.kind != Decl.Kind.ENUM_CONST)
        continue;
      // Not already defined
      if (_d.symbol.topDecl != null && _d.symbol.topDecl.visibilityScope == targetScope)
        continue; // Could happen when doing error recovery
      final EnumConstDecl d = (EnumConstDecl)_d;

      EnumConstDecl decl = new EnumConstDecl(
        d, targetScope,  d.symbol, type, d.isError(), (Constant.IntC) Constant.convert( baseSpec.kind, d.enumValue )
      );

      targetScope.pushDecl( decl );
      if (m_opts.debugEnum)
      {
        System.out.format( " enum const '%s' = %s : %s\n", decl.symbol.name, decl.enumValue.toString(),
                decl.enumValue.spec.str );
      }
    }
  }
  else
  {
    Member[] fields = new Member[decls.size()];

    int i = 0;
    for ( Decl d : decls )
    {
      if (d.kind == Decl.Kind.VAR)
      {
        fields[i] = new Member( d, i, d.symbol, d.type, d.bitfieldWidth );
        ++i;
        tagSpec.orError( d.isError() );
      }
    }
    if (i < fields.length) // Could happen if there were type definitions in that scope
      fields = Arrays.copyOf( fields, i );

    StructUnionSpec aggSpec = (StructUnionSpec)tagSpec;
    calcAggSize( tagDecl, aggSpec, fields );
  }

  tagSpec.orError( tagDecl.isError() );
  tagDecl.orError( tagSpec.isError() );
  tagDecl.defined = true;

  return new TSpecTagNode( tagDecl, tagCode, tagSpec );
}

private final Constant.IntC m_zero = Constant.makeLong( TypeSpec.SINT, 0 );
private final Constant.IntC m_one = Constant.makeLong( TypeSpec.SINT, 1 );

/**
 * Declare an enumeration constant in the "enum" scope. Enum handling is a two-pass process. In the first
 * pass, while we are parsing the enum and it still isn't complete, so we don't know its size, we declare each
 * constant locally in the scope with a type and value determined by its initialization expression. When the
 * enum is complete and the enum scope has been popped, we declare all of the constants with their completed
 * enum type in the surrounding scope.
 *
 * @param identLoc
 * @param ident
 * @param valueLoc
 * @param value
 */
public final void declareEnumConstant (
  ISourceRange identLoc, Symbol ident, ISourceRange valueLoc, TExpr.ArithConstant value
)
{
  EnumScope enumScope = (EnumScope)topScope();

  if (value != null)
  {
    enumScope.orError( value.isError() );
    enumScope.lastValue = (Constant.IntC)value.getValue(); // Note: even error ArithConstant has an integer value
  }
  else
  {
    if (enumScope.lastValue != null)
    {
      TypeSpec spec = TypeRules.usualArithmeticConversions( enumScope.lastValue.spec, m_one.spec );
      Constant.IntC newValue = Constant.newIntConstant( spec );
      newValue.add( Constant.convert(spec, enumScope.lastValue), Constant.convert(spec, m_one) );
      enumScope.lastValue = newValue;
    }
    else
      enumScope.lastValue = m_zero;
  }

  enumScope.baseSpec = TypeRules.usualArithmeticConversions( enumScope.baseSpec, enumScope.lastValue.spec );

  boolean haveError = false;

  if (ident.topDecl != null)
  {
    if (ident.topDecl.visibilityScope == enumScope)
    {
      error( identLoc, "enumerator '%s' already defined here %s", ident.name, SourceRange.formatRange(ident.topDecl) );
      enumScope.orError( true );
      return;
    }
    else if (ident.topDecl.visibilityScope == topNonStructScope())
    {
      error( identLoc, "redefinition of '%s' previously defined here %s", ident.name, SourceRange.formatRange(ident.topDecl) );
      enumScope.orError( true );
      haveError = true;
    }
  }

  // Now we have decision to make. What type to use for the "temporary" in-scope constant?
  // GCC and CLANG use the type of init expression, so that is what we are going to do too
  Qual type = stdQual( enumScope.lastValue.spec );
  EnumConstDecl decl = new EnumConstDecl( identLoc, enumScope, ident, type, haveError, enumScope.lastValue );
  enumScope.pushDecl( decl );
}

private static final class OverflowException extends Exception {
  public OverflowException ( String message ) { super( message ); }
}

private final long sizeAdd ( long size, long inc ) throws OverflowException
{
  assert size >= 0 && inc >= 0;
  long nsize = size + inc;
  if (nsize < size)
    throw new OverflowException( "size integer overflow" );
  if (nsize > TypeSpec.SIZE_T.maxValue)
    throw new OverflowException( "size exceeds size_t range" );
  return nsize;
}

/**
 * Calculathe struct/union size and alignment and assign field offsets
 * @param spec
 */
private final void calcAggSize ( ISourceRange loc, StructUnionSpec spec, final Member[] fields )
{
  if (spec.isError())
    return;

  long size = 0;
  /** bits available in the last consumed byte of 'size' */
  int bitsAvail = 0;
  int align = 1;
  Member curField = null; // We use that for tracking which field threw

  try
  {
    if (spec.kind == TypeSpec.STRUCT)
    {
      for ( Member field : fields )
      {
        curField = field; // record the current field in case we throw an exception

        int falign = field.type.spec.alignOf();
        align = Math.max( falign, align ); // Update the whole struct alignment

        // Offset of the next suitably aligned storage unit
        long noffset = sizeAdd( size, falign - 1 ) & ~((long)falign-1);

        if (!field.isBitField())
        {
          bitsAvail = 0;
          field.setOffset( noffset );
          size = sizeAdd( size, field.type.spec.sizeOf() );
        }
        else
        {
          // Combining a bit-field with previous fields, which may or may not be bit-fields themselves.
          // The rules are subtle. The field must be accessible through a storage unit with the alignment
          // of the bit-field's base type

          int consumedBits; // the number of consumed bits relative to noffset

          // Calculate the number of bits available between the current position and 'noffset'
          final int bits = (int)(noffset - size)*TypeSpec.UCHAR.width + bitsAvail;

          if (field.getBitFieldWidth() <= bits) // Can we pack this field in the 'hole'?
          {
            noffset -= field.type.spec.sizeOf();
            consumedBits = (int)(size - noffset) * TypeSpec.UCHAR.width - bitsAvail;
          }
          else // Allocate a new storage unit
            consumedBits = 0;

          field.setOffset( noffset );
          field.setBitOffset( m_plat.memoryBitOffset( field.type.spec.kind, consumedBits, field.getBitFieldWidth() ) );
          // Note that we handle the special case of "type :0;" here
          consumedBits += field.getBitFieldWidth() != 0 ? field.getBitFieldWidth() : bits;
          size = sizeAdd( noffset, (consumedBits + TypeSpec.UCHAR.width - 1)/TypeSpec.UCHAR.width );
          bitsAvail = (TypeSpec.UCHAR.width - consumedBits % TypeSpec.UCHAR.width) % TypeSpec.UCHAR.width;
        }

        if (m_opts.debugCalcAggSize)
        {
          System.out.format( "[%d]", field.getOffset() );
          if (field.isBitField())
            System.out.format( " bitfield [%d:%d]", field.getBitOffset(), field.getBitFieldWidth() );
          System.out.format( " size %d align %d: field %s:%s",
                  field.type.spec.sizeOf(), field.type.spec.alignOf(),
                  field.name != null ? field.name.name : "<anon>",
                  field.type.readableType() );
          System.out.println();
        }
      }
    }
    else
    {
      assert spec.kind == TypeSpec.UNION;
      for ( Member field : fields )
      {
        field.setOffset( 0 );
        align = Math.max( field.type.spec.alignOf(), align );
        size = Math.max( field.type.spec.sizeOf(), size );
      }
    }

    curField = null;
    size = sizeAdd( size, align - 1 ) & ~(align-1);
  }
  catch (OverflowException e)
  {
    spec.orError( true );
    if (curField != null)
    {
      error( curField, "'%s' field '%s': %s", spec.readableType(), optName(curField.name), e.getMessage() );
    }
    else
      error( loc, "'%s' %s", spec.readableType(), e.getMessage() );
  }
  spec.setFields( fields, size, align );
  if (m_opts.debugCalcAggSize)
  {
    System.out.format( "'%s' size %d align %d", spec.readableType(), spec.sizeOf(), spec.alignOf() );
    System.out.println();
  }
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
    case STRUCT: case UNION: case ENUM:
      {
        TSpecTagNode n = (TSpecTagNode)spec;
        return n.spec.name != null ? spec.code.str + " " + n.spec.name.name : spec.code.str;
      }
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
        base = spec( signed != null ? signed : lenSpec, Code.INT );
      else if (complex != null)
      {
        base = spec( complex, Code.DOUBLE );
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
        signed = spec( base, m_opts.getSignedChar() ? Code.SIGNED : Code.UNSIGNED);
      break;
    case INT:
      if (signed == null)
        signed = spec( base, Code.SIGNED );
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

    case STRUCT: case UNION: case ENUM:
      spec = ((TSpecTagNode)base).spec;
      break;

    default: spec = null; break;
    }

    if (complex != null)
      return new BasedSpec( complex.code == Code._COMPLEX ? TypeSpec.COMPLEX : TypeSpec.IMAGINARY, spec );
    else
      return spec;
  }

  /** Caller must check haveErr */
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

    if (!m_plat.checkAndCompleteAttrs( loc, q ))
      haveErr = true;

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

public final TDeclarator declarator ( ISourceRange loc, Symbol ident )
{
  return new TDeclarator( loc, ident );
}

public final TDeclarator abstractDeclarator ( CParser.Location loc )
{
  // create a position instead of a location
  return declarator( new CParser.Location( loc.begin ), null );
}

public final TDeclarator.Elem pointerDecl ( ISourceRange loc, TSpecNode qualList, TDeclarator.Elem to )
{
  return new TDeclarator.PointerElem( loc, qualList ).append( to );
}

public final TDeclarator.Elem arrayDecl (
  ISourceRange loc,
  TSpecNode qualList, ISourceRange _static, ISourceRange asterisk, ISourceRange nelemLoc, TExpr.Expr nelem
)
{
  return new TDeclarator.ArrayElem(
    loc, qualList, _static, asterisk, nelemLoc, nelem != null ? implicitLoad(nelem) : null
  );
}

public final TIdentList identList ()
{
  return new TIdentList();
}

public final TIdentList identListAdd (
  ISourceRange loc, TIdentList list, Symbol sym
)
{
  Types.Param m;
  if ( (m = list.get( sym )) == null)
  {
    m = new Types.Param( loc, list.size(), sym, null, null );
    list.put( sym, m );
  }
  else
    error( loc, "parameter '%s' already declared here: %s", sym.name, SourceRange.formatRange(m) );
  return list;
}

public final TDeclarator.Elem funcDecl ( ISourceRange loc, ParamScope paramScope )
{
  return new TDeclarator.FuncElem( loc, paramScope, null );
}

public final TDeclarator.Elem oldFuncDecl ( ISourceRange loc, TIdentList identList )
{
  return new TDeclarator.FuncElem( loc, null, identList );
}

private final TDeclaration mkDeclaration ( TDeclarator dr, TSpecNode dsNode )
{
  return new TDeclaration( dr, dsNode, dr );
}

public final TDeclaration mkTypeName ( TDeclarator dr, TSpecNode dsNode )
{
  TDeclaration decl = mkDeclaration( dr, dsNode );
  validateAndBuildType( decl, false );
  return decl;
}

private final class TypeChecker implements TDeclarator.Visitor
{
  private final TDeclarator declarator;
  private final boolean functionDefinition;
  Qual qual;
  boolean haveError;

  TypeChecker ( TDeclarator declarator, boolean functionDefinition, Qual qual )
  {
    this.declarator = declarator;
    this.functionDefinition = functionDefinition;
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
    if (th.haveErr)
    {
      haveError = true;
      return false;
    }

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

    if (this.qual.spec.kind == TypeSpec.FUNCTION)
    {
      error( elem, "array of functions" );
      haveError = true;
      return false;
    }
    else if (!qual.spec.isComplete())
    {
      error( elem, "array has incomplete type '%s'", qual.readableType() );
      haveError = true;
      return false;
    }

    long nelem = -1; // -1 indicates no size specified

    if (elem.nelem != null)
    {
      if (elem.nelem.isError())
      {
        haveError = true;
        return false;
      }

      TExpr.ArithConstant c = constantExpression( elem.nelemLoc, elem.nelem );
      if (c.isError()) // not a constant integer?
        haveError = true;

      Constant.IntC ic = (Constant.IntC) c.getValue();
      if (ic.sign() < 0)
      {
        error( elem, "negative array size" );
        haveError = true;
        ic = Constant.makeLong( ic.spec, 1 ); // Replace with a normal value to continue error checking
      }

      if (!ic.fitsInLong())
      {
        error( elem, "array size integer overflow" );
        haveError = true;
        ic = Constant.makeLong( ic.spec, 1 ); // Replace with a normal value to continue error checking
      }

      nelem = ic.asLong();
    }

    ArraySpec spec = newArraySpec( elem, this.qual, nelem );
    if (spec == null) // Should never happen, but we want to obey the function interface
    {
      haveError = true;
      return false;
    }
    spec._static = elem._static != null;
    spec.asterisk = elem.asterisk != null;
    TypeHelper th = new TypeHelper( elem );
    th.accumulate( elem.qualList );
    this.qual = th.mkQual( spec );
    if (th.haveErr)
    {
      haveError = true;
      return false;
    }

    return true;
  }

  @Override public boolean function ( int depth, TDeclarator.FuncElem elem )
  {
    if (!checkDepth( depth, elem ))
      return false;

    if (this.qual.spec.kind == TypeSpec.ARRAY)
    {
      error( elem, "function returning an array" );
      this.qual = s_errorQual;
      this.haveError = true;
    }
    else if (this.qual.spec.kind == TypeSpec.FUNCTION)
    {
      error( elem, "function returning a function" );
      this.qual = s_errorQual;
      this.haveError = true;
    }
    else if (this.qual.isVoid())
    {
      if (!this.qual.isUnqualified())
      {
        error( elem, "'void' function result must not have qualifiers" );
        this.qual = new Qual(this.qual.spec);
      }
    }

    final FunctionSpec spec;

    if (elem.paramScope != null) // new-style function?
    {
      Param[] params = null;

      final List<Decl> decls = elem.paramScope.decls();

      // check for func (void)
      if (decls.size() == 1)
      {
        Decl d = decls.get(0);
        if (d.symbol == null && d.type.isVoid())
        {
          params = FunctionSpec.NO_PARAMS;
          if (!d.type.isUnqualified())
            error( d, "'void' as parameter must not have qualifiers" );
        }
      }

      if (params == null) // If we didn't determine it is a '(void)'
      {
        params = new Param[decls.size()];
        int i = 0;
        for ( Decl d : decls )
        {
          if (d.type.isVoid())
          {
            error( d, "parameter %d ('%s') has type 'void'", i+1, optName(d.symbol) );
            params[i] = new Param( d, i, d.symbol, s_errorQual, null );
            ++i;
            continue;
          }

          if (functionDefinition && declarator.isStartElem( elem ))
          {
            if (d.symbol == null)
              error( d, "parameter %d: anonymous parameters are not allowed in function definition", i+1 );
            if (!d.type.spec.isComplete())
            {
              error( d, "parameter %d ('%s') has incomplete type '%s'", i+1, optName(d.symbol), d.type.readableType() );
              params[i] = new Param( d, i, d.symbol, s_errorQual, null );
              ++i;
              continue;
            }
          }

          params[i] = new Param( d, i, d.symbol, d.type, null );
          ++i;
        }
      }

      spec = new FunctionSpec( false, params, elem.paramScope.getEllipsis(), qual );
    }
    else // old-style function
    {
      final Param[] params;

      if (elem.identList == null)
        params = null;
      else if (functionDefinition && declarator.isStartElem( elem ))
      {
        params = new Param[elem.identList.size()];
        int i = 0;
        for ( Param m : elem.identList.values() )
          params[i++] = m; // FIXME: coordinates
      }
      else
      {
        warning( elem, "old-style parameter names only allowed in function definition" );
        params = null;
      }

      spec = new FunctionSpec( true, params, false, qual );
    }
    this.qual = new Qual( spec );
    return true;
  }
}

/**
 * Validate the type described in TDeclaration and generate the TypeSpec chain for it.
 * Store the result in {@code decl.type}
 * @param decl
 */
private final void validateAndBuildType ( TDeclaration decl, boolean functionDefinition )
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

  if (decl.declarator != null)
  {
    TypeChecker checker = new TypeChecker( decl.declarator, functionDefinition, ds.qual );

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

private Qual adjustParamType ( Qual qual )
{
  if (qual.spec.kind == TypeSpec.FUNCTION)
  {
    // function => pointer to function
    return new Qual(newPointerSpec(qual) );
  }
  else if (qual.spec.kind == TypeSpec.ARRAY)
  {
    // array => pointer to element

    ArraySpec arraySpec = (ArraySpec)qual.spec;
    PointerSpec ptrSpec = newPointerSpec( arraySpec.of );
    if (arraySpec._static)
      ptrSpec.staticSize = arraySpec.hasNelem() ? arraySpec.getNelem() : -1;
    Qual q = new Qual( ptrSpec );
    q.combine( qual ); // Keep the C99 array qualifiers

    return q;
  }

  return qual;
}

private static boolean isFunc ( Qual q )
{
  return q.spec.kind == TypeSpec.FUNCTION;
}

private ArraySpec compositeArray ( ArraySpec a, ArraySpec b )
{
  Qual compOf = compositeType( a.of, b.of );
  if (a.hasNelem() == b.hasNelem())
    return a.of == compOf ? a : (b.of == compOf ? b : new ArraySpec( compOf, a.getNelem(), a.sizeOf() ) );
  else if (a.hasNelem())
    return a.of == compOf ? a : new ArraySpec( compOf, a.getNelem(), a.sizeOf() );
  else // b.hasNelem()
    return b.of == compOf ? b : new ArraySpec( compOf, b.getNelem(), b.sizeOf() );
}

private FunctionSpec compositeFunction ( FunctionSpec a, FunctionSpec b )
{
  Qual compOf = compositeType( a.of, b.of );
  if (a.isNewStyle() && b.isNewStyle())
  {
    // When combining two new-style functions, we give priority to the second one
    final int count = a.getParams().length;
    Param[] params = null;

    for ( int i = 0; i < count; ++i )
    {
      Param pa = a.getParams()[i];
      Param pb = b.getParams()[i];
      Qual compType = compositeType( pa.type, pb.type );

      // If we were good so far, and the new type is good, continue to be good
      if (params == null && compType == pb.type)
        continue;

      // If we were good up to here, we must re-create all the previous params
      if (params == null)
      {
        params = new Param[count];
        for ( int o = 0; o < i; ++o )
          params[o] = b.getParams()[o];
      }

      params[i] = compType == pb.type ? pb : new Param( pb, i, pb.name, compType, pb.extAttrs );
    }

    if (b.of == compOf && params == null)
      return b;

    if (params == null)
      params = b.getParams();

    return new FunctionSpec( b.isOldStyle(), params, b.getEllipsis(), compOf );
  }
  else if (!a.isNewStyle() && !b.isNewStyle())
  {
    return a.of == compOf ? a :
            (b.of == compOf ? b : new FunctionSpec( a.isOldStyle(), a.getParams(), a.getEllipsis(), compOf ));
  }
  else if (a.isNewStyle())
    return a.of == compOf ? a : new FunctionSpec( a.isOldStyle(), a.getParams(), a.getEllipsis(), compOf );
  else // b.isNewStyle()
    return b.of == compOf ? b : new FunctionSpec( b.isOldStyle(), b.getParams(), b.getEllipsis(), compOf );
}

private Qual compositeType ( Qual qa, Qual qb )
{
  assert qa.compatible( qb );

  if (qa == qb || qa.spec == qb.spec)
    return qa;

  if (qa.spec.isError())
    return qa;
  else if (qb.spec.isError())
    return qb;

  Spec compositeSpec;
  if (qa.spec.kind == TypeSpec.ARRAY)
    compositeSpec = compositeArray( (ArraySpec)qa.spec, (ArraySpec)qb.spec );
  else if (qa.spec.kind == TypeSpec.FUNCTION)
    compositeSpec = compositeFunction( (FunctionSpec)qa.spec, (FunctionSpec)qb.spec );
  else
    return qa;

  return qa.spec == compositeSpec ? qa : (qb.spec == compositeSpec ? qb : qa.copy( compositeSpec ));
}

/**
 * Sets {@link TDeclaration#sclass}, {@link TDeclaration#linkage}, {@link TDeclaration#defined}
 * @param di
 */
private final void validateAndSetLinkage ( final TDeclaration di )
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

    switch (di.sclass)
    {
    case EXTERN: // only in case of isFunc()
    case NONE:
      di.linkage = Linkage.EXTERNAL;
      break;
    case STATIC:
      di.linkage = Linkage.INTERNAL;
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

    di.linkage = di.sclass == SClass.EXTERN ? Linkage.EXTERNAL : Linkage.NONE;
    di.defined = di.sclass != SClass.EXTERN;
    break;

  case PARAM:
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

  case ENUM:
  case AGGREGATE:
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

private final Decl declare ( TDeclaration di, boolean functionDefinition )
{
  validateAndBuildType( di, functionDefinition );
  validateAndSetLinkage( di );

  Decl prevDecl = null;
  if (!di.error)
  {
    // Check for a previous declaration in this scope
    if (di.hasIdent() && di.getIdent().topDecl != null && di.getIdent().topDecl.visibilityScope == m_topScope)
      prevDecl = di.getIdent().topDecl;

    // Locate a previous declaration with linkage in any parent scope, skipping declarations without linkage
    // For cases like this:
    //
    // int a; //@1 EXTERNAL
    // void func () {
    //   int a; //@2 NONE
    //   {
    //     extern int a; //@3 EXTERNAL
    //   }
    // }
    //
    // @3 refers to @1, even though it is shadowed by @2
    if (prevDecl == null && di.linkage != Linkage.NONE && di.hasIdent() /*always true or linkage would be NONE*/)
    {
      prevDecl = di.getIdent().topDecl;
      while (prevDecl != null && prevDecl.linkage == Linkage.NONE)
        prevDecl = prevDecl.listPrev;
    }
  }

redeclaration:
  if (prevDecl != null)
  {
    if (!prevDecl.type.compatible( di.type ))
    {
      error( di, "'%s' redeclared differently; previous declaration here: %s",
             di.getIdent().name, SourceRange.formatRange(prevDecl) );
      di.error = true;
      break redeclaration;
    }

  /*
    Check for re-declaration.
    The only allowed cases of re-declaration:
      - [EXTERNAL] ... [EXTERNAL]
      - [INTERNAL] ... extern [EXTERNAL]
      - [INTERNAL] ... [INTERNAL]
   */
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

    di.type = compositeType( prevDecl.type, di.type );

    Decl decl = new Decl( di, prevDecl, m_topScope, di.sclass, di.linkage, di.type, di.defined, di.error );
    m_topScope.pushDecl( decl );

    return decl;
  }

  if (di.defined && di.sclass == SClass.EXTERN)
    di.sclass = SClass.NONE;

  Decl decl = new Decl(
    di, di.sclass != SClass.TYPEDEF ? Decl.Kind.VAR : Decl.Kind.TYPE,
    di.linkage == Linkage.NONE ? m_topScope : m_translationUnitScope, // storage scope
    m_topScope, // visibility scope
    di.sclass, di.linkage, di.getIdent(), di.type, di.defined, di.error
  );

  // Only make the declaration visible if there isn't a previous one. prevDecl is not null
  // only if we excited the re-declaration block with an error, so we don't want to activate this one
  if (prevDecl == null)
    m_topScope.pushDecl( decl );
  return decl;
}

public final Decl finishDeclarator ( TSpecNode specNode, TDeclarator declarator )
{
  TDeclaration tDecl = mkDeclaration( declarator, specNode );
  return declare( tDecl, false );
}
public final Decl funcDefDeclarator ( TSpecNode specNode, TDeclarator declarator )
{
  TDeclaration tDecl = mkDeclaration( declarator, specNode );
  return declare( tDecl, true );
}

/** A width to use for bit-fields in case of error */
private final Constant.IntC m_errBitFieldWidth = Constant.makeLong( TypeSpec.SINT, 1 );

public final void finishBitfield (
  TSpecNode specNode, TDeclarator declarator, ISourceRange widthLoc, TExpr.ArithConstant width
)
{
  TDeclaration tDecl = mkDeclaration( declarator, specNode );
  final Decl decl = declare( tDecl, false );

  Constant.IntC ic;
  if (width.isError())
  {
    decl.orError( true );
    ic = m_errBitFieldWidth;
  }
  else
    ic = (Constant.IntC)width.getValue();

  if (ic.sign() < 0)
  {
    error( widthLoc, "negative bit-field width for field '%s'", optName(decl.symbol) );
    decl.orError( true );
    ic = m_errBitFieldWidth;
  }

  if (decl.isError())
    return;

  if (!decl.type.spec.isInteger())
  {
    error( decl, "'%s': invalid type of bit-field '%s'. Must be integer", decl.type.readableType(), optName(decl.symbol) );
    decl.orError( true );
    return;
  }

  if (ic.isZero())
  {
    if (decl.symbol != null)
    {
      error( decl, "zero-width bit-field '%s' must be anonymous", optName(decl.symbol) );
      decl.orError( true );
      ic = m_errBitFieldWidth;
    }
  }
  else
  {
    if (!ic.fitsInLong())
    {
      error( widthLoc, "bit-field '%s' width integer overflow", optName(decl.symbol) );
      decl.orError( true );
      ic = m_errBitFieldWidth;
    }
    else
    if (ic.asLong() > decl.type.spec.kind.width)
    {
      error( widthLoc, "width of bit-field '%s' (%d bits) exceeds width of its type (%d bits)",
              optName(decl.symbol), ic.asLong(), decl.type.spec.kind.width );
      decl.orError( true );
      ic = m_errBitFieldWidth;
    }
  }

  assert ic.fitsInLong();
  decl.bitfieldWidth = (int)ic.asLong();
}

public final void emptyDeclaration ( TSpecNode specNode )
{
  TDeclaration tDecl = mkDeclaration( new TDeclarator( specNode, null ), specNode );
  validateAndBuildType( tDecl, false );
  validateAndSetLinkage( tDecl );
}

public final void initDeclaration ( Decl decl, parsedInit.Initializer init )
{
  if (decl.isError())
    return;

  if (decl.sclass == SClass.EXTERN)
  {
    error( decl, "'%s': 'extern' in initialization", optName(decl.symbol) );
    decl.orError();
    return;
  }
  // This check is redundant since it is covered by the previous one. Still, we want to be
  // explicit
  if (decl.visibilityScope.kind == Scope.Kind.BLOCK && decl.linkage != Linkage.NONE)
  {
    error( decl, "'%s': invalid initialization", optName(decl.symbol) );
    decl.orError();
    return;
  }

  // Check if the variable has a complete type. It could however be an array of unknown size.
  // Note that if it is an array, it is already validated to have a complete element type
  if (!decl.type.spec.isArray() && !decl.type.spec.isComplete())
  {
    error( decl, "variable '%s' has incomplete type '%s'", optName(decl.symbol), decl.type.readableType() );
    decl.orError();
    return;
  }

  final TInit.Value initValue = parseInitializer( decl.type, init );

  // Optionally complete the array type
  if (initValue != null && !initValue.isError() && decl.type.spec.isArray() && !((ArraySpec)decl.type.spec).hasNelem())
  {
    decl.type = initValue.getQual();
    if (m_opts.debugDecl)
      decl.storageScope.debugDecl( "complete", decl );
  }

  Decl prevDecl = decl.prevDecl;
redeclaration:
  if (prevDecl != null)
  {
    if (!prevDecl.type.compatible( decl.type ))
    {
      error( decl, "'%s' redeclared differently; previous declaration here: %s",
              decl.symbol.name, SourceRange.formatRange(prevDecl) );
      decl.orError();
      break redeclaration;
    }

    if (prevDecl.defined)
    {
      error( decl, "'%s': invalid redefinition; already defined here: %s",
              decl.symbol.name, SourceRange.formatRange(prevDecl) );
      decl.orError();
      return;
    }
  }

  if (initValue != null && !initValue.isError())
    decl.initValue = initValue;
  else
    decl.orError();

  if (m_topScope.kind == Scope.Kind.FILE)
  {
    switch (decl.sclass)
    {
    case EXTERN: // only in case of isFunc()
    case NONE:
    case STATIC:
      decl.defined = true;
      break;
    default: assert false; break;
    }
  }

  // Fold and validate the static storage duration expressions
  if (!decl.isError() && decl.isStaticStorageDuration())
  {
??    TInit.Value staticInit = validateStaticInitExpression(decl.initValue);
??    decl.orError(staticInit != null && staticInit.isError());
  }
}

private TInit.Value validateStaticInitExpression (TInit.Value initValue)
{
  if (initValue == null) // an empty init value is valid
    return null;
  if (initValue.isError())
    return initValue;

  TInit.Value res = new TInit.Value(initValue, initValue.getQual());

  if (initValue.getQual().spec.isAggregate())
  {
    if (!initValue.isCompound())
    {
      error(initValue, "initializer is not a constant");
      res.markError();
    }
    else
    {
      TInit.Compound inputCompound = initValue.getCompound();
      TInit.Compound resCompound = res.makeCompound(inputCompound.getLength());

      for ( int i = 0, e = inputCompound.getLength(); i < e; ++i )
        resCompound.setSubObject(i, validateStaticInitExpression(inputCompound.getSubObject(i)));
    }
  }
  else
  {
    assert initValue.isSingle();

    TExpr.StaticInit initExpr = classifyInitExpression(initValue, initValue.getExpr());
    if (initExpr == null)
    {
      error(initValue, "initializer is not a constant");
      res.markError();
    }
    else
      res.setExpr(initExpr);
  }

  return res;
}

private @NotNull TInit.Value newInitObject ( ISourceRange loc, Qual type )
{
  final Spec spec = type.spec;
  if (!spec.isError())
  {
    if (!spec.isComplete() && !(spec.isArray() && ((ArraySpec)spec).of.spec.isComplete()))
      error( loc, "cannot initialize incomplete type '%s'", spec.readableType() );
    else if (spec.isStructUnion() || spec.isArray() || spec.isScalar())
      return new TInit.Value(loc, type.newUnqualified());
    else
      error( loc, "cannot initialize type '%s'", spec.readableType() );
  }

  TExpr.Expr err = exprError(loc);
  final TInit.Value res = new TInit.Value(loc, err.getQual());
  res.setExpr(err);
  return res;
}

/** An entry the stack maintained by {@link InitValueIterator} */
private static final class InitStackEntry
{
  public TInit.Value value;
  public int nextSubObject;

  void reset ()
  {
    value = null;
    nextSubObject = 0;
  }
}

/**
 * Iterate recursively the values inside a {@link c99.parser.tree.TInit.Value} using a stack.
 */
private final class InitValueIterator
{
  private InitStackEntry[] m_stack;
  /** number of elements in the stack */
  private int m_count;
  /* elements < m_limit have associated objects */
  private int m_limit;

  private InitStackEntry m_top;
  private boolean m_error;

  public InitValueIterator (TInit.Value root)
  {
    m_stack = new InitStackEntry[4];
    push(root);
  }

  public boolean isError () { return m_error; }
  public void markError () { m_error = true; }

  @Nullable public TInit.Value next (ISourceRange loc)
  {
    int subObjIndex;

    // Pop all exhausted elements. Note that in unions only the first element is initialized
    while ((subObjIndex = m_top.nextSubObject) >= m_top.value.getSequentialInitLength())
    {
      pop();
      if (m_top == null) // the stack is empty?
        return null;
    }

    ++m_top.nextSubObject;

    TInit.Value elem = subObjectAt(loc, subObjIndex);
    if (elem == null) // error?
      return null;

    push(elem);

    return elem;
  }

  /**
   * Iterates through all destination values encoded in a designation. This is needed to support the GCC range
   * extension.
   */
/*  public final class DesignationIterator implements Iterator<TInit.Value>
  {
    private TInit.Value m_next;

    public DesignationIterator ( TInit.Value next )
    {
      m_next = next;
    }

    @Override public boolean hasNext ()
    {
      return m_next != null;
    }
    @Override public TInit.Value next ()
    {
      if (m_next == null)
        throw new NoSuchElementException();
      TInit.Value res = m_next;
      m_next = null;
      return res;
    }
  }*/

  public TInit.Value selectDesignation (parsedInit.Designator designator)
  {
    popToRoot();

    for(;;)
    {
      TInit.Value nextObject = null;

      if (designator.isError())
      {}
      else if (designator.isFieldDesignator())
      {
        final parsedInit.FieldDesignator fd = designator.asFieldDesignator();
        if (m_top.value.getQual().spec.isStructUnion())
        {
          final StructUnionSpec spec = (StructUnionSpec)m_top.value.getQual().spec;
          final Member member = spec.lookupMember( fd.ident );
          if (member != null)
          {
            nextObject = subObjectAt(designator, member.index);
            m_top.nextSubObject = member.index + 1;
          }
          else
            error( designator, "field '.%s' is not a member of '%s'", optName( fd.ident ), spec.readableType() );
        }
        else
        {
          error( designator, "field designator in initializer for type '%s'", m_top.value.getQual().spec.readableType() );
        }
      }
      else if (designator.isIndexDesignator())
      {
        final parsedInit.IndexDesignator id = designator.asIndexDesignator();
        if (m_top.value.getQual().spec.isArray())
        {
          final ArraySpec spec = (ArraySpec)m_top.value.getQual().spec;

          if (!spec.hasNelem() || spec.getNelem() > id.index)
          {
            nextObject = subObjectAt(designator, id.index);
            m_top.nextSubObject = id.index + 1;
          }
          else
            error( designator, "array designator index (%d) exceeds array bounds (%d)", id.index, spec.getNelem() );
        }
        else
        {
          error( designator, "index designator in initializer for type '%s'", m_top.value.getQual().spec.readableType() );
        }
      }
      else if (designator.isRangeDesignator())
      {
        final parsedInit.RangeDesignator id = designator.asRangeDesignator();
        if (m_top.value.getQual().spec.isArray())
        {
          final ArraySpec spec = (ArraySpec)m_top.value.getQual().spec;

          if (!spec.hasNelem() || spec.getNelem() > id.first && spec.getNelem() > id.last)
          {
            error(designator, "GCC range designators are not implemented yet"); // TODO
  /*          InitResult initRes;
            InitAst.Initializer saveElem = elem[0];

            for ( int i = id.first;; ) // Have to code the iteration carefully to avoid overflow
            {
              TInit.Value lookupObj = subObjectAt(designator, i);
              if (lookupObj == null)
                return false;

              elem[0] = saveElem;
              if ( (initRes = initValues( lookupObj, elem, designator.getNext() )) == InitResult.ERROR)
                return InitResult.ERROR;

              // Careful to check the exit condition before incrementing, as id.last could be INT_MAX
              if (i == id.last)
                break;
              ++i;
            }

            return initRes;*/
          }
          else
          {
            error( designator, "array designator index (%d) exceeds array bounds (%d)",
                    Math.max(id.first, id.last), spec.getNelem() );
          }
        }
        else
        {
          error( designator, "range designator in initializer for type '%s'", m_top.value.getQual().spec.readableType() );
        }
      }
      else
      {
        assert false : "Unknown designator type " + designator.getClass().getName();
      }

      if (nextObject == null)
      {
        markError();
        return null;
      }

      push(nextObject); // should we push non-aggregates?

      designator = designator.getNext();
      if (designator == null)
        return nextObject;
    }
  }

  /**
   * Get a sub-object at index, creating it if necessary
   * @return null in case of error (which has already been reported)
   */
  private TInit.Value subObjectAt (ISourceRange loc, int subObjIndex)
  {
    if (!m_top.value.isCompound())
      m_top.value.makeCompound();

    TInit.Compound c = m_top.value.getCompound();

    TInit.Value elem = c.getSubObject(subObjIndex);
    if (elem == null) // create a new sub-object if it doesn't exist
    {
      Qual subObjectType;

      switch (m_top.value.getQual().spec.kind)
      {
      case ARRAY:
        subObjectType = ((ArraySpec)m_top.value.getQual().spec).of;
        break;
      case STRUCT: case UNION:
        subObjectType = ((StructUnionSpec)m_top.value.getQual().spec).getFields()[subObjIndex].type;
        break;
      default:
        assert false;
        error(loc, "invalid parent object type "+ m_top.value.getQual().readableType());
        markError();
        return null;
      }

      elem = newInitObject(loc, subObjectType);
      if (elem == null)
        markError();
      else
        c.setSubObject(subObjIndex, elem);
    }
    else
    {
      warning(loc, "overwriting prior initialization");
      elem.setRange(loc);
    }

    return elem;
  }

  private void push (TInit.Value v)
  {
    if (m_count < m_limit) // fast path: can we reuse an already allocated object?
    {
      (m_top = m_stack[m_count++]).value = v;
      return;
    }

    if (m_count == m_stack.length) // do we need to resize?
      m_stack = Arrays.copyOf(m_stack, m_stack.length << 1);

    (m_top = m_stack[m_count++] = new InitStackEntry()).value = v;
    m_limit = m_count;
  }

  public void pop ()
  {
    m_stack[--m_count].reset();
    m_top = m_count > 0 ? m_stack[m_count-1] : null;
  }

  /**
   * Pop all elements except the root. The stack will have a depth of 1 after this.
   */
  private void popToRoot ()
  {
    assert m_count > 0;
    if (m_count > 1) // defensive programming while optimizing the case of a single element
    {
      for ( int i = 1, e = m_count; i < e; ++i )
        m_stack[i].reset();

      m_count = 1;
      m_top = m_stack[0];
    }
  }
}

/**
 * Initialize a non-compound {@link c99.parser.tree.TInit.Value}.
 *
 * @param scalar
 * @param elem
 * @param warnAboutBraces whether to warn about braces around a scalar initializer. The first time
 *                        we set it to false, as a single pair of braces around a scalar initializer is allowed,
 *                        but in consequent recursive invocations we set it to true
 *
 */
private void initSingle ( TInit.Value scalar, parsedInit.Initializer elem, boolean warnAboutBraces)
{
  if (elem.isExpr())
  {
    // Finally we get to the meat: initialize a scalar initializer with an expression
    if (!scalar.isEmpty()) // overwriting previous value
      scalar.setRange(elem);
    scalar.setExpr(implicitTypecastExpr(scalar.getQual(), elem.asExpr().getExpr()));
  }
  else
  {
    parsedInit.InitializerList list = elem.asList();

    if (warnAboutBraces)
      warning(list, "too many braces around scalar initializer");

    elem = list.getFirst();
    if (elem == null)
    {
      error(list, "empty scalar initializer");
      scalar.setExpr(exprError(list));
    }
    else if (elem.getDesignation() != null)
    {
      error(elem.getDesignation(), "designator in initializer for '%s'", scalar.getQual().readableType());
      scalar.setExpr(exprError(elem));
    }
    else
    {
      initSingle(scalar, elem, true);

      elem = elem.getNext();
      if (elem != null)
        warning(elem, "excess elements in scalar initializer");
    }
  }
}

/** check if we have a string initializer compatible with an array */
private final boolean isArrayStringInit ( Spec spec, parsedInit.Initializer elem )
{
  if (!spec.isArray())
    return false;

  // Check for { "string" } which is also valid
  if (elem.isList())
  {
    elem = elem.asList().getFirst();
    if (elem.getNext() != null)
      return false;
    if (elem.getDesignation() != null)
      return false;
  }

  if (!elem.isExpr())
    return false;

  final TExpr.Expr e = elem.asExpr().getExpr();
  if (e.getCode() != TreeCode.STRING)
    return false;
  final TExpr.StringLiteral lit = (TExpr.StringLiteral)e;

  final TypeSpec kind = ((ArraySpec)spec).of.spec.kind;
  return kind.integer && kind.width == lit.getValue().spec.width;
}

private void initArrayWithString (TInit.Value obj, parsedInit.Initializer init)
{
  // Check for { "string" } which is also valid
  if (init.isList())
    init = init.asList().getFirst();

  final TExpr.Expr e = init.asExpr().getExpr();
  assert(e.getCode() == TreeCode.STRING);
  final TExpr.StringLiteral lit = (TExpr.StringLiteral)e;

  obj.makeCompound();
  final TInit.Compound comp = obj.getCompound();
  final Qual elemType = ((ArraySpec)lit.getQual().spec).of;
  final AnyStringConst litValue = lit.getValue();
  final int len = Math.min(obj.getSequentialInitLength(), litValue.length() + 1); // NOTE: the +1 is for the trailing 0

  if (litValue.length() > obj.getSequentialInitLength())
    warning(init, "initializer string for array '%s' is too long", obj.getQual().readableType());

  for ( int index = 0; index < len; ++index )
  {
    final TInit.Value value = newInitObject(init, elemType);
    comp.setSubObject(index, value);
    value.setExpr(
      new TExpr.ArithConstant(init, elemType,
        Constant.makeLong(elemType.spec.kind, litValue.at(index))
      )
    );
  }
}

private void processBracedList (TInit.Value rootObj, parsedInit.InitializerList list)
{
  final InitValueIterator iterator = new InitValueIterator(rootObj);

  parsedInit.Initializer elem = list.getFirst();
elemLoop:
  while (elem != null)
  {
    TInit.Value curObject = elem.getDesignation() == null ?
      iterator.next(elem) : iterator.selectDesignation(elem.getDesignation());

  goDeeper:
    for(;;) // loop calling iterator.next() and going deeper into aggregate objects
    {
      if (curObject == null)
      {
        if (!iterator.isError())
          warning(elem, "excess elements in '%s' initializer", rootObj.getQual().readableType());
        else
          rootObj.markError();
        break elemLoop;
      }

      if (curObject.getQual().spec.isScalar())
        initSingle(curObject, elem, false);
      else if (isArrayStringInit(curObject.getQual().spec, elem))
      {
        initArrayWithString(curObject, elem);
        iterator.pop();
      }
      else
      {
        if (elem.isList()) // a braced list initialization of an aggregate
        {
          if (!curObject.isCompound())
          {
            if (curObject.getExpr() != null)
            {
              TODO("extract the field name by looking a the previous element in the stack");
              warning(elem, "overwriting value which was previously initialized at '%s'",
                m_reporter.formatRange(curObject.getExpr()));
            }
            curObject.makeCompound();
          }
          processBracedList(curObject, elem.asList());
          iterator.pop();
        }
        else
        {
          if (curObject.getQual().spec.isStructUnion() &&
              curObject.getQual().spec == elem.asExpr().getExpr().getQual().spec)
          {
            initSingle(curObject, elem, false);
            iterator.pop();
          }
          else
          {
            // an aggregate that must be initialized by the consecutive initializers
            // go deeper into the curObject
            curObject = iterator.next(elem);
            continue goDeeper; // loop again
          }
        }
      }

      break goDeeper;
    }

    if (curObject.isError())
      rootObj.markError();

    elem = elem.getNext();
  }
}

/**
 * Parse the initializer list and return a structured initializer
 */
private @NotNull TInit.Value parseInitializer ( Qual type, parsedInit.Initializer init )
{
  TInit.Value obj;

  obj = newInitObject( init, type );

  if (type.spec.isStructUnion())
  {
    // the value must either be a braced list or a value with a compatible type
    if (init.isList())
      processBracedList(obj, init.asList());
    else
      initSingle(obj, init, false);
  }
  else if (type.spec.isArray())
  {
    // the value must either be a braced list, of possibly a character string (of compatible base type)
    if (isArrayStringInit(type.spec, init))
      initArrayWithString(obj, init);
    else if (init.isList())
      processBracedList(obj, init.asList());
    else
    {
      error( init, "attempting to initialize '%s' with '%s'", type.spec.readableType(),
        init.asExpr().getExpr().getQual().spec.readableType() );
      obj.setExpr(exprError(init));
    }

    // Complete the array size if missing
    ArraySpec as = (ArraySpec)type.spec;
    if (!obj.isError() && !as.hasNelem())
      obj.updateQual(obj.getQual().copy(newArraySpec( init, as.of, obj.getCompound().getLength() ) ) );
  }
  else if (type.spec.isScalar())
  {
    initSingle(obj, init, false);
  }
  else
  {
    error( init, "cannot initialize type '%s'", type.spec.readableType() );
    obj.markError();
  }

  if (m_opts.debugInit)
    ExprFormatter.format( 0, m_debugWriter, obj );

  return obj;
}

} // class
