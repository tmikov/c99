package c99.parser;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import c99.*;
import c99.Types.*;
import c99.parser.tree.*;

import static c99.parser.Trees.*;

public class DeclActions extends ExprActions
{
private static final boolean DEBUG_CALC_AGG_SIZE = false;
private static final boolean DEBUG_ENUM = false;

private Scope m_topScope;

protected void init ( CompEnv compEnv, SymTable symTab )
{
  super.init( compEnv, symTab );
}

public final Object FIXME ( String msg )
{
  assert false;
  return null;
}

public final Object FIXME ()
{
  assert false;
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
  return m_topScope = new Scope( Scope.Kind.FILE, m_topScope );
}
public final Scope pushBlockScope ()
{
  return m_topScope = new Scope( Scope.Kind.BLOCK, m_topScope );
}
public final ParamScope pushParamScope ()
{
  return pushScope( new ParamScope( m_topScope ) );
}
public final Scope pushAggScope ()
{
  return pushScope( new Scope( Scope.Kind.AGGREGATE, m_topScope ) );
}
public final EnumScope pushEnumScope ()
{
  return pushScope( new EnumScope( m_topScope ) );
}

private final Scope topNonStructScope ()
{
  Scope res = m_topScope;
  while (res.kind == Scope.Kind.AGGREGATE || res.kind == Scope.Kind.ENUM)
    res = res.getParent();
  return res;
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
  if ((def = m_plat.findExtAttr(name)) == null)
  {
    error( locName, "unknown attribute '%s'", name );
    return null;
  }
  SourceRange rngAll = BisonLexer.fromLocation(locAll);
  ExtAttr extAttr = m_plat.parseExtAttr(
    rngAll, BisonLexer.fromLocation(locName), def, params
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

public final TSpecNode referenceAgg (
  final CParser.Location loc, final Code code, final CParser.Location identLoc, final Symbol ident
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
      tagDecl = BisonLexer.setLocation(
        new Decl( null, Decl.Kind.TAG, declScope, SClass.NONE, Linkage.NONE, null, new Qual( spec ), false, true ),
        loc
      );
      fwdDecl = true;
    }
  }
  else
  {
    // Forward declaration of tag
    fwdDecl = true;
    Spec spec = tagSpec == TypeSpec.ENUM ? new EnumSpec( ident ) : new StructUnionSpec( tagSpec, ident );
    tagDecl = BisonLexer.setLocation(
      new Decl( null, Decl.Kind.TAG, declScope, SClass.NONE, Linkage.NONE, ident, new Qual( spec ), false, false ),
      identLoc
    );
    declScope.pushTag( tagDecl );
    if (declScope.kind == Scope.Kind.PARAM)
      warning( tagDecl, "declaration of '%s' will not be visible outside of the function", spec.readableType() );
  }

  return new TSpecTagNode( tagDecl, code, (TagSpec) tagDecl.type.spec );
}

public final Decl beginDeclareAgg (
  final CParser.Location loc, final Code code, final CParser.Location identLoc, Symbol ident
)
{
  final TypeSpec tagSpec = code == Code.ENUM ? TypeSpec.ENUM : (code == Code.STRUCT ? TypeSpec.STRUCT : TypeSpec.UNION);
  final Scope declScope = topNonStructScope(); // a forward decl would go in this scope

  Decl tagDecl = null;
  boolean haveErr = false;

  // Check for redefinition: it must have been defined in the current scope
  if (ident != null && ident.topTag != null && ident.topTag.scope == declScope)
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

  final CParser.Location aggLoc = identLoc != null ? identLoc : loc;

  if (tagDecl == null) // If not completing a previous forward declaration
  {
    Spec spec = tagSpec == TypeSpec.ENUM ? new EnumSpec( ident ) : new StructUnionSpec( tagSpec, ident );
    tagDecl = new Decl( null, Decl.Kind.TAG, declScope, SClass.NONE, Linkage.NONE, ident,
                        new Qual( spec ), true, haveErr );
    declScope.pushTag( tagDecl );
    if (declScope.kind == Scope.Kind.PARAM)
      warning( aggLoc, "declaration of '%s' will not be visible outside of the function", spec.readableType() );
  }

  // Update the location to this one in all cases
  BisonLexer.setLocation( tagDecl, aggLoc );

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
    for ( Decl d : decls )
    {
      if (d.kind != Decl.Kind.ENUM_CONST)
        continue;

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

    if (DEBUG_ENUM)
      System.out.format( "defined '%s' based on '%s'\n", type.readableType(), enumSpec.getBaseSpec().readableType() );

    // Declare all enum constants in the parent scope
    for ( Decl d : decls )
    {
      if (d.kind != Decl.Kind.ENUM_CONST)
        continue;
      if (d.symbol.topDecl != null && d.symbol.topDecl.scope == targetScope)
        continue; // Could happen when doing error recovery

      Decl decl = new Decl(
        d, Decl.Kind.ENUM_CONST, targetScope, SClass.NONE, Linkage.NONE, d.symbol, type, true, d.error
      );
      decl.enumValue = (Constant.IntC) Constant.convert( baseSpec.kind, d.enumValue );

      targetScope.pushDecl( decl );
      if (DEBUG_ENUM)
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
        fields[i++] = new Member( d, d.symbol, d.type, d.bitfieldWidth );
        tagSpec.orError( true );
      }
    }
    if (i < fields.length) // Could happen if there were type definitions in that scope
      fields = Arrays.copyOf( fields, i );

    StructUnionSpec aggSpec = (StructUnionSpec)tagSpec;
    aggSpec.setFields( fields );
    calcAggSize( tagDecl, aggSpec );
  }

