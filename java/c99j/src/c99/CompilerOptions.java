package c99;

import c99.parser.pp.IPreprOptions;
import c99.parser.pp.PreprOptions;

public class CompilerOptions implements IPreprOptions
{
private final PreprOptions m_preprOptions = new PreprOptions();

public int maxAlign = 8;
public int defCodePointers = 0; // 0:near, 1:far, 2:huge
public int defDataPointers = 0; // 0:near, 1:far, 2:huge

public PreprOptions getPreprOptions ()
{
  return m_preprOptions;
}

public boolean getSignedChar ()
{
  return m_preprOptions.getSignedChar();
}

public boolean getNoStdInc ()
{
  return m_preprOptions.getNoStdInc();
}

public boolean getGccExtensions ()
{
  return m_preprOptions.getGccExtensions();
}

public boolean getWarnUndef ()
{
  return m_preprOptions.getWarnUndef();
}

public int getMaxIncludeDepth ()
{
  return m_preprOptions.getMaxIncludeDepth();
}
} // class

