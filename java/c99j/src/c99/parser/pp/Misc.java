package c99.parser.pp;

public final class Misc
{
private Misc () {}

public static String simpleEscapeString ( String str )
{
  final StringBuilder buf = new StringBuilder( str.length() + 8 );
  buf.append( '"' );
  for ( int len = str.length(), i = 0; i < len; ++i )
  {
    final char ch = str.charAt( i );
    if (ch == '"' || ch == '\\')
      buf.append( '\\' );
    else if (ch < 32)
    {
      buf.append( '\\' )
         .append( (ch >>> 6) & 7 )
         .append( (ch >>> 3) & 7 )
         .append( ch & 7 );
    }
    buf.append( ch );
  }
  buf.append( '"' );
  return buf.toString();
}


} // class

