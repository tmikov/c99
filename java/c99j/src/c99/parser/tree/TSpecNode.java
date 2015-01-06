package c99.parser.tree;

import c99.ISourceRange;
import c99.SourceRange;
import c99.parser.Code;
import c99.parser.DeclActions;

/**
 * Created by tmikov on 1/5/15.
 */
public class TSpecNode extends SourceRange
{
  public final Code code;
  public TSpecNode next;

  public TSpecNode ( ISourceRange rgn, Code code )
  {
    super(rgn);
    this.code = code;
  }
}
