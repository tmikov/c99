package c99.parser;

public class Decl
{
public final Code sclass;
public final Symbol symbol;
public final Scope scope;
Decl prevDecl;
Code storage;
Tree type;
Tree init;
Tree body;

public Decl ( final Code sclass, final Symbol symbol, final Scope scope )
{
  this.sclass = sclass;
  this.symbol = symbol;
  this.scope = scope;
}
}
