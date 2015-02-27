package c99.parser.tree;

import c99.parser.Decl;

public final class Visitor
{
private Visitor () {};

public static interface TranslationUnit
{
  public void visitRecordDecl ( TSpecAggNode specNode, Decl decl, boolean definition );
  public DeclaratorList visitDeclaratorList ( TSpecNode specNode );
}

public static interface DeclaratorList
{
  public void visitDeclaration ( TDeclaration tDecl, Decl decl );
  public void visitEmptyDeclaration ( TDeclaration tDecl );
  public void end ();
}

}
