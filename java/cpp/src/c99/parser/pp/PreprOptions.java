package c99.parser.pp;

public class PreprOptions implements IPreprOptions
{
private boolean m_noStdInc = false;
private boolean m_gccExtensions = true;

private boolean m_warnUndef = false;
private int m_maxIncludeDepth = 256;

@Override public boolean getNoStdInc () { return m_noStdInc; }
@Override public boolean getGccExtensions () { return m_gccExtensions; }
@Override public boolean getWarnUndef () { return m_warnUndef; }
@Override public int getMaxIncludeDepth () { return m_maxIncludeDepth; }

public void setNoStdInc ( boolean noStdInc ) { this.m_noStdInc = noStdInc; }
public void setGccExtensions ( boolean gccExtensions ) { this.m_gccExtensions = gccExtensions; }
public void setWarnUndef ( boolean warnUndef ) { this.m_warnUndef = warnUndef; }
public void setMaxIncludeDepth ( int maxIncludeDepth ) { this.m_maxIncludeDepth = maxIncludeDepth; }
}
