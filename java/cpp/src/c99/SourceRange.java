package c99;

public class SourceRange implements ISourceRange
{
public String fileName;
public String fileName2;
public int line1, col1;
public int line2;
public int col2;

public SourceRange ()
{}

public SourceRange ( ISourceRange rng )
{
  if (rng != null)
    setRange( rng );
}

public final SourceRange setRange ( String fileName, int line1, int col1, String fileName2, int line2, int col2 )
{
  this.fileName = fileName;
  this.line1 = line1;
  this.col1 = col1;
  this.fileName2 = fileName2;
  this.line2 = line2;
  this.col2 = col2;
  return this;
}

public final SourceRange setRange ( String fileName, int line1, int col1, int line2, int col2 )
{
  this.fileName = fileName;
  this.line1 = line1;
  this.col1 = col1;
  this.fileName2 = null;
  this.line2 = line2;
  this.col2 = col2;
  return this;
}

public final SourceRange setRange ( ISourceRange rng )
{
  this.fileName = rng.getFileName();
  this.line1 = rng.getLine1();
  this.col1 = rng.getCol1();
  this.fileName2 = rng.getFileName2();
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

public final SourceRange setFileName2 ( String fileName2 )
{
  this.fileName2 = fileName2;
  return this;
}

public final SourceRange setLocation ( String fileName, int line, int col )
{
  return setRange( fileName, line, col, line, col );
}

public final SourceRange setLocation ( int line, int col )
{
  return setRange( line, col, line, col );
}

public final SourceRange setLocation ( ISourceRange rng )
{
  return setLocation( rng.getFileName(), rng.getLine1(), rng.getCol1() );
}

public final SourceRange union ( ISourceRange rng )
{
  // TODO: better union algorithm
  if (Utils.equals( this.fileName, rng.getFileName() ) &&
          (rng.getLine1() < this.line1 || rng.getLine1() == this.line1 && rng.getCol1() < this.col1))
  {
    this.line1 = rng.getLine1();
    this.col1 = rng.getCol1();
  }
  if (Utils.equals( this.fileName2, rng.getFileName2() ) &&
          (rng.getLine2() > this.line2 || rng.getLine2() == this.line2 && rng.getCol2() > this.col2))
  {
    this.line2 = rng.getLine2();
    this.col2 = rng.getCol2();
  }
  return this;
}

public final SourceRange shiftExtend ( int length )
{
  this.line1 = this.line2;
  this.col1 = this.col2;
  this.col2 += length;
  return this;
}

public final SourceRange extendBefore ( ISourceRange end )
{
  this.line2 = end.getLine1();
  this.col2 = end.getCol1();
  return this;
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

@Override
public String getFileName2 ()
{
  return this.fileName2;
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

public static boolean isRealRange ( ISourceRange rng )
{
  return rng != null &&
         (rng.getLine2() > rng.getLine1() ||
          rng.getLine2() == rng.getLine1() && rng.getCol2() > rng.getCol1());
}

public static String formatRange ( ISourceRange rng )
{
  if (rng == null)
    return "";

  if (isRealRange( rng ))
  {
    if (rng.getLine1() != rng.getLine2())
    {
      return String.format( "%s(%d)[%d]..(%d)[%d]",
                            Utils.defaultString( rng.getFileName() ),
                            rng.getLine1(), rng.getCol1(),
                            rng.getLine2(), rng.getCol2()
      );
    }
    else
    {
      return String.format( "%s(%d)[%d..%d]",
                            Utils.defaultString( rng.getFileName() ),
                            rng.getLine1(), rng.getCol1(), rng.getCol2()
      );
    }
  }
  else
  {
    return String.format( "%s(%d)[%d]",
      Utils.defaultString( rng.getFileName() ),
      rng.getLine1(), rng.getCol1()
    );
  }
}

@Override
public String toString ()
{
  return "SourceRange{" + formatRange( this ) + '}';
}
} // class
