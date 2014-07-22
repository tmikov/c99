package c99.parser.pp;

import c99.CompilerOptions;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;

public class SearchPathFactory
{
private LinkedHashSet<File> m_defSet = new LinkedHashSet<File>();
private LinkedHashSet<File> m_angledSet = new LinkedHashSet<File>();
private LinkedHashSet<File> m_quotedSet = new LinkedHashSet<File>();

private final HashMap<File,ISearchPath.Result> m_angledCache = new HashMap<File, ISearchPath.Result>();
/**
 * Maps from "cur-directory:q-include-name" to Result
 */
private final HashMap<String,ISearchPath.Result> m_quotedCache = new HashMap<String, ISearchPath.Result>();

public void addDefaultInclude ( String path )
{
  assert path != null;
  m_defSet.add( new File(path) );
}

public void addInclude ( String path )
{
  assert path != null;
  m_angledSet.add( new File(path) );
}

public void addQuotedInclude ( String path )
{
  assert path != null;
  m_quotedSet.add( new File(path) );
}

/** Must be invoked after all options have been parsed, to prepare the internal structures */
public ISearchPath finish ( CompilerOptions opts )
{
  if (opts.noStdInc)
    m_defSet.clear();
  else
    m_angledSet.removeAll( m_defSet );

  // Remove all non-existent include directories to speed up the lookup later
  for (Iterator<File> it = m_defSet.iterator(); it.hasNext(); )
  {
    final File dirPath = it.next();
    if (!dirPath.isDirectory())
      it.remove();
  }
  for (Iterator<File> it = m_angledSet.iterator(); it.hasNext(); )
  {
    final File dirPath = it.next();
    if (!dirPath.isDirectory())
      it.remove();
  }

  return new ISearchPath(){
    @Override
    public Result searchQuoted ( final String curFile, final String fileName )
    {
      return _searchQuoted( curFile, new File(fileName) );
    }

    @Override
    public Result searchAngled ( final String fileName )
    {
      return _searchAngled( new File(fileName) );
    }
  };
}

private final ISearchPath.Result _searchQuoted ( final String curFile, final File fileName )
{
  if (fileName.isAbsolute())
    return _searchAngled( fileName );

  ISearchPath.Result res;
  final File curDir = new File(curFile).getParentFile();
  final String key = curDir.getPath() + File.pathSeparatorChar + fileName;

  if ( (res = m_quotedCache.get( key )) == null )
  {
    File f = new File( curDir, fileName.getPath() );
    if (!f.exists())
      f = search( m_quotedSet, fileName.getPath() );

    if (f != null)
      res = new ISearchPath.Result( f.getPath(), f.getAbsolutePath() );
    else
      res = _searchAngled( fileName );

    if (res != null)
      m_quotedCache.put( key, res );
  }

  return res;
}

private final ISearchPath.Result _searchAngled ( final File fileName )
{
  ISearchPath.Result res;

  if ( (res = m_angledCache.get( fileName )) == null)
  {
    File f;

    if (fileName.isAbsolute())
    {
      if (fileName.exists())
        f = fileName;
      else
        f = null;
    }
    else if ( (f = search( m_angledSet, fileName.getPath() )) == null)
      f = search( m_defSet, fileName.getPath() );

    if (f != null)
      m_angledCache.put( fileName, res = new ISearchPath.Result(f.getPath(), f.getAbsolutePath()) );
  }

  return res;
}

private static File search ( LinkedHashSet<File> set, String fileName )
{
  for ( File dir : set )
  {
    File file = new File(dir, fileName);
    if (file.exists())
      return file;
  }
  return null;
}

}
