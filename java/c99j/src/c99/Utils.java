package c99;

public final class Utils
{
private Utils () {}

public static boolean equals ( Object a, Object b )
{
  return a == b || !(a == null || b == null) && a.equals( b );
}

public static boolean isEmpty ( String x )
{
  return x == null || x.isEmpty();
}

public static boolean isNotEmpty ( String x )
{
  return x == null || !x.isEmpty();
}

public static String defaultIfEmpty ( String x, String def )
{
  return isNotEmpty( x ) ? x : def;
}

public static String defaultString ( String x )
{
  return defaultIfEmpty( x, "" );
}

public static String asciiString ( byte[] bytes, int from, int count )
{
  char value[] = new char[count];

  for ( int i = 0; i < count; ++i )
    value[i] = (char)(bytes[i + from] & 0xff);

  return new String( value );
}

public static boolean equals ( byte[] a, int offA, byte[] b, int offB, int len )
{
  final int end = offA + len;
  for ( ; offA < end; ++offA, ++offB )
    if (a[offA] != b[offB])
      return false;

  return true;
}

} // class
