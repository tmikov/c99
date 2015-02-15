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

public static String asciiString ( byte[] bytes )
{
  return asciiString( bytes, 0, bytes.length );
}

public static byte[] asciiBytes ( String x )
{
  byte[] res = new byte[x.length()];
  for ( int len = x.length(), i = 0; i < len; ++i )
    res[i] = (byte)x.charAt( i );
  return res;
}

public static boolean equals ( byte[] a, int offA, byte[] b, int offB, int len )
{
  assert offA + len <= a.length && offB + len <= b.length;
  final int end = offA + len;
  for ( ; offA < end; ++offA, ++offB )
    if (a[offA] != b[offB])
      return false;

  return true;
}

public static int compare ( byte[] a, int offA, int lenA, byte[] b, int offB, int lenB )
{
  int len = Math.min(lenA, lenB);
  int end = offA + len;
  for ( ; offA < end; ++offA, ++offB )
  {
    int d = (a[offA] & 255) - (b[offB] & 255);
    if (d != 0)
      return d;
  }
  return len < lenB ? -1 : (len < lenA ? +1 : 0);
}
} // class
