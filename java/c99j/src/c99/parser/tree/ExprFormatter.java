package c99.parser.tree;

import c99.SourceRange;

import java.io.PrintWriter;

public class ExprFormatter
{
private static final int INDENT_STEP = 4;

static final String s_sp1 = "                                                 ";
static final String s_sp2 = "     ";

public static void printIndent ( int indent, PrintWriter out )
{
  for ( ; indent >= s_sp1.length(); indent -= s_sp1.length() )
    out.print( s_sp1 );
  for ( ; indent >= s_sp2.length(); indent -= s_sp2.length() )
    out.print( s_sp2 );
  while (--indent >= 0)
    out.print( ' ' );
}

public static void format ( int indent, PrintWriter out, TExpr.Expr e )
{
  printIndent( indent, out );
  String details = e.formatDetails();
  out.format( "%s:'%s'%s <%s>\n", e.getCode().name(), e.getQual().readableType(),
          details, SourceRange.formatRange(e) );
  for ( int i = 0, c = e.getNumChildren(); i < c; ++i )
    format( indent + INDENT_STEP, out, e.getChild( i ) );
  out.flush();
}


}
