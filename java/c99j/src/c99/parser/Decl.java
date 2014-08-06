package c99.parser;

import c99.parser.ast.Ast;

public class Decl
{
public final Code sclass;
public final Symbol symbol;
public final Scope scope;
Decl prevDecl;
Code storage;
Ast type;
Ast init;
Ast body;

public Decl ( final Code sclass, final Symbol symbol, final Scope scope )
{
  this.sclass = sclass;
  this.symbol = symbol;
  this.scope = scope;
}
}
