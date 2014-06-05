package c99;

import c99.ILocation;

public final class Location implements ILocation
{
public String fileName;
public int line, col;

public final void init ( int line, int col )
{
  this.line = line;
  this.col = col;
}

@Override
public String getFileName ()
{
  return fileName;
}

@Override
public final int getLine ()
{
  return line;
}

@Override
public final int getCol ()
{
  return col;
}

@Override
public boolean equals ( final Object o )
{
  if (this == o)
  {
    return true;
  }
  if (!(o instanceof Location))
  {
    return false;
  }

  Location location = (Location)o;

  return col == location.col && line == location.line &&
         !(fileName != null ? !fileName.equals( location.fileName ) : location.fileName != null);
}

@Override
public int hashCode ()
{
  int result = fileName != null ? fileName.hashCode() : 0;
  result = 31 * result + line;
  result = 31 * result + col;
  return result;
}

@Override
public String toString ()
{
  return "Location{" +
         "fileName='" + fileName + '\'' +
         ", line=" + line +
         ", col=" + col +
         '}';
}
} // class
