package c99.parser;

import c99.Constant;
import c99.ISourceRange;
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
Decl prev;
public final Decl importedDecl;
public final Scope scope;
public       SClass sclass;
public final Linkage linkage;
public final Symbol symbol;
public Qual type;
public Constant.ArithC enumValue; // FIXME
public int bitfieldWidth = -1; //< -1 means not a bit-field
public boolean defined;
public boolean error;


public Decl (
  ISourceRange rng, Kind kind, Scope scope, SClass sclass, Linkage linkage, Symbol symbol, Qual type,
  boolean defined, boolean error
)
{
  super(rng);
  this.importedDecl = null;
  this.kind = kind;
  this.scope = scope;
  this.sclass = sclass;
  this.linkage = linkage;
  this.symbol = symbol;
  this.type = type;
  this.defined = defined;
  this.error = error || type.spec.isError();
}

/** Import a declaration into the current scope */
public Decl ( ISourceRange rng, Scope scope, Decl importedDecl, boolean error )
{
  super(rng);
  assert importedDecl.scope != scope;
  assert importedDecl.importedDecl == null;

  this.importedDecl = importedDecl;
  this.kind = importedDecl.kind;
  this.scope = scope;
  this.sclass = importedDecl.sclass;
  this.linkage = importedDecl.linkage;
  this.symbol = importedDecl.symbol;
  this.type = importedDecl.type;
  this.defined = importedDecl.defined;
  this.error = importedDecl.error | error;
}

public final void orError ( boolean f )
{
  this.error |= f;
}

}
