package c99;

import java.io.PrintStream;
import java.io.PrintWriter;

public final class MiscUtils
{
private MiscUtils () {}

private static final String s_sp1 = "                                                 ";
private static final String s_sp2 = "     ";

public static void printIndent ( int indent, PrintWriter out )
{
  for ( ; indent >= s_sp1.length(); indent -= s_sp1.length() )
    out.print( s_sp1 );
  for ( ; indent >= s_sp2.length(); indent -= s_sp2.length() )
    out.print( s_sp2 );
  while (--indent >= 0)
    out.print( ' ' );
}

public static void printIndent ( int indent, PrintStream out )
{
  for ( ; indent >= s_sp1.length(); indent -= s_sp1.length() )
    out.print( s_sp1 );
  for ( ; indent >= s_sp2.length(); indent -= s_sp2.length() )
    out.print( s_sp2 );
  while (--indent >= 0)
    out.print( ' ' );
}

public static String objRefString ( Object x )
{
  return "@" + Integer.toHexString(System.identityHashCode( x ));
}
}
