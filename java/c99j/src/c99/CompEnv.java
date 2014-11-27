package c99;

public class CompEnv
{
public final CompilerOptions opts;
public final IErrorReporter  reporter;

public CompEnv ( final CompilerOptions opts, final IErrorReporter reporter )
{
  this.opts = opts;
  this.reporter = reporter;
}
}
