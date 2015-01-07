package c99.parser.tree;

import c99.parser.DeclActions;

import java.util.LinkedList;

/**
 * Created by tmikov on 1/6/15.
 */
public final class TDeclList extends LinkedList<TDeclaration>
{
  public boolean ellipsis;

  public TDeclList setEllipsis () { this.ellipsis = true; return this; }
}
