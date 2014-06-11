package c99;

public class SourceRange implements ISourceRange
{
public String fileName;
public int line1, col1;
public int line2;
public int col2;

public final SourceRange setRange ( String fileName, int line1, int col1, int line2, int col2 )
{
  this.fileName = fileName;
  this.line1 = line1;
  this.col1 = col1;
  this.line2 = line2;
  this.col2 = col2;
  return this;
}

public final SourceRange setRange ( ISourceRange rng )
{
  this.fileName = rng.getFileName();
  this.line1 = rng.getLine1();
  this.col1 = rng.getCol1();
  this.line2 = rng.getLine2();
  this.col2 = rng.getCol2();
  return this;
}

public final SourceRange setRange ( int line1, int col1, int line2, int col2 )
{
  this.line1 = line1;
  this.col1 = col1;
  this.line2 = line2;
  this.col2 = col2;
  return this;
}

public final SourceRange setFileName ( String fileName )
{
  this.fileName = fileName;
  return this;
}

public final SourceRange setLocation ( String fileName, int line, int col )
{
  return setRange( fileName, line, col, line, col + 1 );
}

public final SourceRange setLocation ( int line, int col )
{
  return setRange( line, col, line, col + 1 );
}

public final SourceRange extend ( ISourceRange end )
{
  this.line2 = end.getLine2();
  this.col2 = end.getCol2();
  return this;
}

public final void translate ( int colOfs )
{
  this.col1 += colOfs;
  this.col2 += colOfs;
}

public final void setLength ( int len )
{
  this.line2 = this.line1;
  this.col2 = this.col1 + len;
}

@Override
public final String getFileName ()
{
  return fileName;
}

@Override
public final int getLine1 ()
{
  return line1;
}

@Override
public final int getCol1 ()
{
  return col1;
}

/** Inclusive */
@Override
public final int getLine2 ()
{
  return line2;
}

/** Exclusive */
@Override
public final int getCol2 ()
{
  return col2;
}

@Override
public String toString ()
{
  if (line2 != line1)
    return "SourceRange{" +
           fileName + '(' +
           line1 +
           '[' + col1 +
           "].." + line2 +
           '[' + col2 +
           "])}";
  else
    return "SourceRange{" +
           fileName + '(' +
           line1 +
           '[' + col1 +
           ".." + col2 +
           "])}";
}
} // class
