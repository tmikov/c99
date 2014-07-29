package c99.parser;

public class Position
{
public String fileName;
public int line, col;

public Position ( final String fileName, final int line, final int col )
{
  this.fileName = fileName;
  this.line = line;
  this.col = col;
}

public Position ()
{
}

}
