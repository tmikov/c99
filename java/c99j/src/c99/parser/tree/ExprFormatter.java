package c99.parser.tree;

import c99.MiscUtils;
import c99.SourceRange;

import java.io.PrintWriter;

public class ExprFormatter
{
private static final int INDENT_STEP = 4;

public static void format ( int indent, PrintWriter out, TExpr.Expr e )
{
  MiscUtils.printIndent( indent, out );
  String details = e.formatDetails();
  out.format( "%s:'%s'%s <%s>\n", e.getCode().name(), e.getQual().readableType(),
          details, SourceRange.formatRange(e) );
  for ( int i = 0, c = e.getNumChildren(); i < c; ++i )
    format( indent + INDENT_STEP, out, e.getChild( i ) );
  out.flush();
}


}
