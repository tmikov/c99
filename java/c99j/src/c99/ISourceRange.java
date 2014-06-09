package c99;

public interface ISourceRange
{
public String getFileName ();
public int getLine1 ();
public int getCol1 ();
/** Inclusive */
public int getLine2 ();
/** Exclusive */
public int getCol2 ();
}
