package c99.parser.pp;

import c99.CompilerOptions;

import java.io.File;
import java.util.LinkedHashSet;

public class SearchPathFactory
{
private LinkedHashSet<String> m_defSet = new LinkedHashSet<String>();
private LinkedHashSet<String> m_angledSet = new LinkedHashSet<String>();
private LinkedHashSet<String> m_quotedSet = new LinkedHashSet<String>();

public void addDefaultInclude ( String path )
{
  assert path != null;
  m_defSet.add( path );
}

public void addInclude ( String path )
{
  assert path != null;
  m_angledSet.add( path );
}

public void addQuotedInclude ( String path )
{
  assert path != null;
  m_quotedSet.add( path );
}

/** Must be invoked after all options have been parsed, to prepare the internal structures */
public ISearchPath finish ( CompilerOptions opts )
{
  if (opts.noStdInc)
    m_defSet.clear();
  else
    m_angledSet.removeAll( m_defSet );

  return new ISearchPath(){
    @Override
    public String searchQuoted ( final String curFile, final String fileName )
    {
      File cur = new File(new File(curFile).getParentFile(), fileName);
      if (cur.exists())
        return cur.toString();

      String res;
      if ( (res = search( m_quotedSet, fileName )) != null)
        return res;

      return searchAngled( fileName );
    }

    @Override
    public String searchAngled ( final String fileName )
    {
      String res;
      if ( (res = search( m_angledSet, fileName )) != null)
        return res;
      if ( (res = search( m_defSet, fileName )) != null)
        return res;
      return null;
    }
  };
}

private static String search ( LinkedHashSet<String> set, String fileName )
{
  for ( String dir : set )
  {
    File file = new File(dir, fileName);
    if (file.exists())
      return file.toString();
  }
  return null;
}

}
