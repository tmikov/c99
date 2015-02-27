package c99;

import c99.parser.tree.Visitor;

public class CompEnv
{
public final CompilerOptions opts;
public final IErrorReporter  reporter;
public final Visitor.TranslationUnit visitor;

public CompEnv ( final CompilerOptions opts, final IErrorReporter reporter, Visitor.TranslationUnit visitor )
{
  this.opts = opts;
  this.reporter = reporter;
  this.visitor = visitor;
}
}