  tagSpec.orError( tagDecl.error );
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
  CParser.Location identLoc, Symbol ident, CParser.Location valueLoc, TExpr.ArithConstant value
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
    if (ident.topDecl.scope == enumScope)
    {
      error( identLoc, "enumerator '%s' already defined here %s", ident.name, SourceRange.formatRange(ident.topDecl) );
      enumScope.orError( true );
      return;
    }
    else if (ident.topDecl.scope == topNonStructScope())
    {
      error( identLoc, "redefinition of '%s' previously defined here %s", ident.name, SourceRange.formatRange(ident.topDecl) );
      enumScope.orError( true );
      haveError = true;
    }
  }

  // Now we have decision to make. What type to use for the "temporary" in-scope constant?
  // GCC and CLANG use the type of init expression, so that is what we are going to do too
  Qual type = stdQual( enumScope.lastValue.spec );
  Decl decl = BisonLexer.setLocation(
    new Decl( null, Decl.Kind.ENUM_CONST, enumScope, SClass.NONE, Linkage.NONE, ident, type, true, haveError ),
    identLoc
  );
  decl.enumValue = enumScope.lastValue;
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
private final void calcAggSize ( ISourceRange loc, StructUnionSpec spec )
{
  if (spec.isError())
    return;

  assert spec.isComplete();

  long size = 0;
  /** bits available in the last consumed byte of 'size' */
  int bitsAvail = 0;
  int align = 1;
  Member curField = null; // We use that for tracking which field threw

  try
  {
    if (spec.kind == TypeSpec.STRUCT)
    {
      for ( Member field : spec.getFields() )
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

        if (DEBUG_CALC_AGG_SIZE)
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
      for ( Member field : spec.getFields() )
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
      error( curField, "'%s' field '%s': %s",
              spec.readableType(), curField.name != null ? curField.name.name : "<anonymous>", e.getMessage() );
    }
    else
      error( loc, "'%s' %s", spec.readableType(), e.getMessage() );
  }
  spec.setSizeAlign( size, align );
  if (DEBUG_CALC_AGG_SIZE)
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
  TSpecNode qualList, CParser.Location _static, CParser.Location asterisk, CParser.Location nelemLoc, TExpr.Expr nelem
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
  CParser.Location loc, TIdentList list, Symbol sym
)
{
  Types.Param m;
  if ( (m = list.get( sym )) == null)
  {
    m = new Types.Param( null, sym, null );
    BisonLexer.setLocation( m, loc );
    list.put( sym, m );
  }
  else
    error( loc, "parameter '%s' already declared here: %s", sym.name, SourceRange.formatRange(m) );
  return list;
}

public final TDeclarator.Elem funcDecl ( CParser.Location loc, ParamScope paramScope )
{
  return new TDeclarator.FuncElem( loc, paramScope, null );
}

