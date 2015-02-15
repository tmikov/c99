package c99.parser.tree;

import c99.ISourceRange;
import c99.SourceRange;

/**
 * Untyped string literal. Expressions use {@link c99.parser.tree.TExpr.StringLiteral} instead
 */
public class TStringLiteral extends SourceRange
{
  public final byte[] value;
  public TStringLiteral ( final ISourceRange rng, byte[] value )
  {
    super(rng);
    this.value = value;
  }
}
