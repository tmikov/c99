package c99.parser;

import c99.ISourceRange;
import c99.MiscUtils;
import c99.SourceRange;
import c99.Types.*;

public class Decl extends SourceRange
{

public static enum Kind
{
  VAR,
  ENUM_CONST,
  TYPE,
  TAG,
}
public final Kind kind;
/** Back link into the single-linked list starting from {@link Symbol#topDecl} or {@link Symbol#topTag} */
Decl listPrev;
/**
 * Link to a previous declaration of the same symbol. This can happen when symbols with a linkage are
 * re-declared in nested scopes. Note that in that case {@link #storageScope} will be the value from
 * the previous declaration and will be different from {@link #visibilityScope}
 */
public final Decl prevDecl;
/**
 * Scope where the first declaration occurred and the symbol will be stored. If this is a symbol without
 * linkage, it will be just the local scope, but if it is with a linkage, it will be the translation union scope.
 */
public final Scope storageScope;
/** Scope where this declaration is visible (because it occurred there) */
public final Scope visibilityScope;
public final SClass sclass;
public final Linkage linkage;
public final Symbol symbol;
public final Qual type;
public int bitfieldWidth = -1; //< -1 means not a bit-field
public boolean defined;
private boolean m_error;


public Decl (
  ISourceRange rng, Kind kind, Scope storageScope, Scope visibilityScope, SClass sclass, Linkage linkage, Symbol symbol,
  Qual type, boolean defined, boolean error
)
{
  super(rng);
  this.kind = kind;
  this.prevDecl = null;
  this.storageScope = storageScope;
  this.visibilityScope = visibilityScope;
  this.sclass = sclass;
  this.linkage = linkage;
  this.symbol = symbol;
  this.type = type;
  this.defined = defined;
  this.m_error = error || type.spec.isError();
}

public Decl (
  ISourceRange rng, Decl prevDecl, Scope visibilityScope, SClass sclass, Linkage linkage,
  Qual type, boolean defined, boolean error
)
{
  super(rng);
  this.kind = prevDecl.kind;
  this.prevDecl = prevDecl;
  this.storageScope = prevDecl.storageScope;
  this.visibilityScope = visibilityScope;
  this.sclass = sclass;
  this.linkage = linkage;
  this.symbol = prevDecl.symbol;
  this.type = type;
  this.defined = prevDecl.defined || defined;
  this.m_error = error || type.spec.isError() || prevDecl.isError();
}

public final void orError ( boolean f )
{
  this.m_error |= f;
}
public final void orError ()
{
  this.m_error = true;
}

public final boolean isError ()
{
  return this.m_error || (this.type != null && this.type.spec.isError());
}

@Override
public String toString ()
{
  final StringBuilder sb = new StringBuilder( "Decl" );
  sb.append( MiscUtils.objRefString(this) ).append('{');
  sb.append( sclass );
  sb.append( ' ' ).append( kind );
  sb.append( " '" ).append( symbol ).append( "'" );
  sb.append( " " ).append( type.readableType() ).append( MiscUtils.objRefString( type ) );
  sb.append( ", prevDecl=" ).append( MiscUtils.objRefString(prevDecl) );
  sb.append( ", storageScope=" ).append( MiscUtils.objRefString(storageScope) );
  sb.append( ", visibilityScope=" ).append( MiscUtils.objRefString(visibilityScope) );
  sb.append( ", linkage=" ).append( linkage );
  sb.append( ", bitfieldWidth=" ).append( bitfieldWidth );
  sb.append( ", defined=" ).append( defined );
  sb.append( ", m_error=" ).append( m_error );
  sb.append( '}' );
  return sb.toString();
}
}