public final TDeclarator.Elem oldFuncDecl ( CParser.Location loc, TIdentList identList )
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
  validateAndBuildType( decl );
  return decl;
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
            error( d, "parameter %d ('%s') has type 'void'", i+1, d.symbol != null ? d.symbol.name : "<anonymous>" );
            params[i] = new Param( d, d.symbol, s_errorQual );
          }
          else
            params[i] = new Param( d, d.symbol, d.type );
          ++i;
        }
      }

      spec = new FunctionSpec( false, params, elem.paramScope.getEllipsis(), qual );
    }
    else // old-style function
    {
      Param[] params;

      if (elem.identList == null)
        params = null;
      else
      {
        params = new Param[elem.identList.size()];
        int i = 0;
        for ( Param m : elem.identList.values() )
          params[i++] = m; // FIXME: coordinates
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
private final void validateAndBuildType ( TDeclaration decl )
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

/**
 * Sets {@link TDeclaration#sclass}, {@link TDeclaration#linkage}, {@link TDeclaration#defined}
 * @param di
 * @param hasInit
 */
private final void validateAndSetLinkage ( final TDeclaration di, final boolean hasInit )
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

  case ENUM:
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
  validateAndBuildType( di );
  validateAndSetLinkage( di, hasInit );

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

    if (!impDecl.type.compatible( di.type ))
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
    // FIXME: WTF?
    // Complete the declaration, if it wasn't before
    if (!impDecl.type.spec.isComplete() && di.type.spec.isComplete())
    {
      Qual completed = di.type.copy();
      completed.combine( impDecl.type );
      impDecl.type = completed;
    }

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
    di, di.sclass != SClass.TYPEDEF ? Decl.Kind.VAR : Decl.Kind.TYPE, m_topScope,
    di.sclass, di.linkage, di.getIdent(), di.type, di.defined, di.error
  );
  if (prevDecl == null) // We could arrive here in case of an incorrect redeclaration
    m_topScope.pushDecl( decl );
  return decl;
}

public final void finishDeclarator ( TSpecNode specNode, TDeclarator declarator, boolean init )
{
  TDeclaration tDecl = mkDeclaration( declarator, specNode );
  declare( tDecl, init );
}
public final void finishDeclarator ( TSpecNode specNode, TDeclarator declarator )
{
  finishDeclarator( specNode, declarator, false );
}

/** A width to use for bit-fields in case of error */
private final Constant.IntC m_errBitFieldWidth = Constant.makeLong( TypeSpec.SINT, 1 );

public final void finishBitfield (
  TSpecNode specNode, TDeclarator declarator, CParser.Location widthLoc, TExpr.ArithConstant width
)
{
  TDeclaration tDecl = mkDeclaration( declarator, specNode );
  Decl decl = declare( tDecl, false );

  Constant.IntC ic;
  if (width.isError())
  {
    decl.error = true;
    ic = m_errBitFieldWidth;
  }
  else
    ic = (Constant.IntC)width.getValue();

  final String fieldName = decl.symbol != null ? decl.symbol.name : "<anonymous>";

  if (ic.sign() < 0)
  {
    error( widthLoc, "negative bit-field width for field '%s'", fieldName );
    decl.error = true;
    ic = m_errBitFieldWidth;
  }

  if (decl.error)
    return;

  if (!decl.type.spec.isInteger())
  {
    error( decl, "'%s': invalid type of bit-field '%s'. Must be integer", decl.type.readableType(), fieldName );
    decl.error = true;
    return;
  }

  if (ic.isZero())
  {
    if (decl.symbol != null)
    {
      error( decl, "zero-width bit-field '%s' must be anonymous", fieldName );
      decl.error = true;
      ic = m_errBitFieldWidth;
    }
  }
  else
  {
    if (!ic.fitsInLong())
    {
      error( widthLoc, "bit-field '%s' width integer overflow", fieldName );
      decl.error = true;
      ic = m_errBitFieldWidth;
    }
    else
    if (ic.asLong() > decl.type.spec.kind.width)
    {
      error( widthLoc, "width of bit-field '%s' (%d bits) exceeds width of its type (%d bits)",
              fieldName, ic.asLong(), decl.type.spec.kind.width );
      decl.error = true;
      ic = m_errBitFieldWidth;
    }
  }

  assert ic.fitsInLong();
  decl.bitfieldWidth = (int)ic.asLong();
}

public final void emptyDeclaration ( TSpecNode specNode )
{
  TDeclaration tDecl = mkDeclaration( new TDeclarator( specNode, null ), specNode );
  validateAndBuildType( tDecl );
  validateAndSetLinkage( tDecl, false );
}

} // class
