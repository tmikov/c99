package c99.parser.pp;

public interface ISearchPath
{
class Result
{
  public final String path;
  public final String absPath;

  public Result ( final String path, final String absPath )
  {
    this.path = path;
    this.absPath = absPath;
  }

  @Override
  public String toString ()
  {
    return "Result{" +
           "path='" + path + '\'' +
           ", absPath='" + absPath + '\'' +
           '}';
  }
}

public Result searchQuoted ( String curFile, String fileName );
public Result searchAngled ( String fileName );
}
