package c99.parser;

import java.io.InputStream;

public interface IIncludeLocator
{
/**
 *
 * @param fileName file name to open
 * @return null on error
 */
public InputStream locate ( String fileName );
}
