package c99.parser;

import c99.ISourceRange;
import c99.SourceRange;
import c99.Types.*;

public class Decl extends SourceRange
{

public static enum Kind
{
  VAR,
  ENUM_CONST,
  TAG,
}
Decl prev;

public final Kind kind;
public final Scope scope;
public       SClass sclass;
public final Linkage linkage;
public final Symbol symbol;
public final Qual type;
public boolean defined;
public boolean error;


public Decl (
  ISourceRange rng, Kind kind, Scope scope, SClass sclass, Linkage linkage, Symbol symbol, Qual type,
  boolean defined, boolean error
)
{
  super(rng);
  this.kind = kind;
  this.scope = scope;
  this.sclass = sclass;
  this.linkage = linkage;
  this.symbol = symbol;
  this.type = type;
  this.error = error;
}
}
