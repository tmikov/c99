package c99.parser.tree;

import c99.ISourceRange;
import c99.SourceRange;
import c99.parser.Symbol;
import c99.parser.Trees;

/**
 * Created by tmikov on 1/8/15.
 */
public class TIdent extends SourceRange
{
public final Symbol ident;

public TIdent ( ISourceRange rng, Symbol ident )
{
  super( rng );
  this.ident = ident;
}
}
