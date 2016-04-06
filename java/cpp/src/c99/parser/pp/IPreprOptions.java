package c99.parser.pp;

import java.util.Date;

public interface IPreprOptions
{
boolean getSignedChar ();
boolean getNoStdInc ();
boolean getGccExtensions ();
boolean getWarnUndef ();

int getMaxIncludeDepth ();

Date getForcedDate ();
}
