package c99.parser.pp;

import java.util.Date;

public class PreprOptions implements IPreprOptions
{
private boolean m_signedChar = true;
private boolean m_noStdInc = false;
private boolean m_gccExtensions = true;

private boolean m_warnUndef = false;
private int m_maxIncludeDepth = 256;
private Date m_forcedDate = null;

@Override public final boolean getSignedChar () { return m_signedChar; }
@Override public final boolean getNoStdInc () { return m_noStdInc; }
@Override public final boolean getGccExtensions () { return m_gccExtensions; }
@Override public final boolean getWarnUndef () { return m_warnUndef; }
@Override public final int getMaxIncludeDepth () { return m_maxIncludeDepth; }
@Override public final Date getForcedDate () { return m_forcedDate; }

public final void setSignedChar ( boolean signedChar ) { m_signedChar = signedChar; }
public final void setNoStdInc ( boolean noStdInc ) { this.m_noStdInc = noStdInc; }
public final void setGccExtensions ( boolean gccExtensions ) { this.m_gccExtensions = gccExtensions; }
public final void setWarnUndef ( boolean warnUndef ) { this.m_warnUndef = warnUndef; }
public final void setMaxIncludeDepth ( int maxIncludeDepth ) { this.m_maxIncludeDepth = maxIncludeDepth; }
public final void setForcedDate ( Date forcedDate ) { m_forcedDate = forcedDate; }
}
