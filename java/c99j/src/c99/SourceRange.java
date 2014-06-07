package c99;

public class SourceRange implements ISourceRange
{
public String fileName1;
public int line1, col1;
public String fileName2;
/** Inclusive */
public int line2;
/** Exclusive */
public int col2;

public final SourceRange setRange ( String fileName, int line1, int col1, int line2, int col2 )
{
  this.fileName1 = this.fileName2 = fileName;
  this.line1 = line1;
  this.col1 = col1;
  this.line2 = line2;
  this.col2 = col2;
  return this;
}

public final SourceRange setRange ( ISourceRange rng )
{
  this.fileName1 = rng.getFileName1();
  this.fileName2 = rng.getFileName2();
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
  this.fileName1 = this.fileName2 = fileName;
  return this;
}

public final SourceRange setLocation ( String fileName, int line, int col )
{
  return setRange( fileName, line, col, line, col + 1 );
}

@Override
public final String getFileName1 ()
{
  return fileName1;
}

@Override
public final String getFileName2 ()
{
  return fileName2;
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
  if (Utils.equals( fileName1, fileName2 ))
  {
    if (line2 != line1)
      return "SourceRange{" +
             fileName1 + '(' +
             line1 +
             '[' + col1 +
             "].." + line2 +
             '[' + col2 +
             "])}";
    else
      return "SourceRange{" +
             fileName1 + '(' +
             line1 +
             '[' + col1 +
             ".." + col2 +
             "])}";
  }
  else
  {
    return "SourceRange{" +
           fileName1 + '(' +
           line1 +
           '[' + col1 +
           "]).." + fileName2 +
           '(' + line2 +
           '[' + col2 +
           "])}";
  }
}
} // class
