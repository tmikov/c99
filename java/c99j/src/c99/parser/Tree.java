package c99.parser;

import java.io.PrintStream;

public abstract class Tree
{
public final String name;

protected Tree ( final String name )
{
  this.name = name;
}

public abstract int childCount ();
public abstract Tree child ( int n );
public abstract void setChild ( int n, Tree ch );

@Override
public String toString ()
{
  return this.name+'['+childCount()+']';
}

static final String s_sp1 = "                                                 ";
static final String s_sp2 = "     ";
public static void printIndent ( int indent, PrintStream out )
{
  for ( ; indent >= s_sp1.length(); indent -= s_sp1.length() )
    out.print( s_sp1 );
  for ( ; indent >= s_sp2.length(); indent -= s_sp2.length() )
    out.print( s_sp2 );
  while (--indent >= 0)
    out.print( ' ' );
}

private static final int INDENT_STEP = 4;

private static final Tree s_nullChild = new Tree("<null>"){
  @Override public int childCount () { return 0; }
  @Override public Tree child ( final int n ) { assert false; return null; }
  @Override public void setChild ( final int n, final Tree ch ) { assert false; }
};

private final Tree getChild ( int n )
{
  Tree ch;
  if ((ch = child( n )) == null)
    ch = s_nullChild;
  return ch;
}

public final void print ( int indent, PrintStream out, int maxWidth )
{
  printIndent( indent, out );

  int remWidth = maxWidth - indent;
  StringBuilder shortBuf = remWidth > 0 ? new StringBuilder( remWidth ) : null;

  if (shortBuf != null)
  {
    if (tryFormat( remWidth, shortBuf ))
    {
      out.println( shortBuf.toString() );
      return;
    }
  }

  out.println( this.name );

  final int chCount = childCount();
  if (chCount == 0)
    return;

  indent += INDENT_STEP;
  remWidth -= INDENT_STEP;

  for ( int i = 0; i < chCount; ++i )
  {
    final Tree ch = getChild( i );

    if (remWidth > 0 &&
        shortBuf != null /*redundant but helps verification*/)
    {
      shortBuf.setLength( 0 );
      if (ch.tryFormat( remWidth, shortBuf ))
      {
        printIndent( indent, out );
        out.println( shortBuf.toString() );
        continue;
      }
    }

    ch.print( indent, out, remWidth );
  }
}

public final boolean tryFormat ( int maxWidth, StringBuilder shortBuf )
{
  if (maxWidth <= 0)
    return false;

  final int chCount = childCount();
  final int startLen = shortBuf.length();
  int remWidth = maxWidth;

  if (chCount > 0)
  {
    if (--remWidth < 0)
      return false;
    shortBuf.append( '(' );
  }

  if ( (remWidth -= this.name.length()) < 0)
    return false;
  shortBuf.append( this.name );

  if (chCount > 0)
  {
    for ( int i = 0; i < chCount; ++i )
    {
      if (--remWidth < 0)
        return false;
      shortBuf.append( ' ' );

      if (!getChild( i ).tryFormat( remWidth, shortBuf ))
        return false;
      remWidth = maxWidth - shortBuf.length() + startLen;
      assert remWidth >= 0;
    }

    if (--remWidth < 0)
      return false;
    shortBuf.append( ')' );
  }

  return true;
}

} // class
