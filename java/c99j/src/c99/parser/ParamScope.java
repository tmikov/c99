package c99.parser;

import c99.CompilerOptions;

public final class ParamScope extends Scope
{
private boolean m_ellipsis;

public ParamScope ( CompilerOptions opts, Scope parent )
{
  super( opts, Kind.PARAM, parent );
}

public final boolean getEllipsis ()
{
  assert this.kind == Kind.PARAM;
  return m_ellipsis;
}

public final void setEllipsis ()
{
  assert this.kind == Kind.PARAM;
  m_ellipsis = true;
}
}
