package c99.parser;

import c99.ISourceRange;
import c99.SourceRange;

public class BisonLocation implements ISourceRange
{
/**
 * The first, inclusive, position in the range.
 */
public Position begin;

/**
 * The first position beyond the range.
 */
public Position end;

/**
 * Create a <code>Location</code> denoting an empty range located at
 * a given point.
 * @param loc The position at which the range is anchored.
 */
public BisonLocation (Position loc) {
  this.begin = this.end = loc;
}

/**
 * Create a <code>Location</code> from the endpoints of the range.
 * @param begin The first position included in the range.
 * @param end   The first position beyond the range.
 */
public BisonLocation (Position begin, Position end) {
  this.begin = begin;
  this.end = end;
}

@Override
public boolean equals ( Object o )
{
  if (this == o) return true;
  if (!(o instanceof BisonLocation)) return false;

  final BisonLocation that = (BisonLocation) o;

  if (begin != null ? !begin.equals( that.begin ) : that.begin != null) return false;
  if (end != null ? !end.equals( that.end ) : that.end != null) return false;

  return true;
}

@Override
public int hashCode ()
{
  int result = begin != null ? begin.hashCode() : 0;
  result = 31 * result + (end != null ? end.hashCode() : 0);
  return result;
}

/**
 * Print a representation of the location.  For this to be correct,
 * <code>Position</code> should override the <code>equals</code>
 * method.
 */
public String toString () {
  if (begin == null)
    return "";
  return SourceRange.formatRange( this );
}

@Override public final String getFileName () { return begin.fileName; }
@Override public final int getLine1 () { return begin.line; }
@Override public final int getCol1 () { return begin.col; }
@Override public final String getFileName2 () { return end.fileName; }
@Override public final int getLine2 () { return end.line; }
@Override public final int getCol2 () { return end.col; }
}
