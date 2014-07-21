package c99.parser.pp;

public interface ISearchPath
{
public String searchQuoted ( String curFile, String fileName );
public String searchAngled ( String fileName );
}
