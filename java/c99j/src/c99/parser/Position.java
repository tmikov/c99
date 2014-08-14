package c99.parser;

public class Position
{
public final String fileName;
public final int line, col;

public Position ( final String fileName, final int line, final int col )
{
  this.fileName = fileName;
  this.line = line;
  this.col = col;
}

public Position ()
{
  this.fileName = null;
  this.line = this.col = 0;
}

@Override
public boolean equals ( final Object o )
{
  if (this == o)
  {
    return true;
  }
  if (!(o instanceof Position))
  {
    return false;
  }

  Position position = (Position)o;

  return col == position.col && line == position.line &&
         !(fileName != null ? !fileName.equals( position.fileName ) : position.fileName != null);
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
  return fileName + '(' + line + ")[" + col + ']';
}
}
